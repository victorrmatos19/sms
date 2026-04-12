package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Empresa;
import com.erp.model.Fornecedor;
import com.erp.repository.FornecedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do FornecedorService.
 * Cobre CF-71 a CF-92.
 */
@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock private FornecedorRepository fornecedorRepository;

    @InjectMocks private FornecedorService fornecedorService;

    private Empresa empresa;
    private Fornecedor fornecedorPfBase;
    private Fornecedor fornecedorPjBase;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
            .id(1)
            .razaoSocial("Empresa Teste")
            .cnpj("00.000.000/0001-00")
            .regimeTributario("SIMPLES_NACIONAL")
            .build();

        // CPF 123.456.789-09 — válido
        fornecedorPfBase = Fornecedor.builder()
            .id(1)
            .empresa(empresa)
            .tipoPessoa("PF")
            .nome("José Fornecedor")
            .cpfCnpj("12345678909")
            .ativo(true)
            .build();

        // CNPJ 11.222.333/0001-81 — válido
        fornecedorPjBase = Fornecedor.builder()
            .id(2)
            .empresa(empresa)
            .tipoPessoa("PJ")
            .nome("Distribuidora ABC")
            .razaoSocial("Distribuidora ABC Ltda")
            .cpfCnpj("11222333000181")
            .ativo(true)
            .build();
    }

    // ---- CF-71: Cadastrar PF com CPF válido ----

    @Test
    void dado_fornecedor_pf_quando_salvar_com_cpf_valido_entao_persiste() {
        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(fornecedorRepository.save(any())).thenReturn(fornecedorPfBase);

        fornecedorService.salvar(fornecedorPfBase);

        verify(fornecedorRepository, times(1)).save(fornecedorPfBase);
    }

    @Test
    void dado_novo_fornecedor_pf_quando_salvar_entao_persiste() {
        // CPF 111.444.777-35 — válido
        Fornecedor novo = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Maria Fornecedora")
            .cpfCnpj("11144477735").ativo(true)
            .build();

        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpj(1, "11144477735"))
            .thenReturn(false);
        when(fornecedorRepository.save(any())).thenReturn(novo);

        fornecedorService.salvar(novo);

        verify(fornecedorRepository, times(1)).save(novo);
    }

    // ---- CF-72: Cadastrar PJ com CNPJ válido ----

    @Test
    void dado_fornecedor_pj_quando_salvar_com_cnpj_valido_entao_persiste() {
        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "11222333000181", 2))
            .thenReturn(false);
        when(fornecedorRepository.save(any())).thenReturn(fornecedorPjBase);

        fornecedorService.salvar(fornecedorPjBase);

        verify(fornecedorRepository, times(1)).save(fornecedorPjBase);
    }

    @Test
    void dado_novo_fornecedor_pj_quando_salvar_entao_persiste() {
        // CNPJ 34.028.316/0001-03 — válido
        Fornecedor novo = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PJ").nome("Google Brasil")
            .razaoSocial("Google Brasil Internet Ltda")
            .cpfCnpj("34028316000103").ativo(true)
            .build();

        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpj(1, "34028316000103"))
            .thenReturn(false);
        when(fornecedorRepository.save(any())).thenReturn(novo);

        fornecedorService.salvar(novo);

        verify(fornecedorRepository, times(1)).save(novo);
    }

    // ---- CF-73: CPF inválido lança exceção ----

    @Test
    void dado_fornecedor_pf_com_cpf_invalido_quando_salvar_entao_lanca_negocio_exception() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Teste")
            .cpfCnpj("00000000000").ativo(true)
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CPF inválido");

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-74: CNPJ inválido lança exceção ----

    @Test
    void dado_fornecedor_pj_com_cnpj_invalido_quando_salvar_entao_lanca_negocio_exception() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PJ").nome("Empresa Inválida")
            .cpfCnpj("11111111111111").ativo(true)
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CNPJ inválido");

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-75: CPF duplicado na mesma empresa ----

    @Test
    void dado_cpf_duplicado_na_empresa_quando_salvar_entao_lanca_negocio_exception() {
        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.salvar(fornecedorPfBase))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CPF já cadastrado");

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-76: CNPJ duplicado na mesma empresa ----

    @Test
    void dado_cnpj_duplicado_na_empresa_quando_salvar_entao_lanca_negocio_exception() {
        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "11222333000181", 2))
            .thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.salvar(fornecedorPjBase))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CNPJ já cadastrado");

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-77: CPF duplicado em outra empresa não bloqueia ----

    @Test
    void dado_cpf_existente_em_outra_empresa_quando_salvar_entao_persiste_normalmente() {
        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(fornecedorRepository.save(any())).thenReturn(fornecedorPfBase);

        fornecedorService.salvar(fornecedorPfBase);

        verify(fornecedorRepository, times(1)).save(fornecedorPfBase);
    }

    // ---- CF-78: Editar sem duplicar consigo mesmo ----

    @Test
    void dado_fornecedor_existente_quando_editar_com_mesmo_cpf_entao_nao_lanca_excecao() {
        when(fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(fornecedorRepository.save(any())).thenReturn(fornecedorPfBase);

        fornecedorService.salvar(fornecedorPfBase);

        verify(fornecedorRepository, times(1)).save(fornecedorPfBase);
    }

    // ---- CF-79: Salvar sem CPF/CNPJ não lança exceção ----

    @Test
    void dado_fornecedor_sem_cpf_quando_salvar_entao_persiste_sem_validar_documento() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor Sem CPF")
            .cpfCnpj("").ativo(true)
            .build();
        when(fornecedorRepository.save(any())).thenReturn(f);

        fornecedorService.salvar(f);

        verify(fornecedorRepository, times(1)).save(f);
    }

    // ---- CF-80: Inativar fornecedor ----

    @Test
    void dado_fornecedor_ativo_quando_inativar_entao_ativo_fica_false() {
        when(fornecedorRepository.findById(1)).thenReturn(Optional.of(fornecedorPfBase));

        fornecedorService.inativar(1);

        ArgumentCaptor<Fornecedor> captor = ArgumentCaptor.forClass(Fornecedor.class);
        verify(fornecedorRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void dado_id_inexistente_quando_inativar_entao_nao_lanca_excecao() {
        when(fornecedorRepository.findById(999)).thenReturn(Optional.empty());

        fornecedorService.inativar(999);

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-81: Ativar fornecedor ----

    @Test
    void dado_fornecedor_inativo_quando_ativar_entao_ativo_fica_true() {
        fornecedorPfBase.setAtivo(false);
        when(fornecedorRepository.findById(1)).thenReturn(Optional.of(fornecedorPfBase));

        fornecedorService.ativar(1);

        ArgumentCaptor<Fornecedor> captor = ArgumentCaptor.forClass(Fornecedor.class);
        verify(fornecedorRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    // ---- CF-82: Listar todos ----

    @Test
    void quando_listar_todos_entao_delega_ao_repository() {
        when(fornecedorRepository.findByEmpresaIdOrderByNome(1))
            .thenReturn(List.of(fornecedorPfBase, fornecedorPjBase));

        List<Fornecedor> resultado = fornecedorService.listarTodos(1);

        assertThat(resultado).hasSize(2);
        verify(fornecedorRepository).findByEmpresaIdOrderByNome(1);
    }

    // ---- CF-83: Listar ativos ----

    @Test
    void quando_listar_ativos_entao_retorna_somente_ativos() {
        when(fornecedorRepository.findByEmpresaIdAndAtivoTrueOrderByNome(1))
            .thenReturn(List.of(fornecedorPfBase));

        List<Fornecedor> resultado = fornecedorService.listarAtivos(1);

        assertThat(resultado).hasSize(1);
        verify(fornecedorRepository).findByEmpresaIdAndAtivoTrueOrderByNome(1);
    }

    // ---- CF-84: Contar ativos ----

    @Test
    void quando_contar_ativos_entao_delega_ao_repository() {
        when(fornecedorRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(3L);

        long total = fornecedorService.contarAtivos(1);

        assertThat(total).isEqualTo(3L);
    }

    // ---- CF-85: Contar com PIX ----

    @Test
    void quando_contar_com_pix_entao_delega_ao_repository() {
        when(fornecedorRepository
            .countByEmpresaIdAndBancoPixChaveIsNotNullAndBancoPixChaveNot(1, ""))
            .thenReturn(2L);

        long total = fornecedorService.contarComPix(1);

        assertThat(total).isEqualTo(2L);
    }

    @Test
    void quando_nenhum_fornecedor_tem_pix_entao_retorna_zero() {
        when(fornecedorRepository
            .countByEmpresaIdAndBancoPixChaveIsNotNullAndBancoPixChaveNot(1, ""))
            .thenReturn(0L);

        long total = fornecedorService.contarComPix(1);

        assertThat(total).isEqualTo(0L);
    }

    // ---- CF-86: Buscar por id existente ----

    @Test
    void quando_buscar_por_id_existente_entao_retorna_fornecedor() {
        when(fornecedorRepository.findById(1)).thenReturn(Optional.of(fornecedorPfBase));

        Optional<Fornecedor> resultado = fornecedorService.buscarPorId(1);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("José Fornecedor");
    }

    // ---- CF-87: Buscar por id inexistente ----

    @Test
    void quando_buscar_por_id_inexistente_entao_retorna_empty() {
        when(fornecedorRepository.findById(999)).thenReturn(Optional.empty());

        Optional<Fornecedor> resultado = fornecedorService.buscarPorId(999);

        assertThat(resultado).isEmpty();
    }

    // ---- CF-88: PIX tipo sem chave lança exceção ----

    @Test
    void dado_fornecedor_com_pix_tipo_sem_chave_quando_salvar_entao_lanca_negocio_exception() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor PIX")
            .cpfCnpj("").ativo(true)
            .bancoPixTipo("CPF")
            .bancoPixChave(null)
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Chave PIX é obrigatória");

        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void dado_fornecedor_com_pix_tipo_e_chave_vazia_quando_salvar_entao_lanca_negocio_exception() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor PIX")
            .cpfCnpj("").ativo(true)
            .bancoPixTipo("EMAIL")
            .bancoPixChave("   ")
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Chave PIX é obrigatória");

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-89: PIX chave sem tipo lança exceção ----

    @Test
    void dado_fornecedor_com_pix_chave_sem_tipo_quando_salvar_entao_lanca_negocio_exception() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor PIX")
            .cpfCnpj("").ativo(true)
            .bancoPixTipo(null)
            .bancoPixChave("12345678909")
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Tipo da chave PIX é obrigatório");

        verify(fornecedorRepository, never()).save(any());
    }

    // ---- CF-90: PIX completo (tipo + chave) não lança exceção ----

    @Test
    void dado_fornecedor_com_pix_tipo_e_chave_quando_salvar_entao_persiste() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor PIX Completo")
            .cpfCnpj("").ativo(true)
            .bancoPixTipo("CPF")
            .bancoPixChave("12345678909")
            .build();
        when(fornecedorRepository.save(any())).thenReturn(f);

        fornecedorService.salvar(f);

        verify(fornecedorRepository, times(1)).save(f);
    }

    // ---- CF-91: Sem PIX não lança exceção ----

    @Test
    void dado_fornecedor_sem_pix_quando_salvar_entao_persiste_normalmente() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PJ").nome("Distribuidora Sem PIX")
            .cpfCnpj("").ativo(true)
            .bancoPixTipo(null).bancoPixChave(null)
            .build();
        when(fornecedorRepository.save(any())).thenReturn(f);

        fornecedorService.salvar(f);

        verify(fornecedorRepository, times(1)).save(f);
    }

    // ---- CF-92: Email inválido lança exceção ----

    @Test
    void dado_fornecedor_com_email_invalido_quando_salvar_entao_lanca_negocio_exception() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor Email Inválido")
            .cpfCnpj("").ativo(true)
            .email("email-invalido")
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Email inválido");

        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void dado_fornecedor_com_email_invalido_sem_dominio_quando_salvar_entao_lanca_excecao() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor Email Inválido")
            .cpfCnpj("").ativo(true)
            .email("emailsemarroba.com")
            .build();

        assertThatThrownBy(() -> fornecedorService.salvar(f))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Email inválido");

        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void dado_fornecedor_com_email_valido_quando_salvar_entao_persiste() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor Email Válido")
            .cpfCnpj("").ativo(true)
            .email("contato@fornecedor.com.br")
            .build();
        when(fornecedorRepository.save(any())).thenReturn(f);

        fornecedorService.salvar(f);

        verify(fornecedorRepository, times(1)).save(f);
    }

    @Test
    void dado_fornecedor_sem_email_quando_salvar_entao_persiste_normalmente() {
        Fornecedor f = Fornecedor.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Fornecedor Sem Email")
            .cpfCnpj("").ativo(true)
            .email(null)
            .build();
        when(fornecedorRepository.save(any())).thenReturn(f);

        fornecedorService.salvar(f);

        verify(fornecedorRepository, times(1)).save(f);
    }
}
