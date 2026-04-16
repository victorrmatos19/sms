package com.erp.service;

import com.erp.model.Configuracao;
import com.erp.model.Empresa;
import com.erp.repository.ConfiguracaoRepository;
import com.erp.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public Configuracao buscarPorEmpresa(Integer empresaId) {
        return buscarConfiguracao(empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Configuração não encontrada para empresa: " + empresaId));
    }

    @Transactional(readOnly = true)
    public Optional<Empresa> buscarEmpresa(Integer empresaId) {
        return empresaRepository.findById(empresaId);
    }

    @Transactional
    public Empresa salvarEmpresa(Empresa empresa) {
        Empresa salva = empresaRepository.save(empresa);
        log.info("Empresa id={} atualizada", salva.getId());
        return salva;
    }

    @Transactional
    public Optional<Configuracao> buscarConfiguracao(Integer empresaId) {
        return configuracaoRepository.findByEmpresaId(empresaId)
            .or(() -> {
                Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Empresa não encontrada: " + empresaId));
                Configuracao nova = Configuracao.builder()
                    .empresa(empresa)
                    .build();
                log.info("Criando configuração padrão para empresa id={}", empresaId);
                return Optional.of(configuracaoRepository.save(nova));
            });
    }

    @Transactional
    public Configuracao salvarConfiguracao(Configuracao config) {
        Configuracao salva = configuracaoRepository.save(config);
        Integer empresaId = salva.getEmpresa() != null ? salva.getEmpresa().getId() : null;
        log.info("Configuração id={} salva para empresa id={}", salva.getId(), empresaId);
        return salva;
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
