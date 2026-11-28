package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.*;
import main.java.com.quizpet.ui.AppController;
import main.java.com.quizpet.ui.component.*;

/** 실제 콘텐츠·저장·보상 경로와 FE2/PR #88 경계를 함께 검증합니다. */
public final class Fe2IntegrationTest {
  public static void main(String[] args) throws Exception {
    Path root = Files.createTempDirectory("quizpet-fe2-final-");
    try {
      SwingUtilities.invokeAndWait(() -> {
        try { verify(root, args); verifyEmptyContent(root); }
        catch (Exception e) { throw new RuntimeException(e); }
      });
    } finally {
      try (var paths = Files.walk(root)) {
        for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
    System.out.println("Fe2IntegrationTest passed: real data, all question types, 9-question review, user switch, timers and layouts");
  }

  private static void verify(Path root, String[] args) throws Exception {
    Path save = root.resolve("save");
    AppController app = new AppController(save);
    User first = app.createUser("첫 사용자");
    app.createPet("첫 펫", "DUCK");
    JPanel window = app.getWindowContent();
    PetStatusPanel status = LearningDesignTest.find(window, PetStatusPanel.class).stream()
        .filter(panel -> panel.getParent() == window).findFirst().orElseThrow();
    CategoryPanel categories = LearningDesignTest.find(app.getView(), CategoryPanel.class).get(0);
    LearningPanel learning = LearningDesignTest.find(app.getView(), LearningPanel.class).get(0);
    QuizPanel quiz = LearningDesignTest.find(app.getView(), QuizPanel.class).get(0);
    HomePanel home = LearningDesignTest.find(app.getView(), HomePanel.class).get(0);
    window.addNotify();
    try {
      app.showCategories();
      assert status.isVisible() && status.getStage().getMood() == PetMood.IDLE;
      assert category(categories, "과학").getProgressText().equals("NEW");
      assert timer(status, "messageTimer").isRunning();
      assert timer(status.getStage(), "ambientTimer").isRunning();
      Timer messages = timer(status, "messageTimer");
      String initial = status.getStage().getMessage();
      for (var action : messages.getActionListeners()) action.actionPerformed(null);
      assert !initial.equals(status.getStage().getMessage());
      String rotated = status.getStage().getMessage();
      app.showCategories();
      assert status.getStage().getMessage().equals(rotated) : "Same situation preserves PR #88 rotation";
      app.showHome();
      assert !messages.isRunning() && !timer(status.getStage(), "ambientTimer").isRunning();
      assert timer(home.getStatusPanel(), "messageTimer").isRunning();
      app.showCategories();
      assert messages.isRunning() && timer(status.getStage(), "ambientTimer").isRunning();
      // Peer 없는 headless 루트의 editable text 접근성 오류를 피하고 타이머 검증과 입력 검증을 분리한다.
      window.removeNotify();

      category(categories, "과학").doClick();
      assert learning.isVisible() && status.getStage().getMood() == PetMood.FOCUS;
      capture(window, args, "UI-05-final.png");
      press(learning, "learning.next");
      assert LearningDesignTest.button(learning, "이전").isEnabled();
      press(learning, "learning.previous");
      assert !LearningDesignTest.button(learning, "이전").isEnabled();
      press(learning, "learning.quiz");
      assert quiz.isVisible();
      // 일반 퀴즈는 실제 세션을 검사하고 기존 제출 버튼 경로로 진행한다.
      var sessionField = AppController.class.getDeclaredField("session");
      sessionField.setAccessible(true);
      QuizSession normal = (QuizSession) sessionField.get(app);
      boolean firstQuestion = true;
      while (true) {
        Question question = normal.getCurrentQuestion();
        submitWithKeyboard(quiz, question, firstQuestion ? wrong(question) : question.getAnswer());
        assert normal.isCurrentQuestionAnswered();
        assert !status.isVisible() && !timer(status, "messageTimer").isRunning();
        assert !timer(status.getStage(), "ambientTimer").isRunning();
        capture(window, args, firstQuestion ? "UI-06D-final.png" : "UI-06C-final.png");
        if (firstQuestion) {
          app.showFeedbackCard();
          assert learning.isVisible() && status.isVisible();
          assert !LearningDesignTest.button(learning, "퀴즈 시작").isEnabled();
          press(learning, "learning.quiz");
          assert sessionField.get(app) == normal : "Related card must not replace the session";
          app.backFromLearning();
        }
        firstQuestion = false;
        if (normal.isFinished()) { app.nextQuestion(); break; }
        app.nextQuestion();
      }

      QuestionRepository questions = new QuestionRepository();
      List<Question> selected = new ArrayList<>();
      for (Category category : List.of(Category.SCIENCE, Category.HISTORY, Category.LANGUAGE)) {
        for (String type : List.of("OX", "MULTIPLE_CHOICE", "SA")) {
          selected.add(questions.findAll().stream().filter(q -> q.getCategory() == category && q.getType().equals(type))
              .findFirst().orElseThrow());
        }
      }
      var ids = selected.stream().map(Question::getQuestionId).toList();
      QuizSession seed = new QuizService(questions).startReviewSession(first.getUserId(), ids);
      SubmissionService submissions = new SubmissionService(save);
      while (true) {
        submissions.submit(seed, wrong(seed.getCurrentQuestion()));
        if (seed.isFinished()) break;
        seed.nextQuestion();
      }
      app.showCategories();
      assert category(categories, "역사").getProgressText().equals("정답률 0.0%") : "Wrong-only history is not NEW";
      app.showWrongNote(null, false);
      QuizSession review = app.startReview(new ReviewPreparationService.ReviewRequest(first.getUserId(), ids));
      assert review.isReview() && review.getTotalCount() == 9;
      Set<String> rendered = new HashSet<>();
      while (true) {
        Question question = review.getCurrentQuestion();
        assert status.isVisible() && status.getStage().getMood() == PetMood.FOCUS;
        assert app.getTopBar().getSelectedTab().equals("WRONG_NOTE");
        if (rendered.add(question.getType())) capture(window, args, "UI-06-" + question.getType() + "-final.png");
        submitWithKeyboard(quiz, question, question.getAnswer());
        assert review.isCurrentQuestionAnswered();
        if (review.isFinished()) { app.nextQuestion(); break; }
        app.nextQuestion();
      }
      assert rendered.size() == 3 && review.getAnsweredCount() == 9;
      app.showCategories();
      Pet pet = new PetRepository(save.resolve("pet.txt")).findByUserId(first.getUserId()).orElseThrow();
      assert status.getLevelText().equals("Lv." + pet.getLevel());
      assert status.getExpText().equals("EXP " + pet.getXpInLevel() + " / " + Pet.XP_PER_LEVEL);
      assert status.getStatText("INT").equals(String.valueOf(pet.getStats().getIntelligence()));
      assert status.getStatText("FOC").equals(String.valueOf(pet.getStats().getFocus()));
      assert category(categories, "역사").getProgressText().equals("정답률 50.0%");
      assert category(categories, "스포츠").getProgressText().equals("NEW");
      for (int[] size : new int[][] {{1280,800},{1600,900},{1024,768},{800,650}}) {
        LearningDesignTest.resize(window, size[0], size[1]);
        assert status.getWidth() == 320;
        if (size[0] == 1280) capture(window, args, "UI-04-final.png");
      }

      byte[] original = Files.readAllBytes(save.resolve("record.txt"));
      try {
        new RecordRepository(save.resolve("record.txt")).save(new LearningRecord("UNKNOWN", first.getUserId(),
            "DELETED", "OLD", "O", false, 0, LocalDateTime.now()));
        app.showCategories();
        assert LearningDesignTest.find(categories, CategoryCard.class).stream()
            .allMatch(card -> card.getProgressText().equals("집계 불가") && card.isEnabled());
        Files.writeString(save.resolve("record.txt"), "broken\n");
        app.showCategories();
        assert LearningDesignTest.find(categories, CategoryCard.class).stream()
            .allMatch(card -> card.getProgressText().equals("집계 불가"));
      } finally { Files.write(save.resolve("record.txt"), original); }

      app.showHome();
      assert !status.isVisible() && !timer(status, "messageTimer").isRunning();
      assert home.isVisible() && home.getStatusPanel().getStage().hasImage();
      app.showHistory(null);
      assert !status.isVisible();
      app.showWrongNote(null, false);
      assert !status.isVisible();
      app.showBossBattle();
      assert !status.isVisible();
      app.createUser("두 번째 사용자");
      assert !status.isVisible();
      app.createPet("두 번째 펫", "TURTLE");
      app.showCategories();
      assert status.getLevelText().equals("Lv.1") && status.getExpBar().getValue() == 0;
      assert status.getStatText("INT").equals("10");
      assert LearningDesignTest.find(status, javax.swing.JLabel.class).stream().noneMatch(label -> "첫 펫".equals(label.getText()));
      assert LearningDesignTest.find(categories, CategoryCard.class).stream().allMatch(card -> card.getProgressText().equals("NEW"));
      app.selectUser(first.getUserId());
      app.showCategories();
      assert status.getLevelText().equals("Lv." + pet.getLevel());
      app.showStart();
      assert !status.isVisible() && !messages.isRunning();
    } finally { if (window.isDisplayable()) window.removeNotify(); }
  }

  private static void verifyEmptyContent(Path root) throws Exception {
    Path content = Files.createDirectory(root.resolve("empty-content"));
    for (String name : List.of("category.txt", "question.txt", "question-legacy.txt")) {
      Files.copy(Path.of("data/content", name), content.resolve(name));
    }
    Files.writeString(content.resolve("content.txt"), "");
    AppController app = new AppController(root.resolve("empty-save"), content);
    app.createUser("빈 콘텐츠"); app.createPet("빈 분야 펫", "DUCK"); app.showCategories();
    CategoryPanel panel = LearningDesignTest.find(app.getView(), CategoryPanel.class).get(0);
    for (CategoryCard card : LearningDesignTest.find(panel, CategoryCard.class)) {
      assert !card.isEnabled() && card.getProgressText().equals("준비 중");
      card.doClick(); assert panel.isVisible();
    }
  }

  private static CategoryCard category(CategoryPanel panel, String name) {
    return LearningDesignTest.find(panel, CategoryCard.class).stream().filter(card -> card.getText().equals(name)).findFirst().orElseThrow();
  }
  private static Timer timer(Object owner, String field) throws Exception {
    var member = owner.getClass().getDeclaredField(field); member.setAccessible(true); return (Timer) member.get(owner);
  }
  private static String wrong(Question q) {
    return q instanceof OXQuestion ? q.getAnswer().equals("O") ? "X" : "O"
        : q instanceof MultipleChoiceQuestion ? q.getAnswer().equals("1") ? "2" : "1" : "FE2 오답";
  }
  private static void press(JPanel panel, String key) { panel.getActionMap().get(key).actionPerformed(null); }
  private static void submitWithKeyboard(QuizPanel quiz, Question question, String answer) {
    if (question instanceof ShortAnswerQuestion) {
      JTextField field = LearningDesignTest.find(quiz, JTextField.class).get(0); field.setText(answer); field.postActionEvent();
    } else { press(quiz, "quiz.answer." + answer); press(quiz, "quiz.submit"); }
  }
  private static void capture(JPanel window, String[] args, String name) throws Exception {
    if (args.length == 0) return;
    LearningDesignTest.resize(window,1280,800); LearningDesignTest.render(window,Path.of(args[0],name));
  }
}
