package com.erp.repository;

import com.erp.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {

    /** Carrega todos os produtos da empresa com grupo e unidade em um único JOIN FETCH (evita N+1). */
    @Query("SELECT DISTINCT p FROM Produto p " +
           "LEFT JOIN FETCH p.grupo " +
           "LEFT JOIN FETCH p.unidade " +
           "WHERE p.empresa.id = :empresaId " +
           "ORDER BY p.descricao")
    List<Produto> findAllByEmpresaIdWithDetails(@Param("empresaId") Integer empresaId);

    List<Produto> findByEmpresaIdAndAtivoTrue(Integer empresaId);

    List<Produto> findByEmpresaId(Integer empresaId);

    List<Produto> findByEmpresaIdAndGrupoId(Integer empresaId, Integer grupoId);

    long countByEmpresaIdAndAtivoTrue(Integer empresaId);

    @Query("SELECT p FROM Produto p " +
           "WHERE p.empresa.id = :empresaId " +
           "AND p.ativo = true " +
           "AND p.estoqueAtual < p.estoqueMinimo")
    List<Produto> findByEmpresaIdAndEstoqueAbaixoMinimo(@Param("empresaId") Integer empresaId);

    @Query("SELECT DISTINCT p FROM Produto p " +
           "LEFT JOIN FETCH p.grupo " +
           "LEFT JOIN FETCH p.unidade " +
           "WHERE p.empresa.id = :empresaId " +
           "AND (:mostrarInativos = true OR p.ativo = true) " +
           "AND (:grupoId IS NULL OR p.grupo.id = :grupoId) " +
           "AND (:busca IS NULL OR :busca = '' " +
           "     OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(p.codigoInterno) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(p.codigoBarras) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
           "ORDER BY p.descricao")
    List<Produto> buscarComFiltros(@Param("empresaId") Integer empresaId,
                                   @Param("busca") String busca,
                                   @Param("grupoId") Integer grupoId,
                                   @Param("mostrarInativos") boolean mostrarInativos);
}
