package main.java.com.quizpet.service;

import java.util.ArrayList;
import java.util.List;

import main.java.com.quizpet.model.Profile;
import main.java.com.quizpet.model.User;
import main.java.com.quizpet.repository.PetRepository;
import main.java.com.quizpet.repository.UserRepository;

/** 시작 화면(UI-01)의 "기존 사용자 선택" 목록 */
public class ProfileService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;

    public ProfileService(UserRepository userRepository, PetRepository petRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    public List<Profile> getProfiles() {
        List<Profile> profiles = new ArrayList<>();
        for (User u : userRepository.findAll())
            profiles.add(new Profile(u, petRepository.findByUserId(u.getUserId()).orElse(null)));
        return profiles;
    }
}
