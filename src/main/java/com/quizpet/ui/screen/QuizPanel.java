package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.MultipleChoicePanel;
import main.java.com.quizpet.ui.component.OXQuestionPanel;

public class QuizPanel extends JPanel {
  private static final List<PreviewQuestion> PREVIEW_QUESTIONS = List.of(
      new PreviewQuestion("Q. 물은 고체, 액체, 기체의 세 가지 상태로 존재할 수 있다."),
      new PreviewQuestion("Q. 다음 중 태양계의 행성 이름을 하나 골라 보세요.",
          "수성", "북극성", "달", "태양"),
      new PreviewQuestion("Q. 지구의 자전으로 낮과 밤이 바뀐다."),
      new PreviewQuestion("Q. 다음 중 지구의 움직임을 설명한 문장을 하나 골라 보세요.",
          "지구는 스스로 회전한다.", "지구는 태양 주위를 돈다.",
          "지구의 움직임은 낮과 밤, 계절을 이해하는 데 도움이 된다.",
          "지구의 자전과 공전은 서로 다른 움직임이다."));

  private final ScreenManager screens;
  private final JLabel questionTypeLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel progressLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel statusLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JLabel ruleLabel = UiStyle.label("", UiStyle.BODY_FONT.deriveFont(12f));
  private final JPanel questionContainer = new JPanel(new BorderLayout());
  private final JButton nextButton = UiStyle.button("다음 문제");
  private OXQuestionPanel oxQuestionPanel;
  private MultipleChoicePanel multipleChoicePanel;
  private int currentQuestionIndex;
  private boolean finished;
  private boolean answerRequired;

  public QuizPanel(ScreenManager screens) {
    this.screens = Objects.requireNonNull(screens, "screens는 필수입니다.");
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
    ruleLabel.setPreferredSize(new Dimension(0, 18));
    footer.add(statusLabel, BorderLayout.NORTH);
    nextButton.addActionListener(e -> moveToNextQuestion());
    JPanel navigation = new JPanel(new BorderLayout(0, 6));
    navigation.setOpaque(false);
    navigation.add(nextButton, BorderLayout.NORTH);
    navigation.add(ruleLabel, BorderLayout.SOUTH);
    footer.add(navigation, BorderLayout.CENTER);
    add(footer, BorderLayout.SOUTH);
    showCurrentQuestion();
  }

  public String getSelectedAnswer() {
    return oxQuestionPanel != null
        ? oxQuestionPanel.getSelectedAnswer() : multipleChoicePanel.getSelectedAnswer();
  }

  public boolean isFinished() {
    return finished;
  }

  private void showCurrentQuestion() {
    PreviewQuestion question = PREVIEW_QUESTIONS.get(currentQuestionIndex);
    questionContainer.removeAll();
    oxQuestionPanel = null;
    multipleChoicePanel = null;
    if (question.options.isEmpty()) {
      oxQuestionPanel = new OXQuestionPanel(question.text);
      questionContainer.add(oxQuestionPanel, BorderLayout.CENTER);
      questionTypeLabel.setText("과학&상식 · OX");
    } else {
      multipleChoicePanel = new MultipleChoicePanel(question.text, question.options);
      questionContainer.add(multipleChoicePanel, BorderLayout.CENTER);
      questionTypeLabel.setText("과학&상식 · 4지선다");
    }
    progressLabel.setText((currentQuestionIndex + 1) + " / " + PREVIEW_QUESTIONS.size());
    boolean lastQuestion = currentQuestionIndex == PREVIEW_QUESTIONS.size() - 1;
    nextButton.setText(lastQuestion ? "완료" : "다음 문제");
    ruleLabel.setText(currentQuestionIndex == 0
        ? "다음 문제로 이동하면 이전 답안을 수정할 수 없습니다." : "");
    answerRequired = false;
    observeAnswerSelection(questionContainer);
    updateStatus();
    revalidate();
    repaint();
  }

  private void moveToNextQuestion() {
    if (finished) {
      return;
    }
    if (getSelectedAnswer() == null) {
      answerRequired = true;
      updateStatus();
      return;
    }
    if (currentQuestionIndex == PREVIEW_QUESTIONS.size() - 1) {
      finished = true;
      nextButton.setText("완료됨");
      nextButton.setEnabled(false);
      statusLabel.setText("임시 퀴즈 완료 · 결과 화면 연결 없음");
      disableAnswerControls(questionContainer);
      return;
    }
    currentQuestionIndex++;
    showCurrentQuestion();
  }

  private void updateStatus() {
    if (finished) {
      return;
    }
    if (getSelectedAnswer() != null) {
      answerRequired = false;
    }
    if (answerRequired) {
      statusLabel.setText("답안을 먼저 선택해 주세요.");
    } else {
      statusLabel.setText(currentQuestionIndex == PREVIEW_QUESTIONS.size() - 1
          ? "마지막 문제입니다." : "");
    }
  }

  private void observeAnswerSelection(Container container) {
    for (Component component : container.getComponents()) {
      if (component instanceof JToggleButton button) {
        button.addItemListener(e -> updateStatus());
      } else if (component instanceof JList<?> list) {
        list.addListSelectionListener(e -> {
          if (!e.getValueIsAdjusting()) {
            updateStatus();
          }
        });
      }
      if (component instanceof Container child) {
        observeAnswerSelection(child);
      }
    }
  }

  private static void disableAnswerControls(Container container) {
    for (Component component : container.getComponents()) {
      if (component instanceof JToggleButton || component instanceof JList<?>) {
        component.setEnabled(false);
      }
      if (component instanceof Container child) {
        disableAnswerControls(child);
      }
    }
  }

  private static class PreviewQuestion {
    private final String text;
    private final List<String> options;

    PreviewQuestion(String text, String... options) {
      this.text = text;
      this.options = List.of(options);
    }
  }
}
