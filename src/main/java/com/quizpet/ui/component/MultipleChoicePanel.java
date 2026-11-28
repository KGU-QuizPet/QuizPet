package main.java.com.quizpet.ui.component;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import main.java.com.quizpet.ui.UiStyle;

public class MultipleChoicePanel extends JPanel {
  private final JTextArea questionArea = new JTextArea();
  private final QuestionCard questionCard = new QuestionCard(questionArea);
  private final ButtonGroup answerGroup = new ButtonGroup();
  private final JPanel optionsPanel = new OptionsGrid();
  private final ArrayList<OptionButton> buttons = new ArrayList<>();
  private final JLabel selectionLabel = UiStyle.label("선택한 답안: 없음", UiStyle.BODY_FONT);

  public MultipleChoicePanel(String question, List<String> options) {
    setLayout(new GridBagLayout());
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder());

    GridBagConstraints cell = new GridBagConstraints();
    cell.gridx = 0;
    cell.gridy = 0;
    cell.weightx = 1;
    cell.weighty = 0.3;
    cell.fill = GridBagConstraints.BOTH;
    cell.insets = new Insets(0, 0, 20, 0);
    add(questionCard, cell);

    optionsPanel.setOpaque(false);
    cell.gridy++;
    cell.weighty = 0.7;
    add(createScrollPane(optionsPanel), cell);
    cell.gridy++;
    cell.weighty = 0;
    cell.insets = new Insets(0, 0, 0, 0);
    add(selectionLabel, cell);
    selectionLabel.setHorizontalAlignment(JLabel.LEFT);
    selectionLabel.setForeground(UiStyle.MUTED);
    setQuestion(question, options);
  }

  public void setQuestion(String question, List<String> options) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("질문은 비어 있을 수 없습니다.");
    }
    if (options == null || options.size() != 4) {
      throw new IllegalArgumentException("선택지는 정확히 4개여야 합니다.");
    }
    for (String option : options) {
      if (option == null || option.isBlank()) {
        throw new IllegalArgumentException("선택지는 비어 있을 수 없습니다.");
      }
    }
    questionArea.setText(question);
    questionArea.setCaretPosition(0);
    questionCard.setCompact(false);
    setSpacing(20);
    for (OptionButton button : buttons) answerGroup.remove(button);
    buttons.clear();
    optionsPanel.removeAll();
    for (int i = 0; i < options.size(); i++) {
      OptionButton button = new OptionButton((i + 1) + ". " + options.get(i), String.valueOf(i + 1));
      String optionText = options.get(i);
      button.addActionListener(e -> {
        selectionLabel.setText("선택한 답안: " + getSelectedAnswer() + "번 · " + optionText);
        selectionLabel.setToolTipText(selectionLabel.getText());
      });
      answerGroup.add(button);
      buttons.add(button);
      optionsPanel.add(button);
    }
    revalidate();
    repaint();
    selectionLabel.setText("선택한 답안: 없음");
  }

  public String getSelectedAnswer() {
    return answerGroup.getSelection() == null ? null : answerGroup.getSelection().getActionCommand();
  }

  public void showResult(String selectedAnswer, String correctAnswer) {
    questionCard.setCompact(true);
    // 284px FE3 미리보기에서도 짧은 보기 두 행의 선호 높이를 보존합니다.
    setSpacing(4);
    selectionLabel.setText("내가 고른 답안: " + selectedAnswer + "번 · 정답: " + correctAnswer + "번");
    for (OptionButton button : buttons) {
      button.showResult(button.getActionCommand().equals(correctAnswer),
          button.getActionCommand().equals(selectedAnswer));
    }
  }

  private void setSpacing(int gap) {
    GridBagLayout layout = (GridBagLayout) getLayout();
    for (Component child : getComponents()) {
      GridBagConstraints constraints = layout.getConstraints(child);
      constraints.insets = new Insets(0, 0, constraints.gridy < 2 ? gap : 0, 0);
      layout.setConstraints(child, constraints);
    }
    revalidate();
  }

  private static JScrollPane createScrollPane(Component content) {
    JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    return scroll;
  }

  private static final class OptionsGrid extends JPanel implements Scrollable {
    OptionsGrid() { super(new GridLayout(2, 2, 16, 16)); }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 16; }
    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return r.height; }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() {
      return getParent() != null && getPreferredSize().height <= getParent().getHeight();
    }
    @Override public void setBounds(int x, int y, int width, int height) {
      boolean resized = width != getWidth();
      super.setBounds(x, y, width, height);
      if (resized) revalidate();
    }
  }
}
