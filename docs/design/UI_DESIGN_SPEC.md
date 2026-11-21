# QuizPet UI 디자인 명세 v2.0

> **이 문서를 읽는 AI에게**
> 이 문서는 QuizPet 데스크탑 UI를 Java Swing으로 구현하기 위한 **단일 기준 문서**입니다.
> 시각 참고 자료는 `docs/design/QuizPet_UI_Design_v2.html`(전체 시안)과 `docs/design/screens/*.png`(화면별 1280×800 캡처)입니다.
> 문서와 시안이 다르면 **이 문서가 우선**합니다. 수치에 `[TBD]`가 붙은 항목은 디자인용 예시값이며, 팀 확정 전에는 상수로 분리해 두고 임의로 의미를 바꾸지 마세요.

---

## 0. 구현 규칙 (반드시 지킬 것)

1. **Java 표준 Swing/AWT/Java2D만 사용.** 외부 UI 라이브러리(FlatLaf 등), HTML/JS, JSON 사용 금지 (README의 기술 스택 규칙).
2. **UI 클래스는 파일을 읽거나 성장·점수를 계산하지 않는다.** 데이터는 `AppController` → `service` 계층에서 받는다 (`ui → service → repository`).
3. **색·폰트·크기는 하드코딩 금지.** 모두 `AppTheme` 상수를 참조한다 (§2).
4. **블러·반투명 그림자 금지.** 그림자는 "같은 모양을 아래로 4px 밀어 SHADOW 색으로 먼저 칠하기"만 사용한다. 반투명은 오버레이(잠금·결과 카드 배경)와 효과(피해 플래시)에만 `AlphaComposite`로 허용.
5. 모든 커스텀 페인팅은 `RenderingHints.KEY_ANTIALIASING = ON`, `KEY_TEXT_ANTIALIASING = ON`.
6. 색은 **역할**로만 쓴다: 주황=행동/선택, 초록=정답·성공, 빨강=오답·피해, 금색=EXP, 분야색=분야 표시. 다른 의미로 재사용 금지.
7. 기존 패키지는 `main.java.com.quizpet.*` 이다. 새 클래스도 같은 패키지 규칙을 따른다.
8. 기존 테스트(`src/test/java`)가 깨지지 않게 한다. 화면 클래스 이름·공개 메서드를 바꿀 경우 `AppController`와 테스트를 함께 수정한다.

---

## 1. 프레임과 셸 구조

### 1.1 프레임

| 항목 | 값 |
|---|---|
| 기준·최소 크기 | 1280 × 800 (`setMinimumSize`) |
| 리사이즈 | 허용 (`setResizable(true)`), 시작 시 `MAXIMIZED_BOTH`는 선택 |
| 늘어나는 영역 | 콘텐츠 영역만 늘어남. 상단바 높이·사이드바 너비는 고정 |
| 기존 코드와의 차이 | 현재 `MainFrame`은 480×720 고정. 이 명세 적용 시 `FRAME_WIDTH/HEIGHT`를 1280/800으로 바꾸고 최소 크기로 사용 |

### 1.2 셸 3종

루트는 `CardLayout`이며 아래 3개 셸 중 하나를 보여 준다.

```
AuthShell   (UI-01, UI-02)
┌──────────────┬───────────────────────────┐
│ Hero 560px   │ Form (BoxLayout.Y)        │
│ (브랜드/미리보기) │ padding 48/72, 세로 중앙 정렬 │
└──────────────┴───────────────────────────┘

GameShell   (UI-03 ~ UI-08, UI-09A)
┌──────────────────────────────────────────┐
│ TopBar 64px                               │ NORTH
├────────────┬─────────────────────────────┤
│ PetSidebar │ Content (CardLayout)         │
│ 320px 고정  │ padding 28(상하)/32(좌우)     │
│ WEST       │ 요소 간 gap 20, CENTER        │
└────────────┴─────────────────────────────┘

BattleShell (UI-09B ~ UI-09G)
┌──────────────────────────────────────────┐
│ TopBar 64px                               │ NORTH
├──────────────────────────────────────────┤
│ ArenaPanel 높이 470 고정 (전체 너비)        │ CENTER 상단
├──────────────────────────────────────────┤
│ ActionPanel (나머지 높이, CardLayout)       │ padding 20/32, gap 20
└──────────────────────────────────────────┘
+ 결과 오버레이: JLayeredPane 상위 레이어 (UI-09F/G)
```

- GameShell 안에서 화면이 바뀌어도 **PetSidebar 인스턴스는 유지**하고 `refresh(...)`로 갱신만 한다.
- BattleShell은 몰입을 위해 사이드바를 숨긴다. 펫 상태는 무대 안 이름표로 보여 준다.

---

## 2. 디자인 토큰 (`ui/theme/AppTheme.java`)

### 2.1 색상

