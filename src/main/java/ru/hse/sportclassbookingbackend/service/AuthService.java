package ru.hse.sportclassbookingbackend.service;


import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;

import java.util.UUID;

public interface AuthService {
    AuthResponse registerStudent(RegisterStudentRequest request);

    AuthResponse registerTeacher(RegisterTeacherRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(UUID refresh);

    void logout(UUID refresh);
}
