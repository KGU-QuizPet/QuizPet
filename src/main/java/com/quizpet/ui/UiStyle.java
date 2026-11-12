package main.java.com.quizpet.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public final class UiStyle {
  public static final Color BACKGROUND = new Color(0xFFF8EE);
  public static final Color POINT = new Color(0x6C8EBF);

  public static final Font TITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 28);
  public static final Font SUBTITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 18);
  public static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 15);

  public static final String[] PET_KEYS = {"PET_A", "PET_B", "PET_C"};
  public static final String[] PET_LABELS = {"외형 1", "외형 2", "외형 3"};
  private static final Color[] PET_COLORS = {
    new Color(0xFFE08A), new Color(0x9ED9A0), new Color(0xF4A9A0)
  };

  private UiStyle() {}

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
