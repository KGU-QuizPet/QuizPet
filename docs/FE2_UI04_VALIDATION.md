# FE2 UI-04 구현 및 검증 기록

기준: 2026-11-29, main `aacc5b01f98cc7ca5fce8c1a1a8a98254823d717`.
작업 브랜치: `feature/fe2-final-ui`. 병합·커밋·푸시는 수행하지 않았다.

## 디자인 적용

- 기준 자료: `design/UI_DESIGN_SPEC.md` UI-04, `design/screens/UI-04.png`,
  `design/QuizPet_UI_Design_v2.html`.
- `CategoryPanel`: 공식 제목, 좌측 정렬, 상하 28 / 좌우 32 여백, 카드 간격 20,
  최대 3열·2행(현재 분야 5개), 작은 화면의 가변 열 및 내부 세로 스크롤.
- `CategoryCard`: 기존 JButton/선택 계약 유지, 공통 `CategoryIcon` 72px,
  분야색·능력치 칩·설명·장식 원·둥근 면·오프셋 그림자·호버/포커스 강조.
- `UiStyle`의 기존 배경·폰트·색상을 사용했다. 전역 테마·폰트·다른 화면은 변경하지 않았다.
- 랜덤 도전은 구현하지 않았다. 여섯 번째 영역은 비워 두었다.

## 공유 파일 제한에 따른 보류

현재 `AppController.showCategories()`는 `List<Category>`만 전달한다.
다음 데이터는 기존 CategoryPanel 공개 API나 생성자에 전달되지 않는다.

- 현재 사용자·펫·누적 통계: 실제 펫 사이드바 연결 보류.
- 분야별 풀이 수·정답률·조회 오류: NEW/정답률/집계 불가 표시 보류.
- 분야별 학습 카드 존재 여부: 준비 중 카드의 사전 비활성화 연동 보류.

UI에서 저장 파일을 읽거나 홈 화면의 내부 컴포넌트를 뒤져 사용자 데이터를 추정하지 않았다.
임시 펫, 가짜 정답률, 모든 분야를 NEW로 표시하는 대체도 넣지 않았다.
기존 컨트롤러의 콘텐츠 없는 분야 진입 거부는 그대로 동작한다.
향후 담당자와 데이터 전달 계약을 정한 뒤 컨트롤러에서 실제 값을 주입해야 한다.

PR #88의 `AppController`, `PetStage`, `PetStatusPanel`은 변경·복사하지 않았다.
UI-05·06 및 다른 팀원의 화면·기존 테스트·로고 공통 코드는 변경하지 않았다.

## AppLogoTest 진단

- 단독 재실행에서도 `AppLogoTest.java:21` assertion 실패 재현.
- 실행 위치는 프로젝트 루트, 클래스패스는 컴파일 결과와 `src/main/resources`.
- `images/logo/quizpet-logo-square.png`는 1,337,329바이트이며 ImageIO에서
  1254×1254로 정상 디코딩된다. 경로·파일 누락 문제는 아니다.
- `TopBar.createLogo()` → `UiStyle.logoIcon(52)` →
  `BufferedImage.getScaledInstance(..., Image.SCALE_SMOOTH)` → `ImageIcon` 경로에서
  진단 아이콘은 52×52이나 로딩 상태가 2(`MediaTracker.ABORTED`)였다.
- 즉시/1.5초 후 페인팅에서 진단 아이콘의 불투명 픽셀은 0이었다.
  headless=true/false 양쪽에서 재현되었다.
- 같은 원본을 BufferedImage에 drawImage로 축소하여 만든 비교 아이콘은 상태 8(COMPLETE).
  이는 진단용 비교이며 공통 코드에 적용하지 않았다.
- 별도 실제 앱 흐름의 검증 창에서는 상단 로고가 표시되었다. 따라서 UI-04 기능의
  실행을 직접 막지는 않는다. 실행 경로·초기화 순서 또는 JDK/AWT 동작 차이까지
  근본 원인이 확정된 것은 아니다. 특정 JDK 버그로 단정하지 않는다.
- 실행 환경은 Oracle JDK 26.0.2.1, 컴파일 대상은 `--release 17`이다.
  JDK 17 자체 실행 결과와는 구별한다.

진단 코드와 로그는 Git에서 제외되는 `build/`에만 생성했다.
기존 테스트를 삭제·완화·무시하지 않고 전체 실행 결과에 실패를 그대로 포함했다.

## 검증

- 수정 전 전체 기준 테스트: 52개 중 51개 통과, AppLogoTest 1개 실패.
- 수정 후 전체 테스트: 53개 중 52개 통과, 동일 AppLogoTest 1개 실패.
  기존 테스트에서 새 실패는 없고 추가 CategoryPanelDesignTest도 통과했다.
- 운영·테스트 전체 소스 Java 17 대상 컴파일 및 `git diff --check` 통과.
- `CategoryPanelDesignTest` 추가: 실제 LearningService 분야·설명, 선택·초기화,
  비활성 카드 입력 차단, 빈 목록, 실제 AppController의 분야→학습→분야→홈 전환 검증.
- 1280×736, 1920×1016, 960×736, 800×600, 480×680, 320×640 콘텐츠 크기 검사.
  960 이상에서는 3열·2행 및 내부 스크롤 불필요, 320에서는 마지막 분야까지 세로 접근 검증.
- 실제 Swing 검증 창은 임시 사용자 저장 폴더로 실행했다. 기존 data/save를 사용하지 않았다.
  1280×800 프레임의 콘텐츠 렌더링: `build/fe2-ui04-validation/UI-04-live.png`.
- Windows Computer Use 도구는 javaw 접근을 허용하지 않아 OS 창 캡처·수동 클릭 검증은
  수행하지 못했다. 제공 PNG는 표시 중인 Swing 콘텐츠의 printAll 렌더링이다.

관련 로그: `build/fe2-ui04-validation/`.
