package com.erp.controller;

import com.erp.model.Compra;
import com.erp.service.AuthService;
import com.erp.service.CompraService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompraController implements Initializable {

    private final CompraService compraService;
    private final AuthService authService;
    private final ApplicationContext springContext;

    @FXML private VBox rootPane;

    // Métricas
    @FXML private Label lblTotalMes;
    @FXML private Label lblValorMes;
    @FXML private Label lblRascunhos;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private ComboBox<String> cmbFiltroStatus;

    // Botões de ação
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelarCompra;
    @FXML private Button btnVisualizar;

    // Tabela
    @FXML private TableView<Compra> tblCompras;
    @FXML private TableColumn<Compra, String> colNumero;
    @FXML private TableColumn<Compra, String> colFornecedor;
    @FXML private TableColumn<Compra, String> colDataEmissao;
    @FXML private TableColumn<Compra, String> colPrevisao;
    @FXML private TableColumn<Compra, String> colValorTotal;
    @FXML private TableColumn<Compra, String> colStatus;
    @FXML private TableColumn<Compra, String> colCondicao;

    private final ObservableList<Compra> todasCompras = FXCollections.observableArrayList();
    private FilteredList<Compra> filteredCompras;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltroStatus();
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarCompras();
    }

    private void configurarFiltroStatus() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todos", "Rascunho", "Confirmada", "Cancelada"));
        cmbFiltroStatus.setValue("Todos");
    }

    private void configurarColunas() {
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNumeroDocumento())));

        colFornecedor.setCellValueFactory(c -> {
            var f = c.getValue().getFornecedor();
            if (f == null) return new SimpleStringProperty("—");
            String nome = "PJ".equals(f.getTipoPessoa()) && f.getRazaoSocial() != null
                    ? f.getRazaoSocial() : f.getNome();
            return new SimpleStringProperty(nome);
        });

        colDataEmissao.setCellValueFactory(c -> {
            var d = c.getValue().getDataEmissao();
            return new SimpleStringProperty(d != null ? d.format(DATE_FMT) : "—");
        });

        colPrevisao.setCellValueFactory(c -> {
            var d = c.getValue().getDataPrevisao();
            return new SimpleStringProperty(d != null ? d.format(DATE_FMT) : "—");
        });

        colValorTotal.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getValorTotal();
            return new SimpleStringProperty(v != null ? CURRENCY_FORMAT.format(v) : "R$ 0,00");
        });

        colCondicao.setCellValueFactory(c ->
                new SimpleStringProperty(formatarCondicao(c.getValue().getCondicaoPagamento())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null); return;
                }
                String status = getTableRow().getItem().getStatus();
                Label badge = new Label();
                switch (status) {
                    case "RASCUNHO"   -> { badge.setText("Rascunho");   badge.getStyleClass().add("badge-warning"); }
                    case "CONFIRMADA" -> { badge.setText("Confirmada"); badge.getStyleClass().add("badge-success"); }
                    case "CANCELADA"  -> { badge.setText("Cancelada");  badge.getStyleClass().add("badge-danger"); }
                    default           -> { badge.setText(status);       badge.getStyleClass().add("badge-neutral"); }
                }
                setGraphic(badge); setText(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void configurarFiltros() {
        filteredCompras = new FilteredList<>(todasCompras, c -> true);
        SortedList<Compra> sorted = new SortedList<>(filteredCompras);
        sorted.comparatorProperty().bind(tblCompras.comparatorProperty());
        tblCompras.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblCompras.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean tem = sel != null;
            boolean rascunho = tem && "RASCUNHO".equals(sel.getStatus());
            boolean finalizada = tem && !"RASCUNHO".equals(sel.getStatus());
            btnConfirmar.setDisable(!rascunho);
            btnCancelarCompra.setDisable(!rascunho);
            btnVisualizar.setDisable(!finalizada);
        });

        tblCompras.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tblCompras.getSelectionModel().getSelectedItem() != null) {
                abrirFormulario(tblCompras.getSelectionModel().getSelectedItem());
            }
        });
    }

    void carregarCompras() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todasCompras.setAll(compraService.listarPorEmpresa(empresaId));
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotalMes.setText(String.valueOf(compraService.contarComprasMes(empresaId)));
        lblValorMes.setText(CURRENCY_FORMAT.format(compraService.calcularValorTotalMes(empresaId)));
        lblRascunhos.setText(String.valueOf(compraService.contarRascunhos(empresaId)));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        String statusFiltro = cmbFiltroStatus.getValue();

        filteredCompras.setPredicate(c -> {
            if (statusFiltro != null && !"Todos".equals(statusFiltro)) {
                String statusEsperado = statusFiltro.toUpperCase();
                if (!statusEsperado.equals(c.getStatus())) return false;
            }
            if (!busca.isEmpty()) {
                String numero = c.getNumeroDocumento() != null ? c.getNumeroDocumento().toLowerCase() : "";
                String fornecedor = "";
                if (c.getFornecedor() != null) {
                    fornecedor = "PJ".equals(c.getFornecedor().getTipoPessoa())
                            && c.getFornecedor().getRazaoSocial() != null
                            ? c.getFornecedor().getRazaoSocial().toLowerCase()
                            : c.getFornecedor().getNome() != null ? c.getFornecedor().getNome().toLowerCase() : "";
                }
                if (!numero.contains(busca) && !fornecedor.contains(busca)) return false;
            }
            return true;
        });
    }

    @FXML private void abrirNovo() { abrirFormulario(null); }

    @FXML
    private void confirmarSelecionada() {
        Compra sel = tblCompras.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmar a compra '" + nvl(sel.getNumeroDocumento()) + "'?\n"
                        + "Isso dará entrada no estoque e gerará contas a pagar.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmar Compra");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    compraService.confirmarCompra(sel.getId(), 1);
                    carregarCompras();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        });
    }

    @FXML
    private void cancelarSelecionada() {
        Compra sel = tblCompras.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancelar a compra '" + nvl(sel.getNumeroDocumento()) + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Cancelar Compra");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    compraService.cancelarCompra(sel.getId());
                    carregarCompras();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        });
    }

    @FXML
    private void visualizarSelecionada() {
        Compra sel = tblCompras.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    private void abrirFormulario(Compra compra) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/compra-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent formView = loader.load();

            CompraFormController ctrl = loader.getController();
            StackPane pane = (StackPane) rootPane.getParent();
            ctrl.setCompra(compra);
            ctrl.setConteudoPane(pane);

            pane.getChildren().setAll(formView);
        } catch (Exception e) {
            log.error("Erro ao abrir formulário de compra", e);
            new Alert(Alert.AlertType.ERROR,
                    "Erro ao abrir formulário: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private String formatarCondicao(String c) {
        if (c == null) return "—";
        return switch (c) {
            case "A_VISTA"       -> "À Vista";
            case "30_DIAS"       -> "30 Dias";
            case "60_DIAS"       -> "60 Dias";
            case "90_DIAS"       -> "90 Dias";
            case "30_60_DIAS"    -> "30/60 Dias";
            case "30_60_90_DIAS" -> "30/60/90 Dias";
            case "PERSONALIZADO" -> "Personalizado";
            default -> c;
        };
    }

    private String nvl(String v) { return v != null ? v : "—"; }
}