| 상수 | HEX | 용도 |
|---|---|---|
| `BG` | `#FFF6E9` | 앱 배경 |
| `SURFACE` | `#FFFFFF` | 카드, 상단바, 사이드바 |
| `SUNKEN` | `#FBEFD9` | 보조 면(목록 항목, 바 배경, 칩) |
| `TRACK` | `#F3EADB` | 진행 바 빈 부분 |
| `LINE` | `#EBDDC5` | 테두리(2px) |
| `SHADOW` | `#E6D5B8` | 오프셋 그림자 |
| `INK` | `#2B2440` | 본문, 다크 버튼, 레벨 배지 |
| `INK_DARK` | `#120E20` | 다크 버튼 립 |
| `INK_2` | `#6E6787` | 보조 텍스트 |
| `INK_3` | `#A49DB5` | 힌트, 비활성 텍스트 |
| `BRAND` | `#FF8A00` | 주요 버튼, 선택 상태, 현재 위치 |
| `BRAND_DARK` | `#D96F00` | 주요 버튼 립, 선택 탭 글자 |
| `BRAND_SOFT` | `#FFF0DC` | 선택 탭 배경, 포커스 링 |
| `EXP` / `EXP_DARK` | `#FFC233` / `#E09A00` | 경험치 바 / 바 립·EXP 숫자 |
| `OK` / `OK_DARK` / `OK_SOFT` | `#12B76A` / `#0B8A4F` / `#E3F8EE` | 정답 |
| `ERR` / `ERR_DARK` / `ERR_SOFT` | `#F04438` / `#C42B20` / `#FDE8E7` | 오답, 피해 |
| `DISABLED_BG` / `DISABLED_LIP` | `#EFE6D6` / `#E1D4BE` | 비활성 버튼 |

### 2.2 능력치·분야 색

| 분야 (`Category`) | 이름 | 능력치 | 메인색 | 연한색 |
|---|---|---|---|---|
| `SCIENCE` | 과학 | INT | `#3D8BFD` | `#E4EFFF` |
| `COMMON` | 상식 | INT | `#06B6D4` | `#DDF6FA` |
| `SPORTS` | 스포츠 | STR | `#E8467C` | `#FDE4EC` |
| `LANGUAGE` | 언어 | FOC | `#8B5CF6` | `#EEE7FF` |
| `HISTORY` | 역사 | WIS | `#10A37F` | `#DFF5EE` |

능력치 칩 색: `INT #3D8BFD`, `STR #E8467C`, `FOC #8B5CF6`, `WIS #10A37F`. 한글 이름: INT 지성, STR 체력, FOC 집중, WIS 지혜.
구현: `Map<Category, Color>`와 `Map<String statType, Color>`를 `AppTheme`에 둔다.

### 2.3 타이포그래피

폰트 파일(OFL 무료)을 `src/main/resources/fonts/`에 둔다: `Jua-Regular.ttf`, `NotoSansKR-Regular.ttf`, `NotoSansKR-Bold.ttf`. 프로젝트에 아직 없음 — Google Fonts에서 TTF를 받아 추가. 로딩은 `Font.createFont(Font.TRUETYPE_FONT, file)` 후 `GraphicsEnvironment.registerFont`, 실패 시 `Font.DIALOG`로 대체.

| 토큰 | 폰트 | 크기 | 용도 |
|---|---|---|---|
| `DISPLAY` | Jua | 44 (히어로 로고 72) | 큰 제목, 카드 제목 |
| `H1` | Jua | 32 | 화면 제목 |
| `QUESTION` | Jua | 34 (피드백 중 28) | 퀴즈 문제 |
| `H2` | Jua | 22 (카드 안 18~19) | 섹션 제목 |
| `NUMBER` | Jua | 17~48 | 수치(능력치 값, KPI, 정답률) |
| `BUTTON` | Noto Bold | 16 (sm 14, lg 18) | 버튼 |
| `BODY` | Noto Regular | 15 (학습 카드 본문 21) | 본문 |
| `CAPTION` | Noto Bold | 12~13 | 라벨, 메타 정보 |

### 2.4 간격·모서리·크기

| 토큰 | 값 |
|---|---|
| 간격 스케일 | 4, 8, 12, 16, 20, 28, 32, 56 |
| 모서리 | 칩·pill 999(완전 둥글게), 입력·버튼 14, 작은 버튼 12, 카드 20, 대형 카드(학습 카드·문제 박스) 26 |
| 테두리 | 2px `LINE` (선택 강조는 3px) |
| 그림자 | y+4px `SHADOW` (대형 카드 y+6, 버튼은 립 색) |
| `TOPBAR_H` | 64 |
| `SIDEBAR_W` | 320 |
| `HERO_W` | 560 |
| `ARENA_H` | 470 |
| 버튼 높이 | 기본 52, sm 40, lg 60 |
| 입력 높이 | 52 |

---

## 3. 공통 컴포넌트 (`ui/component/`)

각 컴포넌트는 `JComponent`/`JPanel`을 상속하고 `paintComponent`에서 직접 그린다.

### 3.1 `RoundedPanel`
- 속성: `fill`, `border`, `shadow`, `radius`, `shadowY`(기본 4).
- 그리기 순서: ① `shadow`로 `fillRoundRect(0, shadowY, w, h-shadowY)` ② `fill`로 같은 크기 y=0 ③ `BasicStroke(2f)`로 테두리.
- `setOpaque(false)`. 내부 여백은 `EmptyBorder`.

### 3.2 `GameButton extends JButton`
- `Variant`: `PRIMARY`(BRAND/BRAND_DARK, 흰 글자), `DARK`(INK/INK_DARK), `OK`(OK/OK_DARK), `GHOST`(흰 면 + LINE 테두리 + SHADOW 립, INK 글자), 비활성 시 `DISABLED_BG/DISABLED_LIP` + `INK_3` 글자.
- `Size`: `SM`(40), `MD`(52), `LG`(60).
- `setContentAreaFilled(false)`, `setBorderPainted(false)`, `setFocusPainted(false)`.
- 립: 아래 4px 진한 색. 눌림(`isPressed`) 시 립 1px + 글자 y+3. 호버 시 면 색을 약간 밝게.

