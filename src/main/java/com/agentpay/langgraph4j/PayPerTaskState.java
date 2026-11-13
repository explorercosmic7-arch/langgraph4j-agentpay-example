package com.agentpay.langgraph4j;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared graph state for the pay-per-task workflow.
 *
 * Keys flow through: coordinator → worker → settlement.
 */
public class PayPerTaskState extends AgentState {

  public static final String TASK = "task";
  public static final String BUYER_REPO = "buyer_repo";
  public static final String WORKER_REPO = "worker_repo";
  public static final String AMOUNT = "amount";
  public static final String ESCROW_ID = "escrow_id";
  public static final String PASSPORT_OK = "passport_ok";
  public static final String OUTPUT = "output";
  public static final String OUTPUT_HASH = "output_hash";
  public static final String WORK_OK = "work_ok";
  public static final String SETTLEMENT = "settlement";
  public static final String STATUS = "status";
  public static final String ERROR = "error";
  public static final String LOG = "log";

  public static final Map<String, Channel<?>> SCHEMA = createSchema();

  private static Map<String, Channel<?>> createSchema() {
    Map<String, Channel<?>> m = new HashMap<>();
    m.put(TASK, Channels.base(() -> ""));
    m.put(BUYER_REPO, Channels.base(() -> ""));
    m.put(WORKER_REPO, Channels.base(() -> ""));
    m.put(AMOUNT, Channels.base(() -> 0.05d));
    m.put(ESCROW_ID, Channels.base(() -> ""));
    m.put(PASSPORT_OK, Channels.base(() -> false));
    m.put(OUTPUT, Channels.base(() -> ""));
    m.put(OUTPUT_HASH, Channels.base(() -> ""));
    m.put(WORK_OK, Channels.base(() -> false));
    m.put(SETTLEMENT, Channels.base(() -> ""));
    m.put(STATUS, Channels.base(() -> "init"));
    m.put(ERROR, Channels.base(() -> ""));
    m.put(LOG, Channels.base(() -> ""));
    return Map.copyOf(m);
  }

  public PayPerTaskState(Map<String, Object> initData) {
    super(initData);
  }

  public String task() {
    return str(TASK);
  }

  public String buyerRepo() {
    return str(BUYER_REPO);
  }

  public String workerRepo() {
    return str(WORKER_REPO);
  }

  public double amount() {
    Object v = value(AMOUNT).orElse(0.05d);
    if (v instanceof Number n) return n.doubleValue();
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (Exception e) {
      return 0.05d;
    }
  }

  public String escrowId() {
    return str(ESCROW_ID);
  }

  public boolean workOk() {
    Object v = value(WORK_OK).orElse(false);
    if (v instanceof Boolean b) return b;
    return Boolean.parseBoolean(String.valueOf(v));
  }

  public String status() {
    return str(STATUS);
  }

  private String str(String key) {
    Optional<Object> o = value(key);
    return o.map(String::valueOf).orElse("");
  }
}
