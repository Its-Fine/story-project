package com.example.story.mapper;

import com.example.story.dto.works.*;
import com.example.story.entity.WorkEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkMapper {

    @Mapping(target = "workId", source = "id")
    ResponseWorkDto entityToWorkDto(WorkEntity entity);

    @Mapping(target = "workId", source = "id")
    ResponseWorkIdDto entityToWorkIdDto(WorkEntity entity);

    @Mapping(target = "workId", source = "id")
    ResponseWorkAdminDto entityToWorkAdminDto(WorkEntity entity);
}
