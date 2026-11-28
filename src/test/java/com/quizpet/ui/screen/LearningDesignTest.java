package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;
import main.java.com.quizpet.service.LearningService;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.component.LearningProgressBar;

public final class LearningDesignTest {
  public static void main(String[] args) throws Exception {
    var service = new LearningService();
    SwingUtilities.invokeAndWait(() -> {
      try {
        LearningPanel panel = new LearningPanel(new ScreenManager());
        var cards = service.getContents("SCIENCE");
        panel.showContents(Category.SCIENCE, cards, 0);
        var progress = find(panel, LearningProgressBar.class).get(0);
        assert progress.getAccessibleContext().getAccessibleDescription().equals("1 / " + cards.size());
        assert !button(panel, "이전").isEnabled();
        for (int i = 1; i < cards.size(); i++) button(panel, "다음").doClick();
        assert !button(panel, "다음").isEnabled();
        assert progress.getToolTipText().equals(cards.size() + " / " + cards.size());
        button(panel, "이전").doClick();
        assert button(panel, "다음").isEnabled();
        AtomicInteger quizzes = new AtomicInteger(), backs = new AtomicInteger();
        panel.setOnQuiz(quizzes::incrementAndGet);
        panel.setOnBack(backs::incrementAndGet);
        panel.setQuizEnabled(false);
        panel.showContents(Category.SCIENCE, cards, 0);
        button(panel, "퀴즈 시작").doClick();
        assert quizzes.get() == 0;
        button(panel, "뒤로").doClick();
        assert backs.get() == 1;
        panel.setQuizEnabled(true);
        button(panel, "퀴즈 시작").doClick();
        assert quizzes.get() == 1;
        for (int[] size : new int[][] {{1280,736}, {960,736}, {640,600}, {480,680}}) {
          panel.showContents(Category.SCIENCE, cards, 0);
          resize(panel, size[0], size[1]);
          for (String action : List.of("이전", "다음", "뒤로", "퀴즈 시작")) {
            JButton button = button(panel, action);
            assert button.getWidth() >= 90 : size[0] + "px: " + action + " width=" + button.getWidth();
          }
          if (args.length > 0) render(panel, Path.of(args[0], "UI-05-" + size[0] + ".png"));
        }
        LearningContent longCard = new LearningContent("LONG", Category.HISTORY, 1, 4,
            "긴 학습 본문", List.of("역사", "긴 본문"), "긴 학습 설명입니다. ".repeat(1000));
        panel.showContents(Category.HISTORY, List.of(longCard), 0);
        resize(panel, 480, 680);
        JTextArea body = find(panel, JTextArea.class).stream().filter(a -> a.getText().equals(longCard.getDescription())).findFirst().orElseThrow();
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, body);
        assert scroll.getVerticalScrollBar().getMaximum() > scroll.getViewport().getHeight();
        panel.showContents(Category.HISTORY, List.of(), 0);
        assert progress.getToolTipText().equals("0 / 0");
        assert !button(panel, "퀴즈 시작").isEnabled();
        assert find(panel, JLabel.class).stream().anyMatch(l -> "학습 카드가 없습니다.".equals(l.getText()));
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    System.out.println("LearningDesignTest passed");
  }

  static JButton button(Container root, String text) {
    return find(root, JButton.class).stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
  }
  static void resize(javax.swing.JPanel panel, int width, int height) {
    panel.setSize(width, height);
    for (int i=0;i<10;i++) layout(panel);
  }
  static void layout(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }
  static void render(javax.swing.JPanel panel, Path path) throws Exception {
    BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
    var g = image.createGraphics(); panel.printAll(g); g.dispose();
    ImageIO.write(image, "png", path.toFile());
  }
  static <T> List<T> find(Container root, Class<T> type) {
    List<T> result = new ArrayList<>();
    for (Component child : root.getComponents()) {
      if (type.isInstance(child)) result.add(type.cast(child));
      if (child instanceof Container c) result.addAll(find(c, type));
    }
    return result;
  }
}
