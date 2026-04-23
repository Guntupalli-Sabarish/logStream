package logger.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "Logging System Service is Running! 🚀 (v2) <br> Go to <a href='/api/logs'>/api/logs</a> to see the logs.";
    }
}
