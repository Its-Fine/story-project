package com.example.story.utilities;

import com.example.story.entity.UserEntity;
import com.example.story.repository.UserRepository;

import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.Period;

public class Helpers {

    public static UserEntity getCurrentUser(UserRepository repository) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return repository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public static boolean validateAge(LocalDate birthDate, int start_age, int end_age) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        return age >= start_age && age <= end_age;
    }
}
