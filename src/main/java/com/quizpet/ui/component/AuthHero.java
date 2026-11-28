package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.UiStyle;

/**
 * 시작·캐릭터 생성 화면(UI-01, UI-02)의 왼쪽 브랜드 영역입니다.
 * 배경색·장식 원·지면 띠를 그리고, 지면 위에 펫을 나란히 세웁니다. 이름표를 주면 첫 번째 펫 위에 표시합니다.
 * 두 화면이 함께 쓰는 폼 열·입력칸 스타일 도우미도 둡니다.
 */
public final class AuthHero extends JPanel {
  public static final int WIDTH = 560;
  private static final int GROUND_HEIGHT = 150;
  private static final int FORM_MAX_WIDTH = 640;
  private static final Font TAG_FONT = new Font(Font.DIALOG, Font.BOLD, 24);

  /** 지면 위에 세울 펫 한 마리입니다. size는 최대 크기이며 창이 낮으면 줄어듭니다. */
  public record HeroPet(String spriteKey, PetMood mood, int size) { }

  private Color ground;
  private List<HeroPet> pets = List.of();
  private String nameTag;

  public AuthHero(Color background, Color ground, JComponent header) {
    super(new BorderLayout());
    setOpaque(false);
    setColors(background, ground);
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(56, 52, 0, 52));
    add(header, BorderLayout.NORTH);
  }

  public void setColors(Color background, Color ground) {
    setBackground(background);
    this.ground = ground;
    repaint();
  }

  public void setPets(HeroPet... pets) {
    this.pets = List.of(pets);
    repaint();
  }

  public void setNameTag(String text) {
    nameTag = text;
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(WIDTH, super.getPreferredSize().height);
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    int width = getWidth();
    int height = getHeight();
    int groundY = height - GROUND_HEIGHT;
    g.setColor(getBackground());
    g.fillRect(0, 0, width, height);
    g.setColor(new Color(255, 255, 255, 36));
    g.fillOval(width - 220, -170, 440, 440);
    g.fillOval(-120, height / 2 - 140, 240, 240);
    g.setColor(ground);
    g.fillRect(0, groundY, width, GROUND_HEIGHT);
    g.setColor(new Color(255, 255, 255, 90));
    g.fillRect(0, groundY, width, 4);

    for (int i = 0; i < pets.size(); i++) {
      HeroPet pet = pets.get(i);
      int size = Math.max(80, Math.min(pet.size(), groundY - 200));
      int centerX = width * (i + 1) / (pets.size() + 1);
      int top = groundY + 40 - size;
      BufferedImage image = UiStyle.petImage(pet.spriteKey(), pet.mood());
      if (image != null) {
        g.drawImage(image, centerX - size / 2, top, size, size, null);
      } else {
        g.setColor(new Color(255, 255, 255, 160));
        g.fillOval(centerX - size / 2, top, size, size);
      }
      if (i == 0 && nameTag != null) {
        paintNameTag(g, nameTag, centerX, top - 12);
      }
    }
    g.dispose();
  }

  private static void paintNameTag(Graphics2D g, String text, int centerX, int bottom) {
    g.setFont(TAG_FONT);
    FontMetrics metrics = g.getFontMetrics();
    int width = metrics.stringWidth(text) + 40;
    int height = 52;
    int x = centerX - width / 2;
    int y = bottom - height;
    g.setColor(Color.WHITE);
    g.fillRoundRect(x, y, width, height, 18, 18);
    g.setColor(UiStyle.TEXT);
    g.setStroke(new BasicStroke(3));
    g.drawRoundRect(x, y, width, height, 18, 18);
    g.drawString(text, x + 20, y + (height + metrics.getAscent() - metrics.getDescent()) / 2);
  }

  /** 오른쪽 폼 열입니다. 창이 넓어져도 폭은 640px까지만 늘어납니다. */
  public static JPanel column() {
    JPanel column = new JPanel() {
      @Override
      public Dimension getMaximumSize() {
        return new Dimension(FORM_MAX_WIDTH, getPreferredSize().height);
      }
    };
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setOpaque(false);
    return column;
  }

  /** 폼 열을 크림색 배경 가운데(세로 중앙)에 둡니다. */
  public static JPanel formArea(JComponent column) {
    JPanel area = new JPanel();
    area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
    area.setBackground(UiStyle.BACKGROUND);
    area.setBorder(BorderFactory.createEmptyBorder(48, 72, 48, 72));
    column.setAlignmentX(Component.CENTER_ALIGNMENT);
    area.add(Box.createVerticalGlue());
    area.add(column);
    area.add(Box.createVerticalGlue());
    return area;
  }

  /** 폼 열에 넣는 왼쪽 정렬 글자입니다. */
  public static JLabel text(String text, Font font, Color color) {
    JLabel label = new JLabel(text);
    label.setFont(font);
    label.setForeground(color);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  /** 흰 면에 둥근 테두리를 두르고, field에 포커스가 있으면 테두리를 브랜드색으로 바꿉니다. */
  public static void styleInput(JTextComponent field, JComponent frame) {
    frame.setBackground(Color.WHITE);
    frame.setOpaque(true);
    frame.setBorder(inputBorder(UiStyle.BORDER));
    field.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        frame.setBorder(inputBorder(UiStyle.BRAND));
      }

      @Override
      public void focusLost(FocusEvent e) {
        frame.setBorder(inputBorder(UiStyle.BORDER));
      }
    });
  }

  private static Border inputBorder(Color color) {
    return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2, true),
        BorderFactory.createEmptyBorder(10, 14, 10, 14));
  }

  /** 입력이 바뀔 때마다 action을 실행합니다. */
  public static void onChange(JTextComponent field, Runnable action) {
    field.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        action.run();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        action.run();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        action.run();
      }
    });
  }
}
