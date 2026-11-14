package main.java.com.quizpet.ui.screen;

import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.service.StatisticsService.Statistics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** UI-08 학습 기록 + 오답노트(P1). */
public class HistoryPanel extends JPanel {
    public static final String SCREEN_NAME = "HISTORY";

    /** selectedCategoryId가 null이면 「전체」입니다 (BE3 전체 통계의 categoryId와 같음). */
    public record HistoryView(
            String selectedCategoryId,
            int totalSubmissions,
            double accuracyPercent,
            long totalXp,
            List<CategoryRate> categoryRates,
            List<RecordRow> recentRecords
    ) {
        public HistoryView {
            categoryRates = List.copyOf(categoryRates);
            recentRecords = List.copyOf(recentRecords);
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
            /** selectedAnswer는 기록에 저장된 값 그대로 넣습니다 (4지선다 "1"~"4"는 보기 문장으로 바꿔 표시). */
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
            List<WrongItem> items
    ) {
        public WrongNoteView {
            items = List.copyOf(items);
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public record WrongItem(String questionId, String categoryName, String prompt,
                                String lastAnswer, String correctAnswer, String explanation) {
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

    // FE1 UiStyle과 같은 값 (UiStyle이 main에 머지되면 UiStyle로 교체)
    private static final Color BACKGROUND = new Color(0xFFF8EE);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(0xDADCE3);
    private static final Color TEXT = new Color(0x1F2330);
    private static final Color TEXT_MUTED = new Color(0x6B7080);
    private static final Color PRIMARY = new Color(0x6C8EBF);
    private static final Color CORRECT = new Color(0x2F9E44);
    private static final Color WRONG = new Color(0xE03131);
    private static final Font TITLE = new Font(Font.DIALOG, Font.BOLD, 18);
    private static final Font SUBTITLE = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font BODY = new Font(Font.DIALOG, Font.PLAIN, 15);
    private static final Font BODY_BOLD = new Font(Font.DIALOG, Font.BOLD, 15);
    private static final Font SMALL = new Font(Font.DIALOG, Font.PLAIN, 13);

    private static final DateTimeFormatter ANSWERED_AT_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private static final String CARD_RECORD = "RECORD";
    private static final String CARD_WRONG_NOTE = "WRONG_NOTE";
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_EMPTY = "EMPTY";
    private static final String CARD_ERROR = "ERROR";

    private final CardLayout pages = new CardLayout();

    private final JComboBox<CategoryOption> recordCategoryBox = new JComboBox<>();
    private final JLabel totalCountValue = centeredLabel("0", TITLE, TEXT);
    private final JLabel accuracyValue = centeredLabel("0%", TITLE, TEXT);
    private final JLabel totalXpValue = centeredLabel("0", TITLE, TEXT);
    private final JPanel categoryRatesPanel = new JPanel(new GridBagLayout());
    private final DefaultTableModel recordModel = readOnlyModel("일시", "분야", "문제", "내 답", "결과", "EXP");
    private final JTable recordTable = new JTable(recordModel);
    private final CardLayout recordListPages = new CardLayout();
    private final JPanel recordListArea = new JPanel(recordListPages);
    private final JLabel recordErrorLabel = centeredLabel(" ", BODY, WRONG);

    private final JComboBox<CategoryOption> wrongCategoryBox = new JComboBox<>();
    private final JLabel wrongAccuracyLabel = label(" ", SUBTITLE, TEXT);
    private final WrongNoteTableModel wrongModel = new WrongNoteTableModel();
    private final JTable wrongTable = new JTable(wrongModel);
    private final JTextArea wrongDetailArea = new JTextArea(4, 20);
    private final CardLayout wrongListPages = new CardLayout();
    private final JPanel wrongListArea = new JPanel(wrongListPages);
    private final JButton reviewButton = button("복습하기");

    private List<WrongNoteView.WrongItem> wrongItems = List.of();
    private List<HistoryView.RecordRow> recordRows = List.of();
    private final JTextArea recordDetailArea = new JTextArea();

    // 코드로 콤보박스 값을 바꿀 때 콜백이 다시 불리지 않게 막습니다.
    private boolean updatingCombo = false;

    private Runnable onGoMain = () -> { };
    private Consumer<String> onCategoryChanged = id -> { };
    private Consumer<String> onOpenWrongNote = id -> { };
    private Consumer<String> onWrongNoteCategoryChanged = id -> { };
    private Consumer<List<String>> onReview = ids -> { };

    public HistoryPanel() {
        setLayout(pages);
        setBackground(BACKGROUND);
        add(buildRecordPage(), CARD_RECORD);
        add(buildWrongNotePage(), CARD_WRONG_NOTE);
        setCategories(CategoryOption.defaults());
        showRecordPage();
    }

    private JComponent buildRecordPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.add(titleBar("학습 기록"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(leftAligned(categoryRow(recordCategoryBox, null)));
        top.add(Box.createVerticalStrut(12));
        top.add(leftAligned(buildSummaryCard()));
        top.add(Box.createVerticalStrut(12));

        JPanel ratesCard = card();
        ratesCard.setLayout(new BorderLayout(0, 8));
        ratesCard.add(label("분야별 정답률", BODY_BOLD, TEXT), BorderLayout.NORTH);
        categoryRatesPanel.setOpaque(false);
        ratesCard.add(categoryRatesPanel, BorderLayout.CENTER);
        top.add(leftAligned(ratesCard));
        body.add(top, BorderLayout.NORTH);

        JPanel recentCard = card();
        recentCard.setLayout(new BorderLayout(0, 8));
        recentCard.add(label("최근 풀이 기록", BODY_BOLD, TEXT), BorderLayout.NORTH);
        setUpRecordTable();
        JScrollPane scroll = new JScrollPane(recordTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setPreferredSize(new Dimension(300, 120));
        recordDetailArea.setFont(SMALL);
        recordDetailArea.setForeground(TEXT);
        recordDetailArea.setLineWrap(true);
        recordDetailArea.setWrapStyleWord(true);
        recordDetailArea.setEditable(false);
        recordDetailArea.setFocusable(false);
        recordDetailArea.setOpaque(false);
        recordDetailArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        // 줄바꿈된 문장 높이는 폭이 정해진 뒤에 알 수 있어서, 폭이 바뀌면 다시 배치합니다.
        recordDetailArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(HistoryPanel.this::revalidate);
            }
        });
        recordTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showRecordDetail(recordTable.getSelectedRow());
            }
        });
        JPanel tableWithDetail = new JPanel(new BorderLayout(0, 6));
        tableWithDetail.setOpaque(false);
        tableWithDetail.add(scroll, BorderLayout.CENTER);
        tableWithDetail.add(recordDetailArea, BorderLayout.SOUTH);
        recordListArea.setOpaque(false);
        recordListArea.add(tableWithDetail, CARD_TABLE);
        recordListArea.add(emptyMessage("아직 학습 기록이 없습니다."), CARD_EMPTY);
        recordErrorLabel.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
        recordListArea.add(recordErrorLabel, CARD_ERROR);
        recentCard.add(recordListArea, BorderLayout.CENTER);
        body.add(recentCard, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);

        JButton mainButton = button("메인으로");
        JButton wrongNoteButton = button("오답노트");
        mainButton.addActionListener(e -> onGoMain.run());
        wrongNoteButton.addActionListener(e -> onOpenWrongNote.accept(selectedId(recordCategoryBox)));
        page.add(buttonRow(mainButton, wrongNoteButton), BorderLayout.SOUTH);

        recordCategoryBox.addActionListener(e -> {
            if (!updatingCombo) {
                onCategoryChanged.accept(selectedId(recordCategoryBox));
            }
        });
        return page;
    }

