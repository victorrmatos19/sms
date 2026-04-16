package com.erp.repository;

import com.erp.model.Empresa;
import com.erp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmpresaAndLoginAndAtivoTrue(Empresa empresa, String login);

    @Query("""
            SELECT DISTINCT u FROM Usuario u
            LEFT JOIN FETCH u.perfis
            WHERE u.login = :login
            """)
    Optional<Usuario> findByLogin(@Param("login") String login);

    boolean existsByEmpresaAndLogin(Empresa empresa, String login);

    @Query("""
            SELECT DISTINCT u FROM Usuario u
            LEFT JOIN FETCH u.perfis
            WHERE u.empresa.id = :empresaId
            ORDER BY u.nome
            """)
    List<Usuario> findByEmpresaIdOrderByNomeAsc(@Param("empresaId") Integer empresaId);

    @Query("""
            SELECT DISTINCT u FROM Usuario u
            LEFT JOIN FETCH u.perfis
            WHERE u.empresa.id = :empresaId
              AND (LOWER(u.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR LOWER(u.login) LIKE LOWER(CONCAT('%', :termo, '%')))
            ORDER BY u.nome
            """)
    List<Usuario> buscarPorEmpresaETermo(@Param("empresaId") Integer empresaId,
                                          @Param("termo") String termo);

    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.empresa.id = :empresaId
              AND LOWER(u.login) = LOWER(:login)
              AND (:ignorarId IS NULL OR u.id <> :ignorarId)
            """)
    boolean existsLoginEmOutroUsuario(@Param("empresaId") Integer empresaId,
                                      @Param("login") String login,
                                      @Param("ignorarId") Integer ignorarId);

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            JOIN u.perfis p
            WHERE u.empresa.id = :empresaId
              AND u.ativo = true
              AND p.nome = :perfil
            """)
    long countAtivosByEmpresaIdAndPerfilNome(@Param("empresaId") Integer empresaId,
                                             @Param("perfil") String perfil);
}
