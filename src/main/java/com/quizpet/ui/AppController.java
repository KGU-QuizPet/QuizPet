package main.java.com.quizpet.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetStats;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.*;
import main.java.com.quizpet.ui.component.AnswerFeedbackPanel;
import main.java.com.quizpet.ui.component.TopBar;
import main.java.com.quizpet.ui.screen.*;
import main.java.com.quizpet.ui.screen.HistoryPanel.HistoryView;

// 화면의 사용자·분야·세션 상태와 서비스 호출을 연결.
public final class AppController {
  private final ScreenManager screens = new ScreenManager();
  private final UserService users;
  private final PetService pets;
  private final ProfileService profiles;
  private final LearningService learning;
  private final QuizService quizzes;
  private final QuestionRepository questions;
  private final RecordService records;
  private final StatisticsService statistics;
  private final RecordQuestionLookup recordQuestions;
  private final WrongAnswerService wrongAnswers;
  private final SubmissionService submissions;
  private final StartPanel start = new StartPanel(screens);
  private final PetCreatePanel petCreate = new PetCreatePanel(screens);
  private final HomePanel home = new HomePanel(screens);
  private final CategoryPanel categories = new CategoryPanel(screens);
  private final LearningPanel cards = new LearningPanel(screens);
  private final QuizPanel quiz = new QuizPanel(screens);
  private final AnswerFeedbackPanel feedback = new AnswerFeedbackPanel();
  private final QuizResultPanel result = new QuizResultPanel();
  private final HistoryPanel history = new HistoryPanel();
  private final BattleService battles = new BattleService();
  private final BossBattlePanel bossBattle = new BossBattlePanel();
  private final TopBar topBar = new TopBar();
  private final JPanel window = new JPanel(new BorderLayout());
  private User currentUser;
  private Category currentCategory;
  private QuizSession session;
  private int levelBefore;
  /** 퀴즈 시작 시점의 능력치입니다. 결과 화면 비교와 홈 성장 강조에 씁니다. */
  private Map<String, Integer> statsBefore;
  /** 결과 화면을 본 뒤 홈에서 한 번 보여 줄 성장 정보입니다(없으면 null). */
  private Growth pendingGrowth;
  private boolean feedbackCard;
  private boolean viewingWrongNote;
  private String pendingAnswer;

  public AppController(Path saveDirectory) throws IOException {
    this(saveDirectory, Path.of("data/content"));
  }

  public AppController(Path saveDirectory, Path contentDirectory) throws IOException {
    UserRepository userRepository = new UserRepository(saveDirectory.resolve("user.txt"));
    PetRepository petRepository = new PetRepository(saveDirectory.resolve("pet.txt"));
    users = new UserService(userRepository);
    pets = new PetService(petRepository, userRepository);
    profiles = new ProfileService(userRepository, petRepository);
    ContentRepository contents = new ContentRepository(contentDirectory.resolve("content.txt"));
    learning = new LearningService(new CategoryRepository(contentDirectory.resolve("category.txt")), contents);
    questions = new QuestionRepository(contentDirectory.resolve("question.txt"));
    quizzes = new QuizService(questions);
    records = new RecordService(new RecordRepository(saveDirectory.resolve("record.txt")));
    recordQuestions = new RecordQuestionLookup(questions, contentDirectory.resolve("question-legacy.txt"));
    statistics = new StatisticsService(records, id -> recordQuestions.findById(id).map(Question::getCategory));
    wrongAnswers = new WrongAnswerService(records, recordQuestions);
    submissions = new SubmissionService(saveDirectory);
    registerScreens();
    connectActions();
    connectTopBar();
    showStart();
  }

  public JPanel getView() {
    return screens.getContainer();
  }

  public JPanel getWindowContent() {
    return window;
  }

  public TopBar getTopBar() {
    return topBar;
  }

  private void connectTopBar() {
    window.add(topBar, BorderLayout.NORTH);
    window.add(screens.getContainer(), BorderLayout.CENTER);
    topBar.setOnChangeUser(() -> {
      run(() -> { if (confirmLeaveQuiz()) showStart(); });
    });
    topBar.setOnTab(tab -> {
      run(() -> {
        if (!confirmLeaveQuiz()) return;
        switch (tab) {
          case TopBar.TAB_HOME -> showHome();
          case TopBar.TAB_LEARN -> showCategories();
          case TopBar.TAB_HISTORY -> showHistory(null);
          case TopBar.TAB_WRONG_NOTE -> showWrongNote(null, false);
          case TopBar.TAB_BATTLE -> showBossBattle();
          default -> { }
        }
      });
      selectCurrentTab();
    });
    screens.setOnShow(name -> {
      if (!ScreenManager.BOSS_BATTLE.equals(name)) bossBattle.stopBattle();
      topBar.setVisible(!ScreenManager.START.equals(name) && !ScreenManager.PET_CREATE.equals(name));
      selectCurrentTab();
    });
  }

  private void selectCurrentTab() {
    topBar.select(ScreenManager.HISTORY.equals(screens.getCurrent()) && viewingWrongNote
        ? TopBar.TAB_WRONG_NOTE : tabOf(screens.getCurrent()));
  }

  static String tabOf(String screen) {
    if (screen == null) return null;
    return switch (screen) {
      case ScreenManager.HOME -> TopBar.TAB_HOME;
      case ScreenManager.CATEGORY, ScreenManager.LEARNING, ScreenManager.QUIZ,
          ScreenManager.ANSWER_FEEDBACK, ScreenManager.QUIZ_RESULT -> TopBar.TAB_LEARN;
      case ScreenManager.HISTORY -> TopBar.TAB_HISTORY;
      case ScreenManager.BOSS_BATTLE -> TopBar.TAB_BATTLE;
      default -> null;
    };
  }

  private boolean confirmLeaveQuiz() throws IOException {
    String current = screens.getCurrent();
    boolean inQuiz = session != null && !session.isFinished()
        && (ScreenManager.QUIZ.equals(current) || ScreenManager.ANSWER_FEEDBACK.equals(current)
            || feedbackCard);
    if (!inQuiz) return true;
    int answer = JOptionPane.showConfirmDialog(getView(), "진행 중인 퀴즈를 종료할까요?",
        "퀴즈 종료", JOptionPane.YES_NO_OPTION);
    if (answer != JOptionPane.YES_OPTION) return false;
    endQuiz();
    return true;
  }

  private void registerScreens() {
    screens.addScreen(ScreenManager.START, start);
    screens.addScreen(ScreenManager.PET_CREATE, petCreate);
    screens.addScreen(ScreenManager.HOME, home);
    screens.addScreen(ScreenManager.CATEGORY, categories);
    screens.addScreen(ScreenManager.LEARNING, cards);
    screens.addScreen(ScreenManager.QUIZ, quiz);
    screens.addScreen(ScreenManager.ANSWER_FEEDBACK, feedback);
    screens.addScreen(ScreenManager.QUIZ_RESULT, result);
    screens.addScreen(ScreenManager.HISTORY, history);
    screens.addScreen(ScreenManager.BOSS_BATTLE, bossBattle);
  }

