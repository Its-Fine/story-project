package com.example.story.dto.users;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestUserDataDto {

    @Pattern(regexp = "^(?!\\s*$).+", message = "The 'firstName' field cannot be empty")
    private String firstName;

    @Pattern(regexp = "^(?!\\s*$).+", message = "The 'lastName' field cannot be empty")
    private String lastName;
}
