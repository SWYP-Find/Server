package com.swyp.picke.global.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaticTextFileControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StaticTextFileController()).build();

    @Test
    void appAdsTxt_isServedAsTextPlain() throws Exception {
        mockMvc.perform(get("/app-ads.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    @Test
    void robotsTxt_isServedAsTextPlain() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }
}
