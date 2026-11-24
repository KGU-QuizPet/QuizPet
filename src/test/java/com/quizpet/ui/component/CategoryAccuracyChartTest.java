package main.java.com.quizpet.ui.component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.CategoryAccuracyChart.CategoryData;
import main.java.com.quizpet.ui.component.CategoryAccuracyChart.DisplayState;

public final class CategoryAccuracyChartTest {
  public static void main(String[] args) throws Exception {
    boolean enabled = false; assert enabled = true;
    if (!enabled) throw new AssertionError("Run with -ea");
    SwingUtilities.invokeAndWait(() -> {
      CategoryAccuracyChart chart = new CategoryAccuracyChart();
      for (Category category : Category.values()) assert chart.getDisplayState(category) == DisplayState.NOT_STARTED;
      List<CategoryData> data = List.of(new CategoryData(Category.SCIENCE, 1, 0),
          new CategoryData(Category.COMMON, 2, 100), new CategoryData(Category.SPORTS, 3, 66.7),
          new CategoryData(Category.LANGUAGE, Integer.MAX_VALUE, 50), new CategoryData(Category.HISTORY, 0, 0));
      chart.setData(data);
      List<JProgressBar> bars = find(chart, JProgressBar.class);
      assert bars.size() == 5;
      assert bars.get(0).getValue() == 0 && bars.get(1).getValue() == 1000 && bars.get(2).getValue() == 667;
      for(int i=0;i<5;i++) {
        assert bars.get(i).getForeground().equals(UiStyle.categoryColor(Category.values()[i]));
        assert bars.get(i).isFocusable() && bars.get(i).getAccessibleContext().getAccessibleName() != null;
      }
      checkFocusRepaints(bars.get(0));
      assert labels(chart).contains("66.7%") && labels(chart).contains("2147483647회 풀이");
      for (int width : List.of(180, 320, 600, 1000, 1280)) {
        chart.setSize(width, 400); layout(chart);
        BufferedImage canvas = new BufferedImage(width, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics(); chart.paint(g); g.dispose();
        for (JLabel label : find(chart, JLabel.class)) assert label.getWidth() >= label.getPreferredSize().width : label.getText();
        for (JProgressBar bar : find(chart,JProgressBar.class)) assert bar.getWidth() >= 60;
      }
      reject(() -> chart.setData(List.of(data.get(0), data.get(0))));
      assert chart.getDisplayState(Category.COMMON) == DisplayState.RECORDED;
      reject(() -> chart.setData(data, Set.of(Category.SCIENCE)));
      reject(() -> new CategoryData(Category.SCIENCE, -1, 1));
      for(double bad : new double[]{-1,101,Double.NaN,Double.POSITIVE_INFINITY}) reject(() -> new CategoryData(Category.SCIENCE,1,bad));
      reject(() -> new CategoryData(Category.SCIENCE,0,50));
      chart.setData(List.of(data.get(0)), Set.of(Category.COMMON));
      assert chart.getDisplayState(Category.COMMON) == DisplayState.UNAVAILABLE;
      assert chart.getDisplayState(Category.SPORTS) == DisplayState.NOT_STARTED;
      List<CategoryData> mutable = new ArrayList<>(data);
      Collections.reverse(mutable);
      chart.setData(mutable);
      mutable.clear();
      assert chart.getDisplayState(Category.COMMON) == DisplayState.RECORDED;
      assert find(chart,JProgressBar.class).get(1).getValue() == 1000;
      chart.showUnavailable();
      for(Category c:Category.values()) assert chart.getDisplayState(c) == DisplayState.UNAVAILABLE;
      chart.showError("<파일 오류> 긴 오류 안내 ".repeat(30));
      for(Category c:Category.values()) assert chart.getDisplayState(c) == DisplayState.ERROR;
      assert find(chart,JTextArea.class).get(0).getText().contains("<파일 오류>");
      assert find(chart,JProgressBar.class).stream().allMatch(bar -> bar.getValue()==0);
      chart.setSize(440, 370); layout(chart); layout(chart); layout(chart);
      JTextArea error = find(chart,JTextArea.class).get(0);
      assert error.getParent() instanceof JViewport;
      JScrollPane errorScroll = (JScrollPane) error.getParent().getParent();
      assert errorScroll.getVerticalScrollBar().isVisible();
      errorScroll.getVerticalScrollBar().setValue(errorScroll.getVerticalScrollBar().getMaximum());
      assert errorScroll.getViewport().getViewPosition().y > 0;
      try {
        assert error.modelToView2D(error.getDocument().getLength()).getMaxY() <= error.getHeight();
      } catch (javax.swing.text.BadLocationException e) { throw new AssertionError(e); }
      chart.setData(List.of());
      assert find(chart,JTextArea.class).isEmpty();
      for(Category c:Category.values()) assert chart.getDisplayState(c) == DisplayState.NOT_STARTED;
    });
    try { new CategoryAccuracyChart().setData(List.of()); throw new AssertionError("off-EDT update"); }
    catch(IllegalStateException expected) { }
    System.out.println("CategoryAccuracyChart checks passed");
  }
  private static void checkFocusRepaints(JProgressBar bar) {
    bar.setSize(120, 18);
    RepaintManager previous = RepaintManager.currentManager(bar);
    boolean[] repainted = {false};
    RepaintManager tracking = new RepaintManager() {
      @Override public void addDirtyRegion(JComponent component, int x, int y, int width, int height) {
        if (component == bar && width > 0 && height > 0) repainted[0] = true;
      }
    };
    RepaintManager.setCurrentManager(tracking);
    try {
      for (int event : new int[]{java.awt.event.FocusEvent.FOCUS_GAINED, java.awt.event.FocusEvent.FOCUS_LOST}) {
        repainted[0] = false;
        var focus = new java.awt.event.FocusEvent(bar, event);
        for (var listener : bar.getFocusListeners()) {
          if (event == java.awt.event.FocusEvent.FOCUS_GAINED) listener.focusGained(focus);
          else listener.focusLost(focus);
        }
        assert repainted[0] : "focus transition did not repaint: " + event;
      }
    } finally { RepaintManager.setCurrentManager(previous); }
  }

  private static void reject(Runnable action) { try { action.run(); throw new AssertionError("invalid data accepted"); } catch(IllegalArgumentException expected) { } }
  private static void layout(Container parent) { parent.doLayout(); for(Component child:parent.getComponents()) if(child instanceof Container c) layout(c); }
  private static String labels(Component c) { return find(c,JLabel.class).stream().map(JLabel::getText).reduce("",(a,b)->a+" "+b); }
  private static <T> List<T> find(Component root,Class<T> type) {
    List<T> result=new ArrayList<>(); if(type.isInstance(root))result.add(type.cast(root));
    if(root instanceof Container p)for(Component c:p.getComponents())result.addAll(find(c,type)); return result;
  }
}