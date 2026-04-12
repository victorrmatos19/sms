package com.erp.repository;

import com.erp.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    List<Cliente> findByEmpresaIdAndAtivoTrueOrderByNome(Integer empresaId);

    List<Cliente> findByEmpresaIdOrderByNome(Integer empresaId);

    Optional<Cliente> findByEmpresaIdAndCpfCnpj(Integer empresaId, String cpfCnpj);

    /** Verifica duplicidade de CPF/CNPJ ignorando o próprio registro (para edição). */
    boolean existsByEmpresaIdAndCpfCnpjAndIdNot(Integer empresaId, String cpfCnpj, Integer id);

    /** Verifica duplicidade de CPF/CNPJ para novo registro. */
    boolean existsByEmpresaIdAndCpfCnpj(Integer empresaId, String cpfCnpj);

    long countByEmpresaIdAndAtivoTrue(Integer empresaId);

    long countByEmpresaIdAndLimiteCreditoGreaterThan(Integer empresaId, BigDecimal valor);

    @Query("SELECT c FROM Cliente c " +
           "WHERE c.empresa.id = :empresaId " +
           "AND (:mostrarInativos = true OR c.ativo = true) " +
           "AND (:busca IS NULL OR :busca = '' " +
           "     OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(c.razaoSocial, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(c.cpfCnpj, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(c.telefone, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(c.celular, '')) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
           "ORDER BY c.nome")
    List<Cliente> buscarComFiltros(@Param("empresaId") Integer empresaId,
                                   @Param("busca") String busca,
                                   @Param("mostrarInativos") boolean mostrarInativos);
}
