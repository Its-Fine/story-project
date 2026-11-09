package com.example.story.dto.applications;

import com.example.story.dto.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RequestApplicationDto {

    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "The 'name' field cannot be empty")
    private String name;

    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "The 'description' field cannot be empty")
    private String description;

    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "The 'city' field cannot be empty")
    private String city;

    private String organization;

    private String mentorName;

    @Valid
    private List<RequestApplicationMemberDto> members;

    @NotNull(groups = {Group.Create.class}, message = "The 'isTeam' flag is null")
    private Boolean isTeam;

    @NotNull(groups = {Group.Create.class}, message = "The 'isCurator' flag is null")
    private Boolean isCurator;

    @AssertTrue(groups = {Group.Create.class}, message = "Team must have at least one new member")
    private boolean isParamsCorrect() {
        return isTeam == false || (members != null && !members.isEmpty());
    }
}
