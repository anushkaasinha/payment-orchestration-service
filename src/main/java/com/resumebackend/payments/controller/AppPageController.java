package com.resumebackend.payments.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppPageController {

    @GetMapping({"/app", "/app/"})
    public String app() {
        return "forward:/app/index.html";
    }
}
