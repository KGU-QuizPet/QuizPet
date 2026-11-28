package main.java.com.quizpet.ui;

import java.awt.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.*;
import main.java.com.quizpet.service.ReviewPreparationService.ReviewRequest;
import main.java.com.quizpet.ui.screen.HistoryPanel;

public final class ReviewFlowTest {
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("quizpet-review-flow-");
    try {
      SwingUtilities.invokeAndWait(() -> {
        try { check(dir, args); } catch (Exception e) { throw new AssertionError(e); }
      });
    } finally {
      try (var paths = Files.walk(dir)) {
        for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
      }
    }
    System.out.println("Wrong-note review UI, mixed submissions, recovery, completion and ownership checks passed");
  }

  private static void check(Path dir, String[] args) throws Exception {
    AppController app = new AppController(dir);
    User a = app.createUser("복습사용자"); app.createPet("복습펫", "DUCK");
    User b = app.createUser("다른사용자"); app.createPet("다른펫", "TURTLE");
    app.selectUser(a.getUserId());
    QuestionRepository questions = new QuestionRepository();
    LinkedHashMap<String, Question> pool = new LinkedHashMap<>();
    for (Category c : Category.values()) { Question q = questions.findByCategory(c).get(0); pool.put(q.getQuestionId(), q); }
    for (Question q : questions.findAll()) { if (pool.size() >= 8) break; pool.put(q.getQuestionId(), q); }
    RecordRepository records = new RecordRepository(dir.resolve("record.txt"));
    int number = 0;
    for (Question q : pool.values()) save(records, a, q.getQuestionId(), false, number++);
    String completed = pool.keySet().iterator().next();
    save(records, a, completed, true, number++);
    Question old = new QuestionRepository(Path.of("data/content/question-legacy.txt")).findAll().get(0);
    save(records, a, old.getQuestionId(), false, number++);
    save(records, a, "UNKNOWN", false, number++);
    String otherOnly = questions.findAll().stream().map(Question::getQuestionId).filter(id -> !pool.containsKey(id)).findFirst().orElseThrow();
    save(records, b, otherOnly, false, number++);
    app.showWrongNote(null, false);
    JTable table = find(app.getView(), JTable.class).stream().filter(t -> t.getColumnCount() == 7).findFirst().orElseThrow();
    List<String> selected = new ArrayList<>();
    for (int row = 0; row < table.getRowCount(); row++) {
      String id = table.getValueAt(row, 6).toString();
      if (pool.containsKey(id)) { assert table.isCellEditable(row, 0); table.setValueAt(true, row, 0); selected.add(id); }
      else {
        assert !table.isCellEditable(row, 0) : "레거시·누락 문제는 조회만 허용합니다.";
        table.setRowSelectionInterval(row, row);
        assert find(app.getView(), JTextArea.class).stream().anyMatch(t -> t.getText().contains("복습 불가:"));
      }
    }
    JButton review = find(app.getView(), JButton.class).stream().filter(x -> x.getText().startsWith("복습하기")).findFirst().orElseThrow();
    assert review.isEnabled() && review.getText().contains("8");
    render(app, args, "selection");
    review.doClick();
    render(app, args, "quiz");
    QuizSession session = session(app);
    assert session.isReview() && session.getTotalCount() == 8;
    assert session.getQuestions().stream().map(Question::getQuestionId).toList().equals(selected);
    assert app.getTopBar().getSelectedTab().equals("WRONG_NOTE");
    String answer = session.getCurrentQuestion().getAnswer();
    Files.createDirectory(dir.resolve("submission-pending.txt"));
    try { app.submitAnswer(answer); throw new AssertionError("저장 오류 무시"); } catch (java.io.IOException expected) { }
    assert session.getAnsweredCount() == 0 && session(app) == session;
    try { app.startReview(new ReviewRequest(a.getUserId(), selected)); throw new AssertionError(); }
    catch (IllegalStateException expected) { }
    assert session(app) == session;
    Files.delete(dir.resolve("submission-pending.txt"));
    int expectedXp = 0;
    for (Question q : session.getQuestions()) {
      LearningRecord saved = app.submitAnswer(q.getAnswer());
      expectedXp += q.getQuestionId().equals(completed) ? 0 : 10;
      assert saved.getEarnedXp() == (q.getQuestionId().equals(completed) ? 0 : 10);
      app.showFeedbackCard();
      assert app.getTopBar().getSelectedTab().equals("WRONG_NOTE");
      app.backFromLearning();
      app.nextQuestion();
    }
    assert session.isFinished();
    render(app, args, "result");
    assert new PetRepository(dir.resolve("pet.txt")).findByUserId(a.getUserId()).orElseThrow().getTotalXp() == expectedXp;
    find(app.getView(), JButton.class).stream().filter(x -> x.getText().equals("오답노트로")).findFirst().orElseThrow().doClick();
    assert find(app.getView(), JTable.class).get(0).getRowCount() == 10;
    var wrong = new WrongAnswerService(new RecordService(records), new RecordQuestionLookup(questions, Path.of("data/content/question-legacy.txt")));
    assert wrong.getWrongAnswers(a.getUserId()).stream().filter(w -> pool.containsKey(w.questionId()))
        .allMatch(w -> w.reviewStatus() == WrongAnswerService.ReviewStatus.COMPLETED);
    app.startReview(new ReviewRequest(a.getUserId(), List.of(completed)));
    Question repeat = session(app).getCurrentQuestion();
    String incorrect = repeat.getAnswer().equals("O") ? "X" : "O";
    app.submitAnswer(incorrect);
    app.nextQuestion();
    find(app.getView(), JButton.class).stream().filter(x -> x.getText().equals("오답노트로")).findFirst().orElseThrow().doClick();
    assert wrong.getWrongAnswers(a.getUserId()).stream().filter(w -> w.questionId().equals(completed)).findFirst()
        .orElseThrow().reviewStatus() == WrongAnswerService.ReviewStatus.NEEDS_REVIEW;
    QuizSession preserved = session(app);
    byte[] beforeRecords = Files.readAllBytes(dir.resolve("record.txt"));
    byte[] beforePet = Files.readAllBytes(dir.resolve("pet.txt"));
    for (String id : List.of("UNKNOWN", old.getQuestionId(), otherOnly)) {
      try { app.startReview(new ReviewRequest(a.getUserId(), List.of(id))); throw new AssertionError("불가 항목 허용"); }
      catch (IllegalArgumentException expected) { }
      assert session(app) == preserved : "생성 실패 시 기존 세션을 보존합니다.";
    }
    app.showWrongNote("HISTORY", true);
    String historyId = pool.values().stream().filter(q -> q.getCategory() == Category.HISTORY).findFirst().orElseThrow().getQuestionId();
    app.startReview(new ReviewRequest(a.getUserId(), List.of(historyId)));
    app.endQuiz();
    find(app.getView(), JButton.class).stream().filter(x -> x.getText().equals("오답노트로")).findFirst().orElseThrow().doClick();
    HistoryPanel history = find(app.getView(), HistoryPanel.class).get(0);
    assert history.getWrongCategoryId().equals("HISTORY") && history.isNeedsReviewOnly();
    app.selectUser(b.getUserId());
    try { app.startReview(new ReviewRequest(a.getUserId(), selected)); throw new AssertionError("사용자 변경 무시"); }
    catch (IllegalArgumentException expected) { }
    assert Arrays.equals(beforeRecords, Files.readAllBytes(dir.resolve("record.txt")));
    assert Arrays.equals(beforePet, Files.readAllBytes(dir.resolve("pet.txt")));
  }

  private static void render(AppController app, String[] args, String state) throws Exception {
    if (args.length == 0) return;
    for (int[] size : new int[][] {{1280, 800}, {1024, 680}}) {
      JPanel window = app.getWindowContent();
      window.setSize(size[0], size[1]);
      for (int i = 0; i < 4; i++) layout(window);
      java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size[0], size[1], java.awt.image.BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics(); window.printAll(g); g.dispose();
      javax.imageio.ImageIO.write(image, "png", Path.of(args[0], "review-" + state + "-" + size[0] + ".png").toFile());
    }
  }
  private static void layout(Container root) {
    root.invalidate();
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }

  private static QuizSession session(AppController app) throws Exception {
    Field f = AppController.class.getDeclaredField("session"); f.setAccessible(true); return (QuizSession) f.get(app);
  }
  private static void save(RecordRepository records, User user, String id, boolean correct, int seq) throws Exception {
    records.save(new LearningRecord("SEED" + seq, user.getUserId(), id, "SEED" + seq, "X", correct, 0,
        LocalDateTime.of(2020, 1, 1, 0, 0).plusSeconds(seq)));
  }
  private static <T> List<T> find(Component root, Class<T> type) {
    List<T> found = new ArrayList<>(); if (!root.isVisible()) return found;
    if (type.isInstance(root)) found.add(type.cast(root));
    if (root instanceof Container c) for (Component child : c.getComponents()) found.addAll(find(child, type));
    return found;
  }
}
