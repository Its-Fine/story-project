package com.example.story.configuration;

import com.example.story.entity.ContestEntity;
import com.example.story.entity.UserEntity;
import com.example.story.repository.ContestRepository;
import com.example.story.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@DependsOn("entityManagerFactory")
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ContestRepository contestRepository;
    private final StoryProperties storyProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfNotExists();
        fillContestsIfNotExist();
    }

    private void createAdminIfNotExists() {
        boolean adminExists = userRepository.findByEmail(storyProperties.getAdmin().getEmail()).isPresent();

        if (!adminExists) {
            System.out.println("Creating default admin user...");

            UserEntity admin = new UserEntity();
            admin.setEmail(storyProperties.getAdmin().getEmail());
            admin.setPassword(passwordEncoder.encode(storyProperties.getAdmin().getPassword()));
            admin.setRole(StoryProperties.Roles.ADMIN.toString());
            admin.setClosedAccess(true);
            admin.setBanned(false);
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setBirthDate(LocalDate.of(2001, 11, 4));
            admin.setConsent(true);
            admin.setParentConsent(true);

            userRepository.save(admin);
            System.out.println("Admin user created successfully with email: " + storyProperties.getAdmin().getEmail());
        }
        else {
            System.out.println("Admin user already exists, skipping creation");
        }
    }

    private void fillContestsIfNotExist() {
        List<TempContest> newContests = List.of(
                new TempContest("Дизайн костюма", "Коллекции авторской одежды эскизы, макеты и/или готовые изделия одежды, обуви, аксессуаров", 14, 100),
                new TempContest("Графический дизайн", "Плакат, календарь, открытка, упаковка и др.", 10, 100),
                new TempContest("Иллюстрация", "Авторская графика, выполненная различными художественными материалами", 10, 100),
                new TempContest("Арт-дизайн", "Проекты, соединяющие в себе принципы дизайн-проектирования и традиционного художественного творчества", 10, 100),
                new TempContest("Декоративно-прикладное искусство", "Сохранение народных традиций, творческое осмысление, поиск современных форм и их интерпретация", 35, 100),
                new TempContest("Туристический сувенир", "Современное прочтение традиционных ремесел и народных промыслов в сувенирной продукции", 10, 100),
                new TempContest("Диджитал-арт и цифровая живопись", "Художественно-графические произведения, выполненные с помощью компьютерных технологий и специализированного программного обеспечения", 18, 100),
                new TempContest("Промышленный дизайн", "Эскизы, макеты, действующие прототипы промышленных изделий: техника, оборудование, предметы интерьера и городской среды, игрушки, ювелирные изделия, сувенирная продукция и др.", 18, 100)
        );

        for (TempContest newContest : newContests) {
            Optional<ContestEntity> entity = contestRepository.findByName(newContest.name);
            if (entity.isEmpty()) {
                ContestEntity newEntity = new ContestEntity();
                newEntity.setName(newContest.name);
                newEntity.setDescription(newContest.description);
                newEntity.setStartAge(newContest.startAge);
                newEntity.setEndAge(newContest.endAge);
                contestRepository.save(newEntity);
            }
        }
    }

    private static class TempContest {
        public String name;
        public String description;
        public Integer startAge;
        public Integer endAge;

        public TempContest(String name, String description, Integer startAge, Integer endAge) {
            this.name = name;
            this.description = description;
            this.startAge = startAge;
            this.endAge = endAge;
        }
    }
}
