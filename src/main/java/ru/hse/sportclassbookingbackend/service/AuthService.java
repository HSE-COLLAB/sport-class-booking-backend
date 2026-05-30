package ru.hse.sportclassbookingbackend.service;


import ru.hse.sportclassbookingbackend.dto.auth.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.auth.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterResponse;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.auth.RegisterTeacherRequest;

import java.util.UUID;

public interface AuthService {
    RegisterResponse registerStudent(RegisterStudentRequest request);

    RegisterResponse registerTeacher(RegisterTeacherRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(UUID refresh);

    void logout(UUID refresh);

    void verifyEmail(UUID token);

    void resendVerification(String email);

    void forgotPassword(String email);

    void resetPassword(String email, String code, String newPassword);
}
