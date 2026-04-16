package com.erp.repository;

import com.erp.model.PerfilAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerfilAcessoRepository extends JpaRepository<PerfilAcesso, Integer> {

    Optional<PerfilAcesso> findByNome(String nome);

    List<PerfilAcesso> findAllByOrderByNomeAsc();
}
