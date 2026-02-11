# SkyControl

O **SkyControl** é um sistema distribuído para monitorização, controle e simulação de drones. O projeto utiliza uma arquitetura orientada a eventos (Event-Driven Architecture) para processar telemetria, gerir alertas e enviar comandos entre um simulador de drones e um backend de gestão.

##  Arquitetura do Projeto

O sistema é composto por três serviços principais que comunicam entre si:

1. **RabbitMQ (Broker de Mensagens):**
* Responsável pela comunicação assíncrona entre o simulador e o backend.
* Gere filas de telemetria, alertas e comandos.


2. **Drone Simulator (`dronesimulator`):**
* Serviço que simula o comportamento de drones.
* Gera dados de telemetria em tempo real e consome comandos de controlo.
* Publica eventos na fila de telemetria.


3. **Drone Backend (`dronebackend`):**
* O "cérebro" do sistema.
* Consome a telemetria enviada pelo simulador.
* Processa regras de negócio, gera alertas e persiste dados.
* Expõe APIs para interação (via REST ou WebSocket).



## 🚀 Tecnologias Utilizadas

* **Linguagem:** Java
* **Framework:** Spring Boot 3.5.7
* Spring AMQP (Integração com RabbitMQ)
* Spring Web / WebFlux
* Spring WebSocket


* **Mensageria:** RabbitMQ 3.12 (Management Alpine)
* **Containerização:** Docker & Docker Compose
* **Build Tool:** Maven

## 📂 Estrutura de Filas (RabbitMQ)

Conforme configurado no `application.properties`, o sistema utiliza as seguintes filas e *routing keys*:

| Tipo | Fila (Queue) | Routing Key | Descrição |
| --- | --- | --- | --- |
| **Telemetria** | `drone.telemetry.queue` | `drone.telemetry.*` | Dados de voo enviados pelo simulador. |
| **Alertas** | `alert.queue` | `drone.alert.*` | Alertas gerados pelo sistema (ex: bateria fraca). |
| **Comandos** | `drone.command.queue` | `drone.command.*` | Comandos enviados do backend para os drones. |

## 🛠️ Como Executar

A forma mais fácil de correr o projeto é utilizando o **Docker Compose**, que orquestra todos os serviços automaticamente.

### Pré-requisitos

* Docker e Docker Compose instalados na máquina.

### Passo a Passo

1. **Clonar o repositório:**
```bash
git clone <https://github.com/LeonardoPPasseri/SkyControl.git>
cd SkyControl

```


2. **Compilar e Iniciar os contentores:**
Na raiz do projeto (onde está o ficheiro `docker-compose.yaml`), executa:
```bash
docker-compose up --build

```


*Este comando irá construir as imagens do backend e do simulador e iniciar o RabbitMQ.*
3. **Verificar o estado dos serviços:**
O sistema estará pronto quando vires logs a indicar que as aplicações Spring Boot iniciaram com sucesso.

### 🔌 Aceder aos Serviços

Uma vez que o sistema esteja a correr, podes aceder através das seguintes portas:

* **Backend API:** `http://localhost:8080`
* **Simulador API:** `http://localhost:8081`
* **RabbitMQ Management UI:** `http://localhost:15672`
* **User:** `guest`
* **Password:** `guest`

  

## 📝 Notas Adicionais

* **Persistência:** O backend está configurado para montar um volume em `./data` (`./data:/app/data`), garantindo que dados gerados (logs ou ficheiros JSON) não se perdem ao reiniciar o contentor.
* **Dependências:** O serviço `backend` aguarda que o `simulator` inicie, e ambos aguardam que o `rabbitmq` esteja "saudável" (healthcheck) antes de arrancar.

---
