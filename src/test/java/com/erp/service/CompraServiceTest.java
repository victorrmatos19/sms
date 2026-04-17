package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do CompraService.
 * CF-117 a CF-137.
 */
@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock private CompraRepository          compraRepository;
    @Mock private ContaPagarRepository      contaPagarRepository;
    @Mock private ProdutoRepository         produtoRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private LoteRepository            loteRepository;
    @Mock private AuthService               authService;

    @InjectMocks private CompraService compraService;

    private Empresa   empresa;
    private Fornecedor fornecedor;
    private Produto   produto;
    private CompraItem item;
    private Compra    compraRascunho;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(1).razaoSocial("Empresa Teste")
                .cnpj("00.000.000/0001-00")
                .regimeTributario("SIMPLES_NACIONAL").build();

        fornecedor = Fornecedor.builder()
                .id(1).empresa(empresa).nome("Fornecedor Teste")
                .tipoPessoa("PJ").ativo(true).build();

        produto = Produto.builder()
                .id(1).empresa(empresa)
                .descricao("Arroz Tipo 1")
                .precoVenda(new BigDecimal("24.90"))
                .precoCusto(new BigDecimal("18.50"))
                .estoqueAtual(new BigDecimal("50"))
                .estoqueMinimo(new BigDecimal("20"))
                .usaLoteValidade(false)
                .ativo(true).build();

        item = CompraItem.builder()
                .produto(produto)
                .quantidade(new BigDecimal("30"))
                .custoUnitario(new BigDecimal("18.50"))
                .desconto(BigDecimal.ZERO)
                .valorTotal(new BigDecimal("555.00")).build();

        compraRascunho = Compra.builder()
                .id(1).empresa(empresa)
                .fornecedor(fornecedor)
                .status("RASCUNHO")
                .numeroDocumento("C2026-00001")
                .dataEmissao(LocalDate.now())
                .condicaoPagamento("A_VISTA")
                .valorTotal(new BigDecimal("555.00"))
                .itens(new ArrayList<>(List.of(item))).build();
        item.setCompra(compraRascunho);
    }

    // ---- CF-117: Fornecedor obrigatório ----

    @Test
    void dado_compra_sem_fornecedor_quando_salvar_rascunho_entao_lanca_negocio_exception() {
        Compra semFornecedor = Compra.builder()
                .empresa(empresa)
                .itens(new ArrayList<>(List.of(item)))
                .build();

        assertThatThrownBy(() -> compraService.salvarRascunho(semFornecedor))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Fornecedor");

        verify(compraRepository, never()).save(any());
    }

    // ---- CF-118: Pelo menos um item ----

    @Test
    void dado_compra_sem_itens_quando_salvar_rascunho_entao_lanca_negocio_exception() {
        Compra semItens = Compra.builder()
                .empresa(empresa)
                .fornecedor(fornecedor)
                .itens(new ArrayList<>())
                .build();

        assertThatThrownBy(() -> compraService.salvarRascunho(semItens))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("item");

        verify(compraRepository, never()).save(any());
    }

    // ---- CF-119: Sugestão de último custo ----

    @Test
    void dado_produto_ja_comprado_quando_buscar_ultimo_custo_entao_retorna_valor() {
        when(compraRepository.findUltimoCustoProduto(1, 1))
                .thenReturn(Optional.of(new BigDecimal("18.50")));

        Optional<BigDecimal> resultado = compraService.buscarUltimoCusto(1, 1);

        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualByComparingTo("18.50");
    }

    // ---- CF-120: Produto nunca comprado ----

    @Test
    void dado_produto_nunca_comprado_quando_buscar_ultimo_custo_entao_retorna_vazio() {
        when(compraRepository.findUltimoCustoProduto(1, 1))
                .thenReturn(Optional.empty());

        Optional<BigDecimal> resultado = compraService.buscarUltimoCusto(1, 1);

        assertThat(resultado).isEmpty();
    }

    // ---- CF-121: Cálculo total da linha ----

    @Test
    void dado_quantidade_10_custo_18_50_desconto_5_quando_calcular_total_entao_retorna_180() {
        CompraItem ci = CompraItem.builder()
                .quantidade(new BigDecimal("10"))
                .custoUnitario(new BigDecimal("18.50"))
                .desconto(new BigDecimal("5.00"))
                .valorTotal(BigDecimal.ZERO)
                .build();

        ci.recalcularTotal();

        assertThat(ci.getValorTotal()).isEqualByComparingTo("180.00");
    }

    // ---- CF-122: Cálculo total geral ----

    @Test
    void dado_itens_frete_desconto_quando_recalcular_totais_entao_valor_total_correto() {
        CompraItem ci = CompraItem.builder()
                .produto(produto)
                .quantidade(new BigDecimal("15"))
                .custoUnitario(new BigDecimal("18.60"))
                .desconto(BigDecimal.ZERO)
                .valorTotal(new BigDecimal("279.00")).build();

        Compra compra = Compra.builder()
                .empresa(empresa)
                .fornecedor(fornecedor)
                .valorFrete(new BigDecimal("15.00"))
                .valorDesconto(new BigDecimal("10.00"))
                .valorOutras(BigDecimal.ZERO)
                .itens(new ArrayList<>(List.of(ci))).build();

        compraService.recalcularTotais(compra);

        assertThat(compra.getValorTotal()).isEqualByComparingTo("284.00");
    }

    // ---- CF-123: Confirmar compra sucesso ----

    @Test
    void dado_compra_rascunho_quando_confirmar_entao_status_confirmada_e_repos_chamados() {
        when(compraRepository.findById(1)).thenReturn(Optional.of(compraRascunho));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(contaPagarRepository.save(any())).thenReturn(new ContaPagar());
        when(compraRepository.save(any())).thenReturn(compraRascunho);
        when(authService.getUsuarioLogado()).thenReturn(null);

        compraService.confirmarCompra(1, 1);

        assertThat(compraRascunho.getStatus()).isEqualTo("CONFIRMADA");
        verify(movimentacaoEstoqueRepository, times(1)).save(any());
        verify(contaPagarRepository, times(1)).save(any());
    }

    // ---- CF-124: Confirmar apenas rascunho ----

    @Test
    void dado_compra_confirmada_quando_tentar_confirmar_entao_lanca_negocio_exception() {
        Compra confirmada = Compra.builder()
                .id(2).empresa(empresa).fornecedor(fornecedor)
                .status("CONFIRMADA")
                .itens(new ArrayList<>(List.of(item))).build();

        when(compraRepository.findById(2)).thenReturn(Optional.of(confirmada));

        assertThatThrownBy(() -> compraService.confirmarCompra(2, 1))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Rascunho");
    }

    // ---- CF-125: Movimentação de estoque ao confirmar ----

    @Test
    void dado_produto_estoque_50_item_30_quando_confirmar_entao_movimentacao_com_saldos_corretos() {
        when(compraRepository.findById(1)).thenReturn(Optional.of(compraRascunho));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(contaPagarRepository.save(any())).thenReturn(new ContaPagar());
        when(compraRepository.save(any())).thenReturn(compraRascunho);
        when(authService.getUsuarioLogado()).thenReturn(null);

        compraService.confirmarCompra(1, 1);

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());
        assertThat(captor.getValue().getSaldoAnterior()).isEqualByComparingTo("50");
        assertThat(captor.getValue().getSaldoPosterior()).isEqualByComparingTo("80");
        assertThat(captor.getValue().getTipo()).isEqualTo("ENTRADA");
        assertThat(captor.getValue().getOrigem()).isEqualTo("COMPRA");
    }

    // ---- CF-126: Atualiza custo do produto ----

    @Test
    void dado_compra_com_custo_18_50_quando_confirmar_entao_produto_custo_atualizado() {
        when(compraRepository.findById(1)).thenReturn(Optional.of(compraRascunho));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(contaPagarRepository.save(any())).thenReturn(new ContaPagar());
        when(compraRepository.save(any())).thenReturn(compraRascunho);
        when(authService.getUsuarioLogado()).thenReturn(null);

        compraService.confirmarCompra(1, 1);

        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(captor.capture());
        assertThat(captor.getValue().getPrecoCusto()).isEqualByComparingTo("18.50");
    }

    // ---- CF-127: Contas a pagar A_VISTA ----

    @Test
    void dado_compra_a_vista_valor_555_quando_confirmar_entao_uma_conta_a_pagar_no_vencimento_hoje() {
        when(compraRepository.findById(1)).thenReturn(Optional.of(compraRascunho));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(contaPagarRepository.save(any())).thenReturn(new ContaPagar());
        when(compraRepository.save(any())).thenReturn(compraRascunho);
        when(authService.getUsuarioLogado()).thenReturn(null);

        compraService.confirmarCompra(1, 1);

        ArgumentCaptor<ContaPagar> captor = ArgumentCaptor.forClass(ContaPagar.class);
        verify(contaPagarRepository, times(1)).save(captor.capture());

        ContaPagar conta = captor.getValue();
        assertThat(conta.getValor()).isEqualByComparingTo("555.00");
        assertThat(conta.getDataVencimento()).isEqualTo(LocalDate.now());
        assertThat(conta.getNumeroParcela()).isEqualTo(1);
        assertThat(conta.getTotalParcelas()).isEqualTo(1);
    }

    // ---- CF-128: Contas a pagar 30/60/90 ----

    @Test
    void dado_compra_30_60_90_valor_300_quando_confirmar_entao_tres_contas_a_pagar() {
        Compra compra30_60_90 = Compra.builder()
                .id(3).empresa(empresa).fornecedor(fornecedor)
                .status("RASCUNHO")
                .numeroDocumento("C2026-00003")
                .dataEmissao(LocalDate.now())
                .condicaoPagamento("30_60_90_DIAS")
                .valorTotal(new BigDecimal("300.00"))
                .itens(new ArrayList<>(List.of(item))).build();
        item.setCompra(compra30_60_90);

        when(compraRepository.findById(3)).thenReturn(Optional.of(compra30_60_90));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(contaPagarRepository.save(any())).thenReturn(new ContaPagar());
        when(compraRepository.save(any())).thenReturn(compra30_60_90);
        when(authService.getUsuarioLogado()).thenReturn(null);

        compraService.confirmarCompra(3, 3);

        verify(contaPagarRepository, times(3)).save(any());

        ArgumentCaptor<ContaPagar> captor = ArgumentCaptor.forClass(ContaPagar.class);
        verify(contaPagarRepository, times(3)).save(captor.capture());
        List<ContaPagar> contas = captor.getAllValues();
        assertThat(contas.get(0).getDataVencimento()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(contas.get(1).getDataVencimento()).isEqualTo(LocalDate.now().plusDays(60));
        assertThat(contas.get(2).getDataVencimento()).isEqualTo(LocalDate.now().plusDays(90));
    }

    // ---- CF-132: Cancelar rascunho ----

    @Test
    void dado_compra_rascunho_quando_cancelar_entao_status_cancelada_sem_movimentacao() {
        when(compraRepository.findById(1)).thenReturn(Optional.of(compraRascunho));
        when(compraRepository.save(any())).thenReturn(compraRascunho);

        compraService.cancelarCompra(1);

        assertThat(compraRascunho.getStatus()).isEqualTo("CANCELADA");
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    // ---- CF-133: Cancelar confirmada lança exceção ----

    @Test
    void dado_compra_confirmada_quando_cancelar_entao_lanca_negocio_exception() {
        Compra confirmada = Compra.builder()
                .id(2).empresa(empresa).fornecedor(fornecedor)
                .status("CONFIRMADA")
                .itens(new ArrayList<>(List.of(item))).build();

        when(compraRepository.findById(2)).thenReturn(Optional.of(confirmada));

        assertThatThrownBy(() -> compraService.cancelarCompra(2))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Rascunho");
    }

    // ---- CF-134: Geração de número ----

    @Test
    void dado_sem_compras_quando_gerar_numero_entao_retorna_primeiro_numero() {
        when(compraRepository.countByEmpresaId(1)).thenReturn(0L);
        int ano = LocalDate.now().getYear();

        String numero = compraService.gerarNumeroCompra(1);

        assertThat(numero).isEqualTo("C" + ano + "-00001");
    }

    @Test
    void dado_cinco_compras_existentes_quando_gerar_numero_entao_retorna_sexto() {
        when(compraRepository.countByEmpresaId(1)).thenReturn(5L);
        int ano = LocalDate.now().getYear();

        String numero = compraService.gerarNumeroCompra(1);

        assertThat(numero).isEqualTo("C" + ano + "-00006");
    }

    // ---- CF-135: calcularVencimentos A_VISTA ----

    @Test
    void dado_condicao_a_vista_quando_calcular_vencimentos_entao_retorna_data_base() {
        LocalDate base = LocalDate.of(2026, 4, 1);

        List<LocalDate> vencimentos = compraService.calcularVencimentos(base, "A_VISTA", 1);

        assertThat(vencimentos).containsExactly(LocalDate.of(2026, 4, 1));
    }

    // ---- CF-136: calcularVencimentos 30_60_DIAS ----

    @Test
    void dado_condicao_30_60_quando_calcular_vencimentos_entao_retorna_duas_datas() {
        LocalDate base = LocalDate.of(2026, 4, 1);

        List<LocalDate> vencimentos = compraService.calcularVencimentos(base, "30_60_DIAS", 2);

        assertThat(vencimentos).hasSize(2);
        assertThat(vencimentos.get(0)).isEqualTo(base.plusDays(30));
        assertThat(vencimentos.get(1)).isEqualTo(base.plusDays(60));
    }

    // ---- CF-137: calcularVencimentos PERSONALIZADO ----

    @Test
    void dado_condicao_personalizado_3_parcelas_quando_calcular_vencimentos_entao_tres_datas() {
        LocalDate base = LocalDate.of(2026, 4, 1);

        List<LocalDate> vencimentos = compraService.calcularVencimentos(base, "PERSONALIZADO", 3);

        assertThat(vencimentos).hasSize(3);
        assertThat(vencimentos.get(0)).isEqualTo(base.plusDays(30));
        assertThat(vencimentos.get(1)).isEqualTo(base.plusDays(60));
        assertThat(vencimentos.get(2)).isEqualTo(base.plusDays(90));
    }

    // ---- CF-201: confirmar compra sem itens deve lançar exceção ----

    @Test
    void dado_compra_rascunho_sem_itens_quando_confirmar_entao_lanca_negocio_exception() {
        Compra semItens = Compra.builder()
                .id(5).empresa(empresa).fornecedor(fornecedor)
                .status("RASCUNHO")
                .numeroDocumento("C2026-00005")
                .dataEmissao(LocalDate.now())
                .condicaoPagamento("A_VISTA")
                .valorTotal(BigDecimal.ZERO)
                .itens(new ArrayList<>()).build();

        when(compraRepository.findById(5)).thenReturn(Optional.of(semItens));

        assertThatThrownBy(() -> compraService.confirmarCompra(5, 1))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("itens");

        verify(movimentacaoEstoqueRepository, never()).save(any());
        verify(contaPagarRepository, never()).save(any());
    }

    // ---- CF-202: confirmar compra já CONFIRMADA é idempotência (lança exceção) ----
    // Já coberto por CF-124: dado_compra_confirmada_quando_tentar_confirmar_entao_lanca_negocio_exception

    // ---- CF-203: cancelar compra CONFIRMADA deve reverter estoque ----
    // GAP: cancelarCompra() atual só aceita RASCUNHO. Compras CONFIRMADAS não podem ser canceladas
    // pelo serviço — é necessário um fluxo de devolução. Documentado como gap de negócio.

    // ---- CF-204: cancelar compra CANCELADA deve lançar exceção ----

    @Test
    void dado_compra_cancelada_quando_cancelar_novamente_entao_lanca_negocio_exception() {
        Compra cancelada = Compra.builder()
                .id(9).empresa(empresa).fornecedor(fornecedor)
                .status("CANCELADA")
                .itens(new ArrayList<>(List.of(item))).build();

        when(compraRepository.findById(9)).thenReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> compraService.cancelarCompra(9))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Rascunho");
    }

    // ---- CF-206: confirmar compra A_VISTA gera parcela com vencimento = emissão ----
    // Já coberto por CF-127

    // ---- CF-207: confirmar compra 30_60_90 gera 3 parcelas nos dias corretos ----
    // Já coberto por CF-128

    // ---- CF-208: calcularVencimentos PERSONALIZADO com 0 parcelas ----

    @Test
    void dado_condicao_personalizado_zero_parcelas_quando_calcular_entao_retorna_uma_data() {
        // Math.max(1, 0) = 1, então o service garante mínimo 1 parcela
        LocalDate base = LocalDate.of(2026, 4, 1);

        List<LocalDate> vencimentos = compraService.calcularVencimentos(base, "PERSONALIZADO", 0);

        assertThat(vencimentos).hasSize(1);
        assertThat(vencimentos.get(0)).isEqualTo(base.plusDays(30));
    }

    // ---- CF-209: valor_total da compra ----
    // Já coberto por CF-122: recalcularTotais

    // ---- CF-210: custo do produto atualizado ao confirmar ----
    // Já coberto por CF-126

    // ---- CF-211/212: numeração sequencial por empresa ----

    @Test
    void dado_compra_de_empresa_2_quando_gerar_numero_entao_usa_contagem_da_empresa_2() {
        when(compraRepository.countByEmpresaId(2)).thenReturn(3L);
        int ano = LocalDate.now().getYear();

        String numero = compraService.gerarNumeroCompra(2);

        assertThat(numero).isEqualTo("C" + ano + "-00004");
        verify(compraRepository).countByEmpresaId(2);
        // Empresa 1 não é consultada — isolamento por empresa confirmado
        verify(compraRepository, never()).countByEmpresaId(1);
    }

    // ---- CF-205: movimentação de cancelamento ----
    // GAP: cancelarCompra() não cria movimentação de SAIDA / CANCELAMENTO_COMPRA.
    // Atualmente apenas muda status para CANCELADA sem estornar estoque.
    // Recomendação: implementar cancelamento de compra confirmada com estorno de estoque
    // e cancelamento de parcelas ABERTA em ContaPagar.
}
