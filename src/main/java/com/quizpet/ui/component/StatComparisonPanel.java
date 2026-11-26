package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

/**
 * 능력치 네 가지를 성장 전·후로 비교해 보여 줍니다(퀴즈 결과 화면).
 * 늘어난 능력치는 막대의 증가분과 "10 → 11 (+1)" 문구로 강조하고,
 * 가장 낮은 능력치에는 "보충 필요" 배지와 올리는 방법을 안내합니다.
 * 값 계산은 하지 않고 받은 값만 표시합니다.
 */
public final class StatComparisonPanel extends JPanel {
  private static final Font CAPTION_FONT = new Font(Font.DIALOG, Font.BOLD, 13);
  private static final Font CHIP_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
  private static final Font VALUE_FONT = new Font(Font.DIALOG, Font.BOLD, 15);
  private static final Font HINT_FONT = new Font(Font.DIALOG, Font.PLAIN, 13);

  private final Map<String, GaugeBar> bars = new LinkedHashMap<>();
  private final Map<String, JLabel> changes = new LinkedHashMap<>();
  private final Map<String, Badge> weakBadges = new LinkedHashMap<>();
  private final JLabel hintLabel = new JLabel(" ");

  public StatComparisonPanel() {
    setLayout(new BorderLayout(0, 8));
    setOpaque(false);
    JLabel title = new JLabel("능력치 변화");
    title.setFont(CAPTION_FONT);
    title.setForeground(UiStyle.MUTED);
    add(title, BorderLayout.NORTH);

    JPanel rows = new JPanel(new GridBagLayout());
    rows.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = new Insets(0, 0, 8, 0);
    for (String type : UiStyle.STAT_TYPES) {
      rows.add(createRow(type), c);
    }
    add(rows, BorderLayout.CENTER);

    hintLabel.setFont(HINT_FONT);
    hintLabel.setForeground(UiStyle.MUTED);
    add(hintLabel, BorderLayout.SOUTH);
  }

  /**
   * 성장 전·후 능력치를 표시합니다. 키는 STR·INT·FOC·WIS입니다.
   *
   * @param animate true면 늘어난 막대가 차오르는 애니메이션을 보여 줍니다.
   */
  public void showComparison(Map<String, Integer> before, Map<String, Integer> after, boolean animate) {
    requireStats(before, "before");
    requireStats(after, "after");
    int highest = 0;
    for (String type : UiStyle.STAT_TYPES) {
      highest = Math.max(highest, Math.max(before.get(type), after.get(type)));
    }
    int scale = PetStatusPanel.statScale(highest);
    List<String> weakest = weakestStats(after);
    for (String type : UiStyle.STAT_TYPES) {
      int from = before.get(type);
      int to = after.get(type);
      boolean grew = to > from;
      bars.get(type).setValue(to, scale, grew ? from : -1, animate && grew);
      JLabel change = changes.get(type);
      change.setText(grew ? from + " → " + to + "  (+" + (to - from) + ")" : String.valueOf(to));
      change.setForeground(grew ? UiStyle.CORRECT : UiStyle.TEXT);
      weakBadges.get(type).setVisible(weakest.contains(type));
    }
    hintLabel.setText(weakest.isEmpty() ? " " : weaknessHint(weakest));
    revalidate();
    repaint();
  }

  /**
   * 가장 낮은 능력치의 종류입니다(표시 순서). 네 능력치가 모두 같으면 강조할 것이 없으므로 빈 목록입니다.
   */
  public static List<String> weakestStats(Map<String, Integer> stats) {
    requireStats(stats, "stats");
    int lowest = Collections.min(stats.values());
    int highest = Collections.max(stats.values());
    if (lowest == highest) {
      return List.of();
    }
    List<String> weakest = new ArrayList<>();
    for (String type : UiStyle.STAT_TYPES) {
      if (stats.get(type) == lowest) {
        weakest.add(type);
      }
    }
    return List.copyOf(weakest);
  }

