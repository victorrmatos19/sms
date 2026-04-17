package com.erp.service;

import com.erp.model.Configuracao;
import com.erp.model.Empresa;
import com.erp.repository.ConfiguracaoRepository;
import com.erp.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do ConfiguracaoService.
 */
@ExtendWith(MockitoExtension.class)
class ConfiguracaoServiceTest {

    @Mock private ConfiguracaoRepository configuracaoRepository;
    @Mock private EmpresaRepository empresaRepository;

    @InjectMocks private ConfiguracaoService configuracaoService;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(1)
                .razaoSocial("Empresa Teste")
                .build();
    }

    // ---- 1. Sem config existente → cria padrão com tema DARK ----

    @Test
    void dado_empresa_sem_config_quando_buscar_entao_cria_padrao() {
        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.empty());
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));

        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        Configuracao salva = Configuracao.builder().empresa(empresa).build();
        when(configuracaoRepository.save(any())).thenReturn(salva);

        configuracaoService.buscarPorEmpresa(1);

        verify(configuracaoRepository).save(captor.capture());
        Configuracao nova = captor.getValue();
        // O builder default para tema é "DARK" (conforme definido na entidade)
        assertThat(nova.getTema()).isEqualTo("DARK");
    }

    // ---- 2. Config existente → retorna sem novo save ----

    @Test
    void dado_empresa_com_config_quando_buscar_entao_retorna_existente() {
        Configuracao existente = Configuracao.builder()
                .id(1)
                .empresa(empresa)
                .tema("LIGHT")
                .build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(existente));

        Configuracao resultado = configuracaoService.buscarPorEmpresa(1);

        verify(configuracaoRepository, never()).save(any());
        assertThat(resultado.getTema()).isEqualTo("LIGHT");
    }

    // ---- 3. salvarTema → persiste LIGHT ----

    @Test
    void dado_tema_light_quando_salvar_entao_persiste() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        when(configuracaoRepository.save(any())).thenReturn(config);

        configuracaoService.salvarTema(1, "LIGHT");

        verify(configuracaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTema()).isEqualTo("LIGHT");
    }

    // ---- 4. salvarTema → persiste DARK ----

    @Test
    void dado_tema_dark_quando_salvar_entao_persiste() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("LIGHT").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        when(configuracaoRepository.save(any())).thenReturn(config);

        configuracaoService.salvarTema(1, "DARK");

        verify(configuracaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTema()).isEqualTo("DARK");
    }

    // ---- 5. isModoEscuro → true quando tema DARK ----

    @Test
    void dado_tema_dark_quando_verificar_modo_escuro_entao_true() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        assertThat(configuracaoService.isModoEscuro(1)).isTrue();
    }

    // ---- 6. isModoEscuro → false quando tema LIGHT ----

    @Test
    void dado_tema_light_quando_verificar_modo_escuro_entao_false() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("LIGHT").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        assertThat(configuracaoService.isModoEscuro(1)).isFalse();
    }

    // ---- CF-213: buscar configuração de empresa inexistente cria padrão ----
    // Já coberto pelo teste 1: dado_empresa_sem_config_quando_buscar_entao_cria_padrao

    // ---- CF-214: tema da empresa 1 não afeta empresa 2 ----

    @Test
    void dado_tema_dark_empresa_1_quando_buscar_empresa_2_entao_retorna_config_da_empresa_2() {
        Empresa empresa2 = Empresa.builder().id(2).razaoSocial("Empresa 2").build();
        Configuracao configEmpresa1 = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK").build();
        Configuracao configEmpresa2 = Configuracao.builder()
                .id(2).empresa(empresa2).tema("LIGHT").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(configEmpresa1));
        when(configuracaoRepository.findByEmpresaId(2)).thenReturn(Optional.of(configEmpresa2));

        Configuracao c1 = configuracaoService.buscarPorEmpresa(1);
        Configuracao c2 = configuracaoService.buscarPorEmpresa(2);

        assertThat(c1.getTema()).isEqualTo("DARK");
        assertThat(c2.getTema()).isEqualTo("LIGHT");
    }

    // ---- CF-215: usa_lote_validade propagação ----
    // ConfiguracaoService retorna a entidade Configuracao completa com o campo usaLoteValidade.
    // EstoqueService e ProdutoService devem consultar este valor ao criar produtos/movimentações.
    // A responsabilidade de propagação é do controller ou service de produto — gap documentado.

    // ---- CF-216: alerta_estoque_minimo = false ----

    @Test
    void dado_config_com_alerta_estoque_minimo_false_quando_buscar_entao_retorna_flag() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK")
                .alertaEstoqueMinimo(false).build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        Configuracao resultado = configuracaoService.buscarPorEmpresa(1);

        assertThat(resultado.getAlertaEstoqueMinimo()).isFalse();
    }

    // ---- CF-217: permite_venda_estoque_zero propagação ----

    @Test
    void dado_config_com_permite_venda_estoque_zero_true_quando_buscar_entao_retorna_flag() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK")
                .permiteVendaEstoqueZero(true).build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        Configuracao resultado = configuracaoService.buscarPorEmpresa(1);

        assertThat(resultado.getPermiteVendaEstoqueZero()).isTrue();
    }

    // ---- CF-218: dias_validade_orcamento retornado pela configuração ----

    @Test
    void dado_config_com_dias_validade_orcamento_30_quando_buscar_entao_retorna_valor() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK")
                .diasValidadeOrcamento(30).build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        Configuracao resultado = configuracaoService.buscarPorEmpresa(1);

        assertThat(resultado.getDiasValidadeOrcamento()).isEqualTo(30);
    }

    // ---- isModoEscuro sem config → padrão dark ----

    @Test
    void dado_sem_config_quando_verificar_modo_escuro_entao_retorna_true_como_padrao() {
        when(configuracaoRepository.findByEmpresaId(99)).thenReturn(Optional.empty());

        assertThat(configuracaoService.isModoEscuro(99)).isTrue();
    }
}
