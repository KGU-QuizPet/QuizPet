package main.java.com.quizpet.model;

/** 시작 화면(UI-01) 기존 사용자 목록의 한 줄입니다. 캐릭터가 없으면 pet은 null입니다. */
public final class Profile {
  private final User user;
  private final Pet pet;

  public Profile(User user, Pet pet) {
    if (user == null) {
      throw new IllegalArgumentException("user는 필수입니다.");
    }
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

  /** JList에 그대로 넣으면 "닉네임 (캐릭터 이름)  Lv 4" 형태로 표시됩니다. */
  @Override
  public String toString() {
    if (pet == null) {
      return user.getNickname() + " (캐릭터 없음)";
    }
    return user.getNickname() + " (" + pet.getName() + ")  Lv " + pet.getLevel();
  }
}
