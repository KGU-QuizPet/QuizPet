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
        loadErrors();
        unavailableContent();
        failedSubmissionRetry();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("AppController integration checks passed");
  }

  private static void creationBackRefresh() throws Exception {
    AppController app = new AppController(Files.createTempDirectory("quizpet-creation-back-"));
    User created = app.createUser("돌아가기");
    clickVisible(app.getView(), "사용자 선택으로");
    assert visible(app, StartPanel.class);
    javax.swing.JList<?> list = findVisible(app.getView(), javax.swing.JList.class);
    assert list.getModel().getSize() == 1 : "created user missing after returning to start";
    assert ((Profile) list.getModel().getElementAt(0)).getUser().getUserId().equals(created.getUserId());
    list.setSelectedIndex(0);
    clickVisible(app.getView(), "시작");
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
    assert new PetRepository(save.resolve("pet.txt")).findById(pet.getPetId()).orElseThrow().getLevel() == 5;

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
    Files.delete(save.resolve("record.txt"));
    app.submitAnswer(answer);
    app.submitAnswer(answer);
    app.nextQuestion();
    app.endQuiz();
    assert session.getAnsweredCount() == 1;
    assert new RecordRepository(save.resolve("record.txt")).findByUserId(user.getUserId()).size() == 1;
    assert new PetRepository(save.resolve("pet.txt")).findByUserId(user.getUserId()).orElseThrow().getTotalXp() == 10;
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
    for (Component component : app.getView().getComponents()) {
      if (type.isInstance(component)) return component.isVisible();
    }
    throw new AssertionError("screen missing: " + type);
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
