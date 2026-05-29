package io.book.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BookAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookAiApplication.class, args);
    }
}
