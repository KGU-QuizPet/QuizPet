package main.java.com.quizpet.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.repository.UserRepository;

/** 사용자 프로필 생성(FR-001)·선택(FR-002) */
public class UserService {
  // 화면마다 UserService를 새로 만들어도 닉네임 중복 확인-저장이 겹치지 않도록 static 잠금을 씁니다.
  private static final Object LOCK = new Object();

  private final UserRepository userRepository;

  public UserService() {
    this(new UserRepository());
  }

  public UserService(UserRepository userRepository) {
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository는 필수입니다.");
  }

  /**
   * 닉네임의 앞뒤 공백을 제거한 뒤 1~12자·중복 여부를 검사하고 저장합니다.
   * 버튼을 연달아 누르거나 서비스 객체가 여러 개여도 같은 닉네임 프로필이 두 개 생기지 않습니다.
   */
  public User createUser(String nickname) throws IOException {
    String trimmed = nickname == null ? null : nickname.strip();
    synchronized (LOCK) {
      if (trimmed != null && userRepository.existsByNickname(trimmed)) {
        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
      }
      User user = new User(userRepository.nextId(), trimmed,
          LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
      userRepository.save(user);
      return user;
    }
  }

  public List<User> getUsers() throws IOException {
    return userRepository.findAll();
  }

  /** 저장되지 않은 userId는 IllegalArgumentException으로 진입을 막습니다. */
  public User getUser(String userId) throws IOException {
    return userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
  }
}