### 3.3 `OptionButton extends JToggleButton`
- 번호 배지(46×46, 반경 14) + 텍스트(22px). 긴 텍스트는 HTML 라벨로 2줄까지.
- `State`: `NORMAL`(흰 면/LINE), `SELECTED`(테두리 BRAND 3px, 면 `#FFF7EB`, 배지 BRAND), `RIGHT`(OK/OK_SOFT, 배지 OK, 오른쪽 "정답" pill), `WRONG`(ERR/ERR_SOFT, 배지 ERR), `DIMMED`(NORMAL + 투명도 0.55 — 피드백 시 나머지 보기).
- 키보드: 숫자 1~4로 선택, Enter로 제출.

### 3.4 `OXButton extends JToggleButton`
- 큰 기호(Jua 150px, O는 INT 파랑, X는 ERR 빨강) + 캡션("맞다 · 키보드 O"). 선택 시 테두리 3px 기호색, 연한 배경.

### 3.5 `ProgressBar`
- EXP·능력치·정답률·HP 공용. 속성: `value`, `max`, `color`, `trackColor`, `height`, `gainValue`(선택).
- `gainValue`가 있으면 끝에서 gain만큼을 금색 사선 줄무늬로 표시(획득분 강조).
- `animateTo(int)` : `javax.swing.Timer`(16ms)로 0.6초 동안 보간.
- HP 바: 앞쪽 실제 HP(보스 ERR, 펫 OK) + 뒤쪽 "잃은 HP"(`#FFC2BC`)가 0.5초 뒤 줄어듦.

### 3.6 `SegmentProgress`
- 전체 칸 수 N, 각 칸 높이 8, 간격 6. 칸 상태: 빈칸(TRACK), 완료(분야색 또는 OK/ERR), 현재(BRAND).

### 3.7 `Chip`, `LevelBadge`
- `Chip`: 높이 ~28, 반경 999, 기본 SUNKEN/INK_2. 분야 칩은 연한색 배경 + 메인색 글자 + 아이콘.
- `LevelBadge`: INK 배경, 흰 글자 Jua 16 "Lv.N", 반경 10. 큰 버전 Jua 22.

### 3.8 `TopBar`
- 왼쪽: 로고(36×36 주황 둥근 사각 + 발바닥 아이콘, "QuizPet" Jua 26), 폭 272.
- 가운데: 탭 5개 `JToggleButton` + `ButtonGroup`: **홈 / 학습하기 / 학습 기록 / 오답노트 / 보스 결투**. 선택 탭: BRAND_SOFT 배경, BRAND_DARK 글자. 높이 40, 반경 12.
- 오른쪽: 사용자 칩(펫 아바타 30px + 닉네임 + "사용자 변경"). 클릭 시 UI-01.
- 퀴즈·결투 진행 중에는 탭 클릭 시 "진행 중인 퀴즈를 종료할까요?" 확인 후 이동.

### 3.9 `PetSidebar`
위에서 아래로 (padding 20, gap 14):
1. **PetStage** (높이 276, 반경 20): 크림색 세로 그라디언트(`#FFF3DE`→`#FFE7C2`), 바닥 띠 58px `#F7D9A8`, 그림자 타원, 펫 이미지 196px, 왼쪽 위 기분 태그, 오른쪽 위 말풍선(흰 면, INK 2px 테두리, 꼬리).
2. 이름 행: 펫 이름(Jua 24) + 외형 이름(12, INK_3) + 오른쪽 `LevelBadge`.
3. EXP: `ProgressBar`(높이 14, EXP 색) + 아래 "EXP {xpInLevel} / 50", 오른쪽 "다음 레벨까지 {50 - xpInLevel}" 또는 획득 시 "+{n} EXP"(OK_DARK).
4. "능력치" 라벨 + 4행: [능력치 칩 54px][한글 이름][바][값 Jua 17]. 증가 시 값 옆에 "+n"(OK, 1.5초 후 사라짐).
5. 하단(`margin-top: auto`) 미니 카드: 기본은 "누적 정답률 / 푼 문제", 퀴즈 중에는 "이번 퀴즈 n / m 정답" + 진행 점 5개(정답 OK, 오답 ERR, 현재 BRAND 테두리, 남은 칸 흰색).

공개 메서드(제안):
```java
void refresh(Pet pet);                              // 이름·레벨·EXP·능력치
void setMood(PetMood mood, String speech);          // 이미지 교체 + 말풍선
void showGain(int earnedXp, String statType, int statGained); // 획득 애니메이션
void showSession(List<Boolean> answers, int total); // null이면 기본 미니 카드
```

데이터 바인딩: `Pet.getName()`, `Pet.getAppearance().getDisplayName()`, `Pet.getLevel()`, `Pet.getXpInLevel()`, `Pet.XP_PER_LEVEL`, `Pet.getStats().get("INT"|"STR"|"FOC"|"WIS")`, 정답 보상 후에는 `GrowthResult`(`getEarnedXp`, `getStatType`, `getStatGained`, `isLevelUp`, `getLevelBefore/After`).
능력치 바의 최대값: 30 `[TBD]` (넘으면 꽉 찬 상태로 표시).

---

## 4. 펫 기분(Mood) 규칙

`enum PetMood { IDLE, FOCUS, HAPPY, SAD, LEVEL_UP }`

