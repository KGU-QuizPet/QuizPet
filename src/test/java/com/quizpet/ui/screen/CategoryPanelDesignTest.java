package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.service.LearningService;
import main.java.com.quizpet.ui.AppController;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.component.CategoryCard;

/** UI-04 계약·실제 데이터·작은 화면 스크롤 회귀 검증. */
public final class CategoryPanelDesignTest {
  public static void main(String[] args) throws Exception {
    var categories = new LearningService().getCategories();
    SwingUtilities.invokeAndWait(() -> {
      try {
        verifyPanel(categories, args);
        verifyControllerNavigation();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    System.out.println("CategoryPanelDesignTest passed");
  }

  private static void verifyPanel(List<Category> categories, String[] args) throws Exception {
    CategoryPanel panel = new CategoryPanel(new ScreenManager());
    panel.showCategories(categories);
    List<CategoryCard> cards = find(panel, CategoryCard.class);
    assert cards.size() == categories.size();
    AtomicReference<String> selected = new AtomicReference<>();
    panel.setOnSelect(selected::set);
    for (int i = 0; i < cards.size(); i++) {
      assert cards.get(i).getText().equals(categories.get(i).getName());
      assert cards.get(i).getToolTipText().contains(categories.get(i).getDescription());
    }
    JPanel grid = (JPanel) cards.get(0).getParent();
    GridLayout layout = (GridLayout) grid.getLayout();
    assert layout.getHgap() == 20 && layout.getVgap() == 20;
    for (int[] size : new int[][] {{1280, 736}, {1920, 1016}, {960, 736}, {800, 600}, {480, 680}, {320, 640}}) {
      resize(panel, size[0], size[1]);
      JScrollPane scroll = find(panel, JScrollPane.class).get(0);
      assert scroll.getHorizontalScrollBar().getMaximum() <= scroll.getViewport().getWidth();
      if (size[0] >= 960) {
        assert layout.getColumns() == 3;
        assert cards.get(0).getY() == cards.get(2).getY();
        assert cards.get(3).getY() > cards.get(0).getY();
        assert !scroll.getVerticalScrollBar().isVisible() : "Desktop cards must fit";
      }
      if (size[0] == 320) {
        assert layout.getColumns() == 1;
        assert scroll.getVerticalScrollBar().isVisible();
        cards.get(4).scrollRectToVisible(new java.awt.Rectangle(0, 0,
            cards.get(4).getWidth(), cards.get(4).getHeight()));
        assert scroll.getViewport().getViewPosition().y > 0 : "Last category must be reachable";
      }
      if (args.length > 0) {
        BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); panel.printAll(g); g.dispose();
        ImageIO.write(image, "png", Path.of(args[0], "UI-04-" + size[0] + ".png").toFile());
      }
    }
    cards.get(0).setEnabled(false);
    cards.get(0).doClick();
    assert selected.get() == null : "Disabled card cannot select a category";
    cards.get(0).setEnabled(true);
    cards.get(0).doClick();
    assert categories.get(0).getCategoryId().equals(selected.get());
    panel.showCategories(categories);
    assert panel.getSelectedCategoryId() == null;
    panel.showCategories(List.of());
    assert find(panel, CategoryCard.class).isEmpty();
    assert find(panel, JLabel.class).stream().anyMatch(label -> label.getText().equals("학습 가능한 분야가 없습니다."));
  }

  private static void verifyControllerNavigation() throws Exception {
    Path save = Files.createTempDirectory("quizpet-ui04-");
    try {
      AppController app = new AppController(save);
      app.createUser("UI04 검증");
      app.createPet("검증 펫", "DUCK");
      app.showCategories();
      CategoryPanel panel = find(app.getView(), CategoryPanel.class).get(0);
      CategoryCard science = find(panel, CategoryCard.class).stream()
          .filter(card -> card.getText().equals(Category.SCIENCE.getName())).findFirst().orElseThrow();
      science.doClick();
      assert Category.SCIENCE.getCategoryId().equals(panel.getSelectedCategoryId());
      LearningPanel learning = find(app.getView(), LearningPanel.class).get(0);
      assert learning.isVisible() && !panel.isVisible();
      String expectedTitle = new LearningService().getContents("SCIENCE").get(0).getTitle();
      assert find(learning, JLabel.class).stream().anyMatch(label -> label.getText().equals(expectedTitle));
      app.showCategories();
      assert panel.isVisible() && panel.getSelectedCategoryId() == null;
      find(panel, JButton.class).stream().filter(button -> button.getText().equals("홈으로"))
          .findFirst().orElseThrow().doClick();
      assert find(app.getView(), HomePanel.class).get(0).isVisible();
    } finally {
      try (var paths = Files.walk(save)) {
        for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
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
