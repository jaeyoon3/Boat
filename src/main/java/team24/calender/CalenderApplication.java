package team24.calender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


import javax.annotation.PostConstruct;
import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
@EnableMongoRepositories(basePackages={"team24.calender.domain","team24.calender.repository"})
public class CalenderApplication {



	public static void main(String[] args) {
		SpringApplication.run(CalenderApplication.class, args);
	}


}