  private void connectActions() {
    start.setOnSelect(id -> run(() -> selectUser(id)));
    start.setOnCreate(nickname -> run(() -> createUser(nickname)));
    petCreate.setOnBack(() -> run(this::showStart));
    petCreate.setOnCreate((name, appearance) -> run(() -> createPet(name, appearance)));
    home.setOnLearn(() -> run(this::showCategories));
    home.setOnHistory(() -> run(() -> showHistory(null)));
    home.setOnCategory(id -> run(() -> selectCategory(id)));
    home.setOnWrongNote(() -> run(() -> showWrongNote(null, true)));
    bossBattle.setOnExit(() -> run(this::showHome));
    categories.setOnSelect(id -> run(() -> selectCategory(id)));
    cards.setOnQuiz(() -> run(this::startQuiz));
    cards.setOnBack(this::backFromLearning);
    quiz.setOnSubmit(answer -> run(() -> submitAnswer(answer)));
    quiz.setOnEnd(() -> run(this::endQuiz));
    feedback.setOnNext(() -> run(this::nextQuestion));
    feedback.setOnViewCard(() -> run(this::showFeedbackCard));
    result.setOnGoMain(() -> run(this::showHome));
    result.setOnGoHistory(() -> run(() -> showHistory(null)));
    history.setOnGoMain(() -> run(this::showHome));
    history.setOnCategoryChanged(id -> run(() -> showHistory(id)));
    history.setWrongNoteEnabled(true);
    history.setReviewEnabled(false);
    history.setOnOpenWrongNote(id -> run(() -> showWrongNote(id, false)));
    history.setOnWrongNoteCategoryChanged(id -> run(() -> showWrongNote(id, history.isNeedsReviewOnly())));
    history.setOnWrongNoteRefresh(
        () -> run(() -> showWrongNote(history.getWrongCategoryId(), history.isNeedsReviewOnly())));
    history.setOnWrongNoteBack(() -> run(() -> showHistory(null)));
  }

  public void showStart() throws IOException {
    ready();
    start.showProfiles(profiles.getProfiles());
    history.clearWrongNote();
    currentUser = null;
    currentCategory = null;
    session = null;
    feedbackCard = false;
    screens.show(ScreenManager.START);
  }

  public User createUser(String nickname) throws IOException {
    ready();
    User created = users.createUser(nickname);
    selectUser(created.getUserId());
    return created;
  }

  public void selectUser(String userId) throws IOException {
    ready();
    User selected = users.getUser(userId);
    Pet pet = pets.findPetByUserId(userId).orElse(null);
    history.clearWrongNote();
    currentUser = selected;
    currentCategory = null;
    session = null;
    feedbackCard = false;
    if (pet == null) {
      petCreate.prepare(selected.getNickname());
      screens.show(ScreenManager.PET_CREATE);
    } else {
      displayHome(pet);
    }
  }

  public Pet createPet(String name, String appearance) throws IOException {
    requireUser();
    ready();
    Pet pet = pets.createPet(currentUser.getUserId(), name, appearance);
    displayHome(pet);
    return pet;
  }

  public void showHome() throws IOException {
    requireUser();
    ready();
    displayHome(currentPet());
  }

  public void showBossBattle() throws IOException {
    requireUser();
    ready();
    bossBattle.showBattle(currentPet());
    screens.show(ScreenManager.BOSS_BATTLE);
  }

  private void displayHome(Pet pet) throws IOException {
    home.setGreeting(currentUser == null ? null : currentUser.getNickname());
    topBar.setUser(currentUser == null ? null : currentUser.getNickname(), pet.getSpriteKey());
    StatisticsService.Statistics overall = statistics.getOverallStatistics(pet.getUserId());
    home.showSummary(overall.getAccuracy(), overall.getTotalAttempts());
    home.showPet(pet.getName(), pet.getSpriteKey(), pet.getLevel(), pet.getXpInLevel(),
        pet.getStats().getStrength(), pet.getStats().getIntelligence(),
        pet.getStats().getFocus(), pet.getStats().getWisdom());
    showHomeRecords(pet.getUserId());
    if (pendingGrowth != null && pendingGrowth.userId().equals(pet.getUserId())) {
      home.showGrowth(pendingGrowth.statsBefore(), pendingGrowth.gainedXp(), pendingGrowth.leveledUp());
    }
    pendingGrowth = null;
    screens.show(ScreenManager.HOME);
  }