| Mood | 언제 | 기분 태그 | 말풍선 예시 |
|---|---|---|---|
| `IDLE` | 홈, 분야 선택, 기록 화면 | 평온 | "오늘은 어떤 걸 배워볼까?" |
| `FOCUS` | 학습 카드, 문제 표시~제출 전, 오답노트, 보스 선택 | 집중 | "두근두근… 할 수 있어!" |
| `HAPPY` | 정답 피드백 | 신남 | "정답이야! 머리가 똑똑해진 느낌!" |
| `SAD` | 오답 피드백, 결투 패배·피격 | 아쉬움 | "으앙… 카드를 한 번 더 볼까?" |
| `LEVEL_UP` | 결과 화면에서 레벨이 오른 경우, 결투 승리 | 레벨 업 | "레벨 업!! 고마워!" |

전이: 문제 진입 → FOCUS, 정답 제출 → HAPPY, 오답 제출 → SAD(다음 문제 진입 시 FOCUS 복귀), 퀴즈 종료 → `isLevelUp`이면 LEVEL_UP 아니면 HAPPY(정답률 ≥ 60%) / IDLE.
말풍선 문구는 `PetMood`별 문자열 배열에서 무작위 선택해도 된다.

---

## 5. 이미지 에셋

모두 배경 투명 PNG. 512px 원본은 큰 화면용, `small/`의 256px은 사이드바·목록용.

### 5.1 펫 (15장 × 2크기)
```
src/main/resources/images/pets/{appearance}_{mood}.png        (512×512)
src/main/resources/images/pets/small/{appearance}_{mood}.png  (256×256)
appearance ∈ duck | turtle | lizard        (= PetAppearance.name().toLowerCase())
mood       ∈ idle | focus | happy | sad | levelup   (= PetMood, LEVEL_UP → levelup)
```
> 주의: 현재 `PetAppearance.getImagePath()`는 `images/pets/duck.png`처럼 기분 없는 경로를 반환하며, 이 파일은 존재하지 않는다. 기분별 경로를 만드는 메서드(예: `getImagePath(PetMood)`)를 추가하거나 UI에서 위 규칙으로 경로를 조합한다.

### 5.2 보스 (16장 × 2크기)
```
src/main/resources/images/bosses/{boss}_{state}.png        (512×512)
src/main/resources/images/bosses/small/{boss}_{state}.png  (256×256)
boss  ∈ slime | ghost | golem | dragon
state ∈ idle | attack | hurt | defeated
```

### 5.3 로딩 방법
README의 컴파일 명령은 `src/main/resources`를 클래스패스에 넣지 않는다. 따라서 `getResource()` 대신 **프로젝트 루트 기준 파일 경로**로 읽는다 (`data/`와 같은 방식).
```java
Path p = Path.of("src/main/resources/images/pets/small", key + "_" + mood + ".png");
BufferedImage img = ImageIO.read(p.toFile());  // 한 번 읽어 Map<String, BufferedImage>에 캐시
g2.drawImage(img, x, y, size, size, null);     // RenderingHints.KEY_INTERPOLATION = BICUBIC
```
파일이 없으면 기존 `UiStyle.createPetBox()` 방식(색 박스 + 이름)으로 대체해 앱이 멈추지 않게 한다.

### 5.4 참고 이미지
- `docs/design/screens/pet-sheet.png` (펫 3×5)
- `docs/design/screens/boss-sheet.png` (보스 4×4)

---

## 6. 화면별 명세

표기: `[컴포넌트]`, `{데이터}`, `→` 이동. 캡처 파일은 `docs/design/screens/{ID}.png`.

### UI-01 시작·사용자 선택 (AuthShell)
- **Hero**(BRAND 배경): 로고 "QuizPet"(Jua 72, 아래 BRAND_DARK 5px 그림자), 부제 "퀴즈로 성장하는 나만의 캐릭터", 분야 5개 반투명 pill, 하단 지면 띠(`#FFB44D`, 높이 150) 위에 펫 3마리(turtle idle 150 / duck happy 190 / lizard idle 150).
- **Form**: H1 "다시 만나서 반가워요!" → 사용자 목록 `JList` + `UserRow` 렌더러(아바타 52, 닉네임, "{펫이름} · {외형}", 오른쪽 LevelBadge, 선택 행은 BRAND 테두리 + [입장] sm 버튼) → 구분선 "처음이신가요?" → 입력 → [새 사용자 만들기] DARK LG 전체 폭.
- 동작: 행 더블클릭 또는 [입장] → `AppController.selectUser(userId)`. [새 사용자 만들기] → `createUser(nickname)` → UI-02.
- 입력이 비면 버튼 비활성.
- **시안과 API 차이**: 시안에는 "사용자 ID + 닉네임" 두 입력이 있지만 현재 API는 `createUser(String nickname)`만 받는다. **닉네임 입력 하나만 구현**하고 ID 칸은 만들지 않는다(팀이 ID 입력을 확정하면 추가).

