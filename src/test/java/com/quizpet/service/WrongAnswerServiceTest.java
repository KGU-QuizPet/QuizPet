package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.service.WrongAnswerService.*;

public final class WrongAnswerServiceTest {
  private static int sequence;
  private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 24, 12, 0);
  public static void main(String[] args) throws Exception {
    boolean enabled=false; assert enabled=true;
    if(!enabled) throw new AssertionError("Run with -ea");
    Path dir=Files.createTempDirectory("quizpet-wrong-");
    try {
      QuestionRepository current=new QuestionRepository();
      Path archive=Path.of("data/content/question-legacy.txt");
      RecordQuestionLookup lookup=new RecordQuestionLookup(current,archive);
      RecordRepository repo=new RecordRepository(dir.resolve("record.txt"));
      WrongAnswerService service=new WrongAnswerService(new RecordService(repo),lookup);
      assert service.getWrongAnswers("EMPTY").isEmpty();
      Question science=current.findByCategory(Category.SCIENCE).get(0);
      String id=science.getQuestionId();
      save(repo,"A",id,false,0,"X");
      var first=service.getWrongAnswers("A").get(0);
      assert first.wrongCount()==1&&first.reviewStatus()==ReviewStatus.NEEDS_REVIEW;
      save(repo,"A",id,false,1,"wrong again");
      save(repo,"A",id,true,2,"correct");
      var completed=service.getWrongAnswers("A").get(0);
      assert completed.wrongCount()==2&&completed.reviewStatus()==ReviewStatus.COMPLETED;
      assert completed.lastWrongAt().equals(BASE.plusSeconds(1));
      assert completed.lastAnsweredAt().equals(BASE.plusSeconds(2))&&completed.lastCorrect().orElseThrow();
      assert service.getWrongAnswers("A",null,ListPolicy.NEEDS_REVIEW_ONLY).isEmpty();
      save(repo,"A",id,false,3,"wrong later");
      assert service.getWrongAnswers("A").get(0).reviewStatus()==ReviewStatus.NEEDS_REVIEW;
      assert service.getWrongAnswers("B").isEmpty();
      save(repo,"B",id,true,0,"O"); assert service.getWrongAnswers("B").isEmpty();
      save(repo,"CORRECT-FIRST",id,true,0,"O");
      save(repo,"CORRECT-FIRST",id,false,1,"X");
      var correctFirst=service.getWrongAnswers("CORRECT-FIRST").get(0);
      assert correctFirst.wrongCount()==1 && correctFirst.reviewStatus()==ReviewStatus.NEEDS_REVIEW;
      assert correctFirst.lastCorrect().equals(Optional.of(false));
      Question common=current.findByCategory(Category.COMMON).get(0);
      save(repo,"A",common.getQuestionId(),false,4,"X");
      Question sa=current.findAll().stream().filter(q->q.getType().equals("SA")).findFirst().orElseThrow();
      save(repo,"SA",sa.getQuestionId(),false,0,"  MiXeD answer  ");
      var raw=service.getWrongAnswers("SA").get(0);
      assert raw.question().orElseThrow().getType().equals("SA");
      assert raw.latestRecords().get(0).getSelectedAnswer().equals("  MiXeD answer  ");
      assert service.getWrongAnswers("A",Category.COMMON).get(0).questionId().equals(common.getQuestionId());
      assert service.getWrongAnswers("A",Category.SCIENCE).size()==1;
      save(repo,"A","NO-CONTENT",false,5,"original");
      assert service.getUnclassifiedWrongAnswers("A").get(0).category().isEmpty();
      assert service.getWrongAnswers("A").get(0).questionId().equals("NO-CONTENT");
      assert service.getWrongAnswers("A",Category.SCIENCE).size()==1;
      save(repo,"TIES","Z",false,0,"X"); save(repo,"TIES","A",false,0,"X");
      save(repo,"TIES","A",true,0,"O");
      var ties=service.getWrongAnswers("TIES");
      assert ties.get(0).questionId().equals("A")&&ties.get(1).questionId().equals("Z");
      assert ties.get(0).reviewStatus()==ReviewStatus.AMBIGUOUS&&ties.get(0).lastCorrect().isEmpty();
      assert ties.get(0).latestRecords().size()==2;
      assert service.getWrongAnswers("TIES",null,ListPolicy.NEEDS_REVIEW_ONLY).size()==2;
      List<String> originalLines=Files.readAllLines(dir.resolve("record.txt"));
      List<String> reversed=new ArrayList<>(originalLines); Collections.reverse(reversed);
      Files.write(dir.resolve("record.txt"),reversed);
      var reordered=service.getWrongAnswers("TIES");
      assert reordered.stream().map(WrongAnswer::questionId).toList().equals(ties.stream().map(WrongAnswer::questionId).toList());
      assert reordered.get(0).reviewStatus()==ReviewStatus.AMBIGUOUS;
      assert reordered.get(0).latestRecords().stream().map(LearningRecord::getRecordId).toList()
          .equals(ties.get(0).latestRecords().stream().map(LearningRecord::getRecordId).toList());
      Files.write(dir.resolve("record.txt"),originalLines);
      try{ties.clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
      try{ties.get(0).records().clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
      try{ties.get(0).latestRecords().clear();throw new AssertionError();}catch(UnsupportedOperationException expected){}
      QuestionRepository old=new QuestionRepository(archive);
      for(Question q:old.findAll()) save(repo,"LEGACY",q.getQuestionId(),false,0,"raw");
      var legacy=service.getWrongAnswers("LEGACY"); assert legacy.size()==80;
      assert legacy.stream().allMatch(item->item.question().isPresent());
      byte[] original=Files.readAllBytes(dir.resolve("record.txt"));
      service.getWrongAnswers("A");service.getWrongAnswers("LEGACY");
      assert Arrays.equals(original,Files.readAllBytes(dir.resolve("record.txt")));
      for(String bad:new String[]{null,""," ","A|B","A\nB"}) {
        try{service.getWrongAnswers(bad);throw new AssertionError("invalid user accepted");}catch(IllegalArgumentException expected){}
      }
      Path conflict=dir.resolve("conflict.txt");Question q=old.findAll().get(0);
      Files.writeString(conflict,q.getQuestionId()+"|SPORTS|C|OX|O|\nChanged\nExplanation\n");
      try{new RecordQuestionLookup(new QuestionRepository(conflict),archive);throw new AssertionError("ID reused");}catch(IllegalStateException expected){}
      Path brokenArchive=dir.resolve("archive.txt");Files.writeString(brokenArchive,"bad\n");
      try{new RecordQuestionLookup(current,brokenArchive);throw new AssertionError("bad archive");}catch(IOException expected){}
      // Duplicate archived IDs must not silently become two different historical questions.
      List<String> oldLines=Files.readAllLines(archive);String block=String.join("\n",oldLines.subList(0,3))+"\n";
      Files.writeString(brokenArchive,block+block);
      try{new RecordQuestionLookup(current,brokenArchive);throw new AssertionError("duplicate archive");}catch(IOException expected){}
      assert Arrays.equals(original,Files.readAllBytes(dir.resolve("record.txt")));
      for(byte[] damaged:List.of("bad record".getBytes(java.nio.charset.StandardCharsets.UTF_8),new byte[]{(byte)0xc3,0x28})) {
        Files.write(dir.resolve("record.txt"),damaged);
        try{service.getWrongAnswers("A");throw new AssertionError("corrupt record ignored");}catch(IOException expected){}
        assert Arrays.equals(damaged,Files.readAllBytes(dir.resolve("record.txt")));
      }
      System.out.println("WrongAnswerService checks passed: grouping, policies, timestamp ambiguity, categories, SA, legacy and preservation");
    } finally {try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
  }
  private static void save(RecordRepository repo,String user,String question,boolean correct,int second,String answer)throws IOException {
    String id="R"+(++sequence);
    repo.save(new LearningRecord(id,user,question,"S"+id,answer,correct,correct?10:0,BASE.plusSeconds(second)));
  }
}