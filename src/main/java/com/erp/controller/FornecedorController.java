package com.erp.controller;

import com.erp.StageManager;
import com.erp.model.Fornecedor;
import com.erp.service.AuthService;
import com.erp.service.FornecedorService;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class FornecedorController implements Initializable {

    private final FornecedorService fornecedorService;
    private final AuthService authService;
    private final StageManager stageManager;
    private final ApplicationContext springContext;

    // Métricas
    @FXML private Label lblTotal;
    @FXML private Label lblAtivos;
    @FXML private Label lblComPix;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkInativos;

    // Ações
    @FXML private Button btnEditar;
    @FXML private Button btnToggleAtivo;

    // Tabela
    @FXML private TableView<Fornecedor> tblFornecedores;
    @FXML private TableColumn<Fornecedor, String> colNome;
    @FXML private TableColumn<Fornecedor, String> colCpfCnpj;
    @FXML private TableColumn<Fornecedor, String> colTelefone;
    @FXML private TableColumn<Fornecedor, String> colCidadeUf;
    @FXML private TableColumn<Fornecedor, String> colContato;
    @FXML private TableColumn<Fornecedor, String> colStatus;

    private final ObservableList<Fornecedor> todosFornecedores = FXCollections.observableArrayList();
    private FilteredList<Fornecedor> filteredFornecedores;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarFornecedores();
    }

    private void configurarColunas() {
        colNome.setCellValueFactory(c -> {
            Fornecedor f = c.getValue();
            String nome = "PJ".equals(f.getTipoPessoa()) && f.getRazaoSocial() != null
                ? f.getRazaoSocial() : f.getNome();
            return new SimpleStringProperty(nome);
        });

        colCpfCnpj.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getCpfCnpj())));

        colTelefone.setCellValueFactory(c -> {
            Fornecedor f = c.getValue();
            String tel = f.getCelular() != null ? f.getCelular() : f.getTelefone();
            return new SimpleStringProperty(nvl(tel));
        });

        colCidadeUf.setCellValueFactory(c -> {
            Fornecedor f = c.getValue();
            if (f.getCidade() != null && f.getUf() != null)
                return new SimpleStringProperty(f.getCidade() + "/" + f.getUf());
            return new SimpleStringProperty(nvl(f.getCidade()));
        });

        colContato.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getContatoNome())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Fornecedor f = getTableRow().getItem();
                Label badge = new Label(Boolean.TRUE.equals(f.getAtivo()) ? "Ativo" : "Inativo");
                badge.getStyleClass().add(
                    Boolean.TRUE.equals(f.getAtivo()) ? "badge-success" : "badge-neutral");
                setGraphic(badge);
                setText(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void configurarFiltros() {
        filteredFornecedores = new FilteredList<>(todosFornecedores,
            f -> Boolean.TRUE.equals(f.getAtivo()));
        SortedList<Fornecedor> sorted = new SortedList<>(filteredFornecedores);
        sorted.comparatorProperty().bind(tblFornecedores.comparatorProperty());
        tblFornecedores.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        chkInativos.selectedProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblFornecedores.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                boolean tem = sel != null;
                btnEditar.setDisable(!tem);
                btnToggleAtivo.setDisable(!tem);
                if (tem) btnToggleAtivo.setText(
                    Boolean.TRUE.equals(sel.getAtivo()) ? "Inativar" : "Ativar");
            });

        tblFornecedores.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2
                    && tblFornecedores.getSelectionModel().getSelectedItem() != null) {
                abrirEdicao();
            }
        });
    }

    void carregarFornecedores() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosFornecedores.setAll(fornecedorService.listarTodos(empresaId));
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotal.setText(String.valueOf(todosFornecedores.size()));
        lblAtivos.setText(String.valueOf(fornecedorService.contarAtivos(empresaId)));
        lblComPix.setText(String.valueOf(fornecedorService.contarComPix(empresaId)));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        boolean mostrarInativos = chkInativos.isSelected();

        filteredFornecedores.setPredicate(f -> {
            if (!mostrarInativos && Boolean.FALSE.equals(f.getAtivo())) return false;
            if (!busca.isEmpty()) {
                boolean match =
                    (f.getNome() != null && normalizar(f.getNome()).contains(normalizar(busca))) ||
                    (f.getRazaoSocial() != null && normalizar(f.getRazaoSocial()).contains(normalizar(busca))) ||
                    (f.getCpfCnpj() != null && f.getCpfCnpj().contains(busca)) ||
                    (f.getTelefone() != null && f.getTelefone().contains(busca)) ||
                    (f.getCelular() != null && f.getCelular().contains(busca));
                if (!match) return false;
            }
            return true;
        });
    }

    private String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase();
    }

    @FXML private void abrirNovo()  { abrirFormulario(null); }

    @FXML
    private void abrirEdicao() {
        Fornecedor sel = tblFornecedores.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    @FXML
    private void toggleAtivo() {
        Fornecedor sel = tblFornecedores.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String acao = Boolean.TRUE.equals(sel.getAtivo()) ? "inativar" : "ativar";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Deseja " + acao + " o fornecedor '" + sel.getNome() + "'?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmação");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (Boolean.TRUE.equals(sel.getAtivo())) fornecedorService.inativar(sel.getId());
                else fornecedorService.ativar(sel.getId());
                carregarFornecedores();
            }
        });
    }

    private void abrirFormulario(Fornecedor fornecedor) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/fornecedor-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            FornecedorFormController formCtrl = loader.getController();
            formCtrl.setFornecedor(fornecedor);
            formCtrl.setOnSaved(this::carregarFornecedores);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            dialog.setTitle(fornecedor == null
                ? "Novo Fornecedor"
                : "Editar Fornecedor — " + fornecedor.getNome());
            dialog.setResizable(true);
            dialog.setMinWidth(720);
            dialog.setMinHeight(600);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Erro ao abrir formulário de fornecedor", e);
        }
    }

    private String nvl(String v) { return v != null ? v : "—"; }
}
