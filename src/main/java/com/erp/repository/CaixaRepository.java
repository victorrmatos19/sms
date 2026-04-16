package com.erp.repository;

import com.erp.model.Caixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaixaRepository extends JpaRepository<Caixa, Integer> {
    Optional<Caixa> findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(Integer empresaId);
}
