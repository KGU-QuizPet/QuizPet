package main.java.com.quizpet.service;

import java.nio.file.*;
import java.util.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.*;

public final class ReviewSubmissionTest {
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("quizpet-review-submit-");
    try {
      UserRepository users = new UserRepository(dir.resolve("user.txt"));
      PetRepository pets = new PetRepository(dir.resolve("pet.txt"));
      User user = new UserService(users).createUser("복습사용자");
      new PetService(pets, users).createPet(user.getUserId(), "복습펫", "DUCK");
      List<Question> pool = new ArrayList<>();
      QuestionRepository repository = new QuestionRepository();
      for (Category category : Category.values()) pool.add(repository.findByCategory(category).get(0));
      pool.add(repository.findAll().stream().filter(q -> q instanceof MultipleChoiceQuestion).findFirst().orElseThrow());
      pool.add(repository.findAll().stream().filter(q -> q instanceof ShortAnswerQuestion).findFirst().orElseThrow());
      List<String> ids = pool.stream().map(Question::getQuestionId).distinct().toList();
      QuizService quizzes = new QuizService(repository);
      SubmissionService submissions = new SubmissionService(dir);
      Question first = pool.get(0);
      submissions.submit(new QuizSession("OLD", user.getUserId(), first.getCategory(), List.of(first)), first.getAnswer());
      QuizSession review = quizzes.startReviewSession(user.getUserId(), ids);
      byte[] before = Files.readAllBytes(dir.resolve("pet.txt"));
      Files.createDirectory(dir.resolve("submission-pending.txt"));
      try { submissions.submit(review, review.getCurrentQuestion().getAnswer()); throw new AssertionError(); }
      catch (java.io.IOException expected) { }
      assert review.getAnsweredCount() == 0;
      assert Arrays.equals(before, Files.readAllBytes(dir.resolve("pet.txt")));
      Files.delete(dir.resolve("submission-pending.txt"));
      Map<String, Integer> expectedStats = new HashMap<>(Map.of("STR", 10, "INT", 10, "FOC", 10, "WIS", 10));
      for (Question q : review.getQuestions()) {
        LearningRecord saved = submissions.submit(review, q.getAnswer());
        assert saved.getEarnedXp() == (q.getQuestionId().equals(first.getQuestionId()) ? 0 : 10);
        assert submissions.submit(review, q.getAnswer()).getRecordId().equals(saved.getRecordId());
        expectedStats.merge(q.getCategory().getStatType(), 1, Integer::sum);
        if (!review.isFinished()) review.nextQuestion();
      }
      Pet pet = pets.findByUserId(user.getUserId()).orElseThrow();
      assert pet.getTotalXp() == ids.size() * 10;
      for (String type : expectedStats.keySet()) assert pet.getStats().get(type) == expectedStats.get(type);
      QuizSession again = quizzes.startReviewSession(user.getUserId(), ids);
      while (!again.isFinished()) {
        assert submissions.submit(again, again.getCurrentQuestion().getAnswer()).getEarnedXp() == 0;
        if (!again.isFinished()) again.nextQuestion();
      }
      assert pets.findByUserId(user.getUserId()).orElseThrow().getTotalXp() == pet.getTotalXp();
      assert new RecordRepository(dir.resolve("record.txt")).findAll().size() == 1 + ids.size() * 2;
      User other = new UserService(users).createUser("다른사용자");
      new PetService(pets, users).createPet(other.getUserId(), "다른펫", "TURTLE");
      QuizSession separate = quizzes.startReviewSession(other.getUserId(), List.of(first.getQuestionId()));
      assert submissions.submit(separate, first.getAnswer()).getEarnedXp() == 10;
      assert pets.findByUserId(user.getUserId()).orElseThrow().getTotalXp() == pet.getTotalXp();
    } finally {
      try (var paths = Files.walk(dir)) {
        for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
      }
    }
    System.out.println("Mixed review rewards, idempotent retry, save failure and user isolation checks passed");
  }
}
