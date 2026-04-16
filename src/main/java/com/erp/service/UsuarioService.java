package com.erp.service;

import com.erp.model.Empresa;
import com.erp.model.PerfilAcesso;
import com.erp.model.Usuario;
import com.erp.repository.EmpresaRepository;
import com.erp.repository.PerfilAcessoRepository;
import com.erp.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Pattern LOGIN_PATTERN = Pattern.compile("[a-zA-Z0-9_]{3,}");
    private static final String PERFIL_ADMINISTRADOR = "ADMINISTRADOR";

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PerfilAcessoRepository perfilAcessoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<Usuario> listarPorEmpresa(Integer empresaId) {
        return usuarioRepository.findByEmpresaIdOrderByNomeAsc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Usuario> buscar(Integer empresaId, String termo) {
        if (termo == null || termo.isBlank()) {
            return listarPorEmpresa(empresaId);
        }
        return usuarioRepository.buscarPorEmpresaETermo(empresaId, termo.trim());
    }

    @Transactional(readOnly = true)
    public boolean existeLogin(Integer empresaId, String login, Integer ignorarId) {
        if (login == null || login.isBlank()) {
            return false;
        }
        return usuarioRepository.existsLoginEmOutroUsuario(empresaId, login.trim(), ignorarId);
    }

    @Transactional(readOnly = true)
    public List<PerfilAcesso> listarPerfis() {
        return perfilAcessoRepository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Usuario criar(Integer empresaId, String nome, String login, String email,
                         Set<Integer> perfilIds, String senhaPlana) {
        validarNome(nome);
        validarLogin(login);
        validarSenha(senhaPlana);
        validarPerfis(perfilIds);
        String loginNormalizado = login.trim();

        if (existeLogin(empresaId, loginNormalizado, null)) {
            throw new IllegalArgumentException("Login já existe para outro usuário desta empresa");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada: " + empresaId));
        Set<PerfilAcesso> perfis = buscarPerfis(perfilIds);
        validarConcessaoAdministrador(perfis);

        Usuario usuario = Usuario.builder()
                .empresa(empresa)
                .perfis(perfis)
                .nome(nome.trim())
                .login(loginNormalizado)
                .email(blankToNull(email))
                .senhaHash(passwordEncoder.encode(senhaPlana))
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário criado: id={} login={} empresa={}", salvo.getId(), salvo.getLogin(), empresaId);
        return salvo;
    }

    @Transactional
    public Usuario editar(Integer id, String nome, String email, Set<Integer> perfilIds, Boolean ativo) {
        validarNome(nome);
        validarPerfis(perfilIds);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));

        Set<PerfilAcesso> perfis = buscarPerfis(perfilIds);
        validarConcessaoAdministrador(perfis);
        boolean desativando = Boolean.FALSE.equals(ativo) && Boolean.TRUE.equals(usuario.getAtivo());
        if (desativando) {
            validarDesativacao(id);
        }
        validarRemocaoUnicoAdministrador(usuario, perfis);

        usuario.setNome(nome.trim());
        usuario.setEmail(blankToNull(email));
        if (usuario.getPerfis() == null) {
            usuario.setPerfis(new LinkedHashSet<>());
        }
        usuario.getPerfis().clear();
        usuario.getPerfis().addAll(perfis);
        usuario.setAtivo(!Boolean.FALSE.equals(ativo));

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário editado: id={} login={} ativo={}", salvo.getId(), salvo.getLogin(), salvo.getAtivo());
        return salvo;
    }

    @Transactional
    public Usuario alterarSenha(Integer id, String novaSenhaPlana) {
        validarSenha(novaSenhaPlana);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
        usuario.setSenhaHash(passwordEncoder.encode(novaSenhaPlana));
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Senha alterada para usuário id={} login={}", salvo.getId(), salvo.getLogin());
        return salvo;
    }

    private void validarDesativacao(Integer id) {
        Usuario usuarioLogado = authService.getUsuarioLogado();
        if (usuarioLogado != null && id.equals(usuarioLogado.getId())) {
            throw new IllegalStateException("Você não pode desativar seu próprio usuário");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + id));
        boolean administrador = usuario.temPerfil(PERFIL_ADMINISTRADOR);
        if (!administrador || usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            return;
        }

        long administradoresAtivos = usuarioRepository.countAtivosByEmpresaIdAndPerfilNome(
                usuario.getEmpresa().getId(), PERFIL_ADMINISTRADOR);
        if (administradoresAtivos <= 1) {
            throw new IllegalStateException("Não é possível desativar o único administrador ativo da empresa");
        }
    }

    private Set<PerfilAcesso> buscarPerfis(Set<Integer> perfilIds) {
        Set<PerfilAcesso> perfis = new LinkedHashSet<>();
        for (Integer perfilId : perfilIds) {
            PerfilAcesso perfil = perfilAcessoRepository.findById(perfilId)
                    .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado: " + perfilId));
            perfis.add(perfil);
        }
        return perfis;
    }

    private void validarConcessaoAdministrador(Set<PerfilAcesso> perfis) {
        boolean contemAdministrador = perfis.stream()
                .anyMatch(perfil -> PERFIL_ADMINISTRADOR.equals(perfil.getNome()));
        if (contemAdministrador && !authService.temPerfil(PERFIL_ADMINISTRADOR)) {
            throw new IllegalStateException("Apenas administradores podem conceder acesso de administrador");
        }
    }

    private void validarRemocaoUnicoAdministrador(Usuario usuario, Set<PerfilAcesso> novosPerfis) {
        boolean eraAdministrador = usuario.temPerfil(PERFIL_ADMINISTRADOR);
        boolean continuaAdministrador = novosPerfis.stream()
                .anyMatch(perfil -> PERFIL_ADMINISTRADOR.equals(perfil.getNome()));
        if (!eraAdministrador || continuaAdministrador || !Boolean.TRUE.equals(usuario.getAtivo())
                || usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            return;
        }

        long administradoresAtivos = usuarioRepository.countAtivosByEmpresaIdAndPerfilNome(
                usuario.getEmpresa().getId(), PERFIL_ADMINISTRADOR);
        if (administradoresAtivos <= 1) {
            throw new IllegalStateException("Não é possível remover o perfil do único administrador ativo da empresa");
        }
    }

    private void validarPerfis(Set<Integer> perfilIds) {
        if (perfilIds == null || perfilIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um perfil");
        }
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
    }

    private void validarLogin(String login) {
        if (login == null || !LOGIN_PATTERN.matcher(login.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Login inválido: use letras, números e underscore, mínimo 3 caracteres");
        }
    }

    private void validarSenha(String senhaPlana) {
        if (senhaPlana == null || senhaPlana.length() < 6) {
            throw new IllegalArgumentException("Senha inválida: mínimo 6 caracteres");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
