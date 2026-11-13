# Steps: create GitHub repo + run (no coding required)

## A. Create the GitHub repository

1. Open https://github.com/new
2. Repository name: `langgraph4j-agentpay-example`
3. Public
4. **Do not** add README/license on GitHub (we already have files)
5. Create repository

## B. Upload these files

### Option 1 — GitHub website (easiest)

1. On the empty repo page → **uploading an existing file**
2. Drag the **entire contents** of folder `langgraph4j-agentpay-example`:
   - `pom.xml`
   - `README.md`
   - `.gitignore`
   - `.env.example`
   - `src/` (whole folder tree)
3. Commit message: `Initial AgentPay × LangGraph4j example`
4. Commit

### Option 2 — Git command line

```bash
cd langgraph4j-agentpay-example
git init
git add .
git commit -m "Initial AgentPay × LangGraph4j pay-per-task example"
git branch -M main
git remote add origin https://github.com/YOUR_USER/langgraph4j-agentpay-example.git
git push -u origin main
```

## C. Get an API key

1. Open AgentPay dashboard → API keys  
2. Create key with at least **pay** (or full) scope  
3. Copy `a2a_…` once  

## D. Fund buyer

1. Lab or dashboard faucet → fund `swarm/bot-1` (or your buyer repo)  
2. Enough balance for `AGENTPAY_AMOUNT` (e.g. 0.05)

## E. Run smoke test

```bash
export AGENTPAY_API_KEY=a2a_...
export AGENTPAY_BUYER_REPO=swarm/bot-1
export AGENTPAY_WORKER_REPO=swarm/bot-2
mvn -q compile exec:java -Dexec.mainClass=com.agentpay.langgraph4j.SmokeTestMain
```

## F. Run full graph

```bash
mvn -q compile exec:java
```

## G. Email Bartolomeo

Subject: LangGraph4j pay-per-task example (AgentPay escrow)

Body idea:
- Link to your public repo
- Graph: coordinator → worker → settlement
- REST today; MCP optional later
- Ask if the StateGraph shape matches how they prefer side effects in nodes

## Common errors

| Error | Fix |
|-------|-----|
| `Set AGENTPAY_API_KEY` | Export real key |
| `Insufficient funds` / 402 | Faucet buyer repo |
| `key_scope_denied` | Key needs pay scope |
| `escrow hold failed` | Check Worker routes; fund buyer |
| Maven / Java version | Install JDK 17+ |
| Compile error on Channels / AgentState | Ensure `langgraph4j-core` 1.9.9 resolved (`mvn -U compile`) |
