package ru.hse.sportclassbookingbackend.dto;



public record AuthResponse (
     String accessToken,
     String refreshToken
){

}
