package main.java.com.quizpet.ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.UiStyle;

public final class OptionButton extends JToggleButton {
  private final JTextArea text = new JTextArea();
  private Boolean correct;
  private boolean chosen;

  public OptionButton(String label, String answer) {
    super(label);
    setActionCommand(answer);
    setContentAreaFilled(false);
    setOpaque(false);
    setBorderPainted(false);
    setRolloverEnabled(true);
    setBorder(BorderFactory.createEmptyBorder(12, 14, 16, 14));
    text.setFont(UiStyle.SUBTITLE_FONT);
    text.setText(label);
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
        ? (getParent().getWidth() - gap) / 2 - 28 : 160;
    text.setSize(Math.max(1, width), Short.MAX_VALUE);
    return new Dimension(180, Math.max(64, text.getPreferredSize().height + 32));
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
    } else if (correct == null && (isSelected() || hasFocus() || getModel().isRollover())) {
      border = UiStyle.BRAND;
      fill = new Color(0xFFF0DC);
    }
    ImageButton.paintSurface(g, getWidth(), getHeight(), fill, border, getModel().isPressed());
    text.setForeground(isEnabled() || correct != null ? UiStyle.TEXT : UiStyle.MUTED);
    text.setSize(Math.max(1, getWidth() - 28), Math.max(1, getHeight() - 32));
    Graphics content = g.create(14, 12, text.getWidth(), text.getHeight());
    text.paint(content);
    content.dispose();
  }
}
