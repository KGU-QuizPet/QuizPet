package main.java.com.quizpet.model;

/** 보상 1회 적용 결과입니다(UI-06 해설, UI-07 결과 화면 표시용). */
public final class GrowthResult {
  private final int earnedXp;
  private final String statType;
  private final int statGained;
  private final int statBefore;
  private final int statAfter;
  private final int levelBefore;
  private final int levelAfter;
  private final int totalXp;

  GrowthResult(int earnedXp, String statType, int statGained, int statBefore, int statAfter,
      int levelBefore, int levelAfter, int totalXp) {
    this.earnedXp = earnedXp;
    this.statType = statType;
    this.statGained = statGained;
    this.statBefore = statBefore;
    this.statAfter = statAfter;
    this.levelBefore = levelBefore;
    this.levelAfter = levelAfter;
    this.totalXp = totalXp;
  }

  public boolean isLevelUp() {
    return levelAfter > levelBefore;
  }

  public int getEarnedXp() {
    return earnedXp;
  }

  public String getStatType() {
    return statType;
  }

  public int getStatGained() {
    return statGained;
  }

  public int getStatBefore() {
    return statBefore;
  }

  public int getStatAfter() {
    return statAfter;
  }

  public int getLevelBefore() {
    return levelBefore;
  }

  public int getLevelAfter() {
    return levelAfter;
  }

  public int getTotalXp() {
    return totalXp;
  }

  @Override
  public String toString() {
    return "EXP +" + earnedXp + ", " + statType + " +" + statGained
        + ", Lv." + levelBefore + " -> Lv." + levelAfter;
  }
}
