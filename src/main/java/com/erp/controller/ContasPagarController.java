package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.ContaPagar;
import com.erp.model.Fornecedor;
import com.erp.model.dto.financeiro.ContasPagarResumoDTO;
import com.erp.service.AuthService;
import com.erp.service.ContaPagarService;
import com.erp.util.MoneyUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContasPagarController implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContaPagarService contaPagarService;
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
    @FXML private Button btnBaixar;
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
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblContas.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> atualizarBotoes(sel));
        tblContas.setOnMouseClicked(e -> {
            ContaPagar selecionada = tblContas.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && selecionada != null && podeBaixar(selecionada)) {
                baixarSelecionada();
            }
        });
    }

    @FXML
    private void atualizar() {
        carregarDados();
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todasContas.setAll(contaPagarService.listarPorEmpresa(empresaId));
        preencherResumo(contaPagarService.obterResumo(empresaId));
        aplicarFiltro();
        atualizarBotoes(tblContas.getSelectionModel().getSelectedItem());
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

    private void atualizarBotoes(ContaPagar conta) {
        boolean podeBaixar = podeBaixar(conta);
        btnBaixar.setDisable(!podeBaixar);
        btnCancelar.setDisable(conta == null || "PAGA".equals(conta.getStatus()) || "CANCELADA".equals(conta.getStatus()));
    }

    @FXML
    private void baixarSelecionada() {
        ContaPagar conta = tblContas.getSelectionModel().getSelectedItem();
        if (conta == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Baixar Conta a Pagar");
        dialog.setHeaderText(conta.getDescricao());

        TextField txtValorPago = new TextField(MoneyUtils.formatInput(valorPadraoBaixa(conta)));
        TextField txtJuros = new TextField("0,00");
        TextField txtMulta = new TextField("0,00");
        TextField txtDesconto = new TextField(MoneyUtils.formatInput(conta.getDesconto()));
        DatePicker dpPagamento = new DatePicker(LocalDate.now());
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
    private void cancelarSelecionada() {
        ContaPagar conta = tblContas.getSelectionModel().getSelectedItem();
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
            default -> forma;
        };
    }

    private String formatarData(LocalDate data) {
        return data != null ? data.format(DATE_FMT) : "—";
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

    private void mostrarAviso(String mensagem) {
        new Alert(Alert.AlertType.WARNING, mensagem, ButtonType.OK).showAndWait();
    }

    private void mostrarErro(String mensagem) {
        new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK).showAndWait();
    }
}
