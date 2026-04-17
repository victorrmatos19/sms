package com.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.nio.file.Path;

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
}
