package main.java.com.quizpet.ui.screen;

import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.component.CategoryProgressCard;
import main.java.com.quizpet.ui.component.TopBar;

public final class HomeDashboardTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      HomePanel home = new HomePanel(new ScreenManager());

      CategoryProgressCard science = home.getProgressCard(Category.SCIENCE);
      assert science.getCaptionText().equals("아직 시작 전");
      home.showCategoryProgress(Category.SCIENCE, 15, 80.0);
      assert science.getCaptionText().equals("80% · 15문제") : science.getCaptionText();
      assert science.getBar().getValue() == 80;

      home.showPet("말랑이", "DUCK", 4, 30, 9, 18, 14, 13);
      assert home.getProgressCard(Category.SPORTS).isRecommended();
      assert !home.getProgressCard(Category.SCIENCE).isRecommended();
      assert home.getGrowthHintText().contains("체력(STR)") : home.getGrowthHintText();
      home.showPet("말랑이", "DUCK", 4, 30, 12, 8, 14, 13);
      assert home.getProgressCard(Category.SCIENCE).isRecommended();
      assert home.getProgressCard(Category.COMMON).isRecommended();
      home.showPet("말랑이", "DUCK", 4, 30, 8, 8, 14, 13);
      assert home.getProgressCard(Category.SCIENCE).isRecommended();
      assert home.getProgressCard(Category.COMMON).isRecommended();
      assert home.getProgressCard(Category.SPORTS).isRecommended();
      assert !home.getProgressCard(Category.HISTORY).isRecommended();
      assert !home.getProgressCard(Category.LANGUAGE).isRecommended();
      assert home.getGrowthHintText().contains("체력(STR)");
      assert home.getGrowthHintText().contains("지성(INT)");
      home.showPet("말랑이", "DUCK", 1, 0, 10, 10, 10, 10);
      for (Category category : Category.values()) {
        assert !home.getProgressCard(category).isRecommended();
      }

      String[] clicked = new String[1];
      home.setOnCategory(id -> clicked[0] = id);
      home.getProgressCard(Category.HISTORY).dispatchEvent(new java.awt.event.MouseEvent(
          home.getProgressCard(Category.HISTORY), java.awt.event.MouseEvent.MOUSE_CLICKED,
          0, 0, 5, 5, 1, false));
      assert "HISTORY".equals(clicked[0]) : clicked[0];

      assert home.getRecentRowCount() == 0;
      HomePanel.RecentItem item = new HomePanel.RecentItem(Category.SCIENCE, "물은 100도에서 끓는다", true, 10);
      home.showRecent(List.of(item, item, item, item));
      assert home.getRecentRowCount() == 3;

      home.showWrongCounts(Map.of(), 0);
      assert home.getWrongCountText().contains("없어요");
      home.showWrongCounts(Map.of(Category.LANGUAGE, 3, Category.COMMON, 2, Category.SPORTS, 2), 0);
      assert home.getWrongCountText().contains("7개") : home.getWrongCountText();

      TopBar bar = new TopBar();
      bar.select(TopBar.TAB_HISTORY);
      assert TopBar.TAB_HISTORY.equals(bar.getSelectedTab());
      bar.setTabEnabled(TopBar.TAB_BATTLE, false);
      assert !bar.getTab(TopBar.TAB_BATTLE).isEnabled();
      bar.select(null);
      assert bar.getSelectedTab() == null;
    });
    System.out.println("Home dashboard checks passed");
  }
}
