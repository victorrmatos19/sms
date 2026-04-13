package com.erp.repository;

import com.erp.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendaRepository extends JpaRepository<Venda, Integer> {
    long countByEmpresaId(Integer empresaId);
    long countByEmpresaIdAndStatus(Integer empresaId, String status);
}
