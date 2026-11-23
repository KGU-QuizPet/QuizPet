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

  private String spriteKey = "DUCK";
  private PetMood mood = PetMood.IDLE;
  private String message = "";

  public PetStage() {
    setOpaque(false);
    setPreferredSize(new Dimension(280, 270));
    setMinimumSize(new Dimension(200, 170));
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
    int x = (w - size) / 2;
    int y = floorY - size + size / 12;
    g.setColor(new Color(0, 0, 0, 28));
    g.fillOval(w / 2 - size * 3 / 8, floorY + size / 20, size * 3 / 4, Math.max(8, size / 9));

    BufferedImage image = UiStyle.petImage(spriteKey, mood);
    if (image != null) {
      g.drawImage(image, x, y, size, size, null);
    } else {
      paintFallback(g, x + size / 8, y + size / 8, size * 3 / 4);
    }
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
