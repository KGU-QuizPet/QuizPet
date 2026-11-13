package main.java.com.quizpet.ui.screen;

import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.QuizResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

/** UI-07 퀴즈 결과. */
public class QuizResultPanel extends JPanel {
    public static final String SCREEN_NAME = "QUIZ_RESULT";

    /** 정답률과 answeredCount는 BE2 QuizResult처럼 제출한 문제 기준입니다. */
    public record QuizResultView(
            String categoryName,
            String statType,
            int correctCount,
            int totalCount,
            double accuracyPercent,
            int gainedXp,
            int statGain,
            int levelBefore,
            int levelAfter,
            int xpInLevel,
            int xpPerLevel,
            int answeredCount
    ) {
        public QuizResultView(String categoryName, String statType, int correctCount, int totalCount,
                              double accuracyPercent, int gainedXp, int statGain,
                              int levelBefore, int levelAfter, int xpInLevel, int xpPerLevel) {
            this(categoryName, statType, correctCount, totalCount, accuracyPercent, gainedXp, statGain,
                    levelBefore, levelAfter, xpInLevel, xpPerLevel, totalCount);
        }

        public boolean leveledUp() {
            return levelAfter > levelBefore;
        }
    }

    private static final int DEFAULT_XP_PER_LEVEL = 50;

    // FE1 UiStyle과 같은 값 (UiStyle이 main에 머지되면 UiStyle로 교체)
    private static final Color BACKGROUND = new Color(0xFFF8EE);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(0xDADCE3);
    private static final Color TEXT = new Color(0x1F2330);
    private static final Color TEXT_MUTED = new Color(0x6B7080);
    private static final Color PRIMARY = new Color(0x6C8EBF);
    private static final Color HIGHLIGHT = new Color(0xF08C00);
    private static final Font TITLE = new Font(Font.DIALOG, Font.BOLD, 18);
    private static final Font HEADLINE = new Font(Font.DIALOG, Font.BOLD, 28);
    private static final Font SUBTITLE = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font BODY = new Font(Font.DIALOG, Font.PLAIN, 15);
    private static final Font BODY_BOLD = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font SMALL = new Font(Font.DIALOG, Font.PLAIN, 13);

    private final JLabel categoryLabel = centeredLabel(" ", BODY, TEXT_MUTED);
    private final JLabel scoreLabel = centeredLabel(" ", HEADLINE, TEXT);
    private final JLabel accuracyLabel = centeredLabel(" ", SUBTITLE, TEXT_MUTED);

    private final JLabel xpValue = label(" ", SUBTITLE, TEXT);
    private final JLabel statValue = label(" ", SUBTITLE, TEXT);
    private final JLabel levelValue = label(" ", SUBTITLE, TEXT);
    private final JLabel levelUpBadge = label("LEVEL UP!", BODY_BOLD, HIGHLIGHT);
    private final JProgressBar xpBar = new JProgressBar();

    private final JButton mainButton = button("메인으로");
    private final JButton historyButton = button("학습 기록");

    private Runnable onGoMain = () -> { };
    private Runnable onGoHistory = () -> { };

