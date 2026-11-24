package main.java.com.quizpet.ui.screen;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.*;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.component.CategoryAccuracyChart;
import main.java.com.quizpet.ui.component.CategoryAccuracyChart.DisplayState;
import main.java.com.quizpet.ui.screen.HistoryPanel.HistoryView;

public final class HistoryChartIntegrationTest {
  public static void main(String[] args) throws Exception {
    boolean enabled=false; assert enabled=true;if(!enabled)throw new AssertionError("Run with -ea");
    SwingUtilities.invokeAndWait(()->{
      HistoryPanel panel=new HistoryPanel();
      CategoryAccuracyChart chart=find(panel,CategoryAccuracyChart.class).get(0);
      List<HistoryView.CategoryRate> rates=new ArrayList<>();
      for(Category c:Category.values())rates.add(new HistoryView.CategoryRate(c.name(),c.getName(),3,
          c==Category.SCIENCE?0:c==Category.COMMON?100:66.7));
      var row=new HistoryView.RecordRow(LocalDateTime.of(2026,11,24,12,0),"과학","과거 문제 긴 원문 ".repeat(20),"  SA raw Answer  ",false,0);
      panel.showHistory(new HistoryView(null,3,66.7,20,rates,List.of(row)));
      assert chart.getDisplayState(Category.SCIENCE)==DisplayState.RECORDED;
      assert labels(panel).contains("66.7%") && labels(panel).contains("20");
      JTable table=visibleTable(panel); assert table.getRowCount()==1;
      table.setRowSelectionInterval(0,0);
      assert find(panel,JTextArea.class).stream().anyMatch(t->t.getText().contains(row.prompt())&&t.getText().contains(row.myAnswer()));
      var partial=rates.stream().filter(rate->!rate.categoryId().equals("HISTORY")).toList();
      panel.showHistory(new HistoryView(null,3,66.7,20,partial,List.of(row),Set.of(Category.HISTORY)));
      assert chart.getDisplayState(Category.HISTORY)==DisplayState.UNAVAILABLE;
      assert chart.getDisplayState(Category.SCIENCE)==DisplayState.RECORDED;
      // PR #48 Controller contract: unresolved content keeps overall values and sends no rates.
      panel.showHistory(new HistoryView(null,3,66.7,20,List.of(),List.of(row)));
      for(Category c:Category.values()) assert chart.getDisplayState(c)==DisplayState.UNAVAILABLE;
      assert visibleTable(panel).getRowCount()==1 && labels(panel).contains("66.7%");
      panel.showHistoryError("파일 조회 실패");
      for(Category c:Category.values())assert chart.getDisplayState(c)==DisplayState.ERROR;
      assert find(panel,JTable.class).stream().allMatch(t->t.getRowCount()==0);
      panel.showHistory(new HistoryView(null,0,0,0,List.of(),List.of()));
      for(Category c:Category.values())assert chart.getDisplayState(c)==DisplayState.NOT_STARTED;
      assert find(panel,JTable.class).stream().allMatch(t->t.getRowCount()==0);
      assert find(panel,JTextArea.class).stream().noneMatch(t->t.getText().contains(row.prompt()));
      panel.showHistory(new HistoryView("COMMON",3,100,10,rates,List.of(
          new HistoryView.RecordRow(row.answeredAt(),"상식","현재 문제","O",true,10))));
      assert visibleTable(panel).getValueAt(0,2).equals("현재 문제");
      for(int width:List.of(320,800,1280,1920)){
        panel.setSize(width,800); for(int i=0;i<8;i++)layout(panel);
        BufferedImage image=new BufferedImage(width,800,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
        JSplitPane columns=find(panel,JSplitPane.class).get(0);
        assert !columns.getLeftComponent().getBounds().intersects(columns.getRightComponent().getBounds());
        assert chart.getWidth()>0 && visibleTable(panel).getWidth()>0;
      }
    });
    System.out.println("History chart integration checks passed");
  }
  private static JTable visibleTable(Component root){return find(root,JTable.class).stream().filter(t->t.isShowing()||t.getParent().getParent().getParent().isVisible()).filter(t->t.getRowCount()>0).findFirst().orElseThrow();}
  private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container p)layout(p);}
  private static String labels(Component c){return find(c,JLabel.class).stream().map(JLabel::getText).reduce("",(a,b)->a+" "+b);}
  private static <T> List<T> find(Component c,Class<T> type){List<T> result=new ArrayList<>();if(type.isInstance(c))result.add(type.cast(c));if(c instanceof Container p)for(Component child:p.getComponents())result.addAll(find(child,type));return result;}
}