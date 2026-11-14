package main.java.com.quizpet.model;

/** 캐릭터입니다. 레벨은 저장하지 않고 누적 경험치로 계산합니다(level = 1 + totalXp / 50). */
public final class Pet {
  public static final int XP_PER_LEVEL = 50;

  private final String petId;
  private final String userId;
  private final String name;
  private final PetAppearance appearance;
  private final PetStats stats;
  private int totalXp;

  /** 신규 캐릭터: Lv.1, XP 0, 능력치 각 10 */
  public Pet(String petId, String userId, String name, PetAppearance appearance) {
    this(petId, userId, name, appearance, 0, new PetStats());
  }

  public Pet(String petId, String userId, String name, PetAppearance appearance,
      int totalXp, PetStats stats) {
    requireText(petId, "petId");
    requireText(userId, "userId");
    User.validateName(name, "캐릭터 이름");
    if (appearance == null) {
      throw new IllegalArgumentException("외형을 선택해 주세요.");
    }
    if (totalXp < 0) {
      throw new IllegalArgumentException("totalXp는 음수가 될 수 없습니다.");
    }
    if (stats == null) {
      throw new IllegalArgumentException("stats는 필수입니다.");
    }
    this.petId = petId;
    this.userId = userId;
    this.name = name;
    this.appearance = appearance;
    this.totalXp = totalXp;
    this.stats = stats;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
    }
  }

  /** 경험치와 statType 능력치를 함께 올리고 변화량을 반환합니다. */
  public GrowthResult applyReward(int xp, String statType, int statAmount) {
    if (xp < 0 || statAmount < 0) {
      throw new IllegalArgumentException("보상은 음수가 될 수 없습니다.");
    }
    int levelBefore = getLevel();
    int statBefore = stats.get(statType);
    stats.increase(statType, statAmount);
    totalXp += xp;
    return new GrowthResult(xp, statType, statAmount, statBefore, stats.get(statType),
        levelBefore, getLevel(), totalXp);
  }

  public int getLevel() {
    return 1 + totalXp / XP_PER_LEVEL;
  }

  /** 현재 레벨 안에서의 진행 경험치입니다(경험치 바 표시용, 예: 20/50). */
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

  public String getSpriteKey() {
    return appearance.getSpriteKey();
  }

  public int getTotalXp() {
    return totalXp;
  }

  public PetStats getStats() {
    return stats;
  }

  @Override
  public String toString() {
    return name + " [" + appearance.getDisplayName() + "] Lv." + getLevel()
        + " (EXP " + getXpInLevel() + "/" + XP_PER_LEVEL + ") " + stats;
  }
}
