package main.java.com.quizpet.ui.component;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import main.java.com.quizpet.ui.UiStyle;

/** 실제 학습 카드 수에 맞는 분절 진행 표시. */
public final class LearningProgressBar extends JPanel {
  private int count;
  private int index;

  public LearningProgressBar() {
    setOpaque(false);
    setPreferredSize(new Dimension(0, 12));
    getAccessibleContext().setAccessibleName("학습 카드 진행률");
  }

  public void showProgress(int currentIndex, int total) {
    count = total;
    index = currentIndex;
    String status = (total == 0 ? 0 : currentIndex + 1) + " / " + total;
    setToolTipText(status);
    getAccessibleContext().setAccessibleDescription(status);
    repaint();
  }

  @Override protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    if (count == 0) return;
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    double cell = (double) getWidth() / count;
    int gap = cell >= 12 ? 6 : cell >= 4 ? 2 : 0;
    for (int i = 0; i < count; i++) {
      g.setColor(i <= index ? UiStyle.BRAND : UiStyle.BORDER);
      int start = (int) (i * cell), end = (int) ((i + 1) * cell);
      g.fillRoundRect(start, 2, Math.max(1, end - start - gap), 8, 8, 8);
    }
    g.dispose();
  }
}
