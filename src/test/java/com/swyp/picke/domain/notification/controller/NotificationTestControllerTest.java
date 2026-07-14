package com.swyp.picke.domain.notification.controller;

import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationTestControllerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationTestController(notificationService)).build();

    @Test
    void createTestNotification_callsServiceWithGivenParams() throws Exception {
        mockMvc.perform(post("/api/test/notifications")
                        .param("userId", "1")
                        .param("detailCode", "NEW_BATTLE")
                        .param("body", "테스트 알림 본문")
                        .param("referenceId", "100"))
                .andExpect(status().isOk());

        verify(notificationService).createNotification(1L, NotificationDetailCode.NEW_BATTLE, "테스트 알림 본문", 100L);
    }
}
