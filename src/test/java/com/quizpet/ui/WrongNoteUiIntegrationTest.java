package main.java.com.quizpet.ui;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.ui.screen.HistoryPanel;

public class WrongNoteUiIntegrationTest {
 static int seq;
 static final LocalDateTime TIME=LocalDateTime.of(2026,11,25,12,0);
 public static void main(String[]args)throws Exception{
  boolean enabled=false;assert enabled=true;if(!enabled)throw new AssertionError("Run with -ea");
  Path dir=Files.createTempDirectory("quizpet-wrong-note-ui-");
  try{SwingUtilities.invokeAndWait(()->{try{check(dir);}catch(Exception e){throw new AssertionError(e);}});}
  finally{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  System.out.println("Wrong-note UI integration checks passed");
 }
 static void check(Path dir)throws Exception{
  AppController app=new AppController(dir); User a=app.createUser("Owner");app.createPet("Pet","DUCK");
  User b=app.createUser("Other");app.createPet("OtherPet","TURTLE");app.selectUser(a.getUserId());
  app.showWrongNote(null,false);assert text(app.getView()).contains("아직 틀린 문제가 없습니다.");
  var q=new QuestionRepository();var old=new QuestionRepository(Path.of("data/content/question-legacy.txt")).findAll().get(0);
  Question sa=q.findAll().stream().filter(x->x.getType().equals("SA")).findFirst().orElseThrow();
  Question common=q.findByCategory(Category.COMMON).get(0);
  Question ox=q.findAll().stream().filter(x->x instanceof OXQuestion&& !x.getQuestionId().equals(common.getQuestionId())).findFirst().orElseThrow();
  Question choice=q.findAll().stream().filter(x->x instanceof MultipleChoiceQuestion).findFirst().orElseThrow();
  RecordRepository rr=new RecordRepository(dir.resolve("record.txt"));
  save(rr,a,sa,false,0,"  MiXeD SA answer  ");save(rr,a,sa,true,1,"SA right");
  save(rr,a,common,false,0,"X");save(rr,a,common,false,1,"X");
  save(rr,a,ox,false,2,"X");save(rr,a,ox,true,2,"O");
  save(rr,a,old,false,0,"old answer");save(rr,a,choice,false,1,"2");
  rr.save(new LearningRecord("MISSING",a.getUserId(),"UNKNOWN","UNKNOWN","raw answer",false,0,TIME));
  byte[] before=Files.readAllBytes(dir.resolve("record.txt"));
  app.showHistory(null);click(app.getView(),"오답노트");
  JTable table=table(app.getView());assert table.getRowCount()==6;
  String cells=cells(table);assert cells.contains("복습 필요")&&cells.contains("복습 완료")&&cells.contains("상태 확인 필요");
  assert cells.contains(old.getPrompt());
  for(int row=0;row<table.getRowCount();row++){
   table.setRowSelectionInterval(row,row);String detail=details(app.getView());
   assert detail.contains("문제 ID:")&&detail.contains("마지막 오답:")&&detail.contains("기록 ID:");
   if(table.getValueAt(row,6).equals(sa.getQuestionId()))assert detail.contains("  MiXeD SA answer  ");
   if(table.getValueAt(row,6).equals(ox.getQuestionId()))assert detail.contains("동일 최종 시각 제출이 여러 건")&&detail.contains("오답")&&detail.contains("정답");
   if(table.getValueAt(row,6).equals("UNKNOWN"))assert detail.contains("문제 정보 없음")&&detail.contains("raw answer");
   if(table.getValueAt(row,6).equals(choice.getQuestionId()))assert detail.contains("선택지: 2. ");
  }
  JButton review=find(app.getView(),JButton.class).stream().filter(x->x.getText().equals("복습하기")).findFirst().orElseThrow();assert !review.isEnabled();
  JCheckBox needed=find(app.getView(),JCheckBox.class).get(0);needed.doClick();assert table(app.getView()).getRowCount()==5;
  select(app.getView(),"상식");assert table(app.getView()).getRowCount()==1;
  select(app.getView(),"미분류");assert table(app.getView()).getRowCount()==1&&cells(table(app.getView())).contains("UNKNOWN");
  click(app.getView(),"이전");assert find(app.getView(),JButton.class).stream().anyMatch(x->x.getText().equals("오답노트"));
  app.showWrongNote("LANGUAGE",true);
  assert text(app.getView()).contains("해당 조건의 오답 기록이 없습니다.");
  app.showWrongNote(null,false);assert table(app.getView()).getRowCount()==6;
  for(int width:List.of(320,1280,1920)){
   app.getView().setSize(width,800);for(int i=0;i<8;i++)layout(app.getView());
   BufferedImage image=new BufferedImage(width,800,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();app.getView().paint(g);g.dispose();
   assert !find(app.getView(),JScrollPane.class).isEmpty();
  }
  app.selectUser(b.getUserId());app.showWrongNote(null,false);assert text(app.getView()).contains("아직 틀린 문제가 없습니다.");assert details(app.getView()).indexOf("MiXeD")<0;
  app.selectUser(a.getUserId());app.showWrongNote(null,false);assert table(app.getView()).getRowCount()==6;
  Files.writeString(dir.resolve("record.txt"),"broken");try{app.showWrongNote("COMMON",false);throw new AssertionError("read error ignored");}catch(java.io.IOException expected){}
  assert text(app.getView()).contains("조회 실패")&&!find(app.getView(),JButton.class).stream().filter(x->x.getText().equals("다시 조회")).toList().isEmpty();
  assert details(app.getView()).indexOf("MiXeD")<0;
  Files.write(dir.resolve("record.txt"),before);click(app.getView(),"다시 조회");assert table(app.getView()).getRowCount()==1 : "Retry must retain the requested category";
  app.showWrongNote(null,false);assert table(app.getView()).getRowCount()==6;
  assert Arrays.equals(before,Files.readAllBytes(dir.resolve("record.txt")));
  AppController restarted=new AppController(dir);restarted.selectUser(a.getUserId());restarted.showWrongNote(null,false);assert table(restarted.getView()).getRowCount()==6;
 }
 static void save(RecordRepository rr,User user,Question q,boolean correct,int sec,String answer)throws Exception{String id="R"+(++seq);rr.save(new LearningRecord(id,user.getUserId(),q.getQuestionId(),id,answer,correct,0,TIME.plusSeconds(sec)));}
 static String cells(JTable t){StringBuilder s=new StringBuilder();for(int r=0;r<t.getRowCount();r++)for(int c=0;c<t.getColumnCount();c++)s.append(t.getValueAt(r,c));return s.toString();}
 static JTable table(Component c){return find(c,JTable.class).stream().filter(t->t.getColumnCount()==7).findFirst().orElseThrow();}
 static String details(Component c){return find(c,JTextArea.class).stream().map(JTextArea::getText).reduce("",String::concat);}
 static String text(Component c){return find(c,JLabel.class).stream().map(JLabel::getText).reduce("",String::concat);}
 static void click(Component c,String name){find(c,JButton.class).stream().filter(b->b.getText().equals(name)).findFirst().orElseThrow().doClick();}
 static void select(Component root,String name){find(root,JToggleButton.class).stream().filter(b->!(b instanceof JCheckBox)&&b.getText().equals(name)).findFirst().orElseThrow(()->new AssertionError("분야 탭 없음: "+name)).doClick();}
 static void layout(Container c){c.doLayout();for(Component x:c.getComponents())if(x instanceof Container p)layout(p);}
 static <T> List<T> find(Component c,Class<T> type){List<T> out=new ArrayList<>();if(!c.isVisible())return out;if(type.isInstance(c))out.add(type.cast(c));if(c instanceof Container p)for(Component x:p.getComponents())out.addAll(find(x,type));return out;}
}