package com.example.story.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestEventDto {

    @NotNull(message = "The 'closed' field cannot be empty")
    public Boolean close;
}
