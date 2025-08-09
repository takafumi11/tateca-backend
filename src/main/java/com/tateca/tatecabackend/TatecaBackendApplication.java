package com.tateca.tatecabackend;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(exclude = {
	org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration.class
})
@Slf4j
@RequiredArgsConstructor
public class TatecaBackendApplication {

	private final Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(TatecaBackendApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logApplicationProfile() {
		String[] activeProfiles = environment.getActiveProfiles();
		
		log.info("=== APPLICATION CONFIGURATION ===");
		log.info("Active Profiles: {}", activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "None");
		
		// Log which property files are being used
		if (activeProfiles.length > 0) {
			for (String profile : activeProfiles) {
				log.info("Using property file: application-{}.properties", profile);
			}
		}
		log.info("Using property file: application.properties");
		
		// Log current timezone and JVM settings
		log.info("Timezone: {}", System.getProperty("user.timezone", "System Default"));
		log.info("JVM Memory: Xms={}, Xmx={}", 
			getSystemProperty("Xms"), getSystemProperty("Xmx"));
		
		log.info("=== APPLICATION READY ===");
	}
	
	private String getSystemProperty(String key) {
		return System.getProperty(key, "Not specified");
	}
}
