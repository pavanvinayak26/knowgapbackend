package com.knowgap.knowgap;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/api/home")
    @ResponseBody
    public String homeApi() {
        return "Welcome to Knowgap! App is running on Java 21 with Spring Boot 3.5.";
    }

    // Let Spring Boot BasicErrorController handle /error to avoid ambiguous mapping.
    // Custom error page is provided in src/main/resources/templates/error.html.

}
