package com.erp.repository;

import com.erp.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {

    List<MovimentacaoEstoque> findByEmpresaIdOrderByCriadoEmDesc(Integer empresaId);

    List<MovimentacaoEstoque> findByEmpresaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            Integer empresaId, LocalDateTime inicio, LocalDateTime fim);

    long countByEmpresaIdAndTipoAndCriadoEmBetween(
            Integer empresaId, String tipo, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT m FROM MovimentacaoEstoque m " +
           "LEFT JOIN FETCH m.produto " +
           "WHERE m.empresa.id = :empresaId AND m.produto.id = :produtoId " +
           "ORDER BY m.criadoEm DESC")
    List<MovimentacaoEstoque> findByEmpresaIdAndProdutoIdOrderByCriadoEmDesc(
            @Param("empresaId") Integer empresaId,
            @Param("produtoId") Integer produtoId);
}
