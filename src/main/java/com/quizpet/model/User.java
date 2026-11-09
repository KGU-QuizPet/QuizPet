package main.java.com.quizpet.model;

import java.time.LocalDateTime;

/** 사용자 프로필: 사용자 ID, 닉네임, 계정 생성 일시 */
public class User {
    private final String userId;
    private final String nickname;
    private final LocalDateTime createdAt;

    public User(String userId, String nickname, LocalDateTime createdAt) {
        this.userId = userId;
        this.nickname = nickname;
        this.createdAt = createdAt;
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
        return "%s(%s)".formatted(nickname, userId);
    }
}
