package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import main.java.com.quizpet.ui.UiStyle;

public class MultipleChoicePanel extends JPanel {
  private final JTextArea questionArea = new JTextArea();
  private final JList<String> optionsList = new OptionList();
  private final JLabel selectionLabel = UiStyle.label("선택한 답안: 없음", UiStyle.BODY_FONT);

  public MultipleChoicePanel(String question, List<String> options) {
    setLayout(new BorderLayout(0, 20));
    setBackground(UiStyle.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    questionArea.setFont(UiStyle.SUBTITLE_FONT);
    questionArea.setEditable(false);
    questionArea.setFocusable(false);
    questionArea.setLineWrap(true);
    questionArea.setWrapStyleWord(true);
    questionArea.setOpaque(false);
    JScrollPane questionScroll = createScrollPane(questionArea);
    questionScroll.setPreferredSize(new Dimension(0, 140));
    add(questionScroll, BorderLayout.NORTH);

    optionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    optionsList.setVisibleRowCount(4);
    optionsList.setBackground(UiStyle.BACKGROUND);
    optionsList.setCellRenderer(new OptionRenderer());
    optionsList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        String answer = getSelectedAnswer();
        selectionLabel.setText("선택한 답안: " + (answer == null ? "없음" : answer + "번"));
      }
    });
    add(createScrollPane(optionsList), BorderLayout.CENTER);
    add(selectionLabel, BorderLayout.SOUTH);
    setQuestion(question, options);
  }

  public void setQuestion(String question, List<String> options) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("질문은 비어 있을 수 없습니다.");
    }
    if (options == null || options.size() != 4) {
      throw new IllegalArgumentException("선택지는 정확히 4개여야 합니다.");
    }
    for (String option : options) {
      if (option == null || option.isBlank()) {
        throw new IllegalArgumentException("선택지는 비어 있을 수 없습니다.");
      }
    }
    questionArea.setText(question);
    questionArea.setCaretPosition(0);
    optionsList.setListData(options.toArray(String[]::new));
    optionsList.clearSelection();
    optionsList.ensureIndexIsVisible(0);
    selectionLabel.setText("선택한 답안: 없음");
  }

  public String getSelectedAnswer() {
    int selectedIndex = optionsList.getSelectedIndex();
    return selectedIndex < 0 ? null : Integer.toString(selectedIndex + 1);
  }

  private static JScrollPane createScrollPane(Component content) {
    JScrollPane scroll = new JScrollPane(content);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    return scroll;
  }

  private static class OptionList extends JList<String> {
    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
      super.setBounds(x, y, width, height);
      if (width > 0 && getFixedCellWidth() != width) {
        setFixedCellWidth(width);
      }
    }
  }

  private static class OptionRenderer extends JTextArea implements ListCellRenderer<String> {
    OptionRenderer() {
      setFont(UiStyle.BODY_FONT);
      setEditable(false);
      setFocusable(false);
      setLineWrap(true);
      setWrapStyleWord(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value,
        int index, boolean selected, boolean focused) {
      setText((index + 1) + ". " + value);
      setBackground(selected ? UiStyle.POINT : UiStyle.BACKGROUND);
      setForeground(selected ? Color.WHITE : Color.DARK_GRAY);
      setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(selected ? UiStyle.POINT : Color.LIGHT_GRAY, 2),
          BorderFactory.createEmptyBorder(12, 12, 12, 12)));
      int width = list.getWidth() > 0 ? list.getWidth() : 320;
      setSize(width, Short.MAX_VALUE);
      return this;
    }
  }
}
