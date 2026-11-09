package com.example.story.mapper;

import com.example.story.dto.applications.*;
import com.example.story.entity.ApplicationEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userEntity", ignore = true)
    @Mapping(target = "contestEntity", ignore = true)
    @Mapping(target = "workEntities", ignore = true)
    @Mapping(target = "memberEntities", ignore = true)
    @Mapping(target = "mentor", source = "mentorName")
    @Mapping(target = "createdAt", ignore = true)
    ApplicationEntity entityFromApplicationDto(RequestApplicationDto dto);

    @Mapping(target = "worksCount", ignore = true)
    @Mapping(target = "contestName", ignore = true)
    @Mapping(target = "mentorName", source = "mentor")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "appId", source = "id")
    ResponseApplicationDto entityToApplicationDto(ApplicationEntity entity);

    @Mapping(target = "appId", source = "id")
    ResponseApplicationIdDto entityToApplicationIdDto(ApplicationEntity entity);

    @Mapping(target = "worksCount", ignore = true)
    @Mapping(target = "contestName", ignore = true)
    @Mapping(target = "mentorName", source = "mentor")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "appId", source = "id")
    ResponseApplicationAdminDto entityToApplicationAdminDto(ApplicationEntity entity);
}
