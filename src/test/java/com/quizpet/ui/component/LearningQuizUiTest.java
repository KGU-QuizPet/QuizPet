package main.java.com.quizpet.ui.component;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.ui.UiStyle;

public final class LearningQuizUiTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      ImageButton image = new ImageButton("다음", "/images/pets/small/duck_happy.png", 24);
      assert image.getIcon() != null;
      assert new ImageButton("다음", "/missing.png", 24).getIcon() == null;
      int[] clicks = {0};
      image.addActionListener(e -> clicks[0]++);
      image.doClick();
      image.setEnabled(false);
      image.doClick();
      assert clicks[0] == 1;
      paint(image);
      CategoryCard card = new CategoryCard(Category.COMMON);
      assert card.getAccessibleContext().getAccessibleDescription().contains("INT");
      paint(card);

      QuizSession session = new QuizSession("S", "U", Category.SCIENCE, List.of(
          new OXQuestion("Q1", Category.SCIENCE, "C", "문제1", "O", "해설"),
          new OXQuestion("Q2", Category.SCIENCE, "C", "문제2", "O", "해설")));
      QuizProgressBar progress = new QuizProgressBar();
      progress.showSession(session);
      assert progress.getAccessibleContext().getAccessibleDescription().contains("0 / 2");
      session.submitAnswer("X");
      assert session.getAnswerResults().equals(Map.of(session.getCurrentQuestion().getQuestionId(), false));
      progress.showSession(session);
      BufferedImage submitted = paint(progress);
      assert submitted.getRGB(10, 90) == UiStyle.WRONG.getRGB();
      assert submitted.getRGB(220, 90) == UiStyle.BORDER.getRGB();
      Map<String, Boolean> snapshot = session.getAnswerResults();
      session.nextQuestion();
      session.submitAnswer("O");
      progress.showSession(session);
      assert progress.getAccessibleContext().getAccessibleDescription().contains("2 / 2");
      assert session.getCorrectCount() == 1;
      assert snapshot.size() == 1;
      BufferedImage finished = paint(progress);
      assert finished.getRGB(220, 90) == UiStyle.CORRECT.getRGB();

      MultipleChoicePanel options = new MultipleChoicePanel("문제", List.of("첫 보기", "둘", "셋", "넷"));
      List<OptionButton> buttons = find(options, OptionButton.class);
      buttons.get(1).doClick();
      assert "2".equals(options.getSelectedAnswer());
      options.showResult("2", "1");
      assert buttons.stream().noneMatch(Component::isEnabled);
      assert "정답".equals(buttons.get(0).getAccessibleContext().getAccessibleDescription());
      assert "내가 고른 오답".equals(buttons.get(1).getAccessibleContext().getAccessibleDescription());
      BufferedImage right = paint(buttons.get(0));
      BufferedImage wrong = paint(buttons.get(1));
      assert right.getRGB(20, 8) == UiStyle.CORRECT_SOFT.getRGB();
      assert wrong.getRGB(20, 8) == UiStyle.WRONG_SOFT.getRGB();
      MultipleChoicePanel longOptions = new MultipleChoicePanel("질문 ".repeat(300),
          List.of("긴 보기 ".repeat(300), "둘", "셋", "넷"));
      paint(longOptions);
      List<OptionButton> longButtons = find(longOptions, OptionButton.class);
      javax.swing.JScrollPane scroll = (javax.swing.JScrollPane) SwingUtilities.getAncestorOfClass(
          javax.swing.JScrollPane.class, longButtons.get(0));
      assert scroll.getVerticalScrollBar().getMaximum() > scroll.getViewport().getHeight();
      longButtons.get(3).scrollRectToVisible(new java.awt.Rectangle(0, 0,
          longButtons.get(3).getWidth(), longButtons.get(3).getHeight()));
      assert scroll.getViewport().getViewPosition().y > 0;
      longButtons.get(3).doClick();
      assert "4".equals(longOptions.getSelectedAnswer());

      options.setQuestion("다음 문제", List.of("가", "나", "다", "라"));
      assert options.getSelectedAnswer() == null;
      assert find(options, OptionButton.class).stream().allMatch(Component::isEnabled);

      OXQuestionPanel ox = new OXQuestionPanel("OX 문제");
      ox.showResult("X", "O");
      assert "X".equals(ox.getSelectedAnswer());
      ox.setQuestion("다음 OX 문제");
      assert ox.getSelectedAnswer() == null;
      assert find(ox, OptionButton.class).stream().allMatch(Component::isEnabled);

      AnswerFeedbackPanel feedback = new AnswerFeedbackPanel();
      feedback.showSession(session);
      feedback.showFeedback(session.getCurrentQuestion(), "O", true, 1, 10, false, true);
      assert !find(feedback, JButton.class).stream()
          .filter(b -> "학습 카드 보기".equals(b.getText())).findFirst().orElseThrow().isEnabled();
      assert find(feedback, JButton.class).stream().anyMatch(b -> "결과 보기".equals(b.getText()));
      paint(feedback);
      feedback.showFeedback(new AnswerFeedbackPanel.AnswerFeedbackView(
          true, "O", "O", "해설", "INT", 1, 10, true, false, false));
      assert find(feedback, javax.swing.JLabel.class).stream()
          .anyMatch(label -> label.getText().contains("(예정)"));
      assert find(feedback, JButton.class).stream().anyMatch(b -> "다음 문제".equals(b.getText()));
      feedback.showFeedback(new AnswerFeedbackPanel.AnswerFeedbackView(
          true, "O", "O", "해설", "INT", 0, 0, true, false));
      assert find(feedback, javax.swing.JLabel.class).stream()
          .anyMatch(label -> label.getText().contains("추가 보상은 없어요"));
    });
    System.out.println("Learning/quiz UI checks passed");
  }

  private static BufferedImage paint(Component component) {
    component.setSize(400, 180);
    if (component instanceof Container root) layout(root);
    BufferedImage image = new BufferedImage(400, 180, BufferedImage.TYPE_INT_ARGB);
    java.awt.Graphics2D g = image.createGraphics();
    component.paint(g);
    g.dispose();
    return image;
  }

  private static void layout(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }

  private static <T> List<T> find(Container root, Class<T> type) {
    List<T> found = new ArrayList<>();
    for (Component child : root.getComponents()) {
      if (type.isInstance(child)) found.add(type.cast(child));
      if (child instanceof Container c) found.addAll(find(c, type));
    }
    return found;
  }
}
