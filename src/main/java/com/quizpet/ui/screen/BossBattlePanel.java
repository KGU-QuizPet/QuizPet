package main.java.com.quizpet.ui.screen;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.service.BattleService;
import main.java.com.quizpet.ui.UiStyle;

// 보스 이미지·배경·고정 펫 이미지와 전투 연출을 제공하는 자동 전투 화면.
public final class BossBattlePanel extends JPanel {
  private static final int NORMAL_TURN_DELAY_MS = 1000;
  private static final int FAST_TURN_DELAY_MS = 500;
  private static final int EFFECT_TICK_MS = 30;

  private static final Color PET_HP_HEALTHY = new Color(0x2BB673);
  private static final Color BOSS_HP_HEALTHY = new Color(0xE05A63);
  private static final Color HP_WARN = new Color(0xF59E0B);
  private static final Color HP_DANGER = new Color(0xEF4444);

  private final BattleService service = new BattleService();
  private final JLabel bossBadge = createBossBadge();
  private final JLabel bossImage = new JLabel("", SwingConstants.CENTER);
  private final JLabel petImage = new JLabel("", SwingConstants.CENTER);
  private final JLabel bossName = UiStyle.label("보스", UiStyle.SUBTITLE_FONT.deriveFont(16f));
  private final JLabel petName = UiStyle.label("펫", UiStyle.SUBTITLE_FONT.deriveFont(16f));
  private final JLabel message = UiStyle.label("전투 시작을 누르면 자동으로 진행됩니다.", UiStyle.BODY_FONT);
  private final JLabel turn = UiStyle.label("전투 준비 · 내 펫 선공", UiStyle.SUBTITLE_FONT.deriveFont(15f));
  private final JTextArea battleLog = new JTextArea(3, 28);
  private final HpBar bossHp = new HpBar(BOSS_HP_HEALTHY);
  private final HpBar petHp = new HpBar(PET_HP_HEALTHY);
  private final StyledButton start = new StyledButton("전투 시작", UiStyle.BRAND, Color.WHITE, new Color(0xE07600));
  private final StyledButton speedToggle = new StyledButton("배속 1x", Color.WHITE, UiStyle.TEXT, new Color(0xF3E6D0));
  private final StyledButton exit = new StyledButton("나가기", Color.WHITE, UiStyle.TEXT, new Color(0xF3E6D0));
  private final Arena arena = new Arena();
  private final Timer timer = new Timer(NORMAL_TURN_DELAY_MS, event -> nextTurn());
  private final Timer effectTimer = new Timer(EFFECT_TICK_MS, event -> tickEffects());
  private BattleSession battle;
  private Pet currentPet;
  private int selectedBossIndex;
  private boolean fastMode;
  private Runnable onExit = () -> { };
  private IntSupplier bossIndexPicker = () -> (int) (Math.random() * BattleService.BOSSES.size());

  public BossBattlePanel() {
    setLayout(new BorderLayout(0, 12));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
    add(buildHeader(), BorderLayout.NORTH);
    add(arena, BorderLayout.CENTER);
    add(buildBottom(), BorderLayout.SOUTH);

    start.setEnabled(false);
    start.setPreferredSize(new Dimension(140, 40));
    speedToggle.setPreferredSize(new Dimension(108, 40));
    exit.setPreferredSize(new Dimension(112, 40));

    start.addActionListener(event -> startBattle());
    speedToggle.addActionListener(event -> toggleSpeed());
    exit.addActionListener(event -> { stopBattle(); onExit.run(); });
  }

  public void setOnExit(Runnable action) { onExit = Objects.requireNonNull(action, "onExit는 필수입니다."); }

  public void showBattle(Pet pet) {
    stopBattle();
    battle = null;
    currentPet = Objects.requireNonNull(pet, "pet은 필수입니다.");
    // 전투에 들어갈 때마다 보스와 배경을 함께 랜덤 선택.
    selectRandomBoss();
  }

  private void selectRandomBoss() {
    selectedBossIndex = Math.floorMod(bossIndexPicker.getAsInt(), BattleService.BOSSES.size());
    updatePreview();
  }

  private void toggleSpeed() {
    fastMode = !fastMode;
    int delay = currentTurnDelay();
    timer.setDelay(delay);
    timer.setInitialDelay(delay);
    if (fastMode) {
      speedToggle.setText("배속 2x");
      speedToggle.setPalette(UiStyle.TEXT, Color.WHITE, new Color(0x3D3558));
    } else {
      speedToggle.setText("배속 1x");
      speedToggle.setPalette(Color.WHITE, UiStyle.TEXT, new Color(0xF3E6D0));
    }
    if (battle == null) {
      message.setText(readyMessage());
    }
  }

