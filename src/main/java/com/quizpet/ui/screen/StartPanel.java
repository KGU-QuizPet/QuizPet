package main.java.com.quizpet.ui.screen;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import main.java.com.quizpet.model.Profile;
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
  private Consumer<String> onSelect = id -> { };
  private Consumer<String> onCreate = nickname -> { };

  private final DefaultListModel<Profile> userListModel = new DefaultListModel<>();
  private final JList<Profile> userList = new JList<>(userListModel);
  private final JTextField nicknameField = new JTextField(12);

  public StartPanel(ScreenManager screens, PetCreatePanel petCreatePanel, HomePanel homePanel) {
    this(screens);
  }

  public StartPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");

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
    userList.setVisibleRowCount(6);
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
    nicknameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, nicknameRow.getPreferredSize().height));
    add(nicknameRow);
    JButton createButton = UiStyle.button("새 사용자 만들기");
    createButton.addActionListener(e -> onCreateUser());
    nicknameField.addActionListener(e -> onCreateUser());
    add(createButton);

  }

  public void showProfiles(List<Profile> profiles) {
    userListModel.clear();
    profiles.forEach(userListModel::addElement);
    userList.clearSelection();
    nicknameField.setText("");
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
    if (nickname.isEmpty() || nickname.codePointCount(0, nickname.length()) > 12) {
      JOptionPane.showMessageDialog(this, "닉네임은 1~12자로 입력해 주세요.");
      return;
    }
    onCreate.accept(nickname);
  }
}
