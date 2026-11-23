package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import main.java.com.quizpet.ui.UiStyle;

/** 경험치·능력치를 표시하는 둥근 막대입니다. 값은 0~maximum 범위로 보정해 그립니다. */
public final class GaugeBar extends JPanel {
  private final Color fill;
  private final boolean outlined;
  private int value;
  private int maximum = 1;

  /**
   * @param fill 채움 색
   * @param height 막대 높이(px)
   * @param outlined true면 경험치 바처럼 테두리와 아래쪽 진한 선을 함께 그립니다.
   */
  public GaugeBar(Color fill, int height, boolean outlined) {
    if (height <= 0) {
      throw new IllegalArgumentException("막대 높이는 양수여야 합니다.");
    }
    this.fill = fill;
    this.outlined = outlined;
    setOpaque(false);
    setPreferredSize(new Dimension(120, height));
    setMinimumSize(new Dimension(40, height));
    setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
  }

  public void setValue(int value, int maximum) {
    this.maximum = Math.max(1, maximum);
    this.value = Math.max(0, Math.min(value, this.maximum));
    String description = value + " / " + this.maximum;
    setToolTipText(description);
    getAccessibleContext().setAccessibleDescription(description);
    repaint();
  }

  public int getValue() {
    return value;
  }

  public int getMaximum() {
    return maximum;
  }

  /** 채워진 비율(0.0~1.0)입니다. */
  public double getRatio() {
    return (double) value / maximum;
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    int arc = h;
    g.setColor(outlined ? UiStyle.SUNKEN : new Color(0xF1E6D3));
    g.fillRoundRect(0, 0, w, h, arc, arc);

    int filled = (int) Math.round(w * getRatio());
    if (filled > 0) {
      filled = Math.max(filled, Math.min(w, h));
      g.setColor(fill);
      g.fillRoundRect(0, 0, filled, h, arc, arc);
      if (outlined && h >= 8) {
        g.setColor(UiStyle.EXP_DARK);
        g.fillRoundRect(0, h - 3, filled, 3, 3, 3);
      }
    }
    if (outlined) {
      g.setColor(UiStyle.BORDER);
      g.setStroke(new BasicStroke(2));
      g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
    }
    g.dispose();
  }
}
