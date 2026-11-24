package com.example.story.dto.applications;

import com.example.story.dto.Group;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestApplicationMemberDto {

    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "The 'firstName' field cannot be empty")
    private String firstName;

    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "The 'lastName' field cannot be empty")
    private String lastName;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "The 'birthDate' field cannot be empty")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
