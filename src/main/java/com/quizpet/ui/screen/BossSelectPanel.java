package main.java.com.quizpet.ui.screen;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import main.java.com.quizpet.model.Boss;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.service.BattleService;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.PetStatusPanel;
import main.java.com.quizpet.ui.component.RoundCard;
import main.java.com.quizpet.ui.component.GameButton;

/** UI-09A: 캐릭터 레벨에 따라 보스를 선택하는 화면. */
public final class BossSelectPanel extends JPanel {
  private final PetStatusPanel sidebar = new PetStatusPanel();
  private final JPanel grid = new JPanel(new GridLayout(1, 4, 18, 18));
  private final BossCard[] cards = new BossCard[BattleService.BOSSES.size()];
  private Runnable onExit = () -> { };
  private IntConsumer onSelect = index -> { };

  public BossSelectPanel() {
    super(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);
    add(sidebar, BorderLayout.WEST);
    JPanel body = new JPanel(new BorderLayout(0, 22));
    body.setOpaque(false);
    body.setBorder(BorderFactory.createEmptyBorder(28, 32, 26, 32));
    JPanel header = new JPanel(new BorderLayout(0, 8));
    header.setOpaque(false);
    JLabel title = UiStyle.label("보스 결투", UiStyle.TITLE_FONT);
    title.setForeground(UiStyle.TEXT);
    title.setHorizontalAlignment(SwingConstants.LEFT);
    JPanel titleRow = new JPanel(new BorderLayout());
    titleRow.setOpaque(false);
    titleRow.add(title, BorderLayout.CENTER);
    JButton home = new GameButton("홈으로", Color.WHITE, UiStyle.TEXT, UiStyle.SUNKEN);
    home.setPreferredSize(new Dimension(96, 40));
    home.addActionListener(event -> onExit.run());
    titleRow.add(home, BorderLayout.EAST);
    header.add(titleRow, BorderLayout.NORTH);
    JLabel hint = UiStyle.label("도전할 보스를 골라 주세요. 내 펫이 먼저 공격해요.", UiStyle.BODY_FONT);
    hint.setForeground(UiStyle.MUTED);
    hint.setHorizontalAlignment(SwingConstants.LEFT);
    header.add(hint, BorderLayout.SOUTH);
    body.add(header, BorderLayout.NORTH);
    grid.setOpaque(false);
    for (int i = 0; i < cards.length; i++) {
      cards[i] = new BossCard(i);
      grid.add(cards[i]);
    }
    body.add(grid, BorderLayout.CENTER);
    add(body, BorderLayout.CENTER);
  }

  public void setOnExit(Runnable action) { onExit = java.util.Objects.requireNonNull(action); }

  public void setOnSelect(IntConsumer action) { onSelect = java.util.Objects.requireNonNull(action); }

  public void showPet(Pet pet, Set<Integer> cleared) {
    sidebar.showPet(pet.getName(), pet.getSpriteKey(), pet.getLevel(), pet.getXpInLevel(), Pet.XP_PER_LEVEL,
        pet.getStats().getStrength(), pet.getStats().getIntelligence(), pet.getStats().getFocus(), pet.getStats().getWisdom());
    sidebar.showSituation(PetStatusPanel.Situation.BOSS_SELECT);
    for (BossCard card : cards) card.update(pet.getLevel(), cleared.contains(card.index));
    revalidate();
    repaint();
  }

  @Override public void doLayout() {
    GridLayout layout = (GridLayout) grid.getLayout();
    int columns = getWidth() - PetStatusPanel.SIDEBAR_WIDTH >= 900 ? 4 : 2;
    layout.setRows(0);
    layout.setColumns(columns);
    super.doLayout();
  }

  private final class BossCard extends RoundCard {
    private final int index;
    private final Boss boss;
    private final JButton challenge = new GameButton("도전하기", UiStyle.BRAND, Color.WHITE, new Color(0xE07600));
    private final JPanel art;
    private boolean locked;
    private boolean cleared;
    private int remainingLevels;
    private final Map<String, Optional<BufferedImage>> images = new HashMap<>();
    private BufferedImage image;

