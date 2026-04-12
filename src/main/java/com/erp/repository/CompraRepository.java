package com.erp.repository;

import com.erp.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @Query("SELECT c FROM Compra c JOIN FETCH c.fornecedor WHERE c.empresa.id = :empresaId ORDER BY c.dataEmissao DESC, c.id DESC")
    List<Compra> findByEmpresaIdOrderByDataEmissaoDesc(@Param("empresaId") Integer empresaId);

    @Query("SELECT c FROM Compra c JOIN FETCH c.fornecedor WHERE c.empresa.id = :empresaId AND c.status = :status ORDER BY c.dataEmissao DESC")
    List<Compra> findByEmpresaIdAndStatus(@Param("empresaId") Integer empresaId, @Param("status") String status);

    long countByEmpresaIdAndStatus(Integer empresaId, String status);

    long countByEmpresaIdAndStatusAndDataEmissaoBetween(
            Integer empresaId, String status, LocalDate inicio, LocalDate fim);

    long countByEmpresaId(Integer empresaId);

    /**
     * Carrega a compra com todos os itens inicializados (JOIN FETCH c.itens).
     * Necessário para evitar LazyInitializationException ao abrir o formulário
     * de edição fora do contexto da sessão Hibernate.
     */
    @Query("""
            SELECT c FROM Compra c
            LEFT JOIN FETCH c.itens i
            LEFT JOIN FETCH i.produto
            LEFT JOIN FETCH i.lote
            JOIN FETCH c.fornecedor
            WHERE c.id = :id
            """)
    Optional<Compra> findByIdWithItens(@Param("id") Integer id);

    @Query("""
            SELECT ci.custoUnitario FROM CompraItem ci
            WHERE ci.compra.empresa.id = :empresaId
              AND ci.produto.id = :produtoId
              AND ci.compra.status = 'CONFIRMADA'
            ORDER BY ci.compra.dataRecebimento DESC
            LIMIT 1
            """)
    Optional<BigDecimal> findUltimoCustoProduto(
            @Param("empresaId") Integer empresaId,
            @Param("produtoId") Integer produtoId);
}
