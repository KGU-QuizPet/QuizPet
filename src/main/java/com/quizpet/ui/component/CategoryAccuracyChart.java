package main.java.com.quizpet.ui.component;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

/** 읽기 전용 차트. 서비스 조회·정답률 계산은 호출자 책임이며 갱신은 EDT에서 수행합니다. */
public final class CategoryAccuracyChart extends JPanel {
  public enum DisplayState { RECORDED, NOT_STARTED, UNAVAILABLE, ERROR }

  /** accuracy는 StatisticsService가 계산한 0~100 백분율입니다. */
  public record CategoryData(Category category, int attempts, double accuracy) {
    public CategoryData {
      Objects.requireNonNull(category, "category");
      if (attempts < 0 || !Double.isFinite(accuracy) || accuracy < 0 || accuracy > 100
          || (attempts == 0 && accuracy != 0)) {
        throw new IllegalArgumentException("풀이 수·정답률이 올바르지 않습니다.");
      }
    }
  }

  private final JPanel rows = new JPanel();
  private final Map<Category, DisplayState> states = new EnumMap<>(Category.class);
  private final JTextArea notice = new JTextArea();

  public CategoryAccuracyChart() {
    super(new BorderLayout(0, 12));
    setOpaque(false);
    setBorder(new EmptyBorder(16, 16, 16, 16));
    JLabel title = new JLabel("분야별 정답률");
    title.setFont(UiStyle.SUBTITLE_FONT);
    title.setForeground(UiStyle.TEXT);
    add(title, BorderLayout.NORTH);
    rows.setOpaque(false);
    rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
    JScrollPane scroll = new JScrollPane(rows);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    add(scroll, BorderLayout.CENTER);
    notice.setEditable(false);
    notice.setLineWrap(true);
    notice.setWrapStyleWord(true);
    notice.setOpaque(false);
    notice.setFont(UiStyle.BODY_FONT);
    notice.setForeground(UiStyle.MUTED);
    notice.setRows(3);
    notice.getAccessibleContext().setAccessibleName("통계 상태 안내");
    setPreferredSize(new Dimension(440, 370));
    setMinimumSize(new Dimension(180, 180));
    getAccessibleContext().setAccessibleName("분야별 정답률 차트");
    render(Map.of(), Set.of(), null);
  }

  /** 누락 분야는 학습 전으로 표시합니다. 조회되지 않은 분야를 0건으로 넘기지 마세요. */
  public void setData(List<CategoryData> data) {
    setData(data, Set.of());
  }

  /** 집계 불가 분야는 unavailable로 명시합니다. 중복·상태 충돌은 기존 화면을 유지하며 거부합니다. */
  public void setData(List<CategoryData> data, Set<Category> unavailable) {
    requireEdt();
    Map<Category, CategoryData> indexed = new EnumMap<>(Category.class);
    Set<Category> unknown = Set.copyOf(Objects.requireNonNull(unavailable));
    for (CategoryData item : List.copyOf(Objects.requireNonNull(data))) {
      if (unknown.contains(item.category()) || indexed.putIfAbsent(item.category(), item) != null) {
        throw new IllegalArgumentException("중복 분야 또는 집계 상태 충돌입니다: " + item.category());
      }
    }
    render(indexed, unknown, null);
  }

  /** 분야를 확정할 수 없는 기록이 있어 모든 분야 집계가 불가능한 경우입니다. */
  public void showUnavailable() {
    requireEdt();
    render(Map.of(), EnumSet.allOf(Category.class), null);
  }

  /** 사용자에게 표시할 안내만 전달합니다. 과거 값은 제거해 성공 데이터처럼 보이지 않게 합니다. */
  public void showError(String message) {
    requireEdt();
    if (message == null || message.isBlank()) throw new IllegalArgumentException("오류 안내는 필수입니다.");
    render(Map.of(), Set.of(), message);
  }

  public DisplayState getDisplayState(Category category) {
    requireEdt();
    return states.get(Objects.requireNonNull(category));
  }

