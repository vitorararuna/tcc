package tcc.sba_server.notifications;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WhatsAppNotification extends AbstractEventNotifier {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotification.class);

  private final WebClient webClient;
  private final String accountSid;
  private final String authToken;
  private final String fromNumber;
  private final String toNumber;

  public WhatsAppNotification(
      InstanceRepository repository,
      WebClient.Builder webClientBuilder,
      @Value("${twilio.account-sid}") String accountSid,
      @Value("${twilio.auth-token}") String authToken,
      @Value("${twilio.whatsapp.from}") String fromNumber,
      @Value("${twilio.whatsapp.to}") String toNumber
  ) {
    super(repository);
    // O WebClient precisa ser configurado com a URL base da API do Twilio
    this.webClient = webClientBuilder
        .baseUrl("https://api.twilio.com/2010-04-01")
        .build();
    this.accountSid = accountSid;
    this.authToken = authToken;
    this.fromNumber = fromNumber;
    this.toNumber = toNumber;
    logger.info("🟢🟢🟢 WHATSAPP NOTIFIER INICIALIZADO! Enviando para: {}", toNumber);
  }

  @Override
  protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
    // Vamos notificar apenas sobre mudanças de status para simplificar
    if (event instanceof InstanceStatusChangedEvent) {
      return handleStatusChange(instance, (InstanceStatusChangedEvent) event);
    }
    return Mono.empty();
  }

  private Mono<Void> handleStatusChange(Instance instance, InstanceStatusChangedEvent event) {
    String appName = instance.getRegistration().getName();
    String newStatus = event.getStatusInfo().getStatus();

    // Monta a mensagem de texto para o WhatsApp
    String messageBody = String.format(
        "🚨 *Status Alterado: %s* 🚨\n\n" +
            "O serviço mudou de status.\n\n" +
            "*Aplicação:* `%s`\n" +
            "*Novo Status:* `%s`\n" +
            "*URL:* %s",
        appName, appName, newStatus, instance.getRegistration().getServiceUrl()
    );

    return sendToWhatsApp(messageBody);
  }

  private Mono<Void> sendToWhatsApp(String message) {
    // A API do Twilio usa 'form urlencoded' e Autenticação Básica
    return webClient.post()
        .uri("/Accounts/{accountSid}/Messages.json", this.accountSid)
        .headers(headers -> headers.setBasicAuth(this.accountSid, this.authToken))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData("To", "whatsapp:" + this.toNumber)
            .with("From", "whatsapp:" + this.fromNumber)
            .with("Body", message))
        .retrieve()
        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(), response -> {
          logger.error("❌ FALHA NO ENVIO PARA O WHATSAPP: HTTP {}", response.statusCode());
          return response.bodyToMono(String.class)
              .flatMap(body -> {
                logger.error("Resposta do Twilio: {}", body);
                return Mono.error(new RuntimeException("Erro Twilio: " + body));
              });
        })
        .bodyToMono(Void.class)
        .doOnSubscribe(s -> logger.info("📤 Enviando notificação para o WhatsApp..."))
        .doOnSuccess(v -> logger.info("✅ Notificação do WhatsApp enviada com sucesso"))
        .doOnError(e -> logger.error("🔥 ERRO CRÍTICO NO ENVIO PARA O WHATSAPP", e));
  }
}