package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.model.MultipleChoiceQuestion;
import main.java.com.quizpet.model.OXQuestion;
import main.java.com.quizpet.model.Question;

public class QuestionRepository {
  private final List<Question> questions;

  public QuestionRepository() throws IOException {
    this(Path.of("data/content/question.txt"));
  }

  public QuestionRepository(Path path) throws IOException {
    List<Question> loaded = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    List<String> lines;
    try {
      lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IOException(path + ": 파일을 읽을 수 없습니다.", e);
    }
    for (int i = 0; i < lines.size();) {
      String header = lines.get(i);
      if (header.isBlank()) {
        i++;
        continue;
      }
      int headerLine = i + 1;
      // 각 레코드의 세 줄을 함께 소비해 잘못된 문제의 본문을 다음 헤더로 읽지 않습니다.
      int remaining = lines.size() - i;
      String prompt = remaining >= 2 ? lines.get(i + 1) : "";
      String explanation = remaining >= 3 ? lines.get(i + 2) : "";
      i += Math.min(3, remaining);
      try {
        if (remaining < 3) {
          throw new IllegalArgumentException("문제 또는 해설 줄이 누락되었습니다.");
        }
        String[] fields = header.split("\\|", -1);
        if (fields.length != 6) {
          throw new IllegalArgumentException("첫 줄의 필드는 6개여야 합니다.");
        }
        Category category = Category.valueOf(fields[1]);
        Question question;
        switch (fields[3]) {
          case "OX" -> {
            if (!fields[5].isEmpty()) {
              throw new IllegalArgumentException("OX 문제의 보기는 비어 있어야 합니다.");
            }
            question = new OXQuestion(fields[0], category, fields[2], prompt, fields[4], explanation);
          }
          case "MULTIPLE_CHOICE" -> question = new MultipleChoiceQuestion(
              fields[0], category, fields[2], prompt, fields[4], explanation,
              List.of(fields[5].split(",", -1)));
          default -> throw new IllegalArgumentException("지원하지 않는 문제 유형입니다.");
        }
        if (!ids.add(question.getQuestionId())) {
          throw new IllegalArgumentException("중복된 문제 ID입니다.");
        }
        loaded.add(question);
      } catch (IllegalArgumentException e) {
        System.getLogger(QuestionRepository.class.getName()).log(
            System.Logger.Level.WARNING, path + ":" + headerLine + ": " + e.getMessage());
      }
    }
    questions = List.copyOf(loaded);
  }

  public List<Question> findAll() {
    return questions;
  }

  public Optional<Question> findById(String questionId) {
    return questions.stream()
        .filter(question -> question.getQuestionId().equals(questionId))
        .findFirst();
  }

  public List<Question> findByCategory(Category category) {
    Objects.requireNonNull(category, "category는 필수입니다.");
    return questions.stream()
        .filter(question -> question.getCategory() == category)
        .toList();
  }

  public List<Question> findByContentId(String contentId) {
    return questions.stream()
        .filter(question -> question.getContentId().equals(contentId))
        .toList();
  }
}
