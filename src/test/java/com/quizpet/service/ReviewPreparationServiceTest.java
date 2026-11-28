package main.java.com.quizpet.service;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.ReviewPreparationService.*;
public final class ReviewPreparationServiceTest {
 public static void main(String[] args)throws Exception {
  boolean enabled=false;assert enabled=true;if(!enabled)throw new AssertionError("Run with -ea");
  Path dir=Files.createTempDirectory("quizpet-review-preparation-");
  try{check(dir);}finally{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  System.out.println("Review preparation checks passed; no real session started");
 }
 static void check(Path dir)throws Exception {
  var current=new QuestionRepository();var rr=new RecordRepository(dir.resolve("record.txt"));
  var wrong=new WrongAnswerService(new RecordService(rr),new RecordQuestionLookup(current,Path.of("data/content/question-legacy.txt")));
  var service=new ReviewPreparationService(wrong,current);
  assert service.getCandidates("EMPTY").isEmpty();assert !service.prepareAll("EMPTY").ready();
  assert reason(service.prepare("U",List.of()))==Reason.EMPTY_SELECTION;
  List<Question> chosen=new ArrayList<>();
  for(String type:List.of("OX","MULTIPLE_CHOICE","SA"))chosen.add(current.findAll().stream().filter(q->q.getType().equals(type)).findFirst().orElseThrow());
  chosen.add(current.findByCategory(Category.COMMON).get(0));
  for(Question q:current.findByCategory(Category.SCIENCE))if(chosen.stream().noneMatch(x->x.getQuestionId().equals(q.getQuestionId()))&&chosen.size()<8)chosen.add(q);
  int n=0;for(Question q:chosen)save(rr,"U",q.getQuestionId(),false,n++);
  save(rr,"U",chosen.get(0).getQuestionId(),true,n++);
  Question old=new QuestionRepository(Path.of("data/content/question-legacy.txt")).findAll().get(0);
  save(rr,"U",old.getQuestionId(),false,n++);save(rr,"U","MISSING",false,n++);
  String otherOnly=current.findAll().stream().map(Question::getQuestionId).filter(id->chosen.stream().noneMatch(q->q.getQuestionId().equals(id))).findFirst().orElseThrow();
  save(rr,"OTHER",otherOnly,false,n++);save(rr,"OTHER",chosen.get(0).getQuestionId(),false,n++);
  byte[] original=Files.readAllBytes(dir.resolve("record.txt"));
  String first=chosen.get(0).getQuestionId(),second=chosen.get(1).getQuestionId();
  List<String> selection=new ArrayList<>(List.of(second,first,second));
  var request=service.prepare("U",selection).request().orElseThrow();
  assert request.questionIds().equals(List.of(second,first));selection.clear();assert request.questionIds().size()==2;
  try{request.questionIds().add("bad");throw new AssertionError();}catch(UnsupportedOperationException expected){}
  try{service.getCandidates("U").clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
  assert service.prepare("U",List.of(first)).request().orElseThrow().questionIds().equals(List.of(first));
  assert !service.prepare("U",List.of()).ready();
  var all=service.prepareAll("U").request().orElseThrow();assert all.questionIds().size()==8;
  assert all.questionIds().equals(service.getCandidates("U").stream().filter(Candidate::selectable).map(c->c.wrongAnswer().questionId()).toList());
  assert reason(service.prepare("U",List.of(otherOnly)))==Reason.NOT_WRONG_FOR_USER;
  assert reason(service.prepare("U",List.of(old.getQuestionId())))==Reason.ARCHIVED_ONLY;
  assert reason(service.prepare("U",List.of("MISSING")))==Reason.CURRENT_QUESTION_MISSING;
  assert reason(service.prepare("U",Arrays.asList((String)null)))==Reason.INVALID_ID;
  assert reason(service.prepare("U",List.of("Q\nBAD")))==Reason.INVALID_ID;
  assert reason(service.prepare("U",List.of("not-owned")))==Reason.NOT_WRONG_FOR_USER;
  assert !service.prepare("U",List.of(first,"MISSING")).ready();
  assert service.revalidate("U",request).ready();
  try{service.revalidate("OTHER",request);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{service.prepare(" ",List.of(first));throw new AssertionError();}catch(IllegalArgumentException expected){}
  assert Arrays.equals(original,Files.readAllBytes(dir.resolve("record.txt")));
  var changed=new QuestionRepository(){@Override public Optional<Question> findById(String id){return Optional.of(new OXQuestion(id,Category.HISTORY,"changed","Other question","O","Other"));}};
  assert reason(new ReviewPreparationService(wrong,changed).prepare("U",List.of(first)))==Reason.CONTENT_MISMATCH;
  Question incomplete=new Question("INCOMPLETE",Category.SCIENCE,"C","prompt","O","explanation"){
   public String getType(){return "CUSTOM";}public boolean checkAnswer(String a){return false;}@Override public String getPrompt(){return "";}};
  Question unsupported=new Question("UNSUPPORTED",Category.SCIENCE,"C","prompt","O","explanation"){
   public String getType(){return "CUSTOM";}public boolean checkAnswer(String a){return false;}};
  var special=new QuestionRepository(){@Override public Optional<Question> findById(String id){return Optional.of(id.equals("INCOMPLETE")?incomplete:unsupported);}};
  var specialWrong=new WrongAnswerService(new RecordService(rr),new RecordQuestionLookup(special,dir.resolve("absent-archive")));
  save(rr,"SPECIAL","INCOMPLETE",false,n++);save(rr,"SPECIAL","UNSUPPORTED",false,n++);
  var specialService=new ReviewPreparationService(specialWrong,special);
  assert reason(specialService.prepare("SPECIAL",List.of("INCOMPLETE")))==Reason.INVALID_CONTENT;
  assert reason(specialService.prepare("SPECIAL",List.of("UNSUPPORTED")))==Reason.UNSUPPORTED_TYPE;
  var probe=new QuizSession("PROBE","U",Category.SCIENCE,chosen);
  long science=chosen.stream().filter(q->q.getCategory()==Category.SCIENCE).count();
  assert probe.getTotalCount()==Math.min(5,science)&&probe.getTotalCount()<all.questionIds().size();
  byte[] afterFixtures=Files.readAllBytes(dir.resolve("record.txt"));service.prepareAll("U");
  assert Arrays.equals(afterFixtures,Files.readAllBytes(dir.resolve("record.txt")));
  Files.writeString(dir.resolve("record.txt"),"broken");byte[] broken=Files.readAllBytes(dir.resolve("record.txt"));
  try{service.getCandidates("U");throw new AssertionError();}catch(IOException expected){}
  assert Arrays.equals(broken,Files.readAllBytes(dir.resolve("record.txt")));
 }
 static Reason reason(Preparation p){assert !p.ready();return p.rejections().get(0).reason();}
 static void save(RecordRepository rr,String user,String id,boolean correct,int seq)throws Exception{
  rr.save(new LearningRecord("R"+seq,user,id,"S"+seq,"  Raw SA Answer  ",correct,0,LocalDateTime.of(2026,11,28,12,0).plusSeconds(seq)));
 }
}
