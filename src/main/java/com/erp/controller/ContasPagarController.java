package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.ContaPagar;
import com.erp.model.Fornecedor;
import com.erp.model.dto.financeiro.ContasPagarResumoDTO;
import com.erp.service.AuthService;
import com.erp.service.ContaPagarService;
import com.erp.service.FornecedorService;
import com.erp.util.MoneyUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContasPagarController implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContaPagarService contaPagarService;
    private final FornecedorService fornecedorService;
    private final AuthService authService;

    @FXML private Label lblAbertas;
    @FXML private Label lblTotalAberto;
    @FXML private Label lblVencidas;
    @FXML private Label lblTotalVencido;
    @FXML private Label lblHoje;
    @FXML private Label lblTotalHoje;
    @FXML private Label lblPagasMes;
    @FXML private Label lblTotalPagoMes;

    @FXML private TextField txtBusca;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private ComboBox<String> filtroPeriodoPor;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private Button btnFiltrarPeriodo;
    @FXML private Button btnLimparFiltro;
    @FXML private Button btnNovasDespesa;
    @FXML private Button btnBaixar;
    @FXML private Button btnEditarVencimento;
    @FXML private Button btnBaixarLote;
    @FXML private Button btnCancelar;

    @FXML private TableView<ContaPagar> tblContas;
    @FXML private TableColumn<ContaPagar, String> colVencimento;
    @FXML private TableColumn<ContaPagar, String> colFornecedor;
    @FXML private TableColumn<ContaPagar, String> colDescricao;
    @FXML private TableColumn<ContaPagar, String> colParcela;
    @FXML private TableColumn<ContaPagar, String> colValor;
    @FXML private TableColumn<ContaPagar, String> colValorPago;
    @FXML private TableColumn<ContaPagar, String> colForma;
    @FXML private TableColumn<ContaPagar, String> colStatus;

    private final ObservableList<ContaPagar> todasContas = FXCollections.observableArrayList();
    private FilteredList<ContaPagar> filteredContas;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltroStatus();
        configurarFiltrosPeriodo();
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarDados();
    }

    private void configurarFiltroStatus() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todas", "Abertas", "Vencidas", "Vencendo Hoje", "Pagas", "Canceladas"));
        cmbFiltroStatus.setValue("Abertas");
    }

    private void configurarFiltrosPeriodo() {
        filtroPeriodoPor.setItems(FXCollections.observableArrayList("Vencimento", "Emissão"));
        filtroPeriodoPor.setValue("Vencimento");
        configurarDatePicker(dpInicio);
        configurarDatePicker(dpFim);
    }

    private void configurarColunas() {
        colVencimento.setCellValueFactory(c -> new SimpleStringProperty(formatarData(c.getValue().getDataVencimento())));
        colFornecedor.setCellValueFactory(c -> new SimpleStringProperty(nomeFornecedor(c.getValue().getFornecedor())));
        colDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colParcela.setCellValueFactory(c -> new SimpleStringProperty(
                nvl(c.getValue().getNumeroParcela()) + "/" + nvl(c.getValue().getTotalParcelas())));
        colValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValor())));
        colValorPago.setCellValueFactory(c -> new SimpleStringProperty(
                "PAGA".equals(c.getValue().getStatus())
                        ? MoneyUtils.formatCurrency(c.getValue().getValorPago())
                        : "—"));
        colForma.setCellValueFactory(c -> new SimpleStringProperty(formatarForma(c.getValue().getFormaPagamento())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(formatarStatusEfetivo(c.getValue())));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add(styleStatus(item));
                setGraphic(badge);
                setText(null);
            }
        });
    }

    private void configurarFiltros() {
        filteredContas = new FilteredList<>(todasContas, conta -> true);
        SortedList<ContaPagar> sorted = new SortedList<>(filteredContas);
        sorted.comparatorProperty().bind(tblContas.comparatorProperty());
        tblContas.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> carregarContas());
    }

    private void configurarSelecao() {
        tblContas.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblContas.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<ContaPagar>) change -> atualizarBotoes());
        tblContas.setOnMouseClicked(e -> {
            ContaPagar selecionada = tblContas.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selecionada != null
                    && tblContas.getSelectionModel().getSelectedItems().size() == 1
                    && podeBaixar(selecionada)) {
                baixarSelecionada();
            }
        });
    }

    @FXML
    private void atualizar() {
        carregarDados();
    }

    @FXML
    private void filtrarPorPeriodo() {
        if (!periodoValido()) {
            return;
        }
        carregarContas();
    }

    @FXML
    private void limparFiltro() {
        txtBusca.clear();
        cmbFiltroStatus.setValue("Todas");
        filtroPeriodoPor.setValue("Vencimento");
        dpInicio.setValue(null);
        dpFim.setValue(null);
        carregarContas();
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        preencherResumo(contaPagarService.obterResumo(empresaId));
        carregarContas();
    }

    private void carregarContas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        if (periodoPreenchido()) {
            todasContas.setAll(buscarPorPeriodo(empresaId));
        } else {
            todasContas.setAll(contaPagarService.listarPorEmpresa(empresaId));
        }
        aplicarFiltro();
        atualizarBotoes();
    }

    private List<ContaPagar> buscarPorPeriodo(Integer empresaId) {
        String status = statusQueryPeriodo();
        if ("Emissão".equals(filtroPeriodoPor.getValue())) {
            return contaPagarService.listarPorPeriodoEmissao(
                    empresaId, status, dpInicio.getValue(), dpFim.getValue());
        }
        return contaPagarService.listarPorPeriodoVencimento(
                empresaId, status, dpInicio.getValue(), dpFim.getValue());
    }

    private void preencherResumo(ContasPagarResumoDTO resumo) {
        lblAbertas.setText(String.valueOf(resumo.abertas()));
        lblTotalAberto.setText(MoneyUtils.formatCurrency(resumo.totalAbertas()));
        lblVencidas.setText(String.valueOf(resumo.vencidas()));
        lblTotalVencido.setText(MoneyUtils.formatCurrency(resumo.totalVencidas()));
        lblHoje.setText(String.valueOf(resumo.vencendoHoje()));
        lblTotalHoje.setText(MoneyUtils.formatCurrency(resumo.totalVencendoHoje()));
        lblPagasMes.setText(String.valueOf(resumo.pagasMes()));
        lblTotalPagoMes.setText(MoneyUtils.formatCurrency(resumo.totalPagasMes()));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        String statusFiltro = cmbFiltroStatus.getValue();
        LocalDate hoje = LocalDate.now();

        filteredContas.setPredicate(conta -> {
            if (!combinaStatus(conta, statusFiltro, hoje)) return false;
            if (busca.isEmpty()) return true;

            String fornecedor = nomeFornecedor(conta.getFornecedor()).toLowerCase();
            String descricao = nvl(conta.getDescricao()).toLowerCase();
            String compra = conta.getCompra() != null && conta.getCompra().getNumeroDocumento() != null
                    ? conta.getCompra().getNumeroDocumento().toLowerCase()
                    : "";
            return fornecedor.contains(busca) || descricao.contains(busca) || compra.contains(busca);
        });
    }

    private boolean combinaStatus(ContaPagar conta, String filtro, LocalDate hoje) {
        if (filtro == null || "Todas".equals(filtro)) return true;
        return switch (filtro) {
            case "Abertas" -> "ABERTA".equals(conta.getStatus());
            case "Vencidas" -> "ABERTA".equals(conta.getStatus())
                    && conta.getDataVencimento() != null
                    && conta.getDataVencimento().isBefore(hoje);
            case "Vencendo Hoje" -> "ABERTA".equals(conta.getStatus())
                    && hoje.equals(conta.getDataVencimento());
            case "Pagas" -> "PAGA".equals(conta.getStatus());
            case "Canceladas" -> "CANCELADA".equals(conta.getStatus());
            default -> true;
        };
    }

    private void atualizarBotoes() {
        int selecionadas = tblContas.getSelectionModel().getSelectedItems().size();
        ContaPagar conta = selecionadas == 1 ? tblContas.getSelectionModel().getSelectedItem() : null;
        boolean umaContaAberta = selecionadas == 1 && podeBaixar(conta);

        btnBaixar.setDisable(!umaContaAberta);
        btnEditarVencimento.setDisable(!umaContaAberta);
        btnCancelar.setDisable(selecionadas != 1 || conta == null
                || "PAGA".equals(conta.getStatus()) || "CANCELADA".equals(conta.getStatus()));
        btnBaixarLote.setDisable(selecionadas <= 1);
    }

    @FXML
    private void baixarSelecionada() {
        ContaPagar conta = contaSelecionadaUnica();
        if (conta == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Baixar Conta a Pagar");
        dialog.setHeaderText(conta.getDescricao());

        TextField txtValorPago = new TextField(MoneyUtils.formatInput(valorPadraoBaixa(conta)));
        TextField txtJuros = new TextField("0,00");
        TextField txtMulta = new TextField("0,00");
        TextField txtDesconto = new TextField(MoneyUtils.formatInput(conta.getDesconto()));
        DatePicker dpPagamento = new DatePicker(LocalDate.now());
        configurarDatePicker(dpPagamento);
        ComboBox<String> cmbForma = new ComboBox<>(FXCollections.observableArrayList(
                "DINHEIRO", "PIX", "TRANSFERENCIA", "BOLETO", "CARTAO_DEBITO", "CARTAO_CREDITO"));
        cmbForma.setValue("PIX");
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações da baixa");
        txtObs.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Valor pago"), txtValorPago);
        grid.addRow(1, new Label("Juros"), txtJuros);
        grid.addRow(2, new Label("Multa"), txtMulta);
        grid.addRow(3, new Label("Desconto"), txtDesconto);
        grid.addRow(4, new Label("Data pagamento"), dpPagamento);
        grid.addRow(5, new Label("Forma"), cmbForma);
        grid.add(new Label("Observações"), 0, 6);
        grid.add(txtObs, 1, 6);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(bt -> executarBaixa(conta, txtValorPago, txtJuros, txtMulta,
                        txtDesconto, dpPagamento, cmbForma, txtObs));
    }

    private void executarBaixa(ContaPagar conta, TextField txtValorPago, TextField txtJuros,
                               TextField txtMulta, TextField txtDesconto, DatePicker dpPagamento,
                               ComboBox<String> cmbForma, TextArea txtObs) {
        try {
            contaPagarService.baixar(conta.getId(),
                    MoneyUtils.parse(txtValorPago.getText()),
                    MoneyUtils.parse(txtJuros.getText()),
                    MoneyUtils.parse(txtMulta.getText()),
                    MoneyUtils.parse(txtDesconto.getText()),
                    dpPagamento.getValue(),
                    cmbForma.getValue(),
                    txtObs.getText());
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao baixar conta a pagar", e);
            mostrarErro("Erro ao baixar conta: " + e.getMessage());
        }
    }

    @FXML
    private void editarVencimento() {
        ContaPagar selecionada = contaSelecionadaUnica();
        if (selecionada == null) return;
        if (!"ABERTA".equals(selecionada.getStatus())) {
            mostrarAviso("Apenas contas ABERTAS podem ter o vencimento alterado.");
            return;
        }

        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Alterar Vencimento");
        dialog.setHeaderText("Conta: " + selecionada.getDescricao());

        DatePicker dp = new DatePicker(selecionada.getDataVencimento());
        configurarDatePicker(dp);
        dp.setPromptText("Nova data de vencimento");

        VBox content = new VBox(8, new Label("Nova data de vencimento:"), dp);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? dp.getValue() : null);

        dialog.showAndWait().ifPresent(novaData -> {
            try {
                contaPagarService.editarVencimento(selecionada.getId(), novaData);
                carregarDados();
            } catch (Exception e) {
                mostrarAviso(e.getMessage());
            }
        });
    }

    @FXML
    private void baixarEmLote() {
        List<ContaPagar> selecionadas = new ArrayList<>(tblContas.getSelectionModel().getSelectedItems());
        List<ContaPagar> abertas = selecionadas.stream()
                .filter(c -> "ABERTA".equals(c.getStatus()))
                .toList();

        if (abertas.isEmpty()) {
            mostrarAviso("Nenhuma conta ABERTA selecionada.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Baixa em Lote");
        dialog.setHeaderText(abertas.size() + " conta(s) serão baixadas");

        DatePicker dpData = new DatePicker(LocalDate.now());
        configurarDatePicker(dpData);
        ComboBox<String> cbForma = new ComboBox<>(FXCollections.observableArrayList(
                "Dinheiro", "PIX", "Transferência", "Cartão Débito", "Cartão Crédito", "Cheque", "Boleto"));
        cbForma.setValue("Transferência");

        BigDecimal total = abertas.stream()
                .map(ContaPagar::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Label lblTotal = new Label("Total: " + MoneyUtils.formatCurrency(total));
        lblTotal.setStyle("-fx-font-weight: bold;");

        VBox content = new VBox(8,
                new Label("Data de pagamento:"), dpData,
                new Label("Forma de pagamento:"), cbForma,
                lblTotal);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                List<Integer> ids = abertas.stream()
                        .map(ContaPagar::getId)
                        .collect(Collectors.toList());
                contaPagarService.baixarEmLote(ids, dpData.getValue(), normalizarFormaPagamento(cbForma.getValue()));
                carregarDados();
                mostrarInfo(abertas.size() + " conta(s) baixada(s) com sucesso.");
            }
        });
    }

    @FXML
    private void novaDespesa() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nova Despesa");
        dialog.setHeaderText("Cadastrar conta a pagar avulsa");

        ComboBox<Fornecedor> cbFornecedor = new ComboBox<>(
                FXCollections.observableArrayList(fornecedorService.listarAtivos(authService.getEmpresaIdLogado())));
        cbFornecedor.setPromptText("Fornecedor opcional");
        cbFornecedor.setEditable(true);
        cbFornecedor.setConverter(fornecedorConverter());

        TextField txtDescricao = new TextField();
        txtDescricao.setPromptText("Descrição da despesa");
        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        DatePicker dpEmissao = new DatePicker(LocalDate.now());
        DatePicker dpVencimento = new DatePicker(LocalDate.now().plusDays(30));
        configurarDatePicker(dpEmissao);
        configurarDatePicker(dpVencimento);
        Spinner<Integer> spParcela = new Spinner<>(1, 999, 1);
        Spinner<Integer> spTotalParcelas = new Spinner<>(1, 999, 1);
        spParcela.setEditable(true);
        spTotalParcelas.setEditable(true);
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);
        txtObs.setPromptText("Observações");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Fornecedor"), cbFornecedor);
        grid.addRow(1, new Label("Descrição *"), txtDescricao);
        grid.addRow(2, new Label("Valor *"), txtValor);
        grid.addRow(3, new Label("Data de Emissão"), dpEmissao);
        grid.addRow(4, new Label("Data de Vencimento *"), dpVencimento);
        grid.addRow(5, new Label("Parcela Nº"), spParcela);
        grid.addRow(6, new Label("Total Parcelas"), spTotalParcelas);
        grid.add(new Label("Observações"), 0, 7);
        grid.add(txtObs, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                salvarDespesaAvulsa(cbFornecedor, txtDescricao, txtValor, dpEmissao,
                        dpVencimento, spParcela, spTotalParcelas, txtObs);
            } catch (Exception e) {
                mostrarAviso(e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void salvarDespesaAvulsa(ComboBox<Fornecedor> cbFornecedor, TextField txtDescricao,
                                     TextField txtValor, DatePicker dpEmissao, DatePicker dpVencimento,
                                     Spinner<Integer> spParcela, Spinner<Integer> spTotalParcelas,
                                     TextArea txtObs) {
        String descricao = txtDescricao.getText();
        BigDecimal valor = MoneyUtils.parse(txtValor.getText());
        Integer numeroParcela = spParcela.getValue();
        Integer totalParcelas = spTotalParcelas.getValue();

        if (descricao == null || descricao.trim().isEmpty()) {
            throw new NegocioException("Descrição é obrigatória.");
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Valor deve ser maior que zero.");
        }
        if (dpVencimento.getValue() == null) {
            throw new NegocioException("Data de vencimento é obrigatória.");
        }
        if (numeroParcela > totalParcelas) {
            throw new NegocioException("Número da parcela deve ser menor ou igual ao total de parcelas.");
        }

        Fornecedor fornecedor = cbFornecedor.getValue();
        contaPagarService.cadastrarAvulsa(
                authService.getEmpresaIdLogado(),
                fornecedor != null ? fornecedor.getId() : null,
                descricao,
                valor,
                dpEmissao.getValue(),
                dpVencimento.getValue(),
                numeroParcela,
                totalParcelas,
                txtObs.getText());
        carregarDados();
        mostrarInfo("Despesa cadastrada com sucesso.");
    }

    @FXML
    private void cancelarSelecionada() {
        ContaPagar conta = contaSelecionadaUnica();
        if (conta == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancelar a conta selecionada?\n" + conta.getDescricao(),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cancelar Conta a Pagar");
        confirm.showAndWait()
                .filter(ButtonType.YES::equals)
                .ifPresent(bt -> executarCancelamento(conta));
    }

    private void executarCancelamento(ContaPagar conta) {
        try {
            contaPagarService.cancelar(conta.getId(), "Cancelada pelo módulo financeiro.");
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cancelar conta a pagar", e);
            mostrarErro("Erro ao cancelar conta: " + e.getMessage());
        }
    }

    private boolean periodoPreenchido() {
        return dpInicio.getValue() != null && dpFim.getValue() != null;
    }

    private boolean periodoValido() {
        if (dpInicio.getValue() == null || dpFim.getValue() == null) {
            mostrarAviso("Informe a data inicial e final do período.");
            return false;
        }
        if (dpInicio.getValue().isAfter(dpFim.getValue())) {
            mostrarAviso("A data inicial não pode ser maior que a data final.");
            return false;
        }
        return true;
    }

    private String statusQueryPeriodo() {
        return switch (cmbFiltroStatus.getValue() == null ? "Todas" : cmbFiltroStatus.getValue()) {
            case "Abertas", "Vencidas", "Vencendo Hoje" -> "ABERTA";
            case "Pagas" -> "PAGA";
            case "Canceladas" -> "CANCELADA";
            default -> null;
        };
    }

    private ContaPagar contaSelecionadaUnica() {
        return tblContas.getSelectionModel().getSelectedItems().size() == 1
                ? tblContas.getSelectionModel().getSelectedItem()
                : null;
    }

    private boolean podeBaixar(ContaPagar conta) {
        return conta != null && "ABERTA".equals(conta.getStatus());
    }

    private BigDecimal valorPadraoBaixa(ContaPagar conta) {
        return nvl(conta.getValor()).add(nvl(conta.getJuros())).add(nvl(conta.getMulta())).subtract(nvl(conta.getDesconto()))
                .max(BigDecimal.ZERO);
    }

    private String formatarStatusEfetivo(ContaPagar conta) {
        if ("ABERTA".equals(conta.getStatus())
                && conta.getDataVencimento() != null
                && conta.getDataVencimento().isBefore(LocalDate.now())) {
            return "Vencida";
        }
        return switch (nvl(conta.getStatus())) {
            case "ABERTA" -> "Aberta";
            case "PAGA" -> "Paga";
            case "CANCELADA" -> "Cancelada";
            default -> nvl(conta.getStatus());
        };
    }

    private String styleStatus(String status) {
        return switch (status) {
            case "Paga" -> "badge-success";
            case "Vencida" -> "badge-danger";
            case "Cancelada" -> "badge-neutral";
            default -> "badge-warning";
        };
    }

    private String nomeFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null) return "—";
        return "PJ".equals(fornecedor.getTipoPessoa()) && fornecedor.getRazaoSocial() != null
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
            default -> forma;
        };
    }

    private String normalizarFormaPagamento(String forma) {
        if (forma == null) return null;
        return switch (forma) {
            case "Dinheiro" -> "DINHEIRO";
            case "Transferência" -> "TRANSFERENCIA";
            case "Cartão Débito" -> "CARTAO_DEBITO";
            case "Cartão Crédito" -> "CARTAO_CREDITO";
            case "Cheque" -> "CHEQUE";
            case "Boleto" -> "BOLETO";
            default -> forma;
        };
    }

    private String formatarData(LocalDate data) {
        return data != null ? data.format(DATE_FMT) : "—";
    }

    private void configurarDatePicker(DatePicker datePicker) {
        datePicker.setConverter(new LocalDateStringConverter(DATE_FMT, DATE_FMT));
    }

    private StringConverter<Fornecedor> fornecedorConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Fornecedor fornecedor) {
                return fornecedor != null ? nomeFornecedor(fornecedor) : "";
            }

            @Override
            public Fornecedor fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                return fornecedorService.listarAtivos(authService.getEmpresaIdLogado()).stream()
                        .filter(f -> nomeFornecedor(f).equalsIgnoreCase(string.trim()))
                        .findFirst()
                        .orElse(null);
            }
        };
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private Integer nvl(Integer valor) {
        return valor != null ? valor : 0;
    }

    private String nvl(String valor) {
        return valor != null ? valor : "";
    }

    private void mostrarInfo(String mensagem) {
        new Alert(Alert.AlertType.INFORMATION, mensagem, ButtonType.OK).showAndWait();
    }

    private void mostrarAviso(String mensagem) {
        new Alert(Alert.AlertType.WARNING, mensagem, ButtonType.OK).showAndWait();
    }

    private void mostrarErro(String mensagem) {
        new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK).showAndWait();
    }
}
