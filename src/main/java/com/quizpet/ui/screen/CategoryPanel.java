package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.Scrollable;
import javax.swing.JScrollPane;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import main.java.com.quizpet.ui.component.CategoryCard;
import main.java.com.quizpet.ui.component.ImageButton;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class CategoryPanel extends JPanel {
  private final JPanel grid = new CategoryGrid();
  private final JLabel selectionLabel = UiStyle.label("선택한 분야: 없음", UiStyle.SUBTITLE_FONT);
  private Consumer<String> onSelect = id -> { };
  private String selectedCategoryId;

  public CategoryPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 24));
    JPanel header = new JPanel(new BorderLayout(0, 10));
    header.setOpaque(false);
    header.add(UiStyle.label("학습 분야 선택", UiStyle.TITLE_FONT), BorderLayout.NORTH);
    header.add(UiStyle.label("학습할 분야를 골라 주세요.", UiStyle.BODY_FONT), BorderLayout.SOUTH);
    add(header, BorderLayout.NORTH);
    grid.setOpaque(false);
    JScrollPane scroll = new JScrollPane(grid,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getViewport().setBackground(UiStyle.BACKGROUND);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    add(scroll, BorderLayout.CENTER);
    JPanel footer = new JPanel(new BorderLayout(0, 10));
    footer.setOpaque(false);
    footer.add(selectionLabel, BorderLayout.NORTH);
    JButton back = new ImageButton("홈으로");
    back.setBackground(java.awt.Color.WHITE);
    back.addActionListener(e -> screens.show(ScreenManager.HOME));
    footer.add(back, BorderLayout.SOUTH);
    add(footer, BorderLayout.SOUTH);
  }

  public void showCategories(List<Category> categories) {
    selectedCategoryId = null;
    selectionLabel.setText(categories.isEmpty() ? "학습 가능한 분야가 없습니다." : "선택한 분야: 없음");
    grid.removeAll();
    for (Category category : categories) grid.add(createCategoryCard(category));
    revalidate();
    repaint();
  }

  public void setOnSelect(Consumer<String> action) {
    onSelect = Objects.requireNonNull(action);
  }

  public String getSelectedCategoryId() {
    return selectedCategoryId;
  }

  private JButton createCategoryCard(Category category) {
    JButton button = new CategoryCard(category);
    button.addActionListener(e -> {
      selectedCategoryId = category.getCategoryId();
      selectionLabel.setText("선택한 분야: " + category.getName());
      onSelect.accept(selectedCategoryId);
    });
    return button;
  }
  private static final class CategoryGrid extends JPanel implements Scrollable {
    CategoryGrid() { super(new GridLayout(0, 3, 12, 12)); }

    @Override
    public void setBounds(int x, int y, int width, int height) {
      super.setBounds(x, y, width, height);
      if (getComponentCount() == 0) return;
      GridLayout layout = (GridLayout) getLayout();
      int cardWidth = getComponent(0).getPreferredSize().width;
      int columns = Math.max(1, Math.min(3, (width + layout.getHgap()) / (cardWidth + layout.getHgap())));
      if (layout.getColumns() != columns) {
        layout.setColumns(columns);
        revalidate();
      }
    }

    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 16; }
    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return r.height; }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() {
      return getParent() != null && getPreferredSize().height <= getParent().getHeight();
    }
  }

}
