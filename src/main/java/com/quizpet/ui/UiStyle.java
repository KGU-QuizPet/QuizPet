package main.java.com.quizpet.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetMood;

public final class UiStyle {
  public static final Color BACKGROUND = new Color(0xFFF8EE);
  public static final Color BRAND = new Color(0xFF8A00);
  public static final Color BORDER = new Color(0xEBDDC5);
  public static final Color TEXT = new Color(0x2B2440);
  public static final Color MUTED = new Color(0x6E6787);
  public static final Color CORRECT = new Color(0x12B76A);
  public static final Color WRONG = new Color(0xF04438);
  public static final Color CORRECT_SOFT = new Color(0xE3F8EE);
  public static final Color WRONG_SOFT = new Color(0xFDE8E7);
  public static final Color POINT = new Color(0x6C8EBF);
  public static final Color SURFACE = Color.WHITE;
  public static final Color SUNKEN = new Color(0xFBEFD9);
  public static final Color SHADOW = new Color(0xE6D5B8);
  public static final Color HINT = new Color(0xA49DB5);
  public static final Color EXP = new Color(0xFFC233);
  public static final Color EXP_DARK = new Color(0xE0A100);
  public static final Color INK_PANEL = new Color(0x2B2440);

  /** 능력치 표시 순서입니다(UI-03 사이드바 기준). */
  public static final String[] STAT_TYPES = {"INT", "STR", "FOC", "WIS"};

  public static final Font TITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 28);
  public static final Font SUBTITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 18);
  public static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 15);

  public static final String[] PET_KEYS = {"DUCK", "TURTLE", "LIZARD"};
  public static final String[] PET_LABELS = {"고라파카", "고북이", "파이리"};
  private static final Color[] PET_COLORS = {
    new Color(0xFFE08A), new Color(0x9ED9A0), new Color(0xF4A9A0)
  };

  private static final Map<PetAppearance, Map<PetMood, Optional<BufferedImage>>> PET_IMAGES =
      new EnumMap<>(PetAppearance.class);
  private static final Map<String, ImageIcon> PET_ICONS = new HashMap<>();

  private UiStyle() {}

  /** 능력치 색은 해당 능력치를 올려 주는 분야 색과 같습니다(INT=과학, STR=스포츠, FOC=언어, WIS=역사). */
  public static Color statColor(String statType) {
    return switch (statType) {
      case "INT" -> categoryColor(Category.SCIENCE);
      case "STR" -> categoryColor(Category.SPORTS);
      case "FOC" -> categoryColor(Category.LANGUAGE);
      case "WIS" -> categoryColor(Category.HISTORY);
      default -> throw new IllegalArgumentException("statType은 STR, INT, FOC, WIS 중 하나여야 합니다.");
    };
  }

  public static String statLabel(String statType) {
    return switch (statType) {
      case "INT" -> "지성";
      case "STR" -> "체력";
      case "FOC" -> "집중";
      case "WIS" -> "지혜";
      default -> throw new IllegalArgumentException("statType은 STR, INT, FOC, WIS 중 하나여야 합니다.");
    };
  }

  /** 정의되지 않은 spriteKey는 기본 외형(DUCK)으로 표시합니다. */
  public static PetAppearance appearanceOf(String spriteKey) {
    for (PetAppearance appearance : PetAppearance.values()) {
      if (appearance.getSpriteKey().equals(spriteKey)) {
        return appearance;
      }
    }
    return PetAppearance.DUCK;
  }

  /**
   * 외형·표정에 맞는 펫 이미지를 한 번만 읽어 재사용합니다.
   * 이미지 파일이 없거나 읽을 수 없으면 null을 반환하므로 호출하는 쪽에서 대체 표시를 그려야 합니다.
   */
  public static BufferedImage petImage(String spriteKey, PetMood mood) {
    PetAppearance appearance = appearanceOf(spriteKey);
    PetMood safeMood = mood == null ? PetMood.IDLE : mood;
    return PET_IMAGES.computeIfAbsent(appearance, key -> new EnumMap<>(PetMood.class))
        .computeIfAbsent(safeMood, key -> loadImage("/" + appearance.getImagePath(key)))
        .orElse(null);
  }

  private static Optional<BufferedImage> loadImage(String resourcePath) {
    URL resource = UiStyle.class.getResource(resourcePath);
    if (resource == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(ImageIO.read(resource));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static ImageIcon petIcon(String spriteKey, int size) {
    PetAppearance appearance = appearanceOf(spriteKey);
    return PET_ICONS.computeIfAbsent(appearance.name() + "@" + size, key -> {
      BufferedImage image = petImage(spriteKey, PetMood.IDLE);
      return image == null ? null
          : new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    });
  }

  public static Color categoryColor(Category category) {
    return switch (category) {
      case SCIENCE -> new Color(0x3D8BFD);
      case COMMON -> new Color(0x06B6D4);
      case SPORTS -> new Color(0xE8467C);
      case LANGUAGE -> new Color(0x8B5CF6);
      case HISTORY -> new Color(0x10A37F);
    };
  }

  public static JLabel label(String text, Font font) {
    JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setFont(font);
    label.setAlignmentX(Component.CENTER_ALIGNMENT);
    return label;
  }

  public static JButton button(String text) {
    JButton button = new JButton(text);
    button.setFont(BODY_FONT);
    button.setFocusPainted(false);
    button.setAlignmentX(Component.CENTER_ALIGNMENT);
    return button;
  }

  public static JLabel createPetBox() {
    JLabel box = new JLabel("", SwingConstants.CENTER);
    Dimension size = new Dimension(180, 180);
    box.setPreferredSize(size);
    box.setMaximumSize(size);
    box.setOpaque(true);
    box.setFont(SUBTITLE_FONT);
    box.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
    box.setAlignmentX(Component.CENTER_ALIGNMENT);
    return box;
  }

  public static void showPet(JLabel box, String spriteKey) {
    int index = indexOfPet(spriteKey);
    box.setBackground(PET_COLORS[index]);
    box.setText(PET_LABELS[index]);
    box.setIcon(petIcon(spriteKey, 160));
    box.setHorizontalTextPosition(SwingConstants.CENTER);
    box.setVerticalTextPosition(SwingConstants.BOTTOM);
  }

  private static int indexOfPet(String spriteKey) {
    for (int i = 0; i < PET_KEYS.length; i++) {
      if (PET_KEYS[i].equals(spriteKey)) {
        return i;
      }
    }
    return 0;
  }
}
