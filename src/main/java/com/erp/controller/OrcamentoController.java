package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Orcamento;
import com.erp.service.AuthService;
import com.erp.service.OrcamentoPdfService;
import com.erp.service.OrcamentoService;
import javafx.application.Platform;
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
public class OrcamentoController implements Initializable {

    private final OrcamentoService orcamentoService;
    private final OrcamentoPdfService orcamentoPdfService;
    private final AuthService authService;
    private final ApplicationContext springContext;

    @FXML private VBox rootPane;

    // Métricas
    @FXML private Label cardAbertos;
    @FXML private Label cardAVencer;
    @FXML private Label cardConvertidosMes;
    @FXML private Label cardTaxaConversao;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private ComboBox<String> cmbFiltroStatus;
    @FXML private CheckBox chkApenasDosMeus;

    // Botões de ação
    @FXML private Button btnEditar;
    @FXML private Button btnConverter;
    @FXML private Button btnCancelarOrcamento;
    @FXML private Button btnImprimirPdf;

    // Tabela
    @FXML private TableView<Orcamento> tabelaOrcamentos;
    @FXML private TableColumn<Orcamento, String> colNumero;
    @FXML private TableColumn<Orcamento, String> colCliente;
    @FXML private TableColumn<Orcamento, String> colVendedor;
    @FXML private TableColumn<Orcamento, String> colEmissao;
    @FXML private TableColumn<Orcamento, String> colValidade;
    @FXML private TableColumn<Orcamento, String> colValorTotal;
    @FXML private TableColumn<Orcamento, String> colStatus;

