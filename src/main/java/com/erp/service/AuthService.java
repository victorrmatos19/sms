package com.erp.service;

import com.erp.model.Usuario;
import com.erp.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço de autenticação de usuários.
 * Gerencia login, logout e a sessão do usuário logado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Usuário da sessão atual (um usuário por vez na aplicação desktop)
    private Usuario usuarioLogado;

    /**
     * Tenta autenticar o usuário com login e senha.
     *
     * @param login login do usuário
     * @param senha senha em texto puro
     * @return Optional com o usuário autenticado, ou vazio se inválido
     */
    @Transactional
    public Optional<Usuario> autenticar(String login, String senha) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByLogin(login);

        if (usuarioOpt.isEmpty()) {
            log.warn("Tentativa de login com usuário inexistente: {}", login);
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.getAtivo()) {
            log.warn("Tentativa de login com usuário inativo: {}", login);
            return Optional.empty();
        }

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            log.warn("Senha incorreta para usuário: {}", login);
            return Optional.empty();
        }

        // Atualiza último acesso
        usuario.setUltimoAcesso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        this.usuarioLogado = usuario;
        log.info("Usuário autenticado: {} ({})", usuario.getNome(), usuario.getPerfil().getNome());

        return Optional.of(usuario);
    }

    public void logout() {
        if (usuarioLogado != null) {
            log.info("Logout: {}", usuarioLogado.getNome());
        }
        this.usuarioLogado = null;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public boolean isLogado() {
        return usuarioLogado != null;
    }

    /**
     * Verifica se o usuário logado tem o perfil informado.
     */
    public boolean temPerfil(String nomePerfil) {
        if (usuarioLogado == null) return false;
        return usuarioLogado.getPerfil().getNome().equals(nomePerfil);
    }

    /**
     * Verifica se o usuário logado é Administrador ou Gerente.
     */
    public boolean isAdminOuGerente() {
        return temPerfil("ADMINISTRADOR") || temPerfil("GERENTE");
    }
}
