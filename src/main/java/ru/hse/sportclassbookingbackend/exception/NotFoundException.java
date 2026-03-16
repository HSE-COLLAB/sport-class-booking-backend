package ru.hse.sportclassbookingbackend.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException (String message) {
        super(message);
    }
}
