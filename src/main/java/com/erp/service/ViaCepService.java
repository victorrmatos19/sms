package com.erp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integração com a API pública ViaCEP para preenchimento automático de endereço.
 * Usa java.net.http.HttpClient (nativo Java 11+) sem dependências externas.
 * Deve ser chamado via JavaFX Task<> para não bloquear a thread da UI.
 */
@Slf4j
@Service
public class ViaCepService {

    /** Campos mapeados da resposta ViaCEP. */
    public record EnderecoViaCep(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String uf
    ) {}

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    /**
     * Busca dados de endereço pelo CEP informado.
     * Remove formatação antes da consulta.
     *
     * @param cep CEP com ou sem máscara (8 dígitos)
     * @return Optional com os dados preenchidos, ou empty se não encontrado / offline
     */
    public Optional<EnderecoViaCep> buscarCep(String cep) {
        if (cep == null) return Optional.empty();
        String cepLimpo = cep.replaceAll("\\D", "");
        if (cepLimpo.length() != 8) return Optional.empty();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://viacep.com.br/ws/" + cepLimpo + "/json/"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("ViaCEP retornou status {} para CEP {}", response.statusCode(), cepLimpo);
                return Optional.empty();
            }

            String body = response.body();

            // "erro": true é retornado quando o CEP não existe
            if (body.contains("\"erro\"")) {
                log.info("CEP não encontrado: {}", cepLimpo);
                return Optional.empty();
            }

            EnderecoViaCep endereco = new EnderecoViaCep(
                extrairCampo(body, "cep"),
                extrairCampo(body, "logradouro"),
                extrairCampo(body, "bairro"),
                extrairCampo(body, "localidade"),
                extrairCampo(body, "uf")
            );

            log.info("CEP {} encontrado: {} - {}/{}", cepLimpo,
                endereco.logradouro(), endereco.cidade(), endereco.uf());
            return Optional.of(endereco);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Busca de CEP interrompida: {}", cepLimpo);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Erro ao buscar CEP {}: {}", cepLimpo, e.getMessage());
            return Optional.empty();
        }
    }

    /** Extrai o valor de um campo JSON simples (string). */
    private String extrairCampo(String json, String campo) {
        Pattern p = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }
}
