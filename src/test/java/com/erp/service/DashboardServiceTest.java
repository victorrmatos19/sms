package com.erp.service;

import com.erp.model.Produto;
import com.erp.model.Empresa;
import com.erp.model.dto.dashboard.DashboardAdminDTO;
import com.erp.model.dto.dashboard.DashboardEstoqueDTO;
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
        when(compraRepository.findTopProdutosMes(eq(1), any())).thenReturn(List.of());

        DashboardAdminDTO dto = dashboardService.carregarAdmin(1);

        assertThat(dto.comprasMes()).isZero();
        assertThat(dto.valorComprasMes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.contasPagarHoje()).isZero();
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
        when(compraRepository.findTopProdutosMes(eq(1), any())).thenReturn(List.of());

        DashboardAdminDTO dto = dashboardService.carregarAdmin(1);

        assertThat(dto.produtosEstoqueZerado()).isEqualTo(1L);
        assertThat(dto.produtosAbaixoMinimo()).isEqualTo(2L);
        assertThat(dto.totalProdutosAtivos()).isEqualTo(2L);
        // Alertas devem incluir mensagem de estoque zerado
        assertThat(dto.alertas()).anyMatch(a -> a.mensagem().contains("estoque zerado"));
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
}
