package main.java.com.quizpet.ui.component;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.component.AnswerFeedbackPanel.AnswerFeedbackView;

public class AnswerFeedbackPetTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(AnswerFeedbackPetTest::run);
    System.out.println("AnswerFeedbackPetTest passed");
  }

  private static void run() {
    AnswerFeedbackPanel feedback = new AnswerFeedbackPanel();
    feedback.setPet("TURTLE");

    // 오답이면 시무룩한 표정과 학습 카드를 권하는 말풍선을 보여 준다.
    feedback.showFeedback(view(false, 0, true));
    assert feedback.getPetStage().getMood() == PetMood.SAD;
    assert feedback.getPetStage().getMessage().equals("으앙… 카드를 한 번 더 볼까?");
    assert feedback.getPetStage().hasImage() : "turtle_sad.png를 찾지 못함";

    // 연결된 학습 카드가 없으면 카드를 권하지 않는다.
    feedback.showFeedback(view(false, 0, false));
    assert feedback.getPetStage().getMood() == PetMood.SAD;
    assert !feedback.getPetStage().getMessage().contains("카드");

    // 정답이면 기뻐하고, 이미 맞힌 문제(보상 0)는 다른 문구를 보여 준다.
    feedback.showFeedback(view(true, 10, true));
    assert feedback.getPetStage().getMood() == PetMood.HAPPY;
    assert feedback.getPetStage().getMessage().equals("정답이야! 머리가 똑똑해진 느낌!");
    feedback.showFeedback(view(true, 0, true));
    assert feedback.getPetStage().getMood() == PetMood.HAPPY;
    assert feedback.getPetStage().getMessage().equals("정답! 이미 맞힌 문제야.");

    // 다시 오답이 되면 표정도 되돌아간다.
    feedback.showFeedback(view(false, 0, true));
    assert feedback.getPetStage().getMood() == PetMood.SAD;

    // 긴 말풍선은 두 줄로 나뉘고, 각 줄은 최대 폭을 넘지 않는다.
    FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
        .getFontMetrics(new Font(Font.DIALOG, Font.BOLD, 13));
    List<String> lines = PetStage.wrap("아쉽다. 카드를 다시 볼까?", fm, 100, 2);
    assert lines.size() == 2 : lines;
    for (String line : lines) assert fm.stringWidth(line) <= 100 : line;
    assert String.join(" ", lines).equals("아쉽다. 카드를 다시 볼까?") : lines;
    List<String> clipped = PetStage.wrap("아주 아주 아주 아주 아주 아주 아주 긴 문장입니다", fm, 60, 2);
    assert clipped.size() == 2 && clipped.get(1).endsWith("…") : clipped;
    assert PetStage.wrap("짧아", fm, 120, 2).equals(List.of("짧아"));
  }

  private static AnswerFeedbackView view(boolean correct, int xp, boolean hasCard) {
    return new AnswerFeedbackView(correct, "O", "X", "해설", "INT", xp > 0 ? 1 : 0, xp, hasCard, false);
  }
}
