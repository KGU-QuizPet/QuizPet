package main.java.com.quizpet.ui.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JPanel;

/** 태그를 가용 너비에 맞춰 줄바꿈하고 필요한 높이를 확보합니다. */
public final class WrapPanel extends JPanel {
  public WrapPanel() {
    super(new FlowLayout(FlowLayout.LEFT, 8, 6));
    setOpaque(false);
  }

  @Override public Dimension getPreferredSize() {
    int width = getWidth();
    if (width <= 0 && getParent() != null) width = getParent().getWidth();
    if (width <= 0) return super.getPreferredSize();
    FlowLayout layout = (FlowLayout) getLayout();
    Insets insets = getInsets();
    int available = Math.max(1, width - insets.left - insets.right - 2 * layout.getHgap());
    int rowWidth = 0, rowHeight = 0, height = 2 * layout.getVgap();
    for (Component child : getComponents()) {
      if (!child.isVisible()) continue;
      Dimension size = child.getPreferredSize();
      if (rowWidth > 0 && rowWidth + layout.getHgap() + size.width > available) {
        height += rowHeight + layout.getVgap();
        rowWidth = 0;
        rowHeight = 0;
      }
      rowWidth += (rowWidth == 0 ? 0 : layout.getHgap()) + size.width;
      rowHeight = Math.max(rowHeight, size.height);
    }
    return new Dimension(width, height + rowHeight + insets.top + insets.bottom);
  }
}
