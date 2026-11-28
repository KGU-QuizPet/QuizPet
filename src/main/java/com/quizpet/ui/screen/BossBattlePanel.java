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
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.service.BattleService;
import main.java.com.quizpet.ui.UiStyle;

// 보스 이미지·배경·펫 외형별 표정 이미지와 전투 연출을 제공하는 자동 전투 화면.
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
  private final JLabel bossImage = new SpriteLabel("보스");
  private final JLabel petImage = new SpriteLabel("내 펫");
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
  private final JLabel turnPill = UiStyle.label("READY", UiStyle.SUBTITLE_FONT);
  private final Arena arena = new Arena();
  private final Timer timer = new Timer(NORMAL_TURN_DELAY_MS, event -> nextTurn());
  private final Timer effectTimer = new Timer(EFFECT_TICK_MS, event -> tickEffects());
  private BattleSession battle;
  private Pet currentPet;
  private PetMood currentPetMood = PetMood.IDLE;
  private int selectedBossIndex;
  private boolean fastMode;
  private Runnable onExit = () -> { };
  private IntSupplier bossIndexPicker = () -> (int) (Math.random() * BattleService.BOSSES.size());

  public BossBattlePanel() {
    setLayout(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);

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
    bossBadge.setText("등장 보스 · " + boss.name() + " " + difficultyStars(selectedBossIndex));
    bossImage.setIcon(icon(boss.imagePath(), 330, 285));
    // 현재 펫의 외형과 기본 표정(IDLE) 이미지를 적용.
    setPetMood(PetMood.IDLE);
    bossName.setText(boss.name() + "  (ATK " + boss.attack() + ")");
    int petAttack = service.calculateAttack(currentPet);
    petName.setText(currentPet.getName() + "  (Lv." + currentPet.getLevel() + " · ATK " + petAttack + ")");
    bossHp.reset(boss.maxHp());
    int maxHp = service.calculateMaxHp(currentPet);
    petHp.reset(maxHp);
    start.setText("전투 시작");
    start.setEnabled(true);
    turn.setText("전투 준비 · 내 펫 선공");
    turnPill.setText("READY");
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
    setPetMood(PetMood.IDLE);
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
    if (battle.getState() == BattleSession.State.WON) {
      setBossImageState("defeated");
      setPetMood(PetMood.HAPPY);
    } else if (battle.getState() == BattleSession.State.LOST) {
      setBossImageState("attack");
      setPetMood(PetMood.SAD);
    } else if (playerAttacked) {
      setBossImageState("hurt");
      setPetMood(PetMood.FOCUS);
    } else {
      setBossImageState("attack");
      setPetMood(PetMood.SAD);
    }

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
    turnPill.setText("TURN " + battle.getTurn());
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

  private void setPetMood(PetMood mood) {
    if (currentPet == null) return;
    currentPetMood = mood == null ? PetMood.IDLE : mood;
    String path = UiStyle.appearanceOf(currentPet.getSpriteKey()).getImagePath(currentPetMood);
    petImage.setIcon(icon(path, 236, 236));
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

      g2.setColor(UiStyle.BACKGROUND);
      g2.fillRoundRect(0, 0, w, 16, arc, arc);

      double ratio = Math.max(0.0, Math.min(1.0, displayedHp / maxHp));
      int fillW = (int) Math.round(w * ratio);
      if (fillW > 0) {
        Color fill = ratio <= 0.3 ? HP_DANGER : ratio <= 0.6 ? HP_WARN : healthyColor;
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, fillW, 16, arc, arc);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.fillRoundRect(2, 2, Math.max(0, fillW - 4), 5, arc - 2, arc - 2);
      }

      String text = "HP " + targetHp + " / " + maxHp;
      g2.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 12f));
      g2.setColor(UiStyle.MUTED);
      g2.drawString(text, 0, 34);
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

  private static final class SpriteLabel extends JLabel {
    SpriteLabel(String fallback) {
      super(fallback, SwingConstants.CENTER);
      setFont(UiStyle.SUBTITLE_FONT);
      setForeground(Color.WHITE);
    }

    @Override protected void paintComponent(Graphics g) {
      if (getIcon() instanceof ImageIcon icon) {
        double scale = Math.min(getWidth() / (double) icon.getIconWidth(), getHeight() / (double) icon.getIconHeight());
        int w = (int) (icon.getIconWidth() * scale);
        int h = (int) (icon.getIconHeight() * scale);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(icon.getImage(), (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, this);
        g2.dispose();
      } else {
        super.paintComponent(g);
      }
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
    private int petOffsetX;
    private int bossOffsetX;
    private int animTicks;
    private boolean lastPlayerAttacked;
    private Color flashColor;
    private float flashAlpha;
    private final List<FloatingDamage> damages = new ArrayList<>();

    private int petGroupX;
    private int bossGroupX;
    private int petY;
    private int bossY;
    private int hudWidth;

    Arena() {
      setLayout(null);
      setOpaque(false);
      bossName.setForeground(UiStyle.TEXT);
      petName.setForeground(UiStyle.TEXT);
      bossName.setHorizontalAlignment(SwingConstants.LEFT);
      petName.setHorizontalAlignment(SwingConstants.LEFT);
      turnPill.setForeground(Color.WHITE);
      add(turnPill);
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
      int dmgY = playerAttacked ? bossY + 90 : petY + 75;
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
      hudWidth = Math.min(380, w / 3);
      petGroupX = (int) (w * 0.10);
      bossGroupX = (int) (w * 0.64);
      petY = Math.max(128, h - 280);
      bossY = 58;
      bossName.setBounds(50, 40, hudWidth - 36, 26);
      bossHp.setBounds(50, 78, hudWidth - 36, 40);
      int petHudX = w - hudWidth - 32;
      int petHudY = h - 134;
      petName.setBounds(petHudX + 18, petHudY + 14, hudWidth - 36, 26);
      petHp.setBounds(petHudX + 18, petHudY + 52, hudWidth - 36, 40);
      turnPill.setBounds((w - 110) / 2, 26, 110, 36);
      applyCharacterBounds();
    }

    private void applyCharacterBounds() {
      int petSize = Math.min(236, getHeight() - petY - 20);
      int bossSize = Math.min(300, getHeight() - bossY - 130);
      petImage.setBounds(petGroupX + 40 + petOffsetX, petY, petSize, petSize);
      bossImage.setBounds(bossGroupX + bossOffsetX, bossY, bossSize, bossSize);
    }

    @Override protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int w = getWidth();
      int h = getHeight();
      g2.setPaint(new GradientPaint(0, 0, new Color(0x3B2F6B), 0, h, new Color(0x6A57B0)));
      g2.fillRect(0, 0, w, h);
      g2.setColor(new Color(255, 255, 255, 178));
      for (int i = 0; i < 12; i++) {
        int x = (int) (w * ((i * 0.173 + 0.07) % 1));
        int y = 30 + (int) ((h - 180) * ((i * 0.271) % 1));
        g2.fillOval(x, y, 3, 3);
      }
      int moonX = (int) (w * 0.62);
      g2.setColor(new Color(255, 241, 193, 35));
      g2.fillOval(moonX - 10, 24, 88, 88);
      g2.setColor(new Color(0xFFF1C1));
      g2.fillOval(moonX + 2, 36, 64, 64);
      g2.setColor(new Color(0x4A3C86));
      g2.fillRect(0, h - 120, w, 120);
      g2.setColor(new Color(0x5E4FA0));
      g2.fillRect(0, h - 120, w, 3);
      paintPlatform(g2, petGroupX, petY + petImage.getHeight() - 22, 310, 58);
      paintPlatform(g2, bossGroupX - 20, bossY + bossImage.getHeight() - 22, 340, 66);
      paintHudCard(g2, 32, 26, hudWidth, 110);
      paintHudCard(g2, w - hudWidth - 32, h - 134, hudWidth, 110);
      g2.setColor(new Color(255, 255, 255, 28));
      g2.fillRoundRect((w - 110) / 2, 26, 110, 36, 36, 36);
      g2.setColor(new Color(255, 255, 255, 75));
      g2.drawRoundRect((w - 110) / 2, 26, 110, 36, 36, 36);
      g2.dispose();
    }

    private void paintPlatform(Graphics2D g, int x, int y, int w, int h) {
      g.setColor(new Color(0x54458F));
      g.fillOval(x, y + 8, w, h);
      g.setColor(new Color(0x7D6BC4));
      g.fillOval(x, y, w, h);
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
      g2.setColor(new Color(0x54458F));
      g2.fillRoundRect(x, y + 5, w, h, 22, 22);
      g2.setColor(UiStyle.SURFACE);
      g2.fillRoundRect(x, y, w, h, 22, 22);
      g2.setStroke(new BasicStroke(2));
      g2.setColor(UiStyle.BORDER);
      g2.drawRoundRect(x, y, w, h, 22, 22);
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