  /** 예: "체력(STR)이 가장 낮아요. 스포츠 퀴즈로 키워 보세요." */
  public static String weaknessHint(List<String> weakest) {
    if (weakest == null || weakest.isEmpty()) {
      return "";
    }
    List<String> stats = new ArrayList<>();
    List<String> categories = new ArrayList<>();
    for (String type : weakest) {
      stats.add(UiStyle.statLabel(type) + "(" + type + ")");
      categories.addAll(categoryNames(type));
    }
    String last = UiStyle.statLabel(weakest.get(weakest.size() - 1));
    return String.join("·", stats) + subjectParticle(last) + " 가장 낮아요. "
        + String.join("·", categories) + " 퀴즈로 키워 보세요.";
  }

  /** 능력치를 올려 주는 학습 분야 이름입니다(INT는 과학·상식). */
  static List<String> categoryNames(String statType) {
    List<String> names = new ArrayList<>();
    for (Category category : Category.values()) {
      if (category.getStatType().equals(statType)) {
        names.add(category.getName());
      }
    }
    return names;
  }

  private static String subjectParticle(String word) {
    char last = word.charAt(word.length() - 1);
    if (last < 0xAC00 || last > 0xD7A3) {
      return "이";
    }
    return (last - 0xAC00) % 28 == 0 ? "가" : "이";
  }

  private static void requireStats(Map<String, Integer> stats, String name) {
    if (stats == null) {
      throw new IllegalArgumentException(name + " 능력치는 필수입니다.");
    }
    for (String type : UiStyle.STAT_TYPES) {
      Integer value = stats.get(type);
      if (value == null || value < 0) {
        throw new IllegalArgumentException(name + " 능력치에 " + type + " 값이 없거나 음수입니다.");
      }
    }
  }

  public GaugeBar getBar(String statType) {
    return bars.get(statType);
  }

  public String getChangeText(String statType) {
    return changes.get(statType).getText();
  }

  public boolean isMarkedWeak(String statType) {
    return weakBadges.get(statType).isVisible();
  }

  public String getHintText() {
    return hintLabel.getText();
  }

  private JPanel createRow(String type) {
    Color color = UiStyle.statColor(type);
    Badge chip = new Badge(type, color, Color.WHITE, CHIP_FONT, 54);
    JLabel name = new JLabel(UiStyle.statLabel(type));
    name.setFont(CAPTION_FONT);
    name.setForeground(UiStyle.MUTED);
    name.setPreferredSize(new Dimension(40, name.getPreferredSize().height));
    GaugeBar bar = new GaugeBar(color, 12, false);
    bar.getAccessibleContext().setAccessibleName(UiStyle.statLabel(type) + "(" + type + ") 변화");
    JLabel change = new JLabel(" ", SwingConstants.LEFT);
    change.setFont(VALUE_FONT);
    change.setPreferredSize(new Dimension(120, change.getPreferredSize().height));
    Badge weak = new Badge("보충 필요", UiStyle.WRONG_SOFT, UiStyle.WRONG, CHIP_FONT, 0);
    weak.setVisible(false);
    weak.setToolTipText("가장 낮은 능력치예요. " + String.join("·", categoryNames(type)) + " 퀴즈를 맞히면 올라요.");
    bars.put(type, bar);
    changes.put(type, change);
    weakBadges.put(type, weak);

    JPanel row = new JPanel(new GridBagLayout());
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
    row.add(change, c);
    c.insets = new Insets(0, 0, 0, 0);
    // 배지가 숨겨져도 줄 높이와 열 위치가 바뀌지 않도록 고정 폭 칸에 넣습니다.
    JPanel weakSlot = new JPanel(new BorderLayout());
    weakSlot.setOpaque(false);
    weakSlot.setPreferredSize(new Dimension(84, weak.getPreferredSize().height));
    weakSlot.add(weak, BorderLayout.WEST);
    row.add(weakSlot, c);
    return row;
  }
}
