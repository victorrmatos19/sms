package com.erp.repository;

import com.erp.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {

    List<MovimentacaoEstoque> findByEmpresaIdOrderByCriadoEmDesc(Integer empresaId);

    List<MovimentacaoEstoque> findByEmpresaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Integer empresaId, LocalDateTime inicio, LocalDateTime fim);
}
