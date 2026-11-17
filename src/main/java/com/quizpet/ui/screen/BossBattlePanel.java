package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.Timer;
import main.java.com.quizpet.model.BattleSession;
import main.java.com.quizpet.model.BattleSession.State;
import main.java.com.quizpet.ui.UiStyle;

// 1초에 한쪽씩 공격하는 보스전 화면.
public final class BossBattlePanel extends JPanel {
  private final JLabel bossInfo = UiStyle.label(" ", UiStyle.SUBTITLE_FONT);
  private final JLabel playerInfo = UiStyle.label(" ", UiStyle.BODY_FONT);
  private final JLabel turnInfo = UiStyle.label("전투 준비", UiStyle.BODY_FONT);
  private final JProgressBar bossHp = hpBar(new Color(0xD56A72));
  private final JProgressBar playerHp = hpBar(new Color(0x70A890));
  private final JTextArea message = new JTextArea(2, 20);
  private final JButton startButton = UiStyle.button("전투 시작");
  private final Battlefield battlefield = new Battlefield();
  private final Timer timer = new Timer(1000, event -> advanceTurn());
  private BattleSession battle;
  private Runnable onExit = () -> { };

  public BossBattlePanel() {
    setLayout(new BorderLayout(0, 12));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    add(UiStyle.label("보스 잡기", UiStyle.TITLE_FONT), BorderLayout.NORTH);
    JPanel arena = new JPanel(new BorderLayout(0, 8));
    arena.setBackground(Color.WHITE);
    arena.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0xDADCE3)),
        BorderFactory.createEmptyBorder(12, 12, 12, 12)));
    arena.add(summary(bossInfo, bossHp), BorderLayout.NORTH);
    arena.add(battlefield, BorderLayout.CENTER);
    arena.add(summary(playerInfo, playerHp), BorderLayout.SOUTH);
    add(arena, BorderLayout.CENTER);

    JPanel footer = new JPanel(new BorderLayout(0, 8));
    footer.setOpaque(false);
    footer.add(turnInfo, BorderLayout.NORTH);
    message.setFont(UiStyle.BODY_FONT);
    message.setForeground(new Color(0x334155));
    message.setEditable(false);
    message.setFocusable(false);
    message.setLineWrap(true);
    message.setWrapStyleWord(true);
    message.setOpaque(false);
    footer.add(message, BorderLayout.CENTER);
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
    actions.setOpaque(false);
    JButton exitButton = UiStyle.button("나가기");
    actions.add(startButton);
    actions.add(exitButton);
    footer.add(actions, BorderLayout.SOUTH);
    add(footer, BorderLayout.SOUTH);
    startButton.setEnabled(false);
    startButton.addActionListener(event -> startBattle());
    exitButton.addActionListener(event -> {
      stopBattle();
      onExit.run();
    });
    // CardLayout 전환이나 창 숨김으로 보이지 않으면 전투를 포기.
    addHierarchyListener(event -> {
      if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
        stopBattle();
      }
    });
  }

  public void setOnExit(Runnable action) {
    onExit = Objects.requireNonNull(action, "나가기 동작은 필수입니다.");
  }

  public void showBattle(BattleSession session) {
    stopBattle();
    battle = Objects.requireNonNull(session, "battle은 필수입니다.");
    battlefield.playerImage = loadImage(battle.getAppearance().getImagePath());
    battlefield.bossImage = loadImage(battle.getBoss().imagePath());
    battlefield.backgroundImage = loadImage("images/backgrounds/battle.jpg");
    updateView();
  }

  private void startBattle() {
    if (battle == null || battle.isRunning()) return;
    if (battle.getState() != State.READY) {
      battle = new BattleSession(battle.getPetName(), battle.getAppearance(), battle.getLevel(),
          battle.getAttack(), battle.getMaxHp(), battle.getBoss());
    }
    battle.start();
    updateView();
    timer.restart(); // 첫 공격도 시작 1초 후에 실행.
  }

  private void advanceTurn() {
    // 세션 종료 시 중단 전에 이벤트 큐에 들어간 공격 무시.
    if (battle == null || !battle.isRunning()) {
      timer.stop();
      return;
    }
    battle.advanceTurn();
    if (!battle.isRunning()) timer.stop();
    updateView();
  }

  public void stopBattle() {
    timer.stop();
    if (battle != null) {
      battle.abandon();
      updateView();
    }
  }

  @Override
  public void removeNotify() {
    stopBattle();
    super.removeNotify();
  }

  private void updateView() {
    bossInfo.setText(battle.getBoss().name() + "  ·  공격력 " + battle.getBoss().attack());
    playerInfo.setText(battle.getPetName() + "  Lv." + battle.getLevel()
        + "  ·  공격력 " + battle.getAttack());
    updateHp(bossHp, battle.getBossHp(), battle.getBoss().maxHp());
    updateHp(playerHp, battle.getPlayerHp(), battle.getMaxHp());
    String stateText = switch (battle.getState()) {
      case READY -> "전투 준비 · 내 캐릭터 선공";
      case PLAYER_TURN -> "다음 공격: 내 캐릭터";
      case BOSS_TURN -> "다음 공격: 보스";
      case WON -> "VICTORY · 승리";
      case LOST -> "DEFEAT · 패배";
      case ABANDONED -> "전투 종료";
    };
    turnInfo.setText((battle.getTurnCount() > 0 ? battle.getTurnCount() + "턴 · " : "") + stateText);
    message.setText(battle.getMessage());
    startButton.setText(battle.isRunning() ? "전투 중"
        : battle.getState() == State.READY ? "전투 시작" : "재도전");
    startButton.setEnabled(!battle.isRunning());
    battlefield.repaint();
  }

  private static JPanel summary(JLabel label, JProgressBar hp) {
    JPanel panel = new JPanel(new BorderLayout(0, 6));
    panel.setOpaque(false);
    panel.add(label, BorderLayout.NORTH);
    panel.add(hp, BorderLayout.SOUTH);
    return panel;
  }

  private static JProgressBar hpBar(Color color) {
    JProgressBar bar = new JProgressBar();
    bar.setForeground(color);
    bar.setFont(UiStyle.BODY_FONT);
    bar.setStringPainted(true);
    bar.setPreferredSize(new Dimension(100, 24));
    return bar;
  }

  private static void updateHp(JProgressBar bar, int hp, int maxHp) {
    bar.setMaximum(maxHp);
    bar.setValue(hp);
    bar.setString("HP " + hp + " / " + maxHp);
  }

  private static Image loadImage(String name) {
    if (name == null || name.isBlank()) return null;
    try (InputStream input = BossBattlePanel.class.getResourceAsStream("/" + name)) {
      if (input != null) return ImageIO.read(input);
    } catch (IOException e) {
      System.getLogger(BossBattlePanel.class.getName()).log(System.Logger.Level.WARNING, name, e);
    }
    Path file = Path.of("src/main/resources").resolve(name);
    if (!Files.isRegularFile(file)) return null;
    try {
      return ImageIO.read(file.toFile());
    } catch (IOException e) {
      System.getLogger(BossBattlePanel.class.getName()).log(System.Logger.Level.WARNING, name, e);
      return null;
    }
  }

  /** 배경과 투명 PNG를 합성합니다. 이미지는 showBattle()에서 한 번만 읽습니다. */
  private final class Battlefield extends JPanel {
    private Image playerImage;
    private Image bossImage;
    private Image backgroundImage;

    Battlefield() {
      setPreferredSize(new Dimension(360, 320));
      setBackground(new Color(0x17324A));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D) graphics.create();
      try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (backgroundImage != null) {
          drawBackground(g);
        }
        if (battle == null) return;
        g.scale(getWidth() / 360.0, getHeight() / 320.0);
        if (bossImage != null) drawImageFit(g, bossImage, 164, 30, 190, 230);
        else drawMissingImage(g, "보스 이미지 없음", 198, 165);
        // 작은 픽셀 아트는 보간으로 흐려지지 않도록 확대.
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (playerImage != null) drawImageFit(g, playerImage, 18, 176, 120, 120);
        else drawMissingImage(g, battle.getAppearance().getDisplayName() + " 이미지 없음", 18, 260);
      } finally {
        g.dispose();
      }
    }

    /** 가로·세로 비율을 유지하면서 전장을 채우고, 넘치는 가장자리만 자릅니다. */
    private void drawBackground(Graphics2D g) {
      double scale = Math.max(getWidth() / (double) backgroundImage.getWidth(this),
          getHeight() / (double) backgroundImage.getHeight(this));
      int width = (int) Math.ceil(backgroundImage.getWidth(this) * scale);
      int height = (int) Math.ceil(backgroundImage.getHeight(this) * scale);
      g.drawImage(backgroundImage, (getWidth() - width) / 2, (getHeight() - height) / 2,
          width, height, this);
    }

    private void drawImageFit(Graphics2D g, Image image, int x, int y, int width, int height) {
      double scale = Math.min(width / (double) image.getWidth(this), height / (double) image.getHeight(this));
      int w = (int) (image.getWidth(this) * scale);
      int h = (int) (image.getHeight(this) * scale);
      g.drawImage(image, x + (width - w) / 2, y + height - h, w, h, this);
    }

    private void drawMissingImage(Graphics2D g, String text, int x, int y) {
      g.setFont(UiStyle.BODY_FONT.deriveFont(12f));
      g.setColor(Color.WHITE);
      g.drawString(text, x, y);
    }
  }
}
