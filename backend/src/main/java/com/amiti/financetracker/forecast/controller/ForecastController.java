package com.amiti.financetracker.forecast.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.forecast.dto.ForecastDtos.ForecastDailyResponse;
import com.amiti.financetracker.forecast.dto.ForecastDtos.ForecastMonthResponse;
import com.amiti.financetracker.forecast.service.ForecastService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/month")
    public ForecastMonthResponse month(Authentication authentication) {
        return forecastService.month(currentUserId(authentication));
    }

    @GetMapping("/daily")
    public ForecastDailyResponse daily(Authentication authentication) {
        return forecastService.daily(currentUserId(authentication));
    }
}