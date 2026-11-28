package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import main.java.com.quizpet.model.LearningRecord;
import main.java.com.quizpet.model.Pet;
import main.java.com.quizpet.model.PetStats;
import main.java.com.quizpet.model.QuizSession;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.RecordRepository;
import main.java.com.quizpet.repository.SubmissionRejectedException;
import main.java.com.quizpet.repository.SubmissionStore;
import main.java.com.quizpet.repository.UserRepository;

/** 답안의 채점·최초 정답 보상·기록 저장을 한 번만 확정합니다. */
public final class SubmissionService {
  private final SubmissionStore store;
  private final PetRepository pets;
  private final UserRepository users;
  private final GrowthService growth;

  public SubmissionService(Path saveDirectory) {
    Objects.requireNonNull(saveDirectory, "saveDirectory는 필수입니다.");
    store = new SubmissionStore(saveDirectory);
    pets = new PetRepository(saveDirectory.resolve("pet.txt"));
    users = new UserRepository(saveDirectory.resolve("user.txt"));
    growth = new GrowthService(new RecordService(new RecordRepository(saveDirectory.resolve("record.txt"))));
  }

  public void recover() throws IOException {
    store.recover();
  }

  public LearningRecord submit(QuizSession session, String selectedAnswer) throws IOException {
    Objects.requireNonNull(session, "session은 필수입니다.");
    return SubmissionStore.locked(() -> {
      store.recover();
      boolean correct = session.getCurrentQuestion().checkAnswer(selectedAnswer);
      if (session.isFinished() && !session.isCurrentQuestionAnswered()) {
        throw new IllegalStateException("종료된 퀴즈 세션입니다.");
      }
      if (session.isCurrentQuestionAnswered()
          && !selectedAnswer.equals(session.getAnswers().get(session.getCurrentQuestion().getQuestionId()))) {
        throw new IllegalStateException("동일 제출의 답안이 다릅니다.");
      }
      if (users.findById(session.getUserId()).isEmpty()) {
        throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
      }
      Pet before = pets.findByUserId(session.getUserId())
          .orElseThrow(() -> new IllegalStateException("사용자의 캐릭터가 없습니다."));
      GrowthService.RewardDecision decision = growth.evaluateReward(session.getUserId(),
          session.getSessionId(), session.getCurrentQuestion().getQuestionId(),
          session.getCategory(), selectedAnswer, correct);
      if (decision.isNewSubmission()) {
        PetStats stats = before.getStats();
        Pet after = new Pet(before.getPetId(), before.getUserId(), before.getName(),
            before.getAppearance(), before.getTotalXp(), new PetStats(stats.getStrength(),
            stats.getIntelligence(), stats.getFocus(), stats.getWisdom()));
        try {
          after.applyReward(decision.getPlannedXp(), decision.getStatType(), decision.getPlannedStatGain());
        } catch (IllegalArgumentException e) {
          // 보상 한도 초과(#74)는 저장 시도 전 거부입니다. 재시도 대기 없이 화면을 이동할 수 있게 합니다.
          throw new SubmissionRejectedException(e);
        }
        store.commit(before, after, decision.getRecord());
      }
      if (!session.isCurrentQuestionAnswered()) session.submitAnswer(selectedAnswer);
      return decision.getRecord();
    });
  }
}