  private void showHomeRecords(String userId) throws IOException {
    List<LearningRecord> all = records.getRecords(userId);
    boolean unresolved = all.stream()
        .anyMatch(record -> recordQuestions.findById(record.getQuestionId()).isEmpty());
    for (Category category : Category.values()) {
      if (unresolved) {
        home.showCategoryProgress(category, 0, 0);
      } else {
        StatisticsService.Statistics stats = statistics.getCategoryStatistics(userId, category.getCategoryId());
        home.showCategoryProgress(category, stats.getTotalAttempts(), stats.getAccuracy());
      }
    }

    List<HomePanel.RecentItem> recent = new ArrayList<>();
    for (LearningRecord record : records.getRecentRecords(userId, HomePanel.RECENT_MAX)) {
      Question question = recordQuestions.findById(record.getQuestionId()).orElse(null);
      recent.add(new HomePanel.RecentItem(question == null ? null : question.getCategory(),
          question == null ? "문제 정보 없음" : question.getPrompt(),
          record.isCorrect(), record.getEarnedXp()));
    }
    home.showRecent(recent);

    Map<Category, Integer> wrongCounts = new EnumMap<>(Category.class);
    int unclassified = 0;
    for (var wrong : wrongAnswers.getWrongAnswers(userId, null,
        WrongAnswerService.ListPolicy.NEEDS_REVIEW_ONLY)) {
      if (wrong.category().isPresent()) {
        wrongCounts.merge(wrong.category().get(), 1, Integer::sum);
      } else {
        unclassified++;
      }
    }
    home.showWrongCounts(wrongCounts, unclassified);
  }

  private static Map<String, Integer> statMap(PetStats stats) {
    return Map.of("STR", stats.getStrength(), "INT", stats.getIntelligence(),
        "FOC", stats.getFocus(), "WIS", stats.getWisdom());
  }

  private record Growth(String userId, Map<String, Integer> statsBefore, int gainedXp, boolean leveledUp) {
  }

  public void showCategories() throws IOException {
    requireUser();
    ready();
    currentPet();
    currentCategory = null;
    session = null;
    categories.showCategories(learning.getCategories());
    screens.show(ScreenManager.CATEGORY);
  }

  public void selectCategory(String categoryId) throws IOException {
    requireUser();
    ready();
    Category selected = Category.valueOf(categoryId);
    List<LearningContent> selectedCards = learning.getContents(categoryId);
    if (selectedCards.isEmpty()) {
      throw new IllegalStateException("선택한 분야에 학습 카드가 없습니다.");
    }
    currentCategory = selected;
    session = null;
    feedbackCard = false;
    cards.setQuizEnabled(!questions.findByCategory(selected).isEmpty());
    cards.showContents(selected, selectedCards, 0);
    screens.show(ScreenManager.LEARNING);
  }

  public QuizSession startQuiz() throws IOException {
    requireUser();
    if (currentCategory == null || feedbackCard) {
      throw new IllegalStateException("학습 분야를 먼저 선택해 주세요.");
    }
    ready();
    Pet pet = currentPet();
    QuizSession created = quizzes.startSession(currentUser.getUserId(), currentCategory.getCategoryId());
    levelBefore = pet.getLevel();
    statsBefore = statMap(pet.getStats());
    pendingGrowth = null;
    session = created;
    quiz.showSession(session);
    screens.show(ScreenManager.QUIZ);
    return session;
  }

  public LearningRecord submitAnswer(String answer) throws IOException {
    requireSession();
    if (pendingAnswer != null && !pendingAnswer.equals(answer)) {
      throw new IllegalStateException("저장하지 못한 답안을 먼저 재시도해 주세요.");
    }
    session.getCurrentQuestion().checkAnswer(answer);
    pendingAnswer = answer;
    LearningRecord record;
    try {
      record = submissions.submit(session, answer);
    } catch (SubmissionRejectedException e) {
      pendingAnswer = null;
      throw e;
    }
    pendingAnswer = null;
    showFeedback(record);
    return record;
  }

