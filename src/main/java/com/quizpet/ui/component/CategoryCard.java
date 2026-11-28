package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import javax.swing.JTextArea;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

/** UI-04 분야 카드. 통계 미연동 상태를 NEW나 0%로 표현하지 않습니다. */
public final class CategoryCard extends ImageButton {
  private static final int PADDING = 24;
  private static final int RADIUS = 26;
  private static final int SHADOW_Y = 6;
  private static final int HOVER_Y = 3;
  private static final int ICON_SIZE = 72;
  private static final Font NAME_FONT = UiStyle.TITLE_FONT;
  private static final Font CHIP_FONT = UiStyle.BODY_FONT.deriveFont(Font.BOLD, 12f);
  private final Category category;
  private final CategoryIcon icon;
  private final JTextArea description = new JTextArea();

  public CategoryCard(Category category) {
    super(Objects.requireNonNull(category, "category는 필수입니다.").getName());
    this.category = category;
    icon = new CategoryIcon(category, ICON_SIZE);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setToolTipText(category.getDescription() + " · " + category.getStatType() + " +1");
    getAccessibleContext().setAccessibleDescription(getToolTipText());
    description.setText(category.getDescription());
    description.setFont(UiStyle.BODY_FONT);
    description.setForeground(UiStyle.MUTED);
    description.setEditable(false);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setOpaque(false);
  }

  @Override public Dimension getPreferredSize() {
    return new Dimension(240, getWidth() > 0 && getWidth() < 260 ? 260 : 220);
  }
  @Override public Dimension getMinimumSize() { return getPreferredSize(); }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    boolean highlighted = isEnabled() && (getModel().isRollover() || hasFocus());
    int top = highlighted ? 0 : HOVER_Y;
    if (getModel().isPressed() && isEnabled()) top = HOVER_Y + 2;
    int width = Math.max(0, getWidth() - 3);
    int height = Math.max(0, getHeight() - SHADOW_Y - HOVER_Y - 2);
    Color color = isEnabled() ? UiStyle.categoryColor(category) : UiStyle.MUTED;
    g.setColor(highlighted ? color : UiStyle.SHADOW);
    g.fillRoundRect(1, top + SHADOW_Y, width, height, RADIUS, RADIUS);
    g.setColor(isEnabled() ? UiStyle.SURFACE : UiStyle.SUNKEN);
    g.fillRoundRect(1, top, width, height, RADIUS, RADIUS);
    g.setColor(highlighted ? color : UiStyle.BORDER);
    g.setStroke(new BasicStroke(highlighted ? 3f : 2f));
    g.drawRoundRect(1, top + 1, width, height - 1, RADIUS, RADIUS);
    g.translate(0, top);
    Graphics2D decoration = (Graphics2D) g.create();
    decoration.clip(new RoundRectangle2D.Float(2, 2, width - 2, height - 3, RADIUS, RADIUS));
    decoration.setColor(CategoryIcon.soft(color, 0.07));
    decoration.fillOval(getWidth() - 106, -30, 140, 140);
    decoration.dispose();
    icon.paintIcon(this, g, PADDING, 20);

    g.setFont(NAME_FONT);
    g.setColor(isEnabled() ? UiStyle.TEXT : UiStyle.MUTED);
    g.drawString(category.getName(), PADDING, 130);
    int chipX = PADDING + g.getFontMetrics().stringWidth(category.getName()) + 10;
    g.setFont(CHIP_FONT);
    int chipWidth = g.getFontMetrics().stringWidth(category.getStatType()) + 16;
    g.setColor(color);
    g.fillRoundRect(chipX, 110, chipWidth, 24, 12, 12);
    g.setColor(UiStyle.SURFACE);
    g.drawString(category.getStatType(), chipX + 8, 127);

    int textWidth = Math.max(1, getWidth() - PADDING * 2);
    description.setSize(textWidth, Math.max(1, height - 190));
    Graphics text = g.create(PADDING, 144, textWidth, description.getHeight());
    description.paint(text);
    text.dispose();
    g.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD));
    g.setColor(isEnabled() ? color : UiStyle.MUTED);
    g.drawString(isEnabled() ? "학습하기 →" : "준비 중", PADDING, height - 18);
    g.dispose();
  }
}
