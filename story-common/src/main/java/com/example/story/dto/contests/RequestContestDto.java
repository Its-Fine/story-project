package com.example.story.dto.contests;

import com.example.story.dto.Group;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RequestContestDto {

    @NotNull(groups = Group.Create.class, message = "The 'name' field cannot be empty")
    @Pattern(regexp = "^(?!\\s*$).+", groups = {Group.Create.class, Group.Update.class}, message = "The 'name' field cannot be empty")
    private String name;

    @NotNull(groups = Group.Create.class, message = "The 'description' field cannot be empty")
    @Pattern(regexp = "^(?!\\s*$).+", groups = {Group.Create.class, Group.Update.class}, message = "The 'description' field cannot be empty")
    private String description;

    @NotNull(groups = Group.Create.class, message = "The 'startAge' field cannot be empty")
    @Min(value = 0, groups = {Group.Create.class, Group.Update.class}, message = "Start age cannot be less than zero")
    private Integer startAge;

    @NotNull(groups = Group.Create.class, message = "The 'endAge' field cannot be empty")
    private Integer endAge;

    @AssertTrue(groups = {Group.Create.class, Group.Update.class}, message = "End age cannot be less than start age")
    private boolean isParamsCorrect() {
        return endAge >= startAge;
    }
}
