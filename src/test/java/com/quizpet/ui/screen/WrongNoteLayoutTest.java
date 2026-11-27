package main.java.com.quizpet.ui.screen;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import main.java.com.quizpet.ui.screen.HistoryPanel.WrongNoteView;

/** Headless geometry checks: both panes must stay visible without scrolling the entire page. */
public final class WrongNoteLayoutTest {
  public static void main(String[] args) throws Exception {
    boolean enabled = false; assert enabled = true;
    if (!enabled) throw new AssertionError("Run with -ea");
    SwingUtilities.invokeAndWait(WrongNoteLayoutTest::check);
    System.out.println("Wrong-note layout checks passed");
  }

  private static void check() {
    HistoryPanel panel = new HistoryPanel();
    String raw = "  SA MiXeD answer  ";
    String detail = "과거 문제 / COMMON / SA / 상태 확인 필요\n" + (raw + " 긴 제출 이력\n").repeat(150);
    var item = new WrongNoteView.WrongItem("legacy-id", "상식", "문제 원문 ".repeat(40), raw,
        "정답", "해설", 2, "상태 확인 필요", LocalDateTime.of(2026, 11, 27, 12, 0), detail);
    List<WrongNoteView.WrongItem> many = new ArrayList<>();
    for (int i = 0; i < 200; i++) many.add(item);
    panel.setReviewEnabled(false);
    for (int[] size : new int[][]{{1280, 800}, {800, 600}, {480, 600}, {1600, 900}}) {
      panel.showWrongNote(new WrongNoteView(null, many.size(), 0, many, true, false));
      panel.setSize(size[0], size[1]); layout(panel);
      JSplitPane split = find(panel, JSplitPane.class).stream()
          .filter(s -> s.getOrientation() == JSplitPane.VERTICAL_SPLIT).findFirst()
          .orElseThrow(() -> new AssertionError("List and detail must have independent visible areas"));
      JTable table = find(split, JTable.class).get(0);
      table.setRowSelectionInterval(0, 0);
      JTextArea text = find(split, JTextArea.class).get(0);
      assert text.getText().equals(detail) : "SA and historical details must stay unchanged";
      layout(panel);
      for (int divider : new int[]{split.getMinimumDividerLocation(), split.getMaximumDividerLocation()}) {
        split.setDividerLocation(divider); layout(panel);
        Component top = split.getTopComponent(), bottom = split.getBottomComponent();
        assert top.getHeight() >= 100 && bottom.getHeight() >= 120 : "Pane collapsed";
        assert !top.getBounds().intersects(bottom.getBounds());
        assert new Rectangle(panel.getSize()).contains(SwingUtilities.convertRectangle(
            bottom.getParent(), bottom.getBounds(), panel)) : "Detail pushed below visible page";
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, text);
        assert scroll.getViewport().getHeight() >= 60 : "Detail unreadable";
        scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
        assert scroll.getViewport().getViewPosition().y > 0 : "Long history cannot scroll";
        JScrollPane rows = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        table.scrollRectToVisible(table.getCellRect(199, 6, true));
        assert rows.getViewport().getViewRect().intersects(table.getCellRect(199, 6, true))
            : "Last row / column unreachable";
      }
      split.setDividerLocation(0.4); layout(panel);
      BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics(); panel.paint(g); g.dispose();
    }
    panel.showWrongNoteError("조회 실패");
    assert find(panel, JTextArea.class).stream().noneMatch(t -> t.getText().contains(raw));
    panel.showWrongNote(new WrongNoteView(null, 0, 0, List.of(), true, false));
    assert find(panel, JLabel.class).stream().anyMatch(l -> l.getText().contains("아직 틀린"));
    panel.showWrongNote(new WrongNoteView(null, 1, 0, List.of(item), true, false));
    find(panel, JTable.class).get(0).setRowSelectionInterval(0, 0);
    assert find(panel, JTextArea.class).get(0).getText().equals(detail);
    panel.clearWrongNote();
    assert find(panel, JTextArea.class).stream().noneMatch(t -> t.getText().contains(raw));
  }

  private static void layout(Container c) {
    for (int i = 0; i < 4; i++) layoutOnce(c);
  }
  private static void layoutOnce(Container c) {
    c.doLayout();
    for (Component child : c.getComponents()) if (child instanceof Container p) layoutOnce(p);
  }
  private static <T> List<T> find(Component c, Class<T> type) {
    List<T> result = new ArrayList<>();
    if (!c.isVisible()) return result;
    if (type.isInstance(c)) result.add(type.cast(c));
    if (c instanceof Container p) for (Component child : p.getComponents()) result.addAll(find(child, type));
    return result;
  }
}
