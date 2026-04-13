package com.erp.repository;

import com.erp.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {

    @Query("""
            SELECT o FROM Orcamento o
            LEFT JOIN FETCH o.cliente
            LEFT JOIN FETCH o.vendedor
            WHERE o.empresa.id = :empresaId
            ORDER BY o.dataEmissao DESC, o.id DESC
            """)
    List<Orcamento> findByEmpresaIdOrderByDataEmissaoDesc(@Param("empresaId") Integer empresaId);

    @Query("""
            SELECT o FROM Orcamento o
            LEFT JOIN FETCH o.cliente
            LEFT JOIN FETCH o.vendedor
            WHERE o.empresa.id = :empresaId AND o.status = :status
            ORDER BY o.dataEmissao DESC
            """)
    List<Orcamento> findByEmpresaIdAndStatus(@Param("empresaId") Integer empresaId,
                                              @Param("status") String status);

    long countByEmpresaIdAndStatus(Integer empresaId, String status);

    long countByEmpresaId(Integer empresaId);

    long countByEmpresaIdAndStatusAndDataEmissaoBetween(
            Integer empresaId, String status, LocalDate inicio, LocalDate fim);

    boolean existsByEmpresaIdAndNumero(Integer empresaId, String numero);

    @Query("""
            SELECT o FROM Orcamento o
            LEFT JOIN FETCH o.itens i
            LEFT JOIN FETCH i.produto p
            LEFT JOIN FETCH p.unidade
            LEFT JOIN FETCH o.cliente
            LEFT JOIN FETCH o.vendedor
            LEFT JOIN FETCH o.empresa
            WHERE o.id = :id
            """)
    Optional<Orcamento> findByIdWithItens(@Param("id") Integer id);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Orcamento o SET o.status = 'EXPIRADO'
            WHERE o.empresa.id = :empresaId
              AND o.status = 'ABERTO'
              AND o.dataValidade < :hoje
            """)
    int expirarVencidos(@Param("empresaId") Integer empresaId, @Param("hoje") LocalDate hoje);

    @Query("""
            SELECT o FROM Orcamento o
            LEFT JOIN FETCH o.cliente
            LEFT JOIN FETCH o.vendedor
            WHERE o.empresa.id = :empresaId
              AND (:status IS NULL OR o.status = :status)
              AND (:vendedorId IS NULL OR o.vendedor.id = :vendedorId)
            ORDER BY o.dataEmissao DESC, o.id DESC
            """)
    List<Orcamento> findComFiltros(
            @Param("empresaId") Integer empresaId,
            @Param("status") String status,
            @Param("vendedorId") Integer vendedorId);
}