    BossCard(int index) {
      super(new BorderLayout(0, 10));
      this.index = index;
      boss = BattleService.BOSSES.get(index);
      setBorder(BorderFactory.createEmptyBorder(3, 3, 16, 3));
      art = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
          Graphics2D g = (Graphics2D) graphics.create();
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(boss.backgroundColor())));
          g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
          if (image != null) {
            int size = Math.min(getWidth() - 14, getHeight() - 14);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, (getWidth() - size) / 2, (getHeight() - size) / 2, size, size, this);
          }
          if (image == null) {
            g.setColor(UiStyle.MUTED);
            drawCentered(g, "이미지 없음", getHeight() / 2, 12);
          }
          if (cleared) {
            g.rotate(-0.12, getWidth() - 50, 30);
            g.setColor(UiStyle.CORRECT);
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(getWidth() - 100, 12, 90, 32, 8, 8);
            g.setFont(UiStyle.SUBTITLE_FONT);
            g.drawString("CLEAR", getWidth() - 88, 35);
          }
          g.dispose();
        }
      };
      add(art, BorderLayout.NORTH);
      JPanel info = new JPanel(new BorderLayout(0, 12));
      info.setOpaque(false);
      info.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
      JPanel text = new JPanel(new GridLayout(4, 1, 0, 2));
      text.setOpaque(false);
      JLabel name = UiStyle.label(boss.name(), UiStyle.SUBTITLE_FONT);
      name.setForeground(UiStyle.TEXT);
      text.add(name);
      JLabel level = UiStyle.label("Lv." + boss.requiredLevel() + "+", UiStyle.BODY_FONT.deriveFont(Font.BOLD));
      level.setForeground(UiStyle.MUTED);
      text.add(level);
      JLabel description = UiStyle.label("<html><center>" + boss.description() + "</center></html>",
          UiStyle.BODY_FONT.deriveFont(12f));
      description.setForeground(UiStyle.MUTED);
      text.add(description);
      text.add(UiStyle.label("HP " + boss.maxHp() + " · ATK " + boss.attack(), UiStyle.BODY_FONT.deriveFont(13f)));
      info.add(text, BorderLayout.NORTH);
      challenge.setPreferredSize(new Dimension(0, 44));
      challenge.addActionListener(event -> { if (!locked) onSelect.accept(index); });
      info.add(challenge, BorderLayout.SOUTH);
      add(info, BorderLayout.CENTER);
    }

    void update(int level, boolean cleared) {
      this.cleared = cleared;
      locked = level < boss.requiredLevel();
      remainingLevels = Math.max(0, boss.requiredLevel() - level);
      challenge.setEnabled(!locked);
      challenge.setText(locked ? "잠김" : cleared ? "다시 도전" : "도전하기");
      challenge.getAccessibleContext().setAccessibleName(boss.name() + " " + challenge.getText());
      setBorderColor(locked || cleared ? UiStyle.BORDER : UiStyle.BRAND);
      String path = cleared ? boss.imagePath().replace("_idle.png", "_defeated.png") : boss.imagePath();
      image = images.computeIfAbsent(path, this::loadImage).orElse(null);
      art.repaint();
    }

    private Optional<BufferedImage> loadImage(String path) {
      java.net.URL resource = UiStyle.resource("/" + path);
      try {
        BufferedImage loaded = resource == null ? null : ImageIO.read(resource);
        if (loaded != null) return Optional.of(loaded);
        System.err.println("보스 이미지를 읽을 수 없습니다: " + path);
      } catch (IOException e) {
        System.err.println("보스 이미지를 읽을 수 없습니다: " + path + " (" + e.getMessage() + ")");
      }
      return Optional.empty();
    }

    @Override public void doLayout() {
      BorderLayout layout = (BorderLayout) getLayout();
      boolean compact = getHeight() < 360;
      String position = compact ? BorderLayout.WEST : BorderLayout.NORTH;
      if (!position.equals(layout.getConstraints(art))) {
        remove(art);
        add(art, position);
      }
      art.setPreferredSize(compact ? new Dimension(96, 0)
          : new Dimension(0, Math.min(236, getHeight() - 204)));
      super.doLayout();
    }

    @Override protected void paintChildren(Graphics graphics) {
      super.paintChildren(graphics);
      if (!locked) return;
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(43, 36, 64, 220));
      g.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 8, 20, 20);
      int cx = getWidth() / 2;
      int cy = getHeight() / 2;
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(3));
      g.drawRoundRect(cx - 7, cy - 44, 14, 20, 12, 12);
      g.fillRoundRect(cx - 11, cy - 34, 22, 18, 4, 4);
      drawCentered(g, "Lv." + boss.requiredLevel() + " 이상", cy + 12, 18);
      drawCentered(g, remainingLevels + "레벨 더 성장하면 열려요", cy + 38, 12);
      g.dispose();
    }

    private void drawCentered(Graphics2D g, String text, int y, float size) {
      g.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, size));
      g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, y);
    }
  }
}
