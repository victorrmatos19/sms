package com.erp.service;

import com.erp.model.dto.update.UpdateCache;
import com.erp.model.dto.update.UpdateCheckResult;
import com.erp.model.dto.update.UpdateManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UpdateService {

    private static final Pattern SAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._ -]");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String updateUrl;
    private final long checkIntervalDays;
    private final Path cachePath;
    private final String currentVersion;

    @Autowired
    public UpdateService(
            @Value("${sms.update.url}") String updateUrl,
            @Value("${sms.update.check-interval-days:7}") long checkIntervalDays) {
        this(updateUrl, checkIntervalDays, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build(), new ObjectMapper(), defaultCachePath(), loadCurrentVersion());
    }

    UpdateService(String updateUrl,
                  long checkIntervalDays,
                  HttpClient httpClient,
                  ObjectMapper objectMapper,
                  Path cachePath,
                  String currentVersion) {
        this.updateUrl = updateUrl;
        this.checkIntervalDays = checkIntervalDays;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cachePath = cachePath;
        this.currentVersion = currentVersion;
    }

    public Optional<UpdateCheckResult> checkForUpdateIfDue() {
        if (!isUpdateUrlConfigured()) {
            log.info("URL de atualização não configurada. Verificação ignorada.");
            return Optional.empty();
        }

        if (!isCheckDue()) {
            log.info("Verificação de atualização ignorada: intervalo de {} dia(s) ainda não venceu.",
                    checkIntervalDays);
            return Optional.empty();
        }

        try {
            UpdateManifest manifest = fetchManifest();
            saveCache(new UpdateCache(Instant.now().toString(), manifest, null));

            if (isNewerVersion(manifest.latestVersion(), currentVersion)) {
                log.info("Nova versão disponível: atual={} nova={}", currentVersion, manifest.latestVersion());
                return Optional.of(new UpdateCheckResult(currentVersion, manifest));
            }

            log.info("SMS já está atualizado: {}", currentVersion);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Não foi possível verificar atualização: {}", e.getMessage());
            saveCache(new UpdateCache(Instant.now().toString(), loadCache().map(UpdateCache::lastManifest).orElse(null),
                    e.getMessage()));
            return Optional.empty();
        }
    }

    public Path downloadAndValidate(UpdateManifest manifest) throws IOException, InterruptedException {
        if (manifest == null || isBlank(manifest.downloadUrl())) {
            throw new IllegalArgumentException("Manifest de atualização sem URL de download.");
        }
        if (isBlank(manifest.sha256())) {
            throw new IllegalArgumentException("Manifest de atualização sem SHA-256.");
        }

        URI downloadUri = URI.create(manifest.downloadUrl().replace(" ", "%20"));
        Path updateDir = Paths.get(System.getProperty("java.io.tmpdir"), "sms-updates");
        Files.createDirectories(updateDir);

        Path uriFilename = Paths.get(Optional.ofNullable(downloadUri.getPath()).orElse("")).getFileName();
        String filename = uriFilename != null ? uriFilename.toString() : null;
        if (isBlank(filename)) {
            filename = "sms-update-" + manifest.latestVersion() + ".exe";
        }
        filename = SAFE_FILENAME.matcher(filename).replaceAll("_");
        Path destination = updateDir.resolve(filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(downloadUri)
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destination));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download retornou HTTP " + response.statusCode());
        }

        String actualHash = sha256(destination);
        if (!actualHash.equalsIgnoreCase(manifest.sha256().trim())) {
            Files.deleteIfExists(destination);
            throw new IOException("SHA-256 inválido. Esperado " + manifest.sha256() + ", obtido " + actualHash);
        }

        return destination;
    }

    public void scheduleInstallerAfterExit(Path installerPath) throws IOException {
        if (installerPath == null || !Files.exists(installerPath)) {
            throw new IllegalArgumentException("Instalador não encontrado.");
        }

        long pid = ProcessHandle.current().pid();
        String command = "$p=" + pid + ";"
                + "$installer=" + quotePowerShell(installerPath.toAbsolutePath().toString()) + ";"
                + "Wait-Process -Id $p -ErrorAction SilentlyContinue;"
                + "Start-Sleep -Seconds 1;"
                + "Start-Process -FilePath $installer";

        new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command)
                .start();
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    boolean isNewerVersion(String candidate, String current) {
        int[] candidateParts = parseVersion(candidate);
        int[] currentParts = parseVersion(current);
        int max = Math.max(candidateParts.length, currentParts.length);
        for (int i = 0; i < max; i++) {
            int c = i < candidateParts.length ? candidateParts[i] : 0;
            int a = i < currentParts.length ? currentParts[i] : 0;
            if (c != a) {
                return c > a;
            }
        }
        return false;
    }

    private UpdateManifest fetchManifest() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Manifest retornou HTTP " + response.statusCode());
        }

        UpdateManifest manifest = objectMapper.readValue(response.body(), UpdateManifest.class);
        if (isBlank(manifest.latestVersion()) || isBlank(manifest.downloadUrl())) {
            throw new IOException("Manifest de atualização incompleto.");
        }
        return manifest;
    }

    private boolean isCheckDue() {
        if (checkIntervalDays <= 0) {
            return true;
        }

        Optional<UpdateCache> cache = loadCache();
        if (cache.isEmpty() || isBlank(cache.get().lastCheckedAt())) {
            return true;
        }

        try {
            Instant lastCheckedAt = Instant.parse(cache.get().lastCheckedAt());
            return lastCheckedAt.plus(Duration.ofDays(checkIntervalDays)).isBefore(Instant.now());
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private Optional<UpdateCache> loadCache() {
        if (!Files.exists(cachePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cachePath.toFile(), UpdateCache.class));
        } catch (Exception e) {
            log.warn("Cache de atualização inválido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void saveCache(UpdateCache cache) {
        try {
            Files.createDirectories(cachePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(cachePath.toFile(), cache);
        } catch (Exception e) {
            log.warn("Não foi possível salvar cache de atualização: {}", e.getMessage());
        }
    }

    private boolean isUpdateUrlConfigured() {
        return !isBlank(updateUrl) && !updateUrl.contains("seudominio.com.br");
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IOException("Não foi possível calcular SHA-256.", e);
        }
    }

    private int[] parseVersion(String version) {
        if (isBlank(version)) {
            return new int[]{0};
        }
        String normalized = version.trim().replaceFirst("^[vV]", "").split("[-+]")[0];
        String[] parts = normalized.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i].replaceAll("\\D.*$", ""));
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private static Path defaultCachePath() {
        return Paths.get(System.getProperty("user.home"), "erp-desktop", "update-cache.json");
    }

    private static String loadCurrentVersion() {
        try (InputStream input = UpdateService.class.getResourceAsStream("/version.properties")) {
            if (input == null) {
                return "dev";
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("app.version", "dev");
        } catch (IOException e) {
            return "dev";
        }
    }

    private static String quotePowerShell(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
