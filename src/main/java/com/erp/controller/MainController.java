package com.erp.controller;

import com.erp.StageManager;
import com.erp.service.AuthService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller da tela principal.
 * Gerencia o menu lateral e carrega os módulos na área de conteúdo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MainController implements Initializable {

    private final AuthService authService;
    private final StageManager stageManager;

    @FXML private Label lblUsuario;
    @FXML private Label lblStatus;
    @FXML private Label lblDataHora;
    @FXML private StackPane conteudoPane;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Exibe nome do usuário logado
        if (authService.getUsuarioLogado() != null) {
            lblUsuario.setText(authService.getUsuarioLogado().getNome()
                + " — " + authService.getUsuarioLogado().getPerfil().getNome());
        }

        // Relógio em tempo real
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
            lblDataHora.setText(LocalDateTime.now().format(FORMATTER))
        ));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // ---- Handlers do menu lateral ----

    @FXML private void abrirClientes()       { setStatus("Clientes"); }
    @FXML private void abrirFornecedores()   { setStatus("Fornecedores"); }
    @FXML private void abrirProdutos()       { setStatus("Produtos"); }
    @FXML private void abrirFuncionarios()   { setStatus("Funcionários"); }
    @FXML private void abrirEstoque()        { setStatus("Movimentações de Estoque"); }
    @FXML private void abrirCompras()        { setStatus("Compras"); }
    @FXML private void abrirOrcamentos()     { setStatus("Orçamentos"); }
    @FXML private void abrirVendas()         { setStatus("Vendas"); }
    @FXML private void abrirCaixa()          { setStatus("Caixa"); }
    @FXML private void abrirContasPagar()    { setStatus("Contas a Pagar"); }
    @FXML private void abrirContasReceber()  { setStatus("Contas a Receber"); }
    @FXML private void abrirRelatorios()     { setStatus("Relatórios"); }
    @FXML private void abrirConfiguracoes()  { setStatus("Configurações"); }

    @FXML
    private void sair() {
        authService.logout();
        stageManager.showLoginScreen();
    }

    private void setStatus(String modulo) {
        lblStatus.setText("Módulo: " + modulo);
        log.info("Abrindo módulo: {}", modulo);
        // TODO: carregar o FXML do módulo no conteudoPane
    }
}
