package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.ImageButton;
import main.java.com.quizpet.ui.component.PetStatusPanel;

/** 메인(홈) 화면(UI-03): 왼쪽 캐릭터 상태 사이드바와 오른쪽 학습 안내 배너로 구성합니다. */
public class HomePanel extends JPanel {
  private static final int EXP_PER_LEVEL = Pet.XP_PER_LEVEL;

  private Runnable onLearn;
  private Runnable onHistory;
  private Runnable onChangeUser;

  private final PetStatusPanel status = new PetStatusPanel();
  private final JLabel greetingLabel = new JLabel(" ");
  private final JLabel headlineLabel = new JLabel(" ");
  private final JLabel rewardLabel = new JLabel("처음 맞힌 문제마다 EXP +10, 해당 분야 능력치 +1");

  public HomePanel(ScreenManager screens) {
    onLearn = () -> screens.show(ScreenManager.CATEGORY);
    onHistory = () -> screens.show(ScreenManager.HISTORY);
    onChangeUser = () -> screens.show(ScreenManager.START);
    setLayout(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);

    add(status, BorderLayout.WEST);
    add(createContent(), BorderLayout.CENTER);
  }

  public void setOnLearn(Runnable action) { onLearn = action; }

  public void setOnHistory(Runnable action) { onHistory = action; }

  public void setOnChangeUser(Runnable action) { onChangeUser = action; }

  public void showPet(String petName, String spriteKey, int level, int expInLevel,
      int str, int intel, int foc, int wis) {
    status.showPet(petName, spriteKey, level, expInLevel, EXP_PER_LEVEL, str, intel, foc, wis);
    status.setMood(PetMood.IDLE, PetStatusPanel.DEFAULT_MESSAGE);
    int remain = EXP_PER_LEVEL - Math.max(0, Math.min(expInLevel, EXP_PER_LEVEL));
    headlineLabel.setText("<html>" + escape(petName) + subjectParticle(petName) + " Lv." + (level + 1)
        + "까지 <font color='#FFC233'>" + remain + " EXP</font> 남았어요</html>");
  }

  /** 배너 첫 줄 인사말입니다. nickname이 없으면 기본 인사를 표시합니다. */
  public void setGreeting(String nickname) {
    greetingLabel.setText(nickname == null || nickname.isBlank()
        ? "안녕하세요!" : nickname + " 님, 안녕하세요!");
  }

  /** 누적 정답률(0~100)과 푼 문제 수를 사이드바 아래에 표시합니다. */
  public void showSummary(double accuracy, int solved) {
    status.showSummary(accuracy, solved);
  }

  public PetStatusPanel getStatusPanel() {
    return status;
  }

  public String getHeadlineText() {
    return headlineLabel.getText();
  }

  /** 이름 마지막 글자에 받침이 있으면 "이", 없으면 "가"를 붙입니다. 한글이 아니면 "가"를 씁니다. */
  static String subjectParticle(String name) {
    if (name == null || name.isEmpty()) {
      return "가";
    }
    char last = name.charAt(name.length() - 1);
    if (last < 0xAC00 || last > 0xD7A3) {
      return "가";
    }
    return (last - 0xAC00) % 28 == 0 ? "가" : "이";
  }

  private static String escape(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private JPanel createContent() {
    JPanel content = new JPanel(new GridBagLayout());
    content.setOpaque(false);
    content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTH;

    c.gridy = 0;
    content.add(createBanner(), c);

    c.gridy = 1;
    c.insets = new Insets(20, 0, 0, 0);
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    actions.setOpaque(false);
    JButton historyButton = UiStyle.button("학습 기록");
    historyButton.addActionListener(e -> onHistory.run());
    JButton changeUserButton = UiStyle.button("사용자 변경");
    changeUserButton.addActionListener(e -> onChangeUser.run());
    actions.add(historyButton);
    actions.add(javax.swing.Box.createHorizontalStrut(12));
    actions.add(changeUserButton);
    content.add(actions, c);

    c.gridy = 2;
    c.weighty = 1;
    content.add(javax.swing.Box.createGlue(), c);
    setGreeting(null);
    return content;
  }

  private JPanel createBanner() {
    JPanel banner = new JPanel(new BorderLayout(24, 0)) {
      @Override
      protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(UiStyle.TEXT);
        g.fillRoundRect(0, 4, getWidth(), getHeight() - 4, 22, 22);
        g.setColor(UiStyle.INK_PANEL);
        g.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 22, 22);
        g.setColor(new Color(255, 255, 255, 14));
        g.fillOval(getWidth() - getHeight() * 3 / 2, -getHeight() / 3,
            getHeight() * 2, getHeight() * 2);
        g.dispose();
      }
    };
    banner.setOpaque(false);
    banner.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

    greetingLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
    greetingLabel.setForeground(UiStyle.EXP);
    headlineLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
    headlineLabel.setForeground(Color.WHITE);
    rewardLabel.setFont(UiStyle.BODY_FONT);
    rewardLabel.setForeground(new Color(0xD8D3E6));

    JPanel texts = new JPanel(new GridBagLayout());
    texts.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.anchor = GridBagConstraints.WEST;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    texts.add(greetingLabel, c);
    c.insets = new Insets(6, 0, 8, 0);
    texts.add(headlineLabel, c);
    c.insets = new Insets(0, 0, 0, 0);
    texts.add(rewardLabel, c);
    banner.add(texts, BorderLayout.CENTER);

    ImageButton learnButton = new ImageButton("학습 시작하기 →");
    learnButton.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
    learnButton.setForeground(Color.WHITE);
    learnButton.setPreferredSize(new Dimension(200, 62));
    learnButton.addActionListener(e -> onLearn.run());
    JPanel buttonHolder = new JPanel(new GridBagLayout());
    buttonHolder.setOpaque(false);
    buttonHolder.add(learnButton);
    banner.add(buttonHolder, BorderLayout.EAST);
    return banner;
  }
}
