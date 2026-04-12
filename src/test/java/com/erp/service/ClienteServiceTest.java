package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Cliente;
import com.erp.model.Empresa;
import com.erp.repository.ClienteRepository;
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
 * Testes unitários do ClienteService.
 * Cobre CF-36, CF-37, CF-48, CF-49, CF-50, CF-51, CF-53 a CF-57.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock private ClienteRepository clienteRepository;

    @InjectMocks private ClienteService clienteService;

    private Empresa empresa;
    private Cliente clientePfBase;
    private Cliente clientePjBase;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
            .id(1)
            .razaoSocial("Empresa Teste")
            .cnpj("00.000.000/0001-00")
            .regimeTributario("SIMPLES_NACIONAL")
            .build();

        clientePfBase = Cliente.builder()
            .id(1)
            .empresa(empresa)
            .tipoPessoa("PF")
            .nome("João Silva")
            .cpfCnpj("12345678909") // CPF limpo, sem máscara
            .ativo(true)
            .limiteCredito(BigDecimal.ZERO)
            .creditoDisponivel(BigDecimal.ZERO)
            .build();

        clientePjBase = Cliente.builder()
            .id(2)
            .empresa(empresa)
            .tipoPessoa("PJ")
            .nome("Empresa XYZ")
            .razaoSocial("Empresa XYZ Ltda")
            .cpfCnpj("11222333000181") // CNPJ limpo
            .ativo(true)
            .limiteCredito(BigDecimal.ZERO)
            .creditoDisponivel(BigDecimal.ZERO)
            .build();
    }

    // ---- CF-36: Cadastrar PF com sucesso ----

    @Test
    void dado_cliente_pf_quando_salvar_com_cpf_valido_entao_persiste() {
        when(clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(clientePfBase);

        clienteService.salvar(clientePfBase);

        verify(clienteRepository, times(1)).save(clientePfBase);
    }

    @Test
    void dado_novo_cliente_pf_quando_salvar_com_cpf_valido_entao_persiste() {
        Cliente novo = Cliente.builder()
            .empresa(empresa).tipoPessoa("PF").nome("Maria Santos")
            .cpfCnpj("11144477735").ativo(true) // 111.444.777-35 — CPF válido
            .limiteCredito(BigDecimal.ZERO).creditoDisponivel(BigDecimal.ZERO)
            .build();

        when(clienteRepository.existsByEmpresaIdAndCpfCnpj(1, "11144477735"))
            .thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(novo);

        clienteService.salvar(novo);

        verify(clienteRepository, times(1)).save(novo);
    }

    // ---- CF-37: Cadastrar PJ com sucesso ----

    @Test
    void dado_cliente_pj_quando_salvar_com_cnpj_valido_entao_persiste() {
        when(clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "11222333000181", 2))
            .thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(clientePjBase);

        clienteService.salvar(clientePjBase);

        verify(clienteRepository, times(1)).save(clientePjBase);
    }

    @Test
    void dado_novo_cliente_pj_quando_salvar_entao_persiste() {
        Cliente novo = Cliente.builder()
            .empresa(empresa).tipoPessoa("PJ").nome("Google Brasil")
            .razaoSocial("Google Brasil Internet Ltda")
            .cpfCnpj("34028316000103").ativo(true)
            .limiteCredito(BigDecimal.ZERO).creditoDisponivel(BigDecimal.ZERO)
            .build();

        when(clienteRepository.existsByEmpresaIdAndCpfCnpj(1, "34028316000103"))
            .thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(novo);

        clienteService.salvar(novo);

        verify(clienteRepository, times(1)).save(novo);
    }

    // ---- CF-48: CPF duplicado na mesma empresa ----

    @Test
    void dado_cpf_duplicado_na_empresa_quando_salvar_entao_lanca_negocio_exception() {
        when(clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(true);

        assertThatThrownBy(() -> clienteService.salvar(clientePfBase))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CPF já cadastrado");

        verify(clienteRepository, never()).save(any());
    }

    // ---- CF-49: CNPJ duplicado na mesma empresa ----

    @Test
    void dado_cnpj_duplicado_na_empresa_quando_salvar_entao_lanca_negocio_exception() {
        when(clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "11222333000181", 2))
            .thenReturn(true);

        assertThatThrownBy(() -> clienteService.salvar(clientePjBase))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("CNPJ já cadastrado");

        verify(clienteRepository, never()).save(any());
    }

    // ---- CF-50: CPF duplicado em outra empresa não bloqueia ----

    @Test
    void dado_cpf_existente_em_outra_empresa_quando_salvar_entao_persiste_normalmente() {
        // existsByEmpresaIdAndCpfCnpjAndIdNot já filtra por empresaId — retorna false
        when(clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(clientePfBase);

        clienteService.salvar(clientePfBase);

        verify(clienteRepository, times(1)).save(clientePfBase);
    }

    // ---- CF-51: Editar sem duplicar consigo mesmo ----

    @Test
    void dado_cliente_existente_quando_editar_com_mesmo_cpf_entao_nao_lanca_excecao() {
        // id=1, CPF "12345678909" — deve ignorar o próprio registro
        when(clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(1, "12345678909", 1))
            .thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(clientePfBase);

        clienteService.salvar(clientePfBase);

        verify(clienteRepository, times(1)).save(clientePfBase);
    }

    // ---- CF-53: Inativar cliente ----

    @Test
    void dado_cliente_ativo_quando_inativar_entao_ativo_fica_false() {
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clientePfBase));

        clienteService.inativar(1);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void dado_id_inexistente_quando_inativar_entao_nao_lanca_excecao() {
        when(clienteRepository.findById(999)).thenReturn(Optional.empty());

        clienteService.inativar(999);

        verify(clienteRepository, never()).save(any());
    }

    // ---- CF-54: Ativar cliente ----

    @Test
    void dado_cliente_inativo_quando_ativar_entao_ativo_fica_true() {
        clientePfBase.setAtivo(false);
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clientePfBase));

        clienteService.ativar(1);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    // ---- CF-55: Verificar limite dentro do permitido ----

    @Test
    void dado_limite_1000_quando_verificar_venda_800_entao_retorna_true() {
        clientePfBase.setLimiteCredito(new BigDecimal("1000.00"));
        clientePfBase.setCreditoDisponivel(new BigDecimal("1000.00"));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clientePfBase));

        boolean resultado = clienteService.verificarLimiteCredito(1, new BigDecimal("800.00"));

        assertThat(resultado).isTrue();
    }

    @Test
    void dado_limite_1000_quando_verificar_venda_igual_ao_limite_entao_retorna_true() {
        clientePfBase.setLimiteCredito(new BigDecimal("1000.00"));
        clientePfBase.setCreditoDisponivel(new BigDecimal("1000.00"));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clientePfBase));

        boolean resultado = clienteService.verificarLimiteCredito(1, new BigDecimal("1000.00"));

        assertThat(resultado).isTrue();
    }

    // ---- CF-56: Verificar limite ultrapassado ----

    @Test
    void dado_limite_1000_quando_verificar_venda_1200_entao_retorna_false() {
        clientePfBase.setLimiteCredito(new BigDecimal("1000.00"));
        clientePfBase.setCreditoDisponivel(new BigDecimal("1000.00"));
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clientePfBase));

        boolean resultado = clienteService.verificarLimiteCredito(1, new BigDecimal("1200.00"));

        assertThat(resultado).isFalse();
    }

    // ---- CF-57: Limite zero significa sem restrição ----

    @Test
    void dado_limite_zero_quando_verificar_qualquer_valor_entao_retorna_true() {
        clientePfBase.setLimiteCredito(BigDecimal.ZERO);
        clientePfBase.setCreditoDisponivel(BigDecimal.ZERO);
        when(clienteRepository.findById(1)).thenReturn(Optional.of(clientePfBase));

        boolean resultado = clienteService.verificarLimiteCredito(1, new BigDecimal("999999.00"));

        assertThat(resultado).isTrue();
    }

    @Test
    void dado_cliente_inexistente_quando_verificar_limite_entao_retorna_true() {
        when(clienteRepository.findById(999)).thenReturn(Optional.empty());

        boolean resultado = clienteService.verificarLimiteCredito(999, new BigDecimal("500.00"));

        assertThat(resultado).isTrue();
    }

    // ---- Delegações simples ----

    @Test
    void quando_listar_todos_entao_delega_ao_repository() {
        when(clienteRepository.findByEmpresaIdOrderByNome(1))
            .thenReturn(List.of(clientePfBase, clientePjBase));

        List<Cliente> resultado = clienteService.listarTodos(1);

        assertThat(resultado).hasSize(2);
        verify(clienteRepository).findByEmpresaIdOrderByNome(1);
    }

    @Test
    void quando_listar_ativos_entao_delega_ao_repository() {
        when(clienteRepository.findByEmpresaIdAndAtivoTrueOrderByNome(1))
            .thenReturn(List.of(clientePfBase));

        List<Cliente> resultado = clienteService.listarAtivos(1);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void quando_contar_ativos_entao_delega_ao_repository() {
        when(clienteRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(5L);

        long total = clienteService.contarAtivos(1);

        assertThat(total).isEqualTo(5L);
    }

    @Test
    void quando_contar_com_limite_entao_delega_ao_repository() {
        when(clienteRepository.countByEmpresaIdAndLimiteCreditoGreaterThan(1, BigDecimal.ZERO))
            .thenReturn(3L);

        long total = clienteService.contarComLimite(1);

        assertThat(total).isEqualTo(3L);
    }
}