  private void showFeedback(LearningRecord record) throws IOException {
    Question question = session.getCurrentQuestion();
    Pet pet = currentPet();
    feedback.setPet(pet.getSpriteKey());
    feedback.setPetStatus(pet.getLevel(), pet.getXpInLevel(), Pet.XP_PER_LEVEL, statMap(pet.getStats()));
    feedback.showSession(session);
    feedback.showFeedback(question, record.getSelectedAnswer(), record.isCorrect(),
        record.getEarnedXp() > 0 ? 1 : 0, record.getEarnedXp(),
        hasLearningCard(question), session.isFinished());
    screens.show(ScreenManager.ANSWER_FEEDBACK);
  }

  private boolean hasLearningCard(Question question) {
    return learning.getContents(question.getCategoryId()).stream()
        .anyMatch(content -> content.getContentId().equals(question.getContentId()));
  }

  public void showFeedbackCard() {
    requireSession();
    if (!session.isCurrentQuestionAnswered()) {
      throw new IllegalStateException("답안을 제출한 뒤 학습 카드를 확인할 수 있습니다.");
    }
    List<LearningContent> selectedCards = learning.getContents(session.getCategoryId());
    String contentId = session.getCurrentQuestion().getContentId();
    int index = -1;
    for (int i = 0; i < selectedCards.size(); i++) {
      if (selectedCards.get(i).getContentId().equals(contentId))
        index = i;
    }
    if (index < 0)
      throw new IllegalStateException("연결된 학습 카드를 찾을 수 없습니다.");
    feedbackCard = true;
    cards.setQuizEnabled(false);
    cards.showContents(session.getCategory(), selectedCards, index);
    screens.show(ScreenManager.LEARNING);
  }

  public void backFromLearning() {
    if (feedbackCard) {
      feedbackCard = false;
      screens.show(ScreenManager.ANSWER_FEEDBACK);
    } else {
      run(this::showCategories);
    }
  }

  public void nextQuestion() throws IOException {
    requireSession();
    ready();
    if (session.isFinished()) {
      showResult();
    } else {
      quizzes.nextQuestion(session);
      quiz.showSession(session);
      screens.show(ScreenManager.QUIZ);
    }
  }

  public void endQuiz() throws IOException {
    requireSession();
    ready();
    quizzes.endSession(session);
    showResult();
  }

  private void showResult() throws IOException {
    Pet pet = currentPet();
    List<LearningRecord> saved = records.getSessionRecords(currentUser.getUserId(), session.getSessionId());
    int gainedXp = saved.stream().mapToInt(LearningRecord::getEarnedXp).sum();
    int statGain = (int) saved.stream().filter(record -> record.getEarnedXp() > 0).count();
    result.showResult(session.getCategory(), session.getResult(), gainedXp, statGain,
        levelBefore, pet.getLevel(), pet.getXpInLevel());
    Map<String, Integer> statsAfter = statMap(pet.getStats());
    if (statsBefore != null) {
      result.showStatComparison(statsBefore, statsAfter);
      pendingGrowth = gainedXp > 0
          ? new Growth(pet.getUserId(), statsBefore, gainedXp, pet.getLevel() > levelBefore)
          : null;
    } else {
      result.hideStatComparison();
      pendingGrowth = null;
    }
    screens.show(ScreenManager.QUIZ_RESULT);
  }

