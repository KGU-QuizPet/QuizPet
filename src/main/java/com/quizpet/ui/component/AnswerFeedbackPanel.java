package main.java.com.quizpet.ui.component;

import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.service.GrowthService.RewardDecision;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Objects;

/** UI-06_3 정답·해설. FE2 QuizPanel의 문제 자리에 넣어 씁니다. */
public class AnswerFeedbackPanel extends JPanel {
    /** 4지선다 답은 보기 문장으로 넣습니다. */
    public record AnswerFeedbackView(
            boolean correct,
            String selectedAnswer,
            String correctAnswer,
            String explanation,
            String statType,
            int statGain,
            int gainedXp,
            boolean hasLearningCard,
            boolean lastQuestion,
            boolean rewardConfirmed
    ) {
        public AnswerFeedbackView(boolean correct, String selectedAnswer, String correctAnswer, String explanation,
                                  String statType, int statGain, int gainedXp,
                                  boolean hasLearningCard, boolean lastQuestion) {
            this(correct, selectedAnswer, correctAnswer, explanation, statType, statGain, gainedXp,
                    hasLearningCard, lastQuestion, true);
        }
    }

    // FE1 UiStyle과 같은 값 (UiStyle이 main에 머지되면 UiStyle로 교체)
    private static final Color BACKGROUND = new Color(0xFFF8EE);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(0xDADCE3);
    private static final Color TEXT = new Color(0x1F2330);
    private static final Color TEXT_MUTED = new Color(0x6B7080);
    private static final Color CORRECT = new Color(0x2F9E44);
    private static final Color WRONG = new Color(0xE03131);
    private static final Color HIGHLIGHT = new Color(0xF08C00);
    private static final Font HEADLINE = new Font(Font.DIALOG, Font.BOLD, 28);
    private static final Font SUBTITLE = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font BODY = new Font(Font.DIALOG, Font.PLAIN, 15);
    private static final Font BODY_BOLD = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font SMALL = new Font(Font.DIALOG, Font.PLAIN, 13);

    private final JLabel verdictLabel = label(" ", HEADLINE, TEXT);
    private final JLabel myAnswerLabel = label(" ", BODY, TEXT);
    private final JLabel correctAnswerLabel = label(" ", BODY_BOLD, CORRECT);
    private final JTextArea explanationArea = new JTextArea();
    private final JLabel statRewardLabel = label(" ", SUBTITLE, TEXT);
    private final JLabel xpRewardLabel = label(" ", SUBTITLE, TEXT);
    private final JLabel rewardNoteLabel = label(" ", SMALL, TEXT_MUTED);

    private final JButton viewCardButton = button("학습 카드 보기");
    private final JButton nextButton = button("다음 문제");
    private final JScrollPane bodyScroll = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private Runnable onViewCard = () -> { };
    private Runnable onNext = () -> { };

