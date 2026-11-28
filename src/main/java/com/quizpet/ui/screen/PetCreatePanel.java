package main.java.com.quizpet.ui.screen;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import main.java.com.quizpet.model.PetAppearance;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.model.PetStats;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.AuthHero;
import main.java.com.quizpet.ui.component.AuthHero.HeroPet;
import main.java.com.quizpet.ui.component.Badge;
import main.java.com.quizpet.ui.component.ImageButton;

/** UI-02 캐릭터 생성 화면입니다. 이름을 입력하면 왼쪽 이름표가, 외형을 고르면 왼쪽 배경색과 펫이 바로 바뀝니다. */
public class PetCreatePanel extends JPanel {
  private static final String INTRO = "이름과 외형을 정하면 Lv.1부터 함께 성장해요.";
  private static final Font CAPTION_FONT = UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f);
  private static final Font CHIP_FONT = UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f);

  private Runnable onBack;
  private BiConsumer<String, String> onCreate = (name, key) -> { };

  private final AuthHero hero;
  private final JLabel intro = AuthHero.text(INTRO, UiStyle.BODY_FONT, UiStyle.MUTED);
  private final JTextField nameField = new JTextField();
  private final JLabel counter = new JLabel();
  private final JToggleButton[] lookButtons = new JToggleButton[PetAppearance.values().length];
  private final JButton createButton = new ImageButton("이 친구로 시작하기");

  private PetAppearance selected = PetAppearance.DUCK;

  public PetCreatePanel(ScreenManager screens, HomePanel homePanel) {
    this(screens);
  }

  public PetCreatePanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    onBack = () -> screens.show(ScreenManager.START);
    setLayout(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);

    JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.add(new Badge("STEP 2 / 2", new Color(255, 255, 255, 150), UiStyle.TEXT, CHIP_FONT, 0));
    header.add(Box.createVerticalStrut(18));
    header.add(AuthHero.text("<html>새 친구를<br>만나볼까요?</html>",
        new Font(Font.DIALOG, Font.BOLD, 44), UiStyle.TEXT));
    hero = new AuthHero(heroColor(selected), groundColor(selected), header);
    add(hero, BorderLayout.WEST);

    JPanel column = AuthHero.column();
    column.add(AuthHero.text("캐릭터 생성", new Font(Font.DIALOG, Font.BOLD, 32), UiStyle.TEXT));
    column.add(Box.createVerticalStrut(10));
    column.add(intro);
    column.add(Box.createVerticalStrut(24));

    column.add(AuthHero.text("캐릭터 이름", CAPTION_FONT, UiStyle.MUTED));
    column.add(Box.createVerticalStrut(8));
    JPanel nameFrame = new JPanel(new BorderLayout(10, 0));
    nameField.setFont(UiStyle.BODY_FONT.deriveFont(16f));
    nameField.setBorder(BorderFactory.createEmptyBorder());
    nameField.setOpaque(false);
    nameField.addActionListener(e -> onCreatePet());
    nameField.getAccessibleContext().setAccessibleName("캐릭터 이름");
    counter.setFont(UiStyle.BODY_FONT.deriveFont(13f));
    nameFrame.add(nameField, BorderLayout.CENTER);
    nameFrame.add(counter, BorderLayout.EAST);
    AuthHero.styleInput(nameField, nameFrame);
    nameFrame.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameFrame.getPreferredSize().height));
    nameFrame.setAlignmentX(Component.LEFT_ALIGNMENT);
    AuthHero.onChange(nameField, this::nameChanged);
    column.add(nameFrame);
    column.add(Box.createVerticalStrut(22));

    column.add(AuthHero.text("외형 선택", CAPTION_FONT, UiStyle.MUTED));
    column.add(Box.createVerticalStrut(8));
    JPanel looks = new JPanel(new GridLayout(1, lookButtons.length, 16, 0));
    looks.setOpaque(false);
    ButtonGroup group = new ButtonGroup();
    for (PetAppearance appearance : PetAppearance.values()) {
      JToggleButton button = new LookCard(appearance);
      button.addActionListener(e -> selectLook(appearance));
      group.add(button);
      looks.add(button);
      lookButtons[appearance.ordinal()] = button;
    }
    looks.setMaximumSize(new Dimension(Integer.MAX_VALUE, LookCard.HEIGHT));
    looks.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(looks);
    column.add(Box.createVerticalStrut(22));

    column.add(AuthHero.text("초기 상태", CAPTION_FONT, UiStyle.MUTED));
    column.add(Box.createVerticalStrut(8));
    JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    chips.setOpaque(false);
    chips.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
    chips.add(new Badge("Lv.1", UiStyle.INK_PANEL, Color.WHITE, CHIP_FONT, 0));
    chips.add(new Badge("EXP 0", UiStyle.SUNKEN, UiStyle.MUTED, CHIP_FONT, 0));
    for (String stat : UiStyle.STAT_TYPES) {
      chips.add(new Badge(stat + " " + PetStats.INITIAL_VALUE, UiStyle.SUNKEN, UiStyle.statColor(stat),
          CHIP_FONT, 0));
    }
    chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, chips.getPreferredSize().height));
    chips.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(chips);
    column.add(Box.createVerticalStrut(40));

    JPanel actions = new JPanel(new BorderLayout(16, 0));
    actions.setOpaque(false);
    JButton backButton = new ImageButton("이전");
    backButton.setBackground(Color.WHITE);
    backButton.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 17f));
    backButton.setPreferredSize(new Dimension(106, 62));
    backButton.addActionListener(e -> onBack.run());
    createButton.setForeground(Color.WHITE);
    createButton.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 17f));
    createButton.addActionListener(e -> onCreatePet());
    actions.add(backButton, BorderLayout.WEST);
    actions.add(createButton, BorderLayout.CENTER);
    actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
    actions.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(actions);

    add(AuthHero.formArea(column), BorderLayout.CENTER);
    prepare(null);
  }

  public void prepare(String nickname) {
    intro.setText(nickname == null ? INTRO : nickname + " 님, " + INTRO);
    nameField.setText("");
    lookButtons[0].setSelected(true);
    selectLook(PetAppearance.values()[0]);
    nameChanged();
  }

  private void selectLook(PetAppearance appearance) {
    selected = appearance;
    hero.setColors(heroColor(appearance), groundColor(appearance));
    hero.setPets(new HeroPet(appearance.getSpriteKey(), PetMood.HAPPY, 270));
    nameChanged();
  }

  private void nameChanged() {
    String name = nameField.getText().strip();
    int length = name.codePointCount(0, name.length());
    counter.setText(length + " / " + User.MAX_NAME_LENGTH);
    counter.setForeground(length > User.MAX_NAME_LENGTH ? UiStyle.WRONG : UiStyle.HINT);
    hero.setNameTag(name.isEmpty() ? selected.getDisplayName() : name);
    createButton.setEnabled(!name.isEmpty());
  }

  public void setOnBack(Runnable action) {
    onBack = Objects.requireNonNull(action);
  }

  public void setOnCreate(BiConsumer<String, String> action) {
    onCreate = Objects.requireNonNull(action);
  }

  private void onCreatePet() {
    String petName = nameField.getText().strip();
    if (petName.isEmpty() || petName.codePointCount(0, petName.length()) > User.MAX_NAME_LENGTH) {
      JOptionPane.showMessageDialog(this, "캐릭터 이름은 1~" + User.MAX_NAME_LENGTH + "자로 입력해 주세요.");
      return;
    }
    onCreate.accept(petName, selected.getSpriteKey());
  }

  private static Color heroColor(PetAppearance appearance) {
    return switch (appearance) {
      case DUCK -> new Color(0xFFC93C);
      case TURTLE -> new Color(0xC6EFCB);
      case LIZARD -> new Color(0xFFD6C8);
    };
  }

  private static Color groundColor(PetAppearance appearance) {
    return switch (appearance) {
      case DUCK -> new Color(0xF2B42A);
      case TURTLE -> new Color(0xA3DFAC);
      case LIZARD -> new Color(0xF9B9A4);
    };
  }

  private static String description(PetAppearance appearance) {
    return switch (appearance) {
      case DUCK -> "오리 · 노랑";
      case TURTLE -> "거북이 · 초록";
      case LIZARD -> "도마뱀 · 코랄";
    };
  }

  /** 외형 선택 카드입니다. 선택되면 브랜드색 테두리와 오른쪽 위 체크 원을 표시하고 펫이 웃습니다. */
  private static final class LookCard extends JToggleButton {
    static final int HEIGHT = 196;
    private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 17);
    private static final Font DESC_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);

    private final PetAppearance appearance;

    LookCard(PetAppearance appearance) {
      super(appearance.getDisplayName());
      this.appearance = appearance;
      getAccessibleContext().setAccessibleDescription(description(appearance));
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(150, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      boolean on = isSelected();
      int width = getWidth() - 2;
      int height = getHeight() - 2;
      Color border = on ? UiStyle.BRAND : hasFocus() ? UiStyle.TEXT : UiStyle.BORDER;
      g.setColor(on ? UiStyle.BRAND.darker() : UiStyle.SHADOW);
      g.fillRoundRect(1, 6, width, height - 6, 20, 20);
      g.setColor(on ? new Color(0xFFFBF3) : Color.WHITE);
      g.fillRoundRect(1, 1, width, height - 6, 20, 20);
      g.setColor(border);
      g.setStroke(new BasicStroke(2));
      g.drawRoundRect(1, 1, width - 1, height - 7, 20, 20);

      int imageSize = Math.min(110, height - 90);
      BufferedImage image = UiStyle.petImage(appearance.getSpriteKey(), on ? PetMood.HAPPY : PetMood.IDLE);
      if (image != null) {
        g.drawImage(image, (getWidth() - imageSize) / 2, 18, imageSize, imageSize, null);
      }
      drawCentered(g, appearance.getDisplayName(), NAME_FONT, UiStyle.TEXT, height - 46);
      drawCentered(g, description(appearance), DESC_FONT, UiStyle.HINT, height - 22);

      if (on) {
        int x = width - 34;
        g.setColor(UiStyle.BRAND);
        g.fillOval(x, 10, 24, 24);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[] {x + 7, x + 11, x + 18}, new int[] {22, 26, 17}, 3);
      }
      g.dispose();
    }

    private void drawCentered(Graphics2D g, String text, Font font, Color color, int baseline) {
      g.setFont(font);
      g.setColor(color);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, baseline);
    }
  }
}
