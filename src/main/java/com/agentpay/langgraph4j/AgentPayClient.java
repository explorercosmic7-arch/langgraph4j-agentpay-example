package com.agentpay.langgraph4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Thin AgentPay REST client (Java 11+ HttpClient).
 * No SDK required — pure JSON over HTTPS.
 *
 * Auth: Authorization: Bearer a2a_...
 * Docs: https://edgerail.pages.dev/docs
 */
public final class AgentPayClient {

  private final String baseUrl;
  private final String apiKey;
  private final HttpClient http;
  private final ObjectMapper mapper;

  public AgentPayClient(String baseUrl, String apiKey) {
    this.baseUrl = trimSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
    this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
    if (!apiKey.startsWith("a2a_")) {
      System.err.println("Warning: AgentPay keys usually start with a2a_");
    }
    this.http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();
    this.mapper = new ObjectMapper();
  }

  public static AgentPayClient fromEnv() {
    String base = env("AGENTPAY_BASE_URL", "https://autonomous-edge-agent-economy.explorercosmic7.workers.dev");
    String key = env("AGENTPAY_API_KEY", null);
    if (key == null || key.isBlank() || key.contains("YOUR_KEY")) {
      throw new IllegalStateException(
          "Set AGENTPAY_API_KEY to a real key from the AgentPay dashboard (API keys).");
    }
    return new AgentPayClient(base, key);
  }

  /** GET /api/passport?repo=owner/repo or path variant — best-effort. */
  public JsonNode passport(String repo) throws IOException, InterruptedException {
    String r = requireRepo(repo);
    // Try query style first
    HttpResponse<String> res = get("/api/passport?repo=" + encode(r));
    if (res.statusCode() == 404 || res.statusCode() == 405) {
      String[] parts = r.split("/", 2);
      if (parts.length == 2) {
        res = get("/api/passport/" + encode(parts[0]) + "/" + encode(parts[1]));
      }
    }
    return parse(res, "passport");
  }

  /**
   * POST /api/escrow/hold
   * Body: from_repo, to_repo, amount, task, ttl_seconds (optional)
   */
  public JsonNode escrowHold(String fromRepo, String toRepo, double amount, String task)
      throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("from_repo", requireRepo(fromRepo));
    body.put("to_repo", requireRepo(toRepo));
    body.put("amount", amount);
    body.put("task", task == null ? "langgraph4j-pay-per-task" : task);
    body.put("ttl_seconds", 3600);
    return parse(post("/api/escrow/hold", body), "escrow/hold");
  }

  /** POST /api/escrow/release  { id } */
  public JsonNode escrowRelease(String escrowId) throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("id", Objects.requireNonNull(escrowId, "escrowId"));
    return parse(post("/api/escrow/release", body), "escrow/release");
  }

  /** POST /api/escrow/refund  { id } */
  public JsonNode escrowRefund(String escrowId) throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("id", Objects.requireNonNull(escrowId, "escrowId"));
    return parse(post("/api/escrow/refund", body), "escrow/refund");
  }

  /** POST /api/pay — optional direct pay (not required for escrow graph). */
  public JsonNode pay(String fromRepo, String toRepo, double amount, String task)
      throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("from_repo", requireRepo(fromRepo));
    body.put("to_repo", requireRepo(toRepo));
    body.put("amount", amount);
    body.put("task", task == null ? "langgraph4j-pay" : task);
    return parse(post("/api/pay", body), "pay");
  }

  /** GET /api/balance?repo= */
  public JsonNode balance(String repo) throws IOException, InterruptedException {
    return parse(get("/api/balance?repo=" + encode(requireRepo(repo))), "balance");
  }

  // ── HTTP helpers ──────────────────────────────────────────────

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + path))
        .timeout(Duration.ofSeconds(60))
        .header("Authorization", "Bearer " + apiKey)
        .header("Accept", "application/json")
        .GET()
        .build();
    return http.send(req, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, ObjectNode body)
      throws IOException, InterruptedException {
    String json = mapper.writeValueAsString(body);
    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + path))
        .timeout(Duration.ofSeconds(60))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
    return http.send(req, HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode parse(HttpResponse<String> res, String label) throws IOException {
    String raw = res.body() == null ? "" : res.body();
    JsonNode node;
    try {
      node = mapper.readTree(raw.isEmpty() ? "{}" : raw);
    } catch (Exception e) {
      throw new IOException(label + " non-JSON HTTP " + res.statusCode() + ": " + raw, e);
    }
    if (res.statusCode() >= 400) {
      String err = node.has("error") ? node.get("error").asText() : raw;
      String code = node.has("code") ? node.get("code").asText() : String.valueOf(res.statusCode());
      throw new IOException(label + " failed HTTP " + res.statusCode() + " [" + code + "]: " + err);
    }
    return node;
  }

  /** Extract escrow id from hold response (several possible shapes). */
  public static String extractEscrowId(JsonNode holdResponse) {
    if (holdResponse == null) return null;
    if (holdResponse.hasNonNull("id")) return holdResponse.get("id").asText();
    if (holdResponse.has("escrow") && holdResponse.get("escrow").hasNonNull("id")) {
      return holdResponse.get("escrow").get("id").asText();
    }
    if (holdResponse.hasNonNull("escrow_id")) return holdResponse.get("escrow_id").asText();
    return null;
  }

  private static String requireRepo(String repo) {
    if (repo == null || !repo.contains("/")) {
      throw new IllegalArgumentException("repo must be owner/repo, got: " + repo);
    }
    return repo.trim();
  }

  private static String trimSlash(String u) {
    while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
    return u;
  }

  private static String encode(String s) {
    return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String env(String k, String def) {
    String v = System.getenv(k);
    if (v == null || v.isBlank()) return def;
    return v.trim();
  }
}
