package com.erp.controller;

import com.erp.StageManager;
import com.erp.model.Cliente;
import com.erp.service.AuthService;
import com.erp.service.ClienteService;
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
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller da listagem de clientes.
 * Filtros aplicados em memória via FilteredList.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteController implements Initializable {

    private final ClienteService clienteService;
    private final AuthService authService;
    private final StageManager stageManager;
    private final ApplicationContext springContext;

    // Métricas
    @FXML private Label lblTotal;
    @FXML private Label lblAtivos;
    @FXML private Label lblComLimite;

    // Filtros
    @FXML private TextField txtBusca;
    @FXML private CheckBox chkInativos;

    // Ações
    @FXML private Button btnEditar;
    @FXML private Button btnToggleAtivo;

    // Tabela
    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, String> colNome;
    @FXML private TableColumn<Cliente, String> colCpfCnpj;
    @FXML private TableColumn<Cliente, String> colTelefone;
    @FXML private TableColumn<Cliente, String> colCidadeUf;
    @FXML private TableColumn<Cliente, String> colLimite;
    @FXML private TableColumn<Cliente, String> colStatus;

    private final ObservableList<Cliente> todosClientes = FXCollections.observableArrayList();
    private FilteredList<Cliente> filteredClientes;

    private static final NumberFormat CURRENCY_FORMAT =
        NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregarClientes();
    }

    // ---- Configuração da tabela ----

    private void configurarColunas() {
        colNome.setCellValueFactory(c -> {
            Cliente cli = c.getValue();
            String nome = "PJ".equals(cli.getTipoPessoa()) && cli.getRazaoSocial() != null
                ? cli.getRazaoSocial()
                : cli.getNome();
            return new SimpleStringProperty(nome);
        });

        colCpfCnpj.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getCpfCnpj())));

        colTelefone.setCellValueFactory(c -> {
            Cliente cli = c.getValue();
            String tel = cli.getCelular() != null ? cli.getCelular() : cli.getTelefone();
            return new SimpleStringProperty(nvl(tel));
        });

        colCidadeUf.setCellValueFactory(c -> {
            Cliente cli = c.getValue();
            if (cli.getCidade() != null && cli.getUf() != null) {
                return new SimpleStringProperty(cli.getCidade() + "/" + cli.getUf());
            }
            return new SimpleStringProperty(nvl(cli.getCidade()));
        });

        colLimite.setCellValueFactory(c ->
            new SimpleStringProperty(
                c.getValue().getLimiteCredito() != null
                    ? CURRENCY_FORMAT.format(c.getValue().getLimiteCredito())
                    : "—"));

        // Badge de status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Cliente cli = getTableRow().getItem();
                Label badge = new Label(Boolean.TRUE.equals(cli.getAtivo()) ? "Ativo" : "Inativo");
                badge.getStyleClass().add(
                    Boolean.TRUE.equals(cli.getAtivo()) ? "badge-success" : "badge-neutral");
                setGraphic(badge);
                setText(null);
            }
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void configurarFiltros() {
        filteredClientes = new FilteredList<>(todosClientes,
            c -> Boolean.TRUE.equals(c.getAtivo()));
        SortedList<Cliente> sorted = new SortedList<>(filteredClientes);
        sorted.comparatorProperty().bind(tblClientes.comparatorProperty());
        tblClientes.setItems(sorted);

        txtBusca.textProperty().addListener((obs, old, val) -> aplicarFiltro());
        chkInativos.selectedProperty().addListener((obs, old, val) -> aplicarFiltro());
    }

    private void configurarSelecao() {
        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean temSelecao = sel != null;
            btnEditar.setDisable(!temSelecao);
            btnToggleAtivo.setDisable(!temSelecao);
            if (temSelecao) {
                btnToggleAtivo.setText(Boolean.TRUE.equals(sel.getAtivo()) ? "Inativar" : "Ativar");
            }
        });

        tblClientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && tblClientes.getSelectionModel().getSelectedItem() != null) {
                abrirEdicao();
            }
        });
    }

    // ---- Carregamento de dados ----

    void carregarClientes() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosClientes.setAll(clienteService.listarTodos(empresaId));
        aplicarFiltro();
        atualizarMetricas();
    }

    private void atualizarMetricas() {
        Integer empresaId = authService.getEmpresaIdLogado();
        lblTotal.setText(String.valueOf(todosClientes.size()));
        lblAtivos.setText(String.valueOf(clienteService.contarAtivos(empresaId)));
        lblComLimite.setText(String.valueOf(clienteService.contarComLimite(empresaId)));
    }

    private void aplicarFiltro() {
        String busca = txtBusca.getText() == null ? "" : txtBusca.getText().toLowerCase().trim();
        boolean mostrarInativos = chkInativos.isSelected();

        filteredClientes.setPredicate(c -> {
            if (!mostrarInativos && Boolean.FALSE.equals(c.getAtivo())) return false;

            if (!busca.isEmpty()) {
                boolean match =
                    (c.getNome() != null && normalizar(c.getNome()).contains(normalizar(busca))) ||
                    (c.getRazaoSocial() != null && normalizar(c.getRazaoSocial()).contains(normalizar(busca))) ||
                    (c.getCpfCnpj() != null && c.getCpfCnpj().contains(busca)) ||
                    (c.getTelefone() != null && c.getTelefone().contains(busca)) ||
                    (c.getCelular() != null && c.getCelular().contains(busca));
                if (!match) return false;
            }
            return true;
        });
    }

    /** Normaliza texto removendo acentos para busca case+accent-insensitive. */
    private String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase();
    }

    // ---- Ações ----

    @FXML
    private void abrirNovo() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirEdicao() {
        Cliente selecionado = tblClientes.getSelectionModel().getSelectedItem();
        if (selecionado != null) abrirFormulario(selecionado);
    }

    @FXML
    private void toggleAtivo() {
        Cliente selecionado = tblClientes.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        String acao = Boolean.TRUE.equals(selecionado.getAtivo()) ? "inativar" : "ativar";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Deseja " + acao + " o cliente '" + selecionado.getNome() + "'?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmação");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (Boolean.TRUE.equals(selecionado.getAtivo())) {
                    clienteService.inativar(selecionado.getId());
                } else {
                    clienteService.ativar(selecionado.getId());
                }
                carregarClientes();
            }
        });
    }

    private void abrirFormulario(Cliente cliente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cliente-form.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            ClienteFormController formCtrl = loader.getController();
            formCtrl.setCliente(cliente);
            formCtrl.setOnSaved(this::carregarClientes);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            dialog.setTitle(cliente == null
                ? "Novo Cliente"
                : "Editar Cliente — " + cliente.getNome());
            dialog.setResizable(true);
            dialog.setMinWidth(680);
            dialog.setMinHeight(580);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            log.error("Erro ao abrir formulário de cliente", e);
        }
    }

    private String nvl(String value) {
        return value != null ? value : "—";
    }
}
