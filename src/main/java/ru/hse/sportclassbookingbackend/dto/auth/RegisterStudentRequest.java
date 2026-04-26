package ru.hse.sportclassbookingbackend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterStudentRequest(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String middleName,
        @NotBlank
        @Email
        @Schema(example = "newstudent@mail.ru")
        String email,
        @NotBlank
        @Schema(description = "пароль в plaintext — будет захеширован BCrypt")
        String password,
        @Schema(description = "ID студенческой группы — список из GET /student-groups",
                example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        @NotNull
        UUID groupId,
        @Schema(description = "ID кампуса (1=Москва, 2=СПб, 3=НН, 4=Пермь)", example = "3")
        @NotNull
        Integer campusId
) {

}
