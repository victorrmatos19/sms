package com.erp.repository;

import com.erp.model.ContaPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaPagarRepository extends JpaRepository<ContaPagar, Integer> {

    long countByEmpresaIdAndStatus(Integer empresaId, String status);

    List<ContaPagar> findByEmpresaIdAndStatusOrderByDataVencimentoAsc(Integer empresaId, String status);

    @Query("""
            SELECT cp FROM ContaPagar cp
            LEFT JOIN FETCH cp.fornecedor
            LEFT JOIN FETCH cp.compra
            LEFT JOIN FETCH cp.usuario
            WHERE cp.empresa.id = :empresaId
            ORDER BY cp.dataVencimento ASC, cp.id ASC
            """)
    List<ContaPagar> findByEmpresaIdComRelacionamentos(@Param("empresaId") Integer empresaId);

    @Query("""
            SELECT cp FROM ContaPagar cp
            LEFT JOIN FETCH cp.fornecedor
            LEFT JOIN FETCH cp.compra
            WHERE cp.empresa.id = :empresaId
              AND (:status IS NULL OR cp.status = :status)
              AND cp.dataVencimento BETWEEN :dataInicio AND :dataFim
            ORDER BY cp.dataVencimento ASC, cp.id ASC
            """)
    List<ContaPagar> findByEmpresaIdAndStatusAndVencimentoBetween(
            @Param("empresaId") Integer empresaId,
            @Param("status") String status,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT cp FROM ContaPagar cp
            LEFT JOIN FETCH cp.fornecedor
            LEFT JOIN FETCH cp.compra
            WHERE cp.empresa.id = :empresaId
              AND (:status IS NULL OR cp.status = :status)
              AND cp.dataEmissao BETWEEN :dataInicio AND :dataFim
            ORDER BY cp.dataVencimento ASC, cp.id ASC
            """)
    List<ContaPagar> findByEmpresaIdAndStatusAndEmissaoBetween(
            @Param("empresaId") Integer empresaId,
            @Param("status") String status,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT cp FROM ContaPagar cp
            LEFT JOIN FETCH cp.empresa
            LEFT JOIN FETCH cp.fornecedor
            LEFT JOIN FETCH cp.compra
            LEFT JOIN FETCH cp.usuario
            WHERE cp.id = :id
            """)
    Optional<ContaPagar> findByIdComRelacionamentos(@Param("id") Integer id);

    // Soma total de contas por status
    @Query("SELECT COALESCE(SUM(cp.valor), 0) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId AND cp.status = :status")
    BigDecimal sumValorByEmpresaIdAndStatus(@Param("empresaId") Integer empresaId,
                                            @Param("status") String status);

    // Contas ABERTAS com vencimento antes de hoje (vencidas)
    @Query("SELECT COUNT(cp) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId " +
           "AND cp.status = 'ABERTA' " +
           "AND cp.dataVencimento < :hoje")
    long countVencidas(@Param("empresaId") Integer empresaId,
                       @Param("hoje") LocalDate hoje);

    @Query("SELECT COALESCE(SUM(cp.valor), 0) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId " +
           "AND cp.status = 'ABERTA' " +
           "AND cp.dataVencimento < :hoje")
    BigDecimal sumValorVencidas(@Param("empresaId") Integer empresaId,
                                @Param("hoje") LocalDate hoje);

    // Contas com vencimento HOJE
    @Query("SELECT COUNT(cp) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId " +
           "AND cp.status = 'ABERTA' " +
           "AND cp.dataVencimento = :hoje")
    long countVencendoHoje(@Param("empresaId") Integer empresaId,
                           @Param("hoje") LocalDate hoje);

    @Query("SELECT COALESCE(SUM(cp.valor), 0) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId " +
           "AND cp.status = 'ABERTA' " +
           "AND cp.dataVencimento = :hoje")
    BigDecimal sumValorVencendoHoje(@Param("empresaId") Integer empresaId,
                                    @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(cp) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId " +
           "AND cp.status = 'PAGA' " +
           "AND cp.dataPagamento BETWEEN :inicio AND :fim")
    long countPagasPeriodo(@Param("empresaId") Integer empresaId,
                           @Param("inicio") LocalDate inicio,
                           @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(cp.valorPago), 0) FROM ContaPagar cp " +
           "WHERE cp.empresa.id = :empresaId " +
           "AND cp.status = 'PAGA' " +
           "AND cp.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal sumValorPagoPeriodo(@Param("empresaId") Integer empresaId,
                                   @Param("inicio") LocalDate inicio,
                                   @Param("fim") LocalDate fim);
}
