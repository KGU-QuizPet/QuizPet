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
  private final int arc;
  private final int lip;
  private Color borderColor = UiStyle.BORDER;

  public RoundCard(LayoutManager layout) {
    this(layout, 20, 5);
  }

  public RoundCard(LayoutManager layout, int arc, int lip) {
    super(layout);
    if (arc <= 0 || lip < 0) throw new IllegalArgumentException("유효한 모서리·그림자 크기가 필요합니다.");
    this.arc = arc;
    this.lip = lip;
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(18, 20, 18 + lip, 20));
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
    int h = getHeight() - lip - 1;
    g.setColor(UiStyle.SHADOW);
    g.fillRoundRect(1, lip, w, h, arc, arc);
    g.setColor(UiStyle.SURFACE);
    g.fillRoundRect(1, 1, w, h, arc, arc);
    g.setColor(borderColor);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(1, 1, w - 1, h - 1, arc, arc);
    g.dispose();
  }
}
