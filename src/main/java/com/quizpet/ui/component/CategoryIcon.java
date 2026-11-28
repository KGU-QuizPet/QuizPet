package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

public final class CategoryIcon implements Icon {
  private final Category category;
  private final int size;

  public CategoryIcon(Category category, int size) {
    this.category = category;
    this.size = size;
  }

  public static Color soft(Color color, double ratio) {
    int r = (int) Math.round(255 - (255 - color.getRed()) * ratio);
    int g = (int) Math.round(255 - (255 - color.getGreen()) * ratio);
    int b = (int) Math.round(255 - (255 - color.getBlue()) * ratio);
    return new Color(r, g, b);
  }

  @Override
  public void paintIcon(Component c, Graphics graphics, int x, int y) {
    Color color = UiStyle.categoryColor(category);
    Graphics2D g = (Graphics2D) graphics.create(x, y, size, size);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(soft(color, 0.16));
    g.fillRoundRect(0, 0, size, size, size / 3, size / 3);
    double scale = size / 48.0;
    g.translate(size / 2.0 - 20 * scale, size / 2.0 - 20 * scale);
    g.scale(scale, scale);
    g.setColor(color);
    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    switch (category) {
      case SCIENCE -> {
        g.drawLine(15, 8, 25, 8);
        g.drawPolyline(new int[] {17, 17, 9, 31, 23, 23}, new int[] {8, 17, 31, 31, 17, 8}, 6);
        g.fillPolygon(new int[] {13, 27, 31, 9}, new int[] {24, 24, 31, 31}, 4);
      }
      case COMMON -> {
        g.fillOval(10, 6, 20, 20);
        g.fillRect(15, 24, 10, 5);
        g.drawLine(16, 33, 24, 33);
      }
      case SPORTS -> {
        g.fillOval(7, 7, 26, 26);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(20, 8, 20, 32);
        g.drawLine(8, 20, 32, 20);
        g.drawArc(11, 8, 18, 24, 100, 160);
      }
      case LANGUAGE -> {
        g.fillRoundRect(5, 7, 30, 23, 8, 8);
        g.fillPolygon(new int[] {11, 19, 11}, new int[] {29, 29, 36}, 3);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        g.drawString("Aa", 11, 24);
      }
      case HISTORY -> {
        g.fillPolygon(new int[] {5, 20, 35}, new int[] {13, 5, 13}, 3);
        for (int column : new int[] {10, 17, 23, 30}) g.drawLine(column, 17, column, 29);
        g.drawLine(6, 33, 34, 33);
      }
    }
    g.dispose();
  }

  @Override
  public int getIconWidth() {
    return size;
  }

  @Override
  public int getIconHeight() {
    return size;
  }
}
