package main.java.com.quizpet.model;

import java.time.LocalDateTime;

public class PetTest {
  public static void main(String[] args) {
    Pet pet = new Pet("P0001", "U0001", "고라고라니", PetAppearance.DUCK);
    assert pet.getLevel() == 1;
    assert pet.getTotalXp() == 0;
    assert pet.getXpInLevel() == 0;
    assert pet.getSpriteKey().equals("DUCK");
    for (String statType : new String[] {"STR", "INT", "FOC", "WIS"}) {
      assert pet.getStats().get(statType) == PetStats.INITIAL_VALUE;
    }

    // 과학&상식(INT) 정답은 INT만 올린다.
    GrowthResult result = pet.applyReward(10, Category.SCIENCE.getStatType(), 1);
    assert result.getEarnedXp() == 10;
    assert result.getStatType().equals("INT");
    assert result.getStatBefore() == 10 && result.getStatAfter() == 11;
    assert pet.getStats().getIntelligence() == 11;
    assert pet.getStats().getStrength() == 10;
    assert pet.getStats().getFocus() == 10;
    assert pet.getStats().getWisdom() == 10;
    assert !result.isLevelUp();

    // 누적 XP 40 -> 50이면 Lv.2, 진행 경험치 0/50
    pet.applyReward(10, "WIS", 1);
    pet.applyReward(10, "WIS", 1);
    result = pet.applyReward(10, "STR", 1);
    assert !result.isLevelUp();
    assert pet.getTotalXp() == 40;
    result = pet.applyReward(10, "FOC", 1);
    assert result.isLevelUp();
    assert result.getLevelBefore() == 1 && result.getLevelAfter() == 2;
    assert pet.getTotalXp() == 50;
    assert pet.getLevel() == 2;
    assert pet.getXpInLevel() == 0;

    expect(IllegalArgumentException.class, () -> pet.applyReward(-1, "INT", 1));
    expect(IllegalArgumentException.class, () -> pet.applyReward(10, "LUCK", 1));
    assert pet.getTotalXp() == 50 : "잘못된 statType이면 경험치도 그대로여야 합니다";

    rewardOverflow();
    expect(IllegalArgumentException.class, () -> new PetStats(-1, 10, 10, 10));
    expect(IllegalArgumentException.class,
        () -> new Pet("P1", "U1", "이름", PetAppearance.DUCK, -10, new PetStats()));
    expect(IllegalArgumentException.class, () -> new Pet("P1", "U1", "이름", null));
    expect(IllegalArgumentException.class, () -> new Pet("P1", "U1", " ", PetAppearance.DUCK));
    expect(IllegalArgumentException.class,
        () -> new Pet("P1", "U1", "가나다라마바사아자차카타파", PetAppearance.DUCK));

    LocalDateTime now = LocalDateTime.now();
    assert new User("U1", "가나다라마바사아자차카타", now).getNickname().length() == 12;
    expect(IllegalArgumentException.class, () -> new User("U1", "가나다라마바사아자차카타파", now));
    expect(IllegalArgumentException.class, () -> new User("U1", "a|b", now));
    expect(IllegalArgumentException.class, () -> new User("U1", "a_b", now));
    expect(IllegalArgumentException.class, () -> new User("U1", " 앞공백", now));
    expect(IllegalArgumentException.class, () -> new User("", "닉네임", now));
    expect(IllegalArgumentException.class, () -> new User("U1", "닉네임", null));

    Profile empty = new Profile(new User("U2", "민지", now), null);
    assert !empty.hasPet();
    assert empty.toString().equals("민지 (캐릭터 없음)");
    assert new Profile(new User("U1", "상훈", now), pet).toString().equals("상훈 (고라고라니)  Lv 2");
    System.out.println("Pet checks passed");
  }

  /** 이슈 #74: int 상한에서 경험치·능력치가 음수로 순환하지 않고, 실패하면 원래 값을 유지한다. */
  private static void rewardOverflow() {
    int max = Integer.MAX_VALUE;

    // 이슈 재현 조건: XP와 INT가 모두 최대값
    Pet both = new Pet("P", "U", "Pet", PetAppearance.DUCK, max, new PetStats(10, max, 10, 10));
    expect(IllegalArgumentException.class, () -> both.applyReward(10, "INT", 1));
    assertUnchanged(both, max, 10, max, 10, 10);

    // XP만 초과: 능력치도 오르지 않는다(부분 보상 없음).
    Pet xpOnly = new Pet("P", "U", "Pet", PetAppearance.DUCK, max - 5, new PetStats());
    expect(IllegalArgumentException.class, () -> xpOnly.applyReward(10, "STR", 1));
    assertUnchanged(xpOnly, max - 5, 10, 10, 10, 10);

    // 능력치만 초과: 경험치도 오르지 않는다.
    Pet statOnly = new Pet("P", "U", "Pet", PetAppearance.DUCK, 100, new PetStats(10, 10, max, 10));
    expect(IllegalArgumentException.class, () -> statOnly.applyReward(10, "FOC", 1));
    assertUnchanged(statOnly, 100, 10, 10, max, 10);
    // 다른 능력치 보상은 정상 적용된다.
    statOnly.applyReward(10, "WIS", 1);
    assertUnchanged(statOnly, 110, 10, 10, max, 11);

    // 정확히 최대값까지는 허용한다.
    Pet edge = new Pet("P", "U", "Pet", PetAppearance.DUCK, max - 10, new PetStats(max - 1, 10, 10, 10));
    GrowthResult result = edge.applyReward(10, "STR", 1);
    assert edge.getTotalXp() == max && edge.getStats().getStrength() == max;
    assert result.getStatBefore() == max - 1 && result.getStatAfter() == max;
    assert edge.getLevel() > 0 && edge.getXpInLevel() >= 0 : "레벨 계산도 음수가 되지 않아야 합니다";
    // 최대값에서 보상 0은 변화 없이 허용한다(이미 맞힌 문제의 무보상 기록).
    edge.applyReward(0, "STR", 0);
    assertUnchanged(edge, max, max, 10, 10, 10);
    // 최대값에서 1이라도 더하면 거부한다.
    expect(IllegalArgumentException.class, () -> edge.applyReward(1, "INT", 0));
    expect(IllegalArgumentException.class, () -> edge.applyReward(0, "STR", 1));
    assertUnchanged(edge, max, max, 10, 10, 10);
  }

  private static void assertUnchanged(Pet pet, int xp, int str, int intel, int foc, int wis) {
    PetStats s = pet.getStats();
    assert pet.getTotalXp() == xp : "XP=" + pet.getTotalXp();
    assert s.getStrength() == str && s.getIntelligence() == intel && s.getFocus() == foc
        && s.getWisdom() == wis : s;
  }

  private static void expect(Class<? extends RuntimeException> type, Runnable action) {
    try {
      action.run();
      throw new AssertionError("예외가 발생해야 합니다: " + type.getSimpleName());
    } catch (RuntimeException exception) {
      if (!type.isInstance(exception)) {
        throw exception;
      }
    }
  }
}
