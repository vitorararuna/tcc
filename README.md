# Projeto TCC: Observabilidade em Microsserviços com Spring Boot Admin e Stack Prometheus/Grafana

## 📝 Visão Geral do Projeto

Este projeto demonstra a implementação de uma arquitetura de **Microsserviços** monitorada de ponta a ponta, utilizando o **Spring Boot Admin (SBA)** como central de gerenciamento e a pilha **Prometheus e Grafana** para análise quantitativa e dashboards de negócio.

O projeto utiliza uma arquitetura organizacional em **Monorepo**, onde todos os serviços e a infraestrutura de orquestração (Docker Compose) residem no mesmo repositório.

### 🎯 Principais Contribuições e Funcionalidades

O projeto vai além do monitoramento técnico, focando na extensão e personalização das ferramentas para atender a necessidades de negócio específicas:

| Funcionalidade | Descrição e Recursos Utilizados |
| :--- | :--- |
| **Automação de Dashboards (Provisioning)** | O dashboard customizado (`tcc-dashboard.json`, UID `spring_boot_21`) é carregado automaticamente no Grafana na inicialização do Docker Compose, garantindo que os links do SBA permaneçam funcionais. |
| **KPIs de Negócio Customizados** | Instrumentação profunda no `order_service` utilizando Micrometer para medir: **Volume Total de Produtos** (`order_products_total`) e **Combinações de Produtos** (`order_product_combinations`), gerando inteligência estratégica diretamente das métricas. |
| **Notificadores Customizados** | Superação da limitação nativa do SBA: Notificadores implementados (extensão de `AbstractEventNotifier`) para enviar alertas ricos e formatados via **Discord** e alertas de alta criticidade via **WhatsApp** (utilizando a API Twilio). |
| **Extensão da UI do SBA** | Criação de uma **Custom View** no frontend (Vue.js) do SBA para exibir dados de negócio em tempo real (ex: **"Pedidos Atrasados"**), com o SBA atuando como proxy para o endpoint `/delayed-view` do `order_service`. |
| **Gerenciamento Ativo** | Utilização da interface nativa do SBA para gerenciar o ciclo de vida dos serviços (`/restart`, `/shutdown`) e inspecionar/acionar tarefas agendadas (`@Scheduled`) críticas, como a `OrderProcessingTask`. |

---

## 🛠️ Setup e Inicialização

Toda a arquitetura é definida e inicializada via Docker Compose e um script de automação (`start_services.sh`) que gerencia a compilação e orquestração.

### 1. Pré-requisitos

*   **Docker** e **Docker Compose**.
*   **Colima** (Recomendado para ambientes Linux/macOS, conforme `start_services.sh`).
*   **Gradle** (Para compilar os projetos Spring Boot).
*   **Credenciais Twilio** (Necessárias para notificações via WhatsApp, definidas como variáveis de ambiente no `docker-compose.yml`).

### 2. Comandos de Inicialização

O script `start_services.sh` garante que o ambiente seja compilado, limpo e inicializado corretamente:

| Comando | Descrição |
| :--- | :--- |
| `./start_services.sh --setup` | **Setup Inicial.** Apaga a VM Colima existente e cria uma nova (necessário apenas na primeira execução ou em caso de problemas). |
| `./start_services.sh` | **Compila, limpa e sobe** todos os contêineres (`sba_server`, `product_service`, `order_service`, `prometheus`, `grafana`) em modo *detached*. |
| `docker-compose down` | Derruba e remove os contêineres ativos. |

### 3. Acessos aos Componentes

Todos os serviços operam na rede Docker customizada `monitoring` (sub-rede `172.19.0.0/24`).

| Componente | Função | Porta Local | URL de Acesso | IP na Rede Docker |
| :--- | :--- | :--- | :--- | :--- |
| **SBA Server** | Central de Gerenciamento | `8080` | `http://localhost:8080` | `172.19.0.2:8080` |
| **Prometheus** | Coleta de Métricas (TSDB) | `9090` | `http://localhost:9090` | `172.19.0.4:9090` |
| **Grafana** | Visualização de Dashboards | `3000` | `http://localhost:3000` | `172.19.0.4:3000` |
| **Order Service** | Microsserviço de Pedidos | `4040` | - | `172.19.0.5:4040` |
| **Product Service** | Microsserviço de Produtos | `2020` | - | `172.19.0.3:2020` |

*Credenciais Padrão (SBA Server):* **Usuário:** `admin`, **Senha:** `admin`.