    public QuizResultPanel() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        add(titleBar("퀴즈 결과"), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        mainButton.addActionListener(e -> onGoMain.run());
        historyButton.addActionListener(e -> onGoHistory.run());
    }

    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(24, 28, 12, 28));

        for (JComponent c : new JComponent[]{categoryLabel, scoreLabel, accuracyLabel}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(c);
            body.add(Box.createVerticalStrut(6));
        }
        body.add(Box.createVerticalStrut(16));

        JPanel rewardCard = card();
        rewardCard.setLayout(new GridBagLayout());
        addRow(rewardCard, 0, "획득 EXP", xpValue);
        addRow(rewardCard, 1, "능력치 변화", statValue);

        JPanel levelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        levelRow.setOpaque(false);
        levelRow.add(levelValue);
        levelRow.add(levelUpBadge);
        addRow(rewardCard, 2, "레벨", levelRow);

        xpBar.setStringPainted(true);
        xpBar.setFont(SMALL);
        xpBar.setForeground(PRIMARY);
        xpBar.setPreferredSize(new Dimension(200, 20));
        GridBagConstraints barGc = new GridBagConstraints();
        barGc.gridx = 0;
        barGc.gridy = 3;
        barGc.gridwidth = 2;
        barGc.fill = GridBagConstraints.HORIZONTAL;
        barGc.weightx = 1;
        barGc.insets = new Insets(12, 0, 0, 0);
        rewardCard.add(xpBar, barGc);

        rewardCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(rewardCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(body, BorderLayout.NORTH);
        return wrapper;
    }

    private static void addRow(JPanel card, int row, String name, JComponent value) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(6, 0, 6, 24);
        card.add(label(name, BODY, TEXT_MUTED), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.anchor = GridBagConstraints.WEST;
        right.weightx = 1;
        right.insets = new Insets(6, 0, 6, 0);
        card.add(value, right);
    }

    private JComponent buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        buttons.add(mainButton);
        buttons.add(historyButton);
        return buttons;
    }

    public void showResult(QuizResultView view) {
        Objects.requireNonNull(view, "QuizResultView가 null입니다. 세션 집계 결과를 먼저 받아오세요.");

        categoryLabel.setText(orDash(view.categoryName()) + " 퀴즈");
        boolean stoppedEarly = view.answeredCount() < view.totalCount();
        int base = view.answeredCount() > 0 ? view.answeredCount() : view.totalCount(); // 하나도 안 풀면 "0 / 5"
        scoreLabel.setText(view.correctCount() + " / " + base + " 정답");
        accuracyLabel.setText("정답률 " + percent(view.accuracyPercent(), view.answeredCount())
                + (stoppedEarly ? "  ·  " + view.totalCount() + "문제 중 " + view.answeredCount() + "문제 풀이" : ""));

        xpValue.setText(signed(view.gainedXp()));
        statValue.setText(orDash(view.statType()) + " " + signed(view.statGain()));
        boolean rewarded = view.gainedXp() > 0;
        xpValue.setForeground(rewarded ? HIGHLIGHT : TEXT);
        statValue.setForeground(rewarded ? HIGHLIGHT : TEXT);

        if (view.leveledUp()) {
            levelValue.setText("Lv." + view.levelBefore() + " → Lv." + view.levelAfter());
        } else {
            levelValue.setText("Lv." + view.levelAfter() + " (유지)");
        }
        levelUpBadge.setVisible(view.leveledUp());

        int perLevel = view.xpPerLevel() > 0 ? view.xpPerLevel() : DEFAULT_XP_PER_LEVEL;
        int inLevel = Math.max(0, Math.min(view.xpInLevel(), perLevel));
        xpBar.setMaximum(perLevel);
        xpBar.setValue(inLevel);
        xpBar.setString("Lv." + view.levelAfter() + "  EXP " + inLevel + " / " + perLevel);

        revalidate();
        repaint();
    }

    public void showResult(Category category, QuizResult result, int gainedXp, int statGain,
                           int levelBefore, int levelAfter, int xpInLevel) {
        Objects.requireNonNull(category, "Category가 null입니다.");
        Objects.requireNonNull(result, "QuizResult가 null입니다. 세션을 종료한 뒤 결과를 받아오세요.");
        showResult(new QuizResultView(category.getName(), category.getStatType(),
                result.getCorrectCount(), result.getTotalCount(), result.getAccuracy(),
                gainedXp, statGain, levelBefore, levelAfter, xpInLevel, DEFAULT_XP_PER_LEVEL,
                result.getAnsweredCount()));
    }

    public void setOnGoMain(Runnable action) {
        this.onGoMain = (action != null) ? action : () -> { };
    }

    public void setOnGoHistory(Runnable action) {
        this.onGoHistory = (action != null) ? action : () -> { };
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static JLabel centeredLabel(String text, Font font, Color color) {
        JLabel label = label(text, font, color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private static JLabel titleBar(String text) {
        JLabel title = centeredLabel(text, TITLE, Color.WHITE);
        title.setOpaque(true);
        title.setBackground(PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        return title;
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

    private static String percent(double percent, int submissions) {
        if (submissions <= 0) {
            return "0%";
        }
        return String.format("%.1f%%", percent);
    }

    private static String signed(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private static String orDash(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }
}
