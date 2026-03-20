package ru.hse.sportclassbookingbackend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TmpContorller {

    @GetMapping("/auth")
    public String tmp(HttpServletRequest req){
        System.out.println(req.getHeader("User-Agent"));
        return "nice";
    }
}
