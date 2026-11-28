package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;

/** Submission boundaries and read-only statistics; never uses real save files. */
public final class IntegrationHardeningTest {
  private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 28, 0, 0);
  public static void main(String[] args) throws Exception {
    boolean assertions = false; assert assertions = true;
    if (!assertions) throw new AssertionError("Run with -ea");
    Path root = Files.createTempDirectory("quizpet-hardening-");
    try {
      int max = Integer.MAX_VALUE;
      int[][] cases = {{0,10,1},{40,10,1},{max-10,max-1,1},
          {max-9,10,0},{max,10,0},{40,max,0},{max,max,0}};
      for (int i=0; i<cases.length; i++) boundary(root.resolve("case"+i),cases[i]);
      statistics(root.resolve("statistics"));
      invalid(root.resolve("invalid"));
    } finally {
      try (var paths = Files.walk(root)) {
        for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
      }
    }
    System.out.println("Integration hardening checks passed");
  }

  private static void boundary(Path dir, int[] values) throws Exception {
    var users = new UserRepository(dir.resolve("user.txt"));
    User a = new UserService(users).createUser("Owner");
    User b = new UserService(users).createUser("Other");
    var pets = new PetRepository(dir.resolve("pet.txt"));
    Pet original = new PetService(pets,users).createPet(a.getUserId(),"Pet","DUCK");
    new PetService(pets,users).createPet(b.getUserId(),"OtherPet","TURTLE");
    pets.save(new Pet(original.getPetId(),a.getUserId(),"Pet",PetAppearance.DUCK,values[0],
        new PetStats(10,values[1],10,10)));
    var records = new RecordRepository(dir.resolve("record.txt"));
    records.save(new LearningRecord("other-record",b.getUserId(),"Q1","old","X",false,0,TIME));
    byte[] beforePet = Files.readAllBytes(dir.resolve("pet.txt"));
    byte[] beforeRecords = Files.readAllBytes(dir.resolve("record.txt"));
    var service = new SubmissionService(dir);
    QuizSession quiz = quiz(a.getUserId(),"first");
    if (values[2]==0) {
      try { service.submit(quiz,"O"); throw new AssertionError("Overflow accepted"); }
      catch (SubmissionRejectedException expected) { }
      assert Arrays.equals(beforePet,Files.readAllBytes(dir.resolve("pet.txt")));
      assert Arrays.equals(beforeRecords,Files.readAllBytes(dir.resolve("record.txt")));
      assert quiz.getAnsweredCount()==0;
      // At the same limit a zero-reward wrong answer remains a valid new submission.
      assert service.submit(quiz(a.getUserId(),"wrong"),"X").getEarnedXp()==0;
    } else {
      LearningRecord saved = service.submit(quiz,"O");
      LearningRecord retry = new SubmissionService(dir).submit(quiz(a.getUserId(),"first"),"O");
      assert saved.getRecordId().equals(retry.getRecordId());
      assert saved.getAnsweredAt().equals(retry.getAnsweredAt());
      assert saved.getEarnedXp()==retry.getEarnedXp() && saved.getEarnedXp()==10;
      assert service.submit(quiz(a.getUserId(),"another-session"),"O").getEarnedXp()==0;
      Pet after = pets.findByUserId(a.getUserId()).orElseThrow();
      assert (long)after.getTotalXp()-values[0]==10;
      assert (long)after.getStats().getIntelligence()-values[1]==1;
      if (values[0]==40) assert after.getLevel()==2 && after.getXpInLevel()==0;
    }
    assert !Files.exists(dir.resolve("submission-pending.txt"));
    service.recover();
    assert new RecordService(records).getRecords(b.getUserId()).size()==1;
    assert service.submit(quiz(b.getUserId(),"other-first"),"O").getEarnedXp()==10;
    assert pets.findByUserId(b.getUserId()).orElseThrow().getTotalXp()==10;
  }

  private static QuizSession quiz(String user, String session) {
    return new QuizSession(session,user,Category.SCIENCE,
        List.of(new OXQuestion("Q1",Category.SCIENCE,"C1","Question","O","Explanation")));
  }

  private static void statistics(Path dir) throws Exception {
    var current = new QuestionRepository();
    var lookup = new RecordQuestionLookup(current,Path.of("data/content/question-legacy.txt"));
    Question now = current.findAll().get(0);
    Question old = new QuestionRepository(Path.of("data/content/question-legacy.txt")).findAll().get(0);
    var repo = new RecordRepository(dir.resolve("record.txt"));
    repo.save(new LearningRecord("now","U",now.getQuestionId(),"S1","O",true,Integer.MAX_VALUE,TIME));
    repo.save(new LearningRecord("old","U",old.getQuestionId(),"S2","O",true,Integer.MAX_VALUE,TIME));
    repo.save(new LearningRecord("unknown","U","NOT-IN-CONTENT","S3"," raw ",false,0,TIME));
    repo.save(new LearningRecord("other","OTHER","NOT-IN-CONTENT","S4","X",false,0,TIME));
    byte[] original = Files.readAllBytes(dir.resolve("record.txt"));
    var records = new RecordService(repo);
    var service = new StatisticsService(records,id -> lookup.findById(id).map(Question::getCategory));
    var all = service.getOverallStatistics("U");
    assert all.getCategoryId()==null && all.getTotalAttempts()==3 && all.getCorrectAnswers()==2;
    assert all.getAccuracy()==66.7 && all.getTotalEarnedXp()==2L*Integer.MAX_VALUE;
    assert lookup.findById(now.getQuestionId()).orElseThrow().getCategory()==now.getCategory();
    assert lookup.findById(old.getQuestionId()).orElseThrow().getCategory()==old.getCategory();
    for (Category c:Category.values()) {
      try { service.getCategoryStatistics("U",c.name()); throw new AssertionError("Incomplete totals accepted"); }
      catch (IllegalStateException expected) { assert expected.getMessage().contains("NOT-IN-CONTENT"); }
    }
    assert records.getRecords("U").size()==3;
    assert service.getOverallStatistics("EMPTY").getTotalAttempts()==0;
    assert service.getOverallStatistics("OTHER").getTotalAttempts()==1;
    assert Arrays.equals(original,Files.readAllBytes(dir.resolve("record.txt")));
  }

  private static void invalid(Path dir) throws Exception {
    try { new LearningRecord("R","U","Q","S","O",true,-1,TIME); throw new AssertionError(); }
    catch (IllegalArgumentException expected) { }
    Files.createDirectories(dir);
    Path file = dir.resolve("record.txt");
    Files.writeString(file,"R|U|Q|S|O|true|-1|2026-11-28T00:00:00\n");
    byte[] before = Files.readAllBytes(file);
    try { new RecordRepository(file).findAll(); throw new AssertionError("Invalid XP accepted"); }
    catch (IOException expected) { }
    assert Arrays.equals(before,Files.readAllBytes(file));
  }
}
