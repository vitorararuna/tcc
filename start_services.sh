#!/bin/bash

# Faz o script sair imediatamente se um comando falhar.
set -e

# --- FUNÇÃO DE CONFIGURAÇÃO ÚNICA DO COLIMA ---
setup_colima() {
  echo "⚠️  AVISO: Esta operação irá apagar sua VM Colima atual."
  read -p "Você tem certeza que quer continuar? (s/n) " -n 1 -r
  echo ""
  if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    echo "Operação cancelada."
    exit 1
  fi

  echo "🔥 Parando e apagando a VM Colima existente..."
  colima stop || true # Ignora o erro se já estiver parado
  colima delete

  echo "🚀 Criando uma nova VM Colima otimizada para performance e com acesso à internet..."
  colima start --cpu 4 --memory 8 --vm-type=vz --mount-type=virtiofs --dns=8.8.8.8 --dns=8.8.4.4

  echo "✅ Nova VM Colima configurada com sucesso!"
}

# --- FUNÇÃO PARA ESPERAR PELO DOCKER ---
wait_for_docker() {
  echo "⏳ Aguardando o Docker daemon ficar pronto..."
  # Aumentamos o timeout para 60 segundos para dar mais tempo após o Mac acordar
  for i in {1..30}; do
    if docker info > /dev/null 2>&1; then
      echo "✅ Docker daemon está online e pronto para receber comandos!"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo ""
  echo "❌ Erro: O Docker daemon não respondeu após 60 segundos."
  echo "   Tente reiniciar o Colima manualmente com 'colima restart' e rode o script novamente."
  exit 1
}


# --- LÓGICA PRINCIPAL DO SCRIPT ---

# Se --setup for passado, executa a configuração e sai.
if [[ "$1" == "--setup" ]]; then
  setup_colima
  wait_for_docker # Espera o Docker subir após a nova instalação
  echo "🎉 Configuração inicial completa. Agora você pode rodar './start_services.sh' para iniciar os serviços."
  exit 0
fi

echo "🩺 Verificando a saúde do ambiente Colima/Docker..."
if ! colima status > /dev/null 2>&1; then
    echo "🤔 Colima não está rodando. Iniciando..."
    colima start
fi

# Etapa importante: garante que o Docker está 100% responsivo antes de executar qualquer comando 'docker-compose'.
wait_for_docker


# --- 1. COMPILAR OS SERVIÇOS ---
echo "🚀 Iniciando a compilação dos serviços..."
SERVICES=("sba_server" "product_service" "order_service")
for SERVICE in "${SERVICES[@]}"; do
  echo "----------------------------------------"
  echo "📦 Compilando o serviço: $SERVICE"
  echo "----------------------------------------"
  (cd "./$SERVICE" && ./gradlew build -x test)
done
echo "✅ Todos os serviços foram compilados com sucesso."
echo ""


# --- 2. GARANTIR UM AMBIENTE DOCKER TOTALMENTE LIMPO ---
echo "🧹 Encerrando ambiente Docker Compose anterior (se existir)..."
docker-compose down --remove-orphans
echo "✅ Ambiente Docker Compose anterior foi limpo."
echo ""


# --- 3. INICIAR OS CONTÊINERES ---
echo "🚢 Iniciando os contêineres com Docker Compose..."
docker-compose up --build -d

echo ""
echo "🎉 Ambiente iniciado! Use 'docker-compose logs -f' para ver os logs."
echo "   Para parar tudo, use 'docker-compose down'."
echo "   Se o Docker parecer travado, tente 'colima restart'."