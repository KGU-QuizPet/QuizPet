package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.UiStyle;

/**
 * 캐릭터 이미지·이름·레벨·경험치·능력치를 한곳에 보여 주는 사이드바입니다(UI-03 왼쪽 320px 영역).
 * 화면은 값을 받아 표시만 하며, 레벨·경험치 계산은 Pet(백엔드)이 담당합니다.
 */
public final class PetStatusPanel extends JPanel {
  public static final int SIDEBAR_WIDTH = 320;
  public static final String DEFAULT_MESSAGE = "오늘은 어떤 분야를 공부할까요?";

  public enum Situation {
    HOME(PetMood.IDLE, DEFAULT_MESSAGE, "오늘도 함께 배워 봐요!", "작은 배움이 나를 키워요."),
    GROWTH(PetMood.HAPPY, "함께 공부해서 더 강해졌어요!", "조금씩 자라는 중이에요!", "다음 퀴즈도 함께 풀어요!"),
    LEVEL_UP(PetMood.LEVEL_UP, "레벨 업! 함께 해냈어요!", "한 단계 더 성장했어요!", "새로운 도전이 기대돼요!"),
    BOSS_SELECT(PetMood.FOCUS, "어떤 보스에게 도전할까요?", "배운 실력을 보여 줘요!", "준비됐어요. 함께 도전해요!"),
    HISTORY(PetMood.HAPPY, "우리가 쌓은 기록을 살펴봐요!", "꾸준히 배운 흔적이 보여요!", "어떤 분야를 많이 풀었나요?"),
    WRONG_NOTE(PetMood.FOCUS, "틀린 문제도 다시 도전해요!", "천천히 복습하면 괜찮아요.", "함께 하나씩 익혀 봐요!");

    private final PetMood mood;
    private final String[] messages;

    Situation(PetMood mood, String... messages) {
      this.mood = mood;
      this.messages = messages;
    }
  }

  private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 22);
  private static final Font CAPTION_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
  private static final Font SMALL_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
  private static final Font CHIP_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
  private static final Font VALUE_FONT = new Font(Font.DIALOG, Font.BOLD, 17);
  private static final Font SUMMARY_FONT = new Font(Font.DIALOG, Font.BOLD, 19);

  private final PetStage stage = new PetStage();
  private final JLabel nameLabel = new JLabel(" ");
  private final JLabel appearanceLabel = new JLabel(" ");
  private final Badge levelBadge = new Badge("Lv.1", UiStyle.INK_PANEL, Color.WHITE,
      new Font(Font.DIALOG, Font.BOLD, 14), 52);
  private final GaugeBar expBar = new GaugeBar(UiStyle.EXP, 14, true);
  private final JLabel expLabel = new JLabel(" ");
  private final JLabel remainLabel = new JLabel(" ", SwingConstants.RIGHT);
  private final Map<String, GaugeBar> statBars = new LinkedHashMap<>();
  private final Map<String, JLabel> statValues = new LinkedHashMap<>();
  private final Map<String, JLabel> statNames = new LinkedHashMap<>();
  private final Map<String, JLabel> statDeltas = new LinkedHashMap<>();
  private final Map<String, Integer> currentStats = new LinkedHashMap<>();
  private final JLabel weakHint = new JLabel(" ");
  private int currentXpInLevel;
  private int currentXpPerLevel = 1;
  private final JPanel summary = new RoundedBox(UiStyle.SUNKEN, null);
  private final JLabel accuracyLabel = new JLabel("-");
  private final JLabel solvedLabel = new JLabel("-", SwingConstants.RIGHT);

  private String spriteKey = "DUCK";
  private Situation situation;
  private int messageIndex;
  private final javax.swing.Timer messageTimer = new javax.swing.Timer(6000, e -> {
    if (isShowing() && situation != null) {
      messageIndex = (messageIndex + 1) % situation.messages.length;
      stage.showPet(spriteKey, situation.mood, situation.messages[messageIndex]);
    }
  });

  public PetStatusPanel() {
    setLayout(new BorderLayout(0, 16));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 2, UiStyle.BORDER),
        BorderFactory.createEmptyBorder(20, 20, 20, 20)));
    setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
    setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));

    stage.setAmbientAnimation(true);
    add(stage, BorderLayout.CENTER);
    add(createInfo(), BorderLayout.SOUTH);
    showSituation(Situation.HOME);
    addHierarchyListener(e -> {
      if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
        if (isShowing() && situation != null) messageTimer.restart();
        else messageTimer.stop();
      }
    });
    summary.setVisible(false);
  }

  /**
   * 캐릭터 상태를 표시합니다.
   *
   * @param xpInLevel 현재 레벨 안에서 모은 경험치(Pet.getXpInLevel())
   * @param xpPerLevel 레벨당 필요한 경험치(Pet.XP_PER_LEVEL)
   */
  public void showPet(String petName, String spriteKey, int level, int xpInLevel, int xpPerLevel,
      int str, int intel, int foc, int wis) {
    this.spriteKey = spriteKey;
    nameLabel.setText(petName);
    appearanceLabel.setText(UiStyle.appearanceOf(spriteKey).getDisplayName());
    levelBadge.setText("Lv." + level);
    levelBadge.getAccessibleContext().setAccessibleName("레벨 " + level);

    int safeXp = Math.max(0, Math.min(xpInLevel, xpPerLevel));
    expBar.setValue(safeXp, xpPerLevel);
    expBar.getAccessibleContext().setAccessibleName("경험치");
    expLabel.setText("EXP " + safeXp + " / " + xpPerLevel);
    remainLabel.setText("다음 레벨까지 " + (xpPerLevel - safeXp));

    currentXpInLevel = safeXp;
    currentXpPerLevel = xpPerLevel;

    Map<String, Integer> values = Map.of("STR", str, "INT", intel, "FOC", foc, "WIS", wis);
    currentStats.clear();
    currentStats.putAll(values);
    int scale = statScale(Math.max(Math.max(str, intel), Math.max(foc, wis)));
    for (String type : UiStyle.STAT_TYPES) {
      int value = values.get(type);
      statBars.get(type).setValue(value, scale);
      statValues.get(type).setText(String.valueOf(value));
      statDeltas.get(type).setText(" ");
    }
    markWeakest(StatComparisonPanel.weakestStats(values));
    stage.showPet(spriteKey, stage.getMood(), stage.getMessage());
    revalidate();
    repaint();
  }

  /** 가장 낮은 능력치 이름을 강조하고, 올리는 방법을 능력치 아래에 안내합니다. */
  private void markWeakest(java.util.List<String> weakest) {
    for (String type : UiStyle.STAT_TYPES) {
      boolean weak = weakest.contains(type);
      JLabel name = statNames.get(type);
      name.setForeground(weak ? UiStyle.WRONG : UiStyle.MUTED);
      name.setToolTipText(weak ? "가장 낮은 능력치예요. "
          + String.join("·", StatComparisonPanel.categoryNames(type)) + " 퀴즈를 맞히면 올라요." : null);
    }
    weakHint.setText(weakest.isEmpty() ? " " : shortWeaknessHint(weakest));
    weakHint.setToolTipText(weakest.isEmpty() ? null : StatComparisonPanel.weaknessHint(weakest));
  }

  /** 사이드바용 짧은 안내입니다. 예: "체력이 가장 낮아요 · 스포츠 퀴즈 추천" */
  static String shortWeaknessHint(java.util.List<String> weakest) {
    java.util.List<String> stats = new java.util.ArrayList<>();
    java.util.List<String> categories = new java.util.ArrayList<>();
    for (String type : weakest) {
      stats.add(UiStyle.statLabel(type));
      categories.addAll(StatComparisonPanel.categoryNames(type));
    }
    String last = stats.get(stats.size() - 1);
    char end = last.charAt(last.length() - 1);
    String particle = end >= 0xAC00 && end <= 0xD7A3 && (end - 0xAC00) % 28 == 0 ? "가" : "이";
    return String.join("·", stats) + particle + " 가장 낮아요 · " + String.join("·", categories) + " 퀴즈 추천";
  }

  /**
   * showPet 다음에 호출하여 지난 퀴즈에서 늘어난 만큼을 강조합니다.
   * 늘어난 능력치는 막대가 성장 전 값에서 차오르고 옆에 "+1"을 표시합니다.
   *
   * @param before 성장 전 능력치(키: STR·INT·FOC·WIS)
   * @param gainedXp 이번에 얻은 경험치. 레벨이 오르지 않았으면 경험치 막대도 이전 값에서 차오릅니다.
   * @param leveledUp 레벨이 올랐으면 경험치 막대를 0에서부터 채웁니다.
   */
  public void showGrowth(Map<String, Integer> before, int gainedXp, boolean leveledUp) {
    if (before == null) {
      throw new IllegalArgumentException("성장 전 능력치는 필수입니다.");
    }
    int scale = statBars.get(UiStyle.STAT_TYPES[0]).getMaximum();
    for (String type : UiStyle.STAT_TYPES) {
      Integer from = before.get(type);
      int to = currentStats.getOrDefault(type, 0);
      if (from == null || from >= to) {
        continue;
      }
      statBars.get(type).setValue(to, scale, from, true);
      JLabel delta = statDeltas.get(type);
      delta.setText("+" + (to - from));
    }
    if (gainedXp > 0) {
      int fromXp = leveledUp ? 0 : Math.max(0, currentXpInLevel - gainedXp);
      expBar.setValue(currentXpInLevel, currentXpPerLevel, fromXp, true);
    }
    revalidate();
    repaint();
  }

  /** 능력치 옆에 표시된 증가량입니다(없으면 빈 문자열). */
  public String getStatDeltaText(String statType) {
    return statDeltas.get(statType).getText().strip();
  }

  /** 가장 낮은 능력치로 강조된 능력치인지 여부입니다. */
  public boolean isStatMarkedWeak(String statType) {
    return statNames.get(statType).getToolTipText() != null;
  }

  public String getWeakHintText() {
    return weakHint.getText().strip();
  }

  /** 표정과 말풍선 문구를 바꿉니다. message가 비어 있으면 말풍선을 숨깁니다. */
  public void setMood(PetMood mood, String message) {
    situation = null;
    messageTimer.stop();
    stage.showPet(spriteKey, mood, message);
  }

  /** 화면 상황이 바뀌면 첫 문구를 표시합니다. 같은 상황은 현재 문구와 타이머를 유지합니다. */
  public void showSituation(Situation next) {
    java.util.Objects.requireNonNull(next, "상황은 필수입니다.");
    if (situation == next) return;
    messageIndex = 0;
    situation = next;
    stage.showPet(spriteKey, next.mood, next.messages[messageIndex]);
    if (isShowing()) messageTimer.restart();
  }

  @Override
  public void removeNotify() {
    messageTimer.stop();
    super.removeNotify();
  }

  /** 아래쪽 누적 정답률·푼 문제 요약을 표시합니다(accuracy는 0~100 퍼센트). */
  public void showSummary(double accuracy, int solved) {
    accuracyLabel.setText(String.format("%.1f%%", accuracy));
    solvedLabel.setText(String.valueOf(solved));
    summary.setVisible(true);
    revalidate();
  }

  public void hideSummary() {
    summary.setVisible(false);
    revalidate();
  }

  /**
   * 능력치 막대의 최대값입니다. 가장 높은 능력치보다 한 단계(10) 이상 여유를 두어
   * 성장해도 막대가 금방 가득 차지 않게 하고, 최소 30을 기준으로 합니다(예: 18 → 30, 34 → 40).
   */
  static int statScale(int highest) {
    return Math.max(30, (highest / 10 + 1) * 10);
  }

  public PetStage getStage() {
    return stage;
  }

  public String getLevelText() {
    return levelBadge.getText();
  }

  public String getExpText() {
    return expLabel.getText();
  }

  public String getRemainText() {
    return remainLabel.getText();
  }

  public GaugeBar getExpBar() {
    return expBar;
  }

  public GaugeBar getStatBar(String statType) {
    return statBars.get(statType);
  }

  public String getStatText(String statType) {
    return statValues.get(statType).getText();
  }

  public boolean isSummaryVisible() {
    return summary.isVisible();
  }

  private JPanel createInfo() {
    JPanel info = new JPanel(new GridBagLayout());
    info.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;

    nameLabel.setFont(NAME_FONT);
    nameLabel.setForeground(UiStyle.TEXT);
    appearanceLabel.setFont(SMALL_FONT);
    appearanceLabel.setForeground(UiStyle.MUTED);
    JPanel nameRow = new JPanel(new BorderLayout(8, 0));
    nameRow.setOpaque(false);
    JPanel names = new JPanel(new BorderLayout(6, 0));
    names.setOpaque(false);
    names.add(nameLabel, BorderLayout.WEST);
    names.add(appearanceLabel, BorderLayout.CENTER);
    nameRow.add(names, BorderLayout.CENTER);
    nameRow.add(levelBadge, BorderLayout.EAST);
    info.add(nameRow, c);

    c.gridy++;
    c.insets = new Insets(14, 0, 6, 0);
    info.add(expBar, c);

    c.gridy++;
    c.insets = new Insets(0, 0, 0, 0);
    expLabel.setFont(CAPTION_FONT);
    expLabel.setForeground(UiStyle.TEXT);
    remainLabel.setFont(CAPTION_FONT);
    remainLabel.setForeground(UiStyle.MUTED);
    JPanel expRow = new JPanel(new BorderLayout());
    expRow.setOpaque(false);
    expRow.add(expLabel, BorderLayout.WEST);
    expRow.add(remainLabel, BorderLayout.EAST);
    info.add(expRow, c);

    c.gridy++;
    c.insets = new Insets(18, 0, 8, 0);
    JLabel statsTitle = new JLabel("능력치");
    statsTitle.setFont(CAPTION_FONT);
    statsTitle.setForeground(UiStyle.MUTED);
    info.add(statsTitle, c);

    c.insets = new Insets(0, 0, 10, 0);
    for (String type : UiStyle.STAT_TYPES) {
      c.gridy++;
      info.add(createStatRow(type), c);
    }

    c.gridy++;
    c.insets = new Insets(0, 0, 0, 0);
    weakHint.setFont(SMALL_FONT);
    weakHint.setForeground(UiStyle.MUTED);
    // 사이드바 폭을 넘는 안내는 줄임표로 자르고 전체 문장은 툴팁으로 보여 줍니다.
    weakHint.setPreferredSize(new Dimension(SIDEBAR_WIDTH - 40, weakHint.getPreferredSize().height));
    info.add(weakHint, c);

    c.gridy++;
    c.insets = new Insets(10, 0, 0, 0);
    info.add(createSummary(), c);
    return info;
  }

  private JPanel createStatRow(String type) {
    Color color = UiStyle.statColor(type);
    Badge chip = new Badge(type, color, Color.WHITE, CHIP_FONT, 54);
    JLabel name = new JLabel(UiStyle.statLabel(type));
    name.setFont(CAPTION_FONT);
    name.setForeground(UiStyle.MUTED);
    name.setPreferredSize(new Dimension(36, name.getPreferredSize().height));
    GaugeBar bar = new GaugeBar(color, 10, false);
    bar.getAccessibleContext().setAccessibleName(UiStyle.statLabel(type) + "(" + type + ")");
    JLabel value = new JLabel("0", SwingConstants.RIGHT);
    value.setFont(VALUE_FONT);
    value.setForeground(UiStyle.TEXT);
    value.setPreferredSize(new Dimension(34, value.getPreferredSize().height));
    JLabel delta = new JLabel(" ", SwingConstants.LEFT);
    delta.setFont(CHIP_FONT);
    delta.setForeground(UiStyle.CORRECT);
    // 증가량이 있든 없든 모든 줄의 막대 길이가 같도록 칸 폭을 고정합니다.
    Dimension deltaSize = new Dimension(24, delta.getPreferredSize().height);
    delta.setPreferredSize(deltaSize);
    delta.setMinimumSize(deltaSize);
    value.setMinimumSize(value.getPreferredSize());
    name.setMinimumSize(name.getPreferredSize());
    // 사이드바 폭(320px) 안에 들어가도록 막대의 기본 폭을 줄이고 남는 폭은 막대가 채웁니다.
    bar.setPreferredSize(new Dimension(80, bar.getPreferredSize().height));
    statBars.put(type, bar);
    statValues.put(type, value);
    statNames.put(type, name);
    statDeltas.put(type, delta);

    JPanel row = new JPanel(new GridBagLayout());
    row.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    c.anchor = GridBagConstraints.WEST;
    c.insets = new Insets(0, 0, 0, 10);
    row.add(chip, c);
    row.add(name, c);
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    row.add(bar, c);
    c.weightx = 0;
    c.fill = GridBagConstraints.NONE;
    c.insets = new Insets(0, 0, 0, 0);
    row.add(value, c);
    c.insets = new Insets(0, 4, 0, 0);
    row.add(delta, c);
    return row;
  }

  private JPanel createSummary() {
    summary.setLayout(new BorderLayout());
    summary.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
    summary.add(summaryColumn("누적 정답률", accuracyLabel, SwingConstants.LEFT), BorderLayout.WEST);
    summary.add(summaryColumn("푼 문제", solvedLabel, SwingConstants.RIGHT), BorderLayout.EAST);
    return summary;
  }

  private static JPanel summaryColumn(String title, JLabel value, int alignment) {
    JLabel titleLabel = new JLabel(title, alignment);
    titleLabel.setFont(CAPTION_FONT);
    titleLabel.setForeground(UiStyle.MUTED);
    value.setFont(SUMMARY_FONT);
    value.setForeground(UiStyle.TEXT);
    value.setHorizontalAlignment(alignment);
    JPanel column = new JPanel(new BorderLayout(0, 2));
    column.setOpaque(false);
    column.add(titleLabel, BorderLayout.NORTH);
    column.add(value, BorderLayout.CENTER);
    return column;
  }

  /** 둥근 모서리 배경 패널입니다. border 색이 null이면 테두리를 그리지 않습니다. */
  static final class RoundedBox extends JPanel {
    private final Color fill;
    private final Color border;

    RoundedBox(Color fill, Color border) {
      this.fill = fill;
      this.border = border;
      setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(fill);
      g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
      if (border != null) {
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
      }
      g.dispose();
      super.paintComponent(graphics);
    }
  }
}
