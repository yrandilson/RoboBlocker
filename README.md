# 🛡️ RoboBlocker — Bloqueador de Chamadas de Robô

App Android avançado para bloquear robocalls e telemarketing com **lista negra manual + IA (Claude API)**.

---

## ✨ Funcionalidades

| Recurso | Descrição |
|---|---|
| 🛡️ Triagem nativa Android | Usa `CallScreeningService` (API 26+) — bloqueia antes de tocar |
| 🔍 Detecção por padrões | Prefixos 0800/4003/3003, códigos internacionais, números spoofados |
| 🤖 IA com Claude | Análise profunda via Claude API para casos borderline |
| ⚡ Abuso de frequência | Auto-bloqueia números que ligam X+ vezes por hora |
| 📋 Lista negra manual | Adicionar, remover, buscar, importar e exportar CSV |
| 🔤 Bloqueio por padrão/prefixo | Ex: bloquear todos que começam com `0800` |
| 📱 Whitelist de contatos | Nunca bloqueia números salvos na agenda |
| 🕐 Bloqueio agendado | Ativa apenas em horários configurados (ex: 22h–8h) |
| 🌍 Bloquear internacionais | Bloqueia tudo que não for +55 |
| 📊 Dashboard com estatísticas | Total bloqueado hoje, semana, total |
| 📜 Histórico detalhado | Motivo, categoria, confiança da IA |
| 🔔 Notificações inteligentes | Notifica chamadas bloqueadas com motivo |

---

## 🚀 Como compilar e instalar

### Pré-requisitos
- **Android Studio Hedgehog** (2023.1.1) ou superior
- **JDK 17**
- **Android SDK 34**
- Dispositivo/emulador com **Android 8.0+** (API 26)

### Passos

```bash
# 1. Abra a pasta no Android Studio
File > Open > selecione a pasta RoboBlocker/

# 2. Sincronize o Gradle
(Android Studio faz isso automaticamente)

# 3. Compile e instale
./gradlew installDebug
# ou use o botão ▶️ no Android Studio
```

### Build de produção (APK assinado)
```bash
./gradlew assembleRelease
# APK em: app/build/outputs/apk/release/app-release.apk
```

---

## ⚙️ Configuração inicial no celular

### 1. Conceder permissões
O app solicitará automaticamente:
- Leitura de estado do telefone
- Leitura/escrita do histórico de chamadas
- Leitura de contatos
- Notificações

### 2. Ativar como serviço de triagem (OBRIGATÓRIO)
O Android exibe um diálogo perguntando se o RoboBlocker pode ser o **aplicativo de triagem de chamadas**.

> Responda **"Sim"** — sem isso o bloqueio não funciona.

Caso não apareça: **Configurações do Android → Apps → Aplicativos padrão → Aplicativo de triagem de chamadas → RoboBlocker**

### 3. Configurar IA (opcional mas recomendado)
1. Acesse a aba **Configurações** no app
2. Role até a seção **🤖 Inteligência Artificial**
3. Cole sua chave de API Anthropic (`sk-ant-...`)
4. Toque em **"Salvar chave"**

> Obtenha uma chave em: https://console.anthropic.com

---

## 🧠 Como funciona o sistema de detecção

```
Chamada recebida
     │
     ▼
[1] É contato salvo? ──► SIM → Permitir
     │ NÃO
     ▼
[2] Está na lista negra manual? ──► SIM → Bloquear
     │ NÃO
     ▼
[3] Corresponde a padrão/prefixo? ──► SIM → Bloquear
     │ NÃO
     ▼
[4] Abuso de frequência (X+/hora)? ──► SIM → Bloquear + auto-adicionar
     │ NÃO
     ▼
[5] Heurística local (0800, spoofing, etc)? ──► SIM → Bloquear
     │ NÃO / Inconclusivo
     ▼
[6] IA Claude API (casos borderline)? ──► SPAM → Bloquear
     │ NÃO
     ▼
Permitir chamada
```

---

## 📁 Estrutura do projeto

```
app/src/main/java/com/roboblocker/
├── App.kt                          # Application class
├── MainActivity.kt                 # Activity principal
├── ai/
│   ├── ClaudeAIAnalyzer.kt        # Integração Claude API
│   └── SpamPatternDetector.kt     # Heurísticas offline (BR + global)
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt         # Room database
│   │   ├── Daos.kt                # DAOs (BlockedNumber + CallLog)
│   │   └── Entities.kt            # Entidades Room
│   ├── prefs/
│   │   └── AppPreferences.kt      # SharedPreferences wrapper
│   └── repository/
│       └── BlockerRepository.kt   # Repositório de dados
├── service/
│   └── RoboCallScreeningService.kt # Serviço de triagem (núcleo)
├── receiver/
│   └── BootReceiver.kt            # Reinicia após reboot
├── ui/
│   ├── dashboard/DashboardFragment.kt
│   ├── blocklist/BlocklistFragment.kt
│   ├── logs/LogsFragment.kt
│   └── settings/SettingsFragment.kt
├── adapter/
│   ├── BlockedNumberAdapter.kt
│   └── CallLogAdapter.kt
├── viewmodel/
│   └── MainViewModel.kt
└── utils/
    ├── Extensions.kt
    └── NotificationHelper.kt
```

---

## 📦 Padrões de telemarketing BR detectados automaticamente

- `0800xxxx` — números gratuitos (SAC, telemarketing)
- `4003xxxx`, `3003xxxx`, `3004xxxx` — bancos / cobranças
- `0900xxxx` — números premium
- Números com 4 dígitos (ex: `4004`)
- Dígitos repetidos: `11111111`, `99999999`
- Números internacionais suspeitos: `+1268`, `+374`, `+234`, `+86`, `+7`

---

## 🔒 Privacidade

- Nenhum dado é enviado a servidores externos **exceto** quando a IA está ativa
- Com IA ativa: apenas o número de telefone é enviado à API Anthropic para análise
- Todos os logs e lista negra ficam armazenados **localmente** no dispositivo (Room/SQLite)
- A chave de API fica armazenada em SharedPreferences no próprio dispositivo

---

## ⚠️ Limitações conhecidas

- **Requer Android 8.0+** — `CallScreeningService` foi introduzido na API 26
- **Requer ser app padrão de triagem** — o Android só permite um app por vez nesse papel
- Em alguns ROMs personalizados (MIUI, One UI), pode ser necessário conceder permissão adicional em "Bateria → Não otimizar" para que o serviço persista
- Chamadas VoIP (WhatsApp, Telegram) **não** são interceptadas pelo `CallScreeningService`

---

## 📄 Licença

MIT — use livremente, inclusive em projetos comerciais.
