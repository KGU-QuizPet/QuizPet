package main.java.com.quizpet.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import main.java.com.quizpet.model.User;

/** data/save/user.txt를 한 줄에 userId|nickname|createdAt 형식으로 읽고 씁니다. */
public class UserRepository {
  private final Path path;

  public UserRepository() {
    this(Path.of("data/save/user.txt"));
  }

  public UserRepository(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("path는 필수입니다.");
    }
    this.path = path;
  }

  public List<User> findAll() throws IOException {
    List<User> loaded = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    List<String> lines = SaveFile.readLines(path);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue;
      }
      try {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 3) {
          throw new IllegalArgumentException("필드는 3개여야 합니다.");
        }
        User user = new User(fields[0], fields[1], LocalDateTime.parse(fields[2]));
        if (!ids.add(user.getUserId())) {
          throw new IllegalArgumentException("중복된 사용자 ID입니다.");
        }
        loaded.add(user);
      } catch (RuntimeException e) {
        // 손상된 줄을 건너뛰고 저장하면 데이터가 사라지므로 전체 로딩을 실패시킵니다.
        throw new IOException(path + ":" + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    return List.copyOf(loaded);
  }

  public Optional<User> findById(String userId) throws IOException {
    return findAll().stream()
        .filter(user -> user.getUserId().equals(userId))
        .findFirst();
  }

  public boolean existsByNickname(String nickname) throws IOException {
    return findAll().stream()
        .anyMatch(user -> user.getNickname().equals(nickname));
  }

  public void save(User user) throws IOException {
    if (user == null) {
      throw new IllegalArgumentException("user는 필수입니다.");
    }
    List<String> lines = new ArrayList<>();
    for (User saved : findAll()) {
      if (saved.getUserId().equals(user.getUserId())) {
        throw new IllegalArgumentException("이미 저장된 사용자 ID입니다: " + user.getUserId());
      }
      lines.add(format(saved));
    }
    lines.add(format(user));
    SaveFile.writeLines(path, lines);
  }

  /** U0001, U0002 순서로 다음 사용자 ID를 만듭니다. */
  public String nextId() throws IOException {
    int max = 0;
    for (User user : findAll()) {
      String id = user.getUserId();
      if (id.matches("U\\d+")) {
        max = Math.max(max, Integer.parseInt(id.substring(1)));
      }
    }
    return String.format("U%04d", max + 1);
  }

  private static String format(User user) {
    return String.join("|", user.getUserId(), user.getNickname(), user.getCreatedAt().toString());
  }
}
