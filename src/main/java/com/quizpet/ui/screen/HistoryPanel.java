package main.java.com.quizpet.ui.screen;

import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.service.StatisticsService.Statistics;
import main.java.com.quizpet.ui.component.CategoryAccuracyChart;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static main.java.com.quizpet.ui.screen.RecordUi.*;

public class HistoryPanel extends JPanel {
    public static final String SCREEN_NAME = "HISTORY";

    public record HistoryView(
            String selectedCategoryId,
            int totalSubmissions,
            double accuracyPercent,
            long totalXp,
            List<CategoryRate> categoryRates,
            List<RecordRow> recentRecords,
            Set<Category> unavailableCategories
    ) {
        public HistoryView {
            categoryRates = List.copyOf(categoryRates);
            recentRecords = List.copyOf(recentRecords);
            unavailableCategories = Set.copyOf(unavailableCategories);
        }

        public HistoryView(String selectedCategoryId, int totalSubmissions, double accuracyPercent,
                           long totalXp, List<CategoryRate> categoryRates, List<RecordRow> recentRecords) {
            this(selectedCategoryId, totalSubmissions, accuracyPercent, totalXp, categoryRates, recentRecords,
                    totalSubmissions > 0 && categoryRates.isEmpty() ? EnumSet.allOf(Category.class) : Set.of());
        }

        public boolean isEmpty() {
            return totalSubmissions == 0;
        }

        public static HistoryView of(Statistics selected, List<CategoryRate> categoryRates,
                                     List<RecordRow> recentRecords) {
            Objects.requireNonNull(selected, "Statistics가 null입니다.");
            return new HistoryView(selected.getCategoryId(), selected.getTotalAttempts(), selected.getAccuracy(),
                    selected.getTotalEarnedXp(), categoryRates, recentRecords);
        }

        public record CategoryRate(String categoryId, String categoryName, int submissions, double accuracyPercent) {
            public static CategoryRate of(Category category, int submissions, double accuracyPercent) {
                return new CategoryRate(category.getCategoryId(), category.getName(), submissions, accuracyPercent);
            }

            public static CategoryRate of(Category category, Statistics statistics) {
                return of(category, statistics.getTotalAttempts(), statistics.getAccuracy());
            }
        }

        public record RecordRow(LocalDateTime answeredAt, String categoryName, String prompt,
                                String myAnswer, boolean correct, int earnedXp) {
            public static RecordRow of(Question question, LocalDateTime answeredAt, String selectedAnswer,
                                       boolean correct, int earnedXp) {
                return new RecordRow(answeredAt, question.getCategory().getName(), question.getPrompt(),
                        answerText(question, selectedAnswer), correct, earnedXp);
            }

            public static RecordRow of(Question question, LearningRecord record) {
                return of(question, record.getAnsweredAt(), record.getSelectedAnswer(),
                        record.isCorrect(), record.getEarnedXp());
            }
        }
    }

