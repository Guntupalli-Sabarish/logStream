package logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = "logger")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
