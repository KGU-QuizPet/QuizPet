package com.quizpet.model;

/** 보상 1회 적용 결과 (UI-06 해설 화면, UI-07 결과 화면 표시용) */
public class GrowthResult {
    private final int xpGained;
    private final StatType statType;
    private final int statGained;
    private final int statBefore;
    private final int statAfter;
    private final int levelBefore;
    private final int levelAfter;
    private final int totalXpAfter;

    public GrowthResult(int xpGained, StatType statType, int statGained, int statBefore, int statAfter,
            int levelBefore, int levelAfter, int totalXpAfter) {
        this.xpGained = xpGained;
        this.statType = statType;
        this.statGained = statGained;
        this.statBefore = statBefore;
        this.statAfter = statAfter;
        this.levelBefore = levelBefore;
        this.levelAfter = levelAfter;
        this.totalXpAfter = totalXpAfter;
    }

    public boolean isLevelUp() {
        return levelAfter > levelBefore;
    }

    public int getXpGained() {
        return xpGained;
    }

    public StatType getStatType() {
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

    public int getTotalXpAfter() {
        return totalXpAfter;
    }

    @Override
    public String toString() {
        return "EXP +%d, %s +%d, Lv.%d -> Lv.%d".formatted(
                xpGained, statType, statGained, levelBefore, levelAfter);
    }
}
