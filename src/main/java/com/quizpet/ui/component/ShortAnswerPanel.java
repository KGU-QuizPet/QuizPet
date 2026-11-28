package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import main.java.com.quizpet.model.ShortAnswerQuestion;
import main.java.com.quizpet.ui.UiStyle;

public final class ShortAnswerPanel extends JPanel {
  private final JTextField answerField = new JTextField();
  private final JTextArea prompt = new JTextArea();
  private final QuestionCard questionCard = new QuestionCard(prompt);
  private final JPanel answer = new RoundCard(new BorderLayout(0, 12));
  private final JLabel label = UiStyle.label("답안을 입력해 주세요.", UiStyle.BODY_FONT);
  private final JTextArea hint = new JTextArea("공백과 영문 대소문자는 구분하지 않으며, |는 입력할 수 없습니다.");

  public ShortAnswerPanel(String question) {
    setLayout(new BorderLayout(0, 16));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder());
    prompt.setText(question);
    prompt.setCaretPosition(0);
    add(questionCard, BorderLayout.CENTER);

    label.setHorizontalAlignment(JLabel.LEFT);
    label.setLabelFor(answerField);
    answer.add(label, BorderLayout.NORTH);
    answerField.setFont(UiStyle.SUBTITLE_FONT.deriveFont(22f));
    answerField.setForeground(UiStyle.TEXT);
    answerField.setDisabledTextColor(UiStyle.TEXT);
    answerField.setBackground(UiStyle.SUNKEN);
    answerField.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    answerField.setPreferredSize(new java.awt.Dimension(0, 56));
    answerField.getAccessibleContext().setAccessibleName("단답형 답안");
    answer.add(answerField, BorderLayout.CENTER);
    hint.setFont(UiStyle.BODY_FONT);
    hint.setForeground(UiStyle.MUTED);
    hint.setEditable(false);
    hint.setFocusable(false);
    hint.setLineWrap(true);
    hint.setWrapStyleWord(true);
    hint.setOpaque(false);
    hint.setRows(2);
    answer.add(hint, BorderLayout.SOUTH);
    add(answer, BorderLayout.SOUTH);
  }

  public String getSelectedAnswer() {
    String answer = answerField.getText();
    return ShortAnswerQuestion.isSubmittable(answer) ? answer : null;
  }

  public void showResult(String selectedAnswer) {
    questionCard.setCompact(true);
    label.setVisible(false);
    hint.setVisible(false);
    answer.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
    answerField.setText(selectedAnswer);
    answerField.setEnabled(false);
  }
}
