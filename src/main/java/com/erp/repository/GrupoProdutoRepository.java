package com.erp.repository;

import com.erp.model.GrupoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoProdutoRepository extends JpaRepository<GrupoProduto, Integer> {
    List<GrupoProduto> findByEmpresaIdAndAtivoTrueOrderByNome(Integer empresaId);
}
