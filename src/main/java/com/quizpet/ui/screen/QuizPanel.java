package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.model.ShortAnswerQuestion;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.ImageButton;
import main.java.com.quizpet.ui.component.MultipleChoicePanel;
import main.java.com.quizpet.ui.component.OXQuestionPanel;
import main.java.com.quizpet.ui.component.QuizProgressBar;
import main.java.com.quizpet.ui.component.ShortAnswerPanel;
import main.java.com.quizpet.ui.component.CategoryIcon;
import main.java.com.quizpet.ui.component.QuestionCard;
import main.java.com.quizpet.ui.component.Badge;

public class QuizPanel extends JPanel {
  private final JLabel questionTypeLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final Badge typeBadge = new Badge("", UiStyle.SUNKEN, UiStyle.MUTED,
      UiStyle.BODY_FONT.deriveFont(java.awt.Font.BOLD, 13f), 0);
  private final JPanel progressDetails = new JPanel(new BorderLayout(16, 0));
  private final QuizProgressBar progressBar = new QuizProgressBar();
  private final JLabel progressLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel statusLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JPanel questionContainer = new JPanel(new BorderLayout());
  private final JButton submitButton = new ImageButton("답안 제출");
  private final JButton endButton = new ImageButton("종료");
  private Consumer<String> onSubmit = answer -> { };
  private Runnable onEnd = () -> { };
  private QuizSession session;
  private OXQuestionPanel oxQuestionPanel;
  private MultipleChoicePanel multipleChoicePanel;
  private ShortAnswerPanel shortAnswerPanel;

