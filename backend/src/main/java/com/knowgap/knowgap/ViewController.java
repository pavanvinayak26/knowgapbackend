package com.knowgap.knowgap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/home")
    public String homeView(Model model) {
        model.addAttribute("appName", "Knowgap");
        model.addAttribute("javaVersion", "21");
        model.addAttribute("springBootVersion", "3.5");
        return "index";
    }
}