### UI-02 캐릭터 생성 (AuthShell)
- **Hero**: 선택한 외형의 테마색 배경(duck `#FFC93C`, turtle `#8EDB9B`, lizard `#F7A28B` 계열), "STEP 2 / 2" 칩, "새 친구를 만나볼까요?"(Jua 44), 하단에 이름표(흰 면, INK 3px 테두리, Jua 24) + 펫 happy 270px. 이름을 입력하면 이름표가 즉시 바뀐다(`DocumentListener`).
- **Form**: "캐릭터 이름" 입력(오른쪽 "{n} / 10" 카운터) → "외형 선택" `JToggleButton` 3개(`ButtonGroup`, 카드 안에 이미지 110 + 이름 + "오리 · 노랑" 설명, 선택 시 BRAND 테두리 + 오른쪽 위 체크 원) → "초기 상태" 칩(Lv.1, EXP 0, INT/STR/FOC/WIS 10) → [이전] GHOST LG + [이 친구로 시작하기] PRIMARY LG.
- 동작: `createPet(name, appearance.name())` → UI-03.

### UI-03 메인(홈) (GameShell, 탭=홈, Mood=IDLE)
1. 다크 배너 카드(INK 배경): "{닉네임} 님, 안녕하세요!"(EXP 색 14) / "{펫} 이 Lv.{L+1}까지 {50-xpInLevel} EXP 남았어요"(H1, 숫자 EXP 색) / 보상 규칙 안내 / 오른쪽 [학습 시작하기 →] PRIMARY LG → UI-04.
2. "분야별 진행도" + 5열 카드: 아이콘 박스 48(연한 분야색), 분야명 + 능력치, 진행 바(정답률, 분야색), "{정답률}% · {풀이 수}문제" 또는 "아직 시작 전". 데이터: `StatisticsService.getCategoryStatistics(userId, categoryId)`.
3. 하단 2열 (1.4 : 1): "최근 학습" 목록 3개(O/X 배지, 분야, 문제, "+10 EXP" 또는 "—"), "전체 기록 →" → UI-08A / "복습이 필요해요" 카드(오답 수, 분야별 칩, [오답노트 열기]) → UI-08B(P1, 미구현이면 카드 숨김).

### UI-04 학습 분야 선택 (GameShell, 탭=학습하기, Mood=IDLE)
- H1 "무엇을 배워볼까요?" + 부제.
- `GridLayout(2, 3, 20, 20)` 카드 5개 + 6번째 칸 "랜덤 도전"(점선 테두리, 선택 구현 — 미구현 시 빈 패널).
- 카테고리 카드: 아이콘 박스 72(반경 20), 분야명 Jua 28 + 능력치 칩, 설명(`Category.getDescription()`), 하단 진행 바 + "정답률 n%" 또는 "NEW". 오른쪽 위 장식 원(연한 분야색).
- 호버: 테두리·그림자를 분야색으로, y −3px. 클릭 → `selectCategory(categoryId)` → UI-05.
- 콘텐츠 없는 분야: 회색 처리 + "준비 중", 클릭 불가.

### UI-05 학습 카드 (GameShell, 탭=학습하기, Mood=FOCUS)
- 상단 행: 분야 칩 + "학습 카드" + 오른쪽 "{index+1} / {size}"(Jua 22). 아래 `SegmentProgress`(N=카드 수).
- 대형 카드(반경 26, 그림자 y+6): 왼쪽 10px 분야색 띠, 태그 칩들 + "난이도 ★☆☆", 제목 Jua 44, 본문 21px 행간 1.7 (`JTextArea` lineWrap, 편집 불가, 투명), 하단 안내 박스("이 카드와 연결된 문제가 출제될 수 있어요").
- 하단 행: [← 이전](첫 카드에서 비활성) [다음 →] … [분야 선택으로] GHOST, [퀴즈 시작] PRIMARY LG → `startQuiz()` → UI-06.
- 키보드: ←/→ 카드 이동, Enter 퀴즈 시작.
- 데이터: `LearningService.getContents(categoryId)`, `LearningContent` 제목·본문·태그·난이도.

### UI-06A 퀴즈 · OX (GameShell, Mood=FOCUS)
- 헤더: 분야 칩 + 유형 칩 "OX" … `SegmentProgress`(220px, 5칸: 완료 OK/ERR, 현재 BRAND) + "{n} / {total}" + [종료] GHOST SM.
- 문제 박스(반경 26): "Q{n}."(BRAND_DARK Jua 20) + 문제(QUESTION).
- `OXButton` 2개, 남은 높이를 반씩 채움(gap 24).
- 하단: 안내 문구 + [답안 제출] LG (선택 전 비활성).
- 사이드바 미니 카드: 이번 퀴즈 진행 점.

### UI-06B 퀴즈 · 4지선다 (GameShell, Mood=FOCUS)
- 헤더·문제 박스는 06A와 동일.
- `OptionButton` 4개 2×2 `GridLayout(2,2,16,16)`, 남은 높이를 채움.
- 하단: "선택한 답: {n}번 · {텍스트}"(BRAND_DARK) + [답안 제출] PRIMARY LG.
- 제출 → `submitAnswer(answer)` → 반환된 `LearningRecord`/세션 상태로 06C 또는 06D.

### UI-06C 정답 피드백 (Mood=HAPPY)
- 문제 박스 축소(Q 텍스트 28), 보기: 정답 `RIGHT`, 나머지 `DIMMED`. OX는 정답 버튼만 OK 스타일.
- 피드백 패널(남은 높이): OK_SOFT 면, OK 3px 테두리, 왼쪽 배지 64(OK, "O" Jua 40), "정답입니다!"(Jua 28 OK_DARK), "해설 · {explanation}", 보상 칩 "⭐ EXP +10", "{STAT} +1".
- 이미 보상받은 문제면 보상 칩 대신 회색 칩 "이미 보상을 받은 문제예요".
- 사이드바: `showGain(...)` — EXP 바 애니메이션 + 능력치 "+1".
- 하단: [학습 카드 보기] GHOST(연결 카드 없으면 비활성) → `showFeedbackCard()`, [다음 문제 →] OK LG → `nextQuestion()`. 마지막 문제면 버튼 문구 "결과 보기".

