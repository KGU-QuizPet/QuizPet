package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.component.*;

public final class DesktopLayoutTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      CategoryPanel categories = new CategoryPanel(new ScreenManager());
      categories.showCategories(List.of(Category.values()));
      resize(categories, 1366, 768);
      JPanel grid = find(categories, JPanel.class).stream()
          .filter(panel -> panel.getLayout() instanceof GridLayout && panel.getComponentCount() == 5)
          .findFirst().orElseThrow();
      List<Component> cards = List.of(grid.getComponents());
      assert cards.get(0).getY() == cards.get(2).getY() : "Desktop needs three columns";
      assert cards.get(3).getY() > cards.get(0).getY();
      int width = cards.get(0).getWidth(), height = cards.get(0).getHeight();
      resize(categories, 1920, 1080);
      assert cards.get(0).getWidth() > width;
      assert cards.get(0).getHeight() > height;
      resize(categories, 480, 680);
      assert ((GridLayout) grid.getLayout()).getColumns() < 3;
      resize(categories, 320, 640);
      assert cards.get(1).getY() > cards.get(0).getY();

      MultipleChoicePanel choice = new MultipleChoicePanel("질문", List.of("하나", "둘", "셋", "넷"));
      grows(choice);
      OXQuestionPanel ox = new OXQuestionPanel("질문");
      grows(ox);
      AnswerFeedbackPanel feedback = new AnswerFeedbackPanel();
      feedback.showFeedback(new MultipleChoiceQuestion("Q", Category.SCIENCE, "C", "질문", "1",
          "해설 ".repeat(10), List.of("하나", "둘", "셋", "넷")), "2", false, 0, 0, true, false);
      grows(feedback);
      HomePanel home = new HomePanel(new ScreenManager());
      home.showPet("펫", "DUCK", 1, 0, 10, 10, 10, 10);
      resize(home, 1366, 768);
      for (javax.swing.JLabel label : find(home, javax.swing.JLabel.class)) {
        if (label.getText().contains("(STR)") || label.getText().contains("(INT)")) {
          assert label.getHeight() >= label.getFontMetrics(label.getFont()).getHeight()
              : "Home stat text is clipped";
        }
      }
      StartPanel start = new StartPanel(new ScreenManager());
      resize(start, 1024, 768);
      int listWidth = find(start, JList.class).get(0).getWidth();
      resize(start, 1920, 1080);
      assert find(start, JList.class).get(0).getWidth() > listWidth;
    });
    System.out.println("Desktop responsive layout checks passed");
  }

  private static void grows(JPanel panel) {
    resize(panel, 1024, 768);
    int oldHeight = growingArea(panel).getHeight();
    resize(panel, 1920, 1080);
    assert growingArea(panel).getHeight() > oldHeight
        : panel.getClass().getSimpleName() + " keeps fixed answer height";
  }

  private static Component growingArea(JPanel panel) {
    if (panel instanceof OXQuestionPanel) return find(panel, JToggleButton.class).get(0);
    if (panel instanceof AnswerFeedbackPanel) return find(panel, JTextArea.class).get(0);
    return find(panel, JScrollPane.class).stream()
        .filter(scroll -> scroll.getViewport().getView() instanceof JList).findFirst().orElseThrow();
  }

  private static void resize(JPanel panel, int width, int height) {
    panel.setSize(width, height);
    for (int i = 0; i < 8; i++) layout(panel);
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
