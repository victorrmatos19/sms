package com.erp.service;

import com.erp.model.dto.update.UpdateCache;
import com.erp.model.dto.update.UpdateCheckResult;
import com.erp.model.dto.update.UpdateManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLSession;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateServiceTest {

    private final UpdateService service = new UpdateService(
            "https://example.com/update.json",
            7,
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            Path.of("target", "test-update-cache.json"),
            "1.0.1"
    );

    @Test
    void comparaVersoesSemanticas() {
        assertThat(service.isNewerVersion("1.0.2", "1.0.1")).isTrue();
        assertThat(service.isNewerVersion("1.0.10", "1.0.2")).isTrue();
        assertThat(service.isNewerVersion("1.1.0", "1.0.99")).isTrue();
        assertThat(service.isNewerVersion("2.0.0", "1.99.99")).isTrue();
    }

    @Test
    void naoConsideraVersaoIgualOuAntigaComoAtualizacao() {
        assertThat(service.isNewerVersion("1.0.1", "1.0.1")).isFalse();
        assertThat(service.isNewerVersion("1.0.0", "1.0.1")).isFalse();
        assertThat(service.isNewerVersion("1.0", "1.0.0")).isFalse();
    }

    @Test
    void ignoraPrefixoVEMetadadosDaVersao() {
        assertThat(service.isNewerVersion("v1.0.2", "1.0.1")).isTrue();
        assertThat(service.isNewerVersion("1.0.2-beta", "1.0.1")).isTrue();
        assertThat(service.isNewerVersion("1.0.1+build5", "1.0.1")).isFalse();
    }

    @Test
    void escolheExtensaoPadraoDoInstaladorPorSistemaOperacional() {
        assertThat(UpdateService.defaultInstallerFilename("1.0.6", "Windows 11"))
                .isEqualTo("sms-update-1.0.6.exe");
        assertThat(UpdateService.defaultInstallerFilename("1.0.6", "Mac OS X"))
                .isEqualTo("sms-update-1.0.6.dmg");
        assertThat(UpdateService.defaultInstallerFilename("1.0.6", "Linux"))
                .isEqualTo("sms-update-1.0.6.bin");
    }

    @Test
    void checkForUpdateIfDueRespeitaCacheRecente() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path cachePath = Path.of("target", "test-update-cache-if-due.json");
        Files.createDirectories(cachePath.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(cachePath.toFile(),
                new UpdateCache(Instant.now().toString(), manifest("1.0.2"), null));
        FakeHttpClient httpClient = new FakeHttpClient(manifestJson("1.0.3"));
        UpdateService cachedService = new UpdateService(
                "https://example.com/update.json",
                7,
                httpClient,
                objectMapper,
                cachePath,
                "1.0.1"
        );

        Optional<UpdateCheckResult> result = cachedService.checkForUpdateIfDue();

        assertThat(result).isEmpty();
        assertThat(httpClient.sendCount()).isZero();
    }

    @Test
    void checkForUpdateNowIgnoraCacheRecenteEConsultaManifest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Path cachePath = Path.of("target", "test-update-cache-now.json");
        Files.createDirectories(cachePath.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(cachePath.toFile(),
                new UpdateCache(Instant.now().toString(), manifest("1.0.1"), null));
        FakeHttpClient httpClient = new FakeHttpClient(manifestJson("1.0.2"));
        UpdateService cachedService = new UpdateService(
                "https://example.com/update.json",
                7,
                httpClient,
                objectMapper,
                cachePath,
                "1.0.1"
        );

        Optional<UpdateCheckResult> result = cachedService.checkForUpdateNow();

        assertThat(result).isPresent();
        assertThat(result.get().manifest().latestVersion()).isEqualTo("1.0.2");
        assertThat(httpClient.sendCount()).isEqualTo(1);
    }

    @Test
    void checkForUpdateNowRetornaVazioQuandoJaEstaAtualizado() throws Exception {
        FakeHttpClient httpClient = new FakeHttpClient(manifestJson("1.0.1"));
        UpdateService upToDateService = new UpdateService(
                "https://example.com/update.json",
                7,
                httpClient,
                new ObjectMapper(),
                Path.of("target", "test-update-cache-up-to-date.json"),
                "1.0.1"
        );

        Optional<UpdateCheckResult> result = upToDateService.checkForUpdateNow();

        assertThat(result).isEmpty();
        assertThat(httpClient.sendCount()).isEqualTo(1);
    }

    private static UpdateManifest manifest(String version) {
        return new UpdateManifest(
                version,
                "https://example.com/sms.exe",
                "abc123",
                "Notas",
                false,
                "2026-04-17"
        );
    }

    private static String manifestJson(String version) {
        return """
                {
                  "latestVersion": "%s",
                  "downloadUrl": "https://example.com/sms.exe",
                  "sha256": "abc123",
                  "releaseNotes": "Notas",
                  "mandatory": false,
                  "publishedAt": "2026-04-17"
                }
                """.formatted(version);
    }

    private static final class FakeHttpClient extends HttpClient {
        private final String body;
        private int sendCount;

        private FakeHttpClient(String body) {
            this.body = body;
        }

        int sendCount() {
            return sendCount;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NORMAL;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            sendCount++;
            HttpResponse.ResponseInfo responseInfo = new HttpResponse.ResponseInfo() {
                @Override
                public int statusCode() {
                    return 200;
                }

                @Override
                public HttpHeaders headers() {
                    return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
                }

                @Override
                public Version version() {
                    return Version.HTTP_2;
                }
            };
            HttpResponse.BodySubscriber<T> subscriber = responseBodyHandler.apply(responseInfo);
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onNext(java.util.List.of(java.nio.ByteBuffer.wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            subscriber.onComplete();
            return new FakeHttpResponse<>(request, subscriber.getBody().toCompletableFuture().join());
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }

    private record FakeHttpResponse<T>(HttpRequest request, T body) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
