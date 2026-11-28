package main.java.com.quizpet.ui.component;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import main.java.com.quizpet.model.PetMood;
import main.java.com.quizpet.ui.ScreenManager;
import main.java.com.quizpet.ui.UiStyle;
import main.java.com.quizpet.ui.component.AnswerFeedbackPanel.AnswerFeedbackView;
import main.java.com.quizpet.ui.screen.HomePanel;
import main.java.com.quizpet.ui.screen.QuizResultPanel;

/** 정답 반응 애니메이션·획득 문구, 능력치 증가분·약점 강조, 결과 화면 전후 비교를 확인합니다. */
public class GrowthVisualTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(GrowthVisualTest::run);
    System.out.println("GrowthVisualTest passed");
  }

  private static void run() {
    ambientAnimations();
    petStageAnimations();
    feedbackReactions();
    gaugeGain();
    weakestStats();
    sidebarGrowth();
    resultComparison();
  }

  private static void ambientAnimations() {
    PetStage stage = new PetStatusPanel().getStage();
    stage.setSize(280, 270);
    stage.advanceAmbient(800);
    double tilt = stage.ambientTilt();
    assert Math.abs(tilt - Math.toRadians(3)) < 1e-9 : "오른쪽으로 3도 기울어야 한다";
    stage.advanceAmbient(1600);
    assert Math.abs(stage.ambientTilt() + tilt) < 1e-9 : "왼쪽으로 같은 폭만큼 기울어야 한다";
    stage.advanceAmbient(16);
    assert Math.abs(stage.ambientTilt() + tilt) < 0.002 : "프레임 사이 변화가 작고 연속적이어야 한다";
    stage.advanceAmbient(3200 - 2416);
    assert Math.abs(stage.ambientTilt()) < 1e-9 : "주기 경계에서 처음 자세로 이어져야 한다";
    stage.advanceAmbient(800);
    paint(stage);
    stage.celebrate(List.of());
    assert stage.ambientTilt() == 0 : "점프 중에는 평상시 움직임을 겹치지 않는다";
    stage.advance(PetStage.HOP_MS);
    assert !stage.isAnimating() && stage.ambientTilt() > 0 : "점프가 끝나면 평상시 움직임으로 돌아간다";
    stage.shake();
    assert stage.ambientTilt() == 0;
    stage.advance(PetStage.SHAKE_MS);
    PetStage feedbackStage = new PetStage();
    feedbackStage.advanceAmbient(800);
    assert feedbackStage.ambientTilt() == 0;
    try {
      var field = PetStage.class.getDeclaredField("ambientTimer");
      field.setAccessible(true);
      javax.swing.Timer timer = (javax.swing.Timer) field.get(stage);
      assert !timer.isRunning();
      javax.swing.JPanel cards = new javax.swing.JPanel(new java.awt.CardLayout());
      cards.add(stage, "pet");
      cards.add(new javax.swing.JPanel(), "other");
      cards.addNotify();
      try {
        assert timer.isRunning();
        stage.celebrate(List.of());
        ((java.awt.CardLayout) cards.getLayout()).show(cards, "other");
        assert !timer.isRunning() && !stage.isAnimating() : "숨겨지면 모든 타이머를 멈춘다";
        assert stage.ambientTilt() == 0;
        ((java.awt.CardLayout) cards.getLayout()).show(cards, "pet");
        assert timer.isRunning();
        for (var action : timer.getActionListeners()) action.actionPerformed(null);
        stage.advanceAmbient(800);
        assert stage.ambientTilt() > 0;
        stage.setAmbientAnimation(false);
        assert !timer.isRunning() && stage.ambientTilt() == 0;
        stage.setAmbientAnimation(true);
        assert timer.isRunning();
      } finally {
        cards.removeNotify();
      }
      assert !timer.isRunning() && !stage.isAnimating();
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static void petStageAnimations() {
    PetStage stage = new PetStage();
    stage.setSize(280, 270);
    stage.showPet("DUCK", PetMood.HAPPY, "정답!");
    List<PetStage.Effect> effects = List.of(
        new PetStage.Effect("+10 EXP", UiStyle.EXP_DARK), new PetStage.Effect("+1 INT", UiStyle.statColor("INT")));
    stage.celebrate(effects);
    assert stage.isCelebrating() && !stage.isShaking();
    assert stage.getEffects().equals(effects);
    // 문구 두 개가 차례로 떠오르므로 뛰는 시간보다 길게 재생된다.
    assert stage.getDurationMs() == PetStage.FLOAT_GAP_MS + PetStage.FLOAT_MS : stage.getDurationMs();

    stage.advance(PetStage.HOP_MS / 4);
    assert stage.hopOffset(200) > 0 : "뛰는 중에는 위로 올라가야 한다";
    paint(stage);
    stage.advance(PetStage.HOP_MS);
    assert stage.hopOffset(200) == 0 : "뛰기가 끝나면 바닥에 있어야 한다";
    assert stage.isAnimating() : "문구는 아직 떠 있어야 한다";
    paint(stage);
    stage.advance(stage.getDurationMs());
    assert !stage.isAnimating() && stage.getEffects().isEmpty();
    assert stage.getMood() == PetMood.HAPPY : "애니메이션이 끝나도 표정은 유지된다";

    stage.shake();
    assert stage.isShaking() && stage.getDurationMs() == PetStage.SHAKE_MS;
    stage.advance(PetStage.SHAKE_MS / 12);
    assert stage.shakeOffset(200) != 0;
    paint(stage);
    stage.advance(PetStage.SHAKE_MS);
    assert !stage.isAnimating() && stage.shakeOffset(200) == 0;

    // 화면에서 빠지면 타이머가 멈춘다.
    stage.celebrate(List.of());
    stage.removeNotify();
    assert !stage.isAnimating();
  }

  private static void feedbackReactions() {
    AnswerFeedbackPanel feedback = new AnswerFeedbackPanel();
    feedback.showFeedback(view(true, 10, true));
    assert feedback.getPetStage().isCelebrating();
    assert texts(feedback.getPetStage().getEffects()).equals(List.of("+10 EXP", "+1 INT"))
        : texts(feedback.getPetStage().getEffects());

    // 이미 맞힌 문제는 기뻐하기만 하고 획득 문구는 없다.
    feedback.showFeedback(view(true, 0, true));
    assert feedback.getPetStage().isCelebrating() && feedback.getPetStage().getEffects().isEmpty();

    // 저장 전(예정) 보상은 획득한 것처럼 띄우지 않는다.
    assert AnswerFeedbackPanel.petEffects(view(true, 10, false)).isEmpty();

    // 오답이면 흔들리기만 한다.
    feedback.showFeedback(view(false, 0, true));
    assert feedback.getPetStage().isShaking() && feedback.getPetStage().getEffects().isEmpty();
    feedback.getPetStage().stopAnimation();

    // 캐릭터 상태를 받기 전에는 성장 현황 카드를 숨긴다.
    assert !feedback.isGrowthCardVisible();
    // 보상 반영 후 Lv.2, EXP 30/50, INT 15 → 경험치 20→30, INT 14→15가 강조된다.
    feedback.setPetStatus(2, 30, 50, stats(10, 15, 10, 10));
    feedback.showFeedback(view(true, 10, true));
    assert feedback.isGrowthCardVisible();
    assert feedback.getGrowthExpBar().getGain() == 10 && feedback.getGrowthExpBar().getValue() == 30;
    assert feedback.getGrowthStatBar().getGain() == 1 && feedback.getGrowthStatBar().getValue() == 15;
    assert feedback.getGrowthStatText().equals("15 (+1)") : feedback.getGrowthStatText();
    assert feedback.getGrowthLevelNote().isEmpty();
    // 네 능력치를 모두 보여 주며, 다른 능력치는 현재 값만 표시하고 가장 낮은 능력치를 강조한다.
    for (String type : UiStyle.STAT_TYPES) {
      assert feedback.getGrowthStatBar(type) != null && feedback.getGrowthStatBar(type).getMaximum() == 30;
    }
    assert feedback.getGrowthStatText("STR").equals("10") && !feedback.getGrowthStatBar("STR").hasGain();
    assert feedback.isGrowthStatMarkedWeak("STR") && !feedback.isGrowthStatMarkedWeak("INT");

    // 경험치가 레벨 경계를 넘으면 0부터 채우고 레벨 업을 알린다.
    feedback.setPetStatus(3, 5, 50, stats(10, 16, 10, 10));
    feedback.showFeedback(view(true, 10, true));
    assert feedback.getGrowthExpBar().getGain() == 5 && feedback.getGrowthLevelNote().equals("레벨 업!");

    // 오답이면 현재 값만 보여 주고 증가분은 없다.
    feedback.setPetStatus(3, 5, 50, stats(10, 16, 10, 10));
    feedback.showFeedback(view(false, 0, true));
    assert !feedback.getGrowthExpBar().hasGain() && !feedback.getGrowthStatBar().hasGain();
    assert feedback.getGrowthStatText().equals("16");
    feedback.getPetStage().stopAnimation();

    // 펫 열 폭은 화면 폭에 맞춰 260~360px 사이에서 변한다.
    assert AnswerFeedbackPanel.petColumnWidth(1024) == 260;
    assert AnswerFeedbackPanel.petColumnWidth(1600) == 320;
    assert AnswerFeedbackPanel.petColumnWidth(2560) == 360;
  }

  private static void gaugeGain() {
    GaugeBar bar = new GaugeBar(UiStyle.statColor("INT"), 10, false);
    bar.setSize(200, 10);
    bar.setValue(11, 30, 10, true);
    assert bar.hasGain() && bar.getGain() == 1;
    assert bar.getValue() == 11 && Math.abs(bar.getRatio() - 11 / 30.0) < 1e-9;
    assert bar.isAnimating();
    assert Math.abs(bar.getShownRatio() - 10 / 30.0) < 1e-9 : "성장 전 값에서 시작해야 한다";
    bar.advance(GaugeBar.FILL_DURATION_MS / 2);
    assert bar.getShownRatio() > 10 / 30.0 && bar.getShownRatio() < 11 / 30.0;
    paint(bar);
    bar.advance(GaugeBar.FILL_DURATION_MS);
    assert !bar.isAnimating() && Math.abs(bar.getShownRatio() - 11 / 30.0) < 1e-9;
    assert bar.getToolTipText().equals("11 / 30 (+1)") : bar.getToolTipText();

    // 줄었거나 같으면 증가분을 표시하지 않고, 기본 setValue는 바로 그린다.
    bar.setValue(10, 30, 12, false);
    assert !bar.hasGain() && bar.getGain() == 0;
    bar.setValue(5, 30);
    assert !bar.isAnimating() && !bar.hasGain() && Math.abs(bar.getShownRatio() - 5 / 30.0) < 1e-9;
  }

  private static void weakestStats() {
    // 모두 같으면 강조하지 않는다(새 캐릭터 10/10/10/10).
    assert StatComparisonPanel.weakestStats(stats(10, 10, 10, 10)).isEmpty();
    assert StatComparisonPanel.weakestStats(stats(12, 18, 14, 13)).equals(List.of("STR"));
    // 동점이면 표시 순서(INT, STR, FOC, WIS)대로 모두 강조한다.
    assert StatComparisonPanel.weakestStats(stats(10, 12, 13, 10)).equals(List.of("STR", "WIS"));

    assert StatComparisonPanel.weaknessHint(List.of("STR"))
        .equals("체력(STR)이 가장 낮아요. 스포츠 퀴즈로 키워 보세요.")
        : StatComparisonPanel.weaknessHint(List.of("STR"));
    assert StatComparisonPanel.weaknessHint(List.of("WIS")).startsWith("지혜(WIS)가 가장 낮아요.");
    // INT는 과학과 상식 두 분야에서 오른다.
    assert StatComparisonPanel.weaknessHint(List.of("INT")).contains("과학·상식 퀴즈");
    assert PetStatusPanel.shortWeaknessHint(List.of("STR")).equals("체력이 가장 낮아요 · 스포츠 퀴즈 추천");
  }

  private static void sidebarGrowth() {
    PetStatusPanel panel = new PetStatusPanel();
    panel.showPet("말랑이", "DUCK", 2, 20, 50, 12, 18, 14, 13);
    assert panel.isStatMarkedWeak("STR") && !panel.isStatMarkedWeak("INT");
    assert panel.getWeakHintText().equals("체력이 가장 낮아요 · 스포츠 퀴즈 추천") : panel.getWeakHintText();
    assert panel.getStatDeltaText("INT").isEmpty();

    // 퀴즈에서 INT가 16 → 18, 경험치 20을 얻었다(레벨 유지).
    panel.showGrowth(stats(12, 16, 14, 13), 20, false);
    assert panel.getStatDeltaText("INT").equals("+2") : panel.getStatDeltaText("INT");
    assert panel.getStatDeltaText("STR").isEmpty();
    assert panel.getStatBar("INT").hasGain() && panel.getStatBar("INT").getGain() == 2;
    assert panel.getExpBar().hasGain() && panel.getExpBar().getGain() == 20 : "EXP 0 → 20";

    // 레벨이 오르면 경험치 막대는 0부터 채운다.
    panel.showPet("말랑이", "DUCK", 3, 10, 50, 12, 19, 14, 13);
    assert panel.getStatDeltaText("INT").isEmpty() : "다시 표시하면 증가량을 지운다";
    panel.showGrowth(stats(12, 18, 14, 13), 20, true);
    assert panel.getExpBar().getGain() == 10;

    // 새 캐릭터는 약점 안내가 없다.
    panel.showPet("새싹", "DUCK", 1, 0, 50, 10, 10, 10, 10);
    assert !panel.isStatMarkedWeak("STR") && panel.getWeakHintText().isEmpty();

    HomePanel home = new HomePanel(new ScreenManager());
    home.showPet("말랑이", "DUCK", 3, 10, 12, 19, 14, 13);
    home.showGrowth(stats(12, 18, 14, 13), 20, true);
    assert home.getStatusPanel().getStage().getMood() == PetMood.LEVEL_UP;
    assert home.getStatusPanel().getStage().isCelebrating();
    home.showPet("말랑이", "DUCK", 3, 30, 12, 20, 14, 13);
    home.showGrowth(stats(12, 19, 14, 13), 10, false);
    assert home.getStatusPanel().getStage().getMood() == PetMood.HAPPY;
    home.getStatusPanel().getStage().stopAnimation();
  }

  private static void resultComparison() {
    QuizResultPanel result = new QuizResultPanel();
    StatComparisonPanel comparison = result.getStatComparison();
    assert !comparison.isVisible() : "비교 정보를 받기 전에는 숨긴다";
    result.showStatComparison(stats(12, 16, 14, 13), stats(12, 18, 14, 13));
    assert comparison.isVisible();
    assert comparison.getChangeText("INT").equals("16 → 18  (+2)") : comparison.getChangeText("INT");
    assert comparison.getChangeText("STR").equals("12");
    assert comparison.getBar("INT").hasGain() && !comparison.getBar("STR").hasGain();
    assert comparison.isMarkedWeak("STR") && !comparison.isMarkedWeak("INT");
    assert comparison.getHintText().startsWith("체력(STR)이 가장 낮아요.");
    comparison.getBar("INT").advance(GaugeBar.FILL_DURATION_MS);
    result.setSize(900, 900);
    result.doLayout();
    paint(result);

    try {
      comparison.showComparison(Map.of("STR", 1), stats(1, 1, 1, 1), false);
      assert false : "능력치가 빠지면 거부해야 한다";
    } catch (IllegalArgumentException expected) {
      // 정상
    }
    result.hideStatComparison();
    assert !comparison.isVisible();
  }

  /** 키 순서: STR, INT, FOC, WIS (Pet 생성자와 같음) */
  private static Map<String, Integer> stats(int str, int intel, int foc, int wis) {
    return Map.of("STR", str, "INT", intel, "FOC", foc, "WIS", wis);
  }

  private static AnswerFeedbackView view(boolean correct, int xp, boolean confirmed) {
    return new AnswerFeedbackView(correct, "O", "X", "해설", "INT", xp > 0 ? 1 : 0, xp, true, false, confirmed);
  }

  private static List<String> texts(List<PetStage.Effect> effects) {
    return effects.stream().map(PetStage.Effect::text).toList();
  }

  /** headless 환경에서 애니메이션 중간 프레임을 그려도 예외가 없어야 합니다. */
  private static void paint(JComponent component) {
    int w = Math.max(1, component.getWidth());
    int h = Math.max(1, component.getHeight());
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    component.paint(g);
    g.dispose();
  }
}
