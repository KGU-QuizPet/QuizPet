package main.java.com.quizpet.ui.screen;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.screen.HistoryPanel.HistoryView;
import main.java.com.quizpet.ui.screen.HistoryPanel.WrongNoteView;
import main.java.com.quizpet.ui.screen.QuizResultPanel.QuestionResult;
import main.java.com.quizpet.ui.screen.QuizResultPanel.QuizResultView;

public final class RecordScreensLayoutTest {
  public static void main(String[] args) throws Exception {
    boolean enabled = false;
    assert enabled = true;
    if (!enabled) throw new AssertionError("Run with -ea");
    SwingUtilities.invokeAndWait(() -> {
      QuizResultPanel result = new QuizResultPanel();
      result.showResult(new QuizResultView("과학", "INT", 4, 5, 80.0, 40, 4, 4, 5, 10, 50, 5));
      List<QuestionResult> rows = new ArrayList<>();
      for (int i = 0; i < 5; i++) rows.add(new QuestionResult("문제 " + i, i != 3, i != 3 ? 10 : 0));
      result.showQuestionResults(rows);
      result.showStatComparison(stats(12, 16, 14, 13), stats(12, 18, 14, 13));
      resize(result, 960, 736);
      JScrollPane list = find(result, JScrollPane.class).get(0);
      Component ringCard = ringCard(result);
      Component statCard = SwingUtilities.getAncestorOfClass(RecordUi.Card.class, result.getStatComparison());
      int listWidth = list.getWidth(), listHeight = list.getHeight();
      assert statCard.getWidth() > 0 && list.getWidth() > 0 : "문제별 결과와 능력치 변화가 나란히 보여야 함";
      resize(result, 1920, 1080);
      assert list.getWidth() > listWidth && list.getHeight() > listHeight : "결과 목록이 창 크기에 맞춰 늘어나야 함";
      assert ringCard.getWidth() == 280 : "정답률 링 칸은 280 고정, 실제 " + ringCard.getWidth();
      result.showResult(new QuizResultView("과학", "INT", 0, 5, 0, 0, 0, 1, 1, 0, 50, 0));
      assert !SwingUtilities.getAncestorOfClass(RecordUi.Card.class, list).isVisible()
          : "새 결과를 보여 주면 이전 문제별 목록이 남지 않고, 목록이 없으면 카드를 숨겨야 함";
      result.hideStatComparison();
      assert !statCard.isVisible() && !result.getStatComparison().isVisible() : "능력치 비교를 숨기면 카드도 숨겨야 함";

      HistoryPanel history = new HistoryPanel();
      List<HistoryView.CategoryRate> rates = new ArrayList<>();
      for (Category category : Category.values()) rates.add(HistoryView.CategoryRate.of(category, 4, 50.0));
      List<HistoryView.RecordRow> records = new ArrayList<>();
      for (int i = 0; i < 30; i++) {
        records.add(new HistoryView.RecordRow(LocalDateTime.of(2026, 11, 17, 14, 0), "과학",
            "긴 문제 ".repeat(40), "1", i % 2 == 0, i % 2 == 0 ? 10 : 0));
      }
      history.showHistory(new HistoryView(null, 30, 50.0, 150, rates, records));
      resize(history, 960, 736);
      JTable table = find(history, JTable.class).get(0);
      int tableWidth = table.getParent().getWidth(), tableHeight = table.getParent().getHeight();
      resize(history, 1920, 1080);
      assert table.getParent().getWidth() > tableWidth && table.getParent().getHeight() > tableHeight
          : "풀이 이력 표가 창 크기에 맞춰 늘어나야 함";
      JSplitPane columns = find(history, JSplitPane.class).get(0);
      double ratio = (double) columns.getRightComponent().getWidth() / columns.getLeftComponent().getWidth();
      assert ratio > 1.3 && ratio < 1.7 : "분야별 정답률 : 풀이 이력 = 1 : 1.5, 실제 " + ratio;
      table.setRowSelectionInterval(0, 0);
      int before = table.getParent().getHeight();
      resize(history, 1920, 1080);
      assert table.getParent().getHeight() == before : "긴 문제를 선택해도 표가 줄어들면 안 됨";

      history.showHistory(new HistoryView("SCIENCE", 4, 50.0, 20, rates, records.subList(0, 4)));
      assert labels(history).contains("과학 정답률") : "분야를 고르면 정답률 카드 제목이 분야 이름으로 바뀌어야 함";
      history.showHistoryError("오류");
      assert table.getRowCount() == 0 : "오류 시 이전 기록이 남으면 안 됨";
      assert find(history, JTextArea.class).stream().noneMatch(t -> t.getText().contains("긴 문제"))
          : "오류 시 이전 상세가 남으면 안 됨";

      WrongNoteView note = new WrongNoteView(null, 5, 40.0, List.of(
          new WrongNoteView.WrongItem("Q1", "과학", "질문 ".repeat(30), "1. 하나", "2. 둘", "해설")));
      history.showWrongNote(note);
      resize(history, 960, 736);
      JTable wrong = find(history, JTable.class).stream().filter(t -> t.getColumnCount() == 7).findFirst().orElseThrow();
      int wrongWidth = wrong.getParent().getWidth();
      resize(history, 1920, 1080);
      assert wrong.getParent().getWidth() > wrongWidth : "오답노트 표가 창 크기에 맞춰 늘어나야 함";
    });
    System.out.println("Record screens layout checks passed");
  }

  private static Map<String, Integer> stats(int str, int intel, int foc, int wis) {
    return Map.of("STR", str, "INT", intel, "FOC", foc, "WIS", wis);
  }

  private static Component ringCard(QuizResultPanel panel) {
    for (Component c : allComponents(panel)) {
      if (c.getClass().getSimpleName().equals("AccuracyRing")) return c.getParent();
    }
    throw new AssertionError("정답률 링이 없음");
  }

  private static String labels(Container root) {
    StringBuilder text = new StringBuilder();
    for (Component c : allComponents(root)) {
      if (c instanceof javax.swing.JLabel label && c.isVisible()) text.append(label.getText()).append(' ');
    }
    return text.toString();
  }

  private static void resize(JPanel panel, int width, int height) {
    panel.setSize(width, height);
    for (int i = 0; i < 8; i++) layout(panel);
  }

  private static void layout(Container root) {
    root.doLayout();
    for (Component child : root.getComponents()) if (child instanceof Container c) layout(c);
  }

  private static List<Component> allComponents(Container root) {
    List<Component> found = new ArrayList<>();
    for (Component child : root.getComponents()) {
      found.add(child);
      if (child instanceof Container c) found.addAll(allComponents(c));
    }
    return found;
  }

  private static <T> List<T> find(Container root, Class<T> type) {
    List<T> found = new ArrayList<>();
    for (Component child : allComponents(root)) {
      if (type.isInstance(child) && child.isVisible()) found.add(type.cast(child));
    }
    return found;
  }
}
