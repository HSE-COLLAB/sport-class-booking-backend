package ru.hse.sportclassbookingbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterTeacherRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String middleName;
    @Email
    private String email;
    @Size(min = 8)
    private String password;
    @NotBlank
    private String position;
}
