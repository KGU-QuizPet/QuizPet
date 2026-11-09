package com.quizpet.model;

/**
 * 시작 화면(UI-01) 기존 사용자 목록의 한 줄: 사용자 + 캐릭터(없으면 null).
 * JList에 그대로 넣으면 "닉네임 (캐릭터 이름) Lv 4" 형태로 보인다.
 */
public class Profile {
    private final User user;
    private final Pet pet;

    public Profile(User user, Pet pet) {
        this.user = user;
        this.pet = pet;
    }

    public User getUser() {
        return user;
    }

    public Pet getPet() {
        return pet;
    }

    public boolean hasPet() {
        return pet != null;
    }

    @Override
    public String toString() {
        if (pet == null)
            return "%s (캐릭터 없음)".formatted(user.getNickname());
        return "%s (%s)  Lv %d".formatted(user.getNickname(), pet.getName(), pet.getLevel());
    }
}