### UI-06D 오답 피드백 (Mood=SAD)
- 내가 고른 보기 `WRONG`(캡션 "내가 고른 답"), 정답 `RIGHT`(캡션 "정답"), 나머지 `DIMMED`.
- 피드백 패널: ERR_SOFT/ERR, 배지 "X", "아쉬워요, 정답은 {정답}", 해설, 칩 "📒 오답노트에 저장됐어요"(P1 미구현이면 칩 생략).
- 하단: [학습 카드 다시 보기] GHOST, [다음 문제 →] DARK LG.

### UI-07 퀴즈 결과 (Mood=LEVEL_UP 또는 HAPPY/IDLE)
- 상단: "퀴즈 완료!"(H1) + "{분야} · {total}문제", 오른쪽 분야 칩.
- 2열(280px : 나머지):
  - 정답률 링: 지름 168, 두께 19 (`fillArc` 2회 + 가운데 흰 원 130), 60% 이상 OK, 미만 BRAND. 가운데 "{accuracy}%"(Jua 40) + "{correct} / {answered} 정답".
  - 오른쪽 위: 레벨 업 배너(INK 배경, 금색 별 박스, "LEVEL UP", "Lv.{before} → Lv.{after}") — 레벨이 안 올랐으면 "다음 레벨까지 {n} EXP" 카드로 대체.
  - 오른쪽 아래 KPI 3개: 획득 EXP(EXP_DARK), 능력치 변화, 신규 정답 수.
- "문제별 결과" 목록(스크롤): O/X 배지, Q번호, 문제, "+10 EXP" 또는 "보상 없음". 클릭 시 해설 다이얼로그(선택).
- 하단: [학습 기록 보기] GHOST, [같은 분야 다시] GHOST, [메인으로] PRIMARY LG.
- 데이터: `QuizSession.getResult()` → `QuizResult`(`getTotalCount`, `getAnsweredCount`, `getCorrectCount`, `getAccuracy`), 보상은 세션 기록 기준.

### UI-08A 학습 기록 (GameShell, 탭=학습 기록, Mood=IDLE)
- H1 + 오른쪽 세그먼트 필터(전체 + 분야 5개, `JToggleButton` 그룹, SUNKEN 트랙, 선택 칸 흰색).
- KPI 4개: 총 풀이 수, 전체 정답률(OK_DARK), 누적 EXP(EXP_DARK), 오답노트 수(ERR).
- 2열(1 : 1.5): "분야별 정답률" 막대 5행(분야명 80 / 바 높이 18 / % Jua 18, 기록 없으면 "—") / "풀이 이력" `JTable`(열: 일시, 분야 pill, 문제, 결과 "정답"/"오답", EXP "+10"·"재정답"·"—"; 행 높이 44, 헤더 배경 없음, 줄바꿈 없음, 최신순).
- 필터 변경 → `showHistory(categoryId)` / `StatisticsService` 재조회.

### UI-08B 오답노트 (P1) (GameShell, 탭=오답노트, Mood=FOCUS)
- H1 + "틀린 문제 {n}개 · 선택한 문제로 복습 퀴즈를 만들 수 있어요" + 세그먼트 필터.
- `GridLayout(1,2,20,0)`: 왼쪽 목록(체크박스 22px 둥근 사각 + 문제 + "{분야} · 오답 {n}회" + 능력치 pill, 선택 행 BRAND 테두리, 하단 "{n}개 선택됨" + [복습 퀴즈 시작] PRIMARY) / 오른쪽 상세(분야·유형 칩, 문제 Jua 28, 보기 2×2 작은 `OptionButton`(WRONG/RIGHT/DIMMED), 해설 박스, [관련 학습 카드] GHOST SM, [이 문제만 다시 풀기] DARK SM).
- 복습 퀴즈의 재정답은 기록되지만 보상 없음(기존 최초 정답 규칙).

---

## 7. 보스 결투 (UI-09) — P2

### 7.1 진행 규칙 (UI가 가정하는 흐름)
```
보스 선택 → [턴 시작]
  → 스킬 4개 중 1개 선택
  → 스킬에 연결된 분야에서 문제 1개 출제 (기존 Question 재사용)
  → 정답: 펫이 스킬 피해만큼 보스 공격 (약점이면 ×1.5)
    오답: 스킬 빗나감 + 보스 반격으로 펫 HP 감소
  → 해설 표시 → [다음 턴]
  → 보스 HP 0 = 승리 / 펫 HP 0 = 패배
```

| 스킬 | 능력치 | 출제 분야 | 피해 |
|---|---|---|---|
| 지식 폭발 | INT | 과학·상식 | `10 + INT` `[TBD]` |
| 몸통 박치기 | STR | 스포츠 | `10 + STR` `[TBD]` |
| 집중 레이저 | FOC | 언어 | `10 + FOC` `[TBD]` |
| 지혜의 일격 | WIS | 역사 | `10 + WIS` `[TBD]` |

