package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import main.java.com.quizpet.ui.UiStyle;

/** 이미지가 없어도 텍스트로 사용할 수 있는 게임 버튼. */
public class ImageButton extends JButton {
  public ImageButton(String text) {
    this(text, null, 28);
  }

  public ImageButton(String text, String imagePath, int imageSize) {
    super(text);
    if (imageSize <= 0) throw new IllegalArgumentException("이미지 크기는 양수여야 합니다.");
    setFont(UiStyle.BODY_FONT);
    setBackground(UiStyle.BRAND);
    setForeground(UiStyle.TEXT);
    setContentAreaFilled(false);
    setOpaque(false);
    setBorderPainted(false);
    setRolloverEnabled(true);
    setBorder(BorderFactory.createEmptyBorder(10, 16, 14, 16));
    setIconTextGap(8);
    if (imagePath != null) {
      URL resource = ImageButton.class.getResource(imagePath);
      if (resource != null) {
        setIcon(new ImageIcon(new ImageIcon(resource).getImage()
            .getScaledInstance(imageSize, imageSize, Image.SCALE_SMOOTH)));
      }
    }
    getAccessibleContext().setAccessibleName(text);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    size.height = Math.max(46, size.height);
    return size;
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    paintSurface(graphics, getWidth(), getHeight(),
        isEnabled() ? (getModel().isRollover() ? getBackground().brighter() : getBackground())
            : UiStyle.BORDER,
        hasFocus() ? UiStyle.TEXT : getBackground().darker(), getModel().isPressed());
    super.paintComponent(graphics);
  }

  static void paintSurface(Graphics graphics, int width, int height,
      Color fill, Color border, boolean pressed) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int y = pressed ? 4 : 1;
    g.setColor(border);
    g.fillRoundRect(1, 5, width - 2, height - 6, 18, 18);
    g.setColor(fill);
    g.fillRoundRect(1, y, width - 2, height - 6, 18, 18);
    g.setColor(border);
    g.setStroke(new BasicStroke(2));
    g.drawRoundRect(1, y, width - 3, height - 7, 18, 18);
    g.dispose();
  }
}
