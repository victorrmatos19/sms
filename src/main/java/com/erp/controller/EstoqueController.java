package com.erp.controller;

import com.erp.model.GrupoProduto;
import com.erp.model.MovimentacaoEstoque;
import com.erp.model.Produto;
import com.erp.repository.GrupoProdutoRepository;
import com.erp.service.AuthService;
import com.erp.service.EstoqueService;
import com.erp.util.MoneyUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class EstoqueController implements Initializable {

    private final EstoqueService estoqueService;
    private final AuthService authService;
    private final ApplicationContext springContext;
    private final GrupoProdutoRepository grupoProdutoRepository;

    @FXML private VBox rootPane;

    // Métricas
    @FXML private Label lblTotalProdutos;
    @FXML private Label lblValorTotal;
    @FXML private Label lblAbaixoMinimo;
    @FXML private Label lblZerados;

    // Posição
    @FXML private TextField txtBuscaEstoque;
    @FXML private ComboBox<String> cmbFiltroEstoque;
    @FXML private ComboBox<String> cmbFiltroGrupo;
    @FXML private TableView<Produto> tabelaPosicao;
    @FXML private TableColumn<Produto, String> colECodigo;
    @FXML private TableColumn<Produto, String> colEDescricao;
    @FXML private TableColumn<Produto, String> colEEstoque;
    @FXML private TableColumn<Produto, String> colEMinimo;
    @FXML private TableColumn<Produto, String> colECusto;
    @FXML private TableColumn<Produto, String> colEValor;
    @FXML private TableColumn<Produto, String> colESituacao;

    // Histórico
    @FXML private Tab tabHistorico;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFim;
    @FXML private ComboBox<String> cmbTipoHistorico;
    @FXML private TextField txtBuscaHistorico;
    @FXML private TableView<MovimentacaoEstoque> tabelaHistorico;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHData;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHProduto;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHTipo;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHOrigem;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHQtd;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHCusto;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHSaldoAnt;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHSaldoPost;
    @FXML private TableColumn<MovimentacaoEstoque, String> colHUsuario;

    private final ObservableList<Produto> todosProdutos = FXCollections.observableArrayList();
    private FilteredList<Produto> filteredProdutos;
    private final ObservableList<MovimentacaoEstoque> todasMovimentacoes = FXCollections.observableArrayList();
    private final ObservableList<MovimentacaoEstoque> movimentacoesFiltradas = FXCollections.observableArrayList();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat QTD_FMT = NumberFormat.getNumberInstance(new Locale("pt", "BR"));

    static {
        QTD_FMT.setMaximumFractionDigits(4);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFiltroEstoque();
        configurarColunasEstoque();
        configurarColunasHistorico();
        configurarFiltrosEstoque();
        configurarFiltrosHistorico();
        inicializarDatas();
        carregarDados();
    }

    private void configurarFiltroEstoque() {
        cmbFiltroEstoque.setItems(FXCollections.observableArrayList(
                "Todas", "Normal", "Abaixo do Mínimo", "Zerado", "Negativo"));
        cmbFiltroEstoque.setValue("Todas");
    }

    private void configurarColunasEstoque() {
        colECodigo.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getCodigoInterno())));
        colEDescricao.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colEEstoque.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getEstoqueAtual();
            String unidade = c.getValue().getUnidade() != null ? " " + c.getValue().getUnidade().getSigla() : "";
            return new SimpleStringProperty(v != null ? QTD_FMT.format(v) + unidade : "0" + unidade);
        });
        colEMinimo.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getEstoqueMinimo();
            return new SimpleStringProperty(v != null ? QTD_FMT.format(v) : "0");
        });
        colECusto.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getPrecoCusto();
            return new SimpleStringProperty(MoneyUtils.formatCurrency(v));
        });
        colEValor.setCellValueFactory(c -> {
            BigDecimal est = c.getValue().getEstoqueAtual() != null ? c.getValue().getEstoqueAtual() : BigDecimal.ZERO;
            BigDecimal cst = c.getValue().getPrecoCusto()   != null ? c.getValue().getPrecoCusto()   : BigDecimal.ZERO;
            return new SimpleStringProperty(MoneyUtils.formatCurrency(est.multiply(cst)));
        });
        colESituacao.setCellValueFactory(c -> new SimpleStringProperty(""));
        colESituacao.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); setText(null); return;
                }
                Produto p = getTableRow().getItem();
                BigDecimal atual  = p.getEstoqueAtual()  != null ? p.getEstoqueAtual()  : BigDecimal.ZERO;
                BigDecimal minimo = p.getEstoqueMinimo() != null ? p.getEstoqueMinimo() : BigDecimal.ZERO;
                Label badge = new Label();
                if (atual.compareTo(BigDecimal.ZERO) < 0) {
                    badge.setText("Negativo"); badge.getStyleClass().add("badge-danger");
                } else if (atual.compareTo(BigDecimal.ZERO) == 0) {
                    badge.setText("Zerado"); badge.getStyleClass().add("badge-danger");
                } else if (atual.compareTo(minimo) < 0) {
                    badge.setText("Abaixo do Mínimo"); badge.getStyleClass().add("badge-warning");
                } else {
                    badge.setText("Normal"); badge.getStyleClass().add("badge-success");
                }
                setGraphic(badge); setText(null);
            }
        });

        // Duplo clique → filtra histórico pelo produto selecionado
        tabelaPosicao.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                Produto selecionado = tabelaPosicao.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    Integer empresaId = authService.getEmpresaIdLogado();
                    List<MovimentacaoEstoque> historicoProduto =
                            estoqueService.buscarHistoricoPorProduto(empresaId, selecionado.getId());
                    todasMovimentacoes.setAll(historicoProduto);
                    movimentacoesFiltradas.setAll(historicoProduto);
                    // Navega para a aba de histórico
                    tabHistorico.getTabPane().getSelectionModel().select(tabHistorico);
                }
            }
        });
    }

    private void configurarColunasHistorico() {
        colHData.setCellValueFactory(c -> {
            LocalDateTime dt = c.getValue().getCriadoEm();
            return new SimpleStringProperty(dt != null ? dt.format(DT_FMT) : "—");
        });
        colHProduto.setCellValueFactory(c -> {
            Produto p = c.getValue().getProduto();
            return new SimpleStringProperty(p != null ? p.getDescricao() : "—");
        });
        // Badge colorido para tipo
        colHTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo()));
        colHTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(formatarTipo(item));
                badge.getStyleClass().add(badgePorTipo(item));
                setGraphic(badge); setText(null);
            }
        });
        colHOrigem.setCellValueFactory(c -> new SimpleStringProperty(formatarOrigem(c.getValue().getOrigem())));
        colHQtd.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getQuantidade();
            return new SimpleStringProperty(v != null ? QTD_FMT.format(v) : "0");
        });
        colHCusto.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getCustoUnitario();
            return new SimpleStringProperty(v != null ? MoneyUtils.formatCurrency(v) : "—");
        });
        colHSaldoAnt.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getSaldoAnterior();
            return new SimpleStringProperty(v != null ? QTD_FMT.format(v) : "0");
        });
        colHSaldoPost.setCellValueFactory(c -> {
            BigDecimal v = c.getValue().getSaldoPosterior();
            return new SimpleStringProperty(v != null ? QTD_FMT.format(v) : "0");
        });
        colHUsuario.setCellValueFactory(c -> {
            var u = c.getValue().getUsuario();
            return new SimpleStringProperty(u != null ? u.getNome() : "—");
        });
        tabelaHistorico.setItems(movimentacoesFiltradas);
    }

    private void configurarFiltrosEstoque() {
        filteredProdutos = new FilteredList<>(todosProdutos, p -> true);
        tabelaPosicao.setItems(filteredProdutos);

        txtBuscaEstoque.textProperty().addListener((obs, old, val) -> aplicarFiltroEstoque());
        cmbFiltroEstoque.valueProperty().addListener((obs, old, val) -> aplicarFiltroEstoque());
        cmbFiltroGrupo.valueProperty().addListener((obs, old, val) -> aplicarFiltroEstoque());
    }

    private void configurarFiltrosHistorico() {
        cmbTipoHistorico.setItems(FXCollections.observableArrayList(
                "Todos", "Entrada", "Saída", "Ajuste Positivo", "Ajuste Negativo"));
        cmbTipoHistorico.setValue("Todos");
        cmbTipoHistorico.valueProperty().addListener((obs, old, val) -> aplicarFiltroHistorico());
        txtBuscaHistorico.textProperty().addListener((obs, old, val) -> aplicarFiltroHistorico());
    }

    private void inicializarDatas() {
        dpFim.setValue(LocalDate.now());
        dpInicio.setValue(LocalDate.now().minusMonths(1));
    }

    void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosProdutos.setAll(estoqueService.listarPosicaoEstoque(empresaId));
        List<MovimentacaoEstoque> movs = estoqueService.buscarHistorico(empresaId);
        todasMovimentacoes.setAll(movs);
        movimentacoesFiltradas.setAll(movs);
        carregarGrupos(empresaId);
        atualizarMetricas();
    }

    private void carregarGrupos(Integer empresaId) {
        ObservableList<String> grupos = FXCollections.observableArrayList();
        grupos.add("Todos");
        grupoProdutoRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId)
                .forEach(g -> grupos.add(g.getNome()));
        cmbFiltroGrupo.setItems(grupos);
        cmbFiltroGrupo.setValue("Todos");
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotalProdutos.setText(String.valueOf(todosProdutos.size()));
        lblValorTotal.setText(MoneyUtils.formatCurrency(estoqueService.calcularValorTotalEstoque(empresaId)));

        long abaixo = todosProdutos.stream()
                .filter(p -> p.getEstoqueAtual() != null && p.getEstoqueMinimo() != null
                        && p.getEstoqueAtual().compareTo(BigDecimal.ZERO) > 0
                        && p.getEstoqueAtual().compareTo(p.getEstoqueMinimo()) < 0)
                .count();
        lblAbaixoMinimo.setText(String.valueOf(abaixo));

        long zerados = todosProdutos.stream()
                .filter(p -> {
                    BigDecimal atual = p.getEstoqueAtual() != null ? p.getEstoqueAtual() : BigDecimal.ZERO;
                    return atual.compareTo(BigDecimal.ZERO) == 0;
                })
                .count();
        lblZerados.setText(String.valueOf(zerados));
    }

    private void aplicarFiltroEstoque() {
        String busca = txtBuscaEstoque.getText() == null ? "" : txtBuscaEstoque.getText().toLowerCase().trim();
        String situacao = cmbFiltroEstoque.getValue();
        String grupo = cmbFiltroGrupo.getValue();

        filteredProdutos.setPredicate(p -> {
            if (!busca.isEmpty()) {
                String desc = p.getDescricao() != null ? p.getDescricao().toLowerCase() : "";
                String cod  = p.getCodigoInterno() != null ? p.getCodigoInterno().toLowerCase() : "";
                if (!desc.contains(busca) && !cod.contains(busca)) return false;
            }
            if (grupo != null && !"Todos".equals(grupo)) {
                String grupoNome = p.getGrupo() != null ? p.getGrupo().getNome() : "";
                if (!grupo.equals(grupoNome)) return false;
            }
            if (situacao != null && !"Todas".equals(situacao)) {
                BigDecimal atual  = p.getEstoqueAtual()  != null ? p.getEstoqueAtual()  : BigDecimal.ZERO;
                BigDecimal minimo = p.getEstoqueMinimo() != null ? p.getEstoqueMinimo() : BigDecimal.ZERO;
                return switch (situacao) {
                    case "Normal"           -> atual.compareTo(BigDecimal.ZERO) > 0 && atual.compareTo(minimo) >= 0;
                    case "Abaixo do Mínimo" -> atual.compareTo(minimo) < 0 && atual.compareTo(BigDecimal.ZERO) > 0;
                    case "Zerado"           -> atual.compareTo(BigDecimal.ZERO) == 0;
                    case "Negativo"         -> atual.compareTo(BigDecimal.ZERO) < 0;
                    default -> true;
                };
            }
            return true;
        });
    }

    private void aplicarFiltroHistorico() {
        String busca = txtBuscaHistorico.getText() == null ? "" : txtBuscaHistorico.getText().toLowerCase().trim();
        String tipoSel = cmbTipoHistorico.getValue();

        List<MovimentacaoEstoque> filtrados = todasMovimentacoes.stream()
                .filter(m -> {
                    if (!busca.isEmpty()) {
                        Produto p = m.getProduto();
                        String desc = p != null && p.getDescricao() != null ? p.getDescricao().toLowerCase() : "";
                        if (!desc.contains(busca)) return false;
                    }
                    if (tipoSel != null && !"Todos".equals(tipoSel)) {
                        String tipoDisplay = formatarTipo(m.getTipo());
                        if (!tipoSel.equals(tipoDisplay)) return false;
                    }
                    return true;
                })
                .toList();
        movimentacoesFiltradas.setAll(filtrados);
    }

    @FXML
    private void filtrarHistorico() {
        Integer empresaId = authService.getEmpresaIdLogado();
        if (dpInicio.getValue() != null && dpFim.getValue() != null) {
            LocalDateTime inicio = dpInicio.getValue().atStartOfDay();
            LocalDateTime fim    = dpFim.getValue().atTime(23, 59, 59);
            List<MovimentacaoEstoque> movs =
                    estoqueService.buscarHistoricoPorPeriodo(empresaId, inicio, fim);
            todasMovimentacoes.setAll(movs);
            aplicarFiltroHistorico();
        }
    }

    @FXML
    private void abrirRegistrarMovimentacao() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/movimentacao-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent form = loader.load();

            MovimentacaoFormController ctrl = loader.getController();
            ctrl.setOnSucessoCallback(this::carregarDados);

            Stage dialog = new Stage();
            dialog.setTitle("Registrar Movimentação");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(rootPane.getScene().getWindow());

            Scene scene = new Scene(form);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/css/global.css")).toExternalForm());
            form.getStyleClass().add(
                    rootPane.getScene().getRoot().getStyleClass()
                            .stream().filter(c -> c.startsWith("theme-")).findFirst().orElse("theme-dark"));
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Erro ao abrir formulário de movimentação", e);
            new Alert(Alert.AlertType.ERROR,
                    "Erro ao abrir formulário: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private String formatarTipo(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo) {
            case "ENTRADA"         -> "Entrada";
            case "SAIDA"           -> "Saída";
            case "AJUSTE_POSITIVO" -> "Ajuste Positivo";
            case "AJUSTE_NEGATIVO" -> "Ajuste Negativo";
            default -> tipo;
        };
    }

    private String badgePorTipo(String tipo) {
        if (tipo == null) return "badge-neutral";
        return switch (tipo) {
            case "ENTRADA"         -> "badge-success";
            case "SAIDA"           -> "badge-danger";
            case "AJUSTE_POSITIVO" -> "badge-info";
            case "AJUSTE_NEGATIVO" -> "badge-warning";
            default -> "badge-neutral";
        };
    }

    private String formatarOrigem(String origem) {
        if (origem == null) return "—";
        return switch (origem) {
            case "AJUSTE_MANUAL" -> "Manual";
            case "COMPRA"        -> "Compra";
            case "VENDA"         -> "Venda";
            default -> origem;
        };
    }

    private String nvl(String v) { return v != null ? v : ""; }
}
