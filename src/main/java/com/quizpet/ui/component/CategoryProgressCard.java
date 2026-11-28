package main.java.com.quizpet.ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

public final class CategoryProgressCard extends RoundCard {
  private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 19);
  private static final Font STAT_FONT = new Font(Font.DIALOG, Font.BOLD, 13);
  private static final Font CAPTION_FONT = new Font(Font.DIALOG, Font.BOLD, 14);

  private final Category category;
  private final GaugeBar bar;
  private final JLabel captionLabel = new JLabel("아직 시작 전");
  private final Badge recommendBadge;
  private Runnable onClick = () -> { };

  public CategoryProgressCard(Category category) {
    super(new GridBagLayout());
    this.category = category;
    Color color = UiStyle.categoryColor(category);
    bar = new GaugeBar(color, 10, false);
    recommendBadge = new Badge("추천", UiStyle.BRAND, Color.WHITE, STAT_FONT, 0);
    recommendBadge.setVisible(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setFocusable(true);
    setToolTipText(category.getName() + " 학습하기 · " + category.getStatType() + " +1");
    getAccessibleContext().setAccessibleName(category.getName() + " 학습하기");

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST;

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(new JLabel(new CategoryIcon(category, 48)), BorderLayout.WEST);
    JPanel badgeHolder = new JPanel(new BorderLayout());
    badgeHolder.setOpaque(false);
    badgeHolder.add(recommendBadge, BorderLayout.NORTH);
    top.add(badgeHolder, BorderLayout.EAST);
    c.gridy = 0;
    add(top, c);

    JPanel nameRow = new JPanel(new BorderLayout());
    nameRow.setOpaque(false);
    JLabel name = new JLabel(category.getName());
    name.setFont(NAME_FONT);
    name.setForeground(UiStyle.TEXT);
    JLabel stat = new JLabel(category.getStatType());
    stat.setFont(STAT_FONT);
    stat.setForeground(color);
    nameRow.add(name, BorderLayout.WEST);
    nameRow.add(stat, BorderLayout.EAST);
    c.gridy = 1;
    c.insets = new Insets(8, 0, 8, 0);
    add(nameRow, c);

    c.gridy = 2;
    c.insets = new Insets(0, 0, 10, 0);
    add(bar, c);

    captionLabel.setFont(CAPTION_FONT);
    captionLabel.setForeground(UiStyle.MUTED);
    c.gridy = 3;
    c.insets = new Insets(0, 0, 0, 0);
    add(captionLabel, c);

    MouseAdapter mouse = new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        setBorderColor(color);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        setBorderColor(UiStyle.BORDER);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        onClick.run();
      }
    };
    addMouseListener(mouse);
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
          onClick.run();
        }
      }
    });
  }

  public void setOnClick(Runnable action) {
    onClick = action;
  }

  public void showProgress(int attempts, double accuracy) {
    if (attempts <= 0) {
      bar.setValue(0, 100);
      captionLabel.setText("아직 시작 전");
    } else {
      int percent = (int) Math.round(accuracy);
      bar.setValue(percent, 100);
      captionLabel.setText(percent + "% · " + attempts + "문제");
    }
  }

  public void setRecommended(boolean recommended) {
    recommendBadge.setVisible(recommended);
  }

  public boolean isRecommended() {
    return recommendBadge.isVisible();
  }

  public Category getCategory() {
    return category;
  }

  public String getCaptionText() {
    return captionLabel.getText();
  }

  public GaugeBar getBar() {
    return bar;
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(140, super.getMinimumSize().height);
  }
}
