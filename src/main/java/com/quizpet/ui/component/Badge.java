package main.java.com.quizpet.ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** 둥근 배경 위에 짧은 글자를 표시합니다(레벨 배지, 능력치 칩 등). */
public final class Badge extends JLabel {
  private final int minWidth;

  public Badge(String text, Color background, Color foreground, Font font, int minWidth) {
    super(text, SwingConstants.CENTER);
    this.minWidth = minWidth;
    setFont(font);
    setBackground(background);
    setForeground(foreground);
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    size.width = Math.max(minWidth, size.width);
    return size;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(getBackground());
    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
    g.dispose();
    super.paintComponent(graphics);
  }
}
