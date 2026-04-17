package com.erp;

import atlantafx.base.theme.PrimerLight;
import com.erp.model.dto.update.UpdateCheckResult;
import com.erp.service.AuthService;
import com.erp.service.ConfiguracaoService;
import com.erp.service.UpdateService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class StageManager {

    private final ApplicationContext springContext;
    private final ConfiguracaoService configuracaoService;
    private final AuthService authService;
    private final UpdateService updateService;

    private Stage primaryStage;
    private boolean verificacaoAtualizacaoIniciada;

    private final BooleanProperty darkMode = new SimpleBooleanProperty(true);

    public StageManager(ApplicationContext springContext,
                        ConfiguracaoService configuracaoService,
                        AuthService authService,
                        UpdateService updateService) {
        this.springContext = springContext;
        this.configuracaoService = configuracaoService;
        this.authService = authService;
        this.updateService = updateService;
    }

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;

        javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        primaryStage.setTitle("SMS - Simple Manage System");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setMaximized(true);

        try (InputStream is = getClass().getResourceAsStream("/images/icon.png")) {
            if (is != null) {
                primaryStage.getIcons().add(new Image(is));
            }
        } catch (Exception e) {
            log.warn("Icone nao encontrado: {}", e.getMessage());
        }
    }

    public void showLoginScreen() {
        showScene("/fxml/login.fxml", "SMS - Simple Manage System", false);
        iniciarVerificacaoAtualizacao();
    }

    public void showMainScreen() {
        showScene("/fxml/main.fxml", "SMS - Simple Manage System", true);
    }

    public void showScene(String fxmlPath, String title, boolean maximized) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource(fxmlPath));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/global.css")).toExternalForm()
            );

            applyTheme(root);
            applySceneFill(scene);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(maximized);
            primaryStage.show();

        } catch (IOException e) {
            log.error("Erro ao carregar tela: {}", fxmlPath, e);
            throw new RuntimeException("Nao foi possivel carregar: " + fxmlPath, e);
        }
    }

    public void carregarTemaDoUsuario(Integer empresaId) {
        try {
            darkMode.set(configuracaoService.isModoEscuro(empresaId));
            log.info("Tema carregado: {}", darkMode.get() ? "DARK" : "LIGHT");
        } catch (Exception e) {
            log.warn("Nao foi possivel carregar tema do banco, usando padrao DARK: {}", e.getMessage());
            darkMode.set(true);
        }
    }

    public void toggleDarkMode() {
        darkMode.set(!darkMode.get());

        if (primaryStage.getScene() != null) {
            applyTheme(primaryStage.getScene().getRoot());
            applySceneFill(primaryStage.getScene());
        }

        try {
            if (authService.getUsuarioLogado() != null
                    && authService.getUsuarioLogado().getEmpresa() != null) {
                Integer empresaId = authService.getEmpresaIdLogado();
                configuracaoService.salvarTema(empresaId, darkMode.get() ? "DARK" : "LIGHT");
            }
        } catch (Exception e) {
            log.warn("Nao foi possivel persistir tema: {}", e.getMessage());
        }
    }

    public boolean isDarkMode() {
        return darkMode.get();
    }

    public ReadOnlyBooleanProperty darkModeProperty() {
        return darkMode;
    }

    public void applyTheme(Parent root) {
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(darkMode.get() ? "theme-dark" : "theme-light");
    }

    public void applySceneFill(Scene scene) {
        scene.setFill(darkMode.get() ? Color.web("#000000") : Color.web("#f5f5f5"));
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private void iniciarVerificacaoAtualizacao() {
        if (verificacaoAtualizacaoIniciada) {
            return;
        }
        verificacaoAtualizacaoIniciada = true;

        CompletableFuture
                .supplyAsync(updateService::checkForUpdateIfDue)
                .thenAccept(resultado -> resultado.ifPresent(update ->
                        Platform.runLater(() -> mostrarDialogAtualizacao(update))))
                .exceptionally(e -> {
                    log.warn("Falha inesperada ao verificar atualizacao: {}", e.getMessage());
                    return null;
                });
    }

    private void mostrarDialogAtualizacao(UpdateCheckResult update) {
        ButtonType btnAtualizar = new ButtonType("Atualizar agora", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnDepois = new ButtonType("Depois", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", btnAtualizar, btnDepois);
        alert.initOwner(primaryStage);
        alert.setTitle("Atualizacao disponivel");
        alert.setHeaderText("SMS " + update.manifest().latestVersion() + " esta disponivel");

        TextArea notas = new TextArea(Optional.ofNullable(update.manifest().releaseNotes())
                .filter(s -> !s.isBlank())
                .orElse("Correcoes e melhorias disponiveis."));
        notas.setEditable(false);
        notas.setWrapText(true);
        notas.setPrefRowCount(6);

        VBox content = new VBox(10,
                new Label("Versao instalada: " + update.currentVersion()),
                new Label("Nova versao: " + update.manifest().latestVersion()),
                new Label("Novidades:"),
                notas
        );
        alert.getDialogPane().setContent(content);
        alert.setResizable(true);

        var atualizarButton = alert.getDialogPane().lookupButton(btnAtualizar);
        atualizarButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            alert.close();
            baixarEInstalarAtualizacao(update);
        });

        alert.show();
    }

    private void baixarEInstalarAtualizacao(UpdateCheckResult update) {
        Alert progresso = new Alert(Alert.AlertType.INFORMATION);
        progresso.initOwner(primaryStage);
        progresso.setTitle("Atualizando SMS");
        progresso.setHeaderText("Baixando atualizacao...");
        progresso.getDialogPane().setContent(new VBox(12,
                new ProgressIndicator(),
                new Label("Aguarde enquanto o instalador e baixado e validado.")
        ));
        progresso.getButtonTypes().setAll(ButtonType.CANCEL);
        progresso.show();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return updateService.downloadAndValidate(update.manifest());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(installer -> Platform.runLater(() -> {
                    progresso.close();
                    iniciarInstaladorEFechar(installer);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        progresso.close();
                        mostrarErroAtualizacao(e);
                    });
                    return null;
                });
    }

    private void iniciarInstaladorEFechar(Path installer) {
        try {
            updateService.scheduleInstallerAfterExit(installer);
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.initOwner(primaryStage);
            info.setTitle("Atualizacao pronta");
            info.setHeaderText("O SMS sera fechado para iniciar o instalador.");
            info.setContentText("Depois da instalacao, abra o SMS novamente pelo atalho.");
            info.showAndWait();
            Platform.exit();
        } catch (Exception e) {
            mostrarErroAtualizacao(e);
        }
    }

    private void mostrarErroAtualizacao(Throwable e) {
        Throwable causa = e.getCause() != null ? e.getCause() : e;
        log.warn("Erro ao atualizar SMS: {}", causa.getMessage(), causa);

        Alert erro = new Alert(Alert.AlertType.ERROR);
        erro.initOwner(primaryStage);
        erro.setTitle("Atualizacao nao concluida");
        erro.setHeaderText("Nao foi possivel atualizar automaticamente.");
        erro.setContentText(causa.getMessage());
        erro.show();
    }
}
