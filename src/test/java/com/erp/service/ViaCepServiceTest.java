package com.erp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do ViaCepService.
 * Cobre CF-58, CF-59, CF-60.
 * Usa o construtor package-private que aceita um HttpClient mockado.
 */
@ExtendWith(MockitoExtension.class)
class ViaCepServiceTest {

    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private ViaCepService viaCepService;

    @BeforeEach
    void setUp() {
        viaCepService = new ViaCepService(httpClient);
    }

    // ---- CF-58: CEP encontrado ----

    @Test
    @SuppressWarnings("unchecked")
    void dado_cep_valido_quando_buscar_entao_retorna_endereco_preenchido() throws Exception {
        String json = """
            {
              "cep": "01310-100",
              "logradouro": "Avenida Paulista",
              "complemento": "de 610 a 1510 - lado par",
              "bairro": "Bela Vista",
              "localidade": "São Paulo",
              "uf": "SP",
              "ibge": "3550308"
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("01310-100");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().logradouro()).isEqualTo("Avenida Paulista");
        assertThat(resultado.get().bairro()).isEqualTo("Bela Vista");
        assertThat(resultado.get().cidade()).isEqualTo("São Paulo");
        assertThat(resultado.get().uf()).isEqualTo("SP");
        assertThat(resultado.get().cep()).isEqualTo("01310-100");
    }

    @Test
    @SuppressWarnings("unchecked")
    void dado_cep_sem_mascara_quando_buscar_entao_retorna_endereco() throws Exception {
        String json = """
            {
              "cep": "01310-100",
              "logradouro": "Avenida Paulista",
              "bairro": "Bela Vista",
              "localidade": "São Paulo",
              "uf": "SP"
            }
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("01310100");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().uf()).isEqualTo("SP");
    }

    // ---- CF-59: CEP não encontrado (campo "erro" na resposta) ----

    @Test
    @SuppressWarnings("unchecked")
    void dado_cep_inexistente_quando_buscar_entao_retorna_empty() throws Exception {
        String json = "{\"erro\": true}";

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("00000000");

        assertThat(resultado).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void dado_resposta_com_status_nao_200_quando_buscar_entao_retorna_empty() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("99999999");

        assertThat(resultado).isEmpty();
    }

    // ---- CF-60: Sem conexão / timeout — não propaga exceção ----

    @Test
    @SuppressWarnings("unchecked")
    void dado_sem_conexao_quando_buscar_entao_retorna_empty_sem_lancar_excecao() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Connection refused"));

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("01310100");

        assertThat(resultado).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void dado_timeout_quando_buscar_entao_retorna_empty_sem_lancar_excecao() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new java.net.http.HttpTimeoutException("timed out"));

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("01310100");

        assertThat(resultado).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void dado_interrupcao_quando_buscar_entao_retorna_empty_e_flag_mantida() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new InterruptedException("interrupted"));

        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("01310100");

        assertThat(resultado).isEmpty();
        // Interrupt flag deve ter sido restaurado
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted(); // limpa para não poluir outros testes
    }

    // ---- Entradas inválidas (sem chegar na rede) ----

    @Test
    void dado_cep_nulo_quando_buscar_entao_retorna_empty_sem_chamar_http() {
        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep(null);

        assertThat(resultado).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void dado_cep_incompleto_quando_buscar_entao_retorna_empty_sem_chamar_http() {
        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("1234");

        assertThat(resultado).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void dado_cep_vazio_quando_buscar_entao_retorna_empty_sem_chamar_http() {
        Optional<ViaCepService.EnderecoViaCep> resultado = viaCepService.buscarCep("");

        assertThat(resultado).isEmpty();
        verifyNoInteractions(httpClient);
    }
}
