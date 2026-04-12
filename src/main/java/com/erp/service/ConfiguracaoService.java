package com.erp.service;

import com.erp.model.Configuracao;
import com.erp.model.Empresa;
import com.erp.repository.ConfiguracaoRepository;
import com.erp.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public Configuracao buscarPorEmpresa(Integer empresaId) {
        return configuracaoRepository.findByEmpresaId(empresaId)
            .orElseGet(() -> {
                Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Empresa não encontrada: " + empresaId));
                Configuracao nova = Configuracao.builder()
                    .empresa(empresa)
                    .build();
                log.info("Criando configuração padrão para empresa id={}", empresaId);
                return configuracaoRepository.save(nova);
            });
    }

    @Transactional
    public void salvarTema(Integer empresaId, String tema) {
        Configuracao config = buscarPorEmpresa(empresaId);
        config.setTema(tema);
        configuracaoRepository.save(config);
        log.info("Tema '{}' salvo para empresa id={}", tema, empresaId);
    }

    @Transactional(readOnly = true)
    public boolean isModoEscuro(Integer empresaId) {
        return configuracaoRepository.findByEmpresaId(empresaId)
            .map(c -> "DARK".equals(c.getTema()))
            .orElse(true); // padrão é dark
    }
}
