package com.erp.service;

import com.erp.model.Empresa;
import com.erp.model.PerfilAcesso;
import com.erp.model.Usuario;
import com.erp.repository.EmpresaRepository;
import com.erp.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do AuthService.
 * Cobre CF-27 a CF-32.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private Empresa empresa;
    private PerfilAcesso perfilAdmin;
    private PerfilAcesso perfilVendas;
    private Usuario usuarioAtivo;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
            .id(1)
            .razaoSocial("Empresa Teste")
            .cnpj("00.000.000/0001-00")
            .regimeTributario("SIMPLES_NACIONAL")
            .build();

        perfilAdmin = PerfilAcesso.builder().id(1).nome("ADMINISTRADOR").build();
        perfilVendas = PerfilAcesso.builder().id(2).nome("VENDAS").build();

        usuarioAtivo = Usuario.builder()
            .id(1)
            .nome("Admin")
            .login("admin")
            .senhaHash("$2a$10$hash")
            .ativo(true)
            .empresa(empresa)
            .perfil(perfilAdmin)
            .build();
    }

    // ---- CF-27: Login com sucesso ----

    @Test
    void dado_usuario_ativo_quando_autenticar_com_credenciais_corretas_entao_retorna_usuario() {
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));

        Optional<Usuario> resultado = authService.autenticar("admin", "admin123");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getLogin()).isEqualTo("admin");
    }

    @Test
    void dado_login_bem_sucedido_entao_atualiza_ultimo_acesso() {
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));

        authService.autenticar("admin", "admin123");

        verify(usuarioRepository).save(argThat(u -> u.getUltimoAcesso() != null));
    }

    @Test
    void dado_login_bem_sucedido_entao_sessao_fica_com_usuario_logado() {
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));

        authService.autenticar("admin", "admin123");

        assertThat(authService.isLogado()).isTrue();
        assertThat(authService.getUsuarioLogado().getLogin()).isEqualTo("admin");
        assertThat(authService.getEmpresaIdLogado()).isEqualTo(1);
        assertThat(authService.getEmpresaLogada()).isEqualTo(empresa);
    }

    // ---- CF-28: Login com usuário inexistente ----

    @Test
    void dado_usuario_inexistente_quando_autenticar_entao_retorna_empty() {
        when(usuarioRepository.findByLogin("naoexiste")).thenReturn(Optional.empty());

        Optional<Usuario> resultado = authService.autenticar("naoexiste", "qualquer");

        assertThat(resultado).isEmpty();
        assertThat(authService.isLogado()).isFalse();
    }

    @Test
    void dado_usuario_inexistente_quando_autenticar_entao_nao_lanca_excecao() {
        when(usuarioRepository.findByLogin("naoexiste")).thenReturn(Optional.empty());

        // Não deve lançar exceção
        authService.autenticar("naoexiste", "qualquer");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ---- CF-29: Login com senha incorreta ----

    @Test
    void dado_usuario_existente_quando_autenticar_com_senha_errada_entao_retorna_empty() {
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("senhaerrada", "$2a$10$hash")).thenReturn(false);

        Optional<Usuario> resultado = authService.autenticar("admin", "senhaerrada");

        assertThat(resultado).isEmpty();
        assertThat(authService.isLogado()).isFalse();
    }

    // ---- CF-30: Login com usuário inativo ----

    @Test
    void dado_usuario_inativo_quando_autenticar_entao_retorna_empty() {
        Usuario usuarioInativo = Usuario.builder()
            .id(2).login("inativo").senhaHash("$2a$10$hash")
            .ativo(false).empresa(empresa).perfil(perfilVendas).nome("Inativo")
            .build();

        when(usuarioRepository.findByLogin("inativo")).thenReturn(Optional.of(usuarioInativo));

        Optional<Usuario> resultado = authService.autenticar("inativo", "qualquer");

        assertThat(resultado).isEmpty();
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // ---- CF-31: Logout ----

    @Test
    void dado_usuario_logado_quando_logout_entao_sessao_e_limpa() {
        // Loga primeiro
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
        authService.autenticar("admin", "admin123");
        assertThat(authService.isLogado()).isTrue();

        authService.logout();

        assertThat(authService.isLogado()).isFalse();
        assertThat(authService.getUsuarioLogado()).isNull();
        assertThat(authService.getEmpresaIdLogado()).isNull();
        assertThat(authService.getEmpresaLogada()).isNull();
    }

    // ---- CF-32: Verificação de perfil ----

    @Test
    void dado_usuario_com_perfil_vendas_quando_verificar_perfil_vendas_entao_retorna_true() {
        usuarioAtivo.setPerfil(perfilVendas);
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
        authService.autenticar("admin", "admin123");

        assertThat(authService.temPerfil("VENDAS")).isTrue();
    }

    @Test
    void dado_usuario_com_perfil_vendas_quando_verificar_perfil_financeiro_entao_retorna_false() {
        usuarioAtivo.setPerfil(perfilVendas);
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
        authService.autenticar("admin", "admin123");

        assertThat(authService.temPerfil("FINANCEIRO")).isFalse();
    }

    @Test
    void dado_usuario_com_perfil_vendas_quando_verificar_admin_ou_gerente_entao_retorna_false() {
        usuarioAtivo.setPerfil(perfilVendas);
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
        authService.autenticar("admin", "admin123");

        assertThat(authService.isAdminOuGerente()).isFalse();
    }

    @Test
    void dado_usuario_com_perfil_administrador_quando_verificar_admin_ou_gerente_entao_retorna_true() {
        when(usuarioRepository.findByLogin("admin")).thenReturn(Optional.of(usuarioAtivo));
        when(passwordEncoder.matches("admin123", "$2a$10$hash")).thenReturn(true);
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
        authService.autenticar("admin", "admin123");

        assertThat(authService.isAdminOuGerente()).isTrue();
    }

    @Test
    void dado_nenhum_usuario_logado_quando_verificar_perfil_entao_retorna_false() {
        assertThat(authService.temPerfil("QUALQUER")).isFalse();
        assertThat(authService.isAdminOuGerente()).isFalse();
    }
}
