package com.example.story.dto.applications;

import com.example.story.dto.Group;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestApplicationMemberDto {

    @Pattern(regexp = "^(?!\\s*$).+", message = "The 'firstName' field cannot be empty")
    private String firstName;

    @Pattern(regexp = "^(?!\\s*$).+", message = "The 'lastName' field cannot be empty")
    private String lastName;

    @NotNull(groups = Group.Create.class, message = "The 'birthDate' field cannot be empty")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
