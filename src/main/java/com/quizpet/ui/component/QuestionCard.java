package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.UiStyle;

/** UI-06의 공통 문제 박스. 문제·해설은 해석 없이 일반 텍스트로 표시합니다. */
public final class QuestionCard extends RoundCard {
  private final JTextArea prompt;
  private final JLabel number = new JLabel("문제");
  private boolean compact;

  public QuestionCard(JTextArea prompt) {
    super(new BorderLayout(0, 8));
    this.prompt = prompt;
    prompt.setFont(UiStyle.TITLE_FONT.deriveFont(34f));
    prompt.setForeground(UiStyle.TEXT);
    prompt.setEditable(false);
    prompt.setFocusable(false);
    prompt.setLineWrap(true);
    prompt.setWrapStyleWord(true);
    prompt.setOpaque(false);
    number.setFont(UiStyle.SUBTITLE_FONT);
    number.setForeground(UiStyle.BRAND);
    add(number, BorderLayout.NORTH);
    JScrollPane scroll = new JScrollPane(prompt);
    scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(24);
    add(scroll, BorderLayout.CENTER);
  }

  public void setNumber(int index) { number.setText("Q" + index + "."); }

  public void setCompact(boolean compact) {
    this.compact = compact;
    prompt.setFont(UiStyle.TITLE_FONT.deriveFont(compact ? 24f : 34f));
    revalidate();
  }

  @Override public Dimension getPreferredSize() {
    if (!compact) return new Dimension(380, 170);
    var padding = getInsets();
    var textPadding = prompt.getInsets();
    int oneLine = prompt.getFontMetrics(prompt.getFont()).getHeight()
        + textPadding.top + textPadding.bottom;
    int height = padding.top + padding.bottom + number.getPreferredSize().height
        + ((BorderLayout) getLayout()).getVgap() + oneLine;
    return new Dimension(380, height);
  }
  @Override public Dimension getMinimumSize() { return new Dimension(0, compact ? 70 : 110); }
}
