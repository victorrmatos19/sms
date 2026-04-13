package com.erp.repository;

import com.erp.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Integer> {

    List<Funcionario> findByEmpresaIdAndAtivoTrueOrderByNome(Integer empresaId);

    List<Funcionario> findByEmpresaIdOrderByNome(Integer empresaId);

    boolean existsByEmpresaIdAndCpfAndIdNot(Integer empresaId, String cpf, Integer id);

    boolean existsByEmpresaIdAndCpf(Integer empresaId, String cpf);

    long countByEmpresaIdAndAtivoTrue(Integer empresaId);

    long countByEmpresaIdAndUsuarioIdIsNotNull(Integer empresaId);

    Optional<Funcionario> findByEmpresaIdAndUsuarioId(Integer empresaId, Integer usuarioId);

    @Query("SELECT f FROM Funcionario f " +
           "WHERE f.empresa.id = :empresaId " +
           "AND (:mostrarInativos = true OR f.ativo = true) " +
           "AND (:busca IS NULL OR :busca = '' " +
           "     OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(f.cpf, '')) LIKE LOWER(CONCAT('%', :busca, '%')) " +
           "     OR LOWER(COALESCE(f.cargo, '')) LIKE LOWER(CONCAT('%', :busca, '%'))) " +
           "ORDER BY f.nome")
    List<Funcionario> buscarComFiltros(@Param("empresaId") Integer empresaId,
                                       @Param("busca") String busca,
                                       @Param("mostrarInativos") boolean mostrarInativos);
}
