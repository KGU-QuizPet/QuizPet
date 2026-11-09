package com.quizpet.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.quizpet.exception.ValidationException;
import com.quizpet.model.User;
import com.quizpet.repository.UserRepository;

/** 사용자 프로필 생성(FR-001)·선택(FR-002) */
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 새 사용자 생성. 닉네임은 1~12자, 중복 불가.
     * synchronized: 버튼을 빠르게 두 번 눌러도 프로필이 두 개 생기지 않게 한다.
     */
    public synchronized User createUser(String nickname) {
        String name = NameValidator.normalize(nickname, "닉네임");
        if (userRepository.existsByNickname(name))
            throw new ValidationException("이미 사용 중인 닉네임입니다.");
        User user = new User(userRepository.nextId(), name,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        userRepository.insert(user);
        return user;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** 저장소에 없는 userId면 진입을 막는다. */
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("존재하지 않는 사용자입니다."));
    }
}
