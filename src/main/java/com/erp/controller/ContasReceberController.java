package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Cliente;
import com.erp.model.ContaReceber;
import com.erp.model.dto.financeiro.ContasReceberResumoDTO;
import com.erp.service.AuthService;
import com.erp.service.ClienteService;
import com.erp.service.ContaReceberService;
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
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContasReceberController implements Initializable {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContaReceberService contaReceberService;
    private final ClienteService clienteService;
    private final AuthService authService;

    @FXML private Label lblAbertas;
    @FXML private Label lblTotalAberto;
    @FXML private Label lblVencidas;
    @FXML private Label lblTotalVencido;
    @FXML private Label lblHoje;
    @FXML private Label lblTotalHoje;
    @FXML private Label lblRecebidasMes;
    @FXML private Label lblTotalRecebidoMes;

    @FXML private TextField txtBusca;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private Button btnBaixar;
    @FXML private Button btnCancelar;

    @FXML private TableView<ContaReceber> tblContas;
    @FXML private TableColumn<ContaReceber, String> colVencimento;
    @FXML private TableColumn<ContaReceber, String> colCliente;
    @FXML private TableColumn<ContaReceber, String> colDescricao;
    @FXML private TableColumn<ContaReceber, String> colParcela;
    @FXML private TableColumn<ContaReceber, String> colValor;
    @FXML private TableColumn<ContaReceber, String> colValorPago;
    @FXML private TableColumn<ContaReceber, String> colForma;
    @FXML private TableColumn<ContaReceber, String> colStatus;

    private final ObservableList<ContaReceber> todasContas = FXCollections.observableArrayList();
    private FilteredList<ContaReceber> filteredContas;

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
                "Todas", "Abertas", "Vencidas", "Vencendo Hoje", "Recebidas", "Canceladas"));
        cmbFiltroStatus.setValue("Abertas");
    }

    private void configurarColunas() {
        colVencimento.setCellValueFactory(c -> new SimpleStringProperty(formatarData(c.getValue().getDataVencimento())));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(nomeCliente(c.getValue().getCliente())));
        colDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colParcela.setCellValueFactory(c -> new SimpleStringProperty(
                nvl(c.getValue().getNumeroParcela()) + "/" + nvl(c.getValue().getTotalParcelas())));
        colValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValor())));
        colValorPago.setCellValueFactory(c -> new SimpleStringProperty(
                "RECEBIDA".equals(c.getValue().getStatus())
                        ? MoneyUtils.formatCurrency(c.getValue().getValorPago())
                        : "—"));
        colForma.setCellValueFactory(c -> new SimpleStringProperty(formatarForma(c.getValue().getFormaRecebimento())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(formatarStatusEfetivo(c.getValue())));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add(styleStatus(item));
                setGraphic(badge);
                setText(null);
            }
        });
    }

    private void configurarFiltros() {
        filteredContas = new FilteredList<>(todasContas, conta -> true);
        SortedList<ContaReceber> sorted = new SortedList<>(filteredContas);
        sorted.comparatorProperty().bind(tblContas.comparatorProperty());
        tblContas.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblContas.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> atualizarBotoes(sel));
        tblContas.setOnMouseClicked(e -> {
            ContaReceber selecionada = tblContas.getSelectionModel().getSelectedItem();
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
        todasContas.setAll(contaReceberService.listarPorEmpresa(empresaId));
        preencherResumo(contaReceberService.obterResumo(empresaId));
        aplicarFiltro();
        atualizarBotoes(tblContas.getSelectionModel().getSelectedItem());
    }

    private void preencherResumo(ContasReceberResumoDTO resumo) {
        lblAbertas.setText(String.valueOf(resumo.abertas()));
        lblTotalAberto.setText(MoneyUtils.formatCurrency(resumo.totalAbertas()));
        lblVencidas.setText(String.valueOf(resumo.vencidas()));
        lblTotalVencido.setText(MoneyUtils.formatCurrency(resumo.totalVencidas()));
        lblHoje.setText(String.valueOf(resumo.vencendoHoje()));
        lblTotalHoje.setText(MoneyUtils.formatCurrency(resumo.totalVencendoHoje()));
        lblRecebidasMes.setText(String.valueOf(resumo.recebidasMes()));
        lblTotalRecebidoMes.setText(MoneyUtils.formatCurrency(resumo.totalRecebidasMes()));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        String statusFiltro = cmbFiltroStatus.getValue();
        LocalDate hoje = LocalDate.now();

        filteredContas.setPredicate(conta -> {
            if (!combinaStatus(conta, statusFiltro, hoje)) return false;
            if (busca.isEmpty()) return true;

            String cliente = nomeCliente(conta.getCliente()).toLowerCase();
            String descricao = nvl(conta.getDescricao()).toLowerCase();
            String venda = conta.getVenda() != null && conta.getVenda().getNumero() != null
                    ? conta.getVenda().getNumero().toLowerCase() : "";
            return cliente.contains(busca) || descricao.contains(busca) || venda.contains(busca);
        });
    }

    private boolean combinaStatus(ContaReceber conta, String filtro, LocalDate hoje) {
        if (filtro == null || "Todas".equals(filtro)) return true;
        return switch (filtro) {
            case "Abertas"       -> "ABERTA".equals(conta.getStatus());
            case "Vencidas"      -> "ABERTA".equals(conta.getStatus())
                    && conta.getDataVencimento() != null
                    && conta.getDataVencimento().isBefore(hoje);
            case "Vencendo Hoje" -> "ABERTA".equals(conta.getStatus())
                    && hoje.equals(conta.getDataVencimento());
            case "Recebidas"     -> "RECEBIDA".equals(conta.getStatus());
            case "Canceladas"    -> "CANCELADA".equals(conta.getStatus());
            default -> true;
        };
    }

    private void atualizarBotoes(ContaReceber conta) {
        boolean podeBaixar = podeBaixar(conta);
        btnBaixar.setDisable(!podeBaixar);
        btnCancelar.setDisable(conta == null
                || "RECEBIDA".equals(conta.getStatus())
                || "CANCELADA".equals(conta.getStatus()));
    }

    @FXML
    private void baixarSelecionada() {
        ContaReceber conta = tblContas.getSelectionModel().getSelectedItem();
        if (conta == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Baixar Conta a Receber");
        dialog.setHeaderText(conta.getDescricao());

        TextField txtValorPago = new TextField(MoneyUtils.formatInput(valorPadraoBaixa(conta)));
        TextField txtJuros     = new TextField("0,00");
        TextField txtMulta     = new TextField("0,00");
        TextField txtDesconto  = new TextField(MoneyUtils.formatInput(conta.getDesconto()));
        DatePicker dpRecebimento = new DatePicker(LocalDate.now());
        ComboBox<String> cmbForma = new ComboBox<>(FXCollections.observableArrayList(
                "DINHEIRO", "PIX", "TRANSFERENCIA", "CARTAO_DEBITO", "CARTAO_CREDITO", "CHEQUE"));
        cmbForma.setValue("PIX");
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações do recebimento");
        txtObs.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Valor recebido"), txtValorPago);
        grid.addRow(1, new Label("Juros"),           txtJuros);
        grid.addRow(2, new Label("Multa"),           txtMulta);
        grid.addRow(3, new Label("Desconto"),        txtDesconto);
        grid.addRow(4, new Label("Data recebimento"), dpRecebimento);
        grid.addRow(5, new Label("Forma"),           cmbForma);
        grid.add(new Label("Observações"), 0, 6);
        grid.add(txtObs, 1, 6);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(bt -> executarBaixa(conta, txtValorPago, txtJuros, txtMulta,
                        txtDesconto, dpRecebimento, cmbForma, txtObs));
    }

    private void executarBaixa(ContaReceber conta, TextField txtValorPago, TextField txtJuros,
                               TextField txtMulta, TextField txtDesconto, DatePicker dpRecebimento,
                               ComboBox<String> cmbForma, TextArea txtObs) {
        try {
            contaReceberService.baixar(conta.getId(),
                    MoneyUtils.parse(txtValorPago.getText()),
                    MoneyUtils.parse(txtJuros.getText()),
                    MoneyUtils.parse(txtMulta.getText()),
                    MoneyUtils.parse(txtDesconto.getText()),
                    dpRecebimento.getValue(),
                    cmbForma.getValue(),
                    txtObs.getText());
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao baixar conta a receber", e);
            mostrarErro("Erro ao baixar conta: " + e.getMessage());
        }
    }

    @FXML
    private void cancelarSelecionada() {
        ContaReceber conta = tblContas.getSelectionModel().getSelectedItem();
        if (conta == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancelar a conta selecionada?\n" + conta.getDescricao(),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cancelar Conta a Receber");
        confirm.showAndWait()
                .filter(ButtonType.YES::equals)
                .ifPresent(bt -> executarCancelamento(conta));
    }

    private void executarCancelamento(ContaReceber conta) {
        try {
            contaReceberService.cancelar(conta.getId(), "Cancelada pelo módulo financeiro.");
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cancelar conta a receber", e);
            mostrarErro("Erro ao cancelar conta: " + e.getMessage());
        }
    }

    @FXML
    private void novaConta() {
        Integer empresaId = authService.getEmpresaIdLogado();
        List<Cliente> clientes = clienteService.listarAtivos(empresaId);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nova Conta a Receber");
        dialog.setHeaderText("Lançamento avulso de conta a receber");

        ComboBox<Cliente> cmbCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        cmbCliente.setPromptText("Selecione um cliente (opcional)");
        cmbCliente.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Cliente c) { return c == null ? "" : nomeCliente(c); }
            @Override public Cliente fromString(String s) { return null; }
        });

        TextField txtDescricao = new TextField();
        txtDescricao.setPromptText("Descrição da receita");
        TextField txtValor = new TextField("0,00");
        DatePicker dpEmissao    = new DatePicker(LocalDate.now());
        DatePicker dpVencimento = new DatePicker(LocalDate.now().plusDays(30));
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações");
        txtObs.setPrefRowCount(2);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Cliente"),      cmbCliente);
        grid.addRow(1, new Label("Descrição"),    txtDescricao);
        grid.addRow(2, new Label("Valor"),        txtValor);
        grid.addRow(3, new Label("Data emissão"), dpEmissao);
        grid.addRow(4, new Label("Vencimento"),   dpVencimento);
        grid.add(new Label("Observações"), 0, 5);
        grid.add(txtObs, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(bt -> salvarNovaContaAvulsa(cmbCliente, txtDescricao, txtValor,
                        dpEmissao, dpVencimento, txtObs, empresaId));
    }

    private void salvarNovaContaAvulsa(ComboBox<Cliente> cmbCliente, TextField txtDescricao,
                                       TextField txtValor, DatePicker dpEmissao,
                                       DatePicker dpVencimento, TextArea txtObs,
                                       Integer empresaId) {
        try {
            ContaReceber conta = ContaReceber.builder()
                    .empresa(com.erp.model.Empresa.builder().id(empresaId).build())
                    .cliente(cmbCliente.getValue())
                    .descricao(txtDescricao.getText() != null ? txtDescricao.getText().trim() : "")
                    .valor(MoneyUtils.parse(txtValor.getText()))
                    .dataEmissao(dpEmissao.getValue() != null ? dpEmissao.getValue() : LocalDate.now())
                    .dataVencimento(dpVencimento.getValue())
                    .observacoes(txtObs.getText() != null && !txtObs.getText().isBlank()
                            ? txtObs.getText().trim() : null)
                    .build();
            contaReceberService.cadastrarAvulsa(conta);
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao salvar conta a receber avulsa", e);
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    private boolean podeBaixar(ContaReceber conta) {
        return conta != null && "ABERTA".equals(conta.getStatus());
    }

    private BigDecimal valorPadraoBaixa(ContaReceber conta) {
        return nvl(conta.getValor()).add(nvl(conta.getJuros())).add(nvl(conta.getMulta()))
                .subtract(nvl(conta.getDesconto())).max(BigDecimal.ZERO);
    }

    private String formatarStatusEfetivo(ContaReceber conta) {
        if ("ABERTA".equals(conta.getStatus())
                && conta.getDataVencimento() != null
                && conta.getDataVencimento().isBefore(LocalDate.now())) {
            return "Vencida";
        }
        return switch (nvl(conta.getStatus())) {
            case "ABERTA"    -> "Aberta";
            case "RECEBIDA"  -> "Recebida";
            case "CANCELADA" -> "Cancelada";
            default -> nvl(conta.getStatus());
        };
    }

    private String styleStatus(String status) {
        return switch (status) {
            case "Recebida" -> "badge-success";
            case "Vencida"  -> "badge-danger";
            case "Cancelada" -> "badge-neutral";
            default -> "badge-info";
        };
    }

    private String nomeCliente(Cliente cliente) {
        if (cliente == null) return "—";
        if ("PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null
                && !cliente.getRazaoSocial().isBlank()) {
            return cliente.getRazaoSocial();
        }
        return nvl(cliente.getNome());
    }

    private String formatarForma(String forma) {
        if (forma == null || forma.isBlank()) return "—";
        return switch (forma) {
            case "DINHEIRO"      -> "Dinheiro";
            case "PIX"           -> "PIX";
            case "TRANSFERENCIA" -> "Transferência";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            case "CARTAO_CREDITO"-> "Cartão Crédito";
            case "CHEQUE"        -> "Cheque";
            default -> forma;
        };
    }

    private String formatarData(LocalDate data) {
        return data != null ? data.format(DATE_FMT) : "—";
    }

    private BigDecimal nvl(BigDecimal valor) { return valor != null ? valor : BigDecimal.ZERO; }
    private Integer    nvl(Integer valor)    { return valor != null ? valor : 0; }
    private String     nvl(String valor)     { return valor != null ? valor : ""; }

    private void mostrarAviso(String mensagem) {
        new Alert(Alert.AlertType.WARNING, mensagem, ButtonType.OK).showAndWait();
    }

    private void mostrarErro(String mensagem) {
        new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK).showAndWait();
    }
}
