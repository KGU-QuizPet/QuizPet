package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.RenderingHints;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.service.BattleService;
import main.java.com.quizpet.ui.UiStyle;

/** 보스 이미지·배경·고정 펫 이미지를 사용한 자동 전투 화면입니다. */
public final class BossBattlePanel extends JPanel {
  private final BattleService service = new BattleService();
  private final JComboBox<BossOption> bossSelector = new JComboBox<>();
  private final JLabel bossImage = new JLabel();
  private final JLabel petImage = new JLabel();
  private final JLabel bossName = UiStyle.label("보스", UiStyle.SUBTITLE_FONT);
  private final JLabel petName = UiStyle.label("펫", UiStyle.SUBTITLE_FONT);
  private final JLabel versus = UiStyle.label("VS", UiStyle.TITLE_FONT);
  private final JLabel message = UiStyle.label("전투 시작을 누르면 자동으로 진행됩니다.", UiStyle.BODY_FONT);
  private final JLabel turn = UiStyle.label("전투 준비", UiStyle.BODY_FONT);
  private final JTextArea battleLog = new JTextArea(3, 28);
  private final JProgressBar bossHp = hpBar(new Color(0xD56A72));
  private final JProgressBar petHp = hpBar(new Color(0x70A890));
  private final JButton start = UiStyle.button("전투 시작");
  private final JButton exit = UiStyle.button("나가기");
  private final Arena arena = new Arena();
  private final Timer timer = new Timer(1000, event -> nextTurn());
  private BattleSession battle;
  private Pet currentPet;
  private Runnable onExit = () -> { };

  public BossBattlePanel() {
    setLayout(new BorderLayout(0, 10));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
    add(UiStyle.label("보스 잡기", UiStyle.TITLE_FONT), BorderLayout.NORTH);
    for (int i = 0; i < BattleService.BOSSES.size(); i++) bossSelector.addItem(new BossOption(i, BattleService.BOSSES.get(i)));
    bossSelector.setEnabled(false);
    bossSelector.addActionListener(event -> { if (battle == null || !battle.isRunning()) updatePreview(); });
    add(buildTop(), BorderLayout.CENTER);
    add(buildBottom(), BorderLayout.SOUTH);
    start.setEnabled(false);
    start.addActionListener(event -> startBattle());
    exit.addActionListener(event -> { stopBattle(); onExit.run(); });
  }

  public void setOnExit(Runnable action) { onExit = Objects.requireNonNull(action, "onExit는 필수입니다."); }

  public void showBattle(Pet pet) {
    stopBattle();
    currentPet = Objects.requireNonNull(pet, "pet은 필수입니다.");
    // 전투에 들어갈 때마다 보스와 배경을 함께 랜덤 선택합니다.
    bossSelector.setSelectedIndex((int) (Math.random() * BattleService.BOSSES.size()));
    updatePreview();
  }

  private JPanel buildTop() {
    JPanel root = new JPanel(new BorderLayout(0, 8));
    root.setOpaque(false);
    JPanel selector = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    selector.setOpaque(false);
    selector.add(UiStyle.label("보스 선택", UiStyle.BODY_FONT));
    selector.add(bossSelector);
    root.add(selector, BorderLayout.NORTH);
    root.add(arena, BorderLayout.CENTER);
    return root;
  }