- 약점 배율 ×1.5, 소수점 버림 `[TBD]`.
- 펫 최대 HP `100 + Lv × 10` `[TBD]`. 보스 반격 피해: 보스별 고정값(예: 까먹귀신 18) `[TBD]`.
- 승리 보상 EXP는 **최초 승리 1회만** 지급, 재도전 승리는 기록만 `[TBD]`.
- 패배해도 EXP·능력치 감소 없음.
- 결투에서 틀린 문제도 일반 퀴즈처럼 기록·오답노트에 저장.
- 결투 상태는 메모리에만 유지(퀴즈 세션과 동일). 결과 저장 형식(`battle.txt`)은 BE 결정 사항.

### 7.2 보스 데이터 `[TBD — 수치 확정 필요]`

| key | 이름 | 권장 Lv | HP | 약점 | 보상 EXP | 설명 |
|---|---|---|---|---|---|---|
| `SLIME` | 졸음 슬라임 | 1 | 120 | FOC | 30 | 공부만 하면 졸음이 쏟아지게 만든다 |
| `GHOST` | 까먹귀신 | 3 | 200 | WIS | 50 | 외운 내용을 슬쩍 지워 버린다 |
| `GOLEM` | 헷갈골렘 | 5 | 300 | INT | 80 | 비슷한 보기로 머리를 헷갈리게 한다 |
| `DRAGON` | 망각의 용 | 8 | 450 | STR | 120 | 모든 기억을 불태우는 최종 보스 |

보스 카드 테마 그라디언트(위→아래): SLIME `#E3F8F1→#B8EBDC`, GHOST `#EFE9FF→#D3C6F7`, GOLEM `#EEF0F3→#CDD2DA`, DRAGON `#F1E8FF→#D7C4F8`.

### 7.3 보스 상태 이미지 매핑
| 상황 | 보스 state | 펫 mood |
|---|---|---|
| 스킬 선택, 문제 풀이 중 | `idle` | `FOCUS` |
| 정답 → 공격 성공 | `hurt` | `HAPPY` |
| 오답 → 보스 반격 | `attack` | `SAD` |
| 승리 | `defeated` | `LEVEL_UP` |
| 패배 | `attack` | `SAD` |

### UI-09A 보스 선택 (GameShell, 탭=보스 결투, Mood=FOCUS)
- H1 "보스 결투" + 부제, 오른쪽 안내 카드: ①스킬 선택 ②연결 분야 문제 Ⓞ공격 성공 Ⓧ보스 반격.
- 4열 `BossCard`: 위 아트 영역 236px(테마 그라디언트 + 보스 200px), 아래 정보(이름 Jua 24 + "Lv.{n}+" 배지, 설명 2줄, 칩 "HP n" / "약점 {STAT}"(능력치색) / "EXP +n", 버튼).
- 상태: 클리어(보스 `defeated` 이미지 + 회전된 "CLEAR" 도장, [다시 도전] GHOST) / 도전 가능(BRAND 테두리, [도전하기] PRIMARY) / 잠김(남색 오버레이 α 0.86 + 자물쇠 + "Lv.{n} 이상" + "{n}레벨 더 성장하면 열려요", 버튼 비활성).
- 잠금 조건: `pet.getLevel() < 권장 Lv`.

### UI-09B 결투 · 스킬 선택 (BattleShell)
**ArenaPanel (1280×470, 직접 페인팅)**
- 배경: `GradientPaint` `#3B2F6B`(위) → `#6A57B0`(아래), 작은 흰 점 별 12개(α 0.7), 달(지름 64, `#FFF1C1`, 오른쪽 위), 바닥 띠 높이 120 `#4A3C86`(위 테두리 3px `#5E4FA0`).
- 발판: 보스 타원(x 820, y 262, 340×70), 펫 타원(x 120, y 392, 330×66), 색 `#7D6BC4` + 아래 8px `#54458F`.
- 스프라이트: 보스 300px at (840, 20), 펫 236px at (168, 186).
- "TURN {n}" pill: 가운데 위, 반투명 흰 면, 흰 글자 Jua 18.
- 보스 이름표(왼쪽 위 32,28, 폭 380): 이름 Jua 24 + "BOSS" 배지 + 오른쪽 약점 pill, HP 바(ERR) + "HP {cur} / {max}", 오른쪽에 설명 또는 "-{피해}".
- 펫 이름표(오른쪽 아래, 폭 380): 펫 이름 + Lv + 능력치 4개 작은 글자, HP 바(OK) + "HP {cur} / {max}".

**ActionPanel = 스킬 카드**
- 왼쪽: "어떤 스킬로 공격할까요?" + 안내 + [도망가기] GHOST SM / 스킬 버튼 2×2.
- 스킬 버튼: 왼쪽 8px 능력치색 띠, 능력치 아이콘 박스 46, 스킬명 Jua 21(약점이면 금색 "약점" 태그), "{분야} 문제를 맞히면 발동", 오른쪽 예상 피해(Jua 28 능력치색) + "예상 피해" 또는 "{기본} × 1.5". 호버/선택 시 테두리·립 능력치색.
- 오른쪽 330px: 전투 기록 로그(SUNKEN 면, 최근 4~6줄, "T{n} 말랑이의 {스킬}! {n} 피해" 형식).

### UI-09C 결투 · 문제 풀기
- ActionPanel 왼쪽 430px: 발동 대기 스킬 배너(연한 능력치색, 능력치 pill + 스킬명 + 약점 태그 + 피해), "{분야} · {유형}" 라벨, 문제 Jua 30, [스킬 다시 고르기] GHOST SM(제출 전까지만).
- 오른쪽: `OptionButton` 2×2(높이 자동, 글자 19) 또는 `OXButton` 2개, 하단 안내 "맞히면 {n} 피해, 틀리면 {보스}가 반격해요" + [공격!] PRIMARY.

