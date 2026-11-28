package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.UiStyle;

/**
 * 캐릭터 이미지를 바닥·그림자와 함께 그리는 무대입니다(UI-03 사이드바 상단).
 * 크기에 맞춰 이미지를 확대·축소하고, 왼쪽 위에 표정 배지, 오른쪽 위에 말풍선을 표시합니다.
 * 이미지 리소스가 없으면 외형 이름을 적은 원으로 대체합니다.
 */
public final class PetStage extends JPanel {
  private static final Color SKY = new Color(0xFFF3D6);
  private static final Color FLOOR = new Color(0xFBE3B8);
  private static final Font BADGE_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
  private static final Font BUBBLE_FONT = new Font(Font.DIALOG, Font.BOLD, 13);

  private static final Font EFFECT_FONT = new Font(Font.DIALOG, Font.BOLD, 20);
  private static final int FRAME_MS = 16;
  /** 정답 반응: 두 번 통통 뛰는 시간(ms)입니다. */
  static final int HOP_MS = 800;
  /** 오답 반응: 좌우로 흔들리는 시간(ms)입니다. */
  static final int SHAKE_MS = 450;
  /** 획득 문구 하나가 떠올라 사라지는 시간(ms)과 문구 사이 간격(ms)입니다. */
  static final int FLOAT_MS = 1100;
  static final int FLOAT_GAP_MS = 220;

  /** 캐릭터 위로 떠오르는 획득 문구입니다(예: "+10 EXP"). */
  public record Effect(String text, Color color) {
    public Effect {
      if (text == null || text.isBlank()) {
        throw new IllegalArgumentException("효과 문구는 비어 있을 수 없습니다.");
      }
      if (color == null) {
        throw new IllegalArgumentException("효과 색은 필수입니다.");
      }
    }
  }

  private enum Motion { NONE, HOP, SHAKE }

  private String spriteKey = "DUCK";
  private PetMood mood = PetMood.IDLE;
  private String message = "";
  private Motion motion = Motion.NONE;
  private java.util.List<Effect> effects = java.util.List.of();
  private int elapsedMs;
  private boolean ambientAnimation;
  private int ambientElapsedMs;
  private final javax.swing.Timer ambientTimer = new javax.swing.Timer(FRAME_MS, e -> advanceAmbient(FRAME_MS));
  private final javax.swing.Timer timer = new javax.swing.Timer(FRAME_MS, e -> advance(FRAME_MS));

