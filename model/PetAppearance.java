package com.quizpet.model;

/**
 * 캐릭터 생성 시 고를 수 있는 외형 (UI-02 외형 선택 3종).
 * 파일에는 name()(예: DUCK)이 spriteKey로 저장된다.
 */
public enum PetAppearance {
    DUCK("고라파카", "images/pet_duck.png"),
    TURTLE("고북이", "images/pet_turtle.png"),
    LIZARD("파이리", "images/pet_lizard.png");

    private final String displayName;
    private final String imagePath;

    PetAppearance(String displayName, String imagePath) {
        this.displayName = displayName;
        this.imagePath = imagePath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
