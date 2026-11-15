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
