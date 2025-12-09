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

#### Obrigatórios

*   **Java JDK 17** ou superior (necessário para compilar os serviços Spring Boot).
*   **Docker** e **Docker Compose** (versão 2.0+ recomendada).
*   **Gradle** (o projeto utiliza Gradle Wrapper, então não é necessário instalação prévia).

#### Opcionais (mas recomendados)

*   **Colima** (alternativa leve ao Docker Desktop para macOS/Linux). O script `start_services.sh` foi otimizado para trabalhar com Colima, mas também funciona com Docker Desktop.
*   **Credenciais Twilio** (necessárias apenas se desejar usar notificações via WhatsApp):
    * `TWILIO_ACCOUNT_SID`
    * `TWILIO_AUTH_TOKEN`
    * `TWILIO_FROM_NUMBER`
    * `TWILIO_TO_NUMBER`
*   **Discord Webhook URL** (necessária apenas se desejar usar notificações via Discord):
    * `DISCORD_WEBHOOK_URL`

> **Nota:** As notificações são opcionais. O projeto funcionará normalmente sem essas credenciais, apenas sem os recursos de notificação.

### 2. Configuração de Variáveis de Ambiente (Opcional)

Se desejar utilizar notificações via WhatsApp ou Discord, crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```bash
# Twilio (para notificações WhatsApp)
TWILIO_ACCOUNT_SID=seu_account_sid
TWILIO_AUTH_TOKEN=seu_auth_token
TWILIO_FROM_NUMBER=seu_numero_twilio
TWILIO_TO_NUMBER=numero_destino

# Discord (para notificações Discord)
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
```

> **Importante:** O arquivo `.env` não deve ser commitado no repositório. Adicione-o ao `.gitignore`.

### 3. Comandos de Inicialização

O script `start_services.sh` garante que o ambiente seja compilado, limpo e inicializado corretamente:

| Comando | Descrição |
| :--- | :--- |
| `./start_services.sh --setup` | **Setup Inicial (apenas para Colima).** Apaga a VM Colima existente e cria uma nova otimizada. Necessário apenas na primeira execução com Colima ou em caso de problemas. Se estiver usando Docker Desktop, pule este passo. |
| `./start_services.sh` | **Compila, limpa e sobe** todos os contêineres (`sba_server`, `product_service`, `order_service`, `prometheus`, `grafana`) em modo *detached*. O script compila os serviços Spring Boot, limpa containers anteriores e inicia toda a stack. |
| `docker-compose down` | Derruba e remove os contêineres ativos. |
| `docker-compose logs -f [servico]` | Visualiza os logs em tempo real. Substitua `[servico]` por `sba_server`, `order_service`, `product_service`, `prometheus` ou `grafana`. |

### 4. Acessos aos Componentes

Todos os serviços operam na rede Docker customizada `monitoring` (sub-rede `172.19.0.0/24`).

| Componente | Função | Porta Local | URL de Acesso | IP na Rede Docker |
| :--- | :--- | :--- | :--- | :--- |
| **SBA Server** | Central de Gerenciamento | `8080` | `http://localhost:8080` | `172.19.0.2:8080` |
| **Prometheus** | Coleta de Métricas (TSDB) | `9090` | `http://localhost:9090` | `172.19.0.4:9090` |
| **Grafana** | Visualização de Dashboards | `3000` | `http://localhost:3000` | Rede `monitoring` |
| **Order Service** | Microsserviço de Pedidos | `4040` | `http://localhost:4040` | `172.19.0.5:4040` |
| **Product Service** | Microsserviço de Produtos | `2020` | `http://localhost:2020` | `172.19.0.3:2020` |

#### Credenciais Padrão

| Componente | Usuário | Senha |
| :--- | :--- | :--- |
| **SBA Server** | `admin` | `admin` |
| **Grafana** | `admin` | `admin` |

> **Nota:** O dashboard customizado do Grafana é provisionado automaticamente na pasta "TCC - Monitoramento" e já está conectado ao datasource do Prometheus.

---

## 🔧 Troubleshooting

### Problemas Comuns

#### Porta já em uso

Se encontrar erro `Address already in use`, verifique se há containers ou processos usando as portas:

```bash
# Verificar containers em execução
docker ps -a

# Parar todos os containers do projeto
docker-compose down

# Verificar processos usando as portas (macOS/Linux)
lsof -i :8080 -i :9090 -i :3000 -i :4040 -i :2020
```

#### Erro de rede Docker

Se houver conflito de rede (`Pool overlaps with other one`):

```bash
# Remover rede conflitante
docker network ls | grep monitoring
docker network rm <nome_da_rede>

# Recriar ambiente
./start_services.sh
```

#### Grafana não inicia

Se o Grafana falhar ao iniciar, verifique os logs:

```bash
docker logs tcc-grafana-1
```

Problemas comuns:
- **Datasource provisioning error**: Verifique se o arquivo `grafana/provisioning/datasources/prometheus.yml` está correto.
- **Volume permissions**: Certifique-se de que os diretórios `grafana/provisioning` e `grafana/dashboards` existem e têm permissões adequadas.

#### Colima não responde

Se o Docker daemon não responder com Colima:

```bash
# Reiniciar Colima
colima restart

# Verificar status
colima status
```

#### Serviços Spring Boot não compilam

Verifique se o Java 17 está instalado e configurado:

```bash
# Verificar versão do Java
java -version

# Deve mostrar versão 17 ou superior
```

Se necessário, instale o Java 17 através do seu gerenciador de pacotes preferido (Homebrew no macOS, apt no Ubuntu, etc.).

---

## 📚 Estrutura do Projeto

```
tcc/
├── docker-compose.yml          # Orquestração de todos os serviços
├── prometheus.yml              # Configuração do Prometheus
├── start_services.sh           # Script de automação
├── grafana/
│   ├── dashboards/
│   │   └── tcc-dashboard.json  # Dashboard customizado
│   └── provisioning/
│       ├── datasources/        # Provisionamento automático do datasource
│       └── dashboards/         # Provisionamento automático de dashboards
├── sba_server/                 # Spring Boot Admin Server
├── order_service/              # Microsserviço de Pedidos
└── product_service/            # Microsserviço de Produtos
```

---

## 📝 Notas Adicionais

* O projeto utiliza **Gradle Wrapper**, então não é necessário ter o Gradle instalado globalmente.
* Todos os serviços são compilados automaticamente pelo script `start_services.sh`.
* O dashboard do Grafana é provisionado automaticamente e não requer configuração manual.
* As notificações (Twilio/Discord) são completamente opcionais e não impedem o funcionamento do projeto.
