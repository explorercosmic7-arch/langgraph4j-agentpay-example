package com.agentpay.langgraph4j;

import com.fasterxml.jackson.databind.JsonNode;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * Node 1 — Coordinator
 * Optional passport check on worker, then locks budget via escrow hold.
 */
public class CoordinatorNode implements NodeAction<PayPerTaskState> {

  private final AgentPayClient client;

  public CoordinatorNode(AgentPayClient client) {
    this.client = client;
  }

  @Override
  public Map<String, Object> apply(PayPerTaskState state) throws Exception {
    Map<String, Object> out = new HashMap<>();
    String log = "coordinator: start";

    boolean passportOk = true;
    try {
      JsonNode p = client.passport(state.workerRepo());
      passportOk = !p.has("ok") || p.get("ok").asBoolean(true);
      log += " | passport ok=" + passportOk;
    } catch (Exception e) {
      // Passport is soft in v0 — do not block settlement demo if route differs
      log += " | passport skipped (" + e.getMessage() + ")";
      passportOk = true;
    }
    out.put(PayPerTaskState.PASSPORT_OK, passportOk);

    JsonNode hold = client.escrowHold(
        state.buyerRepo(),
        state.workerRepo(),
        state.amount(),
        "lg4j:" + state.task()
    );
    String escrowId = AgentPayClient.extractEscrowId(hold);
    if (escrowId == null || escrowId.isBlank()) {
      throw new IllegalStateException("escrow hold returned no id: " + hold);
    }

    out.put(PayPerTaskState.ESCROW_ID, escrowId);
    out.put(PayPerTaskState.STATUS, "held");
    out.put(PayPerTaskState.LOG, log + " | held escrow_id=" + escrowId);
    System.out.println("[coordinator] escrow held id=" + escrowId + " amount=" + state.amount());
    return out;
  }
}
