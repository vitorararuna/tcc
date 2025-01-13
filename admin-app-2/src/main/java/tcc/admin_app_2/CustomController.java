package tcc.admin_app_2;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-app-2")
public class CustomController {

  private final MeterRegistry meterRegistry;

  public CustomController(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @GetMapping("/custom-healthcheck")
  public String healthCheck() {
    return "A aplicação  1 está rodando!";
  }

  @GetMapping("/custom-metrics")
  public String getMetrics() {
    // Incrementando um contador para monitoramento
    meterRegistry.counter("custom.metrics.accessed").increment();
    return "Métricas acessadas!";
  }

  @GetMapping("/custom-info")
  public String getInfo() {
    return "Informações sobre a aplicação: Esta é uma aplicação monitorada!";
  }
}