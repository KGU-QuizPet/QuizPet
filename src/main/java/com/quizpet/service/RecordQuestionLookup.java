package main.java.com.quizpet.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.ShortAnswerQuestion;
import main.java.com.quizpet.repository.QuestionRepository;

/** 기록 조회 전용입니다. 보존 문제는 새 퀴즈 출제에 사용하지 않습니다. */
public final class RecordQuestionLookup {
  private final QuestionRepository current;
  private final List<Question> archived;

  public RecordQuestionLookup(QuestionRepository current, Path archive) throws IOException {
    this.current = Objects.requireNonNull(current);
    Objects.requireNonNull(archive);
    if (Files.notExists(archive)) {
      archived = List.of();
    } else {
      // Repository는 손상된 문제를 경고 후 제외하므로, 보존 파일에서는 누락을 허용하지 않습니다.
      List<String> lines = Files.readAllLines(archive, java.nio.charset.StandardCharsets.UTF_8);
      int expected = 0;
      for (int i = 0; i < lines.size();) {
        if (lines.get(i).isBlank()) { i++; continue; }
        if (lines.size() - i < 3) throw new IOException(archive + ": 보존 문제의 줄이 누락되었습니다.");
        expected++;
        i += 3;
      }
      archived = new QuestionRepository(archive).findAll();
      if (expected == 0 || archived.size() != expected) {
        throw new IOException(archive + ": 보존 문제 파일이 비어 있거나 손상되었습니다.");
      }
    }
    for (Question old : archived) {
      current.findById(old.getQuestionId()).ifPresent(now -> {
        if (!old.getCategory().equals(now.getCategory()) || !old.getType().equals(now.getType())
            || !old.getPrompt().equals(now.getPrompt()) || !old.getAnswer().equals(now.getAnswer())
            || !old.getContentId().equals(now.getContentId())
            || !old.getExplanation().equals(now.getExplanation())
            || (old instanceof MultipleChoiceQuestion a && now instanceof MultipleChoiceQuestion b
                && !a.getOptions().equals(b.getOptions()))
            || (old instanceof ShortAnswerQuestion a && now instanceof ShortAnswerQuestion b
                && !a.getAlternativeAnswers().equals(b.getAlternativeAnswers()))) {
          throw new IllegalStateException("기존 문제 ID가 다른 콘텐츠에 재사용됐습니다: " + old.getQuestionId());
        }
      });
    }
  }

  public Optional<Question> findById(String questionId) {
    Optional<Question> found = current.findById(questionId);
    return found.isPresent() ? found : archived.stream()
        .filter(question -> question.getQuestionId().equals(questionId)).findFirst();
  }
}
