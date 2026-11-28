# FE2 UI-05·UI-06 디자인 및 검증

## 기준과 협업 범위

- 작업 기준 HEAD는 `aacc5b01f98cc7ca5fce8c1a1a8a98254823d717`이다.
- 2026-11-29 fetch 결과 origin/main은 PR #88 병합 커밋 `551f44c`로 갱신되었다.
  공식 디자인 명세·HTML·PNG와 이번 학습·퀴즈 대상 파일은 기존 HEAD와 동일하다.
  사용자의 merge 금지에 따라 브랜치에 병합하거나 PR 파일을 복사하지 않았다.
- UI-04 승인된 변경을 보존했다. UI-04 최종 연동 항목은
  `FE2_UI04_VALIDATION.md`에 기록된 펫 사이드바·분야별 실제 통계·콘텐츠 사전 진입 제한이다.
- AppController, PetStage, PetStatusPanel, UiStyle, TopBar, FE3의 결과·기록·정답 피드백
  화면 소스, 서비스·모델·저장·채점·성장 코드는 수정하지 않았다.
- 디자인 기준은 공식 `UI_DESIGN_SPEC.md`, UI-05·UI-06A~D PNG와 HTML이다.
  기존 UiStyle 폰트·배경·색상 및 ImageButton·RoundCard·Badge·CategoryIcon을 재사용한다.
  전역 테마 변경이나 외부 라이브러리 도입은 없다.

## UI-05

- 실제 LearningContent의 제목·본문·분야·태그·난이도를 표시한다.
- 둥근 카드, 분야색 띠, 키워드 칩, 별 난이도, 본문 21px 및 내부 세로 스크롤 적용.
- LearningProgressBar는 전달받은 실제 카드 수와 현재 인덱스를 표시한다.
- 이전·다음·뒤로·퀴즈 시작은 기존 콜백과 버튼 문구를 유지한다.
  넓은 화면은 한 행, 작은 화면은 2×2로 배치한다.
- 해설에서 들어온 경우 퀴즈 시작 비활성, 원래 피드백으로 돌아가기, 첫·마지막 경계,
  빈 목록 처리는 기존 계약을 유지한다. 공유 컨트롤러를 우회하지 않는다.
- 화면별 별도 펫 데이터는 전달되지 않아 사이드바는 최종 연동으로 보류한다.

## UI-06

- QuestionCard로 문제 박스·문항 번호·일반 텍스트 줄바꿈·스크롤을 통일했다.
- 기존 QuizProgressBar를 그대로 재사용한다. 실제 문항 수를 사용하므로 5개로 제한하지 않는다.
- 분야 표시와 아이콘·색상은 `session.getCurrentQuestion().getCategory()`를 기준으로 한다.
  혼합 복습에서 `session.getCategory()`를 사용하지 않는다.
- OX: 큰 O/X, 고유 색, 선택 배경 및 정답·오답·비활성 표시.
- 객관식: 번호 배지, 2×2 보기, 선택 강조, 정답·오답·나머지 보기 약화.
- 단답형: 같은 문제 박스와 둥근 입력 카드, 실제 입력 검증·답안 원문 유지.
- 제출 전 선택/입력 여부 확인, 제출 뒤 잠금, 마지막 문제 종료와 화면 이동은 기존 콜백 유지.
- OptionButton의 기존 생성자·메서드·ActionCommand·접근성 계약을 유지했다.
  FE3는 이 컴포넌트를 재사용하므로 정오답 모드의 최소 크기와 단답형 미리보기 높이를
  별도로 조정했다. FE3 화면 소스를 변경하지 않고 실제 피드백 렌더링으로 확인했다.
- 피드백의 서비스 처리·보상·펫 표정·결과 전환은 기존 FE3와 컨트롤러 구현을 사용한다.
  FE3 정답/오답 화면 전체를 공식 시안으로 다시 디자인하는 작업은 하지 않았다.

## 검증 방법

- JDK 26.0.2.1의 `javac --release 17 -encoding UTF-8`로 운영·테스트 전체 컴파일.
  JDK 17 자체에서 실행한 결과는 아니다.
- UI-05 후 LearningDesignTest, RealDataPanelsTest, AppControllerTest,
  ReviewFlowTest, ShortAnswerQuizTest 관련 검증.
