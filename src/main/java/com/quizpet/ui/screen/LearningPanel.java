package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.List;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.LearningContent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class LearningPanel extends JPanel {
  private List<LearningContent> contents = List.of();
  private Runnable onQuiz = () -> { };
  private Runnable onBack = () -> { };
  private final JLabel categoryLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JButton quizButton = UiStyle.button("퀴즈 시작");
  private boolean quizEnabled = true;
  private final JLabel progressLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel cardTitleLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel difficultyLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JTextArea descriptionArea = new JTextArea();
  private final JLabel keywordsLabel = UiStyle.label("", UiStyle.BODY_FONT);
  private final JButton previousButton = UiStyle.button("이전");
  private final JButton nextButton = UiStyle.button("다음");
  private int currentCardIndex;

  public LearningPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 24));

    add(createHeader(), BorderLayout.NORTH);
    add(createLearningCard(), BorderLayout.CENTER);
    add(createFooter(), BorderLayout.SOUTH);
    showCurrentCard();
  }

  public void showContents(Category category, List<LearningContent> cards, int startingIndex) {
    Objects.requireNonNull(category, "category는 필수입니다.");
    contents = List.copyOf(cards);
    if (contents.stream().anyMatch(card -> card.getCategory() != category)) {
      throw new IllegalArgumentException("선택한 분야와 학습 카드의 분야가 다릅니다.");
    }
    if (startingIndex < 0 || (!contents.isEmpty() && startingIndex >= contents.size())) {
      throw new IllegalArgumentException("학습 카드 위치가 범위를 벗어났습니다.");
    }
    categoryLabel.setText(category.getName());
    currentCardIndex = startingIndex;
    showCurrentCard();
  }

  public void setOnQuiz(Runnable action) {
    onQuiz = Objects.requireNonNull(action);
  }

  public void setOnBack(Runnable action) {
    onBack = Objects.requireNonNull(action);
  }

  public void setQuizEnabled(boolean enabled) {
    quizEnabled = enabled;
    quizButton.setEnabled(enabled && !contents.isEmpty());
  }

  private JPanel createHeader() {
    JPanel header = new JPanel(new BorderLayout(0, 14));
    header.setOpaque(false);
    header.add(UiStyle.label("학습 카드", UiStyle.TITLE_FONT), BorderLayout.NORTH);

    JPanel information = new JPanel(new BorderLayout());
    information.setOpaque(false);
    information.add(categoryLabel, BorderLayout.WEST);
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

    quizButton.addActionListener(e -> onQuiz.run());
    footer.add(quizButton, BorderLayout.CENTER);
    JButton backButton = UiStyle.button("뒤로");
    backButton.addActionListener(e -> onBack.run());
    footer.add(backButton, BorderLayout.SOUTH);
    return footer;
  }

  private void moveCard(int offset) {
    int nextIndex = currentCardIndex + offset;
    if (nextIndex < 0 || nextIndex >= contents.size()) {
      return;
    }
    currentCardIndex = nextIndex;
    showCurrentCard();
  }

  private void showCurrentCard() {
    boolean empty = contents.isEmpty();
    previousButton.setEnabled(!empty && currentCardIndex > 0);
    nextButton.setEnabled(!empty && currentCardIndex < contents.size() - 1);
    quizButton.setEnabled(quizEnabled && !empty);
    if (empty) {
      cardTitleLabel.setText("학습 카드가 없습니다.");
      descriptionArea.setText("");
      difficultyLabel.setText("");
      keywordsLabel.setText("");
      progressLabel.setText("0 / 0");
      return;
    }
    LearningContent card = contents.get(currentCardIndex);
    cardTitleLabel.setText(card.getTitle());
    descriptionArea.setText(card.getDescription());
    descriptionArea.setCaretPosition(0);
    difficultyLabel.setText("난이도 " + card.getDifficulty());
    keywordsLabel.setText("키워드: " + String.join(", ", card.getTags()));
    progressLabel.setText((currentCardIndex + 1) + " / " + contents.size());
  }

  private static void configureTextArea(JTextArea area) {
    area.setFont(UiStyle.BODY_FONT);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setOpaque(false);
  }
}
