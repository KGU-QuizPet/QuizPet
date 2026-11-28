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

  /** int 최대값을 넘으면 값을 바꾸지 않고 IllegalArgumentException을 던집니다. */
  void increase(String statType, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("능력치 증가량은 음수가 될 수 없습니다.");
    }
    String type = requireStatType(statType);
    int increased;
    try {
      increased = Math.addExact(get(type), amount);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("능력치가 최대값을 넘어 증가시킬 수 없습니다: " + type, e);
    }
    switch (type) {
      case "STR":
        strength = increased;
        break;
      case "INT":
        intelligence = increased;
        break;
      case "FOC":
        focus = increased;
        break;
      default:
        wisdom = increased;
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
