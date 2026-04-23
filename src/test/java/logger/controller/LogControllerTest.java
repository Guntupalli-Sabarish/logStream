package logger.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import logger.alert.AlertEngine;
import logger.pojo.Log;
import logger.service.Logger;
import logger.service.SseEmitterRegistry;
import logger.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(LogController.class)
class LogControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean Logger           loggerService;
    @MockBean StatsService     statsService;
    @MockBean SseEmitterRegistry emitterRegistry;
    @MockBean AlertEngine      alertEngine;   
    @Test
    void getLogs_returnsLogList() throws Exception {
        Log log = new Log("test message");
        log.setId("abc-123");
        when(loggerService.getLogs()).thenReturn(List.of(log));
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].data", is("test message")));
    }
    @Test
    void postLog_setsTimestampWhenMissing() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Log("no timestamp"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", notNullValue()));
        verify(loggerService, times(1)).addLog(any());
    }
    @Test
    void postLog_setsIdWhenMissing() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Log("no id"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(blankOrNullString())));
    }
    @Test
    void postLog_setsThreadNameWhenMissing() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Log("no thread"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadName", not(blankOrNullString())));
    }
    @Test
    void postLog_preservesExistingThreadName() throws Exception {
        Log log = new Log("has thread");
        log.setThreadName("UI-Thread");
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threadName", is("UI-Thread")));
    }
    @Test
    void deleteLogs_callsClearAndReturns200() throws Exception {
        mockMvc.perform(delete("/api/logs"))
                .andExpect(status().isOk());
        verify(loggerService, times(1)).clearLogs();
    }
}
