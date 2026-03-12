package org.example.chatservice.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KubernetesApiService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesApiService.class);

    private final ObjectMapper objectMapper;
    private final String apiServer;
    private final String namespace;
    private final String tokenFilePath;
    private final String caFilePath;
    private final String inlineBearerToken;

    private volatile HttpClient httpClient;

    public KubernetesApiService(
            ObjectMapper objectMapper,
            @Value("${app.admin.k8s.api-server:https://kubernetes.default.svc}") String apiServer,
            @Value("${app.admin.k8s.namespace:chat-app}") String namespace,
            @Value("${app.admin.k8s.token-file:/var/run/secrets/kubernetes.io/serviceaccount/token}") String tokenFilePath,
            @Value("${app.admin.k8s.ca-file:/var/run/secrets/kubernetes.io/serviceaccount/ca.crt}") String caFilePath,
            @Value("${app.admin.k8s.bearer-token:}") String inlineBearerToken
    ) {
        this.objectMapper = objectMapper;
        this.apiServer = trimTrailingSlash(apiServer);
        this.namespace = namespace;
        this.tokenFilePath = tokenFilePath;
        this.caFilePath = caFilePath;
        this.inlineBearerToken = inlineBearerToken;
    }

    @PostConstruct
    void init() {
        this.httpClient = buildHttpClient();
    }

    public KubernetesSnapshot fetchSnapshot() {
        try {
            String token = readBearerToken()
                    .orElseThrow(() -> new IllegalStateException("Kubernetes API bearer token is not available"));

            JsonNode nodesRoot = callApi("/api/v1/nodes", token);
            JsonNode podsRoot = callApi("/api/v1/namespaces/" + namespace + "/pods", token);

            List<KubernetesNode> nodes = parseNodes(nodesRoot);
            List<KubernetesPod> pods = parsePods(podsRoot);
            Map<String, Long> podPhaseSummary = summarizePodPhases(pods);

            return new KubernetesSnapshot(apiServer, namespace, nodes, pods, podPhaseSummary, null);
        } catch (Exception ex) {
            log.warn("Failed to load Kubernetes metrics from API server: {}", ex.getMessage());
            return new KubernetesSnapshot(
                    apiServer,
                    namespace,
                    List.of(),
                    List.of(),
                    Map.of(),
                    "Kubernetes API query failed: " + ex.getMessage()
            );
        }
    }

    private JsonNode callApi(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiServer + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " for " + path);
        }
        return objectMapper.readTree(response.body());
    }

    private Optional<String> readBearerToken() {
        if (StringUtils.hasText(inlineBearerToken)) {
            return Optional.of(inlineBearerToken.trim());
        }
        try {
            Path tokenPath = Path.of(tokenFilePath);
            if (!Files.exists(tokenPath)) {
                return Optional.empty();
            }
            String token = Files.readString(tokenPath).trim();
            return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
        } catch (Exception ex) {
            log.warn("Unable to read Kubernetes token from {}: {}", tokenFilePath, ex.getMessage());
            return Optional.empty();
        }
    }

    private HttpClient buildHttpClient() {
        try {
            SSLContext sslContext = buildSslContextFromCa();
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5));
            if (sslContext != null) {
                builder.sslContext(sslContext);
            }
            return builder.build();
        } catch (Exception ex) {
            log.warn("Unable to configure Kubernetes API client SSL context: {}. Using default truststore.", ex.getMessage());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }

    private SSLContext buildSslContextFromCa() throws Exception {
        Path caPath = Path.of(caFilePath);
        if (!Files.exists(caPath)) {
            return null;
        }

        byte[] certBytes = Files.readAllBytes(caPath);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        var certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("k8s-ca", certificate);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

    private List<KubernetesNode> parseNodes(JsonNode root) {
        List<KubernetesNode> nodes = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            String name = item.path("metadata").path("name").asText("");
            JsonNode statusNode = item.path("status");
            String kubeletVersion = statusNode.path("nodeInfo").path("kubeletVersion").asText("");
            String internalIp = extractAddress(statusNode.path("addresses"), "InternalIP").orElse("");
            String role = extractNodeRole(item.path("metadata").path("labels"));
            boolean ready = isNodeReady(statusNode.path("conditions"));
            String readyStatus = ready ? "Ready" : "NotReady";
            nodes.add(new KubernetesNode(name, ready, readyStatus, role, kubeletVersion, internalIp));
        }
        nodes.sort(Comparator.comparing(KubernetesNode::name));
        return nodes;
    }

    private List<KubernetesPod> parsePods(JsonNode root) {
        List<KubernetesPod> pods = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            String name = item.path("metadata").path("name").asText("");
            String podNamespace = item.path("metadata").path("namespace").asText("");
            String phase = item.path("status").path("phase").asText("Unknown");
            String nodeName = item.path("spec").path("nodeName").asText("");
            String podIp = item.path("status").path("podIP").asText("");
            String createdAt = item.path("metadata").path("creationTimestamp").asText("");
            String appLabel = item.path("metadata").path("labels").path("app").asText("");

            int totalContainers = 0;
            int readyContainers = 0;
            int restartCount = 0;
            JsonNode containerStatuses = item.path("status").path("containerStatuses");
            if (containerStatuses.isArray()) {
                totalContainers = containerStatuses.size();
                for (JsonNode status : containerStatuses) {
                    if (status.path("ready").asBoolean(false)) {
                        readyContainers += 1;
                    }
                    restartCount += status.path("restartCount").asInt(0);
                }
            }
            String readyText = readyContainers + "/" + totalContainers;

            pods.add(new KubernetesPod(
                    name,
                    podNamespace,
                    phase,
                    readyText,
                    restartCount,
                    nodeName,
                    podIp,
                    appLabel,
                    createdAt
            ));
        }
        pods.sort(Comparator.comparing(KubernetesPod::name));
        return pods;
    }

    private Map<String, Long> summarizePodPhases(List<KubernetesPod> pods) {
        Map<String, Long> summary = pods.stream()
                .collect(Collectors.groupingBy(KubernetesPod::phase, LinkedHashMap::new, Collectors.counting()));
        if (summary.isEmpty()) {
            return Map.of();
        }
        return summary;
    }

    private Optional<String> extractAddress(JsonNode addresses, String type) {
        if (!addresses.isArray()) {
            return Optional.empty();
        }
        for (JsonNode address : addresses) {
            if (type.equals(address.path("type").asText(""))) {
                return Optional.ofNullable(address.path("address").asText(""));
            }
        }
        return Optional.empty();
    }

    private boolean isNodeReady(JsonNode conditions) {
        if (!conditions.isArray()) {
            return false;
        }
        for (JsonNode condition : conditions) {
            if ("Ready".equals(condition.path("type").asText(""))) {
                return "True".equalsIgnoreCase(condition.path("status").asText(""));
            }
        }
        return false;
    }

    private String extractNodeRole(JsonNode labels) {
        if (labels.has("node-role.kubernetes.io/control-plane")) {
            return "control-plane";
        }
        if (labels.has("node-role.kubernetes.io/master")) {
            return "master";
        }
        if (labels.has("node-role.kubernetes.io/worker")) {
            return "worker";
        }
        String role = labels.path("kubernetes.io/role").asText("");
        return StringUtils.hasText(role) ? role : "worker";
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record KubernetesSnapshot(
            String apiServer,
            String namespace,
            List<KubernetesNode> nodes,
            List<KubernetesPod> pods,
            Map<String, Long> podPhaseSummary,
            String errorMessage
    ) {}

    public record KubernetesNode(
            String name,
            boolean ready,
            String status,
            String role,
            String kubeletVersion,
            String internalIp
    ) {}

    public record KubernetesPod(
            String name,
            String namespace,
            String phase,
            String ready,
            int restartCount,
            String nodeName,
            String podIp,
            String app,
            String createdAt
    ) {}
}
