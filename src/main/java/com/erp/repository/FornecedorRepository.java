package com.erp.repository;

import com.erp.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Integer> {

    List<Fornecedor> findByEmpresaIdAndAtivoTrueOrderByNome(Integer empresaId);

    List<Fornecedor> findByEmpresaIdOrderByNome(Integer empresaId);

    boolean existsByEmpresaIdAndCpfCnpjAndIdNot(Integer empresaId, String cpfCnpj, Integer id);

    boolean existsByEmpresaIdAndCpfCnpj(Integer empresaId, String cpfCnpj);

    long countByEmpresaIdAndAtivoTrue(Integer empresaId);

    /** Fornecedores com chave PIX cadastrada (não nula e não vazia). */
    long countByEmpresaIdAndBancoPixChaveIsNotNullAndBancoPixChaveNot(
        Integer empresaId, String valor);

    @Query("SELECT f FROM Fornecedor f " +
           "WHERE f.empresa.id = :empresaId " +
           "AND (:mostrarInativos = true OR f.ativo = true) " +
           "AND (:busca IS NULL OR :busca = '' " +
           "     OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(f.razaoSocial, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(f.cpfCnpj, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(f.telefone, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(f.celular, '')) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
           "ORDER BY f.nome")
    List<Fornecedor> buscarComFiltros(@Param("empresaId") Integer empresaId,
                                      @Param("busca") String busca,
                                      @Param("mostrarInativos") boolean mostrarInativos);
}
