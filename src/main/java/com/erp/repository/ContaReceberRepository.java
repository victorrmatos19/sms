package com.erp.repository;

import com.erp.model.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaReceberRepository extends JpaRepository<ContaReceber, Integer> {

    long countByEmpresaIdAndStatus(Integer empresaId, String status);

    @Query("""
            SELECT cr FROM ContaReceber cr
            LEFT JOIN FETCH cr.cliente
            LEFT JOIN FETCH cr.venda
            LEFT JOIN FETCH cr.usuario
            WHERE cr.empresa.id = :empresaId
            ORDER BY cr.dataVencimento ASC, cr.id ASC
            """)
    List<ContaReceber> findByEmpresaIdComRelacionamentos(@Param("empresaId") Integer empresaId);

    @Query("""
            SELECT cr FROM ContaReceber cr
            LEFT JOIN FETCH cr.empresa
            LEFT JOIN FETCH cr.cliente
            LEFT JOIN FETCH cr.venda
            LEFT JOIN FETCH cr.usuario
            WHERE cr.id = :id
            """)
    Optional<ContaReceber> findByIdComRelacionamentos(@Param("id") Integer id);

    @Query("SELECT COALESCE(SUM(cr.valor), 0) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId AND cr.status = :status")
    BigDecimal sumValorByEmpresaIdAndStatus(@Param("empresaId") Integer empresaId,
                                            @Param("status") String status);

    @Query("SELECT COUNT(cr) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId " +
           "AND cr.status = 'ABERTA' " +
           "AND cr.dataVencimento < :hoje")
    long countVencidas(@Param("empresaId") Integer empresaId,
                       @Param("hoje") LocalDate hoje);

    @Query("SELECT COALESCE(SUM(cr.valor), 0) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId " +
           "AND cr.status = 'ABERTA' " +
           "AND cr.dataVencimento < :hoje")
    BigDecimal sumValorVencidas(@Param("empresaId") Integer empresaId,
                                @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(cr) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId " +
           "AND cr.status = 'ABERTA' " +
           "AND cr.dataVencimento = :hoje")
    long countVencendoHoje(@Param("empresaId") Integer empresaId,
                           @Param("hoje") LocalDate hoje);

    @Query("SELECT COALESCE(SUM(cr.valor), 0) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId " +
           "AND cr.status = 'ABERTA' " +
           "AND cr.dataVencimento = :hoje")
    BigDecimal sumValorVencendoHoje(@Param("empresaId") Integer empresaId,
                                    @Param("hoje") LocalDate hoje);

    @Query("SELECT COUNT(cr) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId " +
           "AND cr.status = 'RECEBIDA' " +
           "AND cr.dataRecebimento BETWEEN :inicio AND :fim")
    long countRecebidasPeriodo(@Param("empresaId") Integer empresaId,
                               @Param("inicio") LocalDate inicio,
                               @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(cr.valorPago), 0) FROM ContaReceber cr " +
           "WHERE cr.empresa.id = :empresaId " +
           "AND cr.status = 'RECEBIDA' " +
           "AND cr.dataRecebimento BETWEEN :inicio AND :fim")
    BigDecimal sumValorRecebidoPeriodo(@Param("empresaId") Integer empresaId,
                                       @Param("inicio") LocalDate inicio,
                                       @Param("fim") LocalDate fim);

    List<ContaReceber> findByVendaIdOrderByNumeroParcela(Integer vendaId);
}
