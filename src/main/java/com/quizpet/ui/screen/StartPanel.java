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
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.model.Profile;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.AuthHero;
import main.java.com.quizpet.ui.component.AuthHero.HeroPet;
import main.java.com.quizpet.ui.component.Badge;
import main.java.com.quizpet.ui.component.ImageButton;

/** UI-01 시작·사용자 선택 화면입니다. 행 더블클릭, Enter, 선택 행의 [입장]으로 들어갑니다. */
public class StartPanel extends JPanel {
  /** 사용자 목록에서 Enter를 눌렀을 때 실행되는 동작의 이름입니다. */
  public static final String ENTER_ACTION = "enter";
  private static final int ROW_HEIGHT = 90;
  private static final int ROW_GAP = 10;
  private static final int ENTER_BUTTON_WIDTH = 62;

  private Consumer<String> onSelect = id -> { };
  private Consumer<String> onCreate = nickname -> { };

  private final DefaultListModel<Profile> userListModel = new DefaultListModel<>();
  private final JList<Profile> userList = new JList<>(userListModel);
  private final JScrollPane listScroll = new JScrollPane(userList);
  private final JTextField nicknameField = new JTextField();
  private final JLabel greeting = AuthHero.text("", new Font(Font.DIALOG, Font.BOLD, 32), UiStyle.TEXT);
  private final JLabel guide = AuthHero.text("", UiStyle.BODY_FONT, UiStyle.MUTED);
  private final JButton createButton = new ImageButton("새 사용자 만들기");

  public StartPanel(ScreenManager screens, PetCreatePanel petCreatePanel, HomePanel homePanel) {
    this(screens);
  }

