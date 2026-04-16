package com.erp.service;

import com.erp.model.Produto;
import com.erp.model.Venda;
import com.erp.model.VendaPagamento;
import com.erp.model.dto.caixa.CaixaResumoDTO;
import com.erp.model.dto.dashboard.*;
import com.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompraRepository compraRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final VendaRepository vendaRepository;
    private final CaixaService caixaService;

    private static final String STATUS_VENDA_FINALIZADA = "FINALIZADA";

    // -----------------------------------------------------------------------
    // ADMINISTRADOR
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardAdminDTO carregarAdmin(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje = hoje.atTime(LocalTime.MAX);
        LocalDateTime inicioMesVenda = inicioMes.atStartOfDay();
        LocalDateTime fimMesVenda = fimMes.atTime(LocalTime.MAX);

        // ---- Compras ----
        long comprasMes = compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                empresaId, "CONFIRMADA", inicioMes, fimMes);

        BigDecimal valorComprasMes = compraRepository
                .findByEmpresaIdAndStatus(empresaId, "CONFIRMADA")
                .stream()
                .filter(c -> !c.getDataEmissao().isBefore(inicioMes)
                          && !c.getDataEmissao().isAfter(fimMes))
                .map(c -> c.getValorTotal() != null ? c.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long rascunhos = compraRepository.countByEmpresaIdAndStatus(empresaId, "RASCUNHO");

        // ---- Vendas ----
        List<Venda> vendasHojeLista = vendaRepository.findByEmpresaIdAndStatusAndPeriodoComRelacionamentos(
                empresaId, STATUS_VENDA_FINALIZADA, inicioHoje, fimHoje);
        long vendasHoje = vendasHojeLista.size();
        BigDecimal valorVendasHoje = somarVendas(vendasHojeLista);
        List<VendaResumoDTO> vendasHojeDetalhes = mapearVendas(vendasHojeLista);
        List<VendaDiariaDTO> vendasSemana = calcularVendasSemana(empresaId, hoje);
        BigDecimal ticketMedioUltimos7Dias = calcularTicketMedio(vendasSemana);

        // ---- Contas a pagar ----
        long contasPagarHoje = contaPagarRepository.countVencendoHoje(empresaId, hoje);
        BigDecimal valorContasPagarHoje = contaPagarRepository.sumValorVencendoHoje(empresaId, hoje);
        long contasPagarVencidas = contaPagarRepository.countVencidas(empresaId, hoje);
        long contasPagarAbertas = contaPagarRepository.countByEmpresaIdAndStatus(empresaId, "ABERTA");
        BigDecimal totalContasPagarAbertas = contaPagarRepository
                .sumValorByEmpresaIdAndStatus(empresaId, "ABERTA");

        // ---- Caixa ----
        CaixaResumoDTO caixa = caixaService.obterResumoAtual(empresaId);

        // ---- Estoque ----
        List<Produto> zerados = produtoRepository.findByEmpresaIdAndEstoqueZerado(empresaId);
        List<Produto> abaixoMinimo = produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(empresaId);
        long totalProdutosAtivos = produtoRepository.countByEmpresaIdAndAtivoTrue(empresaId);

        // ---- Alertas ----
        List<AlertaDTO> alertas = montarAlertas(zerados, abaixoMinimo, contasPagarVencidas, rascunhos);

        // ---- Top Produtos ----
        List<TopProdutoDTO> topProdutos = carregarTopProdutosVendidos(empresaId, inicioMesVenda, fimMesVenda);

        return new DashboardAdminDTO(
                comprasMes, valorComprasMes, rascunhos,
                vendasHoje, valorVendasHoje, ticketMedioUltimos7Dias,
                contasPagarHoje, valorContasPagarHoje,
                contasPagarVencidas, contasPagarAbertas, totalContasPagarAbertas,
                caixa.aberto(), caixa.saldoAtual(), caixa.totalEntradas(), caixa.totalSaidas(),
                (long) abaixoMinimo.size(), (long) zerados.size(), totalProdutosAtivos,
                vendasSemana, alertas, topProdutos, vendasHojeDetalhes
        );
    }

    // -----------------------------------------------------------------------
    // VENDAS (módulo ainda não implementado — retorna zeros seguros)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardVendasDTO carregarVendas(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        long comprasMes = compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                empresaId, "CONFIRMADA", inicioMes, fimMes);
        BigDecimal valorComprasMes = compraRepository
                .findByEmpresaIdAndStatus(empresaId, "CONFIRMADA")
                .stream()
                .filter(c -> !c.getDataEmissao().isBefore(inicioMes)
                          && !c.getDataEmissao().isAfter(fimMes))
                .map(c -> c.getValorTotal() != null ? c.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardVendasDTO(0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO,
                comprasMes, valorComprasMes);
    }

    // -----------------------------------------------------------------------
    // FINANCEIRO
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardFinanceiroDTO carregarFinanceiro(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        long contasPagarAbertas = contaPagarRepository.countByEmpresaIdAndStatus(empresaId, "ABERTA");
        BigDecimal totalContasPagarAbertas = contaPagarRepository
                .sumValorByEmpresaIdAndStatus(empresaId, "ABERTA");
        long contasPagarVencidas = contaPagarRepository.countVencidas(empresaId, hoje);
        BigDecimal totalContasPagarVencidas = contaPagarRepository.sumValorVencidas(empresaId, hoje);
        long comprasMes = compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                empresaId, "CONFIRMADA", inicioMes, fimMes);
        BigDecimal valorComprasMes = compraRepository
                .findByEmpresaIdAndStatus(empresaId, "CONFIRMADA")
                .stream()
                .filter(c -> !c.getDataEmissao().isBefore(inicioMes)
                          && !c.getDataEmissao().isAfter(fimMes))
                .map(c -> c.getValorTotal() != null ? c.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardFinanceiroDTO(contasPagarAbertas, totalContasPagarAbertas,
                contasPagarVencidas, totalContasPagarVencidas, comprasMes, valorComprasMes);
    }

    // -----------------------------------------------------------------------
    // ESTOQUE
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardEstoqueDTO carregarEstoque(Integer empresaId) {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusSeconds(1);

        long totalProdutosAtivos = produtoRepository.countByEmpresaIdAndAtivoTrue(empresaId);
        long produtosAbaixoMinimo = produtoRepository
                .findByEmpresaIdAndEstoqueAbaixoMinimo(empresaId).size();
        long entradasMes = movimentacaoEstoqueRepository
                .countByEmpresaIdAndTipoAndCriadoEmBetween(empresaId, "ENTRADA", inicioMes, fimMes);
        long saidasMes = movimentacaoEstoqueRepository
                .countByEmpresaIdAndTipoAndCriadoEmBetween(empresaId, "SAIDA", inicioMes, fimMes);

        return new DashboardEstoqueDTO(totalProdutosAtivos, produtosAbaixoMinimo,
                entradasMes, saidasMes);
    }

    // -----------------------------------------------------------------------
    // Auxiliares
    // -----------------------------------------------------------------------

    private List<VendaDiariaDTO> calcularVendasSemana(Integer empresaId, LocalDate hoje) {
        List<VendaDiariaDTO> resultado = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            LocalDateTime inicioDia = dia.atStartOfDay();
            LocalDateTime fimDia = dia.atTime(LocalTime.MAX);
            long quantidade = vendaRepository.countByEmpresaIdAndStatusAndDataVendaBetween(
                    empresaId, STATUS_VENDA_FINALIZADA, inicioDia, fimDia);
            BigDecimal total = vendaRepository.sumValorTotalByEmpresaIdAndStatusAndDataVendaBetween(
                    empresaId, STATUS_VENDA_FINALIZADA, inicioDia, fimDia);
            resultado.add(new VendaDiariaDTO(dia, total != null ? total : BigDecimal.ZERO, quantidade));
        }
        return resultado;
    }

    private BigDecimal calcularTicketMedio(List<VendaDiariaDTO> vendasSemana) {
        long quantidade = vendasSemana.stream().mapToLong(VendaDiariaDTO::quantidade).sum();
        if (quantidade == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = vendasSemana.stream()
                .map(v -> v.total() != null ? v.total() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal somarVendas(List<Venda> vendas) {
        return vendas.stream()
                .map(v -> v.getValorTotal() != null ? v.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<VendaResumoDTO> mapearVendas(List<Venda> vendas) {
        return vendas.stream()
                .map(v -> new VendaResumoDTO(
                        nvl(v.getNumero()),
                        nomeCliente(v),
                        v.getVendedor() != null ? nvl(v.getVendedor().getNome()) : "—",
                        v.getDataVenda(),
                        v.getValorTotal() != null ? v.getValorTotal() : BigDecimal.ZERO,
                        formaPagamento(v)
                ))
                .toList();
    }

    private String nomeCliente(Venda venda) {
        if (venda.getCliente() == null) {
            return "Consumidor final";
        }
        if ("PJ".equals(venda.getCliente().getTipoPessoa()) && venda.getCliente().getRazaoSocial() != null) {
            return venda.getCliente().getRazaoSocial();
        }
        return nvl(venda.getCliente().getNome());
    }

    private String formaPagamento(Venda venda) {
        if (venda.getPagamentos() == null || venda.getPagamentos().isEmpty()) {
            return "—";
        }
        VendaPagamento pagamento = venda.getPagamentos().get(0);
        return nvl(pagamento.getFormaPagamento());
    }

    private List<AlertaDTO> montarAlertas(List<Produto> zerados, List<Produto> abaixoMinimo,
                                          long contasVencidas, long rascunhos) {
        List<AlertaDTO> lista = new ArrayList<>();

        // Estoque zerado — crítico
        for (Produto p : zerados) {
            lista.add(new AlertaDTO(p.getDescricao() + " — estoque zerado", "CRITICO", null));
        }

        // Abaixo do mínimo (que não estão zerados) — aviso
        for (Produto p : abaixoMinimo) {
            if (p.getEstoqueAtual().compareTo(BigDecimal.ZERO) > 0) {
                lista.add(new AlertaDTO(p.getDescricao() + " — abaixo do mínimo", "AVISO", null));
            }
        }

        // Contas vencidas — aviso
        if (contasVencidas > 0) {
            String msg = contasVencidas == 1
                    ? "1 conta a pagar vencida"
                    : contasVencidas + " contas a pagar vencidas";
            lista.add(new AlertaDTO(msg, "AVISO", null));
        }

        // Rascunhos aguardando confirmação — info
        if (rascunhos > 0) {
            String msg = rascunhos == 1
                    ? "1 compra aguardando confirmação"
                    : rascunhos + " compras aguardando confirmação";
            lista.add(new AlertaDTO(msg, "INFO", null));
        }

        // Sem alertas
        if (lista.isEmpty()) {
            lista.add(new AlertaDTO("Tudo em ordem! Nenhum alerta no momento.", "OK", null));
        }

        return lista;
    }

    @SuppressWarnings("unchecked")
    private List<TopProdutoDTO> carregarTopProdutosVendidos(Integer empresaId,
                                                            LocalDateTime inicioMes,
                                                            LocalDateTime fimMes) {
        List<Object[]> rows = vendaRepository.findTopProdutosVendidosPeriodo(
                empresaId, inicioMes, fimMes, PageRequest.of(0, 5));
        List<TopProdutoDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            String nome = (String) row[0];
            BigDecimal valor = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
            result.add(new TopProdutoDTO(nome, valor));
        }
        return result;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
