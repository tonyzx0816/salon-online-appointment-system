package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {
    @GetMapping("/chat")
    public String chatPage() {
        return "chat";
    }
}