  public QuizPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
    JPanel header = new JPanel(new BorderLayout(0, 10)) {
      @Override public void doLayout() {
        Container target = QuizPanel.this.getWidth() >= 760 ? progressDetails : this;
        if (progressBar.getParent() != target) {
          if (progressBar.getParent() != null) progressBar.getParent().remove(progressBar);
          target.add(progressBar, target == this ? BorderLayout.SOUTH : BorderLayout.WEST);
        }
        super.doLayout();
      }
    };
    header.setOpaque(false);
    JPanel information = new JPanel(new BorderLayout());
    information.setOpaque(false);
    JPanel category = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
    category.setOpaque(false);
    category.add(questionTypeLabel);
    category.add(typeBadge);
    information.add(category, BorderLayout.WEST);
    questionTypeLabel.setHorizontalAlignment(JLabel.LEFT);
    progressLabel.setForeground(UiStyle.MUTED);
    progressDetails.setOpaque(false);
    progressDetails.add(progressLabel, BorderLayout.CENTER);
    progressDetails.add(endButton, BorderLayout.EAST);
    information.add(progressDetails, BorderLayout.EAST);
    header.add(information, BorderLayout.CENTER);
    header.add(progressBar, BorderLayout.SOUTH);
    add(header, BorderLayout.NORTH);
    questionContainer.setOpaque(false);
    add(questionContainer, BorderLayout.CENTER);
    JPanel footer = new JPanel(new BorderLayout(0, 10));
    footer.setOpaque(false);
    statusLabel.setHorizontalAlignment(JLabel.LEFT);
    statusLabel.setForeground(UiStyle.MUTED);
    footer.add(statusLabel, BorderLayout.CENTER);
    submitButton.addActionListener(e -> submitAnswer());
    submitButton.setForeground(UiStyle.SURFACE);
    submitButton.setPreferredSize(new Dimension(166, 60));
    footer.add(submitButton, BorderLayout.EAST);
    endButton.setBackground(Color.WHITE);
    endButton.addActionListener(e -> onEnd.run());
    add(footer, BorderLayout.SOUTH);
    submitButton.setEnabled(false);
    endButton.setEnabled(false);
    bindQuizKey("ENTER", "quiz.submit", () -> submitButton.doClick());
    for (String answer : java.util.List.of("O", "X", "1", "2", "3", "4")) {
      bindQuizKey(answer, "quiz.answer." + answer, () -> {
        if (shortAnswerPanel == null) selectAnswer(questionContainer, answer);
      });
    }
  }

  public void setOnSubmit(Consumer<String> action) {
    onSubmit = Objects.requireNonNull(action);
  }

  public void setOnEnd(Runnable action) {
    onEnd = Objects.requireNonNull(action);
  }

  public String getSelectedAnswer() {
    if (shortAnswerPanel != null) return shortAnswerPanel.getSelectedAnswer();
    if (oxQuestionPanel != null) return oxQuestionPanel.getSelectedAnswer();
    return multipleChoicePanel == null ? null : multipleChoicePanel.getSelectedAnswer();
  }

  public boolean isFinished() {
    return session != null && session.isFinished();
  }

  public void showSession(QuizSession session) {
    this.session = Objects.requireNonNull(session, "session은 필수입니다.");
    Question question = session.getCurrentQuestion();
    String categoryName = (session.isReview() ? "복습 · " : "") + question.getCategory().getName();
    questionTypeLabel.setForeground(UiStyle.categoryColor(question.getCategory()));
    questionTypeLabel.setIcon(new CategoryIcon(question.getCategory(), 28));
    questionContainer.removeAll();
    oxQuestionPanel = null;
    multipleChoicePanel = null;
    shortAnswerPanel = null;
    if (question instanceof MultipleChoiceQuestion multipleChoice) {
      multipleChoicePanel = new MultipleChoicePanel(question.getPrompt(), multipleChoice.getOptions());
      questionContainer.add(multipleChoicePanel, BorderLayout.CENTER);
      typeBadge.setText("4지선다");
    } else if (question instanceof ShortAnswerQuestion) {
      shortAnswerPanel = new ShortAnswerPanel(question.getPrompt());
      questionContainer.add(shortAnswerPanel, BorderLayout.CENTER);
      typeBadge.setText("단답형");
    } else {
      oxQuestionPanel = new OXQuestionPanel(question.getPrompt());
      questionContainer.add(oxQuestionPanel, BorderLayout.CENTER);
      typeBadge.setText("OX");
    }
    questionTypeLabel.setText(categoryName);
    progressBar.showSession(session);
    for (Component child : questionContainer.getComponents()) {
      if (child instanceof Container container) setQuestionNumber(container, session.getCurrentIndex() + 1);
    }
    progressLabel.setText((session.getCurrentIndex() + 1) + " / " + session.getTotalCount());
    statusLabel.setText(session.getCurrentIndex() == session.getTotalCount() - 1 ? "마지막 문제입니다." : "");
    boolean answered = session.isCurrentQuestionAnswered();
    submitButton.setEnabled(false);
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
      statusLabel.setText("답안을 먼저 선택하거나 입력해 주세요.");
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
        button.addItemListener(e -> updateSelection());
      } else if (child instanceof JList<?> list) {
        list.addListSelectionListener(e -> updateSelection());
      } else if (child instanceof JTextField field) {
        field.addActionListener(e -> submitButton.doClick());
        field.getDocument().addDocumentListener(new DocumentListener() {
          @Override public void insertUpdate(DocumentEvent e) { updateSelection(); }
          @Override public void removeUpdate(DocumentEvent e) { updateSelection(); }
          @Override public void changedUpdate(DocumentEvent e) { updateSelection(); }
        });
      }
      if (child instanceof Container nested) observeAnswerSelection(nested);
    }
  }

  private void bindQuizKey(String stroke, String name, Runnable action) {
    getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke(stroke), name);
    getActionMap().put(name, new javax.swing.AbstractAction() {
      @Override public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
    });
  }

  private static void selectAnswer(Container root, String answer) {
    for (Component child : root.getComponents()) {
      if (child instanceof JToggleButton button && answer.equals(button.getActionCommand()) && button.isEnabled()) {
        button.doClick();
      } else if (child instanceof Container nested) selectAnswer(nested, answer);
    }
  }

  private void updateSelection() {
    String answer = getSelectedAnswer();
    statusLabel.setText(answer == null ? "답을 고르거나 입력한 뒤 제출할 수 있어요."
        : "답안을 제출하면 정답과 해설을 확인할 수 있어요.");
    statusLabel.setToolTipText(statusLabel.getText());
    submitButton.setEnabled(session != null && !session.isFinished()
        && !session.isCurrentQuestionAnswered() && getSelectedAnswer() != null);
  }

  private static void setQuestionNumber(Container root, int number) {
    if (root instanceof QuestionCard card) card.setNumber(number);
    for (Component child : root.getComponents()) if (child instanceof Container nested) setQuestionNumber(nested, number);
  }

  private static void disableAnswerControls(Container container) {
    for (Component child : container.getComponents()) {
      if (child instanceof JToggleButton || child instanceof JList<?> || child instanceof JTextField) {
        child.setEnabled(false);
      }
      if (child instanceof Container nested) disableAnswerControls(nested);
    }
  }
}
