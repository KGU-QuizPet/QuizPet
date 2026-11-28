package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.Badge;
import main.java.com.quizpet.ui.component.CategoryIcon;
import main.java.com.quizpet.ui.component.CategoryProgressCard;
import main.java.com.quizpet.ui.component.ImageButton;
import main.java.com.quizpet.ui.component.PetStatusPanel;
import main.java.com.quizpet.ui.component.RoundCard;

public class HomePanel extends JPanel {
  private static final int EXP_PER_LEVEL = Pet.XP_PER_LEVEL;
  private static final int RECENT_MIN = 3;
  public static final int RECENT_MAX = 5;
  private static final int RECENT_ROW_HEIGHT = 58;
  private static final Font SECTION_FONT = new Font(Font.DIALOG, Font.BOLD, 20);

  public record RecentItem(Category category, String prompt, boolean correct, int earnedXp) { }

  private Runnable onLearn;
  private Runnable onHistory;
  private Runnable onWrongNote;
  private Consumer<String> onCategory = categoryId -> { };

  private final PetStatusPanel status = new PetStatusPanel();
  private final JLabel greetingLabel = new JLabel(" ");
  private final JLabel headlineLabel = new JLabel(" ");
  private final JLabel rewardLabel = new JLabel("처음 맞힌 문제마다 EXP +10, 해당 분야 능력치 +1");
  private final JLabel growthHintLabel = new JLabel(" ");
  private final Map<Category, CategoryProgressCard> progressCards = new EnumMap<>(Category.class);
  private final JPanel recentList = new JPanel();
  private List<RecentItem> recentItems = List.of();
  private int recentLimit = RECENT_MIN;
  private final JLabel wrongCountLabel = new JLabel(" ");
  private final JPanel wrongChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
  private final ImageButton reviewButton = new ImageButton("틀린 문제 다시 보기");

  public HomePanel(ScreenManager screens) {
    onLearn = () -> screens.show(ScreenManager.CATEGORY);
    onHistory = () -> screens.show(ScreenManager.HISTORY);
    onWrongNote = onHistory;
    setLayout(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);

    add(status, BorderLayout.WEST);
    add(createScroll(createContent()), BorderLayout.CENTER);
  }

