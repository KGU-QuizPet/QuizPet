package main.java.com.quizpet.ui.component;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.UiStyle;

public final class TopBar extends JPanel {
  public static final int HEIGHT = 64;

  public static final String TAB_HOME = "HOME";
  public static final String TAB_LEARN = "LEARN";
  public static final String TAB_HISTORY = "HISTORY";
  public static final String TAB_WRONG_NOTE = "WRONG_NOTE";
  public static final String TAB_BATTLE = "BATTLE";

  private static final Color BRAND_SOFT = new Color(0xFFF0DC);
  private static final Color BRAND_DARK = new Color(0xD96F00);
  private static final Font TAB_FONT = new Font(Font.DIALOG, Font.BOLD, 16);
  private static final Font LOGO_FONT = new Font(Font.DIALOG, Font.BOLD, 24);

  private final Map<String, JToggleButton> tabs = new LinkedHashMap<>();
  private final ButtonGroup group = new ButtonGroup();
  private final JLabel nicknameLabel = new JLabel(" ");
  private final PetAvatar avatar = new PetAvatar();
  private Consumer<String> onTab = tab -> { };
  private Runnable onChangeUser = () -> { };

  public TopBar() {
    super(new BorderLayout());
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 2, 0, UiStyle.BORDER),
        BorderFactory.createEmptyBorder(0, 24, 0, 24)));
    setPreferredSize(new Dimension(0, HEIGHT));

    add(createLogo(), BorderLayout.WEST);
    add(createTabs(), BorderLayout.CENTER);
    add(createUserChip(), BorderLayout.EAST);
  }

  public void setOnTab(Consumer<String> action) {
    onTab = action;
  }

  public void setOnChangeUser(Runnable action) {
    onChangeUser = action;
  }

  public void select(String tab) {
    JToggleButton button = tabs.get(tab);
    if (button == null) {
      group.clearSelection();
    } else {
      button.setSelected(true);
    }
    repaint();
  }

  public String getSelectedTab() {
    for (Map.Entry<String, JToggleButton> entry : tabs.entrySet()) {
      if (entry.getValue().isSelected()) {
        return entry.getKey();
      }
    }
    return null;
  }

  public void setTabEnabled(String tab, boolean enabled) {
    JToggleButton button = tabs.get(tab);
    button.setEnabled(enabled);
    button.setToolTipText(enabled ? null : "준비 중인 화면입니다");
  }

  public void setUser(String nickname, String spriteKey) {
    nicknameLabel.setText(nickname == null || nickname.isBlank() ? " " : nickname);
    avatar.setSpriteKey(spriteKey);
  }

  public JToggleButton getTab(String tab) {
    return tabs.get(tab);
  }

  private JComponent createLogo() {
    JLabel logo = new JLabel("QuizPet");
    logo.setFont(LOGO_FONT);
    logo.setForeground(UiStyle.TEXT);
    logo.setIcon(UiStyle.logoIcon(52));
    logo.setIconTextGap(10);
    logo.setPreferredSize(new Dimension(272, HEIGHT));
    return logo;
  }

  private JComponent createTabs() {
    JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 12));
    holder.setOpaque(false);
    addTab(holder, TAB_HOME, "홈");
    addTab(holder, TAB_LEARN, "학습하기");
    addTab(holder, TAB_HISTORY, "학습 기록");
    addTab(holder, TAB_WRONG_NOTE, "오답노트");
    addTab(holder, TAB_BATTLE, "보스 결투");
    return holder;
  }

  private void addTab(JPanel holder, String key, String text) {
    JToggleButton tab = new TabButton(text, key);
    tab.addActionListener(e -> onTab.accept(key));
    tabs.put(key, tab);
    group.add(tab);
    holder.add(tab);
  }

  private JComponent createUserChip() {
    JPanel chip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)) {
      @Override
      protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UiStyle.BACKGROUND);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g.setColor(UiStyle.BORDER);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 2, getHeight() - 2);
        g.dispose();
      }
    };
    chip.setOpaque(false);
    chip.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 12));
    nicknameLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
    nicknameLabel.setForeground(UiStyle.TEXT);
    JButton change = new JButton("사용자 변경");
    change.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
    change.setForeground(UiStyle.MUTED);
    change.setContentAreaFilled(false);
    change.setBorderPainted(false);
    change.setFocusPainted(false);
    change.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    change.addActionListener(e -> onChangeUser.run());
    chip.add(avatar);
    chip.add(nicknameLabel);
    chip.add(change);

    JPanel holder = new JPanel(new GridBagLayout());
    holder.setOpaque(false);
    holder.add(chip);
    return holder;
  }

  private static final class TabButton extends JToggleButton {
    private final String key;

    TabButton(String text, String key) {
      super(text);
      this.key = key;
      setFont(TAB_FONT);
      setFocusPainted(false);
      setContentAreaFilled(false);
      setBorderPainted(false);
      setRolloverEnabled(true);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 16));
      setPreferredSize(new Dimension(getFontMetrics(TAB_FONT).stringWidth(text) + 60, 40));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color text;
      if (!isEnabled()) {
        text = UiStyle.HINT;
      } else if (isSelected()) {
        g.setColor(BRAND_SOFT);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        text = BRAND_DARK;
      } else if (getModel().isRollover()) {
        g.setColor(UiStyle.BACKGROUND);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        text = UiStyle.TEXT;
      } else {
        text = UiStyle.MUTED;
      }
      paintTabIcon(g, 16, (getHeight() - 18) / 2, text);
      g.setColor(text);
      g.setFont(getFont());
      java.awt.FontMetrics metrics = g.getFontMetrics();
      g.drawString(getText(), 42, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
      g.dispose();
    }

    private void paintTabIcon(Graphics2D graphics, int x, int y, Color color) {
      Graphics2D g = (Graphics2D) graphics.create(x, y, 18, 18);
      g.setColor(color);
      g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      switch (key) {
        case TAB_HOME -> {
          g.fillPolygon(new int[] {1, 9, 17}, new int[] {8, 1, 8}, 3);
          g.fillRect(3, 8, 12, 9);
          g.setColor(Color.WHITE);
          g.fillRect(7, 11, 4, 6);
        }
        case TAB_LEARN -> {
          g.fillRoundRect(1, 2, 7, 14, 3, 3);
          g.fillRoundRect(10, 2, 7, 14, 3, 3);
        }
        case TAB_HISTORY -> {
          g.fillRect(2, 10, 3, 7);
          g.fillRect(7, 6, 3, 11);
          g.fillRect(12, 2, 3, 15);
        }
        case TAB_WRONG_NOTE -> {
          g.drawRoundRect(2, 1, 13, 16, 3, 3);
          g.drawLine(5, 6, 12, 6);
          g.drawLine(5, 10, 12, 10);
          g.drawLine(5, 14, 9, 14);
        }
        case TAB_BATTLE -> {
          g.drawLine(3, 15, 15, 3);
          g.drawLine(11, 3, 15, 3);
          g.drawLine(15, 3, 15, 7);
          g.drawLine(2, 11, 7, 16);
        }
        default -> { }
      }
      g.dispose();
    }
  }

  private static final class PetAvatar extends JComponent {
    private String spriteKey = "DUCK";

    PetAvatar() {
      setPreferredSize(new Dimension(32, 32));
    }

    void setSpriteKey(String key) {
      spriteKey = key == null ? "DUCK" : key;
      repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setColor(new Color(0xFFE7C2));
      g.fillOval(0, 0, 32, 32);
      BufferedImage image = UiStyle.petImage(spriteKey, PetMood.IDLE);
      if (image != null) {
        g.clipRect(0, 0, 32, 32);
        g.drawImage(image, 1, 1, 30, 30, null);
      }
      g.dispose();
    }
  }
}
