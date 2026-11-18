package com.example.story.mapper;

import com.example.story.dto.applications.*;
import com.example.story.entity.MemberEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationEntity", ignore = true)
    MemberEntity entityFromApplicationMemberDto(RequestApplicationMemberDto dto);

    @Mapping(target = "memberId", source = "id")
    ResponseApplicationMemberDto entityToApplicationMemberDto(MemberEntity entity);
}
