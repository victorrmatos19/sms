package com.erp.repository;

import com.erp.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {

    // JOIN FETCH produto e usuario em todas as queries usadas pela UI para evitar LazyInitializationException

    @Query("SELECT m FROM MovimentacaoEstoque m " +
           "JOIN FETCH m.produto " +
           "LEFT JOIN FETCH m.usuario " +
           "WHERE m.empresa.id = :empresaId " +
           "ORDER BY m.criadoEm DESC")
    List<MovimentacaoEstoque> findByEmpresaIdOrderByCriadoEmDesc(@Param("empresaId") Integer empresaId);

    @Query("SELECT m FROM MovimentacaoEstoque m " +
           "JOIN FETCH m.produto " +
           "LEFT JOIN FETCH m.usuario " +
           "WHERE m.empresa.id = :empresaId " +
           "AND m.criadoEm BETWEEN :inicio AND :fim " +
           "ORDER BY m.criadoEm DESC")
    List<MovimentacaoEstoque> findByEmpresaIdAndCriadoEmBetweenOrderByCriadoEmDesc(
            @Param("empresaId") Integer empresaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    long countByEmpresaIdAndTipoAndCriadoEmBetween(
            Integer empresaId, String tipo, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT m FROM MovimentacaoEstoque m " +
           "JOIN FETCH m.produto " +
           "LEFT JOIN FETCH m.usuario " +
           "WHERE m.empresa.id = :empresaId " +
           "AND m.produto.id = :produtoId " +
           "ORDER BY m.criadoEm DESC")
    List<MovimentacaoEstoque> findByEmpresaIdAndProdutoIdOrderByCriadoEmDesc(
            @Param("empresaId") Integer empresaId,
            @Param("produtoId") Integer produtoId);

    @Query("SELECT m FROM MovimentacaoEstoque m " +
           "JOIN FETCH m.produto " +
           "LEFT JOIN FETCH m.usuario " +
           "WHERE m.empresa.id = :empresaId " +
           "AND m.tipo = :tipo " +
           "AND m.criadoEm BETWEEN :inicio AND :fim " +
           "ORDER BY m.criadoEm DESC")
    List<MovimentacaoEstoque> findByEmpresaIdAndTipoAndPeriodo(
            @Param("empresaId") Integer empresaId,
            @Param("tipo") String tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
