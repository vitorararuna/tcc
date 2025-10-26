package tcc.sba_server.notifications;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.*;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DiscordNotification extends AbstractEventNotifier {

  private static final Logger logger = LoggerFactory.getLogger(DiscordNotification.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private final WebClient webClient;
  private final String webhookUrl;

  @Autowired
  public DiscordNotification(
      InstanceRepository repository,
      WebClient.Builder webClientBuilder,
      @Value("${spring.boot.admin.notify.discord.webhook-url}") String webhookUrl
  ) {
    super(repository);
    this.webClient = webClientBuilder.build();
    this.webhookUrl = webhookUrl;
    logger.error("🟢🟢🟢 DISCORD NOTIFIER INICIALIZADO! Webhook: {}", webhookUrl);
  }

  @PostConstruct
  public void init() {
    sendToDiscord(Map.of(
        "content", "🚀 Notificador personalizado inicializado com sucesso",
        "embeds", new Object[]{
            Map.of(
                "title", "Configuração Carregada",
                "description", "Webhook: ||" + webhookUrl + "||",
                "color", 5814783
            )
        }
    )).subscribe();
  }

  @Override
  protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
    if (event instanceof InstanceStatusChangedEvent) {
      return handleStatusChange(instance, (InstanceStatusChangedEvent) event);
    } else if (event instanceof InstanceRegisteredEvent) {
      return handleRegistration(instance);
    } else {
      logger.debug("Evento não tratado: {}", event.getType());
      return Mono.empty();
    }
  }

  private Mono<Void> handleStatusChange(Instance instance, InstanceStatusChangedEvent event) {
    logger.error("🎯 STATUS ALTERADO: {} → {}", instance.getRegistration().getName(), event.getStatusInfo().getStatus());

    Map<String, Object> embed = new LinkedHashMap<>();
    embed.put("title", "🔄 Status Alterado: " + instance.getRegistration().getName());
    embed.put("color", getColor(event.getStatusInfo().getStatus()));

    embed.put("fields", new Object[]{
        Map.of("name", "Aplicação", "value", "```" + instance.getRegistration().getName() + "```", "inline", true),
        Map.of("name", "Status Anterior", "value", "`" + instance.getStatusInfo().getStatus() + "`", "inline", true),
        Map.of("name", "Novo Status", "value", "`" + event.getStatusInfo().getStatus() + "`", "inline", true),
        Map.of("name", "Detalhes", "value", formatDetails(event.getStatusInfo().getDetails()), "inline", false)
    });

    embed.put("footer", Map.of(
        "text", "⏰ " + LocalDateTime.now().format(DATE_FORMATTER),
        "icon_url", "https://cdn-icons-png.flaticon.com/512/5262/5262027.png"
    ));

    return sendToDiscord(Map.of("embeds", new Object[]{embed}));
  }

  private Mono<Void> handleRegistration(Instance instance) {
    logger.error("🎯 NOVO REGISTRO: {}", instance.getRegistration().getName());

    Map<String, Object> embed = new LinkedHashMap<>();
    embed.put("title", "📥 Nova Aplicação Registrada");
    embed.put("color", 3447003); // Azul Discord
    embed.put("thumbnail", Map.of(
        "url", "https://cdn-icons-png.flaticon.com/512/5262/5262027.png"
    ));

    embed.put("fields", new Object[]{
        Map.of("name", "Nome", "value", "```" + instance.getRegistration().getName() + "```", "inline", true),
        Map.of("name", "ID", "value", "```" + instance.getId() + "```", "inline", true),
        Map.of("name", "Service URL", "value", "```" + instance.getRegistration().getServiceUrl() + "```", "inline", false)
    });

    embed.put("footer", Map.of(
        "text", "📅 Registrado em " + LocalDateTime.now().format(DATE_FORMATTER)
    ));

    return sendToDiscord(Map.of("embeds", new Object[]{embed}));
  }

  private String formatDetails(Object details) {
    if (details instanceof Map) {
      StringBuilder sb = new StringBuilder("```json\n");
      ((Map<?, ?>) details).forEach((k, v) -> sb.append("➥ ").append(k).append(": ").append(v).append("\n"));
      return sb.append("```").toString();
    }
    return "```" + (details != null ? details.toString() : "Sem detalhes") + "```";
  }

  private int getColor(String status) {
    return switch (status) {
      case "UP" -> 5763719;    // Verde
      case "DOWN" -> 15548997; // Vermelho
      case "OFFLINE" -> 9807270; // Cinza
      default -> 15844367;     // Amarelo
    };
  }

  private Mono<Void> sendToDiscord(Map<String, Object> message) {
    return webClient.post()
        .uri(webhookUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(message)
        .retrieve()
        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(), response -> {
          logger.error("❌ FALHA NO ENVIO: HTTP {}", response.statusCode());
          return response.bodyToMono(String.class)
              .flatMap(body -> {
                logger.error("Resposta do Discord: {}", body);
                return Mono.error(new RuntimeException("Erro Discord: " + body));
              });
        })
        .bodyToMono(Void.class)
        .doOnSubscribe(s -> logger.info("📤 Enviando notificação: {}", message))
        .doOnSuccess(v -> logger.info("✅ Notificação enviada com sucesso"))
        .doOnError(e -> logger.error("🔥 ERRO CRÍTICO NO ENVIO", e));
  }
}