    public record WrongNoteView(
            String selectedCategoryId,
            int submissions,
            double accuracyPercent,
            List<WrongItem> items, boolean queryView, boolean filtered
    ) {
        public WrongNoteView {
            items = List.copyOf(items);
        }

        public WrongNoteView(String categoryId, int submissions, double accuracy, List<WrongItem> items) {
            this(categoryId, submissions, accuracy, items, false, false);
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public record WrongItem(String questionId, String categoryName, String prompt,
                                String lastAnswer, String correctAnswer, String explanation,
                                int wrongCount, String status, LocalDateTime lastAnsweredAt, String details) {
            public WrongItem(String id, String category, String prompt, String answer, String correct, String explanation) {
                this(id, category, prompt, answer, correct, explanation, 0, "-", null, "");
            }
            public static WrongItem of(Question question, String lastAnswer) {
                return new WrongItem(question.getQuestionId(), question.getCategory().getName(),
                        question.getPrompt(), answerText(question, lastAnswer),
                        answerText(question, question.getAnswer()), question.getExplanation());
            }
        }
    }

    public record CategoryOption(String id, String name) {
        public static final CategoryOption ALL = new CategoryOption(null, "전체");

        public static List<CategoryOption> defaults() {
            List<CategoryOption> options = new ArrayList<>();
            options.add(ALL);
            for (Category category : Category.values()) {
                options.add(new CategoryOption(category.getCategoryId(), category.getName()));
            }
            return List.copyOf(options);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final DateTimeFormatter ANSWERED_AT_FORMAT = DateTimeFormatter.ofPattern("MM.dd HH:mm");
    private static final int TABLE_ROW_HEIGHT = 44;
    private static final int CELL_PAD = 8;
    private static final int COL_MY_ANSWER = 3;
    private static final int COL_RESULT = 4;
    private static final int COL_EXP = 5;
    private static final int MIN_CONTENT_WIDTH = 760;
    private static final String WRONG_COUNT_UNKNOWN = "오답노트 수를 확인할 수 없습니다.";
    private static final int MIN_CONTENT_HEIGHT = 460;

    private static final String CARD_RECORD = "RECORD";
    private static final String CARD_WRONG_NOTE = "WRONG_NOTE";
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";
    private static final String CARD_ERROR = "ERROR";

    private final CardLayout pages = new CardLayout();

    private final Segment recordSegment = new Segment();
    private final JLabel accuracyTitle = label("전체 정답률", SMALL.deriveFont(Font.BOLD), INK_2);
    private final JLabel totalCountValue = label("0", KPI, INK);
    private final JLabel accuracyValue = label("0%", KPI, OK_DARK);
    private final JLabel totalXpValue = label("0", KPI, EXP_DARK);
    private final JLabel wrongCountValue = label("—", KPI, ERR);
    private final CategoryAccuracyChart categoryAccuracyChart = new CategoryAccuracyChart();
    private final DefaultTableModel recordModel = readOnlyModel("일시", "분야", "문제", "내 답", "결과", "EXP");
    private final JTable recordTable = new WideTable(recordModel);
    private final CardLayout recordListPages = new CardLayout();
    private final JPanel recordListArea = clear(recordListPages);
    private final JLabel recordErrorLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JTextArea recordDetailArea = textArea(SMALL, INK);
    private final GameButton wrongNoteButton = new GameButton("오답노트", Variant.GHOST, 52);
    private List<HistoryView.RecordRow> recordRows = List.of();

    private final Segment wrongCategorySegment = new Segment();
    private final JLabel wrongAccuracyLabel = label(" ", BODY, INK_2);
    private final WrongNoteTableModel wrongModel = new WrongNoteTableModel();
    private final JTable wrongTable = new WideTable(wrongModel);
    private final JTextArea wrongDetailArea = textArea(BODY, INK);
    private final CardLayout wrongListPages = new CardLayout();
    private final JPanel wrongListArea = clear(wrongListPages);
    private final GameButton reviewButton = new GameButton("복습하기", Variant.PRIMARY, 52);
    private final JCheckBox needsReviewOnly = new JCheckBox("복습 필요만 보기");
    private final JLabel wrongEmptyLabel = new JLabel("아직 틀린 문제가 없습니다.", SwingConstants.CENTER);
    private final JLabel wrongErrorLabel = new JLabel(" ", SwingConstants.CENTER);
    private boolean reviewEnabled = true;
    private Runnable onWrongRefresh = () -> { };
    private Runnable onWrongBack = this::showRecordPage;
    private List<WrongNoteView.WrongItem> wrongItems = List.of();

    private boolean updatingFilters = false;

    private Runnable onGoMain = () -> { };
    private Consumer<String> onCategoryChanged = id -> { };
    private Consumer<String> onOpenWrongNote = id -> { };
    private Consumer<String> onWrongNoteCategoryChanged = id -> { };
    private Consumer<List<String>> onReview = ids -> { };

    public HistoryPanel() {
        setLayout(pages);
        setBackground(BG);
        add(buildRecordPage(), CARD_RECORD);
        add(buildWrongNotePage(), CARD_WRONG_NOTE);
        setCategories(CategoryOption.defaults());
        recordSegment.setOnChange(id -> onCategoryChanged.accept(id));
        showRecordDetail(-1);
        showWrongDetail(-1);
        updateReviewButton();
        showRecordPage();
    }

    private JComponent buildRecordPage() {
        JPanel page = page();

        JPanel header = clear(new BorderLayout());
        header.add(label("학습 기록", H1, INK), BorderLayout.WEST);
        header.add(recordSegment, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel kpis = new KpiGrid();
        kpis.add(kpiCard(kpiTitle("총 학습 횟수"), totalCountValue, "회", "총 학습 횟수"));
        kpis.add(kpiCard(accuracyTitle, accuracyValue, null, "정답률"));
        kpis.add(kpiCard(kpiTitle("누적 EXP"), totalXpValue, null, "누적 EXP"));
        kpis.add(kpiCard(kpiTitle("전체 오답노트"), wrongCountValue, "문제", "전체 오답노트"));
        wrongCountValue.setToolTipText(WRONG_COUNT_UNKNOWN);

        Card ratesCard = new Card(new BorderLayout(), 4);
        ratesCard.add(categoryAccuracyChart, BorderLayout.CENTER);

        Card historyCard = new Card(new BorderLayout(0, 8), 16);
        JPanel historyHeader = clear(new BorderLayout());
        historyHeader.add(label("풀이 이력", H2.deriveFont(18f), INK), BorderLayout.WEST);
        historyHeader.add(label("최신순", SMALL, INK_2), BorderLayout.EAST);
        historyCard.add(historyHeader, BorderLayout.NORTH);
        setUpRecordTable();
        recordListArea.add(tableScroll(recordTable), CARD_TABLE);
        recordListArea.add(emptyMessage("아직 학습 기록이 없습니다."), CARD_EMPTY);
        recordErrorLabel.setFont(BODY);
        recordErrorLabel.setForeground(ERR);
        recordListArea.add(recordErrorLabel, CARD_ERROR);
        historyCard.add(recordListArea, BorderLayout.CENTER);
        historyCard.add(detailScroll(recordDetailArea, 3), BorderLayout.SOUTH);

        ratesCard.setMinimumSize(new Dimension(180, 200));
        historyCard.setMinimumSize(new Dimension(220, 200));
        ratesCard.setPreferredSize(new Dimension(0, 0));
        historyCard.setPreferredSize(new Dimension(0, 0));
        JSplitPane columns = split(JSplitPane.HORIZONTAL_SPLIT, ratesCard, historyCard, 0.4);

        JPanel center = new FitPanel(new BorderLayout(0, GAP));
        center.add(kpis, BorderLayout.NORTH);
        center.add(columns, BorderLayout.CENTER);
        page.add(fitScroll(center), BorderLayout.CENTER);

        GameButton mainButton = new GameButton("메인으로", Variant.GHOST, 52);
        mainButton.addActionListener(e -> onGoMain.run());
        wrongNoteButton.addActionListener(e -> onOpenWrongNote.accept(recordSegment.selectedId()));
        page.add(buttonBar(null, mainButton, wrongNoteButton), BorderLayout.SOUTH);
        return page;
    }

    private void setUpRecordTable() {
        styleTable(recordTable);
        int[][] widths = {{98, 98}, {74, 74}, {120, Integer.MAX_VALUE}, {60, 180}, {52, 52}, {66, 66}};
        applyWidths(recordTable, widths, new int[]{98, 74, 300, 90, 52, 66});
        TableCellRenderer text = new TextCellRenderer() {
            @Override
            Color colorFor(int row, int column) {
                HistoryView.RecordRow r = row < recordRows.size() ? recordRows.get(row) : null;
                if (column == 0 || column == COL_MY_ANSWER) {
                    return INK_2;
                }
                if (column == COL_RESULT && r != null) {
                    return r.correct() ? OK_DARK : ERR;
                }
                if (column == COL_EXP && r != null) {
                    return r.earnedXp() > 0 ? EXP_DARK : INK_3;
                }
                return INK;
            }

            @Override
            boolean bold(int column) {
                return column == COL_RESULT || column == COL_EXP;
            }

            @Override
            boolean rightAligned(int column) {
                return column == COL_EXP;
            }
        };
        for (int column : new int[]{0, 2, COL_MY_ANSWER, COL_RESULT, COL_EXP}) {
            recordTable.getColumnModel().getColumn(column).setCellRenderer(text);
        }
        recordTable.getColumnModel().getColumn(1).setCellRenderer(new PillCellRenderer());
        recordTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showRecordDetail(recordTable.getSelectedRow());
            }
        });
    }

    public void showHistory(HistoryView view) {
        showHistory(view, null);
    }

    public void showHistory(HistoryView view, Integer wrongCount) {
        if (wrongCount != null && wrongCount < 0) {
            throw new IllegalArgumentException("오답 수는 음수일 수 없습니다.");
        }
        Objects.requireNonNull(view, "HistoryView가 null입니다. 학습 통계를 먼저 받아오세요.");

        wrongCountValue.setText(wrongCount == null ? "—" : wrongCount.toString());
        wrongCountValue.setToolTipText(wrongCount == null ? WRONG_COUNT_UNKNOWN
                : "전체 분야에서 한 번이라도 틀린 고유 문제 수입니다. 복습 완료도 포함합니다.");
        rebuildCategoryRates(view);
        recordSegment.select(view.selectedCategoryId());
        Category selected = view.selectedCategoryId() == null ? null : category(view.selectedCategoryId());
        accuracyTitle.setText(selected == null ? "전체 정답률" : selected.getName() + " 정답률");

        totalCountValue.setText(String.valueOf(view.totalSubmissions()));
        accuracyValue.setText(percent(view.accuracyPercent(), view.totalSubmissions()));
        totalXpValue.setText(String.valueOf(view.totalXp()));

        recordRows = view.recentRecords();
        recordModel.setRowCount(0);
        for (HistoryView.RecordRow row : recordRows) {
            recordModel.addRow(new Object[]{
                    row.answeredAt() == null ? "-" : row.answeredAt().format(ANSWERED_AT_FORMAT),
                    orDash(row.categoryName()),
                    orDash(row.prompt()),
                    orDash(row.myAnswer()),
                    row.correct() ? "정답" : "오답",
                    row.earnedXp() > 0 ? "+" + row.earnedXp() : (row.correct() ? "재정답" : "—")
            });
        }
        showRecordDetail(-1);
        recordListPages.show(recordListArea, recordRows.isEmpty() ? CARD_EMPTY : CARD_TABLE);

        showRecordPage();
        revalidate();
        repaint();
    }

    public void showHistoryError(String message) {
        totalCountValue.setText("—");
        accuracyValue.setText("—");
        totalXpValue.setText("—");
        wrongCountValue.setText("—");
        wrongCountValue.setToolTipText(WRONG_COUNT_UNKNOWN);
        categoryAccuracyChart.showError(message == null || message.isBlank()
                ? "분야별 정답률을 불러오지 못했습니다." : message);
        recordRows = List.of();
        recordModel.setRowCount(0);
        recordDetailArea.setText(" ");
        String text = message == null || message.isBlank()
                ? "학습 기록을 불러오지 못했습니다. 다시 시도해 주세요."
                : message;
        recordErrorLabel.setText(html(text));
        recordListPages.show(recordListArea, CARD_ERROR);

        showRecordPage();
        revalidate();
        repaint();
    }

    private void rebuildCategoryRates(HistoryView view) {
        List<CategoryAccuracyChart.CategoryData> data = view.categoryRates().stream()
                .map(rate -> new CategoryAccuracyChart.CategoryData(Category.valueOf(rate.categoryId()),
                        rate.submissions(), rate.accuracyPercent())).toList();
        categoryAccuracyChart.setData(data, view.unavailableCategories());
    }

    private void showRecordDetail(int row) {
        if (row < 0 || row >= recordRows.size()) {
            recordDetailArea.setText("기록을 클릭하면 문제 전체를 볼 수 있어요.");
            recordDetailArea.setCaretPosition(0);
            return;
        }
        HistoryView.RecordRow r = recordRows.get(row);
        recordDetailArea.setText("분야: " + orDash(r.categoryName()) + " / 풀이 시각: " + r.answeredAt()
                + "\n결과: " + (r.correct() ? "정답" : "오답") + " / 획득 EXP: " + r.earnedXp()
                + "\n\n문제: " + orDash(r.prompt()) + "\n내 답: " + orDash(r.myAnswer()));
        recordDetailArea.setCaretPosition(0);
    }

    private JComponent buildWrongNotePage() {
        JPanel page = page();

        JPanel titles = clear(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = label("오답노트", H1, INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrongAccuracyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(wrongAccuracyLabel);

        wrongCategorySegment.setOnChange(id -> onWrongNoteCategoryChanged.accept(id));
        needsReviewOnly.setOpaque(false);
        needsReviewOnly.setFont(SMALL.deriveFont(Font.BOLD));
        needsReviewOnly.setForeground(INK_2);
        needsReviewOnly.setFocusPainted(false);
        needsReviewOnly.addActionListener(e -> {
            if (!updatingFilters) {
                onWrongRefresh.run();
            }
        });
        GameButton refresh = new GameButton("다시 조회", Variant.GHOST, 40);
        refresh.addActionListener(e -> onWrongRefresh.run());
        JPanel filters = clear(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        filters.add(needsReviewOnly);
        filters.add(refresh);

        JPanel header = clear(new BorderLayout(GAP, 0));
        header.add(titles, BorderLayout.CENTER);
        JPanel filterBox = clear(new GridBagLayout());
        filterBox.add(filters);
        header.add(filterBox, BorderLayout.EAST);
        JPanel categoryRow = clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        categoryRow.add(wrongCategorySegment);
        JScrollPane categoryTabs = new JScrollPane(categoryRow,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, wrongCategorySegment.getPreferredSize().height
                        + getHorizontalScrollBar().getPreferredSize().height);
            }
        };
        categoryTabs.setBorder(BorderFactory.createEmptyBorder());
        categoryTabs.setOpaque(false);
        categoryTabs.getViewport().setOpaque(false);
        header.add(categoryTabs, BorderLayout.SOUTH);
        page.add(header, BorderLayout.NORTH);

        styleTable(wrongTable);
        applyWidths(wrongTable, new int[][]{{36, 36}, {74, 74}, {140, Integer.MAX_VALUE}, {80, 80},
                {96, 110}, {104, 104}, {90, 140}}, new int[]{36, 74, 320, 80, 100, 104, 100});
        TableCellRenderer text = new TextCellRenderer() {
            @Override
            Object display(Object value, int column) {
                if (column == 3 && value instanceof Integer count) {
                    return count + "회";
                }
                if (column == 5) {
                    return value instanceof LocalDateTime time ? time.format(ANSWERED_AT_FORMAT) : "-";
                }
                return value;
            }

            @Override
            Color colorFor(int row, int column) {
                if (column == 4) {
                    String status = row < wrongItems.size() ? wrongItems.get(row).status() : "";
                    return "복습 필요".equals(status) ? ERR : "복습 완료".equals(status) ? OK_DARK : INK_2;
                }
                return column == 2 ? INK : INK_2;
            }

            @Override
            boolean bold(int column) {
                return column == 3 || column == 4;
            }

            @Override
            Font fontFor(int column) {
                return column == 6 ? CAPTION : null;
            }
        };
        for (int column = 2; column < 7; column++) {
            wrongTable.getColumnModel().getColumn(column).setCellRenderer(text);
        }
        wrongTable.getColumnModel().getColumn(1).setCellRenderer(new PillCellRenderer());
        wrongTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showWrongDetail(wrongTable.getSelectedRow());
            }
        });
        wrongModel.addTableModelListener(e -> updateReviewButton());

