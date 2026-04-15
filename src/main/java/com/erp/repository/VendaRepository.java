package com.erp.repository;

import com.erp.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Integer> {

    @Query("""
            SELECT DISTINCT v FROM Venda v
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.vendedor
            LEFT JOIN FETCH v.usuario
            LEFT JOIN FETCH v.orcamento
            LEFT JOIN FETCH v.pagamentos
            WHERE v.empresa.id = :empresaId
            ORDER BY v.dataVenda DESC, v.id DESC
            """)
    List<Venda> findByEmpresaIdOrderByDataVendaDesc(@Param("empresaId") Integer empresaId);

    @Query("""
            SELECT DISTINCT v FROM Venda v
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.vendedor
            LEFT JOIN FETCH v.usuario
            LEFT JOIN FETCH v.pagamentos
            WHERE v.empresa.id = :empresaId
              AND v.status = :status
            ORDER BY v.dataVenda DESC, v.id DESC
            """)
    List<Venda> findByEmpresaIdAndStatus(@Param("empresaId") Integer empresaId,
                                          @Param("status") String status);

    @Query("""
            SELECT DISTINCT v FROM Venda v
            LEFT JOIN FETCH v.itens i
            LEFT JOIN FETCH i.produto
            LEFT JOIN FETCH i.lote
            LEFT JOIN FETCH v.cliente
            LEFT JOIN FETCH v.vendedor
            LEFT JOIN FETCH v.empresa
            WHERE v.id = :id
            """)
    Optional<Venda> findByIdWithItens(@Param("id") Integer id);

    @Query("""
            SELECT DISTINCT v FROM Venda v
            LEFT JOIN FETCH v.pagamentos
            WHERE v.id = :id
            """)
    Optional<Venda> findByIdWithPagamentos(@Param("id") Integer id);

    long countByEmpresaId(Integer empresaId);

    long countByEmpresaIdAndStatus(Integer empresaId, String status);

    long countByEmpresaIdAndStatusAndDataVendaBetween(
            Integer empresaId, String status, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
            SELECT COALESCE(SUM(v.valorTotal), 0)
            FROM Venda v
            WHERE v.empresa.id = :empresaId
              AND v.status = :status
              AND v.dataVenda BETWEEN :inicio AND :fim
            """)
    BigDecimal sumValorTotalByEmpresaIdAndStatusAndDataVendaBetween(
            @Param("empresaId") Integer empresaId,
            @Param("status") String status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