### UI-09D 결투 · 공격 성공
- 보스 `hurt`, 펫 `HAPPY`. 보스 근처에 피해 숫자 "-{n}"(Jua 72, 흰 글자 + ERR 4px 외곽선 `TextLayout.getOutline`, 0.8초간 40px 상승 + 페이드), 약점이면 금색 "약점!" 배지(−6° 회전).
- 보스 흔들기: x ±6px 6회 왕복(300ms). HP 바 감소 + 잃은 HP 잔상.
- ActionPanel: 정답 피드백 패널(06C 스타일, "정답! {스킬} 명중" + 해설) + 로그 + [다음 턴 →] OK LG.

### UI-09E 결투 · 오답, 보스 반격
- 보스 `attack`, 펫 `SAD`. 무대 전체에 ERR α 0.10~0.15 플래시(120ms), 펫 근처 "-{n}", 펫 흔들기, 펫 HP 바 감소.
- ActionPanel: 오답 피드백 패널(06D 스타일, "빗나갔어요! {보스}의 반격" + 해설) + 로그 + [다음 턴 →] DARK LG.

### UI-09F 결투 · 승리 (오버레이)
- 무대 위에 남색 반투명(`rgba(32,26,52,0.62)`) + 가운데 카드 600px(반경 28): 상단 리본 "승리!"(OK 배경), 펫 levelup 170 + 보스 defeated 150, "{보스}를 물리쳤어요!"(Jua 30), "{턴}턴 · 정답률 {n}% ({correct} / {total})", KPI 3개(보상 EXP / 준 피해 / 남은 HP), [보스 목록] GHOST + [다음 보스 보기] PRIMARY.
- 재도전 승리면 보상 KPI에 "보상 없음(이미 클리어)".

### UI-09G 결투 · 패배 (오버레이)
- 같은 오버레이, 리본 "패배"(INK 배경), 펫 sad 170, "조금만 더 공부하면 이길 수 있어요", "{보스} HP {n} 남음 · {턴}턴 · 정답률 {n}%", 추천 박스: 보스 약점 분야 칩 + 이번 결투 오답 분야 칩(클릭 시 해당 분야 학습 카드로), [다시 도전] GHOST + [학습하러 가기] PRIMARY.

---

## 8. 권장 클래스 구조

```
ui/
├─ theme/AppTheme.java          # §2 토큰, 폰트 로딩
├─ theme/Assets.java            # §5 이미지 로딩·캐시, 대체 이미지
├─ shell/AuthShell.java
├─ shell/GameShell.java         # TopBar + PetSidebar + CardLayout
├─ shell/BattleShell.java       # TopBar + ArenaPanel + ActionPanel + 오버레이
├─ component/
│  ├─ RoundedPanel.java  GameButton.java  OptionButton.java  OXButton.java
│  ├─ ProgressBar.java  SegmentProgress.java  Chip.java  LevelBadge.java
│  ├─ TopBar.java  PetSidebar.java  PetStage.java
│  ├─ CategoryCard.java  BossCard.java  SkillButton.java
│  ├─ AnswerFeedbackPanel.java  (기존 파일 — 06C/06D 스타일로 수정)
│  └─ DamagePopup.java
├─ screen/  (기존 패널 유지·리디자인)  + BossSelectPanel.java, BattlePanel.java
└─ model/PetMood.java  BossState.java
```
기존 `UiStyle`은 `AppTheme`로 대체하되, 기존 화면이 모두 옮겨질 때까지 남겨 둔다.

## 9. 애니메이션 요약

| 효과 | 구현 | 시간 |
|---|---|---|
| EXP/HP 바 변화 | `Timer` 16ms 선형 보간 | 600ms |
| 잃은 HP 잔상 | 지연 후 보간 | 500ms 지연 |
| 능력치 "+n" | 표시 후 제거 | 1500ms |
| 피해 숫자 | 위로 40px + `AlphaComposite` 페이드 | 800ms |
| 스프라이트 흔들기 | x ±6px 6회 | 300ms |
| 피격 플래시 | 무대 전체 ERR α 0.12 | 120ms |
| 카드 호버 | y −3px, 테두리색 변경 | 즉시 |

애니메이션 중에도 버튼 입력은 막지 않되, 같은 답의 중복 제출은 기존 `QuizSession` 규칙에 맡긴다.

## 10. 완료 기준 체크리스트

- [ ] 1280×800에서 모든 화면이 잘림·스크롤바 없이 보인다 (목록·표 내부 스크롤은 허용).
- [ ] 창을 키우면 콘텐츠 영역만 늘어나고 사이드바(320)·상단바(64)는 고정이다.
- [ ] 색·폰트·크기 리터럴이 `AppTheme` 밖에 없다.
- [ ] 정답/오답/레벨 업/결투 상황마다 펫 이미지와 말풍선이 §4·§7.3 표대로 바뀐다.
- [ ] 이미지 파일이 없어도 앱이 대체 표시로 계속 동작한다.
- [ ] 폰트 로딩 실패 시 `Font.DIALOG`로 동작한다.
- [ ] 기존 테스트가 모두 통과한다 (README "로컬 테스트" 명령).
- [ ] `[TBD]` 수치는 상수 한 곳에 모여 있다.
