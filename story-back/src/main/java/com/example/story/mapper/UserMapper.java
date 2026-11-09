package com.example.story.mapper;

import com.example.story.dto.auth.RequestAuthDto;
import com.example.story.dto.users.*;
import com.example.story.entity.UserEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "closedAccess", ignore = true)
    @Mapping(target = "banned", ignore = true)
    @Mapping(target = "applicationEntities", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserEntity entityFromAuthDto(RequestAuthDto dto);

    @Mapping(target = "applicationsCount", ignore = true)
    @Mapping(target = "userId", source = "id")
    ResponseUserDataDto entityToUserDataDto(UserEntity entity);

    @Mapping(target = "userId", source = "id")
    ResponseUserDataIdDto entityToUserDataIdDto(UserEntity entity);

    @Mapping(target = "applicationsCount", ignore = true)
    @Mapping(target = "userId", source = "id")
    ResponseUserDataAdminDto entityToUserDataAdminDto(UserEntity entity);
}
