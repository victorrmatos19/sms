package com.erp.controller;

import com.erp.StageManager;
import com.erp.model.Usuario;
import com.erp.service.AuthService;
import com.erp.util.SvgImageLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller da tela de login.
 * É um bean Spring (@Component) — o StageManager injeta via controllerFactory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginController implements Initializable {

    private final AuthService authService;
    private final StageManager stageManager;
    private final SvgImageLoader svgImageLoader;

    @FXML private ImageView imgLogo;
    @FXML private TextField txtLogin;
    @FXML private PasswordField txtSenha;
    @FXML private Button btnEntrar;
    @FXML private Button btnVerificarAtualizacoes;
    @FXML private Label lblErro;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblErro.setVisible(false);

        var logo = svgImageLoader.load("/images/logo-dark.svg", 180, 48);
        if (logo != null) {
            imgLogo.setImage(logo);
        }

        // Enter no campo de senha dispara o login
        txtSenha.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                fazerLogin();
            }
        });

        // Enter no campo de login move para senha
        txtLogin.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtSenha.requestFocus();
            }
        });

        // Foco inicial no campo de login
        Platform.runLater(() -> txtLogin.requestFocus());
    }

    @FXML
    private void fazerLogin() {
        String login = txtLogin.getText().trim();
        String senha = txtSenha.getText();

        if (login.isEmpty() || senha.isEmpty()) {
            mostrarErro("Informe o login e a senha.");
            return;
        }

        btnEntrar.setDisable(true);
        lblErro.setVisible(false);

        Optional<Usuario> usuarioOpt = authService.autenticar(login, senha);

        if (usuarioOpt.isPresent()) {
            log.info("Login bem-sucedido: {}", login);
            stageManager.carregarTemaDoUsuario(authService.getEmpresaIdLogado());
            stageManager.showMainScreen();
        } else {
            mostrarErro("Login ou senha incorretos.");
            txtSenha.clear();
            txtLogin.requestFocus();
            btnEntrar.setDisable(false);
        }
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
    }

    @FXML
    private void verificarAtualizacoes() {
        btnVerificarAtualizacoes.setDisable(true);
        btnVerificarAtualizacoes.setText("Verificando...");

        stageManager.verificarAtualizacaoManual()
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    btnVerificarAtualizacoes.setText("Verificar atualizações");
                    btnVerificarAtualizacoes.setDisable(false);
                }));
    }
}
