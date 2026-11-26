package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;
import main.java.com.quizpet.ui.UiStyle;

/**
 * 경험치·능력치를 표시하는 둥근 막대입니다. 값은 0~maximum 범위로 보정해 그립니다.
 * 성장 전 값(previous)을 함께 주면 늘어난 부분을 밝은 색으로 구분해 그리고,
 * animate를 켜면 이전 값에서 새 값까지 막대가 차오르는 애니메이션을 보여 줍니다.
 */
public final class GaugeBar extends JPanel {
  /** 채움 애니메이션 길이(ms)입니다. */
  static final int FILL_DURATION_MS = 700;
  private static final int FRAME_MS = 16;

  private final Color fill;
  private final boolean outlined;
  private int value;
  private int maximum = 1;
  private int previous = -1;
  /** 화면에 그리는 값입니다. 애니메이션 중에는 value와 다를 수 있습니다. */
  private double shown;
  private double animationFrom;
  private int elapsedMs;
  private final Timer timer = new Timer(FRAME_MS, e -> advance(FRAME_MS));

  /**
   * @param fill 채움 색
   * @param height 막대 높이(px)
   * @param outlined true면 경험치 바처럼 테두리와 아래쪽 진한 선을 함께 그립니다.
   */
  public GaugeBar(Color fill, int height, boolean outlined) {
    if (height <= 0) {
      throw new IllegalArgumentException("막대 높이는 양수여야 합니다.");
    }
    this.fill = fill;
    this.outlined = outlined;
    setOpaque(false);
    setPreferredSize(new Dimension(120, height));
    setMinimumSize(new Dimension(40, height));
    setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
  }

  /** 증가분 표시 없이 값을 바로 바꿉니다. */
  public void setValue(int value, int maximum) {
    setValue(value, maximum, -1, false);
  }

  /**
   * @param previous 성장 전 값입니다. 0 이상이고 value보다 작으면 늘어난 부분을 강조합니다(-1이면 강조 없음).
   * @param animate true면 previous(없으면 현재 표시값)에서 value까지 차오르게 그립니다.
   */
  public void setValue(int value, int maximum, int previous, boolean animate) {
    this.maximum = Math.max(1, maximum);
    this.value = clamp(value);
    this.previous = previous >= 0 && clamp(previous) < this.value ? clamp(previous) : -1;
    String description = value + " / " + this.maximum
        + (hasGain() ? " (+" + getGain() + ")" : "");
    setToolTipText(description);
    getAccessibleContext().setAccessibleDescription(description);
    timer.stop();
    if (animate) {
      animationFrom = hasGain() ? this.previous : Math.min(shown, this.value);
      shown = animationFrom;
      elapsedMs = 0;
      if (animationFrom < this.value) {
        timer.start();
      } else {
        shown = this.value;
      }
    } else {
      shown = this.value;
    }
    repaint();
  }

  private int clamp(int raw) {
    return Math.max(0, Math.min(raw, maximum));
  }

  /** 애니메이션을 ms만큼 진행합니다. 타이머가 호출하며, 테스트에서 직접 호출할 수 있습니다. */
  void advance(int ms) {
    elapsedMs += ms;
    double t = Math.min(1.0, elapsedMs / (double) FILL_DURATION_MS);
    double eased = 1 - Math.pow(1 - t, 3);
    shown = animationFrom + (value - animationFrom) * eased;
    if (t >= 1.0) {
      shown = value;
      timer.stop();
    }
    repaint();
  }

  public boolean isAnimating() {
    return timer.isRunning();
  }

  public int getValue() {
    return value;
  }

  public int getMaximum() {
    return maximum;
  }

  /** 채워진 비율(0.0~1.0)입니다. 애니메이션과 관계없이 최종 값 기준입니다. */
  public double getRatio() {
    return (double) value / maximum;
  }

  /** 지금 화면에 그려지는 비율입니다. */
  double getShownRatio() {
    return shown / maximum;
  }

  public boolean hasGain() {
    return previous >= 0;
  }

  /** 성장 전 값 대비 늘어난 양입니다. 강조가 없으면 0입니다. */
  public int getGain() {
    return hasGain() ? value - previous : 0;
  }

  @Override
  public void removeNotify() {
    timer.stop();
    shown = value;
    super.removeNotify();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int w = getWidth();
    int h = getHeight();
    int arc = h;
    g.setColor(outlined ? UiStyle.SUNKEN : new Color(0xF1E6D3));
    g.fillRoundRect(0, 0, w, h, arc, arc);

    int filled = (int) Math.round(w * shown / maximum);
    if (filled > 0) {
      filled = Math.max(filled, Math.min(w, h));
      if (hasGain()) {
        // 늘어난 부분은 원래 색으로 진하게, 성장 전 부분은 연한 색으로 덮어 그려 증가분이 눈에 띄게 합니다.
        g.setColor(fill);
        g.fillRoundRect(0, 0, filled, h, arc, arc);
        int base = (int) Math.round(w * (double) previous / maximum);
        if (base > 0) {
          base = Math.min(filled, Math.max(base, Math.min(w, h)));
          g.setColor(lighter(fill));
          g.fillRoundRect(0, 0, base, h, arc, arc);
        }
      } else {
        g.setColor(fill);
        g.fillRoundRect(0, 0, filled, h, arc, arc);
      }
      if (outlined && h >= 8) {
        g.setColor(UiStyle.EXP_DARK);
        g.fillRoundRect(0, h - 3, filled, 3, 3, 3);
      }
    }
    if (outlined) {
      g.setColor(UiStyle.BORDER);
      g.setStroke(new BasicStroke(2));
      g.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
    }
    g.dispose();
  }

  private static Color lighter(Color color) {
    return new Color((color.getRed() + 255 * 2) / 3, (color.getGreen() + 255 * 2) / 3,
        (color.getBlue() + 255 * 2) / 3);
  }
}
