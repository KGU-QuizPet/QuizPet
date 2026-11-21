package main.java.com.quizpet.ui.component;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JTextArea;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

public final class CategoryCard extends ImageButton {
  private final Category category;
  private final JTextArea description = new JTextArea();

  public CategoryCard(Category category) {
    super(category.getName());
    this.category = category;
    setToolTipText(category.getDescription() + " · " + category.getStatType() + " +1");
    getAccessibleContext().setAccessibleDescription(getToolTipText());
    description.setText(category.getDescription());
    description.setFont(UiStyle.BODY_FONT);
    description.setForeground(UiStyle.MUTED);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setOpaque(false);
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(180, 176);
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Color color = UiStyle.categoryColor(category);
    paintSurface(graphics, getWidth(), getHeight(), Color.WHITE,
        getModel().isRollover() || hasFocus() ? color : UiStyle.BORDER, getModel().isPressed());
    Graphics2D g = (Graphics2D) graphics.create();
    int offset = getModel().isPressed() ? 3 : 0;
    g.translate(0, offset);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    paintCategoryIcon(g, getWidth() - 54, 16, color);
    g.setColor(color);
    g.fillRoundRect(16, 16, 56, 34, 14, 14);
    g.setColor(Color.WHITE);
    g.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD));
    g.drawString(category.getStatType(), 26, 39);
    g.setColor(UiStyle.TEXT);
    g.setFont(UiStyle.SUBTITLE_FONT);
    g.drawString(category.getName(), 16, 78);
    description.setSize(Math.max(1, getWidth() - 32), Math.max(1, getHeight() - 128));
    Graphics text = g.create(16, 88, description.getWidth(), description.getHeight());
    description.paint(text);
    text.dispose();
    g.setColor(color);
    g.setFont(UiStyle.BODY_FONT);
    g.drawString(category.getStatType() + " +1  ·  학습하기 →", 16, getHeight() - 20);
    g.dispose();
  }

  private void paintCategoryIcon(Graphics2D graphics, int x, int y, Color color) {
    Graphics2D g = (Graphics2D) graphics.create(x, y, 40, 40);
    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 24));
    g.fillRoundRect(0, 0, 40, 40, 12, 12);
    g.setColor(color);
    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    switch (category) {
      case SCIENCE -> {
        g.drawLine(15, 8, 25, 8);
        g.drawPolyline(new int[] {17, 17, 9, 31, 23, 23},
            new int[] {8, 17, 31, 31, 17, 8}, 6);
        g.drawLine(13, 24, 27, 24);
      }
      case COMMON -> {
        g.drawOval(10, 7, 20, 20);
        g.drawLine(16, 29, 24, 29);
        g.drawLine(17, 33, 23, 33);
      }
      case SPORTS -> {
        g.drawOval(7, 7, 26, 26);
        g.drawLine(20, 7, 20, 33);
        g.drawLine(7, 20, 33, 20);
        g.drawArc(10, 7, 20, 26, 100, 160);
      }
      case LANGUAGE -> {
        g.drawRoundRect(5, 7, 30, 23, 6, 6);
        g.drawLine(12, 30, 12, 35);
        g.drawLine(12, 35, 18, 30);
        g.setFont(UiStyle.BODY_FONT);
        g.drawString("Aa", 10, 24);
      }
      case HISTORY -> {
        g.fillPolygon(new int[] {5, 20, 35}, new int[] {13, 5, 13}, 3);
        for (int column : new int[] {11, 20, 29}) g.drawLine(column, 17, column, 29);
        g.drawLine(6, 33, 34, 33);
      }
    }
    g.dispose();
  }
}
