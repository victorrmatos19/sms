package com.erp.repository;

import com.erp.model.CaixaSessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaixaSessaoRepository extends JpaRepository<CaixaSessao, Integer> {
    Optional<CaixaSessao> findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(Integer empresaId,
                                                                                   String status);
}
