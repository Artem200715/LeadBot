package bot.LeadBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"bot.LeadBot", "bot.func"})
@EntityScan(basePackages = "bot.db")
@EnableAsync
public class LeadBotApplication {

	static void main(String[] args) {
		SpringApplication.run(LeadBotApplication.class, args);
	}

}
