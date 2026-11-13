# AgentPay × LangGraph4j — Pay-per-task example

Minimal **3-node** Java graph that settles work with **[AgentPay](https://edgerail.pages.dev/docs)** (Cloudflare Workers ledger).

```text
START → coordinator → worker → settlement → END
          │              │           │
       passport*      fake work   release
       escrow hold    + hash      or refund
```

\* Passport is soft-fail in v0 (demo still runs if the route differs).

Built for conversation with **LangGraph4j** ([org.bsc.langgraph4j](https://github.com/langgraph4j/langgraph4j)) — pure **REST JSON**, no crypto wallets.

## Requirements

- **Java 17+**
- **Maven 3.9+**
- AgentPay API key (`a2a_…`) from the dashboard
- Buyer repo funded (faucet / lab) so escrow hold can debit

## Setup

```bash
git clone <this-repo-url>
cd langgraph4j-agentpay-example

export AGENTPAY_BASE_URL=https://autonomous-edge-agent-economy.explorercosmic7.workers.dev
export AGENTPAY_API_KEY=a2a_YOUR_KEY
export AGENTPAY_BUYER_REPO=swarm/bot-1
export AGENTPAY_WORKER_REPO=swarm/bot-2
export AGENTPAY_AMOUNT=0.05
```

## 1) Smoke test (REST only)

```bash
mvn -q compile exec:java -Dexec.mainClass=com.agentpay.langgraph4j.SmokeTestMain
```

Expect: hold → release JSON without errors.

## 2) Full graph

```bash
mvn -q compile exec:java
```

Default main class: `PayPerTaskGraphApp`.

### Refund path

```bash
export AGENTPAY_FORCE_FAIL=true
mvn -q compile exec:java
```

Settlement node calls **escrow refund** instead of release.

## Project layout

```text
pom.xml
src/main/java/com/agentpay/langgraph4j/
  AgentPayClient.java      # HttpClient + Jackson
  PayPerTaskState.java     # LangGraph4j AgentState
  CoordinatorNode.java     # passport + escrow hold
  WorkerNode.java          # simulated work + sha256
  SettlementNode.java      # release or refund
  PayPerTaskGraphApp.java  # StateGraph wiring
  SmokeTestMain.java       # REST-only check
```

## AgentPay APIs used

| Step | Method | Path |
|------|--------|------|
| Passport (optional) | GET | `/api/passport?repo=` |
| Hold | POST | `/api/escrow/hold` |
| Release | POST | `/api/escrow/release` |
| Refund | POST | `/api/escrow/refund` |
| Balance | GET | `/api/balance?repo=` |

Auth header: `Authorization: Bearer a2a_…`

## Notes for LangGraph4j users

- Nodes are plain `NodeAction<PayPerTaskState>` — same pattern as official examples.
- Side effects (HTTP) run **inside** nodes; state only stores ids/hashes/status.
- MCP is optional: this skeleton uses REST so it runs with only an API key.
- Live lab: https://edgerail.pages.dev/agent-lab  
- Docs: https://edgerail.pages.dev/docs  

## License

MIT — example code. AgentPay is a separate service.
