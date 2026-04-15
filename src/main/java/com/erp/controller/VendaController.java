package com.erp.controller;

import com.erp.model.Venda;
import com.erp.service.AuthService;
import com.erp.service.VendaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
public class VendaController implements Initializable {

    private final VendaService vendaService;
    private final AuthService authService;
    private final ApplicationContext springContext;

    @FXML private VBox rootPane;

    @FXML private Label lblTotalMes;
    @FXML private Label lblValorMes;
    @FXML private Label lblFinalizadas;

    @FXML private TextField txtBusca;
    @FXML private ComboBox<String> cmbFiltroStatus;

    @FXML private Button btnVisualizar;

    @FXML private TableView<Venda> tblVendas;
    @FXML private TableColumn<Venda, String> colNumero;
    @FXML private TableColumn<Venda, String> colCliente;
    @FXML private TableColumn<Venda, String> colDataVenda;
    @FXML private TableColumn<Venda, String> colVendedor;
    @FXML private TableColumn<Venda, String> colValorTotal;
    @FXML private TableColumn<Venda, String> colFormaPagamento;
    @FXML private TableColumn<Venda, String> colStatus;

    private final ObservableList<Venda> todasVendas = FXCollections.observableArrayList();
    private FilteredList<Venda> filteredVendas;

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltroStatus();
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarVendas();
    }

    private void configurarFiltroStatus() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todos", "Finalizada", "Cancelada"));
        cmbFiltroStatus.setValue("Todos");
    }

    private void configurarColunas() {
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNumero())));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(nomeCliente(c.getValue())));
        colVendedor.setCellValueFactory(c -> {
            var vendedor = c.getValue().getVendedor();
            return new SimpleStringProperty(vendedor != null ? vendedor.getNome() : "—");
        });
        colDataVenda.setCellValueFactory(c -> {
            var data = c.getValue().getDataVenda();
            return new SimpleStringProperty(data != null ? data.format(DATE_TIME_FMT) : "—");
        });
        colValorTotal.setCellValueFactory(c -> {
            BigDecimal valor = c.getValue().getValorTotal();
            return new SimpleStringProperty(valor != null ? CURRENCY_FORMAT.format(valor) : "R$ 0,00");
        });
        colFormaPagamento.setCellValueFactory(c -> {
            if (c.getValue().getPagamentos() == null || c.getValue().getPagamentos().isEmpty()) {
                return new SimpleStringProperty("—");
            }
            return new SimpleStringProperty(formatarFormaPagamento(
                    c.getValue().getPagamentos().get(0).getFormaPagamento()));
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String status = getTableRow().getItem().getStatus();
                Label badge = new Label();
                switch (status) {
                    case "FINALIZADA" -> {
                        badge.setText("Finalizada");
                        badge.getStyleClass().add("badge-success");
                    }
                    case "CANCELADA" -> {
                        badge.setText("Cancelada");
                        badge.getStyleClass().add("badge-danger");
                    }
                    default -> {
                        badge.setText(status);
                        badge.getStyleClass().add("badge-neutral");
                    }
                }
                setGraphic(badge);
                setText(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void configurarFiltros() {
        filteredVendas = new FilteredList<>(todasVendas, venda -> true);
        SortedList<Venda> sorted = new SortedList<>(filteredVendas);
        sorted.comparatorProperty().bind(tblVendas.comparatorProperty());
        tblVendas.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblVendas.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
                btnVisualizar.setDisable(sel == null));

        tblVendas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tblVendas.getSelectionModel().getSelectedItem() != null) {
                abrirFormulario(tblVendas.getSelectionModel().getSelectedItem());
            }
        });
    }

    void carregarVendas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todasVendas.setAll(vendaService.listarPorEmpresa(empresaId));
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotalMes.setText(String.valueOf(vendaService.contarVendasMes(empresaId)));
        lblValorMes.setText(CURRENCY_FORMAT.format(vendaService.calcularValorTotalMes(empresaId)));
        lblFinalizadas.setText(String.valueOf(vendaService.contarFinalizadas(empresaId)));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        String statusFiltro = cmbFiltroStatus.getValue();

        filteredVendas.setPredicate(v -> {
            if (statusFiltro != null && !"Todos".equals(statusFiltro)) {
                String esperado = statusFiltro.toUpperCase();
                if (!esperado.equals(v.getStatus())) return false;
            }
            if (!busca.isEmpty()) {
                String numero = v.getNumero() != null ? v.getNumero().toLowerCase() : "";
                String cliente = nomeCliente(v).toLowerCase();
                String vendedor = v.getVendedor() != null && v.getVendedor().getNome() != null
                        ? v.getVendedor().getNome().toLowerCase() : "";
                return numero.contains(busca) || cliente.contains(busca) || vendedor.contains(busca);
            }
            return true;
        });
    }

    @FXML
    private void abrirNovaVenda() {
        abrirFormulario(null);
    }

    @FXML
    private void visualizarSelecionada() {
        Venda selecionada = tblVendas.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            abrirFormulario(selecionada);
        }
    }

    private void abrirFormulario(Venda venda) {
        try {
            Venda vendaParaForm = venda != null
                    ? vendaService.buscarComItens(venda.getId()).orElse(venda)
                    : null;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/venda-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent formView = loader.load();

            VendaFormController ctrl = loader.getController();
            StackPane pane = (StackPane) rootPane.getParent();
            ctrl.setVenda(vendaParaForm);
            ctrl.setConteudoPane(pane);

            pane.getChildren().setAll(formView);
        } catch (Exception e) {
            log.error("Erro ao abrir formulário de venda", e);
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Erro ao abrir formulário: " + e.getMessage(),
                    javafx.scene.control.ButtonType.OK).showAndWait();
        }
    }

    private String nomeCliente(Venda venda) {
        var cliente = venda.getCliente();
        if (cliente == null) return "Consumidor final";
        return "PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null
                ? cliente.getRazaoSocial() : nvl(cliente.getNome());
    }

    private String formatarFormaPagamento(String forma) {
        if (forma == null) return "—";
        return switch (forma) {
            case "DINHEIRO" -> "Dinheiro";
            case "PIX" -> "PIX";
            case "CARTAO_CREDITO" -> "Cartão Crédito";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            case "PRAZO" -> "Prazo";
            case "CREDIARIO" -> "Crediário";
            default -> forma;
        };
    }

    private String nvl(String valor) {
        return valor != null ? valor : "—";
    }
}
