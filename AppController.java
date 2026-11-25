package main.java.com.quizpet.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.*;
import main.java.com.quizpet.ui.component.AnswerFeedbackPanel;
import main.java.com.quizpet.ui.component.TopBar;
import main.java.com.quizpet.ui.screen.*;
import main.java.com.quizpet.ui.screen.HistoryPanel.HistoryView;

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
  private final TopBar topBar = new TopBar();
  private final JPanel window = new JPanel(new BorderLayout());
  private User currentUser;
  private Category currentCategory;
  private QuizSession session;
  private int levelBefore;
  private boolean feedbackCard;
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
    topBar.setTabEnabled(TopBar.TAB_WRONG_NOTE, false);
    topBar.setTabEnabled(TopBar.TAB_BATTLE, false);
    topBar.setOnChangeUser(() -> {
      if (confirmLeaveQuiz()) run(this::showStart);
    });
    topBar.setOnTab(tab -> {
      if (!confirmLeaveQuiz()) {
        topBar.select(tabOf(screens.getCurrent()));
        return;
      }
      switch (tab) {
        case TopBar.TAB_HOME -> run(this::showHome);
        case TopBar.TAB_LEARN -> run(this::showCategories);
        case TopBar.TAB_HISTORY -> run(() -> showHistory(null));
        default -> { }
      }
      topBar.select(tabOf(screens.getCurrent()));
    });
    screens.setOnShow(name -> {
      topBar.setVisible(!ScreenManager.START.equals(name) && !ScreenManager.PET_CREATE.equals(name));
      topBar.select(tabOf(name));
    });
  }

  static String tabOf(String screen) {
    if (screen == null) return null;
    return switch (screen) {
      case ScreenManager.HOME -> TopBar.TAB_HOME;
      case ScreenManager.CATEGORY, ScreenManager.LEARNING, ScreenManager.QUIZ,
          ScreenManager.ANSWER_FEEDBACK, ScreenManager.QUIZ_RESULT -> TopBar.TAB_LEARN;
      case ScreenManager.HISTORY -> TopBar.TAB_HISTORY;
      default -> null;
    };
  }

  private boolean confirmLeaveQuiz() {
    String current = screens.getCurrent();
    boolean inQuiz = session != null && !session.isFinished()
        && (ScreenManager.QUIZ.equals(current) || ScreenManager.ANSWER_FEEDBACK.equals(current));
    if (!inQuiz) return true;
    int answer = JOptionPane.showConfirmDialog(getView(), "진행 중인 퀴즈를 종료할까요?",
        "퀴즈 종료", JOptionPane.YES_NO_OPTION);
    if (answer != JOptionPane.YES_OPTION) return false;
    quizzes.endSession(session);
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
  }

  private void connectActions() {
    start.setOnSelect(id -> run(() -> selectUser(id)));
    start.setOnCreate(nickname -> run(() -> createUser(nickname)));
    petCreate.setOnBack(() -> run(this::showStart));
    petCreate.setOnCreate((name, appearance) -> run(() -> createPet(name, appearance)));
    home.setOnLearn(() -> run(this::showCategories));
    home.setOnHistory(() -> run(() -> showHistory(null)));
    home.setOnCategory(id -> run(() -> selectCategory(id)));
    home.setOnWrongNote(() -> run(() -> showHistory(null)));
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
    history.setWrongNoteEnabled(false);
  }

  public void showStart() throws IOException {
    ready();
    start.showProfiles(profiles.getProfiles());
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

  private void displayHome(Pet pet) throws IOException {
    String userId = pet.getUserId();
    home.setGreeting(currentUser == null ? null : currentUser.getNickname());
    topBar.setUser(currentUser == null ? null : currentUser.getNickname(), pet.getSpriteKey());
    StatisticsService.Statistics overall = statistics.getOverallStatistics(userId);
    home.showSummary(overall.getAccuracy(), overall.getTotalAttempts());
    home.showPet(pet.getName(), pet.getSpriteKey(), pet.getLevel(), pet.getXpInLevel(),
        pet.getStats().getStrength(), pet.getStats().getIntelligence(),
        pet.getStats().getFocus(), pet.getStats().getWisdom());
    showHomeRecords(userId);
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

    Set<String> solved = new HashSet<>();
    for (LearningRecord record : all) {
      if (record.isCorrect()) solved.add(record.getQuestionId());
    }
    Set<String> counted = new HashSet<>();
    Map<Category, Integer> wrongCounts = new EnumMap<>(Category.class);
    for (LearningRecord record : all) {
      String id = record.getQuestionId();
      if (record.isCorrect() || solved.contains(id) || !counted.add(id)) continue;
      recordQuestions.findById(id).ifPresent(
          question -> wrongCounts.merge(question.getCategory(), 1, Integer::sum));
    }
    home.showWrongCounts(wrongCounts);
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
    feedback.setPet(currentPet().getSpriteKey());
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
      if (selectedCards.get(i).getContentId().equals(contentId)) index = i;
    }
    if (index < 0) throw new IllegalStateException("연결된 학습 카드를 찾을 수 없습니다.");
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
    screens.show(ScreenManager.QUIZ_RESULT);
  }

  public void showHistory(String categoryId) throws IOException {
    requireUser();
    history.showRecordPage();
    history.showHistoryError("기록을 불러오는 중입니다.");
    screens.show(ScreenManager.HISTORY);
    try {
      ready();
      boolean unresolved = records.getRecords(currentUser.getUserId()).stream()
          .anyMatch(record -> recordQuestions.findById(record.getQuestionId()).isEmpty());
      if (unresolved) categoryId = null;
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
          if (rows.size() == 20) break;
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
    if (currentUser == null) throw new IllegalStateException("사용자를 먼저 선택해 주세요.");
  }

  private void requireSession() {
    requireUser();
    if (session == null) throw new IllegalStateException("퀴즈를 먼저 시작해 주세요.");
  }

  private void run(Action action) {
    try {
      action.run();
    } catch (IOException | IllegalArgumentException | IllegalStateException e) {
      JOptionPane.showMessageDialog(getView(), e.getMessage(), "처리할 수 없습니다", JOptionPane.ERROR_MESSAGE);
    }
  }

  @FunctionalInterface
  private interface Action { void run() throws IOException; }
}
