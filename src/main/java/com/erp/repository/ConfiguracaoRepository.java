package com.erp.repository;

import com.erp.model.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Integer> {

    Optional<Configuracao> findByEmpresaId(Integer empresaId);
}