  public void showHistory(String categoryId) throws IOException {
    requireUser();
    viewingWrongNote = false;
    history.showRecordPage();
    history.showHistoryError("기록을 불러오는 중입니다.");
    screens.show(ScreenManager.HISTORY);
    try {
      ready();
      boolean unresolved = records.getRecords(currentUser.getUserId()).stream()
          .anyMatch(record -> recordQuestions.findById(record.getQuestionId()).isEmpty());
      // 분야 집계 불가 시에도 계산 가능한 전체 통계와 기록은 유지.
      if (unresolved)
        categoryId = null;
      List<HistoryView.CategoryRate> rates = new ArrayList<>();
      for (Category category : unresolved ? List.<Category>of() : learning.getCategories()) {
        rates.add(HistoryView.CategoryRate.of(category,
            statistics.getCategoryStatistics(currentUser.getUserId(), category.getCategoryId())));
      }
      List<HistoryView.RecordRow> rows = new ArrayList<>();
      for (LearningRecord record : records.getRecentRecords(currentUser.getUserId(), Integer.MAX_VALUE)) {
        Question question = recordQuestions.findById(record.getQuestionId()).orElse(null);
        if (categoryId == null || (question != null && question.getCategoryId().equals(categoryId))) {
          rows.add(question == null
              ? new HistoryView.RecordRow(record.getAnsweredAt(), "분야 확인 불가",
                  "문제 정보 없음: " + record.getQuestionId(), record.getSelectedAnswer(),
                  record.isCorrect(), record.getEarnedXp())
              : HistoryView.RecordRow.of(question, record));
          if (rows.size() == 20)
            break;
        }
      }
      var selected = categoryId == null ? statistics.getOverallStatistics(currentUser.getUserId())
          : statistics.getCategoryStatistics(currentUser.getUserId(), categoryId);
      history.showHistory(HistoryView.of(selected, rates, rows));
    } catch (IOException | RuntimeException e) {
      history.showHistoryError("기록을 불러오지 못했습니다: " + e.getMessage());
      throw e;
    }
  }

  public void showWrongNote(String categoryId, boolean needsReviewOnly) throws IOException {
    requireUser();
    viewingWrongNote = true;
    history.setWrongCategoryId(categoryId);
    history.setWrongNeedsReviewOnly(needsReviewOnly);
    history.showWrongNoteError("오답 기록을 불러오는 중입니다.");
    screens.show(ScreenManager.HISTORY);
    try {
      ready();
      Category selectedCategory = categoryId == null || categoryId.equals("UNCLASSIFIED")
          ? null
          : Category.valueOf(categoryId);
      var all = wrongAnswers.getWrongAnswers(currentUser.getUserId());
      var selected = all.stream()
          .filter(item -> categoryId == null || (categoryId.equals("UNCLASSIFIED") ? item.category().isEmpty()
              : item.category().filter(selectedCategory::equals).isPresent()))
          .filter(item -> !needsReviewOnly || item.reviewStatus() != WrongAnswerService.ReviewStatus.COMPLETED)
          .toList();
      history.showWrongNote(WrongNotePresenter.toView(categoryId, all.size(), selected,
          categoryId != null || needsReviewOnly));
    } catch (IOException | RuntimeException e) {
      history.showWrongNoteError("오답 기록을 불러오지 못했습니다: " + e.getMessage());
      throw e;
    }
  }

  private Pet currentPet() throws IOException {
    return pets.findPetByUserId(currentUser.getUserId()).orElseThrow(
        () -> new IllegalStateException("캐릭터를 먼저 생성해 주세요."));
  }

  private void ready() throws IOException {
    if (pendingAnswer != null) {
      try {
        submissions.submit(session, pendingAnswer);
      } catch (SubmissionRejectedException e) {
        pendingAnswer = null;
        throw e;
      }
      pendingAnswer = null;
    }
    submissions.recover();
  }

  private void requireUser() {
    if (currentUser == null)
      throw new IllegalStateException("사용자를 먼저 선택해 주세요.");
  }

  private void requireSession() {
    requireUser();
    if (session == null)
      throw new IllegalStateException("퀴즈를 먼저 시작해 주세요.");
  }

  private void run(Action action) {
    try {
      action.run();
    } catch (IOException | IllegalArgumentException | IllegalStateException e) {
      JOptionPane.showMessageDialog(getView(), e.getMessage(), "처리할 수 없습니다", JOptionPane.ERROR_MESSAGE);
    }
  }

  @FunctionalInterface
  private interface Action {
    void run() throws IOException;
  }
}