  private int currentTurnDelay() {
    return fastMode ? FAST_TURN_DELAY_MS : NORMAL_TURN_DELAY_MS;
  }

  private String readyMessage() {
    return fastMode
        ? "전투 시작을 누르면 0.5초마다 빠르게 진행됩니다."
        : "전투 시작을 누르면 1초마다 자동으로 진행됩니다.";
  }

  private JPanel buildHeader() {
    JPanel header = new JPanel(new BorderLayout(12, 0));
    header.setOpaque(false);

    JLabel title = UiStyle.label("보스 잡기", UiStyle.TITLE_FONT);
    title.setForeground(UiStyle.TEXT);
    header.add(title, BorderLayout.CENTER);

    JPanel leftSpacer = new JPanel();
    leftSpacer.setOpaque(false);
    leftSpacer.setPreferredSize(new Dimension(210, 32));
    header.add(leftSpacer, BorderLayout.WEST);

    JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
    badgeWrap.setOpaque(false);
    badgeWrap.setPreferredSize(new Dimension(210, 36));
    badgeWrap.add(bossBadge);
    header.add(badgeWrap, BorderLayout.EAST);
    return header;
  }

  private JPanel buildBottom() {
    JPanel bottom = new JPanel(new BorderLayout(0, 8));
    bottom.setOpaque(false);

    JPanel statusBox = new JPanel(new BorderLayout(0, 2));
    statusBox.setOpaque(false);
    turn.setForeground(UiStyle.TEXT);
    message.setForeground(UiStyle.MUTED);
    statusBox.add(turn, BorderLayout.NORTH);
    statusBox.add(message, BorderLayout.CENTER);
    bottom.add(statusBox, BorderLayout.NORTH);

    battleLog.setEditable(false);
    battleLog.setFocusable(false);
    battleLog.setLineWrap(true);
    battleLog.setWrapStyleWord(true);
    battleLog.setFont(UiStyle.BODY_FONT.deriveFont(13f));
    battleLog.setForeground(UiStyle.TEXT);
    battleLog.setBackground(Color.WHITE);
    battleLog.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

    JScrollPane logScroll = new JScrollPane(battleLog);
    logScroll.setBorder(BorderFactory.createLineBorder(UiStyle.BORDER, 1, true));
    logScroll.setPreferredSize(new Dimension(0, 74));
    bottom.add(logScroll, BorderLayout.CENTER);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 2));
    buttons.setOpaque(false);
    buttons.add(start);
    buttons.add(speedToggle);
    buttons.add(exit);
    bottom.add(buttons, BorderLayout.SOUTH);
    return bottom;
  }

  private void updatePreview() {
    if (currentPet == null) return;
    Boss boss = selectedBoss();
    arena.resetEffects();
    arena.background = load(boss.backgroundPath());
    bossBadge.setText("등장 보스 · " + boss.name() + " " + difficultyStars(selectedBossIndex));
    bossImage.setIcon(icon(boss.imagePath(), 330, 285));
    // 현재 PetAppearance 연동 전 임시로 duck_idle.png를 고정 사용.
    petImage.setIcon(icon("images/pets/duck_idle.png", 215, 215));
    bossName.setText(boss.name() + "  (ATK " + boss.attack() + ")");
    int petAttack = service.calculateAttack(currentPet);
    petName.setText(currentPet.getName() + "  (Lv." + currentPet.getLevel() + " · ATK " + petAttack + ")");
    bossHp.reset(boss.maxHp());
    int maxHp = service.calculateMaxHp(currentPet);
    petHp.reset(maxHp);
    start.setText("전투 시작");
    start.setEnabled(true);
    turn.setText("전투 준비 · 내 펫 선공");
    message.setText(readyMessage());
    battleLog.setText("[준비] " + currentPet.getName() + "이(가) " + boss.name() + "과 마주했습니다.\n");
    arena.repaint();
  }

  private void startBattle() {
    if (currentPet == null || battle != null && battle.isRunning()) return;
    if (battle != null) selectRandomBoss();
    battle = service.createBattle(currentPet, selectedBossIndex);
    battle.start();
    arena.resetEffects();
    setBossImageState("idle");
    appendLog("[시작] " + battle.getBoss().name() + "과 전투를 시작했습니다.");
    arena.triggerFlash(new Color(0xF59E0B));
    start.setEnabled(false);
    int delay = currentTurnDelay();
    timer.setDelay(delay);
    timer.setInitialDelay(delay);
    timer.restart();
    ensureEffectTimerRunning();
    updateBattleView();
  }

  private void nextTurn() {
    if (battle == null || !battle.isRunning()) { timer.stop(); return; }
    boolean playerAttacked = battle.getState() == BattleSession.State.PLAYER_TURN;
    int damage = playerAttacked ? battle.getAttack() : battle.getBoss().attack();
    battle.advanceTurn();
    if (battle.getState() == BattleSession.State.WON) setBossImageState("defeated");
    else if (playerAttacked) setBossImageState("hurt");
    else setBossImageState("attack");

    appendLog("[" + battle.getTurn() + "턴] " + battle.getMessage());
    arena.triggerAttackEffect(playerAttacked, damage);
    arena.triggerFlash(battle.getState() == BattleSession.State.WON
        ? new Color(0x2BB673) : playerAttacked ? new Color(0xF59E0B) : new Color(0xEF4444));
    updateBattleView();
    ensureEffectTimerRunning();
    if (!battle.isRunning()) timer.stop();
  }

  private void updateBattleView() {
    bossHp.setTargetHp(battle.getBossHp(), battle.getBoss().maxHp());
    petHp.setTargetHp(battle.getPlayerHp(), battle.getMaxPlayerHp());
    turn.setText(formatTurnStatus(battle));
    message.setText(battle.getMessage());
    start.setText(battle.getState() == BattleSession.State.WON || battle.getState() == BattleSession.State.LOST
        ? "재도전" : "전투 중");
    start.setEnabled(!battle.isRunning());
    arena.repaint();
  }

  private static String formatTurnStatus(BattleSession session) {
    return switch (session.getState()) {
      case READY -> "전투 준비 · 내 펫 선공";
      case PLAYER_TURN -> session.getTurn() == 0
          ? "전투 시작 · 내 펫 선공"
          : session.getTurn() + "턴 · 내 펫 공격 차례";
      case BOSS_TURN -> session.getTurn() + "턴 · 보스 반격 차례";
      case WON -> session.getTurn() + "턴 · 승리!";
      case LOST -> session.getTurn() + "턴 · 패배";
      case ABANDONED -> "전투 종료";
    };
  }

  private static String difficultyStars(int index) {
    return switch (index) {
      case 0 -> "★☆☆";
      case 1 -> "★★☆";
      default -> "★★★";
    };
  }

  private void setBossImageState(String state) {
    if (battle == null) return;
    String idle = battle.getBoss().imagePath();
    String statePath = idle.replace("_idle.png", "_" + state + ".png");
    bossImage.setIcon(icon(statePath, 330, 285));
    arena.revalidate();
    arena.repaint();
  }

  private void appendLog(String line) {
    battleLog.append(line + "\n");
    battleLog.setCaretPosition(battleLog.getDocument().getLength());
  }

  private void ensureEffectTimerRunning() {
    if (!effectTimer.isRunning()) effectTimer.start();
  }

  private void tickEffects() {
    boolean hpAnimating = bossHp.stepAnimation() | petHp.stepAnimation();
    boolean arenaAnimating = arena.stepEffects();
    if (!hpAnimating && !arenaAnimating) {
      effectTimer.stop();
    }
  }

  public void stopBattle() {
    timer.stop();
    effectTimer.stop();
    if (battle != null) battle.abandon();
  }

  private Boss selectedBoss() { return BattleService.BOSSES.get(selectedBossIndex); }

  private static JLabel createBossBadge() {
    JLabel badge = new JLabel("등장 보스 · 준비 중", SwingConstants.CENTER) {
      @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UiStyle.TEXT);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.dispose();
        super.paintComponent(g);
      }
    };
    badge.setOpaque(false);
    badge.setForeground(Color.WHITE);
    badge.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f));
    badge.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
    return badge;
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

  // 부드러운 감소 애니메이션이 적용된 둥근 HP 바.
  private static final class HpBar extends JPanel {
    private final Color healthyColor;
    private int maxHp = 100;
    private int targetHp = 100;
    private double displayedHp = 100;

    HpBar(Color healthyColor) {
      this.healthyColor = healthyColor;
      setOpaque(false);
      setPreferredSize(new Dimension(300, 28));
    }

    void reset(int max) {
      maxHp = Math.max(1, max);
      targetHp = maxHp;
      displayedHp = maxHp;
      repaint();
    }

    void setTargetHp(int current, int max) {
      maxHp = Math.max(1, max);
      targetHp = Math.max(0, Math.min(current, maxHp));
      repaint();
    }

    int getMaximum() { return maxHp; }
    int getValue() { return targetHp; }

    boolean stepAnimation() {
      double diff = targetHp - displayedHp;
      if (Math.abs(diff) < 0.5) {
        if (displayedHp != targetHp) {
          displayedHp = targetHp;
          repaint();
        }
        return false;
      }
      displayedHp += diff * 0.28;
      repaint();
      return true;
    }

    @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int w = getWidth();
      int h = getHeight();
      int arc = 12;

      g2.setColor(new Color(15, 23, 42, 200));
      g2.fillRoundRect(0, 0, w, h, arc, arc);

      double ratio = Math.max(0.0, Math.min(1.0, displayedHp / maxHp));
      int fillW = (int) Math.round(w * ratio);
      if (fillW > 0) {
        Color fill = ratio <= 0.3 ? HP_DANGER : ratio <= 0.6 ? HP_WARN : healthyColor;
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, fillW, h, arc, arc);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.fillRoundRect(2, 2, Math.max(0, fillW - 4), Math.max(1, (h - 4) / 2), arc - 2, arc - 2);
      }

      g2.setColor(new Color(255, 255, 255, 70));
      g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

      String text = "HP " + targetHp + " / " + maxHp;
      g2.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f));
      FontMetrics fm = g2.getFontMetrics();
      int tx = (w - fm.stringWidth(text)) / 2;
      int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
      g2.setColor(new Color(0, 0, 0, 150));
      g2.drawString(text, tx + 1, ty + 1);
      g2.setColor(Color.WHITE);
      g2.drawString(text, tx, ty);
      g2.dispose();
    }
  }

  // QuizPet 테마에 맞춘 둥근 버튼.
  private static final class StyledButton extends JButton {
    private Color bg;
    private Color fg;
    private Color hoverBg;

    StyledButton(String text, Color bg, Color fg, Color hoverBg) {
      super(text);
      this.bg = bg;
      this.fg = fg;
      this.hoverBg = hoverBg;
      setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 14f));
      setForeground(fg);
      setFocusPainted(false);
      setContentAreaFilled(false);
      setBorderPainted(false);
      setOpaque(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void setPalette(Color bg, Color fg, Color hoverBg) {
      this.bg = bg;
      this.fg = fg;
      this.hoverBg = hoverBg;
      setForeground(fg);
      repaint();
    }

    @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color fill;
      if (!isEnabled()) fill = new Color(0xD8D3C8);
      else if (getModel().isRollover() || getModel().isPressed()) fill = hoverBg;
      else fill = bg;
      g2.setColor(fill);
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
      if (bg.equals(Color.WHITE) && isEnabled()) {
        g2.setColor(UiStyle.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
      }
      g2.dispose();
      super.paintComponent(g);
    }
  }

  private static final class FloatingDamage {
    final String text;
    final int baseX;
    final int baseY;
    final Color color;
    float progress;

    FloatingDamage(String text, int baseX, int baseY, Color color) {
      this.text = text;
      this.baseX = baseX;
      this.baseY = baseY;
      this.color = color;
    }
  }

  private final class Arena extends JPanel {
    private Image background;
    private int petOffsetX;
    private int bossOffsetX;
    private int animTicks;
    private boolean lastPlayerAttacked;
    private Color flashColor;
    private float flashAlpha;
    private final List<FloatingDamage> damages = new ArrayList<>();

    private int petGroupX;
    private int bossGroupX;
    private int groupY;

    Arena() {
      setLayout(null);
      setOpaque(false);
      bossName.setForeground(Color.WHITE);
      petName.setForeground(Color.WHITE);
      add(bossName);
      add(bossImage);
      add(bossHp);
      add(petName);
      add(petImage);
      add(petHp);
      setPreferredSize(new Dimension(480, 380));
    }

    void resetEffects() {
      petOffsetX = 0;
      bossOffsetX = 0;
      animTicks = 0;
      flashColor = null;
      flashAlpha = 0f;
      damages.clear();
      revalidate();
      repaint();
    }

    void triggerFlash(Color color) {
      flashColor = color;
      flashAlpha = 0.85f;
      repaint();
    }

    void triggerAttackEffect(boolean playerAttacked, int damage) {
      lastPlayerAttacked = playerAttacked;
      animTicks = 8;
      int dmgX = playerAttacked ? bossGroupX + 170 : petGroupX + 155;
      int dmgY = groupY + 110;
      Color dmgColor = playerAttacked ? new Color(0xFDE047) : new Color(0xFF6B6B);
      damages.add(new FloatingDamage("-" + damage, dmgX, dmgY, dmgColor));
      applyCharacterBounds();
      repaint();
    }

    boolean stepEffects() {
      boolean active = false;
      if (animTicks > 0) {
        animTicks--;
        double t = animTicks / 8.0;
        int lunge = (int) Math.round(Math.sin(t * Math.PI) * 32);
        int shake = animTicks == 0 ? 0 : ((animTicks % 2 == 0) ? 9 : -9);
        if (lastPlayerAttacked) {
          petOffsetX = lunge;
          bossOffsetX = shake;
        } else {
          bossOffsetX = -lunge;
          petOffsetX = shake;
        }
        applyCharacterBounds();
        active = true;
      } else if (petOffsetX != 0 || bossOffsetX != 0) {
        petOffsetX = 0;
        bossOffsetX = 0;
        applyCharacterBounds();
      }

      if (flashAlpha > 0f) {
        flashAlpha = Math.max(0f, flashAlpha - 0.12f);
        active = true;
      }

      if (!damages.isEmpty()) {
        Iterator<FloatingDamage> it = damages.iterator();
        while (it.hasNext()) {
          FloatingDamage d = it.next();
          d.progress += 0.055f;
          if (d.progress >= 1.0f) it.remove();
        }
        active = true;
      }

      if (active) repaint();
      return active;
    }

    @Override public void doLayout() {
      int w = getWidth();
      int h = getHeight();
      groupY = Math.max(18, (h - 360) / 2);
      petGroupX = Math.max(20, (int) (w * 0.28) - 160);
      bossGroupX = Math.min(w - 380, (int) (w * 0.72) - 180);

      petName.setBounds(petGroupX + 12, groupY + 10, 296, 26);
      petHp.setBounds(petGroupX + 12, groupY + 42, 296, 28);
      bossName.setBounds(bossGroupX + 12, groupY + 10, 336, 26);
      bossHp.setBounds(bossGroupX + 12, groupY + 42, 336, 28);
      applyCharacterBounds();
    }

    private void applyCharacterBounds() {
      petImage.setBounds(petGroupX + 35 + petOffsetX, groupY + 96, 250, 240);
      bossImage.setBounds(bossGroupX + 10 + bossOffsetX, groupY + 90, 340, 250);
    }

    @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      int w = getWidth();
      int h = getHeight();
      if (background != null) {
        g2.drawImage(background, 0, 0, w, h, this);
      } else {
        g2.setColor(new Color(0x263447));
        g2.fillRect(0, 0, w, h);
      }

      // 상단·하단 은은한 비네팅으로 HUD와 캐릭터 시인성 확보.
      g2.setPaint(new GradientPaint(0, 0, new Color(15, 23, 42, 130), 0, 120, new Color(15, 23, 42, 0)));
      g2.fillRect(0, 0, w, 120);
      g2.setPaint(new GradientPaint(0, h - 100, new Color(15, 23, 42, 0), 0, h, new Color(15, 23, 42, 125)));
      g2.fillRect(0, h - 100, w, 100);

      // 펫·보스 HUD 반투명 다크 카드.
      paintHudCard(g2, petGroupX, groupY, 320, 82);
      paintHudCard(g2, bossGroupX, groupY, 360, 82);

      // 캐릭터 바닥 그림자.
      g2.setColor(new Color(0, 0, 0, 95));
      g2.fillOval(petGroupX + 85 + petOffsetX, groupY + 312, 150, 24);
      g2.fillOval(bossGroupX + 80 + bossOffsetX, groupY + 318, 200, 28);

      // 중앙 VS 원형 배지.
      int vsSize = 56;
      int vsX = (w - vsSize) / 2;
      int vsY = groupY + 175;
      g2.setColor(new Color(15, 23, 42, 195));
      g2.fillOval(vsX, vsY, vsSize, vsSize);
      g2.setStroke(new BasicStroke(2.5f));
      g2.setColor(UiStyle.BRAND);
      g2.drawOval(vsX + 1, vsY + 1, vsSize - 2, vsSize - 2);
      g2.setFont(UiStyle.SUBTITLE_FONT.deriveFont(Font.BOLD, 20f));
      FontMetrics vsFm = g2.getFontMetrics();
      int vsTx = vsX + (vsSize - vsFm.stringWidth("VS")) / 2;
      int vsTy = vsY + (vsSize - vsFm.getHeight()) / 2 + vsFm.getAscent();
      g2.setColor(Color.WHITE);
      g2.drawString("VS", vsTx, vsTy);

      g2.dispose();
    }

    @Override protected void paintChildren(Graphics g) {
      super.paintChildren(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // 플로팅 데미지 텍스트.
      for (FloatingDamage d : damages) {
        int alpha = Math.max(0, Math.min(255, (int) ((1.0f - d.progress) * 255)));
        int y = d.baseY - (int) (d.progress * 44);
        g2.setFont(UiStyle.TITLE_FONT.deriveFont(Font.BOLD, 26f));
        FontMetrics fm = g2.getFontMetrics();
        int x = d.baseX - fm.stringWidth(d.text) / 2;
        g2.setColor(new Color(0, 0, 0, Math.min(alpha, 200)));
        g2.drawString(d.text, x + 2, y + 2);
        g2.setColor(new Color(d.color.getRed(), d.color.getGreen(), d.color.getBlue(), alpha));
        g2.drawString(d.text, x, y);
      }

      // 공격 시 외곽 플래시 효과.
      if (flashColor != null && flashAlpha > 0f) {
        int alpha = Math.max(0, Math.min(255, (int) (flashAlpha * 220)));
        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), alpha));
        g2.drawRect(3, 3, getWidth() - 6, getHeight() - 6);
      }

      // 승리/패배 결과 오버레이 배너.
      if (battle != null && (battle.getState() == BattleSession.State.WON || battle.getState() == BattleSession.State.LOST)) {
        paintResultOverlay(g2, battle.getState() == BattleSession.State.WON);
      }

      g2.dispose();
    }

    private void paintHudCard(Graphics2D g2, int x, int y, int w, int h) {
      g2.setColor(new Color(15, 23, 42, 180));
      g2.fillRoundRect(x, y, w, h, 16, 16);
      g2.setStroke(new BasicStroke(1.2f));
      g2.setColor(new Color(255, 255, 255, 65));
      g2.drawRoundRect(x, y, w - 1, h - 1, 16, 16);
    }

    private void paintResultOverlay(Graphics2D g2, boolean won) {
      int w = getWidth();
      int h = getHeight();
      g2.setColor(new Color(15, 23, 42, 115));
      g2.fillRect(0, 0, w, h);

      int cardW = 390;
      int cardH = 136;
      int cx = (w - cardW) / 2;
      int cy = (h - cardH) / 2;
      Color accent = won ? new Color(0x34D399) : new Color(0xF87171);

      g2.setColor(new Color(15, 23, 42, 230));
      g2.fillRoundRect(cx, cy, cardW, cardH, 20, 20);
      g2.setStroke(new BasicStroke(2.5f));
      g2.setColor(accent);
      g2.drawRoundRect(cx + 1, cy + 1, cardW - 2, cardH - 2, 20, 20);

      String heading = won ? "승리!" : "패배...";
      g2.setFont(UiStyle.TITLE_FONT.deriveFont(Font.BOLD, 30f));
      FontMetrics hFm = g2.getFontMetrics();
      g2.setColor(accent);
      g2.drawString(heading, cx + (cardW - hFm.stringWidth(heading)) / 2, cy + 48);

      String summary = won
          ? battle.getBoss().name() + " 격파 · 총 " + battle.getTurn() + "턴 · 남은 HP " + battle.getPlayerHp() + "/" + battle.getMaxPlayerHp()
          : battle.getBoss().name() + "에게 패배 · 총 " + battle.getTurn() + "턴";
      g2.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 14f));
      FontMetrics sFm = g2.getFontMetrics();
      g2.setColor(Color.WHITE);
      g2.drawString(summary, cx + (cardW - sFm.stringWidth(summary)) / 2, cy + 82);

      String hint = "아래 [재도전] 버튼으로 새 보스에 도전할 수 있습니다.";
      g2.setFont(UiStyle.BODY_FONT.deriveFont(Font.PLAIN, 12f));
      FontMetrics hintFm = g2.getFontMetrics();
      g2.setColor(new Color(203, 213, 225));
      g2.drawString(hint, cx + (cardW - hintFm.stringWidth(hint)) / 2, cy + 110);
    }
  }
}
