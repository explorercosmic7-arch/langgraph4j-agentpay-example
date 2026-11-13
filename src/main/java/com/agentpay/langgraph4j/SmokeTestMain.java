package com.agentpay.langgraph4j;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * No LangGraph — only AgentPay REST.
 * Use this first to verify API key + escrow before running the graph.
 *
 *   mvn -q exec:java -Dexec.mainClass=com.agentpay.langgraph4j.SmokeTestMain
 */
public class SmokeTestMain {

  public static void main(String[] args) throws Exception {
    AgentPayClient client = AgentPayClient.fromEnv();
    String buyer = System.getenv().getOrDefault("AGENTPAY_BUYER_REPO", "swarm/bot-1");
    String worker = System.getenv().getOrDefault("AGENTPAY_WORKER_REPO", "swarm/bot-2");
    double amount = Double.parseDouble(System.getenv().getOrDefault("AGENTPAY_AMOUNT", "0.05"));

    System.out.println("Smoke: balance buyer");
    System.out.println(client.balance(buyer));

    System.out.println("Smoke: escrow hold");
    JsonNode hold = client.escrowHold(buyer, worker, amount, "smoke-hold");
    System.out.println(hold);
    String id = AgentPayClient.extractEscrowId(hold);
    System.out.println("escrow_id=" + id);

    System.out.println("Smoke: escrow release");
    System.out.println(client.escrowRelease(id));
    System.out.println("OK — REST path works. Next: PayPerTaskGraphApp");
  }
}
