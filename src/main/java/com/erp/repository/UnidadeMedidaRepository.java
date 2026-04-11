package com.erp.repository;

import com.erp.model.UnidadeMedida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeMedidaRepository extends JpaRepository<UnidadeMedida, Integer> {
    List<UnidadeMedida> findByEmpresaIdOrderBySigla(Integer empresaId);
}
