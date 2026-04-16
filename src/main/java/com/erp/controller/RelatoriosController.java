package com.erp.controller;

import com.erp.model.*;
import com.erp.model.dto.relatorio.RelatorioCaixaTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioEstoqueTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioFinanceiroTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioVendasTotaisDTO;
import com.erp.service.AuthService;
import com.erp.service.RelatorioPdfService;
import com.erp.service.RelatorioService;
import com.erp.util.MoneyUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelatoriosController implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RelatorioService relatorioService;
    private final RelatorioPdfService relatorioPdfService;
    private final AuthService authService;

    @FXML private TabPane tabPane;
    @FXML private Tab tabVendas;
    @FXML private Tab tabEstoque;
    @FXML private Tab tabFinanceiro;
    @FXML private Tab tabCaixa;

    @FXML private DatePicker dpInicioVendas;
    @FXML private DatePicker dpFimVendas;
    @FXML private ComboBox<Cliente> cmbClienteVendas;
    @FXML private ComboBox<Funcionario> cmbVendedorVendas;
    @FXML private ComboBox<String> cmbStatusVendas;
    @FXML private Label lblQtdVendas;
    @FXML private Label lblValorBrutoVendas;
    @FXML private Label lblDescontosVendas;
    @FXML private Label lblLiquidoVendas;
    @FXML private Label lblTotalVendas;
    @FXML private Button btnPdfVendas;
    @FXML private TableView<Venda> tblVendas;
    @FXML private TableColumn<Venda, String> colVendaData;
    @FXML private TableColumn<Venda, String> colVendaNumero;
    @FXML private TableColumn<Venda, String> colVendaCliente;
    @FXML private TableColumn<Venda, String> colVendaVendedor;
    @FXML private TableColumn<Venda, String> colVendaForma;
    @FXML private TableColumn<Venda, String> colVendaBruto;
    @FXML private TableColumn<Venda, String> colVendaDesconto;
    @FXML private TableColumn<Venda, String> colVendaLiquido;
    @FXML private TableColumn<Venda, String> colVendaStatus;

    @FXML private ComboBox<GrupoProduto> cmbGrupoEstoque;
    @FXML private TextField txtBuscaEstoque;
    @FXML private ComboBox<String> cmbSituacaoEstoque;
    @FXML private CheckBox chkInativosEstoque;
    @FXML private Label lblProdutosAtivos;
    @FXML private Label lblProdutosNormal;
    @FXML private Label lblProdutosAbaixo;
    @FXML private Label lblProdutosZerados;
    @FXML private Label lblTotalEstoque;
    @FXML private Button btnPdfEstoque;
    @FXML private TableView<Produto> tblEstoque;
    @FXML private TableColumn<Produto, String> colEstCodigo;
    @FXML private TableColumn<Produto, String> colEstDescricao;
    @FXML private TableColumn<Produto, String> colEstGrupo;
    @FXML private TableColumn<Produto, String> colEstUnidade;
    @FXML private TableColumn<Produto, String> colEstAtual;
    @FXML private TableColumn<Produto, String> colEstMinimo;
    @FXML private TableColumn<Produto, String> colEstMaximo;
    @FXML private TableColumn<Produto, String> colEstCusto;
    @FXML private TableColumn<Produto, String> colEstVenda;
    @FXML private TableColumn<Produto, String> colEstValor;
    @FXML private TableColumn<Produto, String> colEstSituacao;

    @FXML private DatePicker dpInicioFinanceiro;
    @FXML private DatePicker dpFimFinanceiro;
    @FXML private ComboBox<String> cmbStatusFinanceiro;
    @FXML private ComboBox<Fornecedor> cmbFornecedorFinanceiro;
    @FXML private ComboBox<Cliente> cmbClienteFinanceiro;
    @FXML private Label lblPagarAberto;
    @FXML private Label lblPagarPago;
    @FXML private Label lblReceberAberto;
    @FXML private Label lblRecebidoPeriodo;
    @FXML private Button btnPdfFinanceiro;
    @FXML private TableView<ContaPagar> tblPagar;
    @FXML private TableView<ContaReceber> tblReceber;
    @FXML private TableColumn<ContaPagar, String> colPagarFornecedor;
    @FXML private TableColumn<ContaPagar, String> colPagarDescricao;
    @FXML private TableColumn<ContaPagar, String> colPagarParcela;
    @FXML private TableColumn<ContaPagar, String> colPagarVencimento;
    @FXML private TableColumn<ContaPagar, String> colPagarValor;
    @FXML private TableColumn<ContaPagar, String> colPagarPago;
    @FXML private TableColumn<ContaPagar, String> colPagarForma;
    @FXML private TableColumn<ContaPagar, String> colPagarStatus;
    @FXML private TableColumn<ContaReceber, String> colReceberCliente;
    @FXML private TableColumn<ContaReceber, String> colReceberDescricao;
    @FXML private TableColumn<ContaReceber, String> colReceberParcela;
    @FXML private TableColumn<ContaReceber, String> colReceberVencimento;
    @FXML private TableColumn<ContaReceber, String> colReceberValor;
    @FXML private TableColumn<ContaReceber, String> colReceberPago;
    @FXML private TableColumn<ContaReceber, String> colReceberForma;
    @FXML private TableColumn<ContaReceber, String> colReceberStatus;

    @FXML private DatePicker dpInicioCaixa;
    @FXML private DatePicker dpFimCaixa;
    @FXML private ComboBox<Caixa> cmbCaixa;
    @FXML private ComboBox<Usuario> cmbOperadorCaixa;
    @FXML private ComboBox<String> cmbStatusCaixa;
    @FXML private Label lblSessoesCaixa;
    @FXML private Label lblEntradasCaixa;
    @FXML private Label lblSaidasCaixa;
    @FXML private Label lblSaldoCaixa;
    @FXML private Button btnPdfCaixa;
    @FXML private TableView<CaixaSessao> tblCaixa;
    @FXML private TableView<CaixaMovimentacao> tblMovCaixa;
    @FXML private TableColumn<CaixaSessao, String> colCaixaNome;
    @FXML private TableColumn<CaixaSessao, String> colCaixaOperador;
    @FXML private TableColumn<CaixaSessao, String> colCaixaAbertura;
    @FXML private TableColumn<CaixaSessao, String> colCaixaFechamento;
    @FXML private TableColumn<CaixaSessao, String> colCaixaInicial;
    @FXML private TableColumn<CaixaSessao, String> colCaixaEntradas;
    @FXML private TableColumn<CaixaSessao, String> colCaixaSaidas;
    @FXML private TableColumn<CaixaSessao, String> colCaixaFinal;
    @FXML private TableColumn<CaixaSessao, String> colCaixaDiferenca;
    @FXML private TableColumn<CaixaSessao, String> colCaixaStatus;
    @FXML private TableColumn<CaixaMovimentacao, String> colMovTipo;
    @FXML private TableColumn<CaixaMovimentacao, String> colMovDescricao;
    @FXML private TableColumn<CaixaMovimentacao, String> colMovForma;
    @FXML private TableColumn<CaixaMovimentacao, String> colMovOrigem;
    @FXML private TableColumn<CaixaMovimentacao, String> colMovValor;

    private List<Venda> vendas = List.of();
    private List<Produto> produtos = List.of();
    private List<ContaPagar> contasPagar = List.of();
    private List<ContaReceber> contasReceber = List.of();
    private List<CaixaSessao> sessoesCaixa = List.of();
    private final Map<Integer, BigDecimal> entradasPorSessao = new HashMap<>();
    private final Map<Integer, BigDecimal> saidasPorSessao = new HashMap<>();
    private boolean estoqueCarregado;
    private boolean financeiroCarregado;
    private boolean caixaCarregado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarDatas();
        configurarFiltros();
        configurarTabelas();
        carregarCombos();
        configurarLazyTabs();
        aplicarFiltrosVendas();
    }

    private void configurarDatas() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = LocalDate.now();
        for (DatePicker dp : List.of(dpInicioVendas, dpFimVendas, dpInicioFinanceiro, dpFimFinanceiro,
                dpInicioCaixa, dpFimCaixa)) {
            dp.setConverter(new LocalDateStringConverter(DATE_FMT, DATE_FMT));
        }
        dpInicioVendas.setValue(inicio);
        dpFimVendas.setValue(fim);
        dpInicioFinanceiro.setValue(inicio);
        dpFimFinanceiro.setValue(fim);
        dpInicioCaixa.setValue(inicio);
        dpFimCaixa.setValue(fim);
    }

    private void configurarFiltros() {
        cmbStatusVendas.setItems(FXCollections.observableArrayList("Todas", "Finalizadas", "Canceladas"));
        cmbStatusVendas.setValue("Finalizadas");
        cmbSituacaoEstoque.setItems(FXCollections.observableArrayList("Todos", "Normal", "Abaixo do Mínimo", "Zerado/Negativo"));
        cmbSituacaoEstoque.setValue("Todos");
        cmbStatusFinanceiro.setItems(FXCollections.observableArrayList("Todos", "Abertos", "Vencidos", "Pagos/Recebidos", "Cancelados"));
        cmbStatusFinanceiro.setValue("Todos");
        cmbStatusCaixa.setItems(FXCollections.observableArrayList("Todos", "Abertos", "Fechados"));
        cmbStatusCaixa.setValue("Todos");
    }

    private void configurarTabelas() {
        configurarTabelaVendas();
        configurarTabelaEstoque();
        configurarTabelaFinanceiro();
        configurarTabelaCaixa();
    }

    private void configurarTabelaVendas() {
        colVendaData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataVenda() != null ? c.getValue().getDataVenda().format(DATE_TIME_FMT) : "—"));
        colVendaNumero.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNumero())));
        colVendaCliente.setCellValueFactory(c -> new SimpleStringProperty(nomeCliente(c.getValue().getCliente())));
        colVendaVendedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVendedor() != null ? nvl(c.getValue().getVendedor().getNome()) : "—"));
        colVendaForma.setCellValueFactory(c -> new SimpleStringProperty(formaVenda(c.getValue())));
        colVendaBruto.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValorProdutos())));
        colVendaDesconto.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValorDesconto())));
        colVendaLiquido.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValorTotal())));
        colVendaStatus.setCellValueFactory(c -> new SimpleStringProperty(statusVenda(c.getValue().getStatus())));
        colVendaStatus.setCellFactory(col -> badgeCell());
    }

    private void configurarTabelaEstoque() {
        colEstCodigo.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCodigoInterno())));
        colEstDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colEstGrupo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGrupo() != null ? nvl(c.getValue().getGrupo().getNome()) : "—"));
        colEstUnidade.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnidade() != null ? nvl(c.getValue().getUnidade().getSigla()) : "—"));
        colEstAtual.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEstoqueAtual()).toPlainString()));
        colEstMinimo.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEstoqueMinimo()).toPlainString()));
        colEstMaximo.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEstoqueMaximo()).toPlainString()));
        colEstCusto.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getPrecoCusto())));
        colEstVenda.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getPrecoVenda())));
        colEstValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(nvl(c.getValue().getEstoqueAtual()).multiply(nvl(c.getValue().getPrecoCusto())))));
        colEstSituacao.setCellValueFactory(c -> new SimpleStringProperty(situacaoEstoqueTexto(c.getValue())));
        colEstSituacao.setCellFactory(col -> badgeCell());
    }

    private void configurarTabelaFinanceiro() {
        colPagarFornecedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFornecedor() != null ? nomeFornecedor(c.getValue().getFornecedor()) : "—"));
        colPagarDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colPagarParcela.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNumeroParcela()) + "/" + nvl(c.getValue().getTotalParcelas())));
        colPagarVencimento.setCellValueFactory(c -> new SimpleStringProperty(formatarData(c.getValue().getDataVencimento())));
        colPagarValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValor())));
        colPagarPago.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValorPago())));
        colPagarForma.setCellValueFactory(c -> new SimpleStringProperty(formatarForma(c.getValue().getFormaPagamento())));
        colPagarStatus.setCellValueFactory(c -> new SimpleStringProperty(statusFinanceiro(c.getValue().getStatus(), c.getValue().getDataVencimento(), false)));
        colPagarStatus.setCellFactory(col -> badgeCell());

        colReceberCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCliente() != null ? nomeCliente(c.getValue().getCliente()) : "—"));
        colReceberDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colReceberParcela.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNumeroParcela()) + "/" + nvl(c.getValue().getTotalParcelas())));
        colReceberVencimento.setCellValueFactory(c -> new SimpleStringProperty(formatarData(c.getValue().getDataVencimento())));
        colReceberValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValor())));
        colReceberPago.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValorPago())));
        colReceberForma.setCellValueFactory(c -> new SimpleStringProperty(formatarForma(c.getValue().getFormaRecebimento())));
        colReceberStatus.setCellValueFactory(c -> new SimpleStringProperty(statusFinanceiro(c.getValue().getStatus(), c.getValue().getDataVencimento(), true)));
        colReceberStatus.setCellFactory(col -> badgeCell());
    }

    private void configurarTabelaCaixa() {
        colCaixaNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCaixa() != null ? nvl(c.getValue().getCaixa().getNome()) : "—"));
        colCaixaOperador.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuario() != null ? nvl(c.getValue().getUsuario().getNome()) : "—"));
        colCaixaAbertura.setCellValueFactory(c -> new SimpleStringProperty(formatarDataHora(c.getValue().getDataAbertura())));
        colCaixaFechamento.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataFechamento() != null ? formatarDataHora(c.getValue().getDataFechamento()) : "Em aberto"));
        colCaixaInicial.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getSaldoInicial())));
        colCaixaEntradas.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(entradasPorSessao.getOrDefault(c.getValue().getId(), BigDecimal.ZERO))));
        colCaixaSaidas.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(saidasPorSessao.getOrDefault(c.getValue().getId(), BigDecimal.ZERO))));
        colCaixaFinal.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(nvl(c.getValue().getSaldoInicial()).add(entradasPorSessao.getOrDefault(c.getValue().getId(), BigDecimal.ZERO)).subtract(saidasPorSessao.getOrDefault(c.getValue().getId(), BigDecimal.ZERO)))));
        colCaixaDiferenca.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getDiferenca())));
        colCaixaStatus.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getStatus())));
        colCaixaStatus.setCellFactory(col -> badgeCell());
        tblCaixa.getSelectionModel().selectedItemProperty().addListener((obs, old, sessao) -> carregarMovimentacoes(sessao));

        colMovTipo.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getTipo())));
        colMovDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colMovForma.setCellValueFactory(c -> new SimpleStringProperty(formatarForma(c.getValue().getFormaPagamento())));
        colMovOrigem.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getOrigem())));
        colMovValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValor())));
    }

    private void carregarCombos() {
        Integer empresaId = authService.getEmpresaIdLogado();
        cmbClienteVendas.setItems(FXCollections.observableArrayList(relatorioService.listarClientes(empresaId)));
        cmbClienteVendas.setConverter(clienteConverter());
        cmbVendedorVendas.setItems(FXCollections.observableArrayList(relatorioService.listarVendedores(empresaId)));
        cmbVendedorVendas.setConverter(funcionarioConverter());
        cmbGrupoEstoque.setItems(FXCollections.observableArrayList(relatorioService.listarGrupos(empresaId)));
        cmbGrupoEstoque.setConverter(grupoConverter());
        cmbFornecedorFinanceiro.setItems(FXCollections.observableArrayList(relatorioService.listarFornecedores(empresaId)));
        cmbFornecedorFinanceiro.setConverter(fornecedorConverter());
        cmbClienteFinanceiro.setItems(FXCollections.observableArrayList(relatorioService.listarClientes(empresaId)));
        cmbClienteFinanceiro.setConverter(clienteConverter());
        cmbCaixa.setItems(FXCollections.observableArrayList(relatorioService.listarCaixas(empresaId)));
        cmbCaixa.setConverter(caixaConverter());
        cmbOperadorCaixa.setItems(FXCollections.observableArrayList(relatorioService.listarUsuarios(empresaId)));
        cmbOperadorCaixa.setConverter(usuarioConverter());
    }

    private void configurarLazyTabs() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> {
            if (tab == tabEstoque && !estoqueCarregado) aplicarFiltrosEstoque();
            if (tab == tabFinanceiro && !financeiroCarregado) aplicarFiltrosFinanceiro();
            if (tab == tabCaixa && !caixaCarregado) aplicarFiltrosCaixa();
        });
    }

    @FXML
    private void aplicarFiltrosVendas() {
        if (!validarPeriodo(dpInicioVendas, dpFimVendas)) return;
        vendas = relatorioService.relatorioVendas(authService.getEmpresaIdLogado(), dpInicioVendas.getValue(), dpFimVendas.getValue(),
                id(cmbClienteVendas.getValue()), id(cmbVendedorVendas.getValue()), statusVendaFiltro());
        tblVendas.setItems(FXCollections.observableArrayList(vendas));
        RelatorioVendasTotaisDTO totais = relatorioService.totaisVendas(vendas);
        lblQtdVendas.setText(String.valueOf(totais.quantidadeVendas()));
        lblValorBrutoVendas.setText(MoneyUtils.formatCurrency(totais.valorBruto()));
        lblDescontosVendas.setText(MoneyUtils.formatCurrency(totais.totalDescontos()));
        lblLiquidoVendas.setText(MoneyUtils.formatCurrency(totais.valorLiquido()));
        lblTotalVendas.setText("Total líquido: " + MoneyUtils.formatCurrency(totais.valorLiquido()));
        configurarBotaoPdf(btnPdfVendas, vendas.isEmpty());
    }

    @FXML
    private void limparFiltrosVendas() {
        dpInicioVendas.setValue(LocalDate.now().withDayOfMonth(1));
        dpFimVendas.setValue(LocalDate.now());
        cmbClienteVendas.setValue(null);
        cmbVendedorVendas.setValue(null);
        cmbStatusVendas.setValue("Finalizadas");
        aplicarFiltrosVendas();
    }

    @FXML
    private void aplicarFiltrosEstoque() {
        produtos = relatorioService.relatorioPosicaoEstoque(authService.getEmpresaIdLogado(), id(cmbGrupoEstoque.getValue()),
                txtBuscaEstoque.getText(), situacaoEstoqueFiltro(), chkInativosEstoque.isSelected());
        tblEstoque.setItems(FXCollections.observableArrayList(produtos));
        RelatorioEstoqueTotaisDTO totais = relatorioService.totaisEstoque(produtos);
        lblProdutosAtivos.setText(String.valueOf(totais.totalProdutosAtivos()));
        lblProdutosNormal.setText(String.valueOf(totais.produtosNormais()));
        lblProdutosAbaixo.setText(String.valueOf(totais.produtosAbaixoMinimo()));
        lblProdutosZerados.setText(String.valueOf(totais.produtosZeradosNegativos()));
        lblTotalEstoque.setText("Valor em estoque: " + MoneyUtils.formatCurrency(totais.valorTotalEstoque()));
        configurarBotaoPdf(btnPdfEstoque, produtos.isEmpty());
        estoqueCarregado = true;
    }

    @FXML
    private void limparFiltrosEstoque() {
        cmbGrupoEstoque.setValue(null);
        txtBuscaEstoque.clear();
        cmbSituacaoEstoque.setValue("Todos");
        chkInativosEstoque.setSelected(false);
        aplicarFiltrosEstoque();
    }

    @FXML
    private void aplicarFiltrosFinanceiro() {
        if (!validarPeriodo(dpInicioFinanceiro, dpFimFinanceiro)) return;
        String status = statusFinanceiroFiltro();
        contasPagar = relatorioService.relatorioContasPagar(authService.getEmpresaIdLogado(), dpInicioFinanceiro.getValue(), dpFimFinanceiro.getValue(),
                id(cmbFornecedorFinanceiro.getValue()), status);
        contasReceber = relatorioService.relatorioContasReceber(authService.getEmpresaIdLogado(), dpInicioFinanceiro.getValue(), dpFimFinanceiro.getValue(),
                id(cmbClienteFinanceiro.getValue()), "PAGA".equals(status) ? "RECEBIDA" : status);
        tblPagar.setItems(FXCollections.observableArrayList(contasPagar));
        tblReceber.setItems(FXCollections.observableArrayList(contasReceber));
        RelatorioFinanceiroTotaisDTO totais = relatorioService.totaisFinanceiro(contasPagar, contasReceber);
        lblPagarAberto.setText(MoneyUtils.formatCurrency(totais.pagarAberto()));
        lblPagarPago.setText(MoneyUtils.formatCurrency(totais.pagarPagoPeriodo()));
        lblReceberAberto.setText(MoneyUtils.formatCurrency(totais.receberAberto()));
        lblRecebidoPeriodo.setText(MoneyUtils.formatCurrency(totais.receberRecebidoPeriodo()));
        configurarBotaoPdf(btnPdfFinanceiro, contasPagar.isEmpty() && contasReceber.isEmpty());
        financeiroCarregado = true;
    }

    @FXML
    private void limparFiltrosFinanceiro() {
        dpInicioFinanceiro.setValue(LocalDate.now().withDayOfMonth(1));
        dpFimFinanceiro.setValue(LocalDate.now());
        cmbStatusFinanceiro.setValue("Todos");
        cmbFornecedorFinanceiro.setValue(null);
        cmbClienteFinanceiro.setValue(null);
        aplicarFiltrosFinanceiro();
    }

    @FXML
    private void aplicarFiltrosCaixa() {
        if (!validarPeriodo(dpInicioCaixa, dpFimCaixa)) return;
        sessoesCaixa = relatorioService.relatorioCaixa(authService.getEmpresaIdLogado(), dpInicioCaixa.getValue(), dpFimCaixa.getValue(),
                id(cmbCaixa.getValue()), id(cmbOperadorCaixa.getValue()), statusCaixaFiltro());
        atualizarCacheCaixa();
        tblCaixa.setItems(FXCollections.observableArrayList(sessoesCaixa));
        RelatorioCaixaTotaisDTO totais = relatorioService.totaisCaixa(sessoesCaixa);
        lblSessoesCaixa.setText(String.valueOf(totais.totalSessoes()));
        lblEntradasCaixa.setText(MoneyUtils.formatCurrency(totais.totalEntradas()));
        lblSaidasCaixa.setText(MoneyUtils.formatCurrency(totais.totalSaidas()));
        lblSaldoCaixa.setText(MoneyUtils.formatCurrency(totais.saldoLiquido()));
        configurarBotaoPdf(btnPdfCaixa, sessoesCaixa.isEmpty());
        caixaCarregado = true;
    }

    @FXML
    private void limparFiltrosCaixa() {
        dpInicioCaixa.setValue(LocalDate.now().withDayOfMonth(1));
        dpFimCaixa.setValue(LocalDate.now());
        cmbCaixa.setValue(null);
        cmbOperadorCaixa.setValue(null);
        cmbStatusCaixa.setValue("Todos");
        aplicarFiltrosCaixa();
    }

    @FXML private void gerarPdfVendas() { gerarPdf(() -> relatorioPdfService.gerarEAbrirRelatorioVendas(vendas, relatorioService.totaisVendas(vendas), dpInicioVendas.getValue(), dpFimVendas.getValue(), nomeCliente(cmbClienteVendas.getValue()), nomeFuncionario(cmbVendedorVendas.getValue()), cmbStatusVendas.getValue())); }
    @FXML private void gerarPdfEstoque() { gerarPdf(() -> relatorioPdfService.gerarEAbrirRelatorioEstoque(produtos, relatorioService.totaisEstoque(produtos), nomeGrupo(cmbGrupoEstoque.getValue()), cmbSituacaoEstoque.getValue())); }
    @FXML private void gerarPdfFinanceiro() { gerarPdf(() -> relatorioPdfService.gerarEAbrirRelatorioFinanceiro(contasPagar, contasReceber, relatorioService.totaisFinanceiro(contasPagar, contasReceber), dpInicioFinanceiro.getValue(), dpFimFinanceiro.getValue(), cmbStatusFinanceiro.getValue())); }
    @FXML private void gerarPdfCaixa() { gerarPdf(() -> relatorioPdfService.gerarEAbrirRelatorioCaixa(sessoesCaixa, relatorioService.totaisCaixa(sessoesCaixa), dpInicioCaixa.getValue(), dpFimCaixa.getValue(), cmbStatusCaixa.getValue())); }

    private void gerarPdf(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Erro ao gerar relatório", e);
            mostrarErro("Não foi possível gerar o PDF: " + e.getMessage());
        }
    }

    private void atualizarCacheCaixa() {
        entradasPorSessao.clear();
        saidasPorSessao.clear();
        for (CaixaSessao sessao : sessoesCaixa) {
            BigDecimal entradas = BigDecimal.ZERO;
            BigDecimal saidas = BigDecimal.ZERO;
            for (CaixaMovimentacao mov : relatorioService.relatorioMovimentacoesCaixa(sessao.getId())) {
                if (List.of("SUPRIMENTO", "VENDA", "RECEBIMENTO").contains(mov.getTipo())) entradas = entradas.add(nvl(mov.getValor()));
                if (List.of("SANGRIA", "PAGAMENTO").contains(mov.getTipo())) saidas = saidas.add(nvl(mov.getValor()));
            }
            entradasPorSessao.put(sessao.getId(), entradas);
            saidasPorSessao.put(sessao.getId(), saidas);
        }
    }

    private void carregarMovimentacoes(CaixaSessao sessao) {
        tblMovCaixa.setItems(FXCollections.observableArrayList(
                sessao != null ? relatorioService.relatorioMovimentacoesCaixa(sessao.getId()) : List.of()));
    }

    private void configurarBotaoPdf(Button button, boolean vazio) {
        button.setDisable(vazio);
        button.setTooltip(vazio ? new Tooltip("Nenhum dado para exportar") : null);
    }

    private boolean validarPeriodo(DatePicker inicio, DatePicker fim) {
        if (inicio.getValue() == null || fim.getValue() == null) {
            mostrarErro("Informe a data inicial e final.");
            return false;
        }
        if (inicio.getValue().isAfter(fim.getValue())) {
            mostrarErro("A data inicial não pode ser maior que a data final.");
            return false;
        }
        return true;
    }

    private String statusVendaFiltro() {
        return switch (cmbStatusVendas.getValue()) {
            case "Finalizadas" -> "FINALIZADA";
            case "Canceladas" -> "CANCELADA";
            default -> "TODAS";
        };
    }

    private String statusFinanceiroFiltro() {
        return switch (cmbStatusFinanceiro.getValue()) {
            case "Abertos" -> "ABERTA";
            case "Vencidos" -> "VENCIDOS";
            case "Pagos/Recebidos" -> "PAGA";
            case "Cancelados" -> "CANCELADA";
            default -> "TODOS";
        };
    }

    private String statusCaixaFiltro() {
        return switch (cmbStatusCaixa.getValue()) {
            case "Abertos" -> "ABERTO";
            case "Fechados" -> "FECHADO";
            default -> "TODOS";
        };
    }

    private String situacaoEstoqueFiltro() {
        return switch (cmbSituacaoEstoque.getValue()) {
            case "Normal" -> "NORMAL";
            case "Abaixo do Mínimo" -> "ABAIXO_MINIMO";
            case "Zerado/Negativo" -> "ZERADO_NEGATIVO";
            default -> "TODOS";
        };
    }

    private <T> TableCell<T, String> badgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add(styleBadge(item));
                setGraphic(badge);
                setText(null);
            }
        };
    }

    private String styleBadge(String item) {
        return switch (item) {
            case "Finalizada", "Paga", "Recebida", "Normal", "FECHADO" -> "badge-success";
            case "Cancelada", "Vencida", "Zerado", "Negativo" -> "badge-danger";
            case "Abaixo Mínimo" -> "badge-warning";
            default -> "badge-info";
        };
    }

    private String statusVenda(String status) {
        return switch (nvl(status)) {
            case "FINALIZADA" -> "Finalizada";
            case "CANCELADA" -> "Cancelada";
            default -> nvl(status);
        };
    }

    private String statusFinanceiro(String status, LocalDate vencimento, boolean receber) {
        if ("ABERTA".equals(status) && vencimento != null && vencimento.isBefore(LocalDate.now())) return "Vencida";
        return switch (nvl(status)) {
            case "ABERTA" -> "Aberta";
            case "PAGA" -> "Paga";
            case "RECEBIDA" -> "Recebida";
            case "CANCELADA" -> "Cancelada";
            default -> nvl(status);
        };
    }

    private String situacaoEstoqueTexto(Produto produto) {
        return switch (relatorioService.situacaoEstoque(produto)) {
            case "ZERADO_NEGATIVO" -> nvl(produto.getEstoqueAtual()).compareTo(BigDecimal.ZERO) < 0 ? "Negativo" : "Zerado";
            case "ABAIXO_MINIMO" -> "Abaixo Mínimo";
            default -> "Normal";
        };
    }

    private String formaVenda(Venda venda) {
        if (venda.getPagamentos() == null || venda.getPagamentos().isEmpty()) return "—";
        if (venda.getPagamentos().size() > 1) return "Múltiplas";
        return formatarForma(venda.getPagamentos().get(0).getFormaPagamento());
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

    private String nomeCliente(Cliente cliente) {
        if (cliente == null) return "Todos";
        return "PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null && !cliente.getRazaoSocial().isBlank()
                ? cliente.getRazaoSocial()
                : nvl(cliente.getNome());
    }

    private String nomeFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null) return "Todos";
        return "PJ".equals(fornecedor.getTipoPessoa()) && fornecedor.getRazaoSocial() != null && !fornecedor.getRazaoSocial().isBlank()
                ? fornecedor.getRazaoSocial()
                : nvl(fornecedor.getNome());
    }

    private String nomeFuncionario(Funcionario funcionario) { return funcionario != null ? nvl(funcionario.getNome()) : "Todos"; }
    private String nomeGrupo(GrupoProduto grupo) { return grupo != null ? nvl(grupo.getNome()) : "Todos"; }

    private String formatarData(LocalDate data) { return data != null ? data.format(DATE_FMT) : "—"; }
    private String formatarDataHora(java.time.LocalDateTime data) { return data != null ? data.format(DATE_TIME_FMT) : "—"; }

    private Integer id(Cliente value) { return value != null ? value.getId() : null; }
    private Integer id(Funcionario value) { return value != null ? value.getId() : null; }
    private Integer id(GrupoProduto value) { return value != null ? value.getId() : null; }
    private Integer id(Fornecedor value) { return value != null ? value.getId() : null; }
    private Integer id(Caixa value) { return value != null ? value.getId() : null; }
    private Integer id(Usuario value) { return value != null ? value.getId() : null; }

    private StringConverter<Cliente> clienteConverter() { return converter(this::nomeCliente); }
    private StringConverter<Funcionario> funcionarioConverter() { return converter(this::nomeFuncionario); }
    private StringConverter<GrupoProduto> grupoConverter() { return converter(this::nomeGrupo); }
    private StringConverter<Fornecedor> fornecedorConverter() { return converter(this::nomeFornecedor); }
    private StringConverter<Caixa> caixaConverter() { return converter(c -> c != null ? nvl(c.getNome()) : "Todos"); }
    private StringConverter<Usuario> usuarioConverter() { return converter(u -> u != null ? nvl(u.getNome()) : "Todos"); }

    private <T> StringConverter<T> converter(java.util.function.Function<T, String> formatter) {
        return new StringConverter<>() {
            @Override public String toString(T object) { return formatter.apply(object); }
            @Override public T fromString(String string) { return null; }
        };
    }

    private BigDecimal nvl(BigDecimal valor) { return valor != null ? valor : BigDecimal.ZERO; }
    private Integer nvl(Integer valor) { return valor != null ? valor : 0; }
    private String nvl(String valor) { return valor != null ? valor : ""; }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Relatórios");
        alert.showAndWait();
    }
}
