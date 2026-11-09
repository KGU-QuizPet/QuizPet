package com.quizpet.model;

/**
 * 캐릭터: 이름, 외형, 누적 경험치, 능력치.
 * 레벨은 저장하지 않고 누적 경험치로 계산한다(level = 1 + totalXp / 50).
 */
public class Pet {
    public static final int XP_PER_LEVEL = 50;

    private final String petId;
    private final String userId;
    private final String name;
    private final PetAppearance appearance;
    private int totalXp;
    private final PetStats stats;

    /** 신규 캐릭터: Lv.1, XP 0, 능력치 각 10 */
    public Pet(String petId, String userId, String name, PetAppearance appearance) {
        this(petId, userId, name, appearance, 0, new PetStats());
    }

    /** 파일에서 불러올 때 사용 */
    public Pet(String petId, String userId, String name, PetAppearance appearance, int totalXp, PetStats stats) {
        if (totalXp < 0)
            throw new IllegalArgumentException("경험치는 음수가 될 수 없습니다.");
        this.petId = petId;
        this.userId = userId;
        this.name = name;
        this.appearance = appearance;
        this.totalXp = totalXp;
        this.stats = stats;
    }

    /** 경험치와 능력치를 함께 올리고 변화량을 돌려준다. */
    public GrowthResult applyReward(int xp, StatType statType, int statAmount) {
        if (xp < 0 || statAmount < 0)
            throw new IllegalArgumentException("보상은 음수가 될 수 없습니다.");
        int levelBefore = getLevel();
        int statBefore = stats.get(statType);
        totalXp += xp;
        stats.increase(statType, statAmount);
        return new GrowthResult(xp, statType, statAmount, statBefore, stats.get(statType),
                levelBefore, getLevel(), totalXp);
    }

    public int getLevel() {
        return 1 + totalXp / XP_PER_LEVEL;
    }

    /** 현재 레벨 안에서의 진행 경험치 (경험치 바 표시용, 예: 20/50) */
    public int getXpInLevel() {
        return totalXp % XP_PER_LEVEL;
    }

    public String getPetId() {
        return petId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public PetAppearance getAppearance() {
        return appearance;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public PetStats getStats() {
        return stats;
    }

    @Override
    public String toString() {
        return "%s [%s] Lv.%d (EXP %d/%d) %s".formatted(
                name, appearance.getDisplayName(), getLevel(), getXpInLevel(), XP_PER_LEVEL, stats);
    }
}