  public PetStage() {
    setOpaque(false);
    setPreferredSize(new Dimension(280, 270));
    setMinimumSize(new Dimension(200, 170));
    addHierarchyListener(e -> {
      if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
        if (isShowing()) {
          if (ambientAnimation) ambientTimer.start();
        } else {
          ambientTimer.stop();
          ambientElapsedMs = 0;
          stopAnimation();
        }
      }
    });
  }

  /** 사이드바에서만 평상시 작은 움직임을 켭니다. 표시 중에만 반복합니다. */
  public void setAmbientAnimation(boolean enabled) {
    ambientAnimation = enabled;
    ambientTimer.stop();
    ambientElapsedMs = 0;
    if (enabled && isShowing()) ambientTimer.start();
    repaint();
  }

  void advanceAmbient(int ms) {
    ambientElapsedMs = (ambientElapsedMs + ms) % 4000;
    repaint();
  }

  double ambientBreath() {
    if (!ambientAnimation || motion != Motion.NONE) return 0;
    return (1 - Math.cos(2 * Math.PI * ambientElapsedMs / 4000)) / 2;
  }

  /** 정답 반응입니다. 캐릭터가 통통 뛰고 effects 문구가 차례로 떠올랐다가 사라집니다. */
  public void celebrate(java.util.List<Effect> effects) {
    startMotion(Motion.HOP, effects);
  }

  /** 오답 반응입니다. 캐릭터가 좌우로 살짝 흔들립니다. */
  public void shake() {
    startMotion(Motion.SHAKE, java.util.List.of());
  }

  public void stopAnimation() {
    timer.stop();
    motion = Motion.NONE;
    effects = java.util.List.of();
    elapsedMs = 0;
    repaint();
  }

  private void startMotion(Motion next, java.util.List<Effect> nextEffects) {
    timer.stop();
    motion = next;
    effects = java.util.List.copyOf(nextEffects == null ? java.util.List.of() : nextEffects);
    elapsedMs = 0;
    timer.start();
    repaint();
  }

  /** 애니메이션을 ms만큼 진행합니다. 타이머가 호출하며, 테스트에서 직접 호출할 수 있습니다. */
  void advance(int ms) {
    elapsedMs += ms;
    if (elapsedMs >= getDurationMs()) {
      stopAnimation();
      return;
    }
    repaint();
  }

  /** 지금 재생 중인 반응의 전체 길이(ms)입니다. */
  int getDurationMs() {
    int moveMs = switch (motion) {
      case HOP -> HOP_MS;
      case SHAKE -> SHAKE_MS;
      case NONE -> 0;
    };
    int floatMs = effects.isEmpty() ? 0 : FLOAT_GAP_MS * (effects.size() - 1) + FLOAT_MS;
    return Math.max(moveMs, floatMs);
  }

  public boolean isAnimating() {
    return timer.isRunning();
  }

  public java.util.List<Effect> getEffects() {
    return effects;
  }

  /** 정답 반응 중인지 여부입니다. */
  public boolean isCelebrating() {
    return isAnimating() && motion == Motion.HOP;
  }

  /** 오답 반응 중인지 여부입니다. */
  public boolean isShaking() {
    return isAnimating() && motion == Motion.SHAKE;
  }

  /** 현재 프레임에서 캐릭터를 위로 올리는 높이(px, 양수가 위쪽)입니다. */
  int hopOffset(int size) {
    if (motion != Motion.HOP || elapsedMs >= HOP_MS) {
      return 0;
    }
    double t = elapsedMs / (double) HOP_MS;
    // 두 번 뛰되 두 번째는 낮게 뜁니다.
    return (int) Math.round(size * 0.16 * Math.abs(Math.sin(Math.PI * 2 * t)) * (1 - t * 0.55));
  }

  /** 현재 프레임에서 캐릭터를 옆으로 미는 거리(px)입니다. */
  int shakeOffset(int size) {
    if (motion != Motion.SHAKE || elapsedMs >= SHAKE_MS) {
      return 0;
    }
    double t = elapsedMs / (double) SHAKE_MS;
    return (int) Math.round(size * 0.04 * Math.sin(Math.PI * 6 * t) * (1 - t));
  }

  @Override
  public void removeNotify() {
    ambientTimer.stop();
    ambientElapsedMs = 0;
    stopAnimation();
    super.removeNotify();
  }

  public void showPet(String spriteKey, PetMood mood, String message) {
    this.spriteKey = spriteKey;
    this.mood = mood == null ? PetMood.IDLE : mood;
    this.message = message == null ? "" : message;
    String name = UiStyle.appearanceOf(spriteKey).getDisplayName();
    getAccessibleContext().setAccessibleName(name + " 캐릭터, 상태 " + this.mood.getDisplayName());
    setToolTipText(this.message.isEmpty() ? null : this.message);
    repaint();
  }

  public PetMood getMood() {
    return mood;
  }

  public String getMessage() {
    return message;
  }

  /** 현재 외형·표정의 이미지 리소스를 찾았는지 여부입니다. */
  public boolean hasImage() {
    return UiStyle.petImage(spriteKey, mood) != null;
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    int arc = 22;
    int floorY = (int) (h * 0.78);

    g.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, arc, arc));
    g.setPaint(new GradientPaint(0, 0, SKY, 0, floorY, new Color(0xFFE9C2)));
    g.fillRect(0, 0, w, floorY);
    g.setColor(FLOOR);
    g.fillRect(0, floorY, w, h - floorY);

    int size = Math.max(40, Math.min(w - 40, floorY - 30));
    int lift = hopOffset(size);
    int x = (w - size) / 2 + shakeOffset(size);
    int y = floorY - size + size / 12 - lift;
    // 뛰어오르면 그림자가 작고 옅어집니다.
    double shadowScale = 1 - Math.min(0.35, lift / (double) size);
    int shadowW = (int) (size * 3 / 4 * shadowScale);
    g.setColor(new Color(0, 0, 0, (int) (28 * shadowScale)));
    g.fillOval(w / 2 - shadowW / 2, floorY + size / 20, shadowW, Math.max(8, size / 9));

    BufferedImage image = UiStyle.petImage(spriteKey, mood);
    Graphics2D body = (Graphics2D) g.create();
    double breath = ambientBreath();
    // 발밑을 기준으로 미세하게 숨 쉬도록 하여 몸 전체가 바닥에서 미끄러지지 않게 합니다.
    body.translate(x + size / 2.0, y + size);
    body.scale(1 + breath * 0.004, 1 - breath * 0.012);
    body.translate(-size / 2.0, -size);
    if (image != null) {
      body.drawImage(image, 0, 0, size, size, null);
    } else {
      paintFallback(body, size / 8, size / 8, size * 3 / 4);
    }
    body.dispose();
    if (motion == Motion.HOP && elapsedMs < HOP_MS) {
      paintSparkles(g, x, y, size);
    }
    // 말풍선(오른쪽 위)과 겹치지 않도록 캐릭터 몸 앞에서 떠올라 머리 근처에서 사라집니다.
    paintEffects(g, w / 2, y + lift + size * 3 / 5);
    g.setClip(null);

    paintBadge(g, mood.getDisplayName(), 14, 14);
    if (!message.isEmpty()) {
      paintBubble(g, message, w - 14, 14);
    }

    g.setColor(UiStyle.BORDER);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
    g.dispose();
  }

  /** 정답 반응 동안 캐릭터 주변에서 반짝이는 별입니다. */
  private void paintSparkles(Graphics2D g, int x, int y, int size) {
    double t = elapsedMs / (double) HOP_MS;
    int alpha = (int) (255 * (1 - t));
    g.setColor(new Color(UiStyle.EXP.getRed(), UiStyle.EXP.getGreen(), UiStyle.EXP.getBlue(), alpha));
    double[][] spots = {{0.05, 0.25}, {0.92, 0.18}, {0.12, 0.70}, {0.88, 0.62}};
    for (int i = 0; i < spots.length; i++) {
      double phase = Math.abs(Math.sin(Math.PI * (t * 3 + i * 0.4)));
      int r = (int) Math.max(3, size * 0.05 * phase);
      paintStar(g, x + (int) (size * spots[i][0]), y + (int) (size * spots[i][1]), r);
    }
  }

  private static void paintStar(Graphics2D g, int cx, int cy, int r) {
    int inner = Math.max(1, r / 3);
    Polygon star = new Polygon(
        new int[] {cx, cx + inner, cx + r, cx + inner, cx, cx - inner, cx - r, cx - inner},
        new int[] {cy - r, cy - inner, cy, cy + inner, cy + r, cy + inner, cy, cy - inner}, 8);
    g.fillPolygon(star);
  }

  /** 획득 문구를 차례로 위로 띄우며 서서히 사라지게 그립니다. */
  private void paintEffects(Graphics2D g, int centerX, int startY) {
    if (effects.isEmpty()) {
      return;
    }
    g.setFont(EFFECT_FONT);
    FontMetrics fm = g.getFontMetrics();
    for (int i = 0; i < effects.size(); i++) {
      int local = elapsedMs - i * FLOAT_GAP_MS;
      if (local < 0 || local >= FLOAT_MS) {
        continue;
      }
      double t = local / (double) FLOAT_MS;
      int alpha = (int) (255 * (t < 0.7 ? 1 : (1 - t) / 0.3));
      int rise = (int) (60 * (1 - Math.pow(1 - t, 2)));
      Effect effect = effects.get(i);
      int tx = centerX - fm.stringWidth(effect.text()) / 2 + (i % 2 == 0 ? -26 : 26);
      int ty = startY - rise;
      g.setColor(new Color(255, 255, 255, alpha));
      for (int dx = -2; dx <= 2; dx += 2) {
        for (int dy = -2; dy <= 2; dy += 2) {
          g.drawString(effect.text(), tx + dx, ty + dy);
        }
      }
      Color c = effect.color();
      g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
      g.drawString(effect.text(), tx, ty);
    }
  }

  private void paintFallback(Graphics2D g, int x, int y, int size) {
    g.setColor(new Color(0xFFE08A));
    g.fillOval(x, y, size, size);
    g.setColor(UiStyle.TEXT);
    g.setFont(BUBBLE_FONT);
    String text = UiStyle.appearanceOf(spriteKey).getDisplayName();
    FontMetrics fm = g.getFontMetrics();
    g.drawString(text, x + (size - fm.stringWidth(text)) / 2, y + (size + fm.getAscent()) / 2 - 2);
  }

  private static void paintBadge(Graphics2D g, String text, int x, int y) {
    g.setFont(BADGE_FONT);
    FontMetrics fm = g.getFontMetrics();
    int bw = fm.stringWidth(text) + 22;
    int bh = fm.getHeight() + 10;
    g.setColor(UiStyle.SURFACE);
    g.fillRoundRect(x, y, bw, bh, bh, bh);
    g.setColor(UiStyle.BORDER);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(x, y, bw, bh, bh, bh);
    g.setColor(UiStyle.TEXT);
    g.drawString(text, x + 11, y + (bh + fm.getAscent() - fm.getDescent()) / 2);
  }

  /**
   * 오른쪽 끝(right)에 맞춰 말풍선을 그립니다.
   * 한 줄에 들어가지 않으면 띄어쓰기 기준으로 최대 두 줄까지 나누고, 그래도 넘치면 줄임표로 자릅니다.
   */
  private void paintBubble(Graphics2D g, String text, int right, int y) {
    g.setFont(BUBBLE_FONT);
    FontMetrics fm = g.getFontMetrics();
    int maxTextWidth = Math.max(60, getWidth() - 120);
    java.util.List<String> lines = wrap(text, fm, maxTextWidth, 2);
    int textWidth = 0;
    for (String line : lines) {
      textWidth = Math.max(textWidth, fm.stringWidth(line));
    }
    int bw = textWidth + 24;
    int bh = fm.getHeight() * lines.size() + 14;
    int x = right - bw;
    Polygon tail = new Polygon(
        new int[] {x + 22, x + 34, x + 28}, new int[] {y + bh - 2, y + bh - 2, y + bh + 9}, 3);
    g.setColor(UiStyle.SURFACE);
    g.fillRoundRect(x, y, bw, bh, 14, 14);
    g.fillPolygon(tail);
    g.setColor(UiStyle.TEXT);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(x, y, bw, bh, 14, 14);
    g.drawLine(x + 22, y + bh, x + 28, y + bh + 9);
    g.drawLine(x + 28, y + bh + 9, x + 34, y + bh);
    g.setColor(UiStyle.SURFACE);
    g.drawLine(x + 24, y + bh, x + 32, y + bh);
    g.setColor(UiStyle.TEXT);
    int baseline = y + 7 + fm.getAscent();
    for (String line : lines) {
      g.drawString(line, x + 12, baseline);
      baseline += fm.getHeight();
    }
  }

  /** text를 maxWidth 안에 들어가도록 최대 maxLines 줄로 나눕니다. 띄어쓰기에서 먼저 끊습니다. */
  static java.util.List<String> wrap(String text, FontMetrics fm, int maxWidth, int maxLines) {
    java.util.List<String> lines = new java.util.ArrayList<>();
    String rest = text.strip();
    while (!rest.isEmpty() && lines.size() < maxLines) {
      if (fm.stringWidth(rest) <= maxWidth) {
        lines.add(rest);
        rest = "";
        break;
      }
      int end = 1;
      while (end < rest.length() && fm.stringWidth(rest.substring(0, end + 1)) <= maxWidth) {
        end++;
      }
      int space = rest.lastIndexOf(' ', end);
      int cut = space > 0 ? space : end;
      lines.add(rest.substring(0, cut).strip());
      rest = rest.substring(cut).strip();
    }
    if (!rest.isEmpty()) {
      int last = lines.size() - 1;
      String line = lines.get(last);
      while (line.length() > 1 && fm.stringWidth(line + "…") > maxWidth) {
        line = line.substring(0, line.length() - 1);
      }
      lines.set(last, line + "…");
    }
    return lines;
  }
}
