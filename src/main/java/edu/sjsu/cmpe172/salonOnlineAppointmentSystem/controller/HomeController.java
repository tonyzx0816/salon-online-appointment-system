package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "API is running. Try /items";
    }
}
