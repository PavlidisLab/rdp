package ubc.pavlab.rdp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EmailConfig {

    @Bean
    public JavaMailSender emailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    public AsyncTaskExecutor emailTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}