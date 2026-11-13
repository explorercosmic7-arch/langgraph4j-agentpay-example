package com.agentpay.langgraph4j;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;

import java.util.HashMap;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Pay-per-task multi-actor graph settled on AgentPay.
 *
 * <pre>
 *   START → coordinator → worker → settlement → END
 *             │              │           │
 *          passport+      fake work   release
 *          escrow hold    + hash      or refund
 * </pre>
 *
 * Run:
 *   export AGENTPAY_API_KEY=a2a_...
 *   export AGENTPAY_BUYER_REPO=swarm/bot-1
 *   export AGENTPAY_WORKER_REPO=swarm/bot-2
 *   mvn -q exec:java
 */
public class PayPerTaskGraphApp {

  public static void main(String[] args) throws Exception {
    AgentPayClient client = AgentPayClient.fromEnv();

    String buyer = env("AGENTPAY_BUYER_REPO", "swarm/bot-1");
    String worker = env("AGENTPAY_WORKER_REPO", "swarm/bot-2");
    double amount = Double.parseDouble(env("AGENTPAY_AMOUNT", "0.05"));
    String task = env("AGENTPAY_TASK", "research-summary");

    System.out.println("=== AgentPay × LangGraph4j — pay-per-task ===");
    System.out.println("buyer=" + buyer + " worker=" + worker + " amount=" + amount);

    var coordinator = new CoordinatorNode(client);
    var workerNode = new WorkerNode();
    var settlement = new SettlementNode(client);

    StateGraph<PayPerTaskState> graph = new StateGraph<>(PayPerTaskState.SCHEMA, PayPerTaskState::new)
        .addNode("coordinator", node_async(coordinator))
        .addNode("worker", node_async(workerNode))
        .addNode("settlement", node_async(settlement))
        .addEdge(START, "coordinator")
        .addEdge("coordinator", "worker")
        .addEdge("worker", "settlement")
        .addEdge("settlement", END);

    CompiledGraph<PayPerTaskState> compiled = graph.compile();

    Map<String, Object> input = new HashMap<>();
    input.put(PayPerTaskState.TASK, task);
    input.put(PayPerTaskState.BUYER_REPO, buyer);
    input.put(PayPerTaskState.WORKER_REPO, worker);
    input.put(PayPerTaskState.AMOUNT, amount);

    var finalState = compiled.invoke(input);
    if (finalState.isEmpty()) {
      System.err.println("Graph returned empty state");
      System.exit(2);
    }

    PayPerTaskState s = finalState.get();
    System.out.println();
    System.out.println("=== FINAL STATE ===");
    System.out.println("status      = " + s.status());
    System.out.println("escrow_id   = " + s.escrowId());
    System.out.println("work_ok     = " + s.workOk());
    System.out.println("output_hash = " + s.value(PayPerTaskState.OUTPUT_HASH).orElse(""));
    System.out.println("settlement  = " + s.value(PayPerTaskState.SETTLEMENT).orElse(""));
    System.out.println("log         = " + s.value(PayPerTaskState.LOG).orElse(""));
    System.out.println();
    System.out.println("Done. Check balances / ledger on AgentPay dashboard or lab.");
  }

  private static String env(String k, String def) {
    String v = System.getenv(k);
    return (v == null || v.isBlank()) ? def : v.trim();
  }
}
