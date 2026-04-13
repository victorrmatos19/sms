package com.erp.repository;

import com.erp.model.ContaPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ContaPagarRepository extends JpaRepository<ContaPagar, Integer> {

    long countByEmpresaIdAndStatus(Integer empresaId, String status);

    List<ContaPagar> findByEmpresaIdAndStatusOrderByDataVencimentoAsc(Integer empresaId, String status);

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
}