  private JPanel buildBottom() {
    JPanel bottom = new JPanel(new BorderLayout(0, 6));
    bottom.setOpaque(false);
    bottom.add(turn, BorderLayout.NORTH);
    JPanel messages = new JPanel(new BorderLayout(0, 4));
    messages.setOpaque(false);
    messages.add(message, BorderLayout.NORTH);
    battleLog.setEditable(false);
    battleLog.setFocusable(false);
    battleLog.setLineWrap(true);
    battleLog.setWrapStyleWord(true);
    battleLog.setFont(UiStyle.BODY_FONT.deriveFont(12f));
    battleLog.setForeground(new Color(0x334155));
    battleLog.setBackground(new Color(255, 255, 255, 210));
    battleLog.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0xDADCE3)),
        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    messages.add(new JScrollPane(battleLog), BorderLayout.CENTER);
    bottom.add(messages, BorderLayout.CENTER);
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
    buttons.setOpaque(false);
    buttons.add(start);
    buttons.add(exit);
    bottom.add(buttons, BorderLayout.SOUTH);
    return bottom;
  }

  private void updatePreview() {
    if (currentPet == null) return;
    Boss boss = selectedBoss();
    arena.background = load(boss.backgroundPath());
    bossImage.setIcon(icon(boss.imagePath(), 340, 300));
    // 현재 PetAppearance 연동 전 임시로 duck_idle.png를 고정 사용합니다.
    petImage.setIcon(icon("images/pets/duck_idle.png", 220, 220));
    bossName.setText(boss.name() + "  (ATK " + boss.attack() + ")");
    petName.setText(currentPet.getName() + "  (Lv." + currentPet.getLevel() + ")");
    bossHp.setMaximum(boss.maxHp());
    bossHp.setValue(boss.maxHp());
    bossHp.setString("HP " + boss.maxHp() + " / " + boss.maxHp());
    int maxHp = 100 + (currentPet.getLevel() - 1) * 20;
    petHp.setMaximum(maxHp);
    petHp.setValue(maxHp);
    petHp.setString("HP " + maxHp + " / " + maxHp);
    start.setText("전투 시작");
    start.setEnabled(true);
    turn.setText("전투 준비 · 내 펫 선공");
    message.setText("전투 시작을 누르면 1초마다 자동으로 진행됩니다.");
    battleLog.setText("[준비] " + currentPet.getName() + "이(가) 전투에 참가했습니다.\n");
    arena.repaint();
  }

  private void startBattle() {
    if (currentPet == null || battle != null && battle.isRunning()) return;
    battle = service.createBattle(currentPet, bossSelector.getSelectedIndex());
    battle.start();
    setBossImageState("idle");
    appendLog("[시작] " + battle.getBoss().name() + "과 전투를 시작했습니다.");
    flashArena(new Color(0xF0C36A));
    start.setEnabled(false);
    timer.restart();
    updateBattleView();
  }

  private void nextTurn() {
    if (battle == null || !battle.isRunning()) { timer.stop(); return; }
    boolean playerAttacked = battle.getState() == BattleSession.State.PLAYER_TURN;
    battle.advanceTurn();
    if (battle.getState() == BattleSession.State.WON) setBossImageState("defeated");
    else if (playerAttacked) setBossImageState("hurt");
    else setBossImageState("attack");
    appendLog("[" + battle.getTurn() + "턴] " + battle.getMessage());
    flashArena(battle.getState() == BattleSession.State.WON
        ? new Color(0x74C69D) : playerAttacked ? new Color(0xF0C36A) : new Color(0xD56A72));
    updateBattleView();
    if (!battle.isRunning()) timer.stop();
  }

  private void updateBattleView() {
    bossHp.setValue(battle.getBossHp());
    bossHp.setString("HP " + battle.getBossHp() + " / " + battle.getBoss().maxHp());
    bossHp.setForeground(hpColor(battle.getBossHp(), battle.getBoss().maxHp(), new Color(0xD56A72)));
    petHp.setValue(battle.getPlayerHp());
    petHp.setString("HP " + battle.getPlayerHp() + " / " + battle.getMaxPlayerHp());
    petHp.setForeground(hpColor(battle.getPlayerHp(), battle.getMaxPlayerHp(), new Color(0x70A890)));
    turn.setText(battle.getTurn() + "턴 · " + battle.getState());
    message.setText(battle.getMessage());
    start.setText(battle.getState() == BattleSession.State.WON || battle.getState() == BattleSession.State.LOST
        ? "재도전" : "전투 중");
    start.setEnabled(!battle.isRunning());
  }

  private void setBossImageState(String state) {
    if (battle == null) return;
    String idle = battle.getBoss().imagePath();
    String statePath = idle.replace("_idle.png", "_" + state + ".png");
    bossImage.setIcon(icon(statePath, 340, 300));
    arena.revalidate();
    arena.repaint();
  }

  private void appendLog(String line) {
    battleLog.append(line + "\n");
    battleLog.setCaretPosition(battleLog.getDocument().getLength());
  }

  private static Color hpColor(int hp, int max, Color healthy) {
    double ratio = max <= 0 ? 0 : hp / (double) max;
    if (ratio <= 0.3) return new Color(0xE03131);
    if (ratio <= 0.6) return new Color(0xF08C00);
    return healthy;
  }

  private void flashArena(Color color) {
    arena.setBorder(BorderFactory.createLineBorder(color, 6));
    Timer reset = new Timer(180, event -> {
      arena.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      ((Timer) event.getSource()).stop();
      arena.revalidate();
      arena.repaint();
    });
    reset.setRepeats(false);
    reset.start();
    arena.revalidate();
    arena.repaint();
  }

  public void stopBattle() {
    timer.stop();
    if (battle != null) battle.abandon();
  }

  private Boss selectedBoss() { return ((BossOption) bossSelector.getSelectedItem()).boss; }

  private static JProgressBar hpBar(Color color) {
    JProgressBar bar = new JProgressBar();
    bar.setForeground(color);
    bar.setStringPainted(true);
    bar.setPreferredSize(new Dimension(360, 32));
    return bar;
  }

  private static Image load(String path) {
    java.net.URL url = BossBattlePanel.class.getClassLoader().getResource(path);
    if (url != null) return new ImageIcon(url).getImage();
    Path file = Path.of("src", "main", "resources").resolve(path);
    if (!Files.isRegularFile(file)) return null;
    try {
      return ImageIO.read(file.toFile());
    } catch (IOException e) {
      return null;
    }
  }

  private static ImageIcon icon(String path, int width, int height) {
    Image image = load(path);
    if (image == null) return null;
    double scale = Math.min(width / (double) image.getWidth(null), height / (double) image.getHeight(null));
    return new ImageIcon(image.getScaledInstance((int) (image.getWidth(null) * scale),
        (int) (image.getHeight(null) * scale), Image.SCALE_SMOOTH));
  }

  private static final class BossOption {
    final int index;
    final Boss boss;
    BossOption(int index, Boss boss) { this.index = index; this.boss = boss; }
    @Override public String toString() { return boss.name(); }
  }

  private final class Arena extends JPanel {
    private Image background;
    Arena() {
      setLayout(null);
      setOpaque(false);
      setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      bossName.setForeground(Color.WHITE);
      petName.setForeground(Color.WHITE);
      versus.setForeground(Color.WHITE);
      add(bossName);
      add(bossImage);
      add(bossHp);
      add(petName);
      add(petImage);
      add(petHp);
      add(versus);
      setPreferredSize(new Dimension(420, 360));
    }
    @Override public void doLayout() {
      int w = getWidth();
      int h = getHeight();
      // 좌우 대칭 전투 배치: 각자 이름 → HP → 이미지 순서
      int groupY = Math.max(18, (h - 350) / 2);
      // 화면 너비 기준 펫 중심 30%, 보스 중심 70%로 배치합니다.
      int petGroupX = Math.max(8, (int) (w * 0.30) - 150);
      int bossGroupX = Math.min(w - 368, (int) (w * 0.70) - 180);
      petName.setBounds(petGroupX, groupY, 300, 32);
      petHp.setBounds(petGroupX, groupY + 38, 300, 32);
      petImage.setBounds(petGroupX + 24, groupY + 78, 250, 250);
      bossName.setBounds(bossGroupX, groupY, 360, 32);
      bossHp.setBounds(bossGroupX, groupY + 38, 360, 32);
      bossImage.setBounds(bossGroupX + 8, groupY + 78, 340, 250);
      versus.setBounds(Math.max(0, (w - 60) / 2), groupY + 150, 60, 50);
    }
    @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D copy = (Graphics2D) g.create();
      copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      if (background != null) copy.drawImage(background, 0, 0, getWidth(), getHeight(), this);
      else { copy.setColor(new Color(0x263447)); copy.fillRect(0, 0, getWidth(), getHeight()); }
      copy.dispose();
    }
  }
}
