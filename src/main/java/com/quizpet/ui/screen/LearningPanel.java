package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.SwingConstants;
import main.java.com.quizpet.ui.component.Badge;
import main.java.com.quizpet.ui.component.CategoryIcon;
import main.java.com.quizpet.ui.component.ImageButton;
import main.java.com.quizpet.ui.component.LearningProgressBar;
import main.java.com.quizpet.ui.component.RoundCard;
import main.java.com.quizpet.ui.component.WrapPanel;
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
  private final JButton quizButton = new ImageButton("퀴즈 시작");
  private boolean quizEnabled = true;
  private final JLabel progressLabel = UiStyle.label("", UiStyle.SUBTITLE_FONT);
  private final JLabel cardTitleLabel = new JLabel("", SwingConstants.LEFT) {
    @Override public void setBounds(int x, int y, int width, int height) {
      Font font = UiStyle.TITLE_FONT.deriveFont(44f);
      while (width > 0 && font.getSize() > 22 && getFontMetrics(font).stringWidth(getText()) > width) {
        font = font.deriveFont((float) font.getSize() - 2);
      }
      if (!font.equals(getFont())) setFont(font);
      super.setBounds(x, y, width, height);
    }
  };
  private final JTextArea descriptionArea = new JTextArea();
  private final WrapPanel keywords = new WrapPanel();
  private final LearningProgressBar progressBar = new LearningProgressBar();
  private Category category;
  private final JButton previousButton = new ImageButton("이전");
  private final JButton nextButton = new ImageButton("다음");
  private int currentCardIndex;

  public LearningPanel(ScreenManager screens) {
    Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

    add(createHeader(), BorderLayout.NORTH);
    add(createLearningCard(), BorderLayout.CENTER);
    add(createFooter(), BorderLayout.SOUTH);
    bindNavigationKey("LEFT", "learning.previous", previousButton);
    bindNavigationKey("RIGHT", "learning.next", nextButton);
    bindNavigationKey("ENTER", "learning.quiz", quizButton);
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
    this.category = category;
    categoryLabel.setText(category.getName() + " · " + category.getStatType());
    categoryLabel.setIcon(new CategoryIcon(category, 28));
    categoryLabel.setForeground(UiStyle.categoryColor(category));
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
    JPanel information = new JPanel(new BorderLayout(20, 0));
    information.setOpaque(false);
    categoryLabel.setHorizontalAlignment(SwingConstants.LEFT);
    progressLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    JPanel heading = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
    heading.setOpaque(false);
    heading.add(categoryLabel);
    heading.add(UiStyle.label("학습 카드", UiStyle.SUBTITLE_FONT));
    information.add(heading, BorderLayout.WEST);
    information.add(progressLabel, BorderLayout.EAST);
    header.add(information, BorderLayout.NORTH);
    header.add(progressBar, BorderLayout.SOUTH);
    return header;
  }

  private JPanel createLearningCard() {
    RoundCard card = new RoundCard(new BorderLayout(0, 20), 26, 6) {
      @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (category == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(UiStyle.categoryColor(category));
        g.fillRoundRect(3, 14, 8, Math.max(0, getHeight() - 32), 8, 8);
        g.dispose();
      }
    };
    card.setBorder(BorderFactory.createEmptyBorder(24, 32, 28, 32));
    JPanel heading = new JPanel(new BorderLayout(0, 14));
    heading.setOpaque(false);
    heading.add(keywords, BorderLayout.NORTH);
    cardTitleLabel.setHorizontalAlignment(SwingConstants.LEFT);
    cardTitleLabel.setForeground(UiStyle.TEXT);
    cardTitleLabel.setFont(UiStyle.TITLE_FONT.deriveFont(44f));
    heading.add(cardTitleLabel, BorderLayout.CENTER);
    card.add(heading, BorderLayout.NORTH);
    configureTextArea(descriptionArea);
    descriptionArea.setFont(UiStyle.BODY_FONT.deriveFont(21f));
    descriptionArea.setForeground(UiStyle.TEXT);
    JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
    descriptionScroll.setBorder(BorderFactory.createEmptyBorder());
    descriptionScroll.setOpaque(false);
    descriptionScroll.getViewport().setOpaque(false);
    descriptionScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    descriptionScroll.getVerticalScrollBar().setUnitIncrement(24);
    card.add(descriptionScroll, BorderLayout.CENTER);
    JTextArea hint = new JTextArea("이 카드와 연결된 문제가 출제될 수 있어요.");
    configureTextArea(hint);
    hint.setOpaque(true);
    hint.setBackground(UiStyle.SUNKEN);
    hint.setForeground(UiStyle.MUTED);
    hint.setRows(2);
    hint.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    card.add(hint, BorderLayout.SOUTH);
    return card;
  }

  private JPanel createFooter() {
    JPanel footer = new JPanel(new GridLayout(1, 4, 12, 12)) {
      @Override public void setBounds(int x, int y, int width, int height) {
        GridLayout grid = (GridLayout) getLayout();
        int columns = width < 520 ? 2 : 4;
        if (grid.getColumns() != columns) { grid.setRows(0); grid.setColumns(columns); revalidate(); }
        super.setBounds(x, y, width, height);
      }
      @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (LearningPanel.this.getWidth() > 0 && LearningPanel.this.getWidth() < 584) size.height = 116;
        return size;
      }
    };
    footer.setOpaque(false);
    previousButton.setBackground(UiStyle.SURFACE);
    nextButton.setBackground(UiStyle.SURFACE);
    previousButton.addActionListener(e -> moveCard(-1));
    nextButton.addActionListener(e -> moveCard(1));
    footer.add(previousButton);
    footer.add(nextButton);
    JButton backButton = new ImageButton("뒤로");
    backButton.setToolTipText("분야 선택 또는 정답 피드백으로 돌아가기");
    backButton.setBackground(UiStyle.SURFACE);
    backButton.addActionListener(e -> onBack.run());
    footer.add(backButton);
    quizButton.setForeground(UiStyle.SURFACE);
    quizButton.setPreferredSize(new Dimension(150, 60));
    quizButton.addActionListener(e -> onQuiz.run());
    footer.add(quizButton);
    return footer;
  }

  private void bindNavigationKey(String stroke, String name, JButton button) {
    getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(javax.swing.KeyStroke.getKeyStroke(stroke), name);
    getActionMap().put(name, new javax.swing.AbstractAction() {
      @Override public void actionPerformed(java.awt.event.ActionEvent event) {
        if (button.isEnabled()) button.doClick();
      }
    });
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
      keywords.removeAll();
      progressBar.showProgress(0, 0);
      progressLabel.setText("0 / 0");
      return;
    }
    LearningContent card = contents.get(currentCardIndex);
    cardTitleLabel.setText(card.getTitle());
    cardTitleLabel.setToolTipText(card.getTitle());
    descriptionArea.setText(card.getDescription());
    descriptionArea.setCaretPosition(0);
    keywords.removeAll();
    Color color = UiStyle.categoryColor(card.getCategory());
    for (String tag : card.getTags()) keywords.add(new Badge("#" + tag,
        CategoryIcon.soft(color, 0.15), color, UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f), 0));
    keywords.add(new Badge("난이도 " + (card.getDifficulty() <= 3
        ? "★".repeat(card.getDifficulty()) + "☆".repeat(3 - card.getDifficulty())
        : String.valueOf(card.getDifficulty())), UiStyle.SUNKEN, UiStyle.MUTED,
        UiStyle.BODY_FONT.deriveFont(Font.BOLD, 13f), 0));
    progressBar.showProgress(currentCardIndex, contents.size());
    keywords.revalidate();
    keywords.repaint();
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
