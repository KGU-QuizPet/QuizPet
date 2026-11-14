package main.java.com.quizpet.model;

import java.time.LocalDateTime;

public final class User {
  public static final int MAX_NAME_LENGTH = 12;

  private final String userId;
  private final String nickname;
  private final LocalDateTime createdAt;

  public User(String userId, String nickname, LocalDateTime createdAt) {
    requireText(userId, "userId");
    validateName(nickname, "닉네임");
    if (createdAt == null) {
      throw new IllegalArgumentException("createdAt는 필수입니다.");
    }
    this.userId = userId;
    this.nickname = nickname;
    this.createdAt = createdAt;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
    }
  }

  /** 앞뒤 공백 없이 1~12자이며, 저장 구분자 '|'와 '_', 줄바꿈은 허용하지 않습니다. */
  static void validateName(String name, String field) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException(field + "을(를) 입력해 주세요.");
    }
    if (!name.equals(name.strip())) {
      throw new IllegalArgumentException(field + "의 앞뒤에 공백을 넣을 수 없습니다.");
    }
    if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(field + "은(는) " + MAX_NAME_LENGTH + "자 이하로 입력해 주세요.");
    }
    if (name.contains("|") || name.contains("_") || name.contains("\n") || name.contains("\r")) {
      throw new IllegalArgumentException(field + "에 '|', '_' 문자나 줄바꿈은 사용할 수 없습니다.");
    }
  }

  public String getUserId() {
    return userId;
  }

  public String getNickname() {
    return nickname;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public String toString() {
    return nickname + "(" + userId + ")";
  }
}
