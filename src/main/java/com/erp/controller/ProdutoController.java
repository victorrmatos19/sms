package com.erp.controller;

import com.erp.StageManager;
import com.erp.model.GrupoProduto;
import com.erp.model.Produto;
import com.erp.repository.GrupoProdutoRepository;
import com.erp.service.AuthService;
import com.erp.service.ProdutoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller da listagem de produtos.
 * Filtros aplicados em memória via FilteredList — adequado para pequenas empresas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProdutoController implements Initializable {

    private final ProdutoService produtoService;
    private final GrupoProdutoRepository grupoProdutoRepository;
    private final AuthService authService;
    private final StageManager stageManager;
    private final ApplicationContext springContext;

    // Métricas
    @FXML private Label lblTotal;
    @FXML private Label lblAtivos;
    @FXML private Label lblAbaixoMinimo;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private ComboBox<GrupoProduto> cmbGrupoFiltro;
    @FXML private CheckBox chkInativos;

    // Ações
    @FXML private Button btnEditar;
    @FXML private Button btnToggleAtivo;

    // Tabela
    @FXML private TableView<Produto> tblProdutos;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colDescricao;
    @FXML private TableColumn<Produto, String> colUnidade;
    @FXML private TableColumn<Produto, String> colPrecoVenda;
    @FXML private TableColumn<Produto, String> colEstoque;
    @FXML private TableColumn<Produto, String> colStatus;

    private final ObservableList<Produto> todosProdutos = FXCollections.observableArrayList();
    private FilteredList<Produto> filteredProdutos;

    private static final NumberFormat CURRENCY_FORMAT =
        NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat DECIMAL_FORMAT =
        NumberFormat.getInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarGruposFiltro();
        carregarProdutos();
    }

    // ---- Configuração da tabela ----

    private void configurarColunas() {
        colCodigo.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getCodigoInterno())));

        colDescricao.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDescricao()));

        colUnidade.setCellValueFactory(c -> {
            var un = c.getValue().getUnidade();
            return new SimpleStringProperty(un != null ? un.getSigla() : "—");
        });

        colPrecoVenda.setCellValueFactory(c ->
            new SimpleStringProperty(CURRENCY_FORMAT.format(c.getValue().getPrecoVenda())));

        colEstoque.setCellValueFactory(c ->
            new SimpleStringProperty(DECIMAL_FORMAT.format(c.getValue().getEstoqueAtual())));

        // Coluna de status com badge
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Produto p = getTableRow().getItem();
                Label badge = new Label(Boolean.TRUE.equals(p.getAtivo()) ? "Ativo" : "Inativo");
                badge.getStyleClass().add(
                    Boolean.TRUE.equals(p.getAtivo()) ? "badge-success" : "badge-neutral");
                setGraphic(badge);
                setText(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void configurarFiltros() {
        filteredProdutos = new FilteredList<>(todosProdutos, p -> !Boolean.FALSE.equals(p.getAtivo()));
        SortedList<Produto> sortedProdutos = new SortedList<>(filteredProdutos);
        sortedProdutos.comparatorProperty().bind(tblProdutos.comparatorProperty());
        tblProdutos.setItems(sortedProdutos);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        cmbGrupoFiltro.valueProperty().addListener((obs, old, val) -> aplicarFiltro());
        chkInativos.selectedProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblProdutos.getSelectionModel().selectedItemProperty().addListener((obs, old, selecionado) -> {
            boolean temSelecao = selecionado != null;
            btnEditar.setDisable(!temSelecao);
            btnToggleAtivo.setDisable(!temSelecao);
            if (temSelecao) {
                btnToggleAtivo.setText(Boolean.TRUE.equals(selecionado.getAtivo()) ? "Inativar" : "Ativar");
            }
        });

        // Duplo clique abre edição
        tblProdutos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tblProdutos.getSelectionModel().getSelectedItem() != null) {
                abrirEdicao();
            }
        });
    }

    // ---- Carregamento de dados ----

    private void carregarGruposFiltro() {
        Integer empresaId = authService.getEmpresaIdLogado();
        List<GrupoProduto> grupos = grupoProdutoRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);

        cmbGrupoFiltro.getItems().clear();
        cmbGrupoFiltro.getItems().add(null); // "Todos os grupos"
        cmbGrupoFiltro.getItems().addAll(grupos);
        cmbGrupoFiltro.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GrupoProduto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Todos os grupos" : item.getNome());
            }
        });
    }

    void carregarProdutos() {
        Integer empresaId = authService.getEmpresaIdLogado();
        List<Produto> lista = produtoService.listarTodos(empresaId);
        todosProdutos.setAll(lista);
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotal.setText(String.valueOf(todosProdutos.size()));
        lblAtivos.setText(String.valueOf(produtoService.contarAtivos(empresaId)));
        lblAbaixoMinimo.setText(String.valueOf(produtoService.contarAbaixoMinimo(empresaId)));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        GrupoProduto grupo = cmbGrupoFiltro.getValue();
        boolean mostrarInativos = chkInativos.isSelected();

        filteredProdutos.setPredicate(p -> {
            if (!mostrarInativos && Boolean.FALSE.equals(p.getAtivo())) return false;

            if (grupo != null && (p.getGrupo() == null || !p.getGrupo().getId().equals(grupo.getId()))) {
                return false;
            }

            if (!busca.isEmpty()) {
                boolean matches =
                    (p.getDescricao() != null && p.getDescricao().toLowerCase().contains(busca)) ||
                    (p.getCodigoInterno() != null && p.getCodigoInterno().toLowerCase().contains(busca)) ||
                    (p.getCodigoBarras() != null && p.getCodigoBarras().toLowerCase().contains(busca));
                if (!matches) return false;
            }
            return true;
        });
    }

    // ---- Ações ----

    @FXML
    private void abrirNovo() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirEdicao() {
        Produto selecionado = tblProdutos.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            abrirFormulario(selecionado);
        }
    }

    @FXML
    private void toggleAtivo() {
        Produto selecionado = tblProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        String acao = Boolean.TRUE.equals(selecionado.getAtivo()) ? "inativar" : "ativar";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Deseja " + acao + " o produto '" + selecionado.getDescricao() + "'?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmação");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (Boolean.TRUE.equals(selecionado.getAtivo())) {
                    produtoService.inativar(selecionado.getId());
                } else {
                    produtoService.ativar(selecionado.getId());
                }
                carregarProdutos();
            }
        });
    }

    @FXML
    private void abrirGerenciarGrupos() {
        abrirModalAuxiliar("/fxml/grupo-produto-form.fxml", "Gerenciar Grupos de Produtos", 600, 480);
        carregarGruposFiltro();
    }

    @FXML
    private void abrirGerenciarUnidades() {
        abrirModalAuxiliar("/fxml/unidade-medida-form.fxml", "Gerenciar Unidades de Medida", 560, 440);
    }

    private void abrirModalAuxiliar(String fxmlPath, String titulo, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            stageManager.applyTheme(root);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            dialog.setTitle(titulo);
            dialog.setResizable(false);
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            stageManager.applySceneFill(scene);
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Erro ao abrir modal: {}", fxmlPath, e);
        }
    }

    private void abrirFormulario(Produto produto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/produto-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            stageManager.applyTheme(root);
            ProdutoFormController formCtrl = loader.getController();
            formCtrl.setProduto(produto);
            formCtrl.setOnSaved(this::carregarProdutos);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            dialog.setTitle(produto == null ? "Novo Produto" : "Editar Produto — " + produto.getDescricao());
            dialog.setResizable(true);
            dialog.setMinWidth(720);
            dialog.setMinHeight(560);
            Scene scene = new Scene(root, 720, 560);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            stageManager.applySceneFill(scene);
            dialog.setScene(scene);
            dialog.setWidth(720);
            dialog.setHeight(560);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Erro ao abrir formulário de produto", e);
        }
    }

    private String nvl(String value) {
        return value != null ? value : "—";
    }
}
