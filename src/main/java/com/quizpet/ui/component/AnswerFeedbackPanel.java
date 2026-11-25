package main.java.com.quizpet.ui.component;

import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.ShortAnswerQuestion;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.service.GrowthService.RewardDecision;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
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

    private static final Color BACKGROUND = UiStyle.BACKGROUND;
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = UiStyle.BORDER;
    private static final Color TEXT = UiStyle.TEXT;
    private static final Color TEXT_MUTED = UiStyle.MUTED;
    private static final Color CORRECT = UiStyle.CORRECT;
    private static final Color WRONG = UiStyle.WRONG;
    private static final Color HIGHLIGHT = UiStyle.BRAND;
    private static final Font HEADLINE = new Font(Font.DIALOG, Font.BOLD, 28);
    private static final Font SUBTITLE = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font BODY = new Font(Font.DIALOG, Font.PLAIN, 15);
    private static final Font BODY_BOLD = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font SMALL = new Font(Font.DIALOG, Font.PLAIN, 13);

    private final QuizProgressBar progressBar = new QuizProgressBar();
    private final JPanel answers = card();
    private final JPanel questionPreview = new JPanel(new BorderLayout());
    private final JLabel verdictLabel = label(" ", HEADLINE, TEXT);
    private final JLabel myAnswerLabel = label(" ", BODY, TEXT);
    private final JLabel correctAnswerLabel = label(" ", BODY_BOLD, CORRECT);
    private final JTextArea explanationArea = new JTextArea();
    private final JLabel statRewardLabel = label(" ", SUBTITLE, TEXT);
    private final JLabel xpRewardLabel = label(" ", SUBTITLE, TEXT);
    private final JLabel rewardNoteLabel = label(" ", SMALL, TEXT_MUTED);

    private final JButton viewCardButton = new ImageButton("학습 카드 보기", "/images/pets/small/duck_focus.png", 24);
    private final JButton nextButton = new ImageButton("다음 문제");
    private final JScrollPane bodyScroll = new JScrollPane(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    /** 정답이면 기뻐하고 오답이면 시무룩해지는 펫 반응입니다(UI-06C·06D). */
    private final PetStage petStage = new PetStage();
    private String petSpriteKey = "DUCK";

    private Runnable onViewCard = () -> { };
    private Runnable onNext = () -> { };

    public AnswerFeedbackPanel() {
        setLayout(new BorderLayout(20, 16));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        verdictLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(progressBar, BorderLayout.NORTH);
        header.add(verdictLabel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        add(buildPetColumn(), BorderLayout.WEST);
        add(buildBody(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        viewCardButton.setBackground(Color.WHITE);
        viewCardButton.addActionListener(e -> onViewCard.run());
        nextButton.addActionListener(e -> onNext.run());
    }

    /**
     * 왼쪽 펫 열입니다. 펫 무대는 남는 높이를 모두 채우고, 아래에 성장 현황 카드를 둡니다.
     * 열 폭은 화면 폭의 1/5을 260~360px 범위로 맞춰 큰 화면에서는 캐릭터도 커집니다.
     */
    private JComponent buildPetColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 12)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = petColumnWidth(AnswerFeedbackPanel.this.getWidth());
                return size;
            }
        };
        column.setOpaque(false);
        petStage.setPreferredSize(new Dimension(PET_COLUMN_MIN, 280));
        column.add(petStage, BorderLayout.CENTER);
        column.add(buildGrowthCard(), BorderLayout.SOUTH);
        return column;
    }

    static final int PET_COLUMN_MIN = 260;
    static final int PET_COLUMN_MAX = 360;

    static int petColumnWidth(int panelWidth) {
        return Math.max(PET_COLUMN_MIN, Math.min(PET_COLUMN_MAX, panelWidth / 5));
    }

    private static final Font CARD_CAPTION = new Font(Font.DIALOG, Font.BOLD, 12);
    private static final Font CARD_VALUE = new Font(Font.DIALOG, Font.BOLD, 13);

    private final PetStatusPanel.RoundedBox growthCard = new PetStatusPanel.RoundedBox(SURFACE, BORDER);
    private final Badge levelBadge = new Badge("Lv.1", UiStyle.INK_PANEL, Color.WHITE, CARD_VALUE, 48);
    private final JLabel levelNote = label(" ", CARD_VALUE, HIGHLIGHT);
    private final GaugeBar expBar = new GaugeBar(UiStyle.EXP, 12, true);
    private final JLabel expLabel = label(" ", CARD_CAPTION, TEXT);
    private final JLabel expGainLabel = label(" ", CARD_CAPTION, UiStyle.CORRECT);
    private final java.util.Map<String, GaugeBar> statBars = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, JLabel> statNames = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, JLabel> statValueLabels = new java.util.LinkedHashMap<>();
    private String shownStatType = "INT";
    private PetStatus petStatus;

    /** 피드백을 보여 줄 때의 캐릭터 상태입니다(보상이 반영된 뒤의 값). */
    private record PetStatus(int level, int xpInLevel, int xpPerLevel, java.util.Map<String, Integer> stats) { }

    /** 펫 무대 아래의 성장 현황 카드입니다. 레벨·경험치와 이번 문제 분야의 능력치를 보여 줍니다. */
    private JComponent buildGrowthCard() {
        growthCard.setLayout(new GridBagLayout());
        growthCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        growthCard.add(label("성장 현황", CARD_CAPTION, TEXT_MUTED), c);

        JPanel levelRow = new JPanel(new BorderLayout(8, 0));
        levelRow.setOpaque(false);
        levelRow.add(levelBadge, BorderLayout.WEST);
        levelRow.add(levelNote, BorderLayout.CENTER);
        c.gridy++;
        c.insets = new Insets(10, 0, 8, 0);
        growthCard.add(levelRow, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 4, 0);
        growthCard.add(expBar, c);

        JPanel expRow = new JPanel(new BorderLayout());
        expRow.setOpaque(false);
        expRow.add(expLabel, BorderLayout.WEST);
        expRow.add(expGainLabel, BorderLayout.EAST);
        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        growthCard.add(expRow, c);

        // 능력치 네 가지를 모두 보여 주고, 이번 문제로 오른 능력치만 증가량을 강조합니다.
        for (String type : UiStyle.STAT_TYPES) {
            c.gridy++;
            c.insets = new Insets(0, 0, 6, 0);
            growthCard.add(createStatRow(type), c);
        }

        growthCard.setVisible(false);
        return growthCard;
    }

    private JPanel createStatRow(String type) {
        Badge chip = new Badge(type, UiStyle.statColor(type), Color.WHITE, CARD_CAPTION, 44);
        JLabel name = label(UiStyle.statLabel(type), CARD_CAPTION, TEXT_MUTED);
        Dimension nameSize = new Dimension(30, name.getPreferredSize().height);
        name.setPreferredSize(nameSize);
        name.setMinimumSize(nameSize);
        GaugeBar bar = new GaugeBar(UiStyle.statColor(type), 8, false);
        bar.setPreferredSize(new Dimension(50, 8));
        bar.getAccessibleContext().setAccessibleName(UiStyle.statLabel(type) + "(" + type + ")");
        JLabel value = label(" ", CARD_VALUE, TEXT);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        // "(+1)"이 붙어도 막대 길이가 줄마다 같도록 값 칸 폭을 고정합니다.
        Dimension valueSize = new Dimension(58, value.getPreferredSize().height);
        value.setPreferredSize(valueSize);
        value.setMinimumSize(valueSize);
        statBars.put(type, bar);
        statNames.put(type, name);
        statValueLabels.put(type, value);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints s = new GridBagConstraints();
        s.gridy = 0;
        s.anchor = GridBagConstraints.WEST;
        s.insets = new Insets(0, 0, 0, 8);
        row.add(chip, s);
        row.add(name, s);
        s.weightx = 1;
        s.fill = GridBagConstraints.HORIZONTAL;
        row.add(bar, s);
        s.weightx = 0;
        s.fill = GridBagConstraints.NONE;
        s.insets = new Insets(0, 0, 0, 0);
        row.add(value, s);
        return row;
    }

    /**
     * 성장 현황 카드에 표시할 캐릭터 상태입니다. 보상이 반영된 뒤의 값을 넣고 showFeedback 전에 호출합니다.
     * 호출하지 않으면 카드를 숨깁니다(펫 무대가 열 전체를 씁니다).
     *
     * @param stats 키: STR·INT·FOC·WIS
     */
    public void setPetStatus(int level, int xpInLevel, int xpPerLevel, java.util.Map<String, Integer> stats) {
        if (stats == null) {
            throw new IllegalArgumentException("능력치는 필수입니다.");
        }
        petStatus = new PetStatus(level, xpInLevel, Math.max(1, xpPerLevel), java.util.Map.copyOf(stats));
    }

    /** 보상이 저장된 경우 늘어난 만큼을 막대 증가분으로 강조하며 채웁니다. */
    private void showGrowthCard(AnswerFeedbackView view) {
        if (petStatus == null || !isStatType(view.statType())) {
            growthCard.setVisible(false);
            return;
        }
        boolean rewarded = view.correct() && view.rewardConfirmed();
        int gainedXp = rewarded ? Math.max(0, view.gainedXp()) : 0;
        int xp = Math.max(0, Math.min(petStatus.xpInLevel(), petStatus.xpPerLevel()));
        boolean leveledUp = gainedXp > xp;
        levelBadge.setText("Lv." + petStatus.level());
        levelNote.setText(leveledUp ? "레벨 업!" : " ");
        expBar.setValue(xp, petStatus.xpPerLevel(), gainedXp > 0 ? (leveledUp ? 0 : xp - gainedXp) : -1,
                gainedXp > 0);
        expLabel.setText("EXP " + xp + " / " + petStatus.xpPerLevel());
        expGainLabel.setText(gainedXp > 0 ? "+" + gainedXp : " ");

        shownStatType = view.statType();
        int gain = rewarded ? Math.max(0, view.statGain()) : 0;
        java.util.Map<String, Integer> stats = petStatus.stats();
        int highest = 0;
        for (String type : UiStyle.STAT_TYPES) {
            highest = Math.max(highest, stats.getOrDefault(type, 0));
        }
        // 막대 최대값은 홈 사이드바와 같은 규칙이라 네 능력치를 같은 기준으로 비교할 수 있습니다.
        int scale = PetStatusPanel.statScale(highest);
        List<String> weakest = StatComparisonPanel.weakestStats(stats);
        for (String type : UiStyle.STAT_TYPES) {
            int value = stats.getOrDefault(type, 0);
            int typeGain = type.equals(shownStatType) ? gain : 0;
            statBars.get(type).setValue(value, scale, typeGain > 0 ? value - typeGain : -1, typeGain > 0);
            JLabel valueLabel = statValueLabels.get(type);
            valueLabel.setText(typeGain > 0 ? value + " (+" + typeGain + ")" : String.valueOf(value));
            valueLabel.setForeground(typeGain > 0 ? UiStyle.CORRECT : TEXT);
            JLabel name = statNames.get(type);
            boolean weak = weakest.contains(type);
            name.setForeground(weak ? WRONG : TEXT_MUTED);
            name.setToolTipText(weak ? "가장 낮은 능력치예요. "
                    + String.join("·", StatComparisonPanel.categoryNames(type)) + " 퀴즈를 맞히면 올라요." : null);
        }
        growthCard.setVisible(true);
    }

    public boolean isGrowthCardVisible() {
        return growthCard.isVisible();
    }

    public GaugeBar getGrowthExpBar() {
        return expBar;
    }

    /** 이번 문제 분야의 능력치 막대입니다. */
    public GaugeBar getGrowthStatBar() {
        return statBars.get(shownStatType);
    }

    public GaugeBar getGrowthStatBar(String statType) {
        return statBars.get(statType);
    }

    /** 이번 문제 분야의 능력치 값 문구입니다(예: "15 (+1)"). */
    public String getGrowthStatText() {
        return getGrowthStatText(shownStatType);
    }

    public String getGrowthStatText(String statType) {
        return statValueLabels.get(statType).getText();
    }

    public boolean isGrowthStatMarkedWeak(String statType) {
        return statNames.get(statType).getToolTipText() != null;
    }

    public String getGrowthLevelNote() {
        return levelNote.getText().strip();
    }

    /** 피드백에 반응할 캐릭터 외형입니다. AppController가 현재 캐릭터의 spriteKey를 넣습니다. */
    public void setPet(String spriteKey) {
        petSpriteKey = (spriteKey == null) ? "DUCK" : spriteKey;
    }

    public PetStage getPetStage() {
        return petStage;
    }

    /** 채점 결과에 맞는 펫 표정입니다. 오답이면 SAD, 정답이면 HAPPY입니다. */
    static PetMood petMood(AnswerFeedbackView view) {
        return view.correct() ? PetMood.HAPPY : PetMood.SAD;
    }

    /**
     * 채점 결과에 맞는 펫 말풍선입니다. 오답이면 학습 카드를 다시 보도록 권합니다.
     * 오답(카드 있음)·처음 정답 문구는 디자인 시안 UI-06D·06C 문구이고, 나머지는 시안에 없는 경우의 문구입니다.
     */
    static String petMessage(AnswerFeedbackView view) {
        if (!view.correct()) {
            return view.hasLearningCard() ? "으앙… 카드를 한 번 더 볼까?" : "아쉽다. 다음엔 맞혀 보자.";
        }
        return view.gainedXp() > 0 ? "정답이야! 머리가 똑똑해진 느낌!" : "정답! 이미 맞힌 문제야.";
    }

    /**
     * 정답 반응 때 캐릭터 위로 떠오를 획득 문구입니다. 보상이 저장된 경우에만 보여 주고,
     * 이미 맞힌 문제나 저장 전(예정) 보상이면 문구 없이 기뻐하기만 합니다.
     */
    static List<PetStage.Effect> petEffects(AnswerFeedbackView view) {
        if (!view.correct() || !view.rewardConfirmed()) {
            return List.of();
        }
        List<PetStage.Effect> effects = new java.util.ArrayList<>();
        if (view.gainedXp() > 0) {
            effects.add(new PetStage.Effect("+" + view.gainedXp() + " EXP", UiStyle.EXP_DARK));
        }
        if (view.statGain() > 0 && isStatType(view.statType())) {
            effects.add(new PetStage.Effect("+" + view.statGain() + " " + view.statType(),
                    UiStyle.statColor(view.statType())));
        }
        return List.copyOf(effects);
    }

    private static boolean isStatType(String statType) {
        return List.of(UiStyle.STAT_TYPES).contains(statType);
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new GridBagLayout());
        GridBagConstraints cell = new GridBagConstraints();
        cell.gridx = 0;
        cell.gridy = 0;
        cell.weightx = 1;
        cell.weighty = 0;
        cell.fill = GridBagConstraints.BOTH;
        cell.insets = new Insets(0, 0, 12, 0);

        questionPreview.setOpaque(false);
        questionPreview.setVisible(false);
        cell.weighty = 0.5;
        body.add(questionPreview, cell);
        cell.gridy++;
        cell.weighty = 0;

        answers.setLayout(new GridLayout(2, 1, 0, 6));
        answers.add(myAnswerLabel);
        answers.add(correctAnswerLabel);
        body.add(answers, cell);
        cell.gridy++;
        cell.weighty = 0.8;

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
        body.add(explanationCard, cell);
        cell.gridy++;
        cell.weighty = 0.2;

        JPanel reward = card();
        reward.setLayout(new BorderLayout(0, 4));
        JPanel rewardRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rewardRow.setOpaque(false);
        rewardRow.add(statRewardLabel);
        rewardRow.add(Box.createHorizontalStrut(24));
        rewardRow.add(xpRewardLabel);
        reward.add(rewardRow, BorderLayout.NORTH);
        reward.add(rewardNoteLabel, BorderLayout.CENTER);
        body.add(reward, cell);

        JPanel wrapper = new WidthTrackingPanel();
        wrapper.add(body, BorderLayout.CENTER);
        // 긴 해설은 본문만 세로 스크롤하고, 판정 문구와 버튼은 고정합니다 (PR #37 리뷰).
        bodyScroll.setViewportView(wrapper);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        return bodyScroll;
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(viewCardButton);
        buttons.add(nextButton);
        return buttons;
    }

    public void showSession(QuizSession session) {
        progressBar.showSession(Objects.requireNonNull(session));
    }

    private void showQuestion(Question question, String selectedAnswer) {
        questionPreview.removeAll();
        if (question instanceof MultipleChoiceQuestion choice) {
            MultipleChoicePanel panel = new MultipleChoicePanel(question.getPrompt(), choice.getOptions());
            panel.showResult(selectedAnswer, question.getAnswer());
            questionPreview.add(panel);
        } else if (question instanceof ShortAnswerQuestion) {
            ShortAnswerPanel panel = new ShortAnswerPanel(question.getPrompt());
            panel.showResult(selectedAnswer);
            questionPreview.add(panel);
        } else {
            OXQuestionPanel panel = new OXQuestionPanel(question.getPrompt());
            panel.showResult(selectedAnswer, question.getAnswer());
            questionPreview.add(panel);
        }
        questionPreview.setPreferredSize(new Dimension(380, 284));
        questionPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 284));
        questionPreview.setVisible(true);
        answers.setVisible(question instanceof ShortAnswerQuestion);
        revalidate();
        repaint();
    }

    public void showFeedback(AnswerFeedbackView view) {
        Objects.requireNonNull(view, "AnswerFeedbackView가 null입니다. 채점 결과를 먼저 받아오세요.");
        questionPreview.setVisible(false);
        answers.setVisible(true);

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
        petStage.showPet(petSpriteKey, petMood(view), petMessage(view));
        if (view.correct()) {
            petStage.celebrate(petEffects(view));
        } else {
            petStage.shake();
        }
        showGrowthCard(view);

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
        showQuestion(question, selectedAnswer);
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
        showQuestion(question, selectedAnswer);
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
            return getParent() != null && getPreferredSize().height <= getParent().getHeight();
        }
    }
}
