package com.example.story.dto.works;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestWorkRatingDto {

    @NotNull(message = "The 'rate' field cannot be empty")
    @Min(value = 0, message = "Rate must be between 0 and 100")
    @Max(value = 100, message = "Rate must be between 0 and 100")
    private Integer rate;
}
