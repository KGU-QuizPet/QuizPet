package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import main.java.com.quizpet.ui.UiStyle;

public class OXQuestionPanel extends JPanel {
  private final JTextArea questionArea = new JTextArea();
  private final ButtonGroup answerGroup = new ButtonGroup();
  private final JToggleButton oButton = new JToggleButton("O");
  private final JToggleButton xButton = new JToggleButton("X");
  private final JLabel selectionLabel = UiStyle.label("선택한 답안: 없음", UiStyle.BODY_FONT);

  public OXQuestionPanel(String question) {
    setLayout(new BorderLayout(0, 24));
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
    add(questionScroll, BorderLayout.CENTER);

    JPanel answers = new JPanel(new GridLayout(1, 2, 20, 0));
    answers.setOpaque(false);
    answers.setPreferredSize(new Dimension(0, 100));
    for (JToggleButton button : new JToggleButton[] {oButton, xButton}) {
      button.setFont(UiStyle.TITLE_FONT);
      button.setFocusPainted(false);
      button.setActionCommand(button.getText());
      button.addActionListener(e -> updateSelection());
      answerGroup.add(button);
      answers.add(button);
    }

    JPanel answerArea = new JPanel(new BorderLayout(0, 16));
    answerArea.setOpaque(false);
    answerArea.add(answers, BorderLayout.CENTER);
    answerArea.add(selectionLabel, BorderLayout.SOUTH);
    add(answerArea, BorderLayout.SOUTH);
    setQuestion(question);
  }

  public void setQuestion(String question) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("질문은 비어 있을 수 없습니다.");
    }
    questionArea.setText(question);
    questionArea.setCaretPosition(0);
    answerGroup.clearSelection();
    updateSelection();
  }

  public String getSelectedAnswer() {
    ButtonModel selection = answerGroup.getSelection();
    return selection == null ? null : selection.getActionCommand();
  }

  private void updateSelection() {
    String answer = getSelectedAnswer();
    selectionLabel.setText("선택한 답안: " + (answer == null ? "없음" : answer));
    for (JToggleButton button : new JToggleButton[] {oButton, xButton}) {
      Color color = button.isSelected() ? UiStyle.POINT : Color.LIGHT_GRAY;
      button.setBorder(BorderFactory.createLineBorder(color, 3));
      button.setForeground(button.isSelected() ? UiStyle.POINT : Color.DARK_GRAY);
    }
    repaint();
  }
}
