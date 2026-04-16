package com.erp.service;

import com.erp.model.*;
import com.erp.model.dto.relatorio.RelatorioCaixaTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioEstoqueTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioFinanceiroTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioVendasTotaisDTO;
import com.erp.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> TIPOS_ENTRADA = List.of("SUPRIMENTO", "VENDA", "RECEBIMENTO");
    private static final List<String> TIPOS_SAIDA = List.of("SANGRIA", "PAGAMENTO");

    private final ConfiguracaoService configuracaoService;
    private final AuthService authService;
    private final RelatorioService relatorioService;

    public void gerarEAbrirRelatorioVendas(List<Venda> vendas, RelatorioVendasTotaisDTO totais,
                                           LocalDate inicio, LocalDate fim, String filtroCliente,
                                           String filtroVendedor, String filtroStatus) {
        try {
            byte[] pdf = gerarPdf("/reports/relatorio-vendas.jrxml",
                    paramsVendas(totais, inicio, fim, filtroCliente, filtroVendedor, filtroStatus),
                    vendas.stream().map(this::linhaVenda).toList());
            salvarEAbrirPdf(pdf, "relatorio-vendas");
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de vendas", e);
            throw new RuntimeException("Erro ao gerar relatório de vendas: " + e.getMessage(), e);
        }
    }

    public void gerarEAbrirRelatorioEstoque(List<Produto> produtos, RelatorioEstoqueTotaisDTO totais,
                                            String filtroGrupo, String filtroSituacao) {
        try {
            Map<String, Object> params = paramsBase("Relatório de Posição de Estoque");
            params.put("PERIODO", "Data de referência: " + LocalDateTime.now().format(DATE_TIME_FMT));
            params.put("FILTROS", filtros("Grupo", filtroGrupo, "Situação", filtroSituacao));
            params.put("TOTAL_1_LABEL", "Produtos ativos");
            params.put("TOTAL_1", String.valueOf(totais.totalProdutosAtivos()));
            params.put("TOTAL_2_LABEL", "Normal");
            params.put("TOTAL_2", String.valueOf(totais.produtosNormais()));
            params.put("TOTAL_3_LABEL", "Abaixo do mínimo");
            params.put("TOTAL_3", String.valueOf(totais.produtosAbaixoMinimo()));
            params.put("TOTAL_4_LABEL", "Valor em estoque");
            params.put("TOTAL_4", MoneyUtils.formatCurrency(totais.valorTotalEstoque()));
            byte[] pdf = gerarPdf("/reports/relatorio-estoque.jrxml", params,
                    produtos.stream().map(this::linhaEstoque).toList());
            salvarEAbrirPdf(pdf, "relatorio-estoque");
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de estoque", e);
            throw new RuntimeException("Erro ao gerar relatório de estoque: " + e.getMessage(), e);
        }
    }

    public void gerarEAbrirRelatorioFinanceiro(List<ContaPagar> pagar, List<ContaReceber> receber,
                                               RelatorioFinanceiroTotaisDTO totais,
                                               LocalDate inicio, LocalDate fim, String filtroStatus) {
        try {
            Map<String, Object> params = paramsBase("Relatório Financeiro");
            params.put("PERIODO", "De " + inicio.format(DATE_FMT) + " até " + fim.format(DATE_FMT));
            params.put("FILTROS", filtros("Status", filtroStatus));
            params.put("TOTAL_1_LABEL", "Pagar aberto");
            params.put("TOTAL_1", MoneyUtils.formatCurrency(totais.pagarAberto()));
            params.put("TOTAL_2_LABEL", "Pagar vencido");
            params.put("TOTAL_2", MoneyUtils.formatCurrency(totais.pagarVencido()));
            params.put("TOTAL_3_LABEL", "Receber aberto");
            params.put("TOTAL_3", MoneyUtils.formatCurrency(totais.receberAberto()));
            params.put("TOTAL_4_LABEL", "Recebido período");
            params.put("TOTAL_4", MoneyUtils.formatCurrency(totais.receberRecebidoPeriodo()));
            List<FinanceiroReportRow> rows = new ArrayList<>();
            pagar.stream().map(this::linhaContaPagar).forEach(rows::add);
            receber.stream().map(this::linhaContaReceber).forEach(rows::add);
            byte[] pdf = gerarPdf("/reports/relatorio-financeiro.jrxml", params, rows);
            salvarEAbrirPdf(pdf, "relatorio-financeiro");
        } catch (Exception e) {
            log.error("Erro ao gerar PDF financeiro", e);
            throw new RuntimeException("Erro ao gerar relatório financeiro: " + e.getMessage(), e);
        }
    }

    public void gerarEAbrirRelatorioCaixa(List<CaixaSessao> sessoes, RelatorioCaixaTotaisDTO totais,
                                          LocalDate inicio, LocalDate fim, String filtroStatus) {
        try {
            Map<String, Object> params = paramsBase("Relatório de Caixa");
            params.put("PERIODO", "De " + inicio.format(DATE_FMT) + " até " + fim.format(DATE_FMT));
            params.put("FILTROS", filtros("Status", filtroStatus));
            params.put("TOTAL_1_LABEL", "Sessões");
            params.put("TOTAL_1", String.valueOf(totais.totalSessoes()));
            params.put("TOTAL_2_LABEL", "Entradas");
            params.put("TOTAL_2", MoneyUtils.formatCurrency(totais.totalEntradas()));
            params.put("TOTAL_3_LABEL", "Saídas");
            params.put("TOTAL_3", MoneyUtils.formatCurrency(totais.totalSaidas()));
            params.put("TOTAL_4_LABEL", "Saldo líquido");
            params.put("TOTAL_4", MoneyUtils.formatCurrency(totais.saldoLiquido()));
            byte[] pdf = gerarPdf("/reports/relatorio-caixa.jrxml", params,
                    sessoes.stream().map(this::linhaCaixa).toList());
            salvarEAbrirPdf(pdf, "relatorio-caixa");
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de caixa", e);
            throw new RuntimeException("Erro ao gerar relatório de caixa: " + e.getMessage(), e);
        }
    }

    private byte[] gerarPdf(String template, Map<String, Object> params, List<?> rows) throws JRException {
        InputStream is = getClass().getResourceAsStream(template);
        if (is == null) {
            throw new IllegalStateException("Template não encontrado: " + template);
        }
        JasperReport report = JasperCompileManager.compileReport(is);
        JasperPrint print = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(rows));
        return JasperExportManager.exportReportToPdf(print);
    }

    private Map<String, Object> paramsVendas(RelatorioVendasTotaisDTO totais, LocalDate inicio, LocalDate fim,
                                             String filtroCliente, String filtroVendedor, String filtroStatus) {
        Map<String, Object> params = paramsBase("Relatório de Vendas");
        params.put("PERIODO", "De " + inicio.format(DATE_FMT) + " até " + fim.format(DATE_FMT));
        params.put("FILTROS", filtros("Cliente", filtroCliente, "Vendedor", filtroVendedor, "Status", filtroStatus));
        params.put("TOTAL_1_LABEL", "Total de vendas");
        params.put("TOTAL_1", String.valueOf(totais.quantidadeVendas()));
        params.put("TOTAL_2_LABEL", "Valor bruto");
        params.put("TOTAL_2", MoneyUtils.formatCurrency(totais.valorBruto()));
        params.put("TOTAL_3_LABEL", "Descontos");
        params.put("TOTAL_3", MoneyUtils.formatCurrency(totais.totalDescontos()));
        params.put("TOTAL_4_LABEL", "Valor líquido");
        params.put("TOTAL_4", MoneyUtils.formatCurrency(totais.valorLiquido()));
        return params;
    }

    private Map<String, Object> paramsBase(String titulo) {
        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, new Locale("pt", "BR"));
        params.put("TITULO", titulo);
        params.put("DATA_GERACAO", LocalDateTime.now().format(DATE_TIME_FMT));

        Empresa empresa = configuracaoService.buscarEmpresa(authService.getEmpresaIdLogado()).orElse(null);
        Configuracao config = empresa != null && empresa.getId() != null
                ? configuracaoService.buscarConfiguracao(empresa.getId()).orElse(null)
                : null;
        params.put("EMPRESA_NOME", empresa != null ? nvl(empresa.getRazaoSocial()) : "");
        params.put("EMPRESA_CNPJ", empresa != null ? nvl(empresa.getCnpj()) : "");
        params.put("EMPRESA_LOGOTIPO", montarLogotipo(empresa, config));
        return params;
    }

    private Image montarLogotipo(Empresa empresa, Configuracao config) {
        if (empresa == null || empresa.getLogotipo() == null || empresa.getLogotipo().length == 0) {
            return null;
        }
        boolean deveExibir = config == null || !Boolean.FALSE.equals(config.getExibirLogotipoImpressao());
        if (!deveExibir) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(empresa.getLogotipo()));
        } catch (IOException e) {
            log.warn("Logotipo da empresa id={} não pôde ser lido para relatório", empresa.getId(), e);
            return null;
        }
    }

    private VendaReportRow linhaVenda(Venda venda) {
        return new VendaReportRow(
                venda.getDataVenda() != null ? venda.getDataVenda().format(DATE_TIME_FMT) : "",
                nvl(venda.getNumero()),
                nomeCliente(venda.getCliente()),
                venda.getVendedor() != null ? nvl(venda.getVendedor().getNome()) : "—",
                formaVenda(venda),
                MoneyUtils.formatCurrency(venda.getValorProdutos()),
                MoneyUtils.formatCurrency(venda.getValorDesconto()),
                MoneyUtils.formatCurrency(venda.getValorTotal()),
                nvl(venda.getStatus()));
    }

    private EstoqueReportRow linhaEstoque(Produto produto) {
        BigDecimal valorEstoque = nvl(produto.getEstoqueAtual()).multiply(nvl(produto.getPrecoCusto()));
        return new EstoqueReportRow(
                nvl(produto.getCodigoInterno()),
                nvl(produto.getDescricao()),
                produto.getGrupo() != null ? nvl(produto.getGrupo().getNome()) : "—",
                produto.getUnidade() != null ? nvl(produto.getUnidade().getSigla()) : "—",
                nvl(produto.getEstoqueAtual()).toPlainString(),
                nvl(produto.getEstoqueMinimo()).toPlainString(),
                nvl(produto.getEstoqueMaximo()).toPlainString(),
                MoneyUtils.formatCurrency(produto.getPrecoCusto()),
                MoneyUtils.formatCurrency(produto.getPrecoVenda()),
                MoneyUtils.formatCurrency(valorEstoque),
                situacaoEstoque(produto));
    }

    private FinanceiroReportRow linhaContaPagar(ContaPagar conta) {
        return new FinanceiroReportRow(
                "Contas a Pagar",
                conta.getFornecedor() != null ? nomeFornecedor(conta.getFornecedor()) : "—",
                nvl(conta.getDescricao()),
                nvl(conta.getNumeroParcela()) + "/" + nvl(conta.getTotalParcelas()),
                conta.getDataVencimento() != null ? conta.getDataVencimento().format(DATE_FMT) : "",
                MoneyUtils.formatCurrency(conta.getValor()),
                MoneyUtils.formatCurrency(conta.getValorPago()),
                formatarForma(conta.getFormaPagamento()),
                nvl(conta.getStatus()));
    }

    private FinanceiroReportRow linhaContaReceber(ContaReceber conta) {
        return new FinanceiroReportRow(
                "Contas a Receber",
                conta.getCliente() != null ? nomeCliente(conta.getCliente()) : "—",
                nvl(conta.getDescricao()),
                nvl(conta.getNumeroParcela()) + "/" + nvl(conta.getTotalParcelas()),
                conta.getDataVencimento() != null ? conta.getDataVencimento().format(DATE_FMT) : "",
                MoneyUtils.formatCurrency(conta.getValor()),
                MoneyUtils.formatCurrency(conta.getValorPago()),
                formatarForma(conta.getFormaRecebimento()),
                nvl(conta.getStatus()));
    }

    private CaixaReportRow linhaCaixa(CaixaSessao sessao) {
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal saidas = BigDecimal.ZERO;
        List<CaixaMovimentacao> movimentacoes = relatorioService.relatorioMovimentacoesCaixa(sessao.getId());
        for (CaixaMovimentacao mov : movimentacoes) {
            if (TIPOS_ENTRADA.contains(mov.getTipo())) {
                entradas = entradas.add(nvl(mov.getValor()));
            } else if (TIPOS_SAIDA.contains(mov.getTipo())) {
                saidas = saidas.add(nvl(mov.getValor()));
            }
        }
        return new CaixaReportRow(
                sessao.getCaixa() != null ? nvl(sessao.getCaixa().getNome()) : "—",
                sessao.getUsuario() != null ? nvl(sessao.getUsuario().getNome()) : "—",
                sessao.getDataAbertura() != null ? sessao.getDataAbertura().format(DATE_TIME_FMT) : "",
                sessao.getDataFechamento() != null ? sessao.getDataFechamento().format(DATE_TIME_FMT) : "Em aberto",
                MoneyUtils.formatCurrency(sessao.getSaldoInicial()),
                MoneyUtils.formatCurrency(entradas),
                MoneyUtils.formatCurrency(saidas),
                MoneyUtils.formatCurrency(nvl(sessao.getSaldoInicial()).add(entradas).subtract(saidas)),
                MoneyUtils.formatCurrency(sessao.getDiferenca()),
                nvl(sessao.getStatus()),
                movimentacoes.stream()
                        .map(m -> nvl(m.getTipo()) + " - " + nvl(m.getDescricao()) + " - " + MoneyUtils.formatCurrency(m.getValor()))
                        .toList()
                        .toString());
    }

    private void salvarEAbrirPdf(byte[] bytes, String nomeBase) throws IOException {
        String nomeArquivo = "sms_" + nomeBase + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        File arquivo = new File(System.getProperty("java.io.tmpdir"), nomeArquivo);
        java.nio.file.Files.write(arquivo.toPath(), bytes);
        abrirArquivo(arquivo);
    }

    private void abrirArquivo(File arquivo) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(arquivo);
                return;
            } catch (IOException e) {
                log.warn("Desktop.OPEN falhou para {}. Tentando comando nativo.",
                        arquivo.getAbsolutePath(), e);
            }
        }
        Optional<List<String>> comando = comandoAberturaNativo(arquivo);
        if (comando.isPresent()) {
            new ProcessBuilder(comando.get()).start();
            return;
        }
        throw new UnsupportedOperationException("Visualizador de PDF não suportado. Arquivo salvo em: " + arquivo.getAbsolutePath());
    }

    private Optional<List<String>> comandoAberturaNativo(File arquivo) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String path = arquivo.getAbsolutePath();
        if (os.contains("mac")) return Optional.of(List.of("open", path));
        if (os.contains("win")) return Optional.of(List.of("rundll32", "url.dll,FileProtocolHandler", path));
        if (os.contains("nux") || os.contains("nix")) return Optional.of(List.of("xdg-open", path));
        return Optional.empty();
    }

    private String formaVenda(Venda venda) {
        if (venda.getPagamentos() == null || venda.getPagamentos().isEmpty()) return "—";
        if (venda.getPagamentos().size() > 1) return "Múltiplas";
        return formatarForma(venda.getPagamentos().get(0).getFormaPagamento());
    }

    private String filtros(String... pares) {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            String valor = pares[i + 1];
            if (valor != null && !valor.isBlank() && !"Todos".equalsIgnoreCase(valor) && !"Todas".equalsIgnoreCase(valor)) {
                partes.add(pares[i] + ": " + valor);
            }
        }
        return partes.isEmpty() ? "Filtros: Todos" : "Filtros: " + String.join(" | ", partes);
    }

    private String situacaoEstoque(Produto produto) {
        return switch (relatorioService.situacaoEstoque(produto)) {
            case "ZERADO_NEGATIVO" -> nvl(produto.getEstoqueAtual()).compareTo(BigDecimal.ZERO) < 0 ? "Negativo" : "Zerado";
            case "ABAIXO_MINIMO" -> "Abaixo Mínimo";
            default -> "Normal";
        };
    }

    private String nomeCliente(Cliente cliente) {
        if (cliente == null) return "Consumidor Final";
        return "PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null && !cliente.getRazaoSocial().isBlank()
                ? cliente.getRazaoSocial()
                : nvl(cliente.getNome());
    }

    private String nomeFornecedor(Fornecedor fornecedor) {
        return "PJ".equals(fornecedor.getTipoPessoa()) && fornecedor.getRazaoSocial() != null && !fornecedor.getRazaoSocial().isBlank()
                ? fornecedor.getRazaoSocial()
                : nvl(fornecedor.getNome());
    }

    private String formatarForma(String forma) {
        if (forma == null || forma.isBlank()) return "—";
        return switch (forma) {
            case "DINHEIRO" -> "Dinheiro";
            case "PIX" -> "PIX";
            case "TRANSFERENCIA" -> "Transferência";
            case "BOLETO" -> "Boleto";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            case "CARTAO_CREDITO" -> "Cartão Crédito";
            case "CHEQUE" -> "Cheque";
            case "PRAZO" -> "Prazo";
            case "CREDIARIO" -> "Crediário";
            default -> forma;
        };
    }

    private BigDecimal nvl(BigDecimal valor) { return valor != null ? valor : BigDecimal.ZERO; }
    private Integer nvl(Integer valor) { return valor != null ? valor : 0; }
    private String nvl(String valor) { return valor != null ? valor : ""; }

    public record VendaReportRow(String data, String numero, String cliente, String vendedor,
                                 String forma, String bruto, String desconto, String liquido, String status) {
        public String getData() { return data; }
        public String getNumero() { return numero; }
        public String getCliente() { return cliente; }
        public String getVendedor() { return vendedor; }
        public String getForma() { return forma; }
        public String getBruto() { return bruto; }
        public String getDesconto() { return desconto; }
        public String getLiquido() { return liquido; }
        public String getStatus() { return status; }
    }
    public record EstoqueReportRow(String codigo, String descricao, String grupo, String unidade,
                                   String atual, String minimo, String maximo, String custo,
                                   String venda, String valor, String situacao) {
        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
        public String getGrupo() { return grupo; }
        public String getUnidade() { return unidade; }
        public String getAtual() { return atual; }
        public String getMinimo() { return minimo; }
        public String getMaximo() { return maximo; }
        public String getCusto() { return custo; }
        public String getVenda() { return venda; }
        public String getValor() { return valor; }
        public String getSituacao() { return situacao; }
    }
    public record FinanceiroReportRow(String secao, String pessoa, String descricao, String parcela,
                                      String vencimento, String valor, String pago, String forma, String status) {
        public String getSecao() { return secao; }
        public String getPessoa() { return pessoa; }
        public String getDescricao() { return descricao; }
        public String getParcela() { return parcela; }
        public String getVencimento() { return vencimento; }
        public String getValor() { return valor; }
        public String getPago() { return pago; }
        public String getForma() { return forma; }
        public String getStatus() { return status; }
    }
    public record CaixaReportRow(String caixa, String operador, String abertura, String fechamento,
                                 String saldoInicial, String entradas, String saidas, String saldoFinal,
                                 String diferenca, String status, String movimentacoes) {
        public String getCaixa() { return caixa; }
        public String getOperador() { return operador; }
        public String getAbertura() { return abertura; }
        public String getFechamento() { return fechamento; }
        public String getSaldoInicial() { return saldoInicial; }
        public String getEntradas() { return entradas; }
        public String getSaidas() { return saidas; }
        public String getSaldoFinal() { return saldoFinal; }
        public String getDiferenca() { return diferenca; }
        public String getStatus() { return status; }
        public String getMovimentacoes() { return movimentacoes; }
    }
}
