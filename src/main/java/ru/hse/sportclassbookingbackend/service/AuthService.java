package ru.hse.sportclassbookingbackend.service;


import ru.hse.sportclassbookingbackend.dto.AuthResponse;
import ru.hse.sportclassbookingbackend.dto.LoginRequest;
import ru.hse.sportclassbookingbackend.dto.RegisterStudentRequest;
import ru.hse.sportclassbookingbackend.dto.RegisterTeacherRequest;

public interface AuthService {
    AuthResponse registerStudent(RegisterStudentRequest request);

    AuthResponse registerTeacher(RegisterTeacherRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refresh);

}
