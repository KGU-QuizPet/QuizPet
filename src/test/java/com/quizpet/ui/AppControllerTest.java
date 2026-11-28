package main.java.com.quizpet.ui;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.ui.component.AnswerFeedbackPanel;
import main.java.com.quizpet.ui.screen.*;

public class AppControllerTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        creationBackRefresh();
        fullFlow();
        growthFlow();
        recordSidebar();
        loadErrors();
        unavailableContent();
        failedSubmissionRetry();
        failedSubmissionExitRetry();
        topBarNavigation();
        battleStopsOnNavigation();
        homeReviewCounts();
        unavailableCategoryProgress();
        rejectedSubmissionAllowsNavigation();
        partiallySavedSubmissionRetry();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("AppController integration checks passed");
  }

  private static void creationBackRefresh() throws Exception {
    AppController app = new AppController(Files.createTempDirectory("quizpet-creation-back-"));
    User created = app.createUser("돌아가기");
    assert clickVisible(app.getView(), "이전");
    assert visible(app, StartPanel.class);
    javax.swing.JList<?> list = findVisible(app.getView(), javax.swing.JList.class);
    assert list.getModel().getSize() == 1 : "created user missing after returning to start";
    assert ((Profile) list.getModel().getElementAt(0)).getUser().getUserId().equals(created.getUserId());
    list.setSelectedIndex(0);
    list.getActionMap().get(StartPanel.ENTER_ACTION).actionPerformed(null);
    assert visible(app, PetCreatePanel.class);
  }

  private static <T> T findVisible(Component root, Class<T> type) {
    if (!root.isVisible()) return null;
    if (type.isInstance(root)) return type.cast(root);
    if (root instanceof Container container) {
      for (Component child : container.getComponents()) {
        T found = findVisible(child, type);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static boolean clickVisible(Component root, String text) {
    if (!root.isVisible()) return false;
    if (root instanceof javax.swing.JButton button && button.getText().equals(text)) {
      button.doClick();
      return true;
    }
    if (root instanceof Container container) {
      for (Component child : container.getComponents()) if (clickVisible(child, text)) return true;
    }
    return false;
  }

  private static void fullFlow() throws Exception {
    Path save = Files.createTempDirectory("quizpet-app-");
    AppController app = new AppController(save);
    assert visible(app, StartPanel.class);
    User user = app.createUser("통합사용자");
    assert visible(app, PetCreatePanel.class);
    Pet pet = app.createPet("통합펫", "DUCK");
    assert visible(app, HomePanel.class);
    expectState(app::startQuiz);
    int totalAnswers = 0;
    for (Category category : Category.values()) {
      app.showCategories();
      app.selectCategory(category.getCategoryId());
      assert visible(app, LearningPanel.class);
      QuizSession session = app.startQuiz();
      assert session.getTotalCount() == 5;
      int beforeXp = new PetRepository(save.resolve("pet.txt")).findById(pet.getPetId()).orElseThrow().getTotalXp();
      for (int i = 0; i < session.getTotalCount(); i++) {
        Question question = session.getCurrentQuestion();
        LearningRecord record = app.submitAnswer(question.getAnswer());
        assert record.isCorrect();
        assert visible(app, AnswerFeedbackPanel.class);
        assert session.getAnsweredCount() == i + 1;
        app.showFeedbackCard();
        assert visible(app, LearningPanel.class);
        app.backFromLearning();
        assert visible(app, AnswerFeedbackPanel.class);
        assert session.getCurrentQuestion() == question;
        // A repeated click must not add a second record or growth.
        app.submitAnswer(question.getAnswer());
        app.nextQuestion();
      }
      totalAnswers += 5;
      assert visible(app, QuizResultPanel.class);
      assert session.getResult().getAccuracy() == 100.0;
      assert new RecordRepository(save.resolve("record.txt")).findByUserId(user.getUserId()).size() == totalAnswers;
      Pet updated = new PetRepository(save.resolve("pet.txt")).findById(pet.getPetId()).orElseThrow();
      assert updated.getTotalXp() == beforeXp + 50;
      app.showHistory(null);
      assert visible(app, HistoryPanel.class);
      app.showHistory(category.getCategoryId());
      app.showHome();
    }
    assert new PetRepository(save.resolve("pet.txt")).findById(pet.getPetId()).orElseThrow().getLevel() == 6;

    app.selectCategory("SCIENCE");
    QuizSession empty = app.startQuiz();
    app.endQuiz();
    assert empty.getResult().getAnsweredCount() == 0;
    assert empty.getResult().getAccuracy() == 0.0;
    app.selectCategory("SCIENCE");
    QuizSession partial = app.startQuiz();
    app.submitAnswer(partial.getCurrentQuestion().getAnswer());
    app.nextQuestion();
    app.endQuiz();
    assert partial.getResult().getAnsweredCount() == 1;
    assert partial.getResult().getAccuracy() == 100.0;

    app.showStart();
    User other = app.createUser("다른사용자");
    app.createPet("다른펫", "TURTLE");
    assert new RecordRepository(save.resolve("record.txt")).findByUserId(other.getUserId()).isEmpty();
    expectState(app::startQuiz);
    app.showHistory(null);
    assert visible(app, HistoryPanel.class);
    app.showStart();
    expectState(() -> app.submitAnswer("O"));

    AppController restarted = new AppController(save);
    restarted.selectUser(user.getUserId());
    assert visible(restarted, HomePanel.class);
    restarted.showHistory("SCIENCE");
    assert new UserRepository(save.resolve("user.txt")).findAll().size() == 2;

    // Failed history refresh clears prior rows instead of showing another user's data.
    Files.writeString(save.resolve("record.txt"), "broken\n");
    try {
      restarted.showHistory(null);
      throw new AssertionError("corrupt records accepted");
    } catch (IOException expected) {
      assert visible(restarted, HistoryPanel.class);
      assert texts(restarted.getView()).contains("불러오");
    }
  }

  /** 정답으로 성장하면 결과 화면에 능력치 전후 비교가, 홈에 증가량과 성장 반응이 한 번 표시됩니다. */
  private static void growthFlow() throws Exception {
    AppController app = new AppController(Files.createTempDirectory("quizpet-growth-"));
    app.createUser("성장확인");
    app.createPet("쑥쑥이", "DUCK");
    app.selectCategory("SCIENCE");
    QuizSession session = app.startQuiz();
    int correct = 0;
    while (true) {
      app.submitAnswer(session.getCurrentQuestion().getAnswer());
      correct++;
      if (session.isFinished()) break;
      app.nextQuestion();
    }
    app.nextQuestion();
    assert visible(app, QuizResultPanel.class);
    main.java.com.quizpet.ui.component.StatComparisonPanel comparison =
        findVisible(app.getView(), main.java.com.quizpet.ui.component.StatComparisonPanel.class);
    assert comparison != null : "결과 화면에 능력치 비교가 없음";
    int after = 10 + correct;
    assert comparison.getChangeText("INT").equals("10 → " + after + "  (+" + correct + ")")
        : comparison.getChangeText("INT");
    assert comparison.getChangeText("STR").equals("10");

    app.showHome();
    main.java.com.quizpet.ui.component.PetStatusPanel status =
        findVisible(app.getView(), main.java.com.quizpet.ui.component.PetStatusPanel.class);
    assert status.getStatDeltaText("INT").equals("+" + correct) : status.getStatDeltaText("INT");
    assert status.getStage().isCelebrating();
    assert status.isStatMarkedWeak("STR") : "INT만 올랐으므로 나머지가 약점으로 표시돼야 함";
    status.getStage().stopAnimation();

    // 성장 강조는 한 번만 보여 준다.
    app.showHome();
    assert status.getStatDeltaText("INT").isEmpty();
  }

  private static void recordSidebar() throws Exception {
    Path save = Files.createTempDirectory("quizpet-record-sidebar-");
    AppController app = new AppController(save);
    User first = app.createUser("기록사용자");
    Pet pet = app.createPet("기록펫", "DUCK");
    app.showHistory(null);
    assertRecordSidebar(app, "기록펫", "Lv.1", "EXP 0 / 50");

    pet.applyReward(60, "INT", 6);
    new PetRepository(save.resolve("pet.txt")).save(pet);
    app.showWrongNote(null, false);
    assertRecordSidebar(app, "기록펫", "Lv.2", "EXP 10 / 50");
    app.showHistory("SCIENCE");
    assertRecordSidebar(app, "기록펫", "Lv.2", "EXP 10 / 50");

    app.createUser("다른사용자");
    app.createPet("다른펫", "TURTLE");
    app.showWrongNote(null, true);
    assertRecordSidebar(app, "다른펫", "Lv.1", "EXP 0 / 50");
    app.selectUser(first.getUserId());
    app.showHistory(null);
    assertRecordSidebar(app, "기록펫", "Lv.2", "EXP 10 / 50");
  }

  private static void assertRecordSidebar(AppController app, String name, String level, String xp) {
    var status = findVisible(app.getView(), main.java.com.quizpet.ui.component.PetStatusPanel.class);
    assert status != null : "학습 기록과 오답노트에도 캐릭터 사이드바가 보여야 함";
    String text = texts(status);
    assert text.contains(name) && text.contains(level) && text.contains(xp) : text;
    for (int width : new int[]{1280, 800}) {
      app.getView().setSize(width, 736);
      for (int i = 0; i < 8; i++) layoutRecordSidebar(app.getView());
      HistoryPanel history = findVisible(app.getView(), HistoryPanel.class);
      assert status.getWidth() == 320 && status.getStage().getHeight() > 0;
      assert history.getWidth() == width - 320 : "캐릭터 오른쪽에 기록 본문이 배치되어야 함";
    }
  }

  private static void layoutRecordSidebar(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) {
      if (child instanceof Container container) layoutRecordSidebar(container);
    }
  }

  private static void unavailableContent() throws Exception {
    Path root = Files.createTempDirectory("quizpet-unavailable-");
    Path content = root.resolve("content");
    Files.createDirectories(content);
    for (String name : List.of("category.txt", "content.txt")) {
      Files.copy(Path.of("data/content").resolve(name), content.resolve(name));
    }
    Files.writeString(content.resolve("question.txt"),
        "Q-MISSING|SCIENCE|NO-CARD|OX|O|\n문제\n해설\n");
    AppController app = new AppController(root.resolve("save"), content);
    app.createUser("누락카드");
    app.createPet("펫", "DUCK");
    app.selectCategory("SCIENCE");
    QuizSession session = app.startQuiz();
    app.submitAnswer("O");
    javax.swing.JButton cardButton = findButton(app.getView(), "학습 카드 보기");
    assert cardButton != null && !cardButton.isEnabled();
    app.nextQuestion();
    assert session.isFinished() && visible(app, QuizResultPanel.class);
    app.selectCategory("SPORTS");
    javax.swing.JButton quizButton = findButton(app.getView(), "퀴즈 시작");
    assert quizButton != null && !quizButton.isEnabled() : "empty quiz start enabled";
  }

  private static void failedSubmissionRetry() throws Exception {
    Path save = Files.createTempDirectory("quizpet-app-retry-");
    AppController app = new AppController(save);
    User user = app.createUser("저장재시도");
    app.createPet("재시도펫", "DUCK");
    app.selectCategory("SCIENCE");
    QuizSession session = app.startQuiz();
    String answer = session.getCurrentQuestion().getAnswer();
    Files.createDirectory(save.resolve("record.txt"));
    try {
      app.submitAnswer(answer);
      throw new AssertionError("record write failure ignored");
    } catch (IOException expected) {
      assert session.getAnsweredCount() == 0;
      assert visible(app, QuizPanel.class);
    }
    expectState(() -> app.submitAnswer(wrongAnswer(session.getCurrentQuestion())));
    Files.delete(save.resolve("record.txt"));
    app.submitAnswer(answer);
    app.submitAnswer(answer);
    app.nextQuestion();
    app.endQuiz();
    assert session.getAnsweredCount() == 1;
    assert new RecordRepository(save.resolve("record.txt")).findByUserId(user.getUserId()).size() == 1;
    assert new PetRepository(save.resolve("pet.txt")).findByUserId(user.getUserId()).orElseThrow().getTotalXp() == 10;
  }

  private static void failedSubmissionExitRetry() throws Exception {
    Path save = Files.createTempDirectory("quizpet-exit-retry-");
    AppController app = new AppController(save);
    User user = app.createUser("종료재시도");
    app.createPet("재시도펫", "DUCK");
    app.selectCategory("SCIENCE");
    QuizSession session = app.startQuiz();
    String answer = session.getCurrentQuestion().getAnswer();
    Files.createDirectory(save.resolve("record.txt"));
    try {
      app.submitAnswer(answer);
      throw new AssertionError("save failure ignored");
    } catch (IOException expected) { }
    try {
      app.endQuizSession();
      throw new AssertionError("quiz ended before pending answer was saved");
    } catch (IOException expected) { }
    assert !session.isFinished();
    assert session.getAnsweredCount() == 0;
    assert visible(app, QuizPanel.class);
    Files.delete(save.resolve("record.txt"));
    app.endQuizSession();
    assert visible(app, QuizPanel.class) : "탭 이탈 준비 중 결과 화면이 표시됨";
    assert session.isFinished();
    assert session.getAnsweredCount() == 1;
    assert new RecordRepository(save.resolve("record.txt")).findByUserId(user.getUserId()).size() == 1;
    assert new PetRepository(save.resolve("pet.txt")).findByUserId(user.getUserId()).orElseThrow().getTotalXp() == 10;
    app.showHome();
    assert !findVisible(app.getView(), HomePanel.class).getStatusPanel().getStage().isCelebrating();
    app.showStart();
    assert visible(app, StartPanel.class);
  }

  private static void topBarNavigation() throws Exception {
    AppController app = new AppController(Files.createTempDirectory("quizpet-topbar-"));
    var bar = app.getTopBar();
    assert !bar.isVisible();
    app.createUser("탭사용자");
    assert !bar.isVisible();
    app.createPet("탭펫", "DUCK");
    assert bar.isVisible();
    assert javax.swing.SwingUtilities.isDescendingFrom(bar, app.getWindowContent());
    assert bar.getSelectedTab().equals("HOME");
    bar.getTab("LEARN").doClick();
    assert visible(app, CategoryPanel.class);
    app.selectCategory("SCIENCE");
    assert bar.getSelectedTab().equals("LEARN");
    bar.getTab("WRONG_NOTE").doClick();
    assert visible(app, HistoryPanel.class);
    assert bar.getSelectedTab().equals("WRONG_NOTE");
    bar.getTab("HISTORY").doClick();
    assert bar.getSelectedTab().equals("HISTORY");
    bar.getTab("BATTLE").doClick();
    assert visible(app, BossBattlePanel.class);
    assert bar.getSelectedTab().equals("BATTLE");
    app.showHome();
    assert bar.getSelectedTab().equals("HOME");
    app.showStart();
    assert !bar.isVisible();
  }

  private static void battleStopsOnNavigation() throws Exception {
    AppController app = new AppController(Files.createTempDirectory("quizpet-battle-navigation-"));
    User user = app.createUser("전투사용자");
    app.createPet("전투펫", "DUCK");
    for (String destination : List.of("HOME", "LEARN", "HISTORY", "WRONG_NOTE", "USER")) {
      app.getTopBar().getTab("BATTLE").doClick();
      BossBattlePanel panel = findVisible(app.getView(), BossBattlePanel.class);
      assert clickVisible(panel, "도전하기");
      var timerField = BossBattlePanel.class.getDeclaredField("timer");
      var effectField = BossBattlePanel.class.getDeclaredField("effectTimer");
      var battleField = BossBattlePanel.class.getDeclaredField("battle");
      timerField.setAccessible(true);
      effectField.setAccessible(true);
      battleField.setAccessible(true);
      var timer = (javax.swing.Timer) timerField.get(panel);
      var effectTimer = (javax.swing.Timer) effectField.get(panel);
      var battle = (BattleSession) battleField.get(panel);
      try {
        assert timer.isRunning() && effectTimer.isRunning();
        if (destination.equals("USER")) {
          assert clickVisible(app.getTopBar(), "사용자 변경");
        } else {
          app.getTopBar().getTab(destination).doClick();
        }
        assert !visible(app, BossBattlePanel.class);
        assert !timer.isRunning() : destination + " 이동 후 전투 타이머 실행 중";
        assert !effectTimer.isRunning() : destination + " 이동 후 효과 타이머 실행 중";
        assert battle.getState() == BattleSession.State.ABANDONED;
      } finally {
        panel.stopBattle();
      }
    }
    app.selectUser(user.getUserId());
    app.getTopBar().getTab("BATTLE").doClick();
    BossBattlePanel panel = findVisible(app.getView(), BossBattlePanel.class);
    assert clickVisible(panel, "도전하기") : "재진입 후 새 전투를 시작할 수 없음";
    app.showHome();
    panel.stopBattle();
  }

  private static void homeReviewCounts() throws Exception {
    Path save = Files.createTempDirectory("quizpet-home-review-");
    AppController app = new AppController(save);
    User user = app.createUser("복습집계");
    app.createPet("복습펫", "DUCK");
    var questions = new QuestionRepository().findByCategory(Category.SCIENCE);
    RecordRepository records = new RecordRepository(save.resolve("record.txt"));
    var time = java.time.LocalDateTime.of(2026, 11, 28, 12, 0);
    String again = questions.get(0).getQuestionId();
    String ambiguous = questions.get(1).getQuestionId();
    String completed = questions.get(2).getQuestionId();
    records.save(new LearningRecord("RIGHT", user.getUserId(), again, "S1", "O", true, 0, time));
    records.save(new LearningRecord("WRONG", user.getUserId(), again, "S2", "X", false, 0, time.plusSeconds(1)));
    app.showHome();
    HomePanel home = findVisible(app.getView(), HomePanel.class);
    assert home.getWrongCountText().contains("1개") : home.getWrongCountText();

    records.save(new LearningRecord("AMB-R", user.getUserId(), ambiguous, "S3", "O", true, 0, time));
    records.save(new LearningRecord("AMB-W", user.getUserId(), ambiguous, "S4", "X", false, 0, time));
    app.showHome();
    assert home.getWrongCountText().contains("2개") : home.getWrongCountText();

    records.save(new LearningRecord("UNKNOWN", user.getUserId(), "MISSING", "S5", "X", false, 0, time));
    records.save(new LearningRecord("DONE-W", user.getUserId(), completed, "S6", "X", false, 0, time));
    records.save(new LearningRecord("DONE-R", user.getUserId(), completed, "S7", "O", true, 0, time.plusSeconds(1)));
    records.save(new LearningRecord("OTHER", "OTHER-USER", questions.get(3).getQuestionId(),
        "S8", "X", false, 0, time));
    app.showHome();
    assert home.getWrongCountText().contains("3개") : home.getWrongCountText();
    assert texts(home).contains("미분류 1") : texts(home);
    assert clickVisible(app.getView(), "틀린 문제 다시 보기") : "복습 버튼이 숨겨짐";
    assert findVisible(app.getView(), javax.swing.JTable.class).getRowCount() == 3;

    records.save(new LearningRecord("DONE-AGAIN", user.getUserId(), again, "S9", "O", true, 0, time.plusSeconds(2)));
    records.save(new LearningRecord("DONE-AMB", user.getUserId(), ambiguous, "S10", "O", true, 0, time.plusSeconds(2)));
    app.showHome();
    assert home.getWrongCountText().contains("1개") : home.getWrongCountText();
    assert clickVisible(app.getView(), "틀린 문제 다시 보기") : "미분류 오답만 있어도 복습 버튼 표시";
    assert findVisible(app.getView(), javax.swing.JTable.class).getRowCount() == 1;
  }

  private static void unavailableCategoryProgress() throws Exception {
    Path save = Files.createTempDirectory("quizpet-category-unavailable-");
    AppController app = new AppController(save);
    User user = app.createUser("분야집계");
    app.createPet("집계펫", "DUCK");
    var records = new RecordRepository(save.resolve("record.txt"));
    var time = java.time.LocalDateTime.of(2026, 11, 28, 12, 0);
    String science = new QuestionRepository().findByCategory(Category.SCIENCE).get(0).getQuestionId();
    records.save(new LearningRecord("KNOWN", user.getUserId(), science, "S1", "O", true, 0, time));
    app.showHome();
    HomePanel home = findVisible(app.getView(), HomePanel.class);
    assert home.getProgressCard(Category.SCIENCE).getCaptionText().equals("100% · 1문제");
    assert home.getProgressCard(Category.SPORTS).getCaptionText().equals("아직 시작 전");
    records.save(new LearningRecord("MISSING", user.getUserId(), "UNKNOWN", "S2", "X", false, 0, time));
    app.showHome();
    for (Category category : Category.values()) {
      var card = home.getProgressCard(category);
      assert card.getCaptionText().equals("분야 집계 불가") : card.getCaptionText();
      assert !card.getBar().isVisible() : "집계 불가를 0% 막대로 표시함";
    }
    Files.writeString(save.resolve("record.txt"), "");
    app.showHome();
    for (Category category : Category.values()) {
      var card = home.getProgressCard(category);
      assert card.getCaptionText().equals("아직 시작 전");
      assert card.getBar().isVisible();
    }
  }

  private static void rejectedSubmissionAllowsNavigation() throws Exception {
    for (String destination : List.of("home", "start", "end", "user", "answer")) {
      Path save = Files.createTempDirectory("quizpet-app-rejected-");
      AppController app = new AppController(save);
      User other = app.createUser("다른사용자");
      app.createPet("정상펫", "DUCK");
      User user = app.createUser("한계사용자");
      Pet pet = app.createPet("한계펫", "DUCK");
      new PetRepository(save.resolve("pet.txt")).save(new Pet(pet.getPetId(), user.getUserId(),
          pet.getName(), pet.getAppearance(), 0, new PetStats(10, Integer.MAX_VALUE, 10, 10)));
      Files.writeString(save.resolve("record.txt"), "");
      byte[] beforePet = Files.readAllBytes(save.resolve("pet.txt"));
      byte[] beforeRecords = Files.readAllBytes(save.resolve("record.txt"));
      app.selectCategory("SCIENCE");
      QuizSession session = app.startQuiz();
      try {
        app.submitAnswer(session.getCurrentQuestion().getAnswer());
        throw new AssertionError("overflow accepted");
      } catch (IllegalArgumentException expected) { }
      assert session.getAnsweredCount() == 0;
      assert java.util.Arrays.equals(beforePet, Files.readAllBytes(save.resolve("pet.txt")));
      assert java.util.Arrays.equals(beforeRecords, Files.readAllBytes(save.resolve("record.txt")));
      assert !Files.exists(save.resolve("submission-pending.txt"));
      switch (destination) {
        case "home" -> { app.showHome(); assert visible(app, HomePanel.class); }
        case "start" -> { app.showStart(); assert visible(app, StartPanel.class); }
        case "end" -> {
          app.endQuiz();
          assert visible(app, QuizResultPanel.class);
          assert session.getResult().getAnsweredCount() == 0;
        }
        case "user" -> { app.selectUser(other.getUserId()); assert visible(app, HomePanel.class); }
        case "answer" -> {
          assert !app.submitAnswer(wrongAnswer(session.getCurrentQuestion())).isCorrect();
          assert session.getAnsweredCount() == 1;
        }
      }
      app.selectUser(other.getUserId());
      app.selectCategory("SCIENCE");
      QuizSession normal = app.startQuiz();
      assert app.submitAnswer(normal.getCurrentQuestion().getAnswer()).getEarnedXp() == 10;
      assert new PetRepository(save.resolve("pet.txt")).findByUserId(other.getUserId()).orElseThrow().getTotalXp() == 10;
      assert new RecordRepository(save.resolve("record.txt")).findByUserId(other.getUserId()).size() == 1;
    }
  }

  private static String wrongAnswer(Question question) {
    return question instanceof OXQuestion ? (question.getAnswer().equals("O") ? "X" : "O")
        : (question.getAnswer().equals("1") ? "2" : "1");
  }

  private static void partiallySavedSubmissionRetry() throws Exception {
    for (boolean petSaved : List.of(false, true)) {
      Path save = Files.createTempDirectory("quizpet-app-partial-");
      AppController app = new AppController(save);
      User user = app.createUser("부분저장");
      Pet pet = app.createPet("복구펫", "DUCK");
      app.selectCategory("SCIENCE");
      QuizSession session = app.startQuiz();
      Question question = session.getCurrentQuestion();
      String before = Files.readString(save.resolve("pet.txt")).strip();
      String after = pet.getPetId() + "|" + user.getUserId() + "|복구펫|DUCK|10|10|11|10|10";
      Files.writeString(save.resolve("submission-pending.txt"), "QUIZPET_SUBMISSION_V1\n"
          + before + "\n" + after + "\nR-PARTIAL|" + user.getUserId() + "|"
          + question.getQuestionId() + "|" + session.getSessionId() + "|" + question.getAnswer()
          + "|true|10|2026-11-16T20:00:00\n");
      if (petSaved) Files.writeString(save.resolve("pet.txt"), after + "\n");
      Files.createDirectory(save.resolve("record.txt"));
      try {
        app.submitAnswer(question.getAnswer());
        throw new AssertionError("partial save failure ignored");
      } catch (IOException expected) { }
      assert session.getAnsweredCount() == 0;
      assert Files.exists(save.resolve("submission-pending.txt"));
      assert new PetRepository(save.resolve("pet.txt")).findById(pet.getPetId()).orElseThrow().getTotalXp()
          == (petSaved ? 10 : 0);
      expectState(() -> app.submitAnswer(wrongAnswer(question)));
      try {
        app.showHome();
        throw new AssertionError("unresolved submission abandoned");
      } catch (IOException expected) { }
      Files.delete(save.resolve("record.txt"));
      app.endQuiz();
      assert visible(app, QuizResultPanel.class);
      assert session.getAnsweredCount() == 1;
      assert !Files.exists(save.resolve("submission-pending.txt"));
      assert app.submitAnswer(question.getAnswer()).getRecordId().equals("R-PARTIAL");
      assert new RecordRepository(save.resolve("record.txt")).findByUserId(user.getUserId()).size() == 1;
      Pet restored = new PetRepository(save.resolve("pet.txt")).findById(pet.getPetId()).orElseThrow();
      assert restored.getTotalXp() == 10 && restored.getStats().getIntelligence() == 11;
    }
  }

  private static javax.swing.JButton findButton(Component root, String text) {
    if (!root.isVisible()) return null;
    if (root instanceof javax.swing.JButton button && button.getText().equals(text)) return button;
    if (root instanceof Container container) {
      for (Component child : container.getComponents()) {
        javax.swing.JButton found = findButton(child, text);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static void loadErrors() throws Exception {
    Path root = Files.createTempDirectory("quizpet-missing-content-");
    try {
      new AppController(root.resolve("save"), root.resolve("content"));
      throw new AssertionError("missing content accepted");
    } catch (IOException expected) {
      assert !Files.exists(root.resolve("save/user.txt"));
    }
  }

  private static boolean visible(AppController app, Class<?> type) {
    return findVisible(app.getView(), type) != null;
  }

  private static String texts(Component component) {
    String result = component instanceof javax.swing.JLabel label ? label.getText() : "";
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) result += " " + texts(child);
    }
    return result;
  }

  private static void expectState(Action action) throws Exception {
    try {
      action.run();
      throw new AssertionError("invalid state accepted");
    } catch (IllegalStateException expected) { }
  }
  private interface Action { void run() throws Exception; }
}