  private void render(Map<Category, CategoryData> data, Set<Category> unavailable, String error) {
    rows.removeAll();
    states.clear();
    for (Category category : Category.values()) {
      CategoryData value = data.get(category);
      DisplayState state = error != null ? DisplayState.ERROR
          : unavailable.contains(category) ? DisplayState.UNAVAILABLE
          : value == null || value.attempts() == 0 ? DisplayState.NOT_STARTED : DisplayState.RECORDED;
      states.put(category, state);
      String metric = switch (state) {
        case RECORDED -> String.format(Locale.ROOT, "%.1f%%", value.accuracy());
        case NOT_STARTED -> "— 학습 전";
        case UNAVAILABLE -> "집계 불가";
        case ERROR -> "조회 실패";
      };
      String count = state == DisplayState.RECORDED ? value.attempts() + "회 풀이"
          : state == DisplayState.NOT_STARTED ? "0회 풀이" : "풀이 수 확인 불가";
      JPanel row = new JPanel(new GridBagLayout());
      row.setOpaque(false);
      row.setBorder(new EmptyBorder(8, 0, 8, 0));
      JLabel name = new JLabel(category.getName());
      name.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD));
      name.setForeground(UiStyle.TEXT);
      name.setPreferredSize(new Dimension(Math.max(80, name.getPreferredSize().width), 24));
      JLabel percent = new JLabel(metric);
      percent.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 18f));
      percent.setForeground(state == DisplayState.RECORDED ? UiStyle.TEXT : UiStyle.MUTED);
      JLabel attempts = new JLabel(count, SwingConstants.RIGHT);
      attempts.setFont(UiStyle.BODY_FONT);
      attempts.setForeground(UiStyle.MUTED);
      String description = category.getName() + ": " + metric + ", " + count;
      RateBar bar = new RateBar();
      bar.setForeground(UiStyle.categoryColor(category));
      bar.setBackground(UiStyle.BORDER);
      bar.setValue(state == DisplayState.RECORDED ? (int) Math.round(value.accuracy() * 10) : 0);
      bar.setFocusable(true);
      bar.getAccessibleContext().setAccessibleName(description);
      bar.getAccessibleContext().setAccessibleDescription("읽기 전용 정답률, Tab 키로 다음 분야 이동");
      bar.setToolTipText(description);
      name.setLabelFor(bar);
      GridBagConstraints c = new GridBagConstraints();
      c.gridy = 0; c.gridx = 0; c.anchor = GridBagConstraints.WEST;
      row.add(name, c);
      c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(0, 8, 0, 12);
      row.add(bar, c);
      c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(0, 0, 0, 0);
      row.add(percent, c);
      c.gridy = 1; c.gridx = 1; c.gridwidth = 2; c.anchor = GridBagConstraints.EAST;
      row.add(attempts, c);
      // 텍스트와 최소 막대 폭을 보존하고 더 좁은 부모에서는 수평 스크롤을 허용합니다.
      row.setMinimumSize(row.getPreferredSize());
      row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
      rows.add(row);
    }
    notice.setText(error == null ? "" : error);
    notice.setCaretPosition(0);
    if (error != null) {
      JScrollPane message = new JScrollPane(notice,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      message.setBorder(BorderFactory.createEmptyBorder());
      message.setOpaque(false);
      message.getViewport().setOpaque(false);
      message.setPreferredSize(new Dimension(300, 90));
      message.setMinimumSize(new Dimension(0, 90));
      message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
      rows.add(message);
    }
    rows.revalidate();
    rows.repaint();
    getAccessibleContext().setAccessibleDescription(error == null ? "5개 분야의 정답률·풀이 수" : "통계 조회 실패: " + error);
  }

  private static void requireEdt() {
    if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("차트 갱신은 EDT에서 실행해 주세요.");
  }

  private static final class RateBar extends JProgressBar {
    RateBar() {
      super(0, 1000);
      setPreferredSize(new Dimension(120, 18));
      setMinimumSize(new Dimension(60, 18));
      setBorderPainted(false);
      setOpaque(false);
      addFocusListener(new java.awt.event.FocusAdapter() {
        @Override public void focusGained(java.awt.event.FocusEvent event) { repaint(); }
        @Override public void focusLost(java.awt.event.FocusEvent event) { repaint(); }
      });
    }

    @Override protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = Math.max(0, getWidth() - 4);
        int height = Math.max(0, getHeight() - 4);
        g.setColor(getBackground());
        g.fillRoundRect(2, 2, width, height, height, height);
        g.setColor(getForeground());
        int filled = (int) Math.round(width * getPercentComplete());
        g.fillRoundRect(2, 2, filled, height, height, height);
        if (hasFocus()) {
          g.setColor(UiStyle.BRAND);
          g.drawRoundRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), getHeight(), getHeight());
        }
      } finally { g.dispose(); }
    }
  }
}