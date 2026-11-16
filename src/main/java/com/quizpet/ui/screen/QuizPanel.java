package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.MultipleChoicePanel;
import main.java.com.quizpet.ui.component.OXQuestionPanel;

public class QuizPanel extends JPanel {
  private final JLabel questionTypeLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel progressLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel statusLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JPanel questionContainer = new JPanel(new BorderLayout());
  private final JButton submitButton = UiStyle.button("답안 제출");
  private final JButton endButton = UiStyle.button("종료");
  private Consumer<String> onSubmit = answer -> { };
  private Runnable onEnd = () -> { };
  private QuizSession session;
  private OXQuestionPanel oxQuestionPanel;
  private MultipleChoicePanel multipleChoicePanel;

  public QuizPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 12));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
    JPanel header = new JPanel(new BorderLayout(0, 10));
    header.setOpaque(false);
    header.add(UiStyle.label("퀴즈", UiStyle.TITLE_FONT), BorderLayout.NORTH);
    JPanel information = new JPanel(new BorderLayout());
    information.setOpaque(false);
    information.add(questionTypeLabel, BorderLayout.WEST);
    information.add(progressLabel, BorderLayout.EAST);
    header.add(information, BorderLayout.SOUTH);
    add(header, BorderLayout.NORTH);
    questionContainer.setOpaque(false);
    add(questionContainer, BorderLayout.CENTER);
    JPanel footer = new JPanel(new BorderLayout(0, 10));
    footer.setOpaque(false);
    statusLabel.setPreferredSize(new Dimension(0, 24));
    footer.add(statusLabel, BorderLayout.NORTH);
    submitButton.addActionListener(e -> submitAnswer());
    footer.add(submitButton, BorderLayout.CENTER);
    endButton.addActionListener(e -> onEnd.run());
    footer.add(endButton, BorderLayout.SOUTH);
    add(footer, BorderLayout.SOUTH);
    submitButton.setEnabled(false);
    endButton.setEnabled(false);
  }

  public void setOnSubmit(Consumer<String> action) {
    onSubmit = Objects.requireNonNull(action);
  }

  public void setOnEnd(Runnable action) {
    onEnd = Objects.requireNonNull(action);
  }

  public String getSelectedAnswer() {
    if (oxQuestionPanel != null) return oxQuestionPanel.getSelectedAnswer();
    return multipleChoicePanel == null ? null : multipleChoicePanel.getSelectedAnswer();
  }

  public boolean isFinished() {
    return session != null && session.isFinished();
  }

  public void showSession(QuizSession session) {
    this.session = Objects.requireNonNull(session, "session은 필수입니다.");
    Question question = session.getCurrentQuestion();
    questionContainer.removeAll();
    oxQuestionPanel = null;
    multipleChoicePanel = null;
    if (question instanceof MultipleChoiceQuestion multipleChoice) {
      multipleChoicePanel = new MultipleChoicePanel(question.getPrompt(), multipleChoice.getOptions());
      questionContainer.add(multipleChoicePanel, BorderLayout.CENTER);
      questionTypeLabel.setText(session.getCategory().getName() + " · 4지선다");
    } else {
      oxQuestionPanel = new OXQuestionPanel(question.getPrompt());
      questionContainer.add(oxQuestionPanel, BorderLayout.CENTER);
      questionTypeLabel.setText(session.getCategory().getName() + " · OX");
    }
    progressLabel.setText((session.getCurrentIndex() + 1) + " / " + session.getTotalCount());
    statusLabel.setText(session.getCurrentIndex() == session.getTotalCount() - 1 ? "마지막 문제입니다." : "");
    boolean answered = session.isCurrentQuestionAnswered();
    submitButton.setEnabled(!session.isFinished() && !answered);
    endButton.setEnabled(!session.isFinished());
    observeAnswerSelection(questionContainer);
    if (answered || session.isFinished()) disableAnswerControls(questionContainer);
    revalidate();
    repaint();
  }

  private void submitAnswer() {
    if (session == null || session.isFinished() || session.isCurrentQuestionAnswered()) return;
    String answer = getSelectedAnswer();
    if (answer == null) {
      statusLabel.setText("답안을 먼저 선택해 주세요.");
      return;
    }
    onSubmit.accept(answer);
    if (session.isCurrentQuestionAnswered()) {
      submitButton.setEnabled(false);
      disableAnswerControls(questionContainer);
    }
  }

  private void observeAnswerSelection(Container container) {
    for (Component child : container.getComponents()) {
      if (child instanceof JToggleButton button) {
        button.addItemListener(e -> statusLabel.setText(""));
      } else if (child instanceof JList<?> list) {
        list.addListSelectionListener(e -> statusLabel.setText(""));
      }
      if (child instanceof Container nested) observeAnswerSelection(nested);
    }
  }

  private static void disableAnswerControls(Container container) {
    for (Component child : container.getComponents()) {
      if (child instanceof JToggleButton || child instanceof JList<?>) child.setEnabled(false);
      if (child instanceof Container nested) disableAnswerControls(nested);
    }
  }
}
