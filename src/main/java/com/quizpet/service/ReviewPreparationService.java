package main.java.com.quizpet.service;

import java.io.IOException;
import java.util.*;
import main.java.com.quizpet.model.*;
import main.java.com.quizpet.repository.QuestionRepository;
import main.java.com.quizpet.service.WrongAnswerService.WrongAnswer;

/** Read-only preparation, not session creation. BE2 must revalidate at the actual start boundary. */
public final class ReviewPreparationService {
  public enum Reason {
    INVALID_ID, EMPTY_SELECTION, NOT_WRONG_FOR_USER, ARCHIVED_ONLY, CURRENT_QUESTION_MISSING,
    CONTENT_MISMATCH, UNSUPPORTED_TYPE, INVALID_CONTENT
  }
  public record Candidate(WrongAnswer wrongAnswer, Optional<Reason> unavailableReason) {
    public Candidate { Objects.requireNonNull(wrongAnswer); Objects.requireNonNull(unavailableReason); }
    public boolean selectable() { return unavailableReason.isEmpty(); }
  }
  public record Rejection(String questionId, Reason reason) { }
  /** A request only: no session ID, reward or successful-start flag. Order is the caller's selection order. */
  public record ReviewRequest(String userId, List<String> questionIds) {
    public ReviewRequest {
      requireId(userId); questionIds = List.copyOf(questionIds);
      if (questionIds.isEmpty()) throw new IllegalArgumentException("선택 문제가 없습니다.");
      for (String id : questionIds) requireId(id);
      if (new HashSet<>(questionIds).size() != questionIds.size()) throw new IllegalArgumentException("중복 선택입니다.");
    }
  }
  public record Preparation(Optional<ReviewRequest> request, List<Rejection> rejections) {
    public Preparation { request = Objects.requireNonNull(request); rejections = List.copyOf(rejections); }
    public boolean ready() { return request.isPresent(); }
  }

  private final WrongAnswerService wrongAnswers;
  private final QuestionRepository currentQuestions;
  public ReviewPreparationService(WrongAnswerService wrongAnswers, QuestionRepository currentQuestions) {
    this.wrongAnswers = Objects.requireNonNull(wrongAnswers);
    this.currentQuestions = Objects.requireNonNull(currentQuestions);
  }

  public List<Candidate> getCandidates(String userId) throws IOException {
    requireId(userId);
    return wrongAnswers.getWrongAnswers(userId).stream()
        .map(item -> new Candidate(item, Optional.ofNullable(reason(item)))).toList();
  }

  /** All-or-nothing: invalid entries are reported, never silently replaced with random questions.
   * No BE2 count limit is invented here; accepting a mixed or >5-item request does not make it playable. */
  public Preparation prepare(String userId, Collection<String> selectedIds) throws IOException {
    requireId(userId); Objects.requireNonNull(selectedIds, "선택 목록은 필수입니다.");
    Map<String,Candidate> candidates = new LinkedHashMap<>();
    for (Candidate c : getCandidates(userId)) candidates.put(c.wrongAnswer().questionId(), c);
    Set<String> unique = new LinkedHashSet<>(); List<Rejection> rejected = new ArrayList<>();
    for (String id : new ArrayList<>(selectedIds)) {
      if (!validId(id)) { rejected.add(new Rejection(id, Reason.INVALID_ID)); continue; }
      if (!unique.add(id)) continue;
      Candidate c = candidates.get(id);
      if (c == null) rejected.add(new Rejection(id, Reason.NOT_WRONG_FOR_USER));
      else c.unavailableReason().ifPresent(r -> rejected.add(new Rejection(id,r)));
    }
    if (unique.isEmpty() && rejected.isEmpty()) rejected.add(new Rejection("",Reason.EMPTY_SELECTION));
    return rejected.isEmpty()
        ? new Preparation(Optional.of(new ReviewRequest(userId,List.copyOf(unique))),List.of())
        : new Preparation(Optional.empty(),rejected);
  }

  /** Select all currently eligible entries in existing wrong-note display order. Unavailable entries stay viewable. */
  public Preparation prepareAll(String userId) throws IOException {
    return prepare(userId,getCandidates(userId).stream().filter(Candidate::selectable)
        .map(c -> c.wrongAnswer().questionId()).toList());
  }

  /** Reject stale UI selection after a user switch; requery all ownership/content facts before handoff. */
  public Preparation revalidate(String currentUserId, ReviewRequest request) throws IOException {
    requireId(currentUserId); Objects.requireNonNull(request);
    if (!currentUserId.equals(request.userId())) throw new IllegalArgumentException("사용자가 변경되어 선택이 만료됐습니다.");
    return prepare(currentUserId,request.questionIds());
  }

  private Reason reason(WrongAnswer wrong) {
    Optional<Question> found = currentQuestions.findById(wrong.questionId());
    if (found.isEmpty()) return wrong.question().isPresent() ? Reason.ARCHIVED_ONLY : Reason.CURRENT_QUESTION_MISSING;
    Question q = found.get();
    if (!wrong.questionId().equals(q.getQuestionId()) || wrong.question().isEmpty()
        || !sameContent(wrong.question().get(),q)) return Reason.CONTENT_MISMATCH;
    if (q.getCategory()==null || blank(q.getPrompt()) || blank(q.getAnswer()) || blank(q.getContentId()))
      return Reason.INVALID_CONTENT;
    if (q instanceof OXQuestion) return Set.of("O","X").contains(q.getAnswer()) ? null : Reason.INVALID_CONTENT;
    if (q instanceof MultipleChoiceQuestion mc) {
      if (!Set.of("1","2","3","4").contains(q.getAnswer()) || mc.getOptions()==null
          || mc.getOptions().size()!=4 || mc.getOptions().stream().anyMatch(ReviewPreparationService::blank))
        return Reason.INVALID_CONTENT;
      return null;
    }
    if (q instanceof ShortAnswerQuestion) return ShortAnswerQuestion.isSubmittable(q.getAnswer()) ? null : Reason.INVALID_CONTENT;
    return Reason.UNSUPPORTED_TYPE;
  }
  private static boolean sameContent(Question a, Question b) {
    return a.getCategory()==b.getCategory() && Objects.equals(a.getType(),b.getType())
        && Objects.equals(a.getPrompt(),b.getPrompt()) && Objects.equals(a.getAnswer(),b.getAnswer())
        && Objects.equals(a.getContentId(),b.getContentId()) && Objects.equals(a.getExplanation(),b.getExplanation())
        && (!(a instanceof MultipleChoiceQuestion x) || (b instanceof MultipleChoiceQuestion y && Objects.equals(x.getOptions(),y.getOptions())))
        && (!(a instanceof ShortAnswerQuestion x) || (b instanceof ShortAnswerQuestion y && Objects.equals(x.getAlternativeAnswers(),y.getAlternativeAnswers())));
  }
  private static boolean blank(String value) { return value==null || value.isBlank(); }
  private static boolean validId(String id) { return !blank(id) && !id.contains("|") && !id.matches("(?s).*\\R.*"); }
  private static void requireId(String id) { if (!validId(id)) throw new IllegalArgumentException("유효한 식별자가 필요합니다."); }
}
