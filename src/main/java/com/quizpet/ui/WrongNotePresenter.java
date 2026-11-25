package main.java.com.quizpet.ui;

import java.util.List;
import main.java.com.quizpet.model.Question;
import main.java.com.quizpet.service.WrongAnswerService.WrongAnswer;
import main.java.com.quizpet.ui.screen.HistoryPanel.WrongNoteView;

/** 서비스의 집계 결과를 표시 데이터로만 변환합니다. 채점·파일 조회·상태 재판정은 하지 않습니다. */
public final class WrongNotePresenter {
  private WrongNotePresenter() { }

  public static WrongNoteView toView(String categoryId, int total, List<WrongAnswer> items, boolean filtered) {
    return new WrongNoteView(categoryId, total, 0, items.stream().map(WrongNotePresenter::toItem).toList(), true, filtered);
  }

  private static WrongNoteView.WrongItem toItem(WrongAnswer item) {
    Question question = item.question().orElse(null);
    String category = item.category().map(value -> value.getName()).orElse("미분류");
    String prompt = question == null ? "문제 정보 없음" : question.getPrompt();
    String type = question == null ? "확인 불가" : question.getType();
    String status = switch (item.reviewStatus()) {
      case NEEDS_REVIEW -> "복습 필요";
      case COMPLETED -> "복습 완료";
      case AMBIGUOUS -> "상태 확인 필요";
    };
    String correct = question == null ? "확인 불가" : answer(question, question.getAnswer());
    String explanation = question == null ? "문제 정보 없음" : question.getExplanation();
    String lastAnswer = item.latestRecords().size() == 1
        ? answer(question, item.latestRecords().get(0).getSelectedAnswer()) : "동일 시각 제출 여러 건 (상세 확인)";
    StringBuilder details = new StringBuilder("문제 ID: ").append(item.questionId())
        .append("\n분야: ").append(category).append(" / 유형: ").append(type)
        .append("\n상태: ").append(status).append(" / 누적 오답: ").append(item.wrongCount())
        .append("\n마지막 오답: ").append(item.lastWrongAt()).append("\n마지막 풀이: ").append(item.lastAnsweredAt())
        .append("\n\n문제: ").append(prompt).append("\n정답: ").append(correct)
        .append("\n해설: ").append(explanation);
    if (item.latestRecords().size() > 1) details.append("\n동일 최종 시각 제출이 여러 건입니다. 표시 순서는 실제 제출 순서를 의미하지 않습니다.");
    details.append("\n\n제출 이력 (답안 원문 보존)\n");
    for (var record : item.records()) {
      details.append(record.getAnsweredAt()).append(" / ").append(record.isCorrect() ? "정답" : "오답")
          .append(" / 기록 ID: ").append(record.getRecordId()).append("\n답안: ")
          .append(record.getSelectedAnswer()).append("\n");
      if (question != null && question.getType().equals("MULTIPLE_CHOICE")) {
        details.append("선택지: ").append(answer(question, record.getSelectedAnswer())).append("\n");
      }
    }
    return new WrongNoteView.WrongItem(item.questionId(), category, prompt, lastAnswer, correct,
        explanation, item.wrongCount(), status, item.lastAnsweredAt(), details.toString());
  }

  private static String answer(Question question, String raw) {
    return question == null ? raw : WrongNoteView.WrongItem.of(question, raw).lastAnswer();
  }
}