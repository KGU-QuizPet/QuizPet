package main.java.com.quizpet.ui.screen;

import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.QuizResult;
import main.java.com.quizpet.ui.component.StatComparisonPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static main.java.com.quizpet.ui.screen.RecordUi.*;

public class QuizResultPanel extends JPanel {
    public static final String SCREEN_NAME = "QUIZ_RESULT";

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

    public record QuestionResult(String prompt, boolean correct, int earnedXp) {
        public static QuestionResult of(Question question, LearningRecord record) {
            Objects.requireNonNull(record, "LearningRecord가 null입니다.");
            String prompt = question == null ? "문제 정보 없음: " + record.getQuestionId() : question.getPrompt();
            return new QuestionResult(prompt, record.isCorrect(), record.getEarnedXp());
        }
    }

    private static final int DEFAULT_XP_PER_LEVEL = 50;
    private static final int RING_COLUMN_WIDTH = 280;
    private static final int RING_DIAMETER = 168;
    private static final int RING_THICKNESS = 19;
    private static final double RING_OK_PERCENT = 60.0;

    private final JLabel subtitleLabel = label(" ", BODY, INK_2);
    private final Pill categoryChip = new Pill(" ", SUNKEN, INK_2, CAPTION.deriveFont(13f));
    private final AccuracyRing ring = new AccuracyRing();

    private final CardLayout bannerPages = new CardLayout();
    private final JPanel bannerArea = clear(bannerPages);
    private final JLabel levelUpValue = label(" ", KPI.deriveFont(28f), Color.WHITE);
    private final JLabel nextLevelValue = label(" ", H2, INK);
    private final JLabel nextLevelMeta = label(" ", CAPTION, INK_2);
    private final Bar nextLevelBar = new Bar(EXP, 14);

    private final JLabel xpValue = label(" ", KPI, EXP_DARK);
    private final JLabel statValue = label(" ", KPI, INK);
    private final JLabel newCorrectValue = label(" ", KPI, INK);

    private final Card questionCard = new Card(new BorderLayout(0, 10), 18);
    private final JPanel rows = new RowsPanel();
    private final StatComparisonPanel statComparison = new StatComparisonPanel();
    private final Card statCard = new Card(new BorderLayout(), 18);

    private final GameButton historyButton = new GameButton("학습 기록 보기", Variant.GHOST, 52);
    private final GameButton retryButton = new GameButton("같은 분야 다시", Variant.GHOST, 52);
    private final GameButton mainButton = new GameButton("메인으로", Variant.PRIMARY, 60);

    private Runnable onGoMain = () -> { };
    private Runnable onGoHistory = () -> { };
    private Runnable onRetry = () -> { };

    public QuizResultPanel() {
        setLayout(new BorderLayout(0, GAP));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(PAD_Y, PAD_X, PAD_Y, PAD_X));

