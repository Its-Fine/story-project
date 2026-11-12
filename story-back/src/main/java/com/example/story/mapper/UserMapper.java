package com.example.story.mapper;

import com.example.story.dto.users.*;
import com.example.story.entity.UserEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "applicationsCount", ignore = true)
    @Mapping(target = "userId", source = "id")
    ResponseUserDataDto entityToUserDataDto(UserEntity entity);

    @Mapping(target = "userId", source = "id")
    ResponseUserDataIdDto entityToUserDataIdDto(UserEntity entity);

    @Mapping(target = "applicationsCount", ignore = true)
    @Mapping(target = "userId", source = "id")
    ResponseUserDataAdminDto entityToUserDataAdminDto(UserEntity entity);
}
