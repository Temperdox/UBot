package com.cottonlesergal.ubot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Default controller to serve static pages
 */
@Controller
public class DefaultController {

    /**
     * Redirect root to index.html
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Redirect /login to login.html
     */
    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }
}