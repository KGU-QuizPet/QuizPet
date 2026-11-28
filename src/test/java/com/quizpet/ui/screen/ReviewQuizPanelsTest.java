package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.ui.ScreenManager;

public final class ReviewQuizPanelsTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      List<Question> questions = List.of(
          new OXQuestion("OX", Category.SCIENCE, "C1", "과학 문제", "O", "해설"),
          new MultipleChoiceQuestion("MC", Category.HISTORY, "C2", "역사 문제", "2", "해설", List.of("가", "나", "다", "라")),
          new ShortAnswerQuestion("SA", Category.LANGUAGE, "C3", "언어 문제", "hello", "해설", List.of()));
      QuizSession review = QuizSession.review("REVIEW", "U", questions);
      QuizPanel quiz = new QuizPanel(new ScreenManager());
      for (Question question : questions) {
        quiz.showSession(review);
        assert labels(quiz).stream().anyMatch(text -> text.contains("복습 · " + question.getCategory().getName()));
        review.submitAnswer(question.getAnswer());
        if (!review.isFinished()) review.nextQuestion();
      }
      QuizResultPanel result = new QuizResultPanel();
      result.showReviewResult(review.getResult(), 30, 3, 1, 1, 30);
      assert labels(result).contains("합계 +3");
      assert labels(result).stream().anyMatch(text -> text.contains("오답 복습 · 3문제"));
      assert buttons(result).contains("오답노트로");
      QuizSession partial = QuizSession.review("PARTIAL", "U", questions);
      partial.submitAnswer("O");
      partial.end();
      result.showReviewResult(partial.getResult(), 10, 1, 1, 1, 10);
      assert labels(result).stream().anyMatch(text -> text.contains("3문제 중 1문제 풀이"));
      result.showResult(Category.SCIENCE, partial.getResult(), 10, 1, 1, 1, 10);
      assert buttons(result).contains("학습 기록 보기");
      assert labels(result).contains("INT +1");
    });
    System.out.println("Mixed review question labels, full and partial result display checks passed");
  }

  private static List<String> labels(Component root) { return texts(root, false); }
  private static List<String> buttons(Component root) { return texts(root, true); }
  private static List<String> texts(Component root, boolean buttons) {
    List<String> texts = new ArrayList<>();
    if (!buttons && root instanceof JLabel label) texts.add(label.getText());
    if (buttons && root instanceof JButton button) texts.add(button.getText());
    if (root instanceof Container c) for (Component child : c.getComponents()) texts.addAll(texts(child, buttons));
    return texts;
  }
}
