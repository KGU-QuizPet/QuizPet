package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import main.java.com.quizpet.ui.UiStyle;

public class RoundCard extends JPanel {
  private static final int ARC = 20;
  private static final int LIP = 5;
  private Color borderColor = UiStyle.BORDER;

  public RoundCard(LayoutManager layout) {
    super(layout);
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(18, 20, 18 + LIP, 20));
  }

  public void setBorderColor(Color color) {
    borderColor = color;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth() - 2;
    int h = getHeight() - LIP - 1;
    g.setColor(UiStyle.SHADOW);
    g.fillRoundRect(1, LIP, w, h, ARC, ARC);
    g.setColor(UiStyle.SURFACE);
    g.fillRoundRect(1, 1, w, h, ARC, ARC);
    g.setColor(borderColor);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(1, 1, w - 1, h - 1, ARC, ARC);
    g.dispose();
  }
}
