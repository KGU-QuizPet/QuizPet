# QuizPet 🐾

> **퀴즈로 성장하는 나만의 캐릭터**  
> 지식을 학습하고 문제를 풀어 애완 캐릭터를 키우는 Java Swing 기반 데스크톱 게임

**프로젝트 유형:** 객체지향 프로그래밍 팀 프로젝트  
**개발 기간:** 5주  
**팀 구성:** 프론트엔드(Swing UI) 3명 · 백엔드(내부 로직·데이터) 3명 · 기획/발표 1명
**기준 문서:** [PRD](docs/PRD.md) · [기능명세서](docs/FUNCTIONAL_SPECIFICATION.md)

> [!NOTE]
> 이 README는 **개발 계획 및 권장 코드베이스 구조**를 설명합니다. 아래의 화면·클래스·폴더는 구현 목표를 포함하며, 모든 기능이 이미 완성되었다는 뜻은 아닙니다. `QuizPet`은 현재 문서상의 프로젝트명으로 표기합니다.

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [핵심 기능](#핵심-기능)
- [학습 분야와 성장 규칙](#학습-분야와-성장-규칙)
- [기술 스택](#기술-스택)
- [추천 프로젝트 폴더 구조](#추천-프로젝트-폴더-구조)
- [프로그램 구조와 데이터 흐름](#프로그램-구조와-데이터-흐름)
- [화면 구성](#화면-구성)
- [데이터 파일](#데이터-파일)
- [실행 방법](#실행-방법)
- [팀원별 역할 분담](#팀원별-역할-분담)
- [5주 개발 계획](#5주-개발-계획)
- [협업 규칙](#협업-규칙)
- [완료 기준](#완료-기준)

## 프로젝트 소개

**QuizPet**은 사용자가 다양한 분야의 학습 카드를 읽고 **OX·4지선다 퀴즈**에 도전하여 자신의 캐릭터를 성장시키는 교육형 육성 게임입니다. 퀴즈 정답 여부는 경험치 및 능력치 성장과 연결되고, 사용자는 학습 기록을 통해 자신의 성취도를 확인할 수 있습니다.

**핵심 플레이 흐름**

```text
사용자 선택/생성
      ↓
캐릭터 생성 또는 불러오기
      ↓
학습 분야 선택
      ↓
학습 카드 열람
      ↓
OX · 4지선다 퀴즈
      ↓
정답·해설 확인
      ↓
경험치·능력치 성장 및 학습 기록 저장
      ↓
메인 화면에서 성장 확인 → 다시 학습
```

## 핵심 기능

| 우선순위 | 기능 | 내용 |
| --- | --- | --- |
| **P0 · 필수** | 사용자 관리 | 신규 사용자 생성, 기존 사용자 선택 |
| **P0 · 필수** | 캐릭터 육성 | 이름·외형(2~3종) 선택, 레벨·경험치·능력치 표시 |
| **P0 · 필수** | 학습 콘텐츠 | 4개 분야의 학습 카드 조회 |
| **P0 · 필수** | 퀴즈 | OX, 4지선다, 정답 판별 및 해설 |
| **P0 · 필수** | 성장 보상 | 최초 정답에 대한 경험치·분야별 능력치 지급 |
| **P0 · 필수** | 학습 기록 | 풀이 이력, 정답률, 분야별 학습 통계 |
| **P0 · 필수** | 로컬 저장 | TXT 파일을 통한 사용자·캐릭터·학습 기록 저장/불러오기 |
| **P1 · 확장** | 오답노트 | 틀린 문제 조회 및 재도전 |
| **P1 · 확장** | 단답형·진화 | 단답형 퀴즈, 캐릭터 외형 성장 |
| **P2 · 검토 중** | 결투·랭킹 | 동일 PC 캐릭터 간 턴제 결투, 로컬 순위 비교 |

**이번 개발 범위에서 제외:** 온라인 실시간 PvP, 별도 웹 서버, 원격 데이터 동기화

## 학습 분야와 성장 규칙

### 분야별 능력치

| 학습 분야 | 연결 능력치 | 설명 |
| --- | --- | --- |
| **과학&상식** | 지성 `INT` | 과학 원리와 일반 상식 |
| **스포츠** | 체력 `STR` | 경기 규칙과 스포츠 용어 |
| **역사** | 지혜 `WIS` | 역사적 사건과 인물 |
| **언어(영어)** | 집중력 `FOC` | 영어 단어 및 기초 언어 지식 |

### MVP 보상 규칙

- 신규 캐릭터는 **레벨 1, 누적 경험치 0**, 네 능력치는 **각 10**으로 시작합니다.
- 사용자가 **해당 문제를 처음으로 정답 처리**하면 **경험치 +10**, 연결된 **능력치 +1**을 얻습니다.
- 오답 또는 **이미 맞힌 문제의 재정답**은 학습 기록에 남지만 추가 성장 보상은 없습니다.
- 레벨은 `1 + (누적 경험치 / 50의 정수 부분)`으로 계산합니다.
- 하나의 퀴즈 세션은 기본 **5문제**, 동일 세션 안에서 문제 ID는 중복 출제되지 않습니다.
- 분야별 정답률은 저장된 **답안 제출 기록**을 기준으로 계산합니다.

> 수치와 밸런스는 PRD의 초기 설정이며 테스트 결과에 따라 조정할 수 있습니다.

## 기술 스택

| 항목 | 기술 |
| --- | --- |
| 개발 언어 | **Java** (JDK 17 이상 권장) |
| 데스크톱 GUI | **Java Swing** (`JFrame`, `JPanel`, `CardLayout` 등) |
| 2D 이미지·그래픽 | `ImageIcon`, Java2D |
| 이벤트 처리 | `ActionListener`, `Swing Timer` |
| 데이터 구조 | Java Collections 및 객체지향 클래스 |
| 로컬 저장 | **UTF-8 TXT** · `java.io`, `java.nio.file` |
| 버전 관리 | Git / GitHub |

**프로그래밍 언어는 Java만 사용합니다.** JSON·SQLite·SQL·HTML·JavaScript·Python을 프로그램 구현에 사용하지 않으며, 이미지와 TXT 데이터는 일반 리소스/데이터 파일로 관리합니다. 외부 UI 라이브러리 없이 **Java 표준 Swing만으로도 구현 가능한 구조**를 우선합니다.

## 추천 프로젝트 폴더 구조

아래는 **권장 구조**입니다. P1·P2 파일은 해당 기능을 실제 구현하기로 확정했을 때 추가합니다.

```text
QuizPet/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/quizpet/
│   │   │       ├── Main.java                    # 프로그램 진입점
│   │   │       │
│   │   │       ├── model/                       # 도메인 객체
│   │   │       │   ├── User.java
│   │   │       │   ├── Pet.java
│   │   │       │   ├── PetStats.java
│   │   │       │   ├── Category.java
│   │   │       │   ├── LearningContent.java
│   │   │       │   ├── Question.java            # 추상 클래스
│   │   │       │   ├── OXQuestion.java
│   │   │       │   ├── MultipleChoiceQuestion.java
│   │   │       │   ├── QuizSession.java
│   │   │       │   ├── QuizResult.java
│   │   │       │   └── LearningRecord.java
│   │   │       │
│   │   │       ├── service/                     # 업무 규칙·게임 로직
│   │   │       │   ├── UserService.java
│   │   │       │   ├── PetService.java
│   │   │       │   ├── LearningService.java
│   │   │       │   ├── QuizService.java
│   │   │       │   ├── GrowthService.java
│   │   │       │   ├── RecordService.java
│   │   │       │   └── StatisticsService.java
│   │   │       │
│   │   │       ├── repository/                  # TXT 파일 읽기·쓰기
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── PetRepository.java
│   │   │       │   ├── CategoryRepository.java
│   │   │       │   ├── ContentRepository.java
│   │   │       │   ├── QuestionRepository.java
│   │   │       │   └── RecordRepository.java
│   │   │       │
│   │   │       ├── ui/                          # Swing UI
│   │   │       │   ├── MainFrame.java           # JFrame / 화면 컨테이너
│   │   │       │   ├── ScreenManager.java       # CardLayout 전환
│   │   │       │   ├── screen/
│   │   │       │   │   ├── StartPanel.java
│   │   │       │   │   ├── PetCreatePanel.java
│   │   │       │   │   ├── HomePanel.java
│   │   │       │   │   ├── CategoryPanel.java
│   │   │       │   │   ├── LearningPanel.java
│   │   │       │   │   ├── QuizPanel.java
│   │   │       │   │   ├── QuizResultPanel.java
│   │   │       │   │   └── HistoryPanel.java
│   │   │       │   └── component/
│   │   │       │       ├── HeaderPanel.java
│   │   │       │       ├── PetDisplayPanel.java
│   │   │       │       ├── StatPanel.java
│   │   │       │       ├── ExpBarPanel.java
│   │   │       │       ├── LearningCardPanel.java
│   │   │       │       ├── OXQuestionPanel.java
│   │   │       │       ├── MultipleChoicePanel.java
│   │   │       │       └── AnswerFeedbackPanel.java
│   │   │       │
│   │   │       └── util/
│   │   │           ├── DataPaths.java           # 데이터 경로 관리
│   │   │           └── FileUtils.java           # 공통 파일 유틸
│   │   │
│   │   └── resources/
│   │       └── images/
│   │           ├── pets/                        # 캐릭터 외형 2~3종
│   │           ├── icons/
│   │           └── backgrounds/
│   │
│   └── test/
│       └── java/com/quizpet/                   # Java 로직 테스트(선택)
│
├── data/
│   ├── content/                               # 프로젝트에 포함되는 기준 데이터
│   │   ├── category.txt
│   │   ├── content.txt
│   │   └── question.txt
│   └── save/                                  # 실행 중 생성되는 사용자 데이터
│       └── .gitkeep
│       # user.txt, pet.txt, record.txt (실행 시 생성)
│
├── docs/
│   ├── PRD.md                                 # 제품 요구사항 정의서 v1.1
│   ├── FUNCTIONAL_SPECIFICATION.md            # 기능명세서 v1.1
│   ├── UI_SPEC.md                             # 화면 설계서(추가 가능)
│   └── DATA_FORMAT.md                         # TXT 데이터 형식(추가 가능)
│
├── .gitignore
└── README.md
```

### 패키지별 책임

| 경로 | 책임 | 주 담당 |
| --- | --- | --- |
| `ui/` | 화면 표시, 클릭/입력 이벤트, 화면 전환 | FE |
| `model/` | 사용자·캐릭터·문제·기록 객체 정의 | 각 도메인 BE 담당 |
| `service/` | 문제 출제, 채점, 성장·통계 계산 | BE |
| `repository/` | TXT 데이터 파싱·저장, 무결성 검사 | BE |
| `util/` | 경로 및 파일 처리 공통 기능 | 협의하여 담당 지정 |
| `resources/images/` | 캐릭터·아이콘·배경 이미지 | FE |
| `data/content/` | 사전 작성하는 기준 콘텐츠 | FE/BE 공동 검수 |
| `data/save/` | 실행 시 생성되는 사용자 데이터 | 애플리케이션이 관리 |

**설계 원칙:** 화면 클래스는 파일을 직접 읽거나 경험치를 계산하지 않습니다. `ui → service → repository` 순서로 호출하고, `model`은 각 계층에서 공유합니다.

## 프로그램 구조와 데이터 흐름

```text
[UI: QuizPanel]         답안 제출
       │
       ▼
[Service: QuizService]  중복 제출 확인 · 채점
       │
       ├──► [GrowthService]  최초 정답 보상 계산
       │
       └──► [RecordService]  학습 기록 생성
                     │
                     ▼
         [Repository: Pet / Record]
                     │
                     ▼
             pet.txt / record.txt
                     │
                     ▼
         [UI: 정답 해설 · 결과 표시]
```

- 정답 및 보상 처리는 **한 번의 답안 제출 흐름**에서 관리합니다.
- 이미 보상을 받은 문제인지 판단할 때는 **사용자 ID + 문제 ID**와 과거 정답 기록을 확인합니다.
- 기록과 캐릭터 데이터를 함께 갱신할 때는 백업·복구 및 정합성 확인으로 **부분 저장·중복 지급**을 방지합니다.
- 프로그램에서 사용하는 퀴즈 세션은 메모리에만 유지하고, 세션이 종료되면 폐기합니다.

## 화면 구성

| 화면 ID | 화면 | 주요 내용 |
| --- | --- | --- |
| UI-01 | 시작 및 사용자 선택 | 신규 사용자 생성 / 기존 사용자 선택 |
| UI-02 | 캐릭터 생성 | 이름 및 외형 선택 |
| UI-03 | 메인 | 캐릭터 레벨·경험치·4개 능력치 조회 |
| UI-04 | 학습 분야 선택 | 4개 학습 분야 선택 |
| UI-05 | 학습 카드 | 개념 카드 읽기, 이전/다음 카드 |
| UI-06 | 퀴즈 진행 | OX·4지선다 문제, 답안 제출, 정답·해설 |
| UI-07 | 퀴즈 결과 | 정답률·경험치·능력치·레벨업 확인 |
| UI-08 | 학습 기록 | 누적 풀이·분야별 정답률, 오답노트(P1) |
| UI-09 | 캐릭터 결투(P2) | 로컬 턴제 결투(구현 여부 검토 중) |

화면별 구현은 독립적인 `JPanel`을 사용하고, `MainFrame`의 `CardLayout`으로 전환합니다.

## 데이터 파일

**파일은 모두 UTF-8 TXT 형식**으로 관리합니다.

| 경로 | 내용 | 초기 데이터 |
| --- | --- | --- |
| `data/content/category.txt` | 학습 분야 ID, 이름, 연결 능력치 | **4개** |
| `data/content/content.txt` | 학습 카드 제목·설명·난이도·태그 | **40개** 목표 |
| `data/content/question.txt` | 문제 유형·보기·정답·해설 | **80문제** 목표 |
| `data/save/user.txt` | 사용자 ID, 닉네임, 생성 일시 | 0개 (테스트 약 10개) |
| `data/save/pet.txt` | 펫 이름·외형·누적 XP·4개 능력치 | 0개 (테스트 약 10개) |
| `data/save/record.txt` | 문제별 풀이·정오·지급 XP·시간 | 0개 (테스트 약 100개) |
| `data/save/battle.txt` | 로컬 결투 이력 | P2 선택 구현 시 생성 |

`Pet`과 `PetStats`는 Java에서는 클래스를 분리하더라도 **`pet.txt` 한 줄에 함께 저장**합니다. TXT 필드 순서 및 줄 구성은 데이터 구성 문서의 규격을 따릅니다.

`category.txt`는 UTF-8로 한 줄에 `categoryId|name|statType|description`을 저장합니다.
4개 분야를 각각 한 번씩 포함하고, 각 필드는 `Category` enum 정의와 일치해야 합니다. 빈 줄은 무시합니다.
`CategoryRepository`는 생성 시 파일을 읽으며, 파일 오류·중복·누락·정의 불일치는 `IOException`으로 전달합니다.
기본 경로는 프로젝트 루트 기준 `data/content/category.txt`이고, `Path` 생성자로 다른 파일을 지정할 수 있습니다.
`findAll()`은 파일 순서의 읽기 전용 목록, `findById(String)`는 `Optional<Category>`를 반환합니다. 찾을 수 없는 ID는 빈 결과입니다.

`content.txt`는 UTF-8로 카드당 2줄을 사용합니다. 첫 줄은 `contentId|categoryId|order|difficulty|title|tags`, 다음 줄은 본문입니다.
태그는 쉼표로 구분하고, 태그가 없으면 마지막 필드를 비워 둡니다. 첫 줄의 필드에는 `|`, 개별 태그에는 `,`를 사용할 수 없습니다.
카드 사이의 빈 줄은 무시하지만 본문은 비어 있을 수 없습니다. 순서는 0 이상, 난이도는 양수이며 ID는 중복될 수 없습니다.
`ContentRepository`는 생성 시 읽고 파일·형식 오류는 `IOException`으로 전달합니다. 형식 오류에는 파일 경로와 카드 시작 줄 번호를 포함합니다.
기본 경로는 `data/content/content.txt`이고 `Path` 생성자로 변경할 수 있습니다. 빈 파일은 빈 목록으로 읽습니다.
`findAll()`은 파일 순서의 목록, `findById(String)`는 `Optional<LearningContent>`, `findByCategory(Category)`는 `order` 오름차순 목록을 반환합니다.
조회 목록은 모두 읽기 전용이며, 포함된 기준 카드는 분야당 1개인 예시 4개입니다. 목표인 40개 콘텐츠는 별도 확충이 필요합니다.

`LearningService`는 기본 생성자로 위 두 Repository를 로딩하거나, 생성자에 기존 Repository를 전달해 사용합니다.
`getCategories()`로 전체 분야를, `getContents(categoryId)`로 해당 분야의 정렬된 카드를 조회합니다. 콘텐츠가 없는 분야는 빈 목록이므로 UI에서 비활성화하거나 빈 상태를 표시할 수 있습니다.
`getContent(categoryId, contentIndex)`는 0부터 시작하는 목록 인덱스로 카드 1개를 조회합니다. 카드의 `order` 값과 목록 인덱스는 별개입니다.
정의되지 않은 분야 ID와 범위 밖 인덱스는 `IllegalArgumentException`으로 거부합니다. 진행 상태는 `contentIndex + 1`과 목록 크기로 표시하며, UI 연결과 연계 문제 확인은 아직 구현하지 않았습니다.

`question.txt`는 UTF-8로 문제당 3줄을 사용합니다. 첫 줄은 `questionId|categoryId|contentId|type|answer|options`, 둘째 줄은 문제, 셋째 줄은 해설입니다.
유형은 `OX` 또는 `MULTIPLE_CHOICE`이며, 정답은 각각 `O`/`X`, `1`~`4`입니다. OX의 마지막 보기 필드는 비우고, 객관식 보기는 비어 있지 않은 4개를 쉼표로 구분합니다.
첫 줄 필드에는 `|`, 개별 보기에는 `,`를 사용할 수 없습니다. 문제·해설은 각각 비어 있지 않은 한 줄이며 `|`를 포함할 수 있습니다. 문제 사이의 빈 줄은 무시합니다.
`QuestionRepository`는 기본 경로 `data/content/question.txt` 또는 `Path` 생성자의 파일을 읽어 `Question` 하위 클래스로 변환합니다.
`findAll()`, `findById(String)`, `findByCategory(Category)`, `findByContentId(String)`로 조회하며 목록은 파일 순서의 읽기 전용입니다. ID 조회는 `Optional<Question>`을 반환하고, 없는 ID와 빈 파일은 빈 결과입니다.
잘못된 문제는 파일 경로·시작 줄 번호를 경고 로그에 남기고 해당 3줄을 제외합니다. 중복 ID는 처음 로딩된 유효 문제를 유지합니다. 줄 누락 시 이후 레코드의 경계를 복구하지 않으므로 반드시 문제당 3줄을 유지해야 합니다.
포함된 문제는 기존 학습 카드에 연결된 분야당 1개인 예시 4개입니다. 목표인 80문제 확충, 임의 파일의 `contentId` 존재·분야 일치 검증, UI 연결은 포함하지 않습니다.

세 Repository 모두 파일 누락·읽기 실패·잘못된 UTF-8을 파일 경로가 포함된 `IOException`으로 전달하고, 원인 예외는 `getCause()`로 확인할 수 있습니다.
`CategoryRepository`와 `ContentRepository`는 잘못된 레코드를 파일 경로·시작 줄 번호와 함께 거부하며, 오류가 발생하면 Repository 생성 전체가 실패합니다. 오류 파일을 덮어쓰거나 빈 데이터로 대체하지 않습니다.

`QuizService`는 기본 생성자로 `QuestionRepository`를 로딩하거나 생성자에 기존 Repository를 전달해 사용합니다.
`startSession(userId, categoryId)`는 고유 세션 ID를 부여하고 선택한 분야에서 중복 없이 무작위 최대 5문제를 담은 `QuizSession`을 반환합니다. 문제가 적으면 그 수만큼 출제하며, 문제가 없거나 사용자·분야 ID가 잘못되면 `IllegalArgumentException`으로 거부합니다.
`submitAnswer(session, selectedAnswer)`는 정오 여부를 반환하고, `nextQuestion(session)`과 `endSession(session)`은 다음 문제 이동과 중도 종료를 처리합니다. 답안 검증·중복 제출 차단·제출 전 이동 차단은 `QuizSession`의 규칙을 따르며, 마지막 제출 시 자동 종료됩니다.
현재 문제·정답·해설·제출 내역·정답 수·전체 수·종료 여부는 반환된 세션에서 조회합니다. 세션은 호출자가 보관하며 서로 독립적입니다. 중도 종료 시 제출 내역은 메모리에 유지되며, UI 연결·풀이 기록 파일 저장·최초 정답 보상 및 XP 집계는 아직 구현하지 않았습니다.

> [!IMPORTANT]
> 사용자별 저장 파일(`data/save/*.txt`)은 개인 실행 데이터이므로 Git에 커밋하지 않습니다. `data/content/*.txt`는 팀이 함께 검토하고 Git으로 관리합니다. 저장 경로는 애플리케이션의 실행 작업 디렉터리에 영향을 받으므로, 개발 환경에서 프로젝트 루트를 기준으로 통일합니다.

## 실행 방법

> 현재는 **개발 예정 구조**를 안내합니다. 아래 실행 단계는 소스 코드와 기준 TXT 데이터가 구현·추가된 뒤 적용할 수 있습니다.

1. **JDK 17 이상**을 설치합니다.
2. IntelliJ IDEA 등의 Java IDE에서 저장소를 엽니다.
3. `src/main/java`를 Java 소스 폴더, `src/main/resources`를 리소스 폴더로 설정합니다.
4. 프로젝트 루트를 **실행 작업 디렉터리(working directory)**로 지정합니다.
5. `data/content/`에 `category.txt`, `content.txt`, `question.txt`가 있는지 확인합니다.
6. `com.quizpet.Main`의 `main()` 메서드를 실행합니다.
7. 첫 실행에서 `data/save/`가 없다면 Java 코드가 생성하도록 구현합니다.

**실행 환경:** 별도 서버·데이터베이스 설치 없이 로컬 PC에서 동작하도록 설계합니다.

### 자동 빌드 검사

[Java CI](.github/workflows/ci.yml)는 모든 push와 Pull Request에서 JDK 17로 `src/main/java`의 전체 Java 소스를 UTF-8로 컴파일합니다. 컴파일 오류와 Java 17에서 지원하지 않는 API·문법 사용을 검사하며, Swing 화면 실행과 동작 테스트는 포함하지 않습니다.

## 팀원별 역할 분담

프론트엔드와 백엔드는 **서로 다른 언어/서버가 아니라 모두 Java 코드**입니다. FE는 Swing UI, BE는 애플리케이션 내부 로직·파일 관리를 담당합니다.

| 담당 | 영역 | 주요 파일/기능 |
| --- | --- | --- |
| **FE 1** | 시작·캐릭터·공통 화면 | `MainFrame`, `ScreenManager`, `StartPanel`, `PetCreatePanel`, `HomePanel` |
| **FE 2** | 학습·퀴즈 화면 | `CategoryPanel`, `LearningPanel`, `QuizPanel`, 유형별 문제 컴포넌트 |
| **FE 3** | 결과·기록 화면 | `AnswerFeedbackPanel`, `QuizResultPanel`, `HistoryPanel`, 오답노트(P1) |
| **BE 1** | 사용자·캐릭터 | `User`, `Pet`, `PetStats`, 사용자·펫 Service/Repository |
| **BE 2** | 학습·퀴즈 | `Category`, `LearningContent`, `Question` 계열, 학습·퀴즈 Service/Repository |
| **BE 3** | 성장·기록 | `GrowthService`, `LearningRecord`, `RecordService`, `StatisticsService`, 기록 Repository |

**권장 협업 조:** FE 1 ↔ BE 1 / FE 2 ↔ BE 2 / FE 3 ↔ BE 3. 각 조는 담당 기능의 화면·로직 연결과 통합 테스트를 공동 진행합니다.

## 5주 개발 계획

| 주차 | 목표 | 주요 산출물 |
| --- | --- | --- |
| **1주 차** | 요구사항·UI·클래스 설계, 역할 분담, 메서드 계약 정의 | PRD, 기능명세, UML, 초기 골격 |
| **2주 차** | 사용자·캐릭터, 학습 카드, OX·4지선다 기본 구현 | 초기 화면 및 핵심 로직 |
| **3주 차** | 채점, 보상·레벨업, 화면 연결 | 핵심 플레이 흐름 |
| **4주 차** | TXT 저장·불러오기, 통계, P0 통합 | MVP 통합 버전 |
| **5주 차** | 테스트·오류 수정·UI 개선·시연 | 최종 실행 결과 및 발표 자료 |

**원칙:** P0를 먼저 완성하고, P1·P2는 일정에 여유가 있을 때만 추가합니다.

## 협업 규칙

1. **기능별 브랜치:** 예) `feature/fe-home`, `feature/be-quiz`, `fix/save-error`
2. **Pull Request 기반 병합:** 메인 브랜치에 직접 작업하지 않고 PR과 코드 리뷰를 거칩니다.
3. **공통 계약 우선:** FE/BE 간 메서드 이름, 입력 타입, 반환 타입, 오류 처리 방식을 먼저 합의합니다.
4. **역할 분리:** UI 코드에서 직접 TXT를 읽거나 채점·경험치 로직을 작성하지 않습니다.
5. **코드 작성 규칙:** 클래스는 `PascalCase`, 메서드·변수는 `camelCase`, 상수는 `UPPER_SNAKE_CASE`를 사용합니다.
6. **자료 일관성:** 학습 분야 ID, 능력치 매핑, TXT 필드 형식을 변경하면 관련 문서와 테스트 데이터를 함께 수정합니다.
7. **공유 데이터 보호:** `data/save/`는 개인별 실행 데이터로 취급하고, 테스트용 기준 데이터만 필요한 범위에서 공유합니다.

### `.gitignore` 권장 항목

```gitignore
# Java 빌드 결과물
*.class
out/
build/
target/

# IDE / OS
.idea/
*.iml
.DS_Store

# 로컬 게임 저장 데이터
/data/save/*.txt
/data/save/*.bak
/data/save/*.tmp
```

> `.gitkeep`을 `data/save/`에 남기면 빈 디렉터리 구조를 Git에서 유지할 수 있습니다.

## 완료 기준

- [ ] 사용자 프로필 생성·선택 및 캐릭터 생성/불러오기
- [ ] 캐릭터 외형 2~3종 선택, 경험치·레벨·능력치 표시
- [ ] 4개 학습 분야와 학습 카드 40개 이상
- [ ] OX·4지선다 퀴즈 80문제 이상
- [ ] 정답·해설 표시, 세션당 기본 5문제 출제
- [ ] 최초 정답에만 성장 보상 지급(중복 보상 방지)
- [ ] 사용자별 학습 기록과 분야별 정답률 조회
- [ ] TXT 저장·불러오기 및 프로그램 재실행 후 데이터 복원
- [ ] 핵심 기능 통합 테스트와 시연 완료

---

**참고 문서:** PRD · 기능명세서 · QuizPet UI 구상 v1.0 · 데이터 구성 문서  
**개발 목표:** 5주 안에 Java Swing 기반 교육형 캐릭터 육성 게임의 안정적인 MVP 완성