- UI-06 후 QuizDesignTest, LearningQuizUiTest, DesktopLayoutTest,
  RealDataPanelsTest, ShortAnswerQuizTest, ReviewQuizPanelsTest, AnswerFeedbackPetTest 검증.
- 신규 테스트는 실제 LearningService/QuestionRepository의 콘텐츠, 9문제·혼합 분야 복습,
  미선택 제출 차단·제출 후 잠금·중복 제출·초기화·긴 본문과 보기·480~1280 크기를 확인한다.
- 전체 테스트는 assertions·headless 모드로 실행하며 기존 AppLogoTest 실패를 제외하지 않는다.
- PNG는 실제 서비스로 임시 저장 폴더에 사용자·펫을 생성하고 학습→퀴즈→정오답 피드백을
  진행한 콘텐츠를 printAll로 렌더링했다. 기존 data/save에 접근하거나 가짜 통계를 넣지 않았다.

## 렌더링 자료

Git에서 제외되는 `build/fe2-learning-quiz/`:

- `UI-05-service.png`: 실제 학습 카드와 TopBar
- `UI-06-OX-service.png`: OX
- `UI-06-MULTIPLE_CHOICE-service.png`: 객관식
- `UI-06-SA-service.png`: 단답형
- `UI-06C-service.png`, `UI-06D-service.png`: 기존 FE3 정답·오답 흐름 호환성
- `UI-05-480.png`, `UI-06-*-480.png`: 작은 화면 렌더링
- `compile.log`, `all.tsv`, `*Test.log`: 컴파일·전체 테스트 결과

로고의 누락/초기 로딩 문제는 기존 기준 실패로 남겼으며 공통 로고 코드·테스트는 수정하지 않았다.

## UI-06 미리보기 회귀 수정 (2026-11-29 후속 작업)

중단 시 남아 있던 `QuizDesignTest`의
`Short choices must fit the existing FE3 preview` 실패를 원래 조건으로 재현했다.
테스트 소스와 assertion은 변경하지 않았다.

### 원인과 최소 수정

- 380×284 결과 모드에서 기존 선호 높이 합은
  문제 카드 106 + 보기 두 행 144 + 간격 16 + 선택 답안 라벨 22 = 288px이었다.
- 4px 부족으로 GridBagLayout이 최소 크기 기준으로 재배분하면서 문제 카드에 121px,
  보기 뷰포트에 124px을 배정했다. 보기의 선호 높이 144px보다 작아 실제 스크롤이 생겼다.
  단순히 스크롤바를 숨기거나 테스트 기대조건을 완화할 문제가 아니었다.
- MultipleChoicePanel의 결과 모드 간격만 8→4px로 조정했다.
  일반 문제 모드는 기존 20px 간격을 유지한다.
- QuestionCard의 결과 모드 높이는 고정 106px 대신 현재 폰트의 한 줄 높이,
  문항 라벨·내부 여백으로 계산한다. 일반 문제 모드 170px은 그대로 유지한다.
- 수정 후 같은 환경에서 문제 카드 108px, 보기 뷰포트 145px을 배정받았다.
  짧은 문제와 짧은 보기 모두 내부 세로 스크롤 없이 표시된다.

### 확인된 결과

- 기존 QuizDesignTest 전체 통과: 단답형 FE3 미리보기 및 9문제 혼합 분야 복습 검사까지 완료.
- AnswerFeedbackPetTest, LearningQuizUiTest 통과.
- 운영 81개·테스트 55개 소스의 Java 17 대상 전체 컴파일 통과.
- 최종 전체 테스트 55개 중 54개 통과, 기존 기준 실패 AppLogoTest만 1개 실패.
  이번 미리보기 회귀를 포함한 새 실패는 없다.
- 별도 진단으로 긴 결과 보기의 세로 스크롤, 정답·오답 상태, 답안 선택 초기화를 확인했다.
- 이번 후속 작업의 운영 소스 변경은 MultipleChoicePanel과 QuestionCard 두 파일뿐이다.
  앞선 UI-04·05·06 변경과 모든 미추적 파일은 보존했다.

재현·수정 후 레이아웃 수치, 380×284 전후 PNG와 긴 결과 보기 진단 로그는
`build/fe2-preview-regression/`에 보관한다.
