package com.amiti.financetracker.notifications.controller;

import static com.amiti.financetracker.security.SecurityUtils.currentUserId;

import com.amiti.financetracker.bootstrap.DemoFinanceDataService;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationClearRequest;
import com.amiti.financetracker.notifications.dto.NotificationDtos.NotificationResponse;
// import com.amiti.financetracker.notifications.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final DemoFinanceDataService demoFinanceDataService;
    // private final NotificationService notificationService;

       public NotificationController(DemoFinanceDataService demoFinanceDataService) {
        this.demoFinanceDataService = demoFinanceDataService;
       }
//     public NotificationController(NotificationService notificationService) {
//         this.notificationService = notificationService;
//     }

//     @GetMapping
//     public List<NotificationResponse> list(Authentication authentication) {
//         return notificationService.list(currentUserId(authentication));
//     }

//     @PostMapping("/clear")
//     public void clear(Authentication authentication, @Valid @RequestBody NotificationClearRequest request) {
//         notificationService.clear(currentUserId(authentication), request);
//     }
   @GetMapping
    public List<NotificationResponse> list() {
        return demoFinanceDataService.notifications();
    }
}
