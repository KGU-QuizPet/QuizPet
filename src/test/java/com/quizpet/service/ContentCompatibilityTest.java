package main.java.com.quizpet.service;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;
import main.java.com.quizpet.ui.AppController;

public class ContentCompatibilityTest {
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("quizpet-compat-");
    try {
      QuestionRepository current = new QuestionRepository();
      QuestionRepository archive = new QuestionRepository(Path.of("data/content/question-legacy.txt"));
      RecordQuestionLookup lookup = new RecordQuestionLookup(current, Path.of("data/content/question-legacy.txt"));
      assert archive.findAll().size() == 80;
      assert current.findAll().size() == 150;
      assert archive.findAll().stream().allMatch(q -> current.findById(q.getQuestionId()).isEmpty());
      for (Question q : archive.findAll()) assert lookup.findById(q.getQuestionId()).orElseThrow().getPrompt().equals(q.getPrompt());
      Question old = archive.findAll().get(0);
      Question fresh = current.findByCategory(Category.SCIENCE).get(0);
      UserRepository users = new UserRepository(dir.resolve("user.txt"));
      PetRepository pets = new PetRepository(dir.resolve("pet.txt"));
      User user = new UserService(users).createUser("Owner");
      Pet pet = new PetService(pets, users).createPet(user.getUserId(), "Pet", "DUCK");
      pets.save(new Pet(pet.getPetId(), user.getUserId(), pet.getName(), pet.getAppearance(), 10, new PetStats(10,11,10,10)));
      RecordRepository records = new RecordRepository(dir.resolve("record.txt"));
      records.save(new LearningRecord("OLD", user.getUserId(), old.getQuestionId(), "OLD", old.getAnswer(), true, 10, LocalDateTime.of(2026,11,10,12,0)));
      records.save(new LearningRecord("OTHER", "OTHER", "UNKNOWN", "OTHER", "X", false, 0, LocalDateTime.of(2026,11,10,12,0)));
      byte[] before = Files.readAllBytes(dir.resolve("record.txt"));
      StatisticsService stats = new StatisticsService(new RecordService(records), id -> lookup.findById(id).map(Question::getCategory));
      assert stats.getOverallStatistics(user.getUserId()).getTotalEarnedXp() == 10;
      assert stats.getCategoryStatistics(user.getUserId(), "SCIENCE").getTotalAttempts() == 1;
      SwingUtilities.invokeAndWait(() -> {
        try {
          AppController app = new AppController(dir);
          app.selectUser(user.getUserId()); app.showHistory(null); app.showHistory("SCIENCE");
          assert hasTableRows(app.getView(),1);
        } catch (Exception e) { throw new AssertionError(e); }
      });
      assert Arrays.equals(before, Files.readAllBytes(dir.resolve("record.txt")));
      SubmissionService service = new SubmissionService(dir);
      assert service.submit(session(user, "REPEAT", old), old.getAnswer()).getEarnedXp() == 0;
      QuizSession freshSession = session(user,"FRESH",fresh);
      LearningRecord saved = service.submit(freshSession,fresh.getAnswer());
      assert saved.getEarnedXp() == 10;
      assert service.submit(freshSession,fresh.getAnswer()).getRecordId().equals(saved.getRecordId());
      Question common = current.findByCategory(Category.COMMON).get(0);
      assert common.getCategory().getStatType().equals("INT");
      assert service.submit(session(user,"COMMON",common),common.getAnswer()).getEarnedXp() == 10;
      assert pets.findByUserId(user.getUserId()).orElseThrow().getTotalXp() == 30;
      assert pets.findByUserId(user.getUserId()).orElseThrow().getStats().getIntelligence() == 13;
      assert stats.getCategoryStatistics(user.getUserId(),"COMMON").getTotalAttempts() == 1;
      records.save(new LearningRecord("MISSING", user.getUserId(), "TRULY-MISSING", "MISSING", "answer", false, 0, LocalDateTime.now()));
      assert stats.getOverallStatistics(user.getUserId()).getTotalAttempts() == 5;
      try { stats.getCategoryStatistics(user.getUserId(),"SCIENCE"); throw new AssertionError("unknown category guessed"); } catch (IllegalStateException expected) { }
      SwingUtilities.invokeAndWait(() -> {
        try {
          AppController app = new AppController(dir); app.selectUser(user.getUserId()); app.showHistory(null);
          assert hasTableRows(app.getView(),5);
          app.showHistory("SCIENCE");
          assert hasTableRows(app.getView(),5);
        } catch (Exception e) { throw new AssertionError(e); }
      });
      Path changed = dir.resolve("changed.txt");
      Files.writeString(changed, old.getQuestionId()+"|SPORTS|X|OX|O|\nDifferent question\nDifferent explanation\n");
      try { new RecordQuestionLookup(new QuestionRepository(changed),Path.of("data/content/question-legacy.txt")); throw new AssertionError("reused ID accepted"); } catch (IllegalStateException expected) { }
      byte[] preserved = Files.readAllBytes(dir.resolve("record.txt"));
      assert new RecordQuestionLookup(current, dir.resolve("absent.txt")).findById(old.getQuestionId()).isEmpty();
      assert new RecordQuestionLookup(archive, Path.of("data/content/question-legacy.txt")).findById(old.getQuestionId()).isPresent();
      for (String invalid : List.of("broken", "", "Q|SCIENCE|C|OX|O|\nPrompt\nExplanation\nBROKEN\n")) {
        Path damaged = dir.resolve("damaged.txt");
        Files.writeString(damaged, invalid);
        try { new RecordQuestionLookup(current, damaged); throw new AssertionError("corrupt archive accepted"); }
        catch (java.io.IOException expected) { }
      }
      try { new QuestionRepository(dir.resolve("missing-current.txt")); throw new AssertionError("missing current accepted"); }
      catch (java.io.IOException expected) { }
      assert Arrays.equals(preserved, Files.readAllBytes(dir.resolve("record.txt")));
      assert records.findByUserId("OTHER").size() == 1;
      System.out.println("ContentCompatibility checks passed (80 archived IDs, unknown history, reward isolation, COMMON, ID conflicts)");
    } finally {
      try (var files = Files.walk(dir)) { for(Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
    }
  }
  private static QuizSession session(User user,String id,Question question) {
    return new QuizSession(id,user.getUserId(),question.getCategory(),List.of(question));
  }
  private static boolean hasTableRows(Component c,int rows) {
    if(!c.isVisible()) return false;
    if(c instanceof JTable table && table.getRowCount()==rows) return true;
    if(c instanceof Container parent) for(Component child:parent.getComponents()) if(hasTableRows(child,rows)) return true;
    return false;
  }
}