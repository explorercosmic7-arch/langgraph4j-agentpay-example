package com.agentpay.langgraph4j;

import com.fasterxml.jackson.databind.JsonNode;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;

/**
 * Node 3 — Verifier / Settlement
 * work_ok → escrow release; else → escrow refund.
 */
public class SettlementNode implements NodeAction<PayPerTaskState> {

  private final AgentPayClient client;

  public SettlementNode(AgentPayClient client) {
    this.client = client;
  }

  @Override
  public Map<String, Object> apply(PayPerTaskState state) throws Exception {
    Map<String, Object> out = new HashMap<>();
    String escrowId = state.escrowId();
    if (escrowId == null || escrowId.isBlank()) {
      out.put(PayPerTaskState.STATUS, "error");
      out.put(PayPerTaskState.ERROR, "missing escrow_id");
      out.put(PayPerTaskState.SETTLEMENT, "none");
      return out;
    }

    if (state.workOk()) {
      JsonNode rel = client.escrowRelease(escrowId);
      out.put(PayPerTaskState.SETTLEMENT, "released");
      out.put(PayPerTaskState.STATUS, "settled");
      out.put(PayPerTaskState.LOG, "settlement: released " + escrowId + " → " + rel);
      System.out.println("[settlement] RELEASED escrow " + escrowId);
    } else {
      JsonNode ref = client.escrowRefund(escrowId);
      out.put(PayPerTaskState.SETTLEMENT, "refunded");
      out.put(PayPerTaskState.STATUS, "refunded");
      out.put(PayPerTaskState.LOG, "settlement: refunded " + escrowId + " → " + ref);
      System.out.println("[settlement] REFUNDED escrow " + escrowId);
    }
    return out;
  }
}
