package main.java.com.quizpet.model;

/**
 * 체력·지성·집중력·지혜 능력치.
 * 값 변경은 같은 패키지의 Pet을 통해서만 가능하다(캡슐화).
 */
public class PetStats {
    public static final int INITIAL_VALUE = 10;

    private int strength;
    private int intelligence;
    private int focus;
    private int wisdom;

    /** 신규 캐릭터용: 4종 모두 초기값 10 */
    public PetStats() {
        this(INITIAL_VALUE, INITIAL_VALUE, INITIAL_VALUE, INITIAL_VALUE);
    }

    /** 파일에서 불러올 때 사용 */
    public PetStats(int strength, int intelligence, int focus, int wisdom) {
        if (strength < 0 || intelligence < 0 || focus < 0 || wisdom < 0)
            throw new IllegalArgumentException("능력치는 음수가 될 수 없습니다.");
        this.strength = strength;
        this.intelligence = intelligence;
        this.focus = focus;
        this.wisdom = wisdom;
    }

    public int get(StatType type) {
        switch (type) {
            case STR: return strength;
            case INT: return intelligence;
            case FOC: return focus;
            case WIS: return wisdom;
            default: throw new IllegalArgumentException("없는 능력치: " + type);
        }
    }

    void increase(StatType type, int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("능력치 증가량은 음수가 될 수 없습니다.");
        switch (type) {
            case STR: strength += amount; break;
            case INT: intelligence += amount; break;
            case FOC: focus += amount; break;
            case WIS: wisdom += amount; break;
        }
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
        return "STR %d / INT %d / FOC %d / WIS %d".formatted(strength, intelligence, focus, wisdom);
    }
}
