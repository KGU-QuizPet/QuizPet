package main.java.com.quizpet.ui;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.*;
import main.java.com.quizpet.ui.component.AnswerFeedbackPanel;
import main.java.com.quizpet.ui.component.OXQuestionPanel;
import main.java.com.quizpet.ui.screen.QuizPanel;

public final class ShortAnswerQuizTest {
  public static void main(String[] args) throws Exception {
    Path root = Files.createTempDirectory("quizpet-short-answer-");
    try {
      Path file = root.resolve("questions.txt");
      Files.writeString(file, "Q|SCIENCE|C|SA|이산화탄소|CO2;CO₂\n기체 이름은?\n해설\n");
      Question question = new QuestionRepository(file).findById("Q").orElseThrow(
          () -> new AssertionError("SA question was skipped"));
      assert question.getType().equals("SA");
      assert question.checkAnswer(" 이산화 탄소 ");
      assert question.checkAnswer(" cO2 ");
      assert question.checkAnswer("CO₂");
      assert !question.checkAnswer("산소");
      for (String invalid : new String[] {null, "", "　 ", "CO|2", "CO\n2", "CO\r2", "CO\u20282"}) {
        try { question.checkAnswer(invalid); throw new AssertionError("Invalid answer accepted: " + invalid); }
        catch (IllegalArgumentException expected) { }
      }
      SwingUtilities.invokeAndWait(() -> checkUi(question));
      checkApplicationFlow(root);

      Path save = root.resolve("save");
      User user = new UserService(new UserRepository(save.resolve("user.txt"))).createUser("단답사용자");
      new PetService(new PetRepository(save.resolve("pet.txt")), new UserRepository(save.resolve("user.txt")))
          .createPet(user.getUserId(), "단답펫", "DUCK");
      SubmissionService submissions = new SubmissionService(save);
      QuizSession session = session(user.getUserId(), "S", question);
      LearningRecord saved = submissions.submit(session, " cO2 ");
      assert saved.isCorrect() && saved.getEarnedXp() == 10;
      assert submissions.submit(session, " cO2 ").getRecordId().equals(saved.getRecordId());
      assert submissions.submit(session(user.getUserId(), "S2", question), "CO₂").getEarnedXp() == 0;
      assert !submissions.submit(session(user.getUserId(), "S3", question), "산소").isCorrect();
      List<LearningRecord> records = new RecordRepository(save.resolve("record.txt")).findByUserId(user.getUserId());
      assert records.size() == 3 && records.get(0).getSelectedAnswer().equals(" cO2 ");
      assert new PetRepository(save.resolve("pet.txt")).findByUserId(user.getUserId()).orElseThrow().getTotalXp() == 10;
      assert session.getAnswerResults().get("Q");
      for (String invalid : List.of("", "A|B", "A\nB")) {
        try { submissions.submit(session(user.getUserId(), "INVALID", question), invalid); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
      }
      assert new RecordRepository(save.resolve("record.txt")).findAll().size() == 3;
      assert !Files.exists(save.resolve("submission-pending.txt"));
      Files.writeString(file, "Q|SCIENCE|C|SA|정답|별칭;\n문제\n해설\n");
      assert new QuestionRepository(file).findAll().isEmpty() : "Empty alias accepted";
      Files.writeString(file, "Q|SCIENCE|C|SA|정답|\n문제\n해설\n");
      assert new QuestionRepository(file).findAll().get(0).checkAnswer("정답");
    } finally {
      try (var paths = Files.walk(root)) {
        for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
      }
    }
    System.out.println("Short-answer loading, grading, UI and persistence checks passed");
  }

  private static void checkApplicationFlow(Path root) throws Exception {
    Path content = root.resolve("content");
    Files.createDirectories(content);
    for (String name : List.of("category.txt", "content.txt")) {
      Files.copy(Path.of("data/content").resolve(name), content.resolve(name));
    }
    Files.writeString(content.resolve("question.txt"),
        "Q|SCIENCE|C-SCI-01|SA|이산화탄소|CO2;CO₂\n기체 이름은?\n해설\n");
    Path save = root.resolve("app-save");
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppController app = new AppController(save, content);
        User user = app.createUser("입력검증");
        app.createPet("단답펫", "DUCK");
        app.selectCategory("SCIENCE");
        QuizSession session = app.startQuiz();
        QuizPanel quiz = find(app.getView(), QuizPanel.class).get(0);
        find(quiz, JTextField.class).get(0).setText(" co2 ");
        find(quiz, JButton.class).stream().filter(b -> b.getText().equals("답안 제출"))
            .findFirst().orElseThrow().doClick();
        assert session.isFinished() && session.getCorrectCount() == 1;
        AnswerFeedbackPanel feedback = find(app.getView(), AnswerFeedbackPanel.class).get(0);
        assert feedback.isVisible();
        assert find(feedback, JLabel.class).stream().anyMatch(l -> l.getText().contains("이산화탄소"));
        app.showFeedbackCard();
        app.backFromLearning();
        app.nextQuestion();
        AppController restarted = new AppController(save, content);
        restarted.selectUser(user.getUserId());
        restarted.showHistory("SCIENCE");
        List<javax.swing.JTable> tables = find(restarted.getView(), javax.swing.JTable.class);
        assert tables.stream().anyMatch(table -> table.getRowCount() == 1);
        assert tables.stream().anyMatch(table -> {
          for (int row = 0; row < table.getRowCount(); row++) {
            for (int col = 0; col < table.getColumnCount(); col++) {
              if (" co2 ".equals(table.getValueAt(row, col))) return true;
            }
          }
          return false;
        }) : "History lost the submitted answer";
      } catch (Exception e) { throw new AssertionError(e); }
    });
    Path archive = root.resolve("archive.txt");
    Files.copy(content.resolve("question.txt"), archive);
    Path changed = root.resolve("changed.txt");
    Files.writeString(changed, Files.readString(archive).replace("CO2;CO₂", "CO2"));
    try {
      new RecordQuestionLookup(new QuestionRepository(changed), archive);
      throw new AssertionError("Changed SA aliases reused an archived ID");
    } catch (IllegalStateException expected) { }
  }

  private static void checkUi(Question question) {
    QuizPanel quiz = new QuizPanel(new ScreenManager());
    QuizSession session = session("U", "UI", question);
    quiz.setOnSubmit(session::submitAnswer);
    quiz.showSession(session);
    JTextField input = find(quiz, JTextField.class).get(0);
    JButton submit = find(quiz, JButton.class).stream().filter(b -> b.getText().equals("답안 제출")).findFirst().orElseThrow();
    assert !submit.isEnabled();
    input.setText("　 ");
    assert !submit.isEnabled();
    input.setText("산소");
    assert submit.isEnabled();
    input.setText("");
    assert !submit.isEnabled();
    input.setText(" cO2 ");
    submit.doClick();
    assert session.isFinished() && session.getCorrectCount() == 1;
    assert !input.isEnabled() && !submit.isEnabled();
    quiz.showSession(session("U", "NEW", question));
    assert find(quiz, JTextField.class).get(0).getText().isEmpty();
    assert !submit.isEnabled();
    AnswerFeedbackPanel feedback = new AnswerFeedbackPanel();
    feedback.showFeedback(question, "산소", false, 0, 0, true, false);
    assert find(feedback, OXQuestionPanel.class).isEmpty() : "SA rendered as OX";
    assert find(feedback, JLabel.class).stream().anyMatch(l -> l.getText().contains("산소"));
    assert find(feedback, JLabel.class).stream().anyMatch(l -> l.getText().contains("이산화탄소"));
  }

  private static QuizSession session(String user, String id, Question question) {
    return new QuizSession(id, user, question.getCategory(), List.of(question));
  }

  private static <T> List<T> find(Container root, Class<T> type) {
    List<T> result = new ArrayList<>();
    for (Component child : root.getComponents()) {
      if (type.isInstance(child)) result.add(type.cast(child));
      if (child instanceof Container container) result.addAll(find(container, type));
    }
    return result;
  }
}
