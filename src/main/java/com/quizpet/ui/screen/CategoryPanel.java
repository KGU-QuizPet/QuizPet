package main.java.com.quizpet.ui.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;

public class CategoryPanel extends JPanel {
  private static final String[][] PREVIEW_CATEGORIES = {
    {"SCIENCE", "과학&상식", "과학 원리와 일반 상식", "지성 (INT)"},
    {"SPORTS", "스포츠", "경기 규칙과 스포츠 용어", "체력 (STR)"},
    {"HISTORY", "역사", "역사적 사건과 인물", "지혜 (WIS)"},
    {"LANGUAGE", "언어(영어)", "영어 단어 및\n기초 언어 지식", "집중력 (FOC)"}
  };

  private final ScreenManager screens;
  private final JPanel[] categoryCards = new JPanel[PREVIEW_CATEGORIES.length];
  private final JButton[] selectButtons = new JButton[PREVIEW_CATEGORIES.length];
  private final JLabel selectionLabel = UiStyle.label("선택한 분야: 없음", UiStyle.SUBTITLE_FONT);
  private String selectedCategoryId;

  public CategoryPanel(ScreenManager screens) {
    this.screens = Objects.requireNonNull(screens, "screens는 필수입니다.");
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 24));

    JPanel header = new JPanel(new BorderLayout(0, 10));
    header.setOpaque(false);
    header.add(UiStyle.label("학습 분야 선택", UiStyle.TITLE_FONT), BorderLayout.NORTH);
    header.add(UiStyle.label("학습할 분야를 골라 주세요.", UiStyle.BODY_FONT), BorderLayout.SOUTH);
    add(header, BorderLayout.NORTH);

    JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
    grid.setOpaque(false);
    for (int i = 0; i < PREVIEW_CATEGORIES.length; i++) {
      categoryCards[i] = createCategoryCard(i);
      grid.add(categoryCards[i]);
    }
    add(grid, BorderLayout.CENTER);

    JPanel footer = new JPanel(new BorderLayout(0, 10));
    footer.setOpaque(false);
    footer.add(selectionLabel, BorderLayout.NORTH);
    footer.add(UiStyle.label("UI 확인용 임시 데이터 · 화면 이동 없음", UiStyle.BODY_FONT),
        BorderLayout.SOUTH);
    add(footer, BorderLayout.SOUTH);
  }

  public String getSelectedCategoryId() {
    return selectedCategoryId;
  }

  private JPanel createCategoryCard(int index) {
    String[] category = PREVIEW_CATEGORIES[index];
    JPanel card = new JPanel(new BorderLayout(0, 12));
    card.setOpaque(false);
    setCardBorder(card, false);
    card.add(UiStyle.label(category[1], UiStyle.SUBTITLE_FONT), BorderLayout.NORTH);

    JTextArea description = new JTextArea(category[2]);
    description.setFont(UiStyle.BODY_FONT);
    description.setEditable(false);
    description.setFocusable(false);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setOpaque(false);
    card.add(description, BorderLayout.CENTER);

    JPanel actions = new JPanel(new BorderLayout(0, 10));
    actions.setOpaque(false);
    actions.add(UiStyle.label(category[3], UiStyle.BODY_FONT), BorderLayout.NORTH);
    JButton button = UiStyle.button("선택");
    button.addActionListener(e -> selectCategory(index));
    selectButtons[index] = button;
    actions.add(button, BorderLayout.SOUTH);
    card.add(actions, BorderLayout.SOUTH);
    return card;
  }

  private void selectCategory(int index) {
    selectedCategoryId = PREVIEW_CATEGORIES[index][0];
    selectionLabel.setText("선택한 분야: " + PREVIEW_CATEGORIES[index][1]);
    for (int i = 0; i < categoryCards.length; i++) {
      boolean selected = i == index;
      setCardBorder(categoryCards[i], selected);
      selectButtons[i].setText(selected ? "선택됨" : "선택");
    }
    repaint();
  }

  private static void setCardBorder(JPanel card, boolean selected) {
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(selected ? UiStyle.POINT : Color.LIGHT_GRAY, 2),
        BorderFactory.createEmptyBorder(16, 12, 16, 12)));
  }
}
