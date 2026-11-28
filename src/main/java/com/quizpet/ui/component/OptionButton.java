package main.java.com.quizpet.ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.UiStyle;

public final class OptionButton extends JToggleButton {
  private final JTextArea text = new JTextArea();
  private Boolean correct;
  private boolean chosen;
  private final boolean ox;

  public OptionButton(String label, String answer) {
    super(label);
    setActionCommand(answer);
    ox = "O".equals(answer) || "X".equals(answer);
    setContentAreaFilled(false);
    setOpaque(false);
    setBorderPainted(false);
    setRolloverEnabled(true);
    setBorder(BorderFactory.createEmptyBorder(12, 14, 16, 14));
    text.setFont(UiStyle.SUBTITLE_FONT);
    text.setText(!ox && label.startsWith(answer + ". ") ? label.substring(answer.length() + 2) : label);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setOpaque(false);
    getAccessibleContext().setAccessibleName(label);
    addItemListener(e -> repaint());
  }

  public void resetResult() {
    correct = null;
    chosen = false;
    setEnabled(true);
    getAccessibleContext().setAccessibleDescription(null);
  }

  public void showResult(boolean correctAnswer, boolean selectedAnswer) {
    correct = correctAnswer;
    chosen = selectedAnswer;
    setSelected(selectedAnswer);
    setEnabled(false);
    getAccessibleContext().setAccessibleDescription(
        correctAnswer ? "정답" : selectedAnswer ? "내가 고른 오답" : "선택하지 않은 보기");
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    int gap = getParent() != null && getParent().getLayout() instanceof GridLayout grid
        ? grid.getHgap() : 12;
    int width = getParent() != null && getParent().getWidth() > 40
        ? (getParent().getWidth() - gap) / 2 - (ox ? 28 : 104) : 160;
    text.setSize(Math.max(1, width), Short.MAX_VALUE);
    return new Dimension(180, ox ? 100 : correct != null
        ? Math.max(64, text.getPreferredSize().height + 32)
        : Math.max(86, text.getPreferredSize().height + 48));
  }

  @Override
  protected void paintComponent(Graphics g) {
    Color border = UiStyle.BORDER;
    Color fill = Color.WHITE;
    if (Boolean.TRUE.equals(correct)) {
      border = UiStyle.CORRECT;
      fill = UiStyle.CORRECT_SOFT;
    } else if (Boolean.FALSE.equals(correct) && chosen) {
      border = UiStyle.WRONG;
      fill = UiStyle.WRONG_SOFT;
    } else if (correct == null && isEnabled() && (isSelected() || hasFocus() || getModel().isRollover())) {
      border = ox ? ("O".equals(getActionCommand()) ? UiStyle.categoryColor(main.java.com.quizpet.model.Category.SCIENCE)
          : UiStyle.WRONG) : UiStyle.BRAND;
      fill = CategoryIcon.soft(border, 0.12);
    } else if (!isEnabled() && correct == null) {
      fill = UiStyle.SUNKEN;
    }
    ImageButton.paintSurface(g, getWidth(), getHeight(), fill, border, getModel().isPressed());
    Graphics2D drawing = (Graphics2D) g.create();
    drawing.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    drawing.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    boolean dimmed = Boolean.FALSE.equals(correct) && !chosen;
    if (ox) {
      Color symbol = "O".equals(getActionCommand())
          ? UiStyle.categoryColor(main.java.com.quizpet.model.Category.SCIENCE) : UiStyle.WRONG;
      drawing.setColor(dimmed || (!isEnabled() && correct == null) ? UiStyle.HINT : symbol);
      int size = Math.max(28, Math.min(150, getHeight() - 64));
      drawing.setFont(UiStyle.TITLE_FONT.deriveFont((float) size));
      int symbolX = (getWidth() - drawing.getFontMetrics().stringWidth(getActionCommand())) / 2;
      int baseline = (getHeight() - 30 + drawing.getFontMetrics().getAscent()) / 2 - 8;
      drawing.drawString(getActionCommand(), symbolX, baseline);
      String caption = Boolean.TRUE.equals(correct) ? "정답" : chosen && correct != null ? "내가 고른 답"
          : "O".equals(getActionCommand()) ? "맞다" : "아니다";
      drawing.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 14f));
      drawing.setColor(Boolean.TRUE.equals(correct) ? UiStyle.CORRECT
          : chosen && correct != null ? UiStyle.WRONG : UiStyle.MUTED);
      drawing.drawString(caption, (getWidth() - drawing.getFontMetrics().stringWidth(caption)) / 2,
          Math.min(getHeight() - 16, baseline + 28));
      drawing.dispose();
      return;
    }
    int badgeY = Math.max(14, (getHeight() - 46) / 2);
    boolean active = Boolean.TRUE.equals(correct) || chosen || isSelected();
    drawing.setColor(active ? border : UiStyle.SUNKEN);
    drawing.fillRoundRect(20, badgeY, 46, 46, 14, 14);
    drawing.setFont(UiStyle.TITLE_FONT.deriveFont(24f));
    drawing.setColor(active ? UiStyle.SURFACE : dimmed ? UiStyle.HINT : UiStyle.MUTED);
    drawing.drawString(getActionCommand(), 20 + (46 - drawing.getFontMetrics().stringWidth(getActionCommand())) / 2,
        badgeY + 32);
    if (Boolean.TRUE.equals(correct)) {
      drawing.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 12f));
      drawing.setColor(UiStyle.CORRECT);
      drawing.drawString("정답", getWidth() - 42, 24);
    }
    drawing.dispose();
    text.setForeground(dimmed ? UiStyle.HINT : isEnabled() || correct != null ? UiStyle.TEXT : UiStyle.MUTED);
    text.setSize(Math.max(1, getWidth() - 100), Short.MAX_VALUE);
    int textHeight = text.getPreferredSize().height;
    Graphics content = g.create(84, Math.max(16, (getHeight() - textHeight) / 2),
        text.getWidth(), Math.max(1, getHeight() - 32));
    text.paint(content);
    content.dispose();
  }
}
