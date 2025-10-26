package tcc.sba_server;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import tcc.sba_server.notifications.DiscordNotification;
import tcc.sba_server.notifications.WhatsAppNotification;

@Import({DiscordNotification.class, WhatsAppNotification.class})

@SpringBootApplication(scanBasePackages = "tcc.sba_server")
@EnableAdminServer
public class SbaServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(SbaServerApplication.class, args);
	}
}