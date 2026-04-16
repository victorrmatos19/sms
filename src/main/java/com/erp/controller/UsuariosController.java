package com.erp.controller;

import com.erp.model.PerfilAcesso;
import com.erp.model.Usuario;
import com.erp.service.AuthService;
import com.erp.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuariosController implements Initializable {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    @FXML private TableView<Usuario> tabelaUsuarios;
    @FXML private TableColumn<Usuario, String> colNome;
    @FXML private TableColumn<Usuario, String> colLogin;
    @FXML private TableColumn<Usuario, String> colPerfil;
    @FXML private TableColumn<Usuario, String> colEmail;
    @FXML private TableColumn<Usuario, String> colStatus;
    @FXML private TextField txtBusca;
    @FXML private Label lblContador;
    @FXML private Button btnNovo;
    @FXML private Button btnEditar;
    @FXML private Button btnAlterarSenha;
    @FXML private Button btnAtivarDesativar;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarColunas();
        configurarBotoes();
        configurarBusca();
        configurarDuploClique();
        carregarUsuarios(null);
    }

    private void configurarColunas() {
        colNome.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNome())));
        colLogin.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getLogin())));
        colPerfil.setCellValueFactory(c -> new SimpleStringProperty(formatarPerfis(c.getValue())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEmail())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getAtivo()) ? "ATIVO" : "INATIVO"));
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
                badge.getStyleClass().add("ATIVO".equals(item) ? "badge-success" : "badge-neutral");
                setGraphic(badge);
                setText(null);
            }
        });
    }

    private void configurarBotoes() {
        btnEditar.disableProperty().bind(tabelaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        btnAlterarSenha.disableProperty().bind(tabelaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        btnAtivarDesativar.disableProperty().bind(tabelaUsuarios.getSelectionModel().selectedItemProperty().isNull());
        tabelaUsuarios.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> atualizarTextoBotaoAtivo());
    }

    private void configurarBusca() {
        txtBusca.textProperty().addListener((obs, old, value) -> carregarUsuarios(value));
    }

    private void configurarDuploClique() {
        tabelaUsuarios.setOnMouseClicked(event -> {
            Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && selecionado != null) {
                abrirFormulario(selecionado);
            }
        });
    }

    private void carregarUsuarios(String termo) {
        Integer empresaId = authService.getEmpresaIdLogado();
        List<Usuario> lista = termo == null || termo.isBlank()
                ? usuarioService.listarPorEmpresa(empresaId)
                : usuarioService.buscar(empresaId, termo);
        tabelaUsuarios.setItems(FXCollections.observableArrayList(lista));
        lblContador.setText(lista.size() + " usuário(s) cadastrado(s)");
        atualizarTextoBotaoAtivo();
    }

    private void atualizarTextoBotaoAtivo() {
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            btnAtivarDesativar.setText("Ativar/Desativar");
        } else if (Boolean.TRUE.equals(selecionado.getAtivo())) {
            btnAtivarDesativar.setText("Desativar");
        } else {
            btnAtivarDesativar.setText("Ativar");
        }
    }

    @FXML
    private void abrirNovo() {
        abrirFormulario(null);
    }

    @FXML
    private void abrirEditar() {
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            abrirFormulario(selecionado);
        }
    }

    private void abrirFormulario(Usuario usuario) {
        boolean novo = usuario == null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(novo ? "Novo Usuário" : "Editar Usuário");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtNome = new TextField(novo ? "" : nvl(usuario.getNome()));
        TextField txtLogin = new TextField(novo ? "" : nvl(usuario.getLogin()));
        Label lblLogin = new Label(novo ? "" : nvl(usuario.getLogin()));
        TextField txtEmail = new TextField(novo ? "" : nvl(usuario.getEmail()));
        Map<PerfilAcesso, CheckBox> checksPerfil = criarChecksPerfil(usuario);
        VBox boxPerfis = new VBox(4);
        boxPerfis.getChildren().addAll(checksPerfil.values());
        CheckBox chkAtivo = new CheckBox("Ativo");
        chkAtivo.setSelected(novo || Boolean.TRUE.equals(usuario.getAtivo()));
        PasswordField pfSenha = new PasswordField();
        PasswordField pfConfirmar = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        int row = 0;
        grid.add(new Label("Nome *"), 0, row);
        grid.add(txtNome, 1, row++);
        grid.add(new Label("Login *"), 0, row);
        grid.add(novo ? txtLogin : lblLogin, 1, row++);
        Label hint = new Label("Letras, números e underscore. Mínimo 3 caracteres.");
        hint.getStyleClass().add("form-hint");
        grid.add(hint, 1, row++);
        grid.add(new Label("Email"), 0, row);
        grid.add(txtEmail, 1, row++);
        grid.add(new Label("Perfis *"), 0, row);
        grid.add(boxPerfis, 1, row++);
        grid.add(new Label("Status"), 0, row);
        grid.add(chkAtivo, 1, row++);
        if (novo) {
            grid.add(new Label("Senha *"), 0, row);
            grid.add(pfSenha, 1, row++);
            grid.add(new Label("Confirmar Senha *"), 0, row);
            grid.add(pfConfirmar, 1, row);
        }

        dialog.getDialogPane().setContent(grid);
        aplicarEstilo(dialog);

        AtomicBoolean salvou = new AtomicBoolean(false);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            Set<Integer> perfilIds = perfisSelecionados(checksPerfil);
            if (!validarFormularioUsuario(novo, txtNome, txtLogin, checksPerfil, pfSenha, pfConfirmar)) {
                event.consume();
                return;
            }

            try {
                if (novo) {
                    usuarioService.criar(authService.getEmpresaIdLogado(),
                            txtNome.getText(), txtLogin.getText(), txtEmail.getText(),
                            perfilIds, pfSenha.getText());
                } else {
                    usuarioService.editar(usuario.getId(), txtNome.getText(), txtEmail.getText(),
                            perfilIds, chkAtivo.isSelected());
                }
                salvou.set(true);
            } catch (IllegalArgumentException | IllegalStateException e) {
                mostrarErro(e.getMessage());
                event.consume();
            } catch (Exception e) {
                log.error("Erro ao salvar usuário", e);
                mostrarErro("Erro ao salvar usuário: " + e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
        if (salvou.get()) {
            carregarUsuarios(null);
        }
    }

    @FXML
    private void alterarSenha() {
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Alterar Senha — " + selecionado.getNome());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField pfNova = new PasswordField();
        PasswordField pfConfirmar = new PasswordField();
        VBox content = new VBox(8,
                new Label("Nova senha"),
                pfNova,
                new Label("Confirmar senha"),
                pfConfirmar);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        aplicarEstilo(dialog);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String senha = pfNova.getText();
            if (senha == null || senha.length() < 6) {
                mostrarErro("Senha inválida: mínimo 6 caracteres");
                event.consume();
                return;
            }
            if (!senha.equals(pfConfirmar.getText())) {
                mostrarErro("A confirmação de senha não confere.");
                event.consume();
                return;
            }
            try {
                usuarioService.alterarSenha(selecionado.getId(), senha);
                mostrarInfo("Senha alterada com sucesso.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                mostrarErro(e.getMessage());
                event.consume();
            } catch (Exception e) {
                log.error("Erro ao alterar senha", e);
                mostrarErro("Erro ao alterar senha: " + e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    @FXML
    private void toggleAtivo() {
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            return;
        }

        String acao = Boolean.TRUE.equals(selecionado.getAtivo()) ? "desativar" : "ativar";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmação");
        confirm.setHeaderText(null);
        confirm.setContentText("Deseja " + acao + " o usuário " + selecionado.getNome() + "?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    usuarioService.editar(selecionado.getId(), selecionado.getNome(),
                            selecionado.getEmail(), perfilIdsDoUsuario(selecionado),
                            !Boolean.TRUE.equals(selecionado.getAtivo()));
                    carregarUsuarios(null);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    mostrarErro(e.getMessage());
                } catch (Exception e) {
                    log.error("Erro ao alternar status do usuário", e);
                    mostrarErro("Erro ao alterar status: " + e.getMessage());
                }
            }
        });
    }

    private boolean validarFormularioUsuario(boolean novo, TextField txtNome, TextField txtLogin,
                                             Map<PerfilAcesso, CheckBox> checksPerfil,
                                             PasswordField pfSenha, PasswordField pfConfirmar) {
        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            mostrarErro("Nome é obrigatório.");
            txtNome.requestFocus();
            return false;
        }
        if (novo && (txtLogin.getText() == null || txtLogin.getText().trim().isEmpty())) {
            mostrarErro("Login é obrigatório.");
            txtLogin.requestFocus();
            return false;
        }
        if (perfisSelecionados(checksPerfil).isEmpty()) {
            mostrarErro("Selecione pelo menos um perfil.");
            checksPerfil.values().stream().findFirst().ifPresent(CheckBox::requestFocus);
            return false;
        }
        if (novo) {
            if (pfSenha.getText() == null || pfSenha.getText().length() < 6) {
                mostrarErro("Senha inválida: mínimo 6 caracteres");
                pfSenha.requestFocus();
                return false;
            }
            if (!pfSenha.getText().equals(pfConfirmar.getText())) {
                mostrarErro("A confirmação de senha não confere.");
                pfConfirmar.requestFocus();
                return false;
            }
        }
        return true;
    }

    private Map<PerfilAcesso, CheckBox> criarChecksPerfil(Usuario usuario) {
        Map<PerfilAcesso, CheckBox> checks = new LinkedHashMap<>();
        boolean usuarioAdmin = authService.temPerfil("ADMINISTRADOR");
        for (PerfilAcesso perfil : ordenarPerfis(usuarioService.listarPerfis())) {
            CheckBox check = new CheckBox(nomePerfil(perfil.getNome()));
            check.setSelected(usuario != null && usuario.temPerfil(perfil.getNome()));
            if ("ADMINISTRADOR".equals(perfil.getNome()) && !usuarioAdmin) {
                check.setDisable(true);
            }
            checks.put(perfil, check);
        }
        return checks;
    }

    private Set<Integer> perfisSelecionados(Map<PerfilAcesso, CheckBox> checksPerfil) {
        return checksPerfil.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(entry -> entry.getKey().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> perfilIdsDoUsuario(Usuario usuario) {
        return usuario.getPerfisOrdenados().stream()
                .map(PerfilAcesso::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String formatarPerfis(Usuario usuario) {
        if (usuario == null || usuario.getPerfisOrdenados().isEmpty()) {
            return "—";
        }
        return usuario.getPerfisOrdenados().stream()
                .map(PerfilAcesso::getNome)
                .map(this::nomePerfil)
                .collect(Collectors.joining(", "));
    }

    private List<PerfilAcesso> ordenarPerfis(List<PerfilAcesso> perfis) {
        return perfis.stream()
                .sorted((a, b) -> Integer.compare(ordemPerfil(a.getNome()), ordemPerfil(b.getNome())))
                .toList();
    }

    private int ordemPerfil(String nomePerfil) {
        return switch (nvl(nomePerfil)) {
            case "ADMINISTRADOR" -> 0;
            case "GERENTE" -> 1;
            case "VENDAS" -> 2;
            case "FINANCEIRO" -> 3;
            case "ESTOQUE" -> 4;
            default -> 99;
        };
    }

    private String nomePerfil(String nome) {
        return switch (nvl(nome)) {
            case "ADMINISTRADOR" -> "Administrador";
            case "GERENTE" -> "Gerente";
            case "VENDAS" -> "Vendas";
            case "FINANCEIRO" -> "Financeiro";
            case "ESTOQUE" -> "Estoque";
            default -> nvl(nome);
        };
    }

    private void aplicarEstilo(Dialog<?> dialog) {
        if (tabelaUsuarios.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(tabelaUsuarios.getScene().getStylesheets());
        }
    }

    private void mostrarInfo(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensagem, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Informação");
        alert.showAndWait();
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Erro");
        alert.showAndWait();
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
