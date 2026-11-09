package com.quizpet.model;

/**
 * 캐릭터 능력치 4종.
 * 분야 매핑(보완점 반영): 과학&상식 -> INT, 스포츠 -> STR, 언어(영어) -> FOC, 역사 -> WIS
 */
public enum StatType {
    STR("체력"),
    INT("지성"),
    FOC("집중력"),
    WIS("지혜");

    private final String koreanName;

    StatType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}
