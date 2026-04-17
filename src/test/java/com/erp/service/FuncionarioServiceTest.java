package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Empresa;
import com.erp.model.Funcionario;
import com.erp.model.Usuario;
import com.erp.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do FuncionarioService.
 * Cobre CF-96 a CF-113.
 */
@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock private FuncionarioRepository funcionarioRepository;

    @InjectMocks private FuncionarioService funcionarioService;

    private Empresa empresa;
    private Funcionario funcionarioBase;
    private Funcionario funcionarioSemCpf;
    private Usuario usuarioVinculado;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
            .id(1)
            .razaoSocial("Empresa Teste")
            .cnpj("00.000.000/0001-00")
            .regimeTributario("SIMPLES_NACIONAL")
            .build();

        // CPF 123.456.789-09 — válido
        funcionarioBase = Funcionario.builder()
            .id(1)
            .empresa(empresa)
            .nome("Carlos Vendedor")
            .cpf("12345678909")
            .cargo("Vendedor")
            .ativo(true)
            .percentualComissao(BigDecimal.ZERO)
            .build();

        funcionarioSemCpf = Funcionario.builder()
            .id(2)
            .empresa(empresa)
            .nome("Maria Caixa")
            .cpf(null)
            .cargo("Caixa")
            .ativo(true)
            .percentualComissao(BigDecimal.ZERO)
            .build();

        usuarioVinculado = Usuario.builder()
            .id(1)
            .empresa(empresa)
            .nome("Carlos Vendedor")
            .login("carlos.vendedor")
            .build();
    }

    // ---- CF-96: Cadastrar sem CPF — sucesso ----

    @Test
    void dado_funcionario_sem_cpf_quando_salvar_entao_persiste() {
        when(funcionarioRepository.save(any())).thenReturn(funcionarioSemCpf);

        funcionarioService.salvar(funcionarioSemCpf);

        verify(funcionarioRepository, times(1)).save(funcionarioSemCpf);
        // Nenhum método de duplicidade deve ser chamado quando CPF é nulo
        verify(funcionarioRepository, never())
            .existsByEmpresaIdAndCpfAndIdNot(any(), any(), any());
        verify(funcionarioRepository, never())
            .existsByEmpresaIdAndCpf(any(), any());
    }

    // ---- CF-97: Cadastrar com CPF válido — sucesso ----

    @Test
    void dado_funcionario_com_cpf_valido_quando_salvar_entao_persiste() {
        // id=1 → usa existsByEmpresaIdAndCpfAndIdNot
        when(funcionarioRepository.existsByEmpresaIdAndCpfAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(funcionarioRepository.save(any())).thenReturn(funcionarioBase);

        funcionarioService.salvar(funcionarioBase);

        verify(funcionarioRepository, times(1)).save(funcionarioBase);
        assertThat(funcionarioBase.getPercentualComissao())
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dado_novo_funcionario_com_cpf_valido_quando_salvar_entao_persiste() {
        // CPF 111.444.777-35 — válido; id=null → usa existsByEmpresaIdAndCpf
        Funcionario novo = Funcionario.builder()
            .empresa(empresa).nome("Pedro Gerente")
            .cpf("11144477735").cargo("Gerente")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        when(funcionarioRepository.existsByEmpresaIdAndCpf(1, "11144477735"))
            .thenReturn(false);
        when(funcionarioRepository.save(any())).thenReturn(novo);

        funcionarioService.salvar(novo);

        verify(funcionarioRepository, times(1)).save(novo);
    }

    // ---- CF-99: CPF inválido quando preenchido ----

    @Test
    void dado_funcionario_com_cpf_invalido_quando_salvar_entao_lanca_negocio_exception() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Teste")
            .cpf("12345678900") // dígito verificador errado
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        assertThatThrownBy(() -> funcionarioService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CPF inválido");

        verify(funcionarioRepository, never()).save(any());
    }

    // ---- CF-100: CPF com sequência repetida rejeitado ----

    @Test
    void dado_funcionario_com_cpf_sequencia_repetida_quando_salvar_entao_lanca_excecao() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Teste Repetido")
            .cpf("11111111111")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        assertThatThrownBy(() -> funcionarioService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CPF inválido");

        verify(funcionarioRepository, never()).save(any());
    }

    // ---- CF-101: CPF duplicado na mesma empresa ----

    @Test
    void dado_cpf_duplicado_na_empresa_quando_salvar_entao_lanca_negocio_exception() {
        // Novo funcionário (id=null) → service chama existsByEmpresaIdAndCpf
        Funcionario novo = Funcionario.builder()
            .empresa(empresa).nome("Outro Vendedor")
            .cpf("12345678909")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        when(funcionarioRepository.existsByEmpresaIdAndCpf(1, "12345678909"))
            .thenReturn(true);

        assertThatThrownBy(() -> funcionarioService.salvar(novo))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CPF já cadastrado");

        verify(funcionarioRepository, never()).save(any());
    }

    // ---- CF-102: CPF duplicado em outra empresa não bloqueia ----

    @Test
    void dado_cpf_existente_em_outra_empresa_quando_salvar_entao_persiste_normalmente() {
        Funcionario novo = Funcionario.builder()
            .empresa(empresa).nome("João Outra Empresa")
            .cpf("12345678909")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        // Query já filtra por empresaId — retorna false para esta empresa
        when(funcionarioRepository.existsByEmpresaIdAndCpf(1, "12345678909"))
            .thenReturn(false);
        when(funcionarioRepository.save(any())).thenReturn(novo);

        funcionarioService.salvar(novo);

        verify(funcionarioRepository, times(1)).save(novo);
    }

    // ---- CF-103: Dois funcionários sem CPF permitido ----

    @Test
    void dado_dois_funcionarios_sem_cpf_quando_salvar_entao_permite() {
        Funcionario segundo = Funcionario.builder()
            .empresa(empresa).nome("Ana Estoque")
            .cpf(null).cargo("Estoquista")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        when(funcionarioRepository.save(any())).thenReturn(segundo);

        funcionarioService.salvar(segundo);

        verify(funcionarioRepository, times(1)).save(segundo);
        verify(funcionarioRepository, never())
            .existsByEmpresaIdAndCpfAndIdNot(any(), any(), any());
        verify(funcionarioRepository, never())
            .existsByEmpresaIdAndCpf(any(), any());
    }

    // ---- CF-104: Editar sem duplicar CPF consigo mesmo ----

    @Test
    void dado_funcionario_existente_quando_editar_com_mesmo_cpf_entao_nao_lanca_excecao() {
        // id=1 → service usa existsByEmpresaIdAndCpfAndIdNot
        when(funcionarioRepository.existsByEmpresaIdAndCpfAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(funcionarioRepository.save(any())).thenReturn(funcionarioBase);

        funcionarioService.salvar(funcionarioBase);

        verify(funcionarioRepository, times(1)).save(funcionarioBase);
    }

    // ---- CF-106: Email inválido lança exceção ----

    @Test
    void dado_funcionario_com_email_invalido_quando_salvar_entao_lanca_negocio_exception() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Teste Email")
            .cpf(null).email("semformato")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();

        assertThatThrownBy(() -> funcionarioService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Email inválido");

        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void dado_funcionario_com_email_valido_quando_salvar_entao_persiste() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Teste Email Válido")
            .cpf(null).email("carlos@empresa.com.br")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();
        when(funcionarioRepository.save(any())).thenReturn(f);

        funcionarioService.salvar(f);

        verify(funcionarioRepository, times(1)).save(f);
    }

    // ---- CF-107: Inativar funcionário ----

    @Test
    void dado_funcionario_ativo_quando_inativar_entao_ativo_fica_false() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioBase));

        funcionarioService.inativar(1);

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void dado_id_inexistente_quando_inativar_entao_nao_lanca_excecao() {
        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        funcionarioService.inativar(999);

        verify(funcionarioRepository, never()).save(any());
    }

    // ---- CF-108: Ativar funcionário ----

    @Test
    void dado_funcionario_inativo_quando_ativar_entao_ativo_fica_true() {
        funcionarioBase.setAtivo(false);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioBase));

        funcionarioService.ativar(1);

        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    // ---- CF-109: Contagem com acesso ao sistema ----

    @Test
    void quando_contar_com_acesso_entao_delega_ao_repository() {
        when(funcionarioRepository.countByEmpresaIdAndUsuarioIdIsNotNull(1)).thenReturn(2L);

        long total = funcionarioService.contarComAcesso(1);

        assertThat(total).isEqualTo(2L);
        verify(funcionarioRepository).countByEmpresaIdAndUsuarioIdIsNotNull(1);
    }

    @Test
    void quando_nenhum_funcionario_tem_acesso_entao_retorna_zero() {
        when(funcionarioRepository.countByEmpresaIdAndUsuarioIdIsNotNull(1)).thenReturn(0L);

        long total = funcionarioService.contarComAcesso(1);

        assertThat(total).isEqualTo(0L);
    }

    // ---- CF-112: Data de admissão opcional ----

    @Test
    void dado_funcionario_sem_data_admissao_quando_salvar_entao_persiste_normalmente() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Sem Data")
            .cpf(null).dataAdmissao(null)
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();
        when(funcionarioRepository.save(any())).thenReturn(f);

        funcionarioService.salvar(f);

        verify(funcionarioRepository, times(1)).save(f);
    }

    // ---- CF-113: Percentual de comissão padrão zero ----

    @Test
    void dado_funcionario_novo_quando_salvar_entao_percentual_comissao_e_zero() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Novo Funcionário")
            .cpf(null).ativo(true)
            .percentualComissao(BigDecimal.ZERO)
            .build();
        when(funcionarioRepository.save(any())).thenReturn(f);

        Funcionario salvo = funcionarioService.salvar(f);

        assertThat(salvo.getPercentualComissao())
            .isEqualByComparingTo(BigDecimal.ZERO);
        verify(funcionarioRepository, times(1)).save(f);
    }

    // ---- Delegações simples ----

    @Test
    void quando_listar_todos_entao_delega_ao_repository() {
        when(funcionarioRepository.findByEmpresaIdOrderByNome(1))
            .thenReturn(List.of(funcionarioBase, funcionarioSemCpf));

        List<Funcionario> resultado = funcionarioService.listarTodos(1);

        assertThat(resultado).hasSize(2);
        verify(funcionarioRepository).findByEmpresaIdOrderByNome(1);
    }

    @Test
    void quando_listar_ativos_entao_delega_ao_repository() {
        when(funcionarioRepository.findByEmpresaIdAndAtivoTrueOrderByNome(1))
            .thenReturn(List.of(funcionarioBase));

        List<Funcionario> resultado = funcionarioService.listarAtivos(1);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void quando_contar_ativos_entao_delega_ao_repository() {
        when(funcionarioRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(4L);

        long total = funcionarioService.contarAtivos(1);

        assertThat(total).isEqualTo(4L);
    }

    @Test
    void quando_buscar_por_id_existente_entao_retorna_funcionario() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioBase));

        Optional<Funcionario> resultado = funcionarioService.buscarPorId(1);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Carlos Vendedor");
    }

    @Test
    void quando_buscar_por_id_inexistente_entao_retorna_empty() {
        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        Optional<Funcionario> resultado = funcionarioService.buscarPorId(999);

        assertThat(resultado).isEmpty();
    }

    // ---- CF-184: funcionário sem CPF deve ser permitido ----
    // Já coberto por CF-96: dado_funcionario_sem_cpf_quando_salvar_entao_persiste

    // ---- CF-185: funcionário com CPF inválido quando preenchido lança exceção ----
    // Já coberto por CF-99: dado_funcionario_com_cpf_invalido_quando_salvar_entao_lanca_negocio_exception

    // ---- CF-186: percentual_comissao negativo deve lançar exceção ----

    @Test
    void dado_funcionario_com_comissao_negativa_quando_salvar_entao_lanca_negocio_exception() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Vendedor Comissão Negativa")
            .cpf(null).cargo("Vendedor")
            .ativo(true).percentualComissao(new BigDecimal("-5.00"))
            .build();

        assertThatThrownBy(() -> funcionarioService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("comissão");

        verify(funcionarioRepository, never()).save(any());
    }

    // ---- CF-187: percentual_comissao acima de 100 deve lançar exceção ----

    @Test
    void dado_funcionario_com_comissao_acima_de_100_quando_salvar_entao_lanca_negocio_exception() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Vendedor Comissão Absurda")
            .cpf(null).cargo("Vendedor")
            .ativo(true).percentualComissao(new BigDecimal("101.00"))
            .build();

        assertThatThrownBy(() -> funcionarioService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("comissão");

        verify(funcionarioRepository, never()).save(any());
    }

    // ---- CF-188: mesmo usuario_id não pode estar vinculado a dois funcionários ativos ----
    // GAP: FuncionarioService não valida unicidade de usuario_id entre funcionários ativos.
    // Recomendação: adicionar FuncionarioRepository.existsByEmpresaIdAndUsuarioIdAndIdNot()
    // e verificar antes de salvar quando usuario_id != null.

    // ---- CF-189: inativar funcionário que é vendedor de orçamentos em aberto ----
    // GAP: inativar() não verifica orçamentos em ABERTO com vendedor vinculado.
    // Recomendação: adicionar aviso (não bloqueio) antes de inativar.

    @Test
    void dado_funcionario_com_comissao_zero_quando_salvar_entao_persiste() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Funcionário Sem Comissão")
            .cpf(null).cargo("Caixa")
            .ativo(true).percentualComissao(BigDecimal.ZERO)
            .build();
        when(funcionarioRepository.save(any())).thenReturn(f);

        funcionarioService.salvar(f);

        verify(funcionarioRepository, times(1)).save(f);
    }

    @Test
    void dado_funcionario_com_comissao_100_quando_salvar_entao_persiste() {
        Funcionario f = Funcionario.builder()
            .empresa(empresa).nome("Funcionário 100% Comissão")
            .cpf(null).cargo("Representante")
            .ativo(true).percentualComissao(new BigDecimal("100.00"))
            .build();
        when(funcionarioRepository.save(any())).thenReturn(f);

        funcionarioService.salvar(f);

        verify(funcionarioRepository, times(1)).save(f);
    }
}
