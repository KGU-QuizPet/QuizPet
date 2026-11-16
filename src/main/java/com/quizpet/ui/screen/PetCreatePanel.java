package main.java.com.quizpet.ui.screen;

import java.awt.Dimension;
import java.awt.FlowLayout;
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
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class PetCreatePanel extends JPanel {
  private Runnable onBack;
  private BiConsumer<String, String> onCreate = (name, key) -> { };

  private final JLabel ownerLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JLabel petBox = UiStyle.createPetBox();
  private final JTextField nameField = new JTextField(12);
  private final JToggleButton[] lookButtons = new JToggleButton[UiStyle.PET_KEYS.length];

  private String selectedKey = UiStyle.PET_KEYS[0];

  public PetCreatePanel(ScreenManager screens, HomePanel homePanel) {
    this(screens);
  }

  public PetCreatePanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    onBack = () -> screens.show(ScreenManager.START);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

    add(UiStyle.label("캐릭터 생성", UiStyle.TITLE_FONT));
    add(Box.createVerticalStrut(5));
    add(ownerLabel);
    add(Box.createVerticalStrut(20));

    add(petBox);
    add(Box.createVerticalStrut(20));

    JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
    nameRow.setOpaque(false);
    JLabel nameLabel = new JLabel("캐릭터 이름");
    nameLabel.setFont(UiStyle.BODY_FONT);
    nameField.setFont(UiStyle.BODY_FONT);
    nameRow.add(nameLabel);
    nameRow.add(nameField);
    nameRow.setMaximumSize(new Dimension(400, 45));
    add(nameRow);
    add(Box.createVerticalStrut(10));

    add(UiStyle.label("외형 선택", UiStyle.SUBTITLE_FONT));
    JPanel lookRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
    lookRow.setOpaque(false);
    ButtonGroup group = new ButtonGroup();
    for (int i = 0; i < UiStyle.PET_KEYS.length; i++) {
      String key = UiStyle.PET_KEYS[i];
      JToggleButton button = new JToggleButton(UiStyle.PET_LABELS[i]);
      button.setFont(UiStyle.BODY_FONT);
      button.setFocusPainted(false);
      button.addActionListener(e -> selectLook(key));
      group.add(button);
      lookRow.add(button);
      lookButtons[i] = button;
    }
    lookRow.setMaximumSize(new Dimension(400, 50));
    add(lookRow);
    add(Box.createVerticalStrut(10));

    add(UiStyle.label("초기 상태: Lv 1  /  EXP 0/50  /  능력치 각 10", UiStyle.BODY_FONT));
    add(Box.createVerticalStrut(25));
    JButton createButton = UiStyle.button("생성하기");
    createButton.addActionListener(e -> onCreatePet());
    add(createButton);
    add(Box.createVerticalStrut(12));
    JButton backButton = UiStyle.button("사용자 선택으로");
    backButton.addActionListener(e -> onBack.run());
    add(backButton);

    prepare(null);
  }

  public void prepare(String nickname) {
    ownerLabel.setText(nickname == null ? "" : nickname + " 님의 캐릭터를 만들어 주세요");
    nameField.setText("");
    lookButtons[0].setSelected(true);
    selectLook(UiStyle.PET_KEYS[0]);
  }

  private void selectLook(String spriteKey) {
    selectedKey = spriteKey;
    UiStyle.showPet(petBox, spriteKey);
  }

  public void setOnBack(Runnable action) {
    onBack = Objects.requireNonNull(action);
  }

  public void setOnCreate(BiConsumer<String, String> action) {
    onCreate = Objects.requireNonNull(action);
  }

  private void onCreatePet() {
    String petName = nameField.getText().strip();
    if (petName.isEmpty() || petName.codePointCount(0, petName.length()) > 12) {
      JOptionPane.showMessageDialog(this, "캐릭터 이름은 1~12자로 입력해 주세요.");
      return;
    }
    onCreate.accept(petName, selectedKey);
  }
}
