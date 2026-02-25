package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index"; // refers to templates/index.html
    }
}
