package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class HomePanel extends JPanel {
  private static final int EXP_PER_LEVEL = 50;

  private final ScreenManager screens;

  private final JLabel petNameLabel = new JLabel();
  private final JLabel levelLabel = new JLabel("", SwingConstants.CENTER);
  private final JLabel expLabel = new JLabel("", SwingConstants.RIGHT);
  private final JProgressBar expBar = new JProgressBar(0, EXP_PER_LEVEL);

  private final JLabel petBox = UiStyle.createPetBox();
  private final JLabel strLabel = statLabel();
  private final JLabel intLabel = statLabel();
  private final JLabel focLabel = statLabel();
  private final JLabel wisLabel = statLabel();

  public HomePanel(ScreenManager screens) {
    this.screens = screens;
    setLayout(new BorderLayout());
    setBackground(UiStyle.BACKGROUND);

    add(createHeader(), BorderLayout.NORTH);
    add(createCenter(), BorderLayout.CENTER);
  }

  public void showPet(String petName, String spriteKey, int level, int expInLevel,
      int str, int intel, int foc, int wis) {
    petNameLabel.setText(petName);
    levelLabel.setText("Lv " + level);
    expLabel.setText("EXP " + expInLevel + "/" + EXP_PER_LEVEL);
    expBar.setValue(expInLevel);

    UiStyle.showPet(petBox, spriteKey);
    strLabel.setText("체력(STR)   " + str);
    intLabel.setText("지성(INT)   " + intel);
    focLabel.setText("집중력(FOC)   " + foc);
    wisLabel.setText("지혜(WIS)   " + wis);
  }

  private JPanel createHeader() {
    JPanel header = new JPanel(new BorderLayout());
    header.setBackground(UiStyle.POINT);
    header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

    JPanel textRow = new JPanel(new GridLayout(1, 3));
    textRow.setOpaque(false);
    for (JLabel label : new JLabel[] {petNameLabel, levelLabel, expLabel}) {
      label.setFont(UiStyle.SUBTITLE_FONT);
      label.setForeground(Color.WHITE);
      textRow.add(label);
    }
    header.add(textRow, BorderLayout.CENTER);
    expBar.setPreferredSize(new Dimension(0, 8));
    header.add(expBar, BorderLayout.SOUTH);
    return header;
  }

  private JPanel createCenter() {
    JPanel center = new JPanel();
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
    center.setOpaque(false);
    center.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

    center.add(petBox);
    center.add(Box.createVerticalStrut(30));

    JPanel stats = new JPanel(new GridLayout(2, 2, 20, 12));
    stats.setOpaque(false);
    stats.add(strLabel);
    stats.add(intLabel);
    stats.add(focLabel);
    stats.add(wisLabel);
    stats.setMaximumSize(new Dimension(360, 70));
    center.add(stats);
    center.add(Box.createVerticalStrut(40));

    JButton learnButton = UiStyle.button("학습하기");
    learnButton.addActionListener(e -> screens.show(ScreenManager.CATEGORY));
    center.add(learnButton);
    center.add(Box.createVerticalStrut(12));

    JButton historyButton = UiStyle.button("기록");
    historyButton.addActionListener(e -> screens.show(ScreenManager.HISTORY));
    center.add(historyButton);
    center.add(Box.createVerticalStrut(12));

    JButton changeUserButton = UiStyle.button("사용자 변경");
    changeUserButton.addActionListener(e -> screens.show(ScreenManager.START));
    center.add(changeUserButton);
    return center;
  }

  private static JLabel statLabel() {
    JLabel label = new JLabel("", SwingConstants.CENTER);
    label.setFont(UiStyle.BODY_FONT);
    return label;
  }
}
