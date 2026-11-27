package main.java.com.quizpet.ui.screen;

import java.awt.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.ui.AppController;
import main.java.com.quizpet.ui.screen.HistoryPanel.HistoryView;

public final class HistoryReadabilityTest {
  public static void main(String[] args) throws Exception {
    boolean enabled=false; assert enabled=true; if(!enabled)throw new AssertionError("Run with -ea");
    Path dir=Files.createTempDirectory("quizpet-readability-");
    try {SwingUtilities.invokeAndWait(()->{try{check(dir);}catch(Exception e){throw new AssertionError(e);}});}
    finally {try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    System.out.println("History readability checks passed");
  }
  static void check(Path dir)throws Exception {
    HistoryPanel panel=new HistoryPanel();
    String raw="  SA MiXeD <html><b>raw</b>\n".repeat(100);
    var row=new HistoryView.RecordRow(LocalDateTime.of(2026,11,28,12,0),"과학","긴 원문 ".repeat(100),raw,true,10);
    var wrong=new HistoryView.RecordRow(row.answeredAt(),"분야 확인 불가","문제 정보 없음",raw,false,0);
    panel.showHistory(new HistoryView(null,500,66.7,4294967294L,List.of(),List.of(row,wrong)),3);
    assert kpi(panel,"전체 오답노트").getText().equals("3");
    assert kpi(panel,"누적 EXP").getText().equals("4294967294");
    JTable table=find(panel,JTable.class).get(0); assert table.getRowHeight()>=40;
    assert cell(table,0,4).getText().equals("정답");assert cell(table,1,4).getText().equals("오답");
    assert cell(table,0,5).getText().equals("+10");assert cell(table,1,5).getText().equals("—");
    JLabel answer=cell(table,0,3);assert answer.getClientProperty("html.disable").equals(Boolean.TRUE);
    assert answer.getToolTipText().contains("&lt;html&gt;");
    table.setRowSelectionInterval(0,0);String detail=find(panel,JTextArea.class).get(0).getText();
    assert detail.contains(raw)&&detail.contains(row.prompt())&&detail.contains("획득 EXP: 10")&&detail.contains("과학");
    for(int[] size:new int[][]{{1280,800},{1600,900},{800,600},{480,600}}){
      panel.setSize(size[0],size[1]);layout(panel);
      for(String title:List.of("총 학습 횟수","정답률","누적 EXP","전체 오답노트")){
        JLabel value=kpi(panel,title);assert value.getWidth()>0&&value.getHeight()>0 : title+" value="+value.getSize()+" at "+size[0];
        assert value.getFontMetrics(value.getFont()).stringWidth(value.getText())<=value.getWidth():"KPI clipped "+title;
      }
      table.scrollRectToVisible(table.getCellRect(1,5,true));
      JScrollPane scroll=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,table);
      assert scroll.getViewport().getViewRect().contains(table.getCellRect(1,5,true)):"XP unreachable";
    }
    panel.showHistoryError("failure");assert kpi(panel,"전체 오답노트").getText().equals("—");
    panel.showHistory(new HistoryView(null,0,0,0,List.of(),List.of()),0);
    assert kpi(panel,"전체 오답노트").getText().equals("0");
    panel.showHistory(new HistoryView(null,0,0,0,List.of(),List.of()));
    assert kpi(panel,"전체 오답노트").getText().equals("—"):"Legacy API must not invent zero";

    AppController app=new AppController(dir);User a=app.createUser("Owner");app.createPet("Pet","DUCK");
    User b=app.createUser("Other");app.createPet("OtherPet","TURTLE");app.selectUser(a.getUserId());
    var questions=new QuestionRepository();Question q=questions.findByCategory(Category.SCIENCE).get(0);
    Question common=questions.findByCategory(Category.COMMON).get(0);var repo=new RecordRepository(dir.resolve("record.txt"));
    for(int i=0;i<3;i++)repo.save(new LearningRecord("r"+i,a.getUserId(),q.getQuestionId(),"s"+i,"O",i==2,i==2?10:0,row.answeredAt().plusSeconds(i)));
    repo.save(new LearningRecord("common",a.getUserId(),common.getQuestionId(),"s4","X",false,0,row.answeredAt()));
    byte[] before=Files.readAllBytes(dir.resolve("record.txt"));
    app.showHistory(null);assert kpi(app.getView(),"전체 오답노트").getText().equals("2"):"Count unique questions incl completed";
    app.showHistory("SCIENCE");assert kpi(app.getView(),"전체 오답노트").getText().equals("2"):"Explicit global KPI contract";
    app.selectUser(b.getUserId());app.showHistory(null);assert kpi(app.getView(),"전체 오답노트").getText().equals("0");
    app.selectUser(a.getUserId());app.showHistory(null);assert kpi(app.getView(),"전체 오답노트").getText().equals("2");
    assert Arrays.equals(before,Files.readAllBytes(dir.resolve("record.txt")));
    repo.save(new LearningRecord("unknown",a.getUserId(),"MISSING","s5"," raw ",false,0,row.answeredAt()));
    app.showHistory(null);assert kpi(app.getView(),"전체 오답노트").getText().equals("3");
    assert kpi(app.getView(),"총 학습 횟수").getText().equals("5");
    app.showWrongNote(null,false);assert find(app.getView(),JSplitPane.class).stream().anyMatch(x->x.getOrientation()==JSplitPane.VERTICAL_SPLIT);
  }
  static JLabel cell(JTable t,int r,int c){return (JLabel)t.prepareRenderer(t.getCellRenderer(r,c),r,c);}
  static JLabel kpi(Component c,String name){return find(c,JLabel.class).stream().filter(x->name.equals(x.getAccessibleContext().getAccessibleName()) && x.getFont().getSize()>=26).findFirst().orElseThrow();}
  static void layout(Container c){for(int i=0;i<8;i++)once(c);}static void once(Container c){c.doLayout();for(Component x:c.getComponents())if(x instanceof Container p)once(p);}
  static <T> List<T> find(Component c,Class<T> type){List<T> out=new ArrayList<>();if(!c.isVisible())return out;if(type.isInstance(c))out.add(type.cast(c));if(c instanceof Container p)for(Component x:p.getComponents())out.addAll(find(x,type));return out;}
}
