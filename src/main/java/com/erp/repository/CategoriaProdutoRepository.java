package com.erp.repository;

import com.erp.model.CategoriaProduto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaProdutoRepository extends JpaRepository<CategoriaProduto, Integer> {
    List<CategoriaProduto> findByEmpresaIdAndAtivoTrueOrderByNome(Integer empresaId);
    List<CategoriaProduto> findByEmpresaIdAndGrupoIdAndAtivoTrueOrderByNome(Integer empresaId, Integer grupoId);
}
