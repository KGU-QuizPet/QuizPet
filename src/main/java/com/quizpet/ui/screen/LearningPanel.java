package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class LearningPanel extends JPanel {
  private static final String PREVIEW_CATEGORY_NAME = "과학&상식";
  private static final String[][] PREVIEW_CARDS = {
    {"물의 세 가지 상태", "물은 고체, 액체, 기체의 세 가지 상태로 존재합니다.\n\n"
        + "얼음은 고체, 흐르는 물은 액체, 수증기는 기체입니다.\n\n"
        + "온도가 달라지면 물의 상태도 달라질 수 있습니다.", "1", "물, 상태 변화"},
    {"태양계의 행성", "태양계에는 태양 주위를 도는 여덟 개의 행성이 있습니다.\n\n"
        + "태양에서 가까운 순서대로 수성, 금성, 지구, 화성, 목성, 토성, 천왕성, 해왕성입니다.\n\n"
        + "가장 큰 행성은 목성이며, 지구는 태양에서 세 번째 행성입니다.", "1", "태양계, 행성"},
    {"지구의 자전과 낮과 밤", "지구는 스스로 회전하며, 이를 자전이라고 합니다.\n\n"
        + "태양을 향한 쪽은 낮이 되고 반대쪽은 밤이 됩니다.\n\n"
        + "지구가 한 바퀴 자전하는 데에는 약 하루가 걸립니다.", "2", "지구, 자전, 낮과 밤"}
  };

  private final ScreenManager screens;
  private final JLabel progressLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel cardTitleLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel difficultyLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JTextArea descriptionArea = new JTextArea();
  private final JLabel keywordsLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JButton previousButton = UiStyle.button("이전");
  private final JButton nextButton = UiStyle.button("다음");
  private int currentCardIndex;

  public LearningPanel(ScreenManager screens) {
    this.screens = Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 24));

    add(createHeader(), BorderLayout.NORTH);
    add(createLearningCard(), BorderLayout.CENTER);
    add(createFooter(), BorderLayout.SOUTH);
    showCurrentCard();
  }

  private JPanel createHeader() {
    JPanel header = new JPanel(new BorderLayout(0, 14));
    header.setOpaque(false);
    header.add(UiStyle.label("학습 카드", UiStyle.TITLE_FONT), BorderLayout.NORTH);

    JPanel information = new JPanel(new BorderLayout());
    information.setOpaque(false);
    information.add(UiStyle.label(PREVIEW_CATEGORY_NAME, UiStyle.SUBTITLE_FONT), BorderLayout.WEST);
    information.add(progressLabel, BorderLayout.EAST);
    header.add(information, BorderLayout.SOUTH);
    return header;
  }

  private JPanel createLearningCard() {
    JPanel card = new JPanel(new BorderLayout(0, 20));
    card.setOpaque(false);
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2),
        BorderFactory.createEmptyBorder(20, 16, 20, 16)));

    JPanel heading = new JPanel(new BorderLayout(0, 10));
    heading.setOpaque(false);
    heading.add(cardTitleLabel, BorderLayout.NORTH);
    heading.add(difficultyLabel, BorderLayout.SOUTH);
    card.add(heading, BorderLayout.NORTH);

    configureTextArea(descriptionArea);
    JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
    descriptionScroll.setBorder(BorderFactory.createEmptyBorder());
    descriptionScroll.setOpaque(false);
    descriptionScroll.getViewport().setOpaque(false);
    descriptionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    card.add(descriptionScroll, BorderLayout.CENTER);

    card.add(keywordsLabel, BorderLayout.SOUTH);
    return card;
  }

  private JPanel createFooter() {
    JPanel footer = new JPanel(new BorderLayout(0, 12));
    footer.setOpaque(false);

    JPanel navigation = new JPanel(new GridLayout(1, 2, 16, 0));
    navigation.setOpaque(false);
    previousButton.addActionListener(e -> moveCard(-1));
    nextButton.addActionListener(e -> moveCard(1));
    navigation.add(previousButton);
    navigation.add(nextButton);
    footer.add(navigation, BorderLayout.NORTH);

    JButton quizButton = UiStyle.button("퀴즈 시작 (준비 중)");
    quizButton.setEnabled(false);
    footer.add(quizButton, BorderLayout.CENTER);
    footer.add(UiStyle.label("UI 확인용 임시 데이터 · 퀴즈 연결 없음", UiStyle.BODY_FONT),
        BorderLayout.SOUTH);
    return footer;
  }

  private void moveCard(int offset) {
    int nextIndex = currentCardIndex + offset;
    if (nextIndex < 0 || nextIndex >= PREVIEW_CARDS.length) {
      return;
    }
    currentCardIndex = nextIndex;
    showCurrentCard();
  }

  private void showCurrentCard() {
    String[] card = PREVIEW_CARDS[currentCardIndex];
    cardTitleLabel.setText(card[0]);
    descriptionArea.setText(card[1]);
    descriptionArea.setCaretPosition(0);
    difficultyLabel.setText("난이도 " + card[2]);
    keywordsLabel.setText("키워드: " + card[3]);
    progressLabel.setText((currentCardIndex + 1) + " / " + PREVIEW_CARDS.length);
    previousButton.setEnabled(currentCardIndex > 0);
    nextButton.setEnabled(currentCardIndex < PREVIEW_CARDS.length - 1);
  }

  private static void configureTextArea(JTextArea area) {
    area.setFont(UiStyle.BODY_FONT);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setOpaque(false);
  }
}
