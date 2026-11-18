package com.example.story.service;

import com.example.story.entity.ApplicationEntity;
import com.example.story.entity.MemberEntity;
import com.example.story.entity.UserEntity;
import com.example.story.repository.ApplicationRepository;
import com.example.story.repository.MemberRepository;
import com.example.story.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private static final Integer[] AGE_GROUP_KEYS = {10, 14, 18, 35, 999};

    private static final Map<Integer, String> AGE_GROUP_NAMES = new LinkedHashMap<>() {{
        put(10, "Младшая группа (10-13 лет)");
        put(14, "Средняя группа (14-17 лет)");
        put(18, "Старшая возрастная группа (18-35 лет)");
        put(35, "Художники, дизайнеры, преподаватели");
        put(999, "Нет группы");
    }};

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;

    public byte[] generateApplicationsReport() {
        List<UserEntity> users = userRepository.findAll();
        Map<String, List<UserApplicationData>> byContest = new LinkedHashMap<>();
        for (UserEntity user : users) {
            List<ApplicationEntity> applications = applicationRepository.findByUserId(user.getId());
            if (applications.isEmpty()) continue;
            for (ApplicationEntity app : applications) {
                String contestName = app.getContestEntity() != null ? app.getContestEntity().getName() : "Без конкурса";
                byContest.computeIfAbsent(contestName, k -> new ArrayList<>()).add(new UserApplicationData(user, app));
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (Map.Entry<String, List<UserApplicationData>> entry : byContest.entrySet()) {
                String contestName = entry.getKey();
                List<UserApplicationData> dataList = entry.getValue();
                Map<String, List<UserApplicationData>> byAgeGroup = new LinkedHashMap<>();
                for (Integer key : AGE_GROUP_KEYS) {
                    byAgeGroup.put(AGE_GROUP_NAMES.get(key), new ArrayList<>());
                }
                for (UserApplicationData uad : dataList) {
                    byAgeGroup.get(ageGroupKey(uad.user.getBirthDate())).add(uad);
                }
                Sheet sheet = workbook.createSheet(safeSheetName(contestName));
                writeSheet(sheet, byAgeGroup);
            }
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private String ageGroupKey(LocalDate birthDate) {
        if (birthDate == null) return AGE_GROUP_NAMES.get(999);
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        int selected = 999;
        for (Integer key : AGE_GROUP_KEYS) {
            if (key != 999 && age >= key) selected = key;
        }
        return AGE_GROUP_NAMES.get(selected);
    }

    private String safeSheetName(String name) {
        if (name == null) return "Sheet";
        String safe = name.replaceAll("[\\\\\\/*:?\\[\\]]", "_");
        return safe.length() > 31 ? safe.substring(0, 31) : safe;
    }

    private void writeSheet(Sheet sheet, Map<String, List<UserApplicationData>> byAgeGroup) {
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setWrapText(true);

        CellStyle altStyle = sheet.getWorkbook().createCellStyle();
        altStyle.cloneStyleFrom(cellStyle);
        altStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowIdx = 0;
        String[] headers = {
                "Email", "Фамилия Имя", "Дата рождения",
                "ID заявки", "Название заявки", "Описание", "Город", "Организация", "Наставник",
                "Члены команды", "Возрастная группа"
        };
        Row headerRow = sheet.createRow(rowIdx++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int userCounter = 0;
        for (Map.Entry<String, List<UserApplicationData>> groupEntry : byAgeGroup.entrySet()) {
            Row groupRow = sheet.createRow(rowIdx++);
            Cell groupCell = groupRow.createCell(0);
            groupCell.setCellValue("Возрастная группа: " + groupEntry.getKey());
            groupCell.setCellStyle(headerStyle);

            for (UserApplicationData uad : groupEntry.getValue()) {
                Row row = sheet.createRow(rowIdx++);
                CellStyle rowStyle = (userCounter % 2 == 0) ? cellStyle : altStyle;

                row.createCell(0).setCellValue(uad.user.getEmail());
                row.getCell(0).setCellStyle(rowStyle);

                row.createCell(1).setCellValue(uad.user.getLastName() + " " + uad.user.getFirstName());
                row.getCell(1).setCellStyle(rowStyle);

                row.createCell(2).setCellValue(uad.user.getBirthDate() != null
                        ? uad.user.getBirthDate().toString() : "");
                row.getCell(2).setCellStyle(rowStyle);

                row.createCell(3).setCellValue(uad.app.getId() != null ? uad.app.getId().toString() : "");
                row.getCell(3).setCellStyle(rowStyle);

                row.createCell(4).setCellValue(uad.app.getName());
                row.getCell(4).setCellStyle(rowStyle);

                row.createCell(5).setCellValue(uad.app.getDescription());
                row.getCell(5).setCellStyle(rowStyle);

                row.createCell(6).setCellValue(uad.app.getCity());
                row.getCell(6).setCellStyle(rowStyle);

                row.createCell(7).setCellValue(uad.app.getOrganization());
                row.getCell(7).setCellStyle(rowStyle);

                row.createCell(8).setCellValue(uad.app.getMentor());
                row.getCell(8).setCellStyle(rowStyle);

                List<MemberEntity> members = memberRepository.findByApplicationId(uad.app.getId());
                String membersStr = members.stream()
                        .map(m -> m.getLastName() + " " + m.getFirstName() + " (" +
                                (m.getBirthDate() != null ? m.getBirthDate() : "") + ")")
                        .reduce((a, b) -> a + "\n" + b).orElse("");
                row.createCell(9).setCellValue(membersStr);
                row.getCell(9).setCellStyle(rowStyle);

                row.createCell(10).setCellValue(ageGroupKey(uad.user.getBirthDate()));
                row.getCell(10).setCellStyle(rowStyle);

                userCounter++;
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static class UserApplicationData {
        final UserEntity user;
        final ApplicationEntity app;

        UserApplicationData(UserEntity user, ApplicationEntity app) {
            this.user = user;
            this.app = app;
        }
    }
}
