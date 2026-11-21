package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import main.java.com.quizpet.model.ShortAnswerQuestion;
import main.java.com.quizpet.ui.UiStyle;

public final class ShortAnswerPanel extends JPanel {
  private final JTextField answerField = new JTextField();

  public ShortAnswerPanel(String question) {
    setLayout(new BorderLayout(0, 16));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    JTextArea prompt = new JTextArea(question);
    prompt.setFont(UiStyle.SUBTITLE_FONT);
    prompt.setEditable(false);
    prompt.setFocusable(false);
    prompt.setLineWrap(true);
    prompt.setWrapStyleWord(true);
    prompt.setOpaque(false);
    JScrollPane scroll = new JScrollPane(prompt);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getViewport().setBackground(UiStyle.BACKGROUND);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    add(scroll, BorderLayout.CENTER);

    JPanel answer = new JPanel(new BorderLayout(0, 8));
    answer.setOpaque(false);
    JLabel label = UiStyle.label("답안을 입력해 주세요.", UiStyle.BODY_FONT);
    label.setLabelFor(answerField);
    answer.add(label, BorderLayout.NORTH);
    answerField.setFont(UiStyle.SUBTITLE_FONT);
    answerField.getAccessibleContext().setAccessibleName("단답형 답안");
    answer.add(answerField, BorderLayout.CENTER);
    answer.add(UiStyle.label("공백과 영문 대소문자는 구분하지 않으며, |는 입력할 수 없습니다.", UiStyle.BODY_FONT), BorderLayout.SOUTH);
    add(answer, BorderLayout.SOUTH);
  }

  public String getSelectedAnswer() {
    String answer = answerField.getText();
    return ShortAnswerQuestion.isSubmittable(answer) ? answer : null;
  }

  public void showResult(String selectedAnswer) {
    answerField.setText(selectedAnswer);
    answerField.setEnabled(false);
  }
}
