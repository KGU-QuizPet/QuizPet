package main.java.com.quizpet.ui.screen;

import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.ListSelectionModel;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class StartPanel extends JPanel {
  private final ScreenManager screens;
  private final PetCreatePanel petCreatePanel;
  private final HomePanel homePanel;

  private final DefaultListModel<DummyUser> userListModel = new DefaultListModel<>();
  private final JList<DummyUser> userList = new JList<>(userListModel);
  private final JTextField nicknameField = new JTextField(12);

  public StartPanel(ScreenManager screens, PetCreatePanel petCreatePanel, HomePanel homePanel) {
    this.screens = screens;
    this.petCreatePanel = petCreatePanel;
    this.homePanel = homePanel;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

    add(UiStyle.label("QuizPet", UiStyle.TITLE_FONT));
    add(UiStyle.label("퀴즈로 성장하는 나만의 캐릭터", UiStyle.BODY_FONT));
    add(Box.createVerticalStrut(30));

    add(UiStyle.label("기존 사용자 선택", UiStyle.SUBTITLE_FONT));
    add(Box.createVerticalStrut(10));
    userList.setFont(UiStyle.BODY_FONT);
    userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane listScroll = new JScrollPane(userList);
    listScroll.setPreferredSize(new Dimension(360, 200));
    listScroll.setMaximumSize(new Dimension(360, 200));
    add(listScroll);
    add(Box.createVerticalStrut(10));
    JButton startButton = UiStyle.button("시작");
    startButton.addActionListener(e -> onStart());
    add(startButton);
    add(Box.createVerticalStrut(40));

    add(UiStyle.label("새 사용자", UiStyle.SUBTITLE_FONT));
    JPanel nicknameRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
    nicknameRow.setOpaque(false);
    JLabel nicknameLabel = new JLabel("닉네임");
    nicknameLabel.setFont(UiStyle.BODY_FONT);
    nicknameField.setFont(UiStyle.BODY_FONT);
    nicknameRow.add(nicknameLabel);
    nicknameRow.add(nicknameField);
    nicknameRow.setMaximumSize(new Dimension(360, 45));
    add(nicknameRow);
    JButton createButton = UiStyle.button("새 사용자 만들기");
    createButton.addActionListener(e -> onCreateUser());
    nicknameField.addActionListener(e -> onCreateUser());
    add(createButton);

    loadUsers();
  }

  private void loadUsers() {
    userListModel.clear();
    userListModel.addElement(new DummyUser("수빈", "뭉치", "PET_A", 4, 20, 15, 20, 25, 40));
    userListModel.addElement(new DummyUser("민수", "초록이", "PET_B", 3, 10, 12, 18, 11, 14));
    userListModel.addElement(new DummyUser("지우", null, null, 0, 0, 0, 0, 0, 0));
  }

  private void onStart() {
    DummyUser user = userList.getSelectedValue();
    if (user == null) {
      JOptionPane.showMessageDialog(this, "사용자를 먼저 선택해 주세요.");
      return;
    }
    if (user.hasPet()) {
      homePanel.showPet(user.petName, user.spriteKey, user.level, user.expInLevel,
          user.str, user.intel, user.foc, user.wis);
      screens.show(ScreenManager.HOME);
    } else {
      petCreatePanel.prepare(user.nickname);
      screens.show(ScreenManager.PET_CREATE);
    }
  }

  private void onCreateUser() {
    String nickname = nicknameField.getText().trim();
    if (nickname.isEmpty() || nickname.length() > 12) {
      JOptionPane.showMessageDialog(this, "닉네임은 1~12자로 입력해 주세요.");
      return;
    }
    if (isDuplicateNickname(nickname)) {
      JOptionPane.showMessageDialog(this, "이미 있는 닉네임입니다. 다른 닉네임을 입력해 주세요.");
      return;
    }
    userListModel.addElement(new DummyUser(nickname, null, null, 0, 0, 0, 0, 0, 0));
    nicknameField.setText("");

    petCreatePanel.prepare(nickname);
    screens.show(ScreenManager.PET_CREATE);
  }

  private boolean isDuplicateNickname(String nickname) {
    for (int i = 0; i < userListModel.size(); i++) {
      if (userListModel.get(i).nickname.equals(nickname)) {
        return true;
      }
    }
    return false;
  }

  private static class DummyUser {
    final String nickname;
    final String petName;
    final String spriteKey;
    final int level;
    final int expInLevel;
    final int str;
    final int intel;
    final int foc;
    final int wis;

    DummyUser(String nickname, String petName, String spriteKey, int level, int expInLevel,
        int str, int intel, int foc, int wis) {
      this.nickname = nickname;
      this.petName = petName;
      this.spriteKey = spriteKey;
      this.level = level;
      this.expInLevel = expInLevel;
      this.str = str;
      this.intel = intel;
      this.foc = foc;
      this.wis = wis;
    }

    boolean hasPet() {
      return petName != null;
    }

    @Override
    public String toString() {
      if (!hasPet()) {
        return nickname + "  (캐릭터 없음)";
      }
      return nickname + "  (" + petName + ")      Lv " + level;
    }
  }
}

