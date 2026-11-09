package com.example.story.mapper;

import com.example.story.dto.contests.*;
import com.example.story.entity.ContestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationEntities", ignore = true)
    ContestEntity entityFromContestDto(RequestContestDto dto);

    @Mapping(target = "conId", source = "id")
    ResponseContestDto entityToContestDto(ContestEntity entity);

    @Mapping(target = "conId", source = "id")
    ResponseContestIdDto entityToContestIdDto(ContestEntity entity);
}