        add(buildHeader(), BorderLayout.NORTH);
        JPanel center = clear(new BorderLayout(0, GAP));
        center.add(buildSummaryRow(), BorderLayout.NORTH);
        center.add(buildLowerRow(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        mainButton.addActionListener(e -> onGoMain.run());
        historyButton.addActionListener(e -> onGoHistory.run());
        retryButton.addActionListener(e -> onRetry.run());
        retryButton.setVisible(false);
        showQuestionResults(List.of());
    }

    private JComponent buildHeader() {
        JPanel titles = clear(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = label("퀴즈 완료!", H1, INK);
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitleLabel);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel chipBox = clear(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        chipBox.add(categoryChip);

        JPanel header = clear(new BorderLayout());
        header.add(titles, BorderLayout.CENTER);
        header.add(chipBox, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSummaryRow() {
        Card ringCard = new Card(new BorderLayout(), 16);
        ringCard.add(ring, BorderLayout.CENTER);
        ringCard.setPreferredSize(new Dimension(RING_COLUMN_WIDTH, RING_DIAMETER + 2 * 16 + SHADOW_Y + 8));

        bannerArea.add(buildLevelUpBanner(), "UP");
        bannerArea.add(buildNextLevelCard(), "NEXT");

        JPanel kpis = clear(new GridLayout(1, 3, 14, 0));
        kpis.add(kpiCard("획득 EXP", xpValue));
        kpis.add(kpiCard("능력치 변화", statValue));
        kpis.add(kpiCard("신규 정답", newCorrectValue));

        JPanel right = clear(new BorderLayout(0, 16));
        right.add(bannerArea, BorderLayout.NORTH);
        right.add(kpis, BorderLayout.CENTER);

        JPanel row = clear(new BorderLayout(GAP, 0));
        row.add(ringCard, BorderLayout.WEST);
        row.add(right, BorderLayout.CENTER);
        return row;
    }

    private JComponent buildLevelUpBanner() {
        Card banner = new Card(new BorderLayout(16, 0), 16, INK, null, DARK_LIP, RADIUS_CARD);
        Card star = new Card(new BorderLayout(), 0, EXP, null, EXP_DARK, 16);
        JLabel starText = new JLabel("★", SwingConstants.CENTER);
        starText.setFont(KPI);
        starText.setForeground(Color.WHITE);
        star.add(starText);
        star.setPreferredSize(new Dimension(58, 58 + SHADOW_Y));

        JPanel texts = clear(null);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(label("LEVEL UP", CAPTION.deriveFont(13f), EXP));
        texts.add(levelUpValue);

        JLabel note = label("한 단계 성장했어요!", SMALL, new Color(0xCFC8E0));
        banner.add(star, BorderLayout.WEST);
        banner.add(texts, BorderLayout.CENTER);
        banner.add(note, BorderLayout.EAST);
        return banner;
    }

    private JComponent buildNextLevelCard() {
        Card card = new Card(new BorderLayout(0, 8), 16);
        JPanel top = clear(new BorderLayout());
        top.add(nextLevelValue, BorderLayout.WEST);
        top.add(nextLevelMeta, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        card.add(nextLevelBar, BorderLayout.CENTER);
        return card;
    }

    private static JComponent kpiCard(String name, JLabel value) {
        Card card = new Card(new BorderLayout(0, 2), 16);
        card.add(label(name, SMALL.deriveFont(Font.BOLD), INK_2), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildLowerRow() {
        statCard.add(statComparison, BorderLayout.CENTER);
        statComparison.setVisible(false);
        statCard.setVisible(false);
        JPanel lower = clear(new GridBagLayout());
        GridBagConstraints cell = new GridBagConstraints();
        cell.fill = GridBagConstraints.BOTH;
        cell.weighty = 1;
        cell.weightx = 1.4;
        cell.insets = new Insets(0, 0, 0, GAP);
        lower.add(buildQuestionList(), cell);
        cell.weightx = 1;
        cell.insets = new Insets(0, 0, 0, 0);
        lower.add(statCard, cell);
        return lower;
    }

    private JComponent buildQuestionList() {
        questionCard.add(label("문제별 결과", H2.deriveFont(18f), INK), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(rows,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        questionCard.add(scroll, BorderLayout.CENTER);
        return questionCard;
    }

    private JComponent buildButtons() {
        JPanel left = clear(new GridBagLayout());
        left.add(historyButton);
        JPanel right = clear(new GridBagLayout());
        GridBagConstraints gap = new GridBagConstraints();
        gap.insets = new Insets(0, 14, 0, 0);
        right.add(retryButton, gap);
        right.add(mainButton, gap);
        JPanel bar = clear(new BorderLayout());
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    public void showResult(QuizResultView view) {
        Objects.requireNonNull(view, "QuizResultView가 null입니다. 세션 집계 결과를 먼저 받아오세요.");

        boolean stoppedEarly = view.answeredCount() < view.totalCount();
        subtitleLabel.setText(orDash(view.categoryName()) + " · " + view.totalCount() + "문제"
                + (stoppedEarly ? " 중 " + view.answeredCount() + "문제 풀이" : ""));
        Color categoryColor = categoryColor(view.categoryName());
        categoryChip.set(orDash(view.categoryName()) + " · " + orDash(view.statType()),
                soft(categoryColor), categoryColor);

        int base = view.answeredCount() > 0 ? view.answeredCount() : view.totalCount();
        ring.set(view.answeredCount() > 0 ? view.accuracyPercent() : 0,
                percent(view.accuracyPercent(), view.answeredCount()),
                view.correctCount() + " / " + base + " 정답");

        int perLevel = view.xpPerLevel() > 0 ? view.xpPerLevel() : DEFAULT_XP_PER_LEVEL;
        int inLevel = Math.max(0, Math.min(view.xpInLevel(), perLevel));
        if (view.leveledUp()) {
            levelUpValue.setText("Lv." + view.levelBefore() + " → Lv." + view.levelAfter());
            bannerPages.show(bannerArea, "UP");
        } else {
            nextLevelValue.setText("다음 레벨까지 " + (perLevel - inLevel) + " EXP");
            nextLevelMeta.setText("Lv." + view.levelAfter() + " · EXP " + inLevel + " / " + perLevel);
            nextLevelBar.set((double) inLevel / perLevel, EXP);
            bannerPages.show(bannerArea, "NEXT");
        }

        xpValue.setText(signed(view.gainedXp()));
        xpValue.setForeground(view.gainedXp() > 0 ? EXP_DARK : INK_3);
        statValue.setText(orDash(view.statType()) + " " + signed(view.statGain()));
        statValue.setForeground(view.statGain() > 0 ? statColor(view.statType()) : INK_3);
        newCorrectValue.setText(view.statGain() + " / " + view.correctCount());

        showQuestionResults(List.of());
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

    public void showQuestionResults(List<QuestionResult> results) {
        Objects.requireNonNull(results, "문제별 결과 목록이 null입니다.");
        rows.removeAll();
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                rows.add(Box.createVerticalStrut(6));
            }
            rows.add(questionRow(i + 1, results.get(i)));
        }
        questionCard.setVisible(!results.isEmpty());
        revalidate();
        repaint();
    }

    private static JComponent questionRow(int number, QuestionResult result) {
        Card row = new Card(new BorderLayout(12, 0), 0, SUNKEN, null, SUNKEN, 14);
        row.setBorder(BorderFactory.createEmptyBorder(6, 12, 6 + SHADOW_Y, 14));

        Pill badge = new Pill(result.correct() ? "O" : "X", result.correct() ? OK : ERR, Color.WHITE, BODY_BOLD);
        badge.setPreferredSize(new Dimension(28, 28));
        badge.setBorder(BorderFactory.createEmptyBorder());
        JPanel left = clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.add(badge);
        left.add(label("Q" + number, BODY_BOLD, INK_3));

        JLabel prompt = label(orDash(result.prompt()), BODY, INK);
        prompt.setToolTipText(result.prompt());
        boolean rewarded = result.earnedXp() > 0;
        JLabel xp = label(rewarded ? "+" + result.earnedXp() + " EXP" : "보상 없음",
                CAPTION.deriveFont(13f), rewarded ? EXP_DARK : INK_3);

        row.add(left, BorderLayout.WEST);
        row.add(prompt, BorderLayout.CENTER);
        row.add(xp, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    public void showStatComparison(Map<String, Integer> before, Map<String, Integer> after) {
        statComparison.showComparison(before, after, true);
        statComparison.setVisible(true);
        statCard.setVisible(true);
        revalidate();
        repaint();
    }

    public void hideStatComparison() {
        statComparison.setVisible(false);
        statCard.setVisible(false);
        revalidate();
    }

    public StatComparisonPanel getStatComparison() {
        return statComparison;
    }

    public void setOnGoMain(Runnable action) {
        this.onGoMain = (action != null) ? action : () -> { };
    }

    public void setOnGoHistory(Runnable action) {
        this.onGoHistory = (action != null) ? action : () -> { };
    }

    public void setOnRetry(Runnable action) {
        this.onRetry = (action != null) ? action : () -> { };
        retryButton.setVisible(action != null);
        revalidate();
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

    private static final class AccuracyRing extends JComponent {
        private double percent;
        private String main = " ";
        private String sub = " ";

        void set(double percent, String main, String sub) {
            this.percent = Math.max(0, Math.min(100, percent));
            this.main = main;
            this.sub = sub;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(RING_DIAMETER, RING_DIAMETER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int size = Math.min(RING_DIAMETER, Math.min(getWidth(), getHeight()) - 4);
            if (size <= RING_THICKNESS * 2) {
                g2.dispose();
                return;
            }
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int inset = RING_THICKNESS / 2;
            g2.setStroke(new BasicStroke(RING_THICKNESS, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setColor(TRACK);
            g2.drawOval(x + inset, y + inset, size - 2 * inset, size - 2 * inset);
            g2.setColor(percent >= RING_OK_PERCENT ? OK : BRAND);
            g2.drawArc(x + inset, y + inset, size - 2 * inset, size - 2 * inset, 90,
                    -(int) Math.round(360 * percent / 100));

            Font mainFont = KPI.deriveFont(size >= RING_DIAMETER ? 34f : 26f);
            g2.setFont(mainFont);
            FontMetrics fm = g2.getFontMetrics();
            int cy = y + size / 2;
            g2.setColor(INK);
            g2.drawString(main, x + (size - fm.stringWidth(main)) / 2, cy + fm.getAscent() / 2 - 8);
            g2.setFont(CAPTION.deriveFont(13f));
            FontMetrics small = g2.getFontMetrics();
            g2.setColor(INK_2);
            g2.drawString(sub, x + (size - small.stringWidth(sub)) / 2, cy + fm.getAscent() / 2 + small.getHeight());
            g2.dispose();
        }
    }

    private static final class RowsPanel extends JPanel implements Scrollable {
        RowsPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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
