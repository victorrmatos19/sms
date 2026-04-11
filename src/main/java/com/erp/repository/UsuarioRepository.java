package com.erp.repository;

import com.erp.model.Empresa;
import com.erp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmpresaAndLoginAndAtivoTrue(Empresa empresa, String login);

    Optional<Usuario> findByLogin(String login);

    boolean existsByEmpresaAndLogin(Empresa empresa, String login);
}