  private static JScrollPane createScroll(JPanel content) {
    JPanel view = new ScrollableContent(content);
    JScrollPane scroll = new JScrollPane(view,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.getVerticalScrollBar().setUnitIncrement(24);
    return scroll;
  }

  private static final class ScrollableContent extends JPanel implements Scrollable {
    ScrollableContent(JPanel content) {
      super(new BorderLayout());
      setOpaque(false);
      add(content, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
      return 24;
    }

    @Override
    public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
      return visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return getParent() != null && getParent().getHeight() >= getMinimumSize().height;
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }
  }

  public void setOnLearn(Runnable action) { onLearn = action; }

  public void setOnHistory(Runnable action) { onHistory = action; }

  public void setOnWrongNote(Runnable action) { onWrongNote = action; }

  public void setOnCategory(Consumer<String> action) { onCategory = action; }

  public void showPet(String petName, String spriteKey, int level, int expInLevel,
      int str, int intel, int foc, int wis) {
    status.showPet(petName, spriteKey, level, expInLevel, EXP_PER_LEVEL, str, intel, foc, wis);
    status.setMood(PetMood.IDLE, PetStatusPanel.DEFAULT_MESSAGE);
    int remain = EXP_PER_LEVEL - Math.max(0, Math.min(expInLevel, EXP_PER_LEVEL));
    headlineLabel.setText("<html>" + escape(petName) + subjectParticle(petName) + " Lv." + (level + 1)
        + "까지 <font color='#FFC233'>" + remain + " EXP</font> 남았어요</html>");
    updateRecommendation(str, intel, foc, wis);
  }

  /**
   * showPet 다음에 호출하여 지난 퀴즈의 성장을 강조합니다.
   * 늘어난 능력치·경험치 막대가 차오르고, 캐릭터가 기뻐하며 성장(레벨 업)을 알려 줍니다.
   */
  public void showGrowth(java.util.Map<String, Integer> statsBefore, int gainedXp, boolean leveledUp) {
    status.showGrowth(statsBefore, gainedXp, leveledUp);
    if (leveledUp) {
      status.setMood(PetMood.LEVEL_UP, "레벨이 올랐어요. 잘하고 있어요!");
    } else if (gainedXp > 0) {
      status.setMood(PetMood.HAPPY, "이번 학습으로 능력치가 올랐어요.");
    }
    if (gainedXp > 0) {
      status.getStage().celebrate(java.util.List.of());
    }
  }

  public void showCategoryProgress(Category category, int attempts, double accuracy) {
    progressCards.get(category).showProgress(attempts, accuracy);
  }

  public void showRecent(List<RecentItem> items) {
    recentItems = List.copyOf(items);
    rebuildRecent();
  }

  private void rebuildRecent() {
    List<RecentItem> items = recentItems;
    recentList.removeAll();
    recentList.setLayout(new BoxLayout(recentList, BoxLayout.Y_AXIS));
    if (items.isEmpty()) {
      JLabel empty = new JLabel("아직 푼 문제가 없어요. 학습을 시작해 보세요!");
      empty.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
      empty.setForeground(UiStyle.MUTED);
      recentList.add(empty);
    }
    for (int i = 0; i < Math.min(recentLimit, items.size()); i++) {
      if (i > 0) recentList.add(javax.swing.Box.createVerticalStrut(10));
      JPanel row = recentRow(items.get(i));
      row.setAlignmentX(LEFT_ALIGNMENT);
      row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
      recentList.add(row);
    }
    recentList.revalidate();
    recentList.repaint();
  }

  public void showWrongCounts(Map<Category, Integer> counts, int unclassified) {
    int total = unclassified;
    wrongChips.removeAll();
    for (Category category : Category.values()) {
      int count = counts.getOrDefault(category, 0);
      if (count <= 0) continue;
      total += count;
      Color color = UiStyle.categoryColor(category);
      wrongChips.add(new Badge(category.getName() + " " + count, CategoryIcon.soft(color, 0.18), color,
          new Font(Font.DIALOG, Font.BOLD, 14), 0));
    }
    if (unclassified > 0) {
      wrongChips.add(new Badge("미분류 " + unclassified, UiStyle.SUNKEN, UiStyle.MUTED,
          new Font(Font.DIALOG, Font.BOLD, 14), 0));
    }
    wrongCountLabel.setText(total == 0
        ? "<html>틀린 문제가 없어요. 아주 잘하고 있어요!</html>"
        : "<html>다시 풀어 볼 문제 <font color='#F04438'><b>" + total + "개</b></font>가 남아 있어요.</html>");
    reviewButton.setVisible(total > 0);
    wrongChips.revalidate();
    wrongChips.repaint();
  }

  public CategoryProgressCard getProgressCard(Category category) {
    return progressCards.get(category);
  }

  public String getGrowthHintText() {
    return growthHintLabel.getText();
  }

  public int getRecentRowCount() {
    return (int) java.util.Arrays.stream(recentList.getComponents())
        .filter(component -> component instanceof JPanel).count();
  }

  public String getWrongCountText() {
    return wrongCountLabel.getText();
  }

  public void setGreeting(String nickname) {
    greetingLabel.setText(nickname == null || nickname.isBlank()
        ? "안녕하세요!" : nickname + " 님, 안녕하세요!");
  }

  public void showSummary(double accuracy, int solved) {
    status.showSummary(accuracy, solved);
  }

  public PetStatusPanel getStatusPanel() {
    return status;
  }

  public String getHeadlineText() {
    return headlineLabel.getText();
  }

  static String subjectParticle(String name) {
    if (name == null || name.isEmpty()) {
      return "가";
    }
    char last = name.charAt(name.length() - 1);
    if (last < 0xAC00 || last > 0xD7A3) {
      return "가";
    }
    return (last - 0xAC00) % 28 == 0 ? "가" : "이";
  }

  private static String escape(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private JPanel createContent() {
    JPanel content = new JPanel(new GridBagLayout());
    content.setOpaque(false);
    content.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTH;

    c.gridy = 0;
    content.add(createBanner(), c);

    c.gridy = 1;
    c.insets = new Insets(18, 0, 2, 0);
    content.add(createSectionHeader(), c);

    c.gridy = 2;
    c.insets = new Insets(0, 0, 10, 0);
    growthHintLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
    growthHintLabel.setForeground(UiStyle.BRAND);
    content.add(growthHintLabel, c);

    c.gridy = 3;
    c.insets = new Insets(0, 0, 0, 0);
    content.add(createProgressGrid(), c);

    c.gridy = 4;
    c.weighty = 1;
    c.fill = GridBagConstraints.BOTH;
    c.insets = new Insets(16, 0, 0, 0);
    content.add(createBottomRow(), c);
    setGreeting(null);
    return content;
  }

  private JPanel createSectionHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    JLabel title = new JLabel("분야별 진행도");
    title.setFont(SECTION_FONT);
    title.setForeground(UiStyle.TEXT);
    JLabel note = new JLabel("정답률 기준 · 카드를 누르면 바로 학습");
    note.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
    note.setForeground(UiStyle.MUTED);
    header.add(title, BorderLayout.WEST);
    header.add(note, BorderLayout.EAST);
    return header;
  }

  private JPanel createProgressGrid() {
    JPanel grid = new JPanel(new GridLayout(1, Category.values().length, 16, 0));
    grid.setOpaque(false);
    for (Category category : Category.values()) {
      CategoryProgressCard card = new CategoryProgressCard(category);
      card.setOnClick(() -> onCategory.accept(category.getCategoryId()));
      progressCards.put(category, card);
      grid.add(card);
    }
    return grid;
  }

  private JPanel createBottomRow() {
    JPanel row = new JPanel(new GridBagLayout());
    row.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    c.weighty = 1;

    c.gridx = 0;
    c.weightx = 1.4;
    c.insets = new Insets(0, 0, 0, 10);
    row.add(createRecentCard(), c);

    c.gridx = 1;
    c.weightx = 1;
    c.insets = new Insets(0, 10, 0, 0);
    row.add(createReviewCard(), c);
    return row;
  }

  private JPanel createRecentCard() {
    RoundCard card = new RoundCard(new BorderLayout(0, 14));
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    JLabel title = new JLabel("최근 학습");
    title.setFont(SECTION_FONT);
    title.setForeground(UiStyle.TEXT);
    JButton allRecords = linkButton("전체 기록 →");
    allRecords.addActionListener(e -> onHistory.run());
    header.add(title, BorderLayout.WEST);
    header.add(allRecords, BorderLayout.EAST);
    card.add(header, BorderLayout.NORTH);

    recentList.setOpaque(false);
    JPanel listHolder = new JPanel(new BorderLayout());
    listHolder.setOpaque(false);
    listHolder.add(recentList, BorderLayout.NORTH);
    listHolder.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        int fit = Math.max(RECENT_MIN, Math.min(RECENT_MAX, listHolder.getHeight() / RECENT_ROW_HEIGHT));
        if (fit != recentLimit) {
          recentLimit = fit;
          rebuildRecent();
        }
      }
    });
    card.add(listHolder, BorderLayout.CENTER);
    showRecent(List.of());
    return card;
  }

  private JPanel createReviewCard() {
    RoundCard card = new RoundCard(new BorderLayout(0, 14));
    JLabel title = new JLabel("복습이 필요해요");
    title.setFont(SECTION_FONT);
    title.setForeground(UiStyle.TEXT);
    card.add(title, BorderLayout.NORTH);

    JPanel body = new JPanel(new BorderLayout(0, 12));
    body.setOpaque(false);
    wrongCountLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
    wrongCountLabel.setForeground(UiStyle.MUTED);
    body.add(wrongCountLabel, BorderLayout.NORTH);
    wrongChips.setOpaque(false);
    body.add(wrongChips, BorderLayout.CENTER);
    card.add(body, BorderLayout.CENTER);

    reviewButton.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
    reviewButton.setForeground(Color.WHITE);
    reviewButton.addActionListener(e -> onWrongNote.run());
    JPanel buttonHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    buttonHolder.setOpaque(false);
    buttonHolder.add(reviewButton);
    card.add(buttonHolder, BorderLayout.SOUTH);
    showWrongCounts(Map.of(), 0);
    return card;
  }

  private JPanel recentRow(RecentItem item) {
    JPanel row = new JPanel(new BorderLayout(14, 0)) {
      @Override
      protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UiStyle.SUNKEN);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g.dispose();
      }
    };
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 16));

    Badge mark = new Badge(item.correct() ? "O" : "X",
        item.correct() ? UiStyle.CORRECT : UiStyle.WRONG, Color.WHITE,
        new Font(Font.DIALOG, Font.BOLD, 16), 30);
    JLabel category = new JLabel(item.category() == null ? "기타" : item.category().getName());
    category.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
    category.setForeground(item.category() == null ? UiStyle.MUTED : UiStyle.categoryColor(item.category()));
    category.setPreferredSize(new Dimension(64, category.getPreferredSize().height));
    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
    left.setOpaque(false);
    left.add(mark);
    left.add(category);

    JLabel prompt = new JLabel(item.prompt());
    prompt.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
    prompt.setForeground(UiStyle.TEXT);
    prompt.setToolTipText(item.prompt());

    JLabel reward = new JLabel(item.earnedXp() > 0 ? "+" + item.earnedXp() + " EXP" : "—");
    reward.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
    reward.setForeground(UiStyle.EXP_DARK);

    row.add(left, BorderLayout.WEST);
    row.add(prompt, BorderLayout.CENTER);
    row.add(reward, BorderLayout.EAST);
    return row;
  }

  private static JButton linkButton(String text) {
    JButton button = new JButton(text);
    button.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
    button.setForeground(new Color(0xD96F00));
    button.setContentAreaFilled(false);
    button.setBorderPainted(false);
    button.setFocusPainted(false);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    return button;
  }

  private void updateRecommendation(int str, int intel, int foc, int wis) {
    Map<String, Integer> stats = new LinkedHashMap<>();
    stats.put("INT", intel);
    stats.put("STR", str);
    stats.put("FOC", foc);
    stats.put("WIS", wis);
    int min = Collections.min(stats.values());
    int max = Collections.max(stats.values());
    String weakest = null;
    for (Map.Entry<String, Integer> entry : stats.entrySet()) {
      if (entry.getValue() == min) {
        weakest = entry.getKey();
        break;
      }
    }
    boolean allSame = min == max;
    List<String> names = new ArrayList<>();
    for (CategoryProgressCard card : progressCards.values()) {
      boolean recommended = !allSame && card.getCategory().getStatType().equals(weakest);
      card.setRecommended(recommended);
      if (recommended) names.add(card.getCategory().getName());
    }
    growthHintLabel.setText(allSame
        ? "능력치가 모두 같아요. 마음에 드는 분야부터 키워 보세요!"
        : "가장 약한 능력치는 " + UiStyle.statLabel(weakest) + "(" + weakest + ") " + min + "점! "
            + String.join("·", names) + " 문제를 맞히면 올라가요.");
  }

  private JPanel createBanner() {
    JPanel banner = new JPanel(new BorderLayout(24, 0)) {
      @Override
      protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UiStyle.TEXT);
        g.fillRoundRect(0, 4, getWidth(), getHeight() - 4, 22, 22);
        g.setColor(UiStyle.INK_PANEL);
        g.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 22, 22);
        g.setColor(new Color(255, 255, 255, 14));
        g.fillOval(getWidth() - getHeight() * 3 / 2, -getHeight() / 3,
            getHeight() * 2, getHeight() * 2);
        g.dispose();
      }
    };
    banner.setOpaque(false);
    banner.setBorder(BorderFactory.createEmptyBorder(22, 32, 26, 32));

    greetingLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
    greetingLabel.setForeground(UiStyle.EXP);
    headlineLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
    headlineLabel.setForeground(Color.WHITE);
    rewardLabel.setFont(UiStyle.BODY_FONT);
    rewardLabel.setForeground(new Color(0xD8D3E6));

    JPanel texts = new JPanel(new GridBagLayout());
    texts.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.anchor = GridBagConstraints.WEST;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    texts.add(greetingLabel, c);
    c.insets = new Insets(6, 0, 8, 0);
    texts.add(headlineLabel, c);
    c.insets = new Insets(0, 0, 0, 0);
    texts.add(rewardLabel, c);
    banner.add(texts, BorderLayout.CENTER);

    ImageButton learnButton = new ImageButton("학습 시작하기 →");
    learnButton.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
    learnButton.setForeground(Color.WHITE);
    learnButton.setPreferredSize(new Dimension(200, 62));
    learnButton.addActionListener(e -> onLearn.run());
    JPanel buttonHolder = new JPanel(new GridBagLayout());
    buttonHolder.setOpaque(false);
    buttonHolder.add(learnButton);
    banner.add(buttonHolder, BorderLayout.EAST);
    return banner;
  }
}
