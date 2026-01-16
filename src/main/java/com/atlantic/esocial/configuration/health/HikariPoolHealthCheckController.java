package com.atlantic.esocial.configuration.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator/pool")
public class HikariPoolHealthCheckController {
    @Autowired
    private HikariHealthIndicator hikariHealthIndicator;

    @GetMapping
    public Health health() {
        return hikariHealthIndicator.health();
    }
}
