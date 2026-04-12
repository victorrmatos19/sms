package com.erp.controller;

import com.erp.StageManager;
import com.erp.model.Funcionario;
import com.erp.service.AuthService;
import com.erp.service.FuncionarioService;
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
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class FuncionarioController implements Initializable {

    private final FuncionarioService funcionarioService;
    private final AuthService authService;
    private final StageManager stageManager;
    private final ApplicationContext springContext;

    // Métricas
    @FXML private Label lblTotal;
    @FXML private Label lblAtivos;
    @FXML private Label lblComAcesso;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkInativos;

    // Ações
    @FXML private Button btnEditar;
    @FXML private Button btnToggleAtivo;

    // Tabela
    @FXML private TableView<Funcionario> tblFuncionarios;
    @FXML private TableColumn<Funcionario, String> colNome;
    @FXML private TableColumn<Funcionario, String> colCpf;
    @FXML private TableColumn<Funcionario, String> colCargo;
    @FXML private TableColumn<Funcionario, String> colTelefone;
    @FXML private TableColumn<Funcionario, String> colEmail;
    @FXML private TableColumn<Funcionario, String> colAdmissao;
    @FXML private TableColumn<Funcionario, String> colAcesso;
    @FXML private TableColumn<Funcionario, String> colStatus;

    private final ObservableList<Funcionario> todosFuncionarios = FXCollections.observableArrayList();
    private FilteredList<Funcionario> filteredFuncionarios;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarFuncionarios();
    }

    private void configurarColunas() {
        colNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));

        colCpf.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCpf())));

        colCargo.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCargo())));

        colTelefone.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getTelefone())));

        colEmail.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEmail())));

        colAdmissao.setCellValueFactory(c -> {
            var data = c.getValue().getDataAdmissao();
            return new SimpleStringProperty(data != null ? data.format(DATE_FMT) : "");
        });

        // Badge Acesso ao Sistema
        colAcesso.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Funcionario f = getTableRow().getItem();
                boolean temAcesso = f.getUsuario() != null;
                Label badge = new Label(temAcesso ? "Sim" : "Não");
                badge.getStyleClass().add(temAcesso ? "badge-success" : "badge-neutral");
                setGraphic(badge);
                setText(null);
            }
        });
        colAcesso.setCellValueFactory(c -> new SimpleStringProperty(""));

        // Badge Status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Funcionario f = getTableRow().getItem();
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
        filteredFuncionarios = new FilteredList<>(todosFuncionarios,
            f -> Boolean.TRUE.equals(f.getAtivo()));
        SortedList<Funcionario> sorted = new SortedList<>(filteredFuncionarios);
        sorted.comparatorProperty().bind(tblFuncionarios.comparatorProperty());
        tblFuncionarios.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        chkInativos.selectedProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblFuncionarios.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                boolean tem = sel != null;
                btnEditar.setDisable(!tem);
                btnToggleAtivo.setDisable(!tem);
                if (tem) btnToggleAtivo.setText(
                    Boolean.TRUE.equals(sel.getAtivo()) ? "Inativar" : "Ativar");
            });

        tblFuncionarios.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2
                    && tblFuncionarios.getSelectionModel().getSelectedItem() != null) {
                abrirEdicao();
            }
        });
    }

    void carregarFuncionarios() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosFuncionarios.setAll(funcionarioService.listarTodos(empresaId));
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotal.setText(String.valueOf(todosFuncionarios.size()));
        lblAtivos.setText(String.valueOf(funcionarioService.contarAtivos(empresaId)));
        lblComAcesso.setText(String.valueOf(funcionarioService.contarComAcesso(empresaId)));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        boolean mostrarInativos = chkInativos.isSelected();

        filteredFuncionarios.setPredicate(f -> {
            if (!mostrarInativos && Boolean.FALSE.equals(f.getAtivo())) return false;
            if (!busca.isEmpty()) {
                boolean match =
                    (f.getNome() != null && normalizar(f.getNome()).contains(normalizar(busca))) ||
                    (f.getCpf() != null && f.getCpf().contains(busca)) ||
                    (f.getCargo() != null && normalizar(f.getCargo()).contains(normalizar(busca)));
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

    @FXML private void abrirNovo() { abrirFormulario(null); }

    @FXML
    private void abrirEdicao() {
        Funcionario sel = tblFuncionarios.getSelectionModel().getSelectedItem();
        if (sel != null) abrirFormulario(sel);
    }

    @FXML
    private void toggleAtivo() {
        Funcionario sel = tblFuncionarios.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String acao = Boolean.TRUE.equals(sel.getAtivo()) ? "inativar" : "ativar";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Deseja " + acao + " o funcionário '" + sel.getNome() + "'?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmação");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (Boolean.TRUE.equals(sel.getAtivo())) funcionarioService.inativar(sel.getId());
                else funcionarioService.ativar(sel.getId());
                carregarFuncionarios();
            }
        });
    }

    private void abrirFormulario(Funcionario funcionario) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/funcionario-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            FuncionarioFormController formCtrl = loader.getController();
            formCtrl.setFuncionario(funcionario);
            formCtrl.setOnSaved(this::carregarFuncionarios);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            dialog.setTitle(funcionario == null
                ? "Novo Funcionário"
                : "Editar Funcionário — " + funcionario.getNome());
            dialog.setResizable(true);
            dialog.setMinWidth(620);
            dialog.setMinHeight(520);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Erro ao abrir formulário de funcionário", e);
        }
    }

    private String nvl(String v) { return v != null ? v : "—"; }
}
