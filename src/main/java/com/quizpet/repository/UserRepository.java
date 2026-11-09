package main.java.com.quizpet.repository;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import main.java.com.quizpet.exception.DataFileException;
import main.java.com.quizpet.model.User;

/**
 * data/save/user.txt 저장소. 한 줄 = 사용자 1명
 * 형식: userId|nickname|createdAt   예) u0001|상훈|2026-11-09T14:30:05
 */
public class UserRepository {
    private static final String HEADER = "userId|nickname|createdAt";

    private final TextFileStore store;

    /** 기본 위치 data/save/user.txt 사용 */
    public UserRepository() {
        this(TextFileStore.DEFAULT_SAVE_DIR);
    }

    public UserRepository(Path saveDir) {
        this.store = new TextFileStore(saveDir.resolve("user.txt"), HEADER);
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        for (String[] row : store.readRows())
            users.add(parse(row));
        return users;
    }

    public Optional<User> findById(String userId) {
        for (User u : findAll())
            if (u.getUserId().equals(userId))
                return Optional.of(u);
        return Optional.empty();
    }

    public boolean existsByNickname(String nickname) {
        for (User u : findAll())
            if (u.getNickname().equals(nickname))
                return true;
        return false;
    }

    public void insert(User user) {
        List<String> lines = new ArrayList<>();
        for (User u : findAll())
            lines.add(format(u));
        lines.add(format(user));
        store.writeRows(lines);
    }

    /** u0001, u0002 ... 순서로 다음 ID */
    public String nextId() {
        int max = 0;
        for (User u : findAll())
            max = Math.max(max, Integer.parseInt(u.getUserId().substring(1)));
        return "u%04d".formatted(max + 1);
    }

    private String format(User u) {
        return String.join(TextFileStore.SEPARATOR, u.getUserId(), u.getNickname(), u.getCreatedAt().toString());
    }

    private User parse(String[] row) {
        try {
            if (row.length != 3)
                throw new IllegalArgumentException("칸 수가 3이 아님");
            return new User(row[0], row[1], LocalDateTime.parse(row[2]));
        } catch (RuntimeException e) {
            throw new DataFileException("사용자 파일이 손상되었습니다: " + String.join("|", row), e);
        }
    }
}
