package com.erp;

import atlantafx.base.theme.PrimerLight;
import com.erp.service.AuthService;
import com.erp.service.ConfiguracaoService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Slf4j
@Component
public class StageManager {

    private final ApplicationContext springContext;
    private final ConfiguracaoService configuracaoService;
    private final AuthService authService;

    private Stage primaryStage;

    // substitui o boolean simples
    private final BooleanProperty darkMode = new SimpleBooleanProperty(true);

    public StageManager(ApplicationContext springContext,
                        ConfiguracaoService configuracaoService,
                        AuthService authService) {
        this.springContext = springContext;
        this.configuracaoService = configuracaoService;
        this.authService = authService;
    }

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;

        javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        primaryStage.setTitle("SMS — Simple Manage System");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setMaximized(true);

        try (InputStream is = getClass().getResourceAsStream("/images/icon.png")) {
            if (is != null) {
                primaryStage.getIcons().add(new Image(is));
            }
        } catch (Exception e) {
            log.warn("Ícone não encontrado: {}", e.getMessage());
        }
    }

    public void showLoginScreen() {
        showScene("/fxml/login.fxml", "SMS — Simple Manage System", false);
    }

    public void showMainScreen() {
        showScene("/fxml/main.fxml", "SMS — Simple Manage System", true);
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
            throw new RuntimeException("Não foi possível carregar: " + fxmlPath, e);
        }
    }

    public void carregarTemaDoUsuario(Integer empresaId) {
        try {
            darkMode.set(configuracaoService.isModoEscuro(empresaId));
            log.info("Tema carregado: {}", darkMode.get() ? "DARK" : "LIGHT");
        } catch (Exception e) {
            log.warn("Não foi possível carregar tema do banco, usando padrão DARK: {}", e.getMessage());
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
            log.warn("Não foi possível persistir tema: {}", e.getMessage());
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
}