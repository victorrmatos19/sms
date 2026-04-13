package com.erp.repository;

import com.erp.model.ContaReceber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaReceberRepository extends JpaRepository<ContaReceber, Integer> {
    List<ContaReceber> findByEmpresaIdOrderByDataVencimentoAsc(Integer empresaId);
    long countByEmpresaIdAndStatus(Integer empresaId, String status);
}
