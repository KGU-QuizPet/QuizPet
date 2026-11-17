package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;

public class SubmissionServiceTest {
  private static final List<Path> DIRECTORIES = new ArrayList<>();
  public static void main(String[] args) throws Exception {
    boolean enabled = false;
    assert enabled = true;
    if (!enabled) throw new AssertionError("Run with -ea");
    try {
      checkRewardsAndRetry();
      checkFailureDoesNotAdvance();
      checkRecovery();
      checkCorruptionAndConflict();
      checkConcurrentFirstCorrect();
      checkMixedRewardPaths();
      checkOverflowDoesNotPoisonJournal();
      checkStatOverflowDoesNotPoisonJournal();
    } finally {
      for (Path directory : DIRECTORIES) {
        try (var paths = Files.walk(directory)) {
          for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
            Files.deleteIfExists(path);
          }
        }
      }
    }
    System.out.println("SubmissionService checks passed (rewards, retries, partial recovery and concurrency)");
  }

  private static void checkRewardsAndRetry() throws Exception {
    Path dir = setup();
    SubmissionService service = new SubmissionService(dir);
    QuizSession first = session("S1");
    LearningRecord saved = service.submit(first, "O");
    assert first.isFinished();
    assert saved.isCorrect() && saved.getEarnedXp() == 10;
    assert pet(dir).getTotalXp() == 10 && pet(dir).getStats().getIntelligence() == 11;
    assert service.submit(first, "O").getRecordId().equals(saved.getRecordId());
    assert service.submit(session("S1"), "O").getRecordId().equals(saved.getRecordId());
    try { service.submit(first, "X"); throw new AssertionError("conflicting retry accepted"); }
    catch (IllegalStateException expected) { }
    assert service.submit(session("S2"), "O").getEarnedXp() == 0;
    assert service.submit(session("S3"), "X").getEarnedXp() == 0;
    assert pet(dir).getTotalXp() == 10;
    assert new RecordRepository(dir.resolve("record.txt")).findAll().size() == 3;
    QuizSession ended = session("ENDED");
    ended.end();
    try { service.submit(ended, "O"); throw new AssertionError("ended session accepted"); }
    catch (IllegalStateException expected) { }
  }

  private static void checkFailureDoesNotAdvance() throws Exception {
    Path dir = setup();
    Files.createDirectory(dir.resolve("submission-pending.txt"));
    QuizSession quiz = session("FAIL");
    try { new SubmissionService(dir).submit(quiz, "O"); throw new AssertionError("save error ignored"); }
    catch (IOException expected) { }
    assert quiz.getAnsweredCount() == 0 && !quiz.isFinished();
    assert pet(dir).getTotalXp() == 0;
  }

  private static void checkRecovery() throws Exception {
    for (int phase = 0; phase < 3; phase++) {
      Path dir = setup();
      User other = new UserService(new UserRepository(dir.resolve("user.txt"))).createUser("다른사람");
      Pet otherPet = new PetService(new PetRepository(dir.resolve("pet.txt")),
          new UserRepository(dir.resolve("user.txt"))).createPet(other.getUserId(), "다른펫", "TURTLE");
      String before = "P0001|U0001|펫|DUCK|0|10|10|10|10";
      String after = "P0001|U0001|펫|DUCK|10|10|11|10|10";
      String record = "R1|U0001|Q1|RECOVERY|O|true|10|2026-11-16T20:00:00";
      Files.writeString(dir.resolve("submission-pending.txt"), "QUIZPET_SUBMISSION_V1\n" + before + "\n" + after + "\n" + record + "\n");
      if (phase > 0) new PetRepository(dir.resolve("pet.txt")).save(new Pet("P0001", "U0001", "펫",
          PetAppearance.DUCK, 10, new PetStats(10, 11, 10, 10)));
      if (phase > 1) new RecordRepository(dir.resolve("record.txt")).save(new LearningRecord("R1", "U0001", "Q1",
          "RECOVERY", "O", true, 10, LocalDateTime.parse("2026-11-16T20:00:00")));
      SubmissionService restarted = new SubmissionService(dir);
      restarted.recover();
      assert pet(dir).getTotalXp() == 10;
      assert new PetRepository(dir.resolve("pet.txt")).findById(otherPet.getPetId()).orElseThrow().getTotalXp() == 0;
      assert new RecordRepository(dir.resolve("record.txt")).findAll().size() == 1;
      assert restarted.submit(session("RECOVERY"), "O").getRecordId().equals("R1");
      assert !Files.exists(dir.resolve("submission-pending.txt"));
    }
  }

  private static void checkCorruptionAndConflict() throws Exception {
    Path dir = setup();
    byte[] original = Files.readAllBytes(dir.resolve("pet.txt"));
    Files.writeString(dir.resolve("submission-pending.txt"), "broken\n");
    try { new SubmissionService(dir).recover(); throw new AssertionError("corrupt journal accepted"); }
    catch (IOException expected) { }
    assert java.util.Arrays.equals(original, Files.readAllBytes(dir.resolve("pet.txt")));
    Files.writeString(dir.resolve("submission-pending.txt"), "QUIZPET_SUBMISSION_V1\n"
        + "P0001|U0001|펫|DUCK|20|10|12|10|10\n"
        + "P0001|U0001|펫|DUCK|30|10|13|10|10\n"
        + "R1|U0001|Q1|S1|O|true|10|2026-11-16T20:00:00\n");
    try { new SubmissionService(dir).recover(); throw new AssertionError("conflicting pet overwritten"); }
    catch (IOException expected) { }
    assert java.util.Arrays.equals(original, Files.readAllBytes(dir.resolve("pet.txt")));
  }

  private static void checkConcurrentFirstCorrect() throws Exception {
    Path dir = setup();
    var pool = Executors.newFixedThreadPool(6);
    try {
      List<Callable<LearningRecord>> tasks = new ArrayList<>();
      for (int i = 0; i < 30; i++) {
        String id = "C" + i;
        tasks.add(() -> new SubmissionService(dir).submit(session(id), "O"));
      }
      int xp = 0;
      for (var result : pool.invokeAll(tasks)) xp += result.get().getEarnedXp();
      assert xp == 10 && pet(dir).getTotalXp() == 10;
      assert new RecordRepository(dir.resolve("record.txt")).findAll().size() == 30;
    } finally { pool.shutdownNow(); }
  }

  private static void checkMixedRewardPaths() throws Exception {
    Path dir = setup();
    var read = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    PetRepository pausedRepository = new PetRepository(dir.resolve("pet.txt")) {
      @Override
      public java.util.Optional<Pet> findById(String id) throws IOException {
        java.util.Optional<Pet> loaded = super.findById(id);
        read.countDown();
        try {
          if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IOException("timed out waiting to release pet read");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException(e);
        }
        return loaded;
      }
    };
    var pool = Executors.newFixedThreadPool(2);
    try {
      PetService legacy = new PetService(pausedRepository, new UserRepository(dir.resolve("user.txt")));
      var sports = pool.submit(() -> legacy.applyCorrectAnswerReward("P0001", "SPORTS"));
      assert read.await(5, java.util.concurrent.TimeUnit.SECONDS) : "legacy read did not start";
      var science = pool.submit(() -> new SubmissionService(dir).submit(session("MIXED"), "O"));
      try {
        // The fixed path waits for the legacy read-modify-save to release the common lock.
        science.get(1, java.util.concurrent.TimeUnit.SECONDS);
      } catch (java.util.concurrent.TimeoutException expected) {
        assert !science.isDone();
      } finally {
        release.countDown();
      }
      sports.get(5, java.util.concurrent.TimeUnit.SECONDS);
      assert science.get(5, java.util.concurrent.TimeUnit.SECONDS).getEarnedXp() == 10;
      Pet saved = pet(dir);
      assert saved.getTotalXp() == 20 : "mixed rewards lost XP: " + saved.getTotalXp();
      assert saved.getStats().getIntelligence() == 11 : "SCIENCE reward overwritten";
      assert saved.getStats().getStrength() == 11 : "SPORTS reward overwritten";
      assert new RecordRepository(dir.resolve("record.txt")).findAll().size() == 1;
      assert !Files.exists(dir.resolve("submission-pending.txt"));
    } finally {
      release.countDown();
      pool.shutdownNow();
    }
  }

  private static void checkOverflowDoesNotPoisonJournal() throws Exception {
    Path dir = setup();
    new PetRepository(dir.resolve("pet.txt")).save(new Pet("P0001", "U0001", "펫",
        PetAppearance.DUCK, Integer.MAX_VALUE, new PetStats()));
    QuizSession quiz = session("OVERFLOW");
    try { new SubmissionService(dir).submit(quiz, "O"); throw new AssertionError("overflow accepted"); }
    catch (IllegalArgumentException expected) { }
    assert quiz.getAnsweredCount() == 0;
    assert pet(dir).getTotalXp() == Integer.MAX_VALUE;
    assert !Files.exists(dir.resolve("submission-pending.txt"));
  }

  private static void checkStatOverflowDoesNotPoisonJournal() throws Exception {
    for (Category category : Category.values()) {
      for (int initial : List.of(Integer.MAX_VALUE, Integer.MAX_VALUE - 1)) {
        Path dir = setup();
        PetRepository pets = new PetRepository(dir.resolve("pet.txt"));
        PetStats stats = new PetStats(
            category == Category.SPORTS ? initial : 10,
            category.getStatType().equals("INT") ? initial : 10,
            category == Category.LANGUAGE ? initial : 10,
            category == Category.HISTORY ? initial : 10);
        pets.save(new Pet("P0001", "U0001", "펫", PetAppearance.DUCK, 0, stats));
        byte[] original = Files.readAllBytes(dir.resolve("pet.txt"));
        QuizSession quiz = new QuizSession("STAT-BOUNDARY", "U0001", category,
            List.of(new OXQuestion("Q1", category, "C1", "질문", "O", "해설")));
        SubmissionService service = new SubmissionService(dir);
        if (initial == Integer.MAX_VALUE) {
          try {
            service.submit(quiz, "O");
            throw new AssertionError(category + " overflow accepted");
          } catch (IllegalArgumentException expected) { }
          assert quiz.getAnsweredCount() == 0 && !quiz.isFinished();
          assert java.util.Arrays.equals(original, Files.readAllBytes(dir.resolve("pet.txt")));
          assert !Files.exists(dir.resolve("record.txt"));
        } else {
          assert service.submit(quiz, "O").getEarnedXp() == 10;
          assert pet(dir).getStats().get(category.getStatType()) == Integer.MAX_VALUE;
          assert pet(dir).getTotalXp() == 10;
          assert quiz.isFinished();
        }
        assert !Files.exists(dir.resolve("submission-pending.txt"));
        service.recover();
        UserRepository users = new UserRepository(dir.resolve("user.txt"));
        User other = new UserService(users).createUser("다른사용자");
        new PetService(pets, users).createPet(other.getUserId(), "다른펫", "TURTLE");
        QuizSession normal = new QuizSession("NORMAL", other.getUserId(), Category.SCIENCE,
            List.of(new OXQuestion("Q1", Category.SCIENCE, "C1", "질문", "O", "해설")));
        assert new SubmissionService(dir).submit(normal, "O").getEarnedXp() == 10;
        assert pets.findByUserId(other.getUserId()).orElseThrow().getTotalXp() == 10;
      }
    }
  }

  private static Path setup() throws IOException {
    Path dir = Files.createTempDirectory("quizpet-submit-");
    DIRECTORIES.add(dir);
    User user = new UserService(new UserRepository(dir.resolve("user.txt"))).createUser("사용자");
    new PetService(new PetRepository(dir.resolve("pet.txt")), new UserRepository(dir.resolve("user.txt")))
        .createPet(user.getUserId(), "펫", "DUCK");
    return dir;
  }

  private static Pet pet(Path dir) throws IOException {
    return new PetRepository(dir.resolve("pet.txt")).findById("P0001").orElseThrow();
  }

  private static QuizSession session(String id) {
    return new QuizSession(id, "U0001", Category.SCIENCE,
        List.of(new OXQuestion("Q1", Category.SCIENCE, "C1", "질문", "O", "해설")));
  }
}