        wrongListArea.add(tableScroll(wrongTable), CARD_TABLE);
        wrongEmptyLabel.setFont(BODY);
        wrongEmptyLabel.setForeground(INK_3);
        wrongListArea.add(wrongEmptyLabel, CARD_EMPTY);
        wrongErrorLabel.setFont(BODY);
        wrongErrorLabel.setForeground(ERR);
        wrongListArea.add(wrongErrorLabel, CARD_ERROR);

        Card listCard = new Card(new BorderLayout(0, 10), 16);
        listCard.add(label("문제를 선택하면 상세 이력을 볼 수 있어요", H2.deriveFont(17f), INK), BorderLayout.NORTH);
        listCard.add(wrongListArea, BorderLayout.CENTER);

        Card detailCard = new Card(new BorderLayout(0, 8), 16);
        detailCard.add(label("문제 상세 · 제출 이력", H2.deriveFont(17f), INK), BorderLayout.NORTH);
        wrongDetailArea.setFocusable(true);
        wrongDetailArea.setOpaque(true);
        wrongDetailArea.setBackground(SUNKEN);
        wrongDetailArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        detailCard.add(scroll(wrongDetailArea), BorderLayout.CENTER);

        listCard.setMinimumSize(new Dimension(0, 100));
        detailCard.setMinimumSize(new Dimension(0, 140));
        JSplitPane sections = split(JSplitPane.VERTICAL_SPLIT, listCard, detailCard, 0.45);
        sections.setDividerLocation(180);
        sections.addComponentListener(new java.awt.event.ComponentAdapter() {
            private boolean placed;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (!placed && sections.getHeight() > 0) {
                    placed = true;
                    sections.setDividerLocation(0.45);
                }
            }
        });
        sections.getAccessibleContext().setAccessibleName("오답 목록과 문제 상세 크기 조절");
        page.add(sections, BorderLayout.CENTER);

        GameButton backButton = new GameButton("이전", Variant.GHOST, 52);
        GameButton mainButton = new GameButton("메인으로", Variant.GHOST, 52);
        backButton.addActionListener(e -> onWrongBack.run());
        mainButton.addActionListener(e -> onGoMain.run());
        reviewButton.addActionListener(e -> {
            List<String> ids = checkedQuestionIds();
            if (!ids.isEmpty()) {
                onReview.accept(ids);
            }
        });
        page.add(buttonBar(backButton, mainButton, reviewButton), BorderLayout.SOUTH);
        return page;
    }

    public void showWrongNote(WrongNoteView view) {
        Objects.requireNonNull(view, "WrongNoteView가 null입니다. 오답 목록을 먼저 받아오세요.");

        wrongCategorySegment.select(view.selectedCategoryId());
        wrongAccuracyLabel.setText(view.queryView()
                ? "오답 문제 " + view.items().size() + "개 / 전체 " + view.submissions() + "개"
                : "정답률 " + percent(view.accuracyPercent(), view.submissions()));
        wrongEmptyLabel.setText(view.filtered() ? "해당 조건의 오답 기록이 없습니다." : "아직 틀린 문제가 없습니다.");

        wrongItems = view.items();
        wrongModel.setItems(wrongItems);
        wrongListPages.show(wrongListArea, view.isEmpty() ? CARD_EMPTY : CARD_TABLE);
        showWrongDetail(-1);
        updateReviewButton();

        pages.show(this, CARD_WRONG_NOTE);
        revalidate();
        repaint();
    }

    private void showWrongDetail(int row) {
        if (row < 0 || row >= wrongItems.size()) {
            wrongDetailArea.setText(wrongItems.isEmpty()
                    ? "오답이 생기면 여기에서 정답과 해설을 다시 볼 수 있어요."
                    : "문제를 클릭하면 문제 전체와 정답·해설이 보여요.");
            wrongDetailArea.setCaretPosition(0);
            return;
        }
        WrongNoteView.WrongItem item = wrongItems.get(row);
        if (!item.details().isEmpty()) {
            wrongDetailArea.setText(item.details());
            wrongDetailArea.setCaretPosition(0);
            return;
        }
        wrongDetailArea.setText("문제: " + orDash(item.prompt())
                + "\n내 답: " + orDash(item.lastAnswer())
                + "    정답: " + orDash(item.correctAnswer())
                + "\n해설: " + orDash(item.explanation()));
        wrongDetailArea.setCaretPosition(0);
    }

    private List<String> checkedQuestionIds() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < wrongItems.size(); i++) {
            if (wrongModel.isChecked(i)) {
                ids.add(wrongItems.get(i).questionId());
            }
        }
        return ids;
    }

    private void updateReviewButton() {
        int count = checkedQuestionIds().size();
        if (!reviewEnabled) {
            reviewButton.setEnabled(false);
            reviewButton.setText("복습 기능 준비 중");
            reviewButton.setToolTipText("선택 문제 복습 세션은 추후 지원합니다.");
            return;
        }
        reviewButton.setEnabled(count > 0);
        reviewButton.setText(count > 0 ? "복습하기 (" + count + ")" : "복습하기");
        reviewButton.setToolTipText(null);
    }

    public void setCategories(List<CategoryOption> categories) {
        Objects.requireNonNull(categories, "분야 목록이 null입니다.");
        recordSegment.setOptions(categories);
        List<CategoryOption> wrongCategories = new ArrayList<>(categories);
        wrongCategories.add(new CategoryOption("UNCLASSIFIED", "미분류"));
        wrongCategorySegment.setOptions(wrongCategories);
    }

    public void setWrongNeedsReviewOnly(boolean selected) {
        updatingFilters = true;
        try {
            needsReviewOnly.setSelected(selected);
        } finally {
            updatingFilters = false;
        }
    }

    public boolean isNeedsReviewOnly() {
        return needsReviewOnly.isSelected();
    }

    public String getWrongCategoryId() {
        return wrongCategorySegment.selectedId();
    }

    public void setWrongCategoryId(String categoryId) {
        wrongCategorySegment.select(categoryId);
    }

    public void setOnWrongNoteRefresh(Runnable action) {
        onWrongRefresh = Objects.requireNonNull(action);
    }

    public void setOnWrongNoteBack(Runnable action) {
        onWrongBack = Objects.requireNonNull(action);
    }

    public void setReviewEnabled(boolean enabled) {
        reviewEnabled = enabled;
        updateReviewButton();
    }

    public void clearWrongNote() {
        updatingFilters = true;
        try {
            needsReviewOnly.setSelected(false);
            wrongCategorySegment.select(null);
        } finally {
            updatingFilters = false;
        }
        wrongItems = List.of();
        wrongModel.setItems(wrongItems);
        showWrongDetail(-1);
        wrongAccuracyLabel.setText(" ");
        wrongErrorLabel.setText(" ");
        wrongEmptyLabel.setText("아직 틀린 문제가 없습니다.");
        wrongListPages.show(wrongListArea, CARD_EMPTY);
        updateReviewButton();
    }

    public void showWrongNoteError(String message) {
        wrongItems = List.of();
        wrongModel.setItems(wrongItems);
        showWrongDetail(-1);
        wrongAccuracyLabel.setText("조회 실패");
        wrongErrorLabel.setText(html(message == null ? "오답 기록을 불러오지 못했습니다." : message));
        wrongListPages.show(wrongListArea, CARD_ERROR);
        pages.show(this, CARD_WRONG_NOTE);
        updateReviewButton();
        revalidate();
        repaint();
    }

    public void setWrongNoteEnabled(boolean enabled) {
        wrongNoteButton.setEnabled(enabled);
        wrongNoteButton.setToolTipText(enabled ? null : "오답노트는 추후 지원 예정입니다.");
    }

    public void showRecordPage() {
        pages.show(this, CARD_RECORD);
    }

    public void setOnGoMain(Runnable action) {
        this.onGoMain = (action != null) ? action : () -> { };
    }

    public void setOnCategoryChanged(Consumer<String> action) {
        this.onCategoryChanged = (action != null) ? action : id -> { };
    }

    public void setOnOpenWrongNote(Consumer<String> action) {
        this.onOpenWrongNote = (action != null) ? action : id -> { };
    }

    public void setOnWrongNoteCategoryChanged(Consumer<String> action) {
        this.onWrongNoteCategoryChanged = (action != null) ? action : id -> { };
    }

    public void setOnReview(Consumer<List<String>> action) {
        this.onReview = (action != null) ? action : ids -> { };
    }

    private static JPanel page() {
        JPanel page = clear(new BorderLayout(0, GAP));
        page.setOpaque(true);
        page.setBackground(BG);
        page.setBorder(BorderFactory.createEmptyBorder(PAD_Y, PAD_X, PAD_Y, PAD_X));
        return page;
    }

    private static JSplitPane split(int orientation, JComponent first, JComponent second, double weight) {
        JSplitPane columns = new JSplitPane(orientation, first, second);
        columns.setResizeWeight(weight);
        columns.setContinuousLayout(true);
        columns.setOpaque(false);
        columns.setBorder(BorderFactory.createEmptyBorder());
        columns.setDividerSize(GAP);
        columns.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                    }
                };
            }
        });
        columns.setBorder(BorderFactory.createEmptyBorder());
        return columns;
    }

    private static JLabel kpiTitle(String text) {
        return label(text, SMALL.deriveFont(Font.BOLD), INK_2);
    }

    private static JComponent kpiCard(JLabel title, JLabel value, String unit, String accessibleName) {
        value.getAccessibleContext().setAccessibleName(accessibleName);
        Card card = new Card(new BorderLayout(0, 2), 16);
        card.add(title, BorderLayout.NORTH);
        JPanel valueRow = clear(new FlowLayout(FlowLayout.LEFT, 4, 0));
        valueRow.add(value);
        if (unit != null) {
            valueRow.add(label(unit, BODY_BOLD, INK_3));
        }
        card.add(valueRow, BorderLayout.CENTER);
        return card;
    }

    private static JComponent buttonBar(JComponent left, JComponent right1, JComponent right2) {
        JPanel bar = clear(new BorderLayout());
        if (left != null) {
            JPanel leftBox = clear(new GridBagLayout());
            leftBox.add(left);
            bar.add(leftBox, BorderLayout.WEST);
        }
        JPanel rightBox = clear(new GridBagLayout());
        GridBagConstraints gap = new GridBagConstraints();
        gap.insets = new Insets(0, 14, 0, 0);
        for (JComponent c : new JComponent[]{right1, right2}) {
            if (c != null) {
                rightBox.add(c, gap);
            }
        }
        bar.add(rightBox, BorderLayout.EAST);
        return bar;
    }

    private static JLabel emptyMessage(String text) {
        JLabel label = label(text, BODY, INK_3);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private static JTextArea textArea(Font font, Color color) {
        JTextArea area = new JTextArea();
        area.setFont(font);
        area.setForeground(color);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        return area;
    }

    private static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static JScrollPane detailScroll(JTextArea area, int visibleLines) {
        JScrollPane scroll = scroll(area);
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        Insets insets = area.getInsets();
        scroll.setPreferredSize(new Dimension(0, lineHeight * visibleLines + insets.top + insets.bottom));
        return scroll;
    }

    private static JScrollPane fitScroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(SURFACE);
        JPanel corner = new JPanel();
        corner.setBackground(SURFACE);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static void styleTable(JTable table) {
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setFont(BODY);
        table.setForeground(INK);
        table.setBackground(SURFACE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(TRACK);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(BRAND_SOFT);
        table.setSelectionForeground(INK);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, column);
                cell.setFont(CAPTION);
                cell.setForeground(INK_3);
                cell.setBackground(SURFACE);
                cell.setHorizontalAlignment("EXP".equals(value) ? SwingConstants.RIGHT : SwingConstants.LEFT);
                cell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, LINE),
                        BorderFactory.createEmptyBorder(10, CELL_PAD, 10, CELL_PAD)));
                return cell;
            }
        });
    }

    private static void applyWidths(JTable table, int[][] minMax, int[] preferred) {
        for (int i = 0; i < minMax.length; i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setMinWidth(minMax[i][0]);
            column.setMaxWidth(minMax[i][1]);
            column.setPreferredWidth(preferred[i]);
        }
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String html(String text) {
        return "<html><div style='text-align:center'>"
                + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div></html>";
    }

    private static String percent(double percent, int submissions) {
        if (submissions <= 0) {
            return "0%";
        }
        return String.format("%.1f%%", percent);
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

    private static String orDash(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }

    private static final class KpiGrid extends JPanel {
        KpiGrid() {
            super(new java.awt.GridLayout(1, 4, 14, 14));
            setOpaque(false);
        }

        private int rows() {
            if (getWidth() <= 0) {
                return 1;
            }
            int cellWidth = (getWidth() - 3 * 14) / 4;
            for (Component card : getComponents()) {
                if (card.getPreferredSize().width > cellWidth) {
                    return 2;
                }
            }
            return 1;
        }

        @Override
        public void doLayout() {
            int rows = rows();
            java.awt.GridLayout grid = (java.awt.GridLayout) getLayout();
            if (grid.getRows() != rows) {
                grid.setRows(rows);
                grid.setColumns(rows == 1 ? 4 : 2);
                revalidate();
            }
            super.doLayout();
        }

        @Override
        public Dimension getPreferredSize() {
            int width = 0;
            int height = 0;
            for (Component card : getComponents()) {
                width = Math.max(width, card.getPreferredSize().width);
                height = Math.max(height, card.getPreferredSize().height);
            }
            int rows = rows();
            return new Dimension(width * 2 + 14, rows * height + (rows - 1) * 14);
        }
    }

    private static final class FitPanel extends JPanel implements Scrollable {
        FitPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(MIN_CONTENT_WIDTH, MIN_CONTENT_HEIGHT);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(MIN_CONTENT_WIDTH, MIN_CONTENT_HEIGHT);
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visible.height : visible.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport viewport && viewport.getWidth() >= MIN_CONTENT_WIDTH;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() >= MIN_CONTENT_HEIGHT;
        }
    }

    private static final class WideTable extends JTable {
        WideTable(DefaultTableModel model) {
            super(model);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport viewport && viewport.getWidth() >= getMinimumSize().width;
        }
    }

    private abstract static class TextCellRenderer extends DefaultTableCellRenderer {
        TextCellRenderer() {
            putClientProperty("html.disable", Boolean.TRUE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, display(value, column),
                    selected, false, row, column);
            cell.setBorder(BorderFactory.createEmptyBorder(0, CELL_PAD, 0, CELL_PAD));
            Font font = fontFor(column);
            cell.setFont(font != null ? font : bold(column) ? BODY_BOLD : BODY);
            cell.setHorizontalAlignment(rightAligned(column) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            cell.setForeground(colorFor(row, column));
            String raw = value == null ? "" : value.toString();
            cell.setToolTipText("<html>" + raw.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\n", "<br>") + "</html>");
            return cell;
        }

        Object display(Object value, int column) {
            return value;
        }

        abstract Color colorFor(int row, int column);

        boolean bold(int column) {
            return false;
        }

        boolean rightAligned(int column) {
            return false;
        }

        Font fontFor(int column) {
            return null;
        }
    }

    private static final class PillCellRenderer extends JPanel implements TableCellRenderer {
        private final Pill pill = new Pill(" ", INK_3, Color.WHITE, CAPTION);

        PillCellRenderer() {
            super(new GridBagLayout());
            GridBagConstraints cell = new GridBagConstraints();
            cell.anchor = GridBagConstraints.WEST;
            cell.weightx = 1;
            cell.insets = new Insets(0, CELL_PAD, 0, 0);
            add(pill, cell);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            doLayout();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            String name = String.valueOf(value);
            pill.set(name, categoryColor(name), Color.WHITE);
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private static final class WrongNoteTableModel extends DefaultTableModel {
        WrongNoteTableModel() {
            super(new Object[]{"", "분야", "문제", "오답 횟수", "복습 상태", "최근 풀이", "문제 ID"}, 0);
        }

        void setItems(List<WrongNoteView.WrongItem> items) {
            setRowCount(0);
            for (WrongNoteView.WrongItem item : items) {
                addRow(new Object[]{Boolean.FALSE, orDash(item.categoryName()), orDash(item.prompt()),
                        item.wrongCount(), item.status(), item.lastAnsweredAt(), item.questionId()});
            }
        }

        boolean isChecked(int row) {
            return row < getRowCount() && Boolean.TRUE.equals(getValueAt(row, 0));
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Boolean.class : column == 3 ? Integer.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }
    }
}
