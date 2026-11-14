package main.java.com.quizpet.model;

/**
 * 체력(STR)·지성(INT)·집중력(FOC)·지혜(WIS) 능력치입니다.
 * statType은 Category.getStatType()과 같은 문자열을 사용하며, 값 변경은 Pet을 통해서만 가능합니다.
 */
public final class PetStats {
  public static final int INITIAL_VALUE = 10;

  private int strength;
  private int intelligence;
  private int focus;
  private int wisdom;

  public PetStats() {
    this(INITIAL_VALUE, INITIAL_VALUE, INITIAL_VALUE, INITIAL_VALUE);
  }

  public PetStats(int strength, int intelligence, int focus, int wisdom) {
    if (strength < 0 || intelligence < 0 || focus < 0 || wisdom < 0) {
      throw new IllegalArgumentException("능력치는 음수가 될 수 없습니다.");
    }
    this.strength = strength;
    this.intelligence = intelligence;
    this.focus = focus;
    this.wisdom = wisdom;
  }

  public int get(String statType) {
    switch (requireStatType(statType)) {
      case "STR":
        return strength;
      case "INT":
        return intelligence;
      case "FOC":
        return focus;
      default:
        return wisdom;
    }
  }

  void increase(String statType, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("능력치 증가량은 음수가 될 수 없습니다.");
    }
    switch (requireStatType(statType)) {
      case "STR":
        strength += amount;
        break;
      case "INT":
        intelligence += amount;
        break;
      case "FOC":
        focus += amount;
        break;
      default:
        wisdom += amount;
    }
  }

  private static String requireStatType(String statType) {
    if (!"STR".equals(statType) && !"INT".equals(statType)
        && !"FOC".equals(statType) && !"WIS".equals(statType)) {
      throw new IllegalArgumentException("statType은 STR, INT, FOC, WIS 중 하나여야 합니다.");
    }
    return statType;
  }

  public int getStrength() {
    return strength;
  }

  public int getIntelligence() {
    return intelligence;
  }

  public int getFocus() {
    return focus;
  }

  public int getWisdom() {
    return wisdom;
  }

  @Override
  public String toString() {
    return "STR " + strength + " / INT " + intelligence + " / FOC " + focus + " / WIS " + wisdom;
  }
}
