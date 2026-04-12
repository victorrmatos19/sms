package com.erp.repository;

import com.erp.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Integer> {

    Optional<Lote> findByProdutoIdAndNumeroLote(Integer produtoId, String numeroLote);
}
