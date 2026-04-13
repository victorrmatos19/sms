package com.erp.service;

import com.erp.model.Compra;
import com.erp.model.Produto;
import com.erp.model.dto.dashboard.*;
import com.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompraRepository compraRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    // -----------------------------------------------------------------------
    // ADMINISTRADOR
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DashboardAdminDTO carregarAdmin(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

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

        // ---- Contas a pagar ----
        long contasPagarHoje = contaPagarRepository.countVencendoHoje(empresaId, hoje);
        BigDecimal valorContasPagarHoje = contaPagarRepository.sumValorVencendoHoje(empresaId, hoje);
        long contasPagarVencidas = contaPagarRepository.countVencidas(empresaId, hoje);
        long contasPagarAbertas = contaPagarRepository.countByEmpresaIdAndStatus(empresaId, "ABERTA");
        BigDecimal totalContasPagarAbertas = contaPagarRepository
                .sumValorByEmpresaIdAndStatus(empresaId, "ABERTA");

        // ---- Estoque ----
        List<Produto> zerados = produtoRepository.findByEmpresaIdAndEstoqueZerado(empresaId);
        List<Produto> abaixoMinimo = produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(empresaId);
        long totalProdutosAtivos = produtoRepository.countByEmpresaIdAndAtivoTrue(empresaId);

        // ---- Gráfico ----
        List<VendaDiariaDTO> comprasSemana = calcularComprasSemana(empresaId, hoje);

        // ---- Alertas ----
        List<AlertaDTO> alertas = montarAlertas(zerados, abaixoMinimo, contasPagarVencidas, rascunhos);

        // ---- Top Produtos ----
        List<TopProdutoDTO> topProdutos = carregarTopProdutos(empresaId, inicioMes);

        return new DashboardAdminDTO(
                comprasMes, valorComprasMes, rascunhos,
                contasPagarHoje, valorContasPagarHoje,
                contasPagarVencidas, contasPagarAbertas, totalContasPagarAbertas,
                (long) abaixoMinimo.size(), (long) zerados.size(), totalProdutosAtivos,
                comprasSemana, alertas, topProdutos
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

    private List<VendaDiariaDTO> calcularComprasSemana(Integer empresaId, LocalDate hoje) {
        LocalDate inicio = hoje.minusDays(6);
        List<Compra> confirmadas = compraRepository
                .findByEmpresaIdAndStatus(empresaId, "CONFIRMADA")
                .stream()
                .filter(c -> !c.getDataEmissao().isBefore(inicio)
                          && !c.getDataEmissao().isAfter(hoje))
                .collect(Collectors.toList());

        Map<LocalDate, List<Compra>> porDia = confirmadas.stream()
                .collect(Collectors.groupingBy(Compra::getDataEmissao));

        List<VendaDiariaDTO> resultado = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            List<Compra> doDia = porDia.getOrDefault(dia, List.of());
            BigDecimal total = doDia.stream()
                    .map(c -> c.getValorTotal() != null ? c.getValorTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resultado.add(new VendaDiariaDTO(dia, total, doDia.size()));
        }
        return resultado;
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
    private List<TopProdutoDTO> carregarTopProdutos(Integer empresaId, LocalDate inicioMes) {
        List<Object[]> rows = compraRepository.findTopProdutosMes(empresaId, inicioMes);
        List<TopProdutoDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            String nome = (String) row[0];
            BigDecimal valor = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
            result.add(new TopProdutoDTO(nome, valor));
        }
        return result;
    }
}