    private JComponent buildSummaryCard() {
        JPanel card = card();
        card.setLayout(new GridLayout(1, 3, 8, 0));
        card.add(summaryCell("총 풀이 수", totalCountValue));
        card.add(summaryCell("정답률", accuracyValue));
        card.add(summaryCell("누적 EXP", totalXpValue));
        return card;
    }

    private static JComponent summaryCell(String name, JLabel value) {
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setOpaque(false);
        cell.add(centeredLabel(name, SMALL, TEXT_MUTED), BorderLayout.NORTH);
        cell.add(value, BorderLayout.CENTER);
        return cell;
    }

    private void setUpRecordTable() {
        styleTable(recordTable);
        recordTable.setFont(SMALL);
        int[][] widths = {{84, 84}, {64, 90}, {100, Integer.MAX_VALUE}, {56, 90}, {40, 40}, {44, 44}};
        for (int i = 0; i < widths.length; i++) {
            var column = recordTable.getColumnModel().getColumn(i);
            column.setMinWidth(widths[i][0]);
            column.setPreferredWidth(widths[i][0]);
            column.setMaxWidth(widths[i][1]);
        }
        recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        recordTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!selected) {
                    c.setForeground("O".equals(value) ? CORRECT : WRONG);
                }
                return c;
            }
        });
        recordTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setToolTipText(value == null ? null : value.toString());
                return c;
            }
        });
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        recordTable.getColumnModel().getColumn(5).setCellRenderer(center);
    }

    public void showHistory(HistoryView view) {
        Objects.requireNonNull(view, "HistoryView가 null입니다. 학습 통계를 먼저 받아오세요.");

        selectCategory(recordCategoryBox, view.selectedCategoryId());

        totalCountValue.setText(String.valueOf(view.totalSubmissions()));
        accuracyValue.setText(percent(view.accuracyPercent(), view.totalSubmissions()));
        totalXpValue.setText(String.valueOf(view.totalXp()));

        rebuildCategoryRates(view.categoryRates());

        recordRows = view.recentRecords();
        recordModel.setRowCount(0);
        showRecordDetail(-1);
        for (HistoryView.RecordRow row : view.recentRecords()) {
            recordModel.addRow(new Object[]{
                    row.answeredAt() == null ? "-" : row.answeredAt().format(ANSWERED_AT_FORMAT),
                    orDash(row.categoryName()),
                    orDash(row.prompt()),
                    orDash(row.myAnswer()),
                    row.correct() ? "O" : "X",
                    signed(row.earnedXp())
            });
        }
        recordListPages.show(recordListArea, view.recentRecords().isEmpty() ? CARD_EMPTY : CARD_TABLE);

        showRecordPage();
        revalidate();
        repaint();
    }

    /** IOException, 삭제된 문제(IllegalStateException) 등으로 기록을 못 불러왔을 때 씁니다. */
    public void showHistoryError(String message) {
        totalCountValue.setText("-");
        accuracyValue.setText("-");
        totalXpValue.setText("-");
        categoryRatesPanel.removeAll();
        categoryRatesPanel.add(label("분야별 정답률을 불러오지 못했습니다.", SMALL, TEXT_MUTED));
        recordRows = List.of();
        recordModel.setRowCount(0);
        showRecordDetail(-1);
        String text = message == null || message.isBlank()
                ? "학습 기록을 불러오지 못했습니다. 다시 시도해 주세요."
                : message;
        recordErrorLabel.setText("<html><div style='text-align:center'>"
                + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div></html>");
        recordListPages.show(recordListArea, CARD_ERROR);

        showRecordPage();
        revalidate();
        repaint();
    }

    private void rebuildCategoryRates(List<HistoryView.CategoryRate> rates) {
        categoryRatesPanel.removeAll();
        if (rates.isEmpty()) {
            categoryRatesPanel.add(label("분야 정보가 없습니다.", SMALL, TEXT_MUTED));
            return;
        }
        int row = 0;
        for (HistoryView.CategoryRate rate : rates) {
            GridBagConstraints name = new GridBagConstraints();
            name.gridx = 0;
            name.gridy = row;
            name.anchor = GridBagConstraints.WEST;
            name.insets = new Insets(3, 0, 3, 12);
            categoryRatesPanel.add(label(orDash(rate.categoryName()), BODY, TEXT), name);

            JProgressBar bar = new JProgressBar(0, 1000);
            bar.setValue((int) Math.round(Math.max(0, Math.min(100, rate.accuracyPercent())) * 10));
            bar.setStringPainted(true);
            bar.setString(percent(rate.accuracyPercent(), rate.submissions())
                    + "  (" + rate.submissions() + "회)");
            bar.setFont(SMALL);
            bar.setForeground(PRIMARY);
            bar.setPreferredSize(new Dimension(180, 18));

            GridBagConstraints barGc = new GridBagConstraints();
            barGc.gridx = 1;
            barGc.gridy = row;
            barGc.weightx = 1;
            barGc.fill = GridBagConstraints.HORIZONTAL;
            barGc.insets = new Insets(3, 0, 3, 0);
            categoryRatesPanel.add(bar, barGc);
            row++;
        }
    }

    private JComponent buildWrongNotePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.add(titleBar("오답노트"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        body.add(categoryRow(wrongCategoryBox, wrongAccuracyLabel), BorderLayout.NORTH);

        styleTable(wrongTable);
        wrongTable.getColumnModel().getColumn(0).setMaxWidth(40);
        wrongTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        wrongTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        wrongTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showWrongDetail(wrongTable.getSelectedRow());
            }
        });
        wrongModel.addTableModelListener(e -> updateReviewButton());

        JScrollPane scroll = new JScrollPane(wrongTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        wrongListArea.setOpaque(false);
        wrongListArea.add(scroll, CARD_TABLE);
        wrongListArea.add(emptyMessage("틀린 문제가 없습니다. 모두 맞혔어요!"), CARD_EMPTY);

        JPanel listCard = card();
        listCard.setLayout(new BorderLayout(0, 8));
        listCard.add(label("복습할 문제를 체크하세요", BODY_BOLD, TEXT), BorderLayout.NORTH);
        listCard.add(wrongListArea, BorderLayout.CENTER);

        wrongDetailArea.setFont(BODY);
        wrongDetailArea.setLineWrap(true);
        wrongDetailArea.setWrapStyleWord(true);
        wrongDetailArea.setEditable(false);
        wrongDetailArea.setFocusable(false);
        wrongDetailArea.setOpaque(false);
        JPanel detailCard = card();
        detailCard.setLayout(new BorderLayout());
        detailCard.add(wrongDetailArea, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(listCard, BorderLayout.CENTER);
        center.add(detailCard, BorderLayout.SOUTH);
        body.add(center, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);

        JButton backButton = button("이전");
        JButton mainButton = button("메인으로");
        backButton.addActionListener(e -> showRecordPage());
        mainButton.addActionListener(e -> onGoMain.run());
        reviewButton.addActionListener(e -> {
            List<String> ids = checkedQuestionIds();
            if (!ids.isEmpty()) {
                onReview.accept(ids);
            }
        });
        page.add(buttonRow(mainButton, backButton, reviewButton), BorderLayout.SOUTH);

        wrongCategoryBox.addActionListener(e -> {
            if (!updatingCombo) {
                onWrongNoteCategoryChanged.accept(selectedId(wrongCategoryBox));
            }
        });
        return page;
    }

    public void showWrongNote(WrongNoteView view) {
        Objects.requireNonNull(view, "WrongNoteView가 null입니다. 오답 목록을 먼저 받아오세요.");

        selectCategory(wrongCategoryBox, view.selectedCategoryId());
        wrongAccuracyLabel.setText("정답률 " + percent(view.accuracyPercent(), view.submissions()));

        wrongItems = view.items();
        wrongModel.setItems(wrongItems);
        wrongListPages.show(wrongListArea, view.isEmpty() ? CARD_EMPTY : CARD_TABLE);
        showWrongDetail(-1);
        updateReviewButton();

        pages.show(this, CARD_WRONG_NOTE);
        revalidate();
        repaint();
    }

    private void showRecordDetail(int row) {
        if (row < 0 || row >= recordRows.size()) {
            recordDetailArea.setText("기록을 클릭하면 문제 전체를 볼 수 있어요.");
            return;
        }
        HistoryView.RecordRow r = recordRows.get(row);
        recordDetailArea.setText("문제: " + orDash(r.prompt()) + "\n내 답: " + orDash(r.myAnswer()));
    }

    private void showWrongDetail(int row) {
        if (row < 0 || row >= wrongItems.size()) {
            wrongDetailArea.setText(wrongItems.isEmpty()
                    ? "오답이 생기면 여기에서 정답과 해설을 다시 볼 수 있어요."
                    : "문제를 클릭하면 문제 전체와 정답·해설이 보여요.");
            return;
        }
        WrongNoteView.WrongItem item = wrongItems.get(row);
        wrongDetailArea.setText("문제: " + orDash(item.prompt())
                + "\n내 답: " + orDash(item.lastAnswer())
                + "    정답: " + orDash(item.correctAnswer())
                + "\n해설: " + orDash(item.explanation()));
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
        reviewButton.setEnabled(count > 0);
        reviewButton.setText(count > 0 ? "복습하기 (" + count + ")" : "복습하기");
    }

    public void setCategories(List<CategoryOption> categories) {
        Objects.requireNonNull(categories, "분야 목록이 null입니다.");
        updatingCombo = true;
        try {
            recordCategoryBox.removeAllItems();
            wrongCategoryBox.removeAllItems();
            for (CategoryOption option : categories) {
                recordCategoryBox.addItem(option);
                wrongCategoryBox.addItem(option);
            }
        } finally {
            updatingCombo = false;
        }
    }

    public void showRecordPage() {
        pages.show(this, CARD_RECORD);
    }

    private void selectCategory(JComboBox<CategoryOption> box, String categoryId) {
        updatingCombo = true;
        try {
            for (int i = 0; i < box.getItemCount(); i++) {
                if (Objects.equals(box.getItemAt(i).id(), categoryId)) {
                    box.setSelectedIndex(i);
                    return;
                }
            }
            if (box.getItemCount() > 0) {
                box.setSelectedIndex(0);
            }
        } finally {
            updatingCombo = false;
        }
    }

    private static String selectedId(JComboBox<CategoryOption> box) {
        Object selected = box.getSelectedItem();
        return (selected instanceof CategoryOption option) ? option.id() : null;
    }

    private static JComponent categoryRow(JComboBox<CategoryOption> box, JLabel trailing) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(label("분야 선택", BODY_BOLD, TEXT));
        box.setFont(BODY);
        box.setPreferredSize(new Dimension(140, 30));
        row.add(box);
        if (trailing != null) {
            row.add(Box.createHorizontalStrut(12));
            row.add(trailing);
        }
        return row;
    }

    private static JComponent buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 20, 0));
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private static JComponent emptyMessage(String text) {
        JLabel label = centeredLabel(text, BODY, TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));
        return label;
    }

    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private static void styleTable(JTable table) {
        table.setFont(BODY);
        table.setRowHeight(26);
        table.getTableHeader().setFont(BODY_BOLD);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public void setOnGoMain(Runnable action) {
        this.onGoMain = (action != null) ? action : () -> { };
    }

    /** 분야 ID를 전달하며, 「전체」면 null입니다. */
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

    private static final class WrongNoteTableModel extends DefaultTableModel {
        WrongNoteTableModel() {
            super(new Object[]{"", "분야", "문제"}, 0);
        }

        void setItems(List<WrongNoteView.WrongItem> items) {
            setRowCount(0);
            for (WrongNoteView.WrongItem item : items) {
                addRow(new Object[]{Boolean.FALSE, orDash(item.categoryName()), orDash(item.prompt())});
            }
        }

        boolean isChecked(int row) {
            return row < getRowCount() && Boolean.TRUE.equals(getValueAt(row, 0));
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }
    }
}
