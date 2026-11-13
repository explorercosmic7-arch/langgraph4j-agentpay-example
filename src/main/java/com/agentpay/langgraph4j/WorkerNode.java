package com.agentpay.langgraph4j;

import org.bsc.langgraph4j.action.NodeAction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Node 2 — Worker
 * Simulates subtask work (no LLM required for this skeleton).
 * Set AGENTPAY_FORCE_FAIL=true to exercise the refund path.
 */
public class WorkerNode implements NodeAction<PayPerTaskState> {

  @Override
  public Map<String, Object> apply(PayPerTaskState state) throws Exception {
    Map<String, Object> out = new HashMap<>();
    boolean forceFail = "true".equalsIgnoreCase(System.getenv().getOrDefault("AGENTPAY_FORCE_FAIL", "false"));

    if (forceFail) {
      out.put(PayPerTaskState.WORK_OK, false);
      out.put(PayPerTaskState.OUTPUT, "");
      out.put(PayPerTaskState.OUTPUT_HASH, "");
      out.put(PayPerTaskState.STATUS, "work_failed");
      out.put(PayPerTaskState.LOG, "worker: forced failure (AGENTPAY_FORCE_FAIL)");
      System.out.println("[worker] forced failure → settlement will refund");
      return out;
    }

    String output = "Completed task: " + state.task() + " by " + state.workerRepo();
    String hash = sha256(output);

    out.put(PayPerTaskState.WORK_OK, true);
    out.put(PayPerTaskState.OUTPUT, output);
    out.put(PayPerTaskState.OUTPUT_HASH, hash);
    out.put(PayPerTaskState.STATUS, "worked");
    out.put(PayPerTaskState.LOG, "worker: ok hash=" + hash);
    System.out.println("[worker] done hash=" + hash);
    return out;
  }

  private static String sha256(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(dig);
  }
}
