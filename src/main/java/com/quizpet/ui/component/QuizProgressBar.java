package main.java.com.quizpet.ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.ui.UiStyle;

public final class QuizProgressBar extends JPanel {
  private List<Color> segments = List.of();

  public QuizProgressBar() {
    setOpaque(false);
    setPreferredSize(new Dimension(220, 24));
    getAccessibleContext().setAccessibleName("퀴즈 진행률");
  }

  public void showSession(QuizSession session) {
    Map<String, Boolean> results = session.getAnswerResults();
    segments = session.getQuestions().stream().map(question -> {
      Boolean correct = results.get(question.getQuestionId());
      if (correct != null) return correct ? UiStyle.CORRECT : UiStyle.WRONG;
      return question == session.getCurrentQuestion() && !session.isFinished()
          ? UiStyle.BRAND : UiStyle.BORDER;
    }).toList();
    String status = "풀이 " + session.getAnsweredCount() + " / " + session.getTotalCount()
        + " · 정답 " + session.getCorrectCount();
    setToolTipText(status);
    getAccessibleContext().setAccessibleDescription(status);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    if (segments.isEmpty()) return;
    Graphics2D g = (Graphics2D) graphics.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    double cell = (double) getWidth() / segments.size();
    for (int i = 0; i < segments.size(); i++) {
      g.setColor(segments.get(i));
      g.fillRoundRect((int) (i * cell), (getHeight() - 8) / 2,
          Math.max(1, (int) cell - 6), 8, 8, 8);
    }
    g.dispose();
  }
}
