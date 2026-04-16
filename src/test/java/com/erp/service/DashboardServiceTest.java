package com.erp.service;

import com.erp.model.Produto;
import com.erp.model.Empresa;
import com.erp.model.Venda;
import com.erp.model.VendaPagamento;
import com.erp.model.dto.caixa.CaixaResumoDTO;
import com.erp.model.dto.dashboard.DashboardAdminDTO;
import com.erp.model.dto.dashboard.DashboardEstoqueDTO;
import com.erp.model.dto.dashboard.VendaDiariaDTO;
import com.erp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do DashboardService.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private ContaPagarRepository contaPagarRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private VendaRepository vendaRepository;
    @Mock private CaixaService caixaService;

    @InjectMocks private DashboardService dashboardService;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder().id(1).razaoSocial("Empresa Teste").build();
    }

    // ---- 1. carregarAdmin: sem compras/contas → retorna zeros ----

    @Test
    void dado_sem_compras_hoje_quando_carregar_admin_entao_retorna_zero() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        when(compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                eq(1), eq("CONFIRMADA"), eq(inicioMes), eq(fimMes)))
                .thenReturn(0L);
        when(compraRepository.findByEmpresaIdAndStatus(1, "CONFIRMADA"))
                .thenReturn(List.of());
        when(compraRepository.countByEmpresaIdAndStatus(1, "RASCUNHO"))
                .thenReturn(0L);
        when(contaPagarRepository.countVencendoHoje(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.sumValorVencendoHoje(1, hoje)).thenReturn(BigDecimal.ZERO);
        when(contaPagarRepository.countVencidas(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.countByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(0L);
        when(contaPagarRepository.sumValorByEmpresaIdAndStatus(1, "ABERTA"))
                .thenReturn(BigDecimal.ZERO);
        when(produtoRepository.findByEmpresaIdAndEstoqueZerado(1)).thenReturn(List.of());
        when(produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(1)).thenReturn(List.of());
        when(produtoRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(0L);
        stubVendasDashboard(hoje, List.of(), List.of(), List.of());
        stubCaixaFechado();

        DashboardAdminDTO dto = dashboardService.carregarAdmin(1);

        assertThat(dto.comprasMes()).isZero();
        assertThat(dto.valorComprasMes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.vendasHoje()).isZero();
        assertThat(dto.valorVendasHoje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.contasPagarHoje()).isZero();
        assertThat(dto.caixaAberto()).isFalse();
        assertThat(dto.saldoCaixaAtual()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.totalProdutosAtivos()).isZero();
    }

    // ---- 2. carregarAdmin: produtos zerados/abaixo do mínimo são contados ----

    @Test
    void dado_produtos_zerados_quando_carregar_admin_entao_conta() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        Produto zerado = Produto.builder()
                .id(1).empresa(empresa).descricao("Produto Zerado")
                .estoqueAtual(BigDecimal.ZERO)
                .estoqueMinimo(new BigDecimal("10"))
                .precoCusto(new BigDecimal("5.00"))
                .usaLoteValidade(false).ativo(true).build();

        Produto abaixo = Produto.builder()
                .id(2).empresa(empresa).descricao("Produto Baixo")
                .estoqueAtual(new BigDecimal("3"))
                .estoqueMinimo(new BigDecimal("10"))
                .precoCusto(new BigDecimal("5.00"))
                .usaLoteValidade(false).ativo(true).build();

        when(compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                eq(1), eq("CONFIRMADA"), eq(inicioMes), eq(fimMes)))
                .thenReturn(0L);
        when(compraRepository.findByEmpresaIdAndStatus(1, "CONFIRMADA")).thenReturn(List.of());
        when(compraRepository.countByEmpresaIdAndStatus(1, "RASCUNHO")).thenReturn(0L);
        when(contaPagarRepository.countVencendoHoje(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.sumValorVencendoHoje(1, hoje)).thenReturn(BigDecimal.ZERO);
        when(contaPagarRepository.countVencidas(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.countByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(0L);
        when(contaPagarRepository.sumValorByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(BigDecimal.ZERO);
        when(produtoRepository.findByEmpresaIdAndEstoqueZerado(1)).thenReturn(List.of(zerado));
        when(produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(1)).thenReturn(List.of(zerado, abaixo));
        when(produtoRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(2L);
        stubVendasDashboard(hoje, List.of(), List.of(), List.of());
        stubCaixaFechado();

        DashboardAdminDTO dto = dashboardService.carregarAdmin(1);

        assertThat(dto.produtosEstoqueZerado()).isEqualTo(1L);
        assertThat(dto.produtosAbaixoMinimo()).isEqualTo(2L);
        assertThat(dto.totalProdutosAtivos()).isEqualTo(2L);
        // Alertas devem incluir mensagem de estoque zerado
        assertThat(dto.alertas()).anyMatch(a -> a.mensagem().contains("estoque zerado"));
    }

    @Test
    void dado_vendas_hoje_quando_carregar_admin_entao_preenche_card_e_modal() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        Venda venda1 = Venda.builder()
                .numero("V2026-00001")
                .dataVenda(hoje.atTime(9, 30))
                .status("FINALIZADA")
                .valorTotal(new BigDecimal("80.00"))
                .pagamentos(List.of(VendaPagamento.builder().formaPagamento("PIX").build()))
                .build();
        Venda venda2 = Venda.builder()
                .numero("V2026-00002")
                .dataVenda(hoje.atTime(14, 15))
                .status("FINALIZADA")
                .valorTotal(new BigDecimal("20.00"))
                .pagamentos(List.of(VendaPagamento.builder().formaPagamento("DINHEIRO").build()))
                .build();

        when(compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                eq(1), eq("CONFIRMADA"), eq(inicioMes), eq(fimMes)))
                .thenReturn(0L);
        when(compraRepository.findByEmpresaIdAndStatus(1, "CONFIRMADA")).thenReturn(List.of());
        when(compraRepository.countByEmpresaIdAndStatus(1, "RASCUNHO")).thenReturn(0L);
        when(contaPagarRepository.countVencendoHoje(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.sumValorVencendoHoje(1, hoje)).thenReturn(BigDecimal.ZERO);
        when(contaPagarRepository.countVencidas(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.countByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(0L);
        when(contaPagarRepository.sumValorByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(BigDecimal.ZERO);
        when(produtoRepository.findByEmpresaIdAndEstoqueZerado(1)).thenReturn(List.of());
        when(produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(1)).thenReturn(List.of());
        when(produtoRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(0L);
        stubVendasDashboard(hoje, List.of(venda1, venda2),
                List.of(new VendaDiariaDTO(hoje, new BigDecimal("100.00"), 2L)),
                List.<Object[]>of(new Object[]{"Produto A", new BigDecimal("100.00")}));
        when(caixaService.obterResumoAtual(1)).thenReturn(new CaixaResumoDTO(
                true, 20, "Caixa Principal", "Admin", hoje.atStartOfDay(), null,
                new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("30.00"),
                new BigDecimal("120.00"), null, null));

        DashboardAdminDTO dto = dashboardService.carregarAdmin(1);

        assertThat(dto.vendasHoje()).isEqualTo(2L);
        assertThat(dto.valorVendasHoje()).isEqualByComparingTo("100.00");
        assertThat(dto.ticketMedioUltimos7Dias()).isEqualByComparingTo("50.00");
        assertThat(dto.vendasHojeDetalhes()).hasSize(2);
        assertThat(dto.vendasHojeDetalhes().get(0).formaPagamento()).isEqualTo("PIX");
        assertThat(dto.topProdutos()).extracting("nome").containsExactly("Produto A");
        assertThat(dto.caixaAberto()).isTrue();
        assertThat(dto.saldoCaixaAtual()).isEqualByComparingTo("120.00");
        assertThat(dto.entradasCaixaAtual()).isEqualByComparingTo("100.00");
        assertThat(dto.saidasCaixaAtual()).isEqualByComparingTo("30.00");
    }

    @Test
    void dado_vendas_hoje_e_ontem_quando_carregar_admin_entao_grafico_inclui_os_dois_dias() {
        LocalDate hoje = LocalDate.now();
        LocalDate ontem = hoje.minusDays(1);
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        Venda vendaHoje = Venda.builder()
                .numero("V2026-00003")
                .dataVenda(hoje.atTime(10, 0))
                .status("FINALIZADA")
                .valorTotal(new BigDecimal("120.00"))
                .pagamentos(List.of(VendaPagamento.builder().formaPagamento("PIX").build()))
                .build();

        when(compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                eq(1), eq("CONFIRMADA"), eq(inicioMes), eq(fimMes)))
                .thenReturn(0L);
        when(compraRepository.findByEmpresaIdAndStatus(1, "CONFIRMADA")).thenReturn(List.of());
        when(compraRepository.countByEmpresaIdAndStatus(1, "RASCUNHO")).thenReturn(0L);
        when(contaPagarRepository.countVencendoHoje(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.sumValorVencendoHoje(1, hoje)).thenReturn(BigDecimal.ZERO);
        when(contaPagarRepository.countVencidas(1, hoje)).thenReturn(0L);
        when(contaPagarRepository.countByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(0L);
        when(contaPagarRepository.sumValorByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(BigDecimal.ZERO);
        when(produtoRepository.findByEmpresaIdAndEstoqueZerado(1)).thenReturn(List.of());
        when(produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(1)).thenReturn(List.of());
        when(produtoRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(0L);
        stubVendasDashboard(hoje, List.of(vendaHoje), List.of(
                new VendaDiariaDTO(ontem, new BigDecimal("80.00"), 1L),
                new VendaDiariaDTO(hoje, new BigDecimal("120.00"), 1L)
        ), List.of());
        stubCaixaFechado();

        DashboardAdminDTO dto = dashboardService.carregarAdmin(1);

        assertThat(dto.vendasSemana()).hasSize(7);
        assertThat(dto.vendasSemana()).anySatisfy(dia -> {
            assertThat(dia.data()).isEqualTo(ontem);
            assertThat(dia.total()).isEqualByComparingTo("80.00");
            assertThat(dia.quantidade()).isEqualTo(1L);
        });
        assertThat(dto.vendasSemana()).anySatisfy(dia -> {
            assertThat(dia.data()).isEqualTo(hoje);
            assertThat(dia.total()).isEqualByComparingTo("120.00");
            assertThat(dia.quantidade()).isEqualTo(1L);
        });
        assertThat(dto.ticketMedioUltimos7Dias()).isEqualByComparingTo("100.00");
    }

    // ---- 3. carregarEstoque: calcula corretamente as contagens ----

    @Test
    void dado_valor_total_estoque_quando_carregar_estoque_entao_calcula() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusSeconds(1);

        Produto abaixo = Produto.builder()
                .id(1).empresa(empresa).descricao("Produto Baixo")
                .estoqueAtual(new BigDecimal("5"))
                .estoqueMinimo(new BigDecimal("10"))
                .precoCusto(new BigDecimal("8.00"))
                .usaLoteValidade(false).ativo(true).build();

        when(produtoRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(10L);
        when(produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(1)).thenReturn(List.of(abaixo));
        when(movimentacaoEstoqueRepository
                .countByEmpresaIdAndTipoAndCriadoEmBetween(eq(1), eq("ENTRADA"), any(), any()))
                .thenReturn(5L);
        when(movimentacaoEstoqueRepository
                .countByEmpresaIdAndTipoAndCriadoEmBetween(eq(1), eq("SAIDA"), any(), any()))
                .thenReturn(3L);

        DashboardEstoqueDTO dto = dashboardService.carregarEstoque(1);

        assertThat(dto.totalProdutosAtivos()).isEqualTo(10L);
        assertThat(dto.produtosAbaixoMinimo()).isEqualTo(1L);
        assertThat(dto.entradasMes()).isEqualTo(5L);
        assertThat(dto.saidasMes()).isEqualTo(3L);
    }

    private void stubVendasDashboard(LocalDate hoje,
                                     List<Venda> vendasHoje,
                                     List<VendaDiariaDTO> vendasSemana,
                                     List<Object[]> topProdutos) {
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        when(vendaRepository.findByEmpresaIdAndStatusAndPeriodoComRelacionamentos(
                eq(1), eq("FINALIZADA"), eq(hoje.atStartOfDay()), eq(hoje.atTime(LocalTime.MAX))))
                .thenReturn(vendasHoje);
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            VendaDiariaDTO vendaDia = vendasSemana.stream()
                    .filter(v -> v.data().equals(dia))
                    .findFirst()
                    .orElse(new VendaDiariaDTO(dia, BigDecimal.ZERO, 0L));
            when(vendaRepository.countByEmpresaIdAndStatusAndDataVendaBetween(
                    eq(1), eq("FINALIZADA"), eq(dia.atStartOfDay()), eq(dia.atTime(LocalTime.MAX))))
                    .thenReturn(vendaDia.quantidade());
            when(vendaRepository.sumValorTotalByEmpresaIdAndStatusAndDataVendaBetween(
                    eq(1), eq("FINALIZADA"), eq(dia.atStartOfDay()), eq(dia.atTime(LocalTime.MAX))))
                    .thenReturn(vendaDia.total());
        }
        when(vendaRepository.findTopProdutosVendidosPeriodo(
                eq(1), eq(inicioMes.atStartOfDay()), eq(fimMes.atTime(LocalTime.MAX)), any()))
                .thenReturn(topProdutos);
    }

    private void stubCaixaFechado() {
        when(caixaService.obterResumoAtual(1)).thenReturn(new CaixaResumoDTO(
                false, null, "Caixa Principal", "—", null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null));
    }
}
