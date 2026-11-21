package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.UiStyle;

public class OXQuestionPanel extends JPanel {
  private final JTextArea questionArea = new JTextArea();
  private final ButtonGroup answerGroup = new ButtonGroup();
  private final OptionButton oButton = new OptionButton("O · 맞다", "O");
  private final OptionButton xButton = new OptionButton("X · 아니다", "X");
  private final JLabel selectionLabel = UiStyle.label("선택한 답안: 없음", UiStyle.BODY_FONT);

  public OXQuestionPanel(String question) {
    setLayout(new GridBagLayout());
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    questionArea.setFont(UiStyle.SUBTITLE_FONT);
    questionArea.setEditable(false);
    questionArea.setFocusable(false);
    questionArea.setLineWrap(true);
    questionArea.setWrapStyleWord(true);
    questionArea.setOpaque(false);
    JScrollPane questionScroll = new JScrollPane(questionArea);
    questionScroll.setBorder(BorderFactory.createEmptyBorder());
    questionScroll.setOpaque(false);
    questionScroll.getViewport().setOpaque(false);
    questionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    questionArea.setRows(3);
    GridBagConstraints cell = new GridBagConstraints();
    cell.gridx = 0;
    cell.gridy = 0;
    cell.weightx = 1;
    cell.weighty = 0.5;
    cell.fill = GridBagConstraints.BOTH;
    cell.insets = new Insets(0, 0, 24, 0);
    add(questionScroll, cell);

    JPanel answers = new JPanel(new GridLayout(1, 2, 20, 0));
    answers.setOpaque(false);
    for (OptionButton button : new OptionButton[] {oButton, xButton}) {
      button.setFocusPainted(false);
      button.addActionListener(e -> updateSelection());
      answerGroup.add(button);
      answers.add(button);
    }

    JPanel answerArea = new JPanel(new BorderLayout(0, 16));
    answerArea.setOpaque(false);
    answerArea.add(answers, BorderLayout.CENTER);
    answerArea.add(selectionLabel, BorderLayout.SOUTH);
    cell.gridy++;
    cell.insets = new Insets(0, 0, 0, 0);
    add(answerArea, cell);
    setQuestion(question);
  }

  public void setQuestion(String question) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("질문은 비어 있을 수 없습니다.");
    }
    questionArea.setText(question);
    questionArea.setCaretPosition(0);
    answerGroup.clearSelection();
    oButton.resetResult();
    xButton.resetResult();
    updateSelection();
  }

  public String getSelectedAnswer() {
    ButtonModel selection = answerGroup.getSelection();
    return selection == null ? null : selection.getActionCommand();
  }

  private void updateSelection() {
    String answer = getSelectedAnswer();
    selectionLabel.setText("선택한 답안: " + (answer == null ? "없음" : answer));
    repaint();
  }

  public void showResult(String selectedAnswer, String correctAnswer) {
    selectionLabel.setText("내가 고른 답안: " + selectedAnswer + " · 정답: " + correctAnswer);
    for (OptionButton button : new OptionButton[] {oButton, xButton}) {
      button.showResult(button.getActionCommand().equals(correctAnswer),
          button.getActionCommand().equals(selectedAnswer));
    }
  }
}