    private final ObservableList<Orcamento> todosOrcamentos = FXCollections.observableArrayList();
    private FilteredList<Orcamento> filteredOrcamentos;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltroStatus();
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        expirarVencidosECarregar();
    }

    private void expirarVencidosECarregar() {
        Integer empresaId = authService.getEmpresaIdLogado();
        int expirados = orcamentoService.expirarVencidos(empresaId);
        if (expirados > 0) {
            log.info("{} orçamento(s) expirado(s) automaticamente", expirados);
        }
        carregarOrcamentos();
    }

    private void configurarFiltroStatus() {
        cmbFiltroStatus.setItems(FXCollections.observableArrayList(
                "Todos", "Aberto", "Convertido", "Cancelado", "Expirado"));
        cmbFiltroStatus.setValue("Todos");
    }

    private void configurarColunas() {
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNumero())));

        colCliente.setCellValueFactory(c -> {
            var cl = c.getValue().getCliente();
            if (cl == null) return new SimpleStringProperty("—");
            String nome = "PJ".equals(cl.getTipoPessoa()) && cl.getRazaoSocial() != null
                    ? cl.getRazaoSocial() : cl.getNome();
            return new SimpleStringProperty(nvl(nome));
        });

        colVendedor.setCellValueFactory(c -> {
            var v = c.getValue().getVendedor();
            return new SimpleStringProperty(v != null ? nvl(v.getNome()) : "—");
        });

        colEmissao.setCellValueFactory(c -> {
            var d = c.getValue().getDataEmissao();
            return new SimpleStringProperty(d != null ? d.format(DATE_FMT) : "—");
        });

        colValidade.setCellValueFactory(c -> {
            var d = c.getValue().getDataValidade();
            return new SimpleStringProperty(d != null ? d.format(DATE_FMT) : "—");
        });

        colValorTotal.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getValorTotal();
            return new SimpleStringProperty(v != null ? CURRENCY_FMT.format(v) : "R$ 0,00");
        });

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
                    case "ABERTO"     -> { badge.setText("Aberto");     badge.getStyleClass().add("badge-success"); }
                    case "CONVERTIDO" -> { badge.setText("Convertido"); badge.getStyleClass().add("badge-info"); }
                    case "CANCELADO"  -> { badge.setText("Cancelado");  badge.getStyleClass().add("badge-danger"); }
                    case "EXPIRADO"   -> { badge.setText("Expirado");   badge.getStyleClass().add("badge-neutral"); }
                    default           -> { badge.setText(status);       badge.getStyleClass().add("badge-neutral"); }
                }
                setGraphic(badge); setText(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void configurarFiltros() {
        filteredOrcamentos = new FilteredList<>(todosOrcamentos, o -> true);
        SortedList<Orcamento> sorted = new SortedList<>(filteredOrcamentos);
        sorted.comparatorProperty().bind(tabelaOrcamentos.comparatorProperty());
        tabelaOrcamentos.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        cmbFiltroStatus.valueProperty().addListener((obs, old, val) -> aplicarFiltro());
        chkApenasDosMeus.selectedProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tabelaOrcamentos.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean tem = sel != null;
            boolean aberto = tem && "ABERTO".equals(sel.getStatus());
            btnEditar.setDisable(!aberto);
            btnConverter.setDisable(!aberto);
            btnCancelarOrcamento.setDisable(!aberto);
            btnImprimirPdf.setDisable(!tem);
        });

        tabelaOrcamentos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tabelaOrcamentos.getSelectionModel().getSelectedItem() != null) {
                abrirFormulario(tabelaOrcamentos.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void carregarOrcamentos() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosOrcamentos.setAll(orcamentoService.listarPorEmpresa(empresaId));
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        long abertos = orcamentoService.contarAbertos(empresaId);
        long aVencer = orcamentoService.contarAVencerHojeAmanha(empresaId);
        BigDecimal convertidosMes = orcamentoService.valorConvertidosMes(empresaId);
        BigDecimal taxa = orcamentoService.calcularTaxaConversaoMes(empresaId);

        cardAbertos.setText(String.valueOf(abertos));
        cardAVencer.setText(String.valueOf(aVencer));
        cardConvertidosMes.setText(CURRENCY_FMT.format(convertidosMes));
        cardTaxaConversao.setText(taxa + "%");
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        String statusFiltro = cmbFiltroStatus.getValue();
        boolean apenasDosMeus = chkApenasDosMeus.isSelected();
        Integer usuarioId = authService.getUsuarioLogado() != null
                ? authService.getUsuarioLogado().getId() : null;

        filteredOrcamentos.setPredicate(o -> {
            if (statusFiltro != null && !"Todos".equals(statusFiltro)) {
                if (!statusFiltro.toUpperCase().equals(o.getStatus())) return false;
            }
            if (!busca.isEmpty()) {
                String numero = o.getNumero() != null ? o.getNumero().toLowerCase() : "";
                String cliente = "";
                if (o.getCliente() != null) {
                    cliente = "PJ".equals(o.getCliente().getTipoPessoa())
                            && o.getCliente().getRazaoSocial() != null
                            ? o.getCliente().getRazaoSocial().toLowerCase()
                            : o.getCliente().getNome() != null
                                ? o.getCliente().getNome().toLowerCase() : "";
                }
                if (!numero.contains(busca) && !cliente.contains(busca)) return false;
            }
            if (apenasDosMeus && usuarioId != null) {
                if (o.getUsuario() == null || !usuarioId.equals(o.getUsuario().getId())) return false;
            }
            return true;
        });
    }

    @FXML
    private void abrirNovo() {
        abrirFormulario(null);
    }

    @FXML
    private void editarSelecionado() {
        Orcamento sel = tabelaOrcamentos.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    @FXML
    private void converterSelecionado() {
        Orcamento sel = tabelaOrcamentos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            Orcamento comItens = orcamentoService.buscarComItens(sel.getId()).orElse(sel);
            abrirDialogConversao(comItens);
        } catch (Exception e) {
            log.error("Erro ao abrir dialog de conversão", e);
            new Alert(Alert.AlertType.ERROR, "Erro: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelarSelecionado() {
        Orcamento sel = tabelaOrcamentos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancelar o orçamento '" + sel.getNumero() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Cancelar Orçamento");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    orcamentoService.cancelar(sel.getId());
                    carregarOrcamentos();
                } catch (NegocioException e) {
                    new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
                } catch (Exception e) {
                    log.error("Erro ao cancelar orçamento", e);
                    new Alert(Alert.AlertType.ERROR, "Erro: " + e.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        });
    }

    @FXML
    private void imprimirPdfSelecionado() {
        Orcamento sel = tabelaOrcamentos.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            orcamentoPdfService.abrirPdfEmVisualizador(sel.getId());
        } catch (Exception e) {
            log.error("Erro ao gerar PDF", e);
            new Alert(Alert.AlertType.ERROR, "Erro ao gerar PDF:\n" + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void abrirFormulario(Orcamento orcamento) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/orcamento-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent formView = loader.load();

            OrcamentoFormController ctrl = loader.getController();
            StackPane pane = (StackPane) rootPane.getParent();
            Orcamento comItens = orcamento != null
                    ? orcamentoService.buscarComItens(orcamento.getId()).orElse(orcamento)
                    : null;
            ctrl.setOrcamento(comItens);
            ctrl.setConteudoPane(pane);
            pane.getChildren().setAll(formView);
        } catch (Exception e) {
            log.error("Erro ao abrir formulário de orçamento", e);
            new Alert(Alert.AlertType.ERROR,
                    "Erro ao abrir formulário: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void abrirDialogConversao(Orcamento orcamento) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/orcamento-conversao.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent content = loader.load();

            OrcamentoConversaoController ctrl = loader.getController();
            ctrl.setOrcamento(orcamento);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Converter em Venda");
            dialog.setHeaderText(null);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            // ocultar o botão padrão — o controller gerencia o fechamento
            Platform.runLater(() -> {
                var closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
                if (closeBtn != null) closeBtn.setVisible(false);
            });
            ctrl.setDialog(dialog);
            ctrl.setOnConversaoRealizada(() -> {
                dialog.close();
                carregarOrcamentos();
            });
            dialog.showAndWait();
        } catch (Exception e) {
            log.error("Erro ao abrir dialog de conversão", e);
            new Alert(Alert.AlertType.ERROR,
                    "Erro ao abrir conversão: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private String nvl(String v) { return v != null ? v : "—"; }
}
