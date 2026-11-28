package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import main.java.com.quizpet.ui.UiStyle;

public final class GameButton extends JButton {
  private Color bg;
  private Color hoverBg;

  public GameButton(String text, Color bg, Color fg, Color hoverBg) {
    super(text);
    this.bg = bg;
    this.hoverBg = hoverBg;
    setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 14f));
    setForeground(fg);
    setFocusPainted(false);
    setContentAreaFilled(false);
    setBorderPainted(false);
    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  public void setPalette(Color bg, Color fg, Color hoverBg) {
    this.bg = bg;
    this.hoverBg = hoverBg;
    setForeground(fg);
    repaint();
  }

  @Override protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Color fill;
    if (!isEnabled()) fill = new Color(0xD8D3C8);
    else if (getModel().isRollover() || getModel().isPressed()) fill = hoverBg;
    else fill = bg;
    g2.setColor(fill);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
    if (bg.equals(Color.WHITE) && isEnabled()) {
      g2.setColor(UiStyle.BORDER);
      g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
    }
    if (hasFocus()) {
      g2.setColor(UiStyle.TEXT);
      g2.setStroke(new BasicStroke(2));
      g2.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 10, 10);
    }
    g2.dispose();
    super.paintComponent(g);
  }
}
