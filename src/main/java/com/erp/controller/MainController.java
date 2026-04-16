package com.erp.controller;

import com.erp.StageManager;
import com.erp.service.AuthService;
import com.erp.util.SvgImageLoader;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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
    private final SvgImageLoader svgImageLoader;
    private final ApplicationContext springContext;

    @FXML private ImageView imgLogoTopbar;
    @FXML private Button btnTema;
    @FXML private Label lblStatus;
    @FXML private Label lblDataHora;
    @FXML private StackPane conteudoPane;

    // Sidebar user area
    @FXML private Label lblAvatarSidebar;
    @FXML private Label lblNomeSidebar;
    @FXML private Label lblPerfilSidebar;

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var logo = svgImageLoader.load("/images/logo-dark.svg", 130, 34);
        if (logo != null) {
            imgLogoTopbar.setImage(logo);
            imgLogoTopbar.setStyle("-fx-cursor: hand;");
        }

        // Preenche área de usuário na sidebar
        var usuario = authService.getUsuarioLogado();
        if (usuario != null) {
            String nome = usuario.getNome() != null ? usuario.getNome() : "?";
            String[] partes = nome.trim().split("\\s+");
            String iniciais = partes.length >= 2
                ? String.valueOf(partes[0].charAt(0)) + partes[partes.length - 1].charAt(0)
                : String.valueOf(partes[0].charAt(0));
            lblAvatarSidebar.setText(iniciais.toUpperCase());
            lblNomeSidebar.setText(nome);
            lblPerfilSidebar.setText(usuario.getPerfil() != null ? usuario.getPerfil().getNome() : "");
        }

        // Relógio em tempo real
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
            lblDataHora.setText(LocalDateTime.now().format(FORMATTER))
        ));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Sincronizar ícone do botão com tema atual
        atualizarIconeTema();

        // Carrega o dashboard do perfil do usuário logado
        carregarDashboard();
    }

    @FXML
    private void toggleTema() {
        stageManager.toggleDarkMode();
        atualizarIconeTema();
    }

    private void atualizarIconeTema() {
        btnTema.setText(stageManager.isDarkMode() ? "☀" : "☾");
    }

    // ---- Handlers do menu lateral ----

    @FXML
    private void abrirClientes() {
        carregarModulo("/fxml/clientes.fxml", "Clientes");
    }
    @FXML
    private void abrirFornecedores() {
        carregarModulo("/fxml/fornecedores.fxml", "Fornecedores");
    }
    @FXML
    private void abrirFuncionarios() {
        carregarModulo("/fxml/funcionarios.fxml", "Funcionários");
    }
    @FXML private void abrirEstoque()        { carregarModulo("/fxml/estoque.fxml", "Movimentações de Estoque"); }
    @FXML private void abrirCompras()        { carregarModulo("/fxml/compras.fxml", "Compras"); }
    @FXML private void abrirOrcamentos()     { carregarModulo("/fxml/orcamentos.fxml", "Orçamentos"); }
    @FXML private void abrirVendas()         { carregarModulo("/fxml/vendas.fxml", "Vendas"); }
    @FXML private void abrirCaixa()          { carregarModulo("/fxml/caixa.fxml", "Caixa"); }
    @FXML private void abrirContasPagar()    { setStatus("Contas a Pagar"); }
    @FXML private void abrirContasReceber()  { setStatus("Contas a Receber"); }
    @FXML private void abrirRelatorios()     { setStatus("Relatórios"); }
    @FXML private void abrirConfiguracoes()  { setStatus("Configurações"); }

    @FXML
    private void abrirProdutos() {
        carregarModulo("/fxml/produtos.fxml", "Produtos");
    }

    @FXML
    private void irParaDashboard() {
        carregarDashboard();
    }

    @FXML
    private void sair() {
        authService.logout();
        stageManager.showLoginScreen();
    }

    public void carregarDashboard() {
        var usuario = authService.getUsuarioLogado();
        String perfil = (usuario != null && usuario.getPerfil() != null)
                ? usuario.getPerfil().getNome() : "ADMINISTRADOR";

        String fxmlPath = switch (perfil) {
            case "VENDAS"      -> "/fxml/dashboard-vendas.fxml";
            case "FINANCEIRO"  -> "/fxml/dashboard-financeiro.fxml";
            case "ESTOQUE"     -> "/fxml/dashboard-estoque.fxml";
            default            -> "/fxml/dashboard-admin.fxml"; // ADMINISTRADOR, GERENTE e demais
        };

        carregarModulo(fxmlPath, "Dashboard");
    }

    private void carregarModulo(String fxmlPath, String nomeModulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent view = loader.load();
            conteudoPane.getChildren().setAll(view);
            lblStatus.setText(nomeModulo);
            log.info("Módulo carregado: {}", nomeModulo);
        } catch (Exception e) {
            log.error("Erro ao carregar módulo '{}': {}", nomeModulo, e.getMessage(), e);
            lblStatus.setText("Erro ao carregar: " + nomeModulo);
        }
    }

    private void setStatus(String modulo) {
        lblStatus.setText("Módulo: " + modulo + " — em breve");
        log.info("Módulo ainda não implementado: {}", modulo);
    }
}