  public StartPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);
    add(createHero(), BorderLayout.WEST);

    JPanel column = AuthHero.column();
    column.add(greeting);
    column.add(Box.createVerticalStrut(10));
    column.add(guide);
    column.add(Box.createVerticalStrut(20));

    userList.setCellRenderer(new UserRow());
    userList.setFixedCellHeight(ROW_HEIGHT);
    userList.setVisibleRowCount(3);
    userList.setOpaque(false);
    userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), ENTER_ACTION);
    userList.getActionMap().put(ENTER_ACTION, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onStart();
      }
    });
    userList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int index = userList.locationToIndex(e.getPoint());
        if (index < 0 || !userList.getCellBounds(index, index).contains(e.getPoint())) {
          return;
        }
        // ponytail: 오른쪽 [입장] 영역 클릭은 막 선택한 행이어도 바로 입장합니다.
        if (e.getClickCount() == 2 || e.getX() >= userList.getWidth() - ENTER_BUTTON_WIDTH - 16) {
          onStart();
        }
      }
    });
    listScroll.setBorder(BorderFactory.createEmptyBorder());
    listScroll.setOpaque(false);
    listScroll.getViewport().setOpaque(false);
    listScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
    column.add(listScroll);
    column.add(Box.createVerticalStrut(16));
    column.add(new Divider("처음이신가요?"));
    column.add(Box.createVerticalStrut(16));

    column.add(AuthHero.text("닉네임", UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f), UiStyle.MUTED));
    column.add(Box.createVerticalStrut(8));
    nicknameField.setFont(UiStyle.BODY_FONT.deriveFont(16f));
    nicknameField.setToolTipText("1~" + User.MAX_NAME_LENGTH + "자");
    AuthHero.styleInput(nicknameField, nicknameField);
    nicknameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, nicknameField.getPreferredSize().height));
    nicknameField.setAlignmentX(Component.LEFT_ALIGNMENT);
    nicknameField.addActionListener(e -> onCreateUser());
    AuthHero.onChange(nicknameField,
        () -> createButton.setEnabled(!nicknameField.getText().isBlank()));
    column.add(nicknameField);
    column.add(Box.createVerticalStrut(22));

    createButton.setBackground(UiStyle.INK_PANEL);
    createButton.setForeground(Color.WHITE);
    createButton.setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 17f));
    createButton.setPreferredSize(new Dimension(320, 62));
    createButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
    createButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    createButton.addActionListener(e -> onCreateUser());
    column.add(createButton);

    add(AuthHero.formArea(column), BorderLayout.CENTER);
    showProfiles(List.of());
  }

  private static AuthHero createHero() {
    JPanel header = new JPanel();
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    JLabel logo = AuthHero.text("QuizPet", new Font(Font.DIALOG, Font.BOLD, 72), Color.WHITE);
    logo.setIcon(UiStyle.logoIcon(200));
    if (logo.getIcon() != null) logo.setText("");
    logo.getAccessibleContext().setAccessibleName("QuizPet");
    header.add(logo);
    header.add(Box.createVerticalStrut(8));
    header.add(AuthHero.text("퀴즈로 성장하는 나만의 캐릭터",
        UiStyle.BODY_FONT.deriveFont(Font.BOLD, 18f), Color.WHITE));
    header.add(Box.createVerticalStrut(18));
    JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    pills.setOpaque(false);
    pills.setAlignmentX(Component.LEFT_ALIGNMENT);
    for (Category category : Category.values()) {
      pills.add(new Badge(category.getName(), new Color(255, 255, 255, 60), Color.WHITE,
          UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f), 0));
    }
    // FlowLayout의 첫 칸 여백을 없애 제목과 왼쪽 선을 맞춥니다.
    pills.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
    header.add(pills);

    AuthHero hero = new AuthHero(UiStyle.BRAND, new Color(0xFFB44D), header);
    hero.setPets(new HeroPet("TURTLE", PetMood.IDLE, 150), new HeroPet("DUCK", PetMood.HAPPY, 190),
        new HeroPet("LIZARD", PetMood.IDLE, 150));
    return hero;
  }

  public void showProfiles(List<Profile> profiles) {
    userListModel.clear();
    profiles.forEach(userListModel::addElement);
    boolean empty = profiles.isEmpty();
    if (empty) {
      userList.clearSelection();
    } else {
      userList.setSelectedIndex(0);
    }
    listScroll.setVisible(!empty);
    greeting.setText(empty ? "QuizPet에 오신 걸 환영해요!" : "다시 만나서 반가워요!");
    guide.setText(empty ? "닉네임을 정하고 첫 캐릭터를 만나 보세요." : "이어서 키울 사용자를 선택하세요.");
    nicknameField.setText("");
    createButton.setEnabled(false);
    revalidate();
  }

  public void setOnSelect(Consumer<String> action) {
    onSelect = Objects.requireNonNull(action);
  }

  public void setOnCreate(Consumer<String> action) {
    onCreate = Objects.requireNonNull(action);
  }

  private void onStart() {
    Profile profile = userList.getSelectedValue();
    if (profile == null) {
      JOptionPane.showMessageDialog(this, "사용자를 먼저 선택해 주세요.");
      return;
    }
    onSelect.accept(profile.getUser().getUserId());
  }

  private void onCreateUser() {
    String nickname = nicknameField.getText().strip();
    if (nickname.isEmpty() || nickname.codePointCount(0, nickname.length()) > User.MAX_NAME_LENGTH) {
      JOptionPane.showMessageDialog(this, "닉네임은 1~" + User.MAX_NAME_LENGTH + "자로 입력해 주세요.");
      return;
    }
    onCreate.accept(nickname);
  }

  /** 사용자 한 명을 카드 모양으로 그립니다. 선택된 행에는 브랜드색 테두리와 [입장] 버튼을 표시합니다. */
  private static final class UserRow extends JPanel implements ListCellRenderer<Profile> {
    private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 18);
    private static final Font SUB_FONT = new Font(Font.DIALOG, Font.PLAIN, 13);
    private static final Font CHIP_FONT = new Font(Font.DIALOG, Font.BOLD, 14);

    private Profile profile;
    private boolean selected;

    UserRow() {
      setOpaque(false);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Profile> list, Profile value,
        int index, boolean isSelected, boolean cellHasFocus) {
      profile = value;
      selected = isSelected;
      return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      int width = getWidth() - 2;
      int height = getHeight() - ROW_GAP;
      Color border = selected ? UiStyle.BRAND : UiStyle.BORDER;
      g.setColor(selected ? UiStyle.BRAND.darker() : UiStyle.SHADOW);
      g.fillRoundRect(1, 4, width, height - 4, 20, 20);
      g.setColor(Color.WHITE);
      g.fillRoundRect(1, 1, width, height - 4, 20, 20);
      g.setColor(border);
      g.setStroke(new BasicStroke(2));
      g.drawRoundRect(1, 1, width - 1, height - 5, 20, 20);

      int avatarY = (height - 52) / 2;
      g.setColor(UiStyle.SUNKEN);
      g.fillRoundRect(16, avatarY, 52, 52, 14, 14);
      Pet pet = profile.getPet();
      if (pet != null) {
        BufferedImage image = UiStyle.petImage(pet.getSpriteKey(), PetMood.IDLE);
        if (image != null) {
          g.drawImage(image, 19, avatarY + 3, 46, 46, null);
        }
      }

      g.setColor(UiStyle.TEXT);
      g.setFont(NAME_FONT);
      g.drawString(profile.getUser().getNickname(), 86, height / 2 - 4);
      g.setColor(UiStyle.MUTED);
      g.setFont(SUB_FONT);
      g.drawString(pet == null ? "캐릭터 없음"
          : pet.getName() + " · " + pet.getAppearance().getDisplayName(), 86, height / 2 + 16);

      int right = width - 16;
      g.setFont(CHIP_FONT);
      FontMetrics metrics = g.getFontMetrics();
      if (selected) {
        int x = right - ENTER_BUTTON_WIDTH;
        int y = (height - 44) / 2;
        g.setColor(UiStyle.BRAND.darker());
        g.fillRoundRect(x, y + 4, ENTER_BUTTON_WIDTH, 40, 14, 14);
        g.setColor(UiStyle.BRAND);
        g.fillRoundRect(x, y, ENTER_BUTTON_WIDTH, 40, 14, 14);
        g.setColor(Color.WHITE);
        g.drawString("입장", x + (ENTER_BUTTON_WIDTH - metrics.stringWidth("입장")) / 2,
            y + (40 + metrics.getAscent() - metrics.getDescent()) / 2);
        right = x - 12;
      }
      if (pet != null) {
        String level = "Lv." + pet.getLevel();
        int chipWidth = metrics.stringWidth(level) + 20;
        int y = (height - 28) / 2;
        g.setColor(UiStyle.INK_PANEL);
        g.fillRoundRect(right - chipWidth, y, chipWidth, 28, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(level, right - chipWidth + 10, y + (28 + metrics.getAscent() - metrics.getDescent()) / 2);
      }
      g.dispose();
    }
  }

  /** 가운데 글자 양옆으로 가는 선을 긋는 구분선입니다. */
  private static final class Divider extends JLabel {
    Divider(String text) {
      super(text, CENTER);
      setFont(UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f));
      setForeground(UiStyle.HINT);
      setAlignmentX(Component.LEFT_ALIGNMENT);
      setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      int textWidth = getFontMetrics(getFont()).stringWidth(getText());
      int y = getHeight() / 2;
      int gap = textWidth / 2 + 12;
      graphics.setColor(UiStyle.BORDER);
      graphics.fillRect(0, y, Math.max(0, getWidth() / 2 - gap), 2);
      graphics.fillRect(getWidth() / 2 + gap, y, Math.max(0, getWidth() / 2 - gap), 2);
    }
  }
}
