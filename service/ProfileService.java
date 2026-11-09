package com.quizpet.service;

import java.util.ArrayList;
import java.util.List;

import com.quizpet.model.Profile;
import com.quizpet.model.User;
import com.quizpet.repository.PetRepository;
import com.quizpet.repository.UserRepository;

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