    public AnswerFeedbackPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);

        verdictLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(verdictLabel, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        viewCardButton.addActionListener(e -> onViewCard.run());
        nextButton.addActionListener(e -> onNext.run());
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel answers = card();
        answers.setLayout(new GridLayout(2, 1, 0, 6));
        answers.add(myAnswerLabel);
        answers.add(correctAnswerLabel);
        body.add(leftAligned(answers));
        body.add(Box.createVerticalStrut(12));

        JPanel explanationCard = card();
        explanationCard.setLayout(new BorderLayout(0, 6));
        explanationCard.add(label("해설", BODY_BOLD, TEXT_MUTED), BorderLayout.NORTH);
        explanationArea.setFont(BODY);
        explanationArea.setForeground(TEXT);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setEditable(false);
        explanationArea.setFocusable(false);
        explanationArea.setOpaque(false);
        // 줄바꿈된 해설 높이는 폭이 정해진 뒤에 알 수 있어서, 폭이 바뀌면 다시 배치합니다.
        explanationArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(AnswerFeedbackPanel.this::revalidate);
            }
        });
        explanationCard.add(explanationArea, BorderLayout.CENTER);
        body.add(leftAligned(explanationCard));
        body.add(Box.createVerticalStrut(12));

        JPanel reward = card();
        reward.setLayout(new BorderLayout(0, 4));
        JPanel rewardRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rewardRow.setOpaque(false);
        rewardRow.add(statRewardLabel);
        rewardRow.add(Box.createHorizontalStrut(24));
        rewardRow.add(xpRewardLabel);
        reward.add(rewardRow, BorderLayout.NORTH);
        reward.add(rewardNoteLabel, BorderLayout.CENTER);
        body.add(leftAligned(reward));

        JPanel wrapper = new WidthTrackingPanel();
        wrapper.add(body, BorderLayout.NORTH);
        // 긴 해설은 본문만 세로 스크롤하고, 판정 문구와 버튼은 고정합니다 (PR #37 리뷰).
        bodyScroll.setViewportView(wrapper);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        return bodyScroll;
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.add(viewCardButton);
        buttons.add(nextButton);
        return buttons;
    }

    public void showFeedback(AnswerFeedbackView view) {
        Objects.requireNonNull(view, "AnswerFeedbackView가 null입니다. 채점 결과를 먼저 받아오세요.");

        if (view.correct()) {
            verdictLabel.setText("정답입니다!");
            verdictLabel.setForeground(CORRECT);
        } else {
            verdictLabel.setText("오답입니다");
            verdictLabel.setForeground(WRONG);
        }

        myAnswerLabel.setText("내 답:  " + orDash(view.selectedAnswer()));
        myAnswerLabel.setForeground(view.correct() ? TEXT : WRONG);
        correctAnswerLabel.setText("정답:  " + orDash(view.correctAnswer()));

        String explanation = view.explanation();
        explanationArea.setText(explanation == null || explanation.isBlank()
                ? "해설이 등록되지 않은 문제입니다."
                : explanation);

        boolean rewarded = view.gainedXp() > 0 || view.statGain() > 0;
        String pending = rewarded && !view.rewardConfirmed() ? " (예정)" : "";
        statRewardLabel.setText(orDash(view.statType()) + " " + signed(view.statGain()) + pending);
        xpRewardLabel.setText("EXP " + signed(view.gainedXp()) + pending);
        statRewardLabel.setForeground(rewarded ? HIGHLIGHT : TEXT_MUTED);
        xpRewardLabel.setForeground(rewarded ? HIGHLIGHT : TEXT_MUTED);
        rewardNoteLabel.setText(rewardNote(view));

        viewCardButton.setEnabled(view.hasLearningCard());
        viewCardButton.setToolTipText(view.hasLearningCard() ? null : "연결된 학습 카드가 없습니다.");
        nextButton.setText(view.lastQuestion() ? "결과 보기" : "다음 문제");

        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> bodyScroll.getVerticalScrollBar().setValue(0));
    }

    /** selectedAnswer는 QuizService.submitAnswer에 넘긴 값 그대로 넣습니다 (4지선다 "1"~"4"는 보기 문장으로 바꿔 표시). */
    public void showFeedback(Question question, String selectedAnswer, boolean correct,
                             int statGain, int gainedXp, boolean hasLearningCard, boolean lastQuestion) {
        Objects.requireNonNull(question, "Question이 null입니다. 현재 문제를 먼저 받아오세요.");
        showFeedback(new AnswerFeedbackView(correct,
                answerText(question, selectedAnswer), answerText(question, question.getAnswer()),
                question.getExplanation(), question.getCategory().getStatType(),
                statGain, gainedXp, hasLearningCard, lastQuestion));
    }

    /** rewardSaved가 false면 보상을 "(예정)"으로 표시합니다 (이슈 #28). */
    public void showFeedback(Question question, String selectedAnswer, boolean correct, RewardDecision reward,
                             boolean rewardSaved, boolean hasLearningCard, boolean lastQuestion) {
        Objects.requireNonNull(question, "Question이 null입니다. 현재 문제를 먼저 받아오세요.");
        Objects.requireNonNull(reward, "RewardDecision이 null입니다. 보상 판단을 먼저 받아오세요.");
        showFeedback(new AnswerFeedbackView(correct,
                answerText(question, selectedAnswer), answerText(question, question.getAnswer()),
                question.getExplanation(), question.getCategory().getStatType(),
                reward.getPlannedStatGain(), reward.getPlannedXp(), hasLearningCard, lastQuestion, rewardSaved));
    }

    private static String answerText(Question question, String answer) {
        if (question instanceof MultipleChoiceQuestion choice && answer != null) {
            List<String> options = choice.getOptions();
            for (int i = 0; i < options.size(); i++) {
                if (answer.equals(String.valueOf(i + 1))) {
                    return (i + 1) + ". " + options.get(i);
                }
            }
        }
        return answer;
    }

    private static String rewardNote(AnswerFeedbackView view) {
        if (!view.correct()) {
            return "틀린 문제는 보상이 없어요. 해설을 확인해 보세요.";
        }
        if (view.gainedXp() == 0) {
            return "이미 맞힌 문제라 추가 보상은 없어요.";
        }
        if (!view.rewardConfirmed()) {
            return "처음 맞힌 문제예요! 보상은 저장이 끝나면 반영돼요.";
        }
        return "처음 맞힌 문제라 보상을 받았어요!";
    }

    public void setOnViewCard(Runnable action) {
        this.onViewCard = (action != null) ? action : () -> { };
    }

    public void setOnNext(Runnable action) {
        this.onNext = (action != null) ? action : () -> { };
    }

    public void setNextButtonVisible(boolean visible) {
        nextButton.setVisible(visible);
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return panel;
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(BODY);
        button.setFocusPainted(false);
        return button;
    }

    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private static String signed(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private static String orDash(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
