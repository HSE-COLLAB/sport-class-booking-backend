package ru.hse.sportclassbookingbackend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hse.sportclassbookingbackend.dto.user.UserRequest;
import ru.hse.sportclassbookingbackend.dto.user.UserResponse;
import ru.hse.sportclassbookingbackend.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "first_name", ignore = true)
    @Mapping(target = "last_name", ignore = true)
    @Mapping(target = "middle_name", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "is_active", ignore = true)
    User toEntity(UserRequest req);
}
