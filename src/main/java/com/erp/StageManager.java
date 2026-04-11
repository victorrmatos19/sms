package com.erp;

import atlantafx.base.theme.PrimerLight;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class StageManager {

    private final ApplicationContext springContext;
    private Stage primaryStage;
    private boolean darkMode = false;

    public StageManager(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Base AtlantaFX — usamos apenas como reset, o design system é nosso CSS
        javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        primaryStage.setTitle("ERP Desktop");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setMaximized(true);
    }

    public void showLoginScreen() {
        showScene("/fxml/login.fxml", "ERP Desktop — Login", false);
    }

    public void showMainScreen() {
        showScene("/fxml/main.fxml", "ERP Desktop", true);
    }

    public void showScene(String fxmlPath, String title, boolean maximized) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource(fxmlPath));
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root);

            // CSS global do design system
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/css/global.css")
                    ).toExternalForm()
            );

            // Aplica tema na raiz
            applyTheme(root);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(maximized);
            primaryStage.show();

        } catch (IOException e) {
            log.error("Erro ao carregar tela: {}", fxmlPath, e);
            throw new RuntimeException("Não foi possível carregar: " + fxmlPath, e);
        }
    }

    public void toggleDarkMode() {
        darkMode = !darkMode;
        if (primaryStage.getScene() != null) {
            applyTheme(primaryStage.getScene().getRoot());
        }
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    private void applyTheme(Parent root) {
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(darkMode ? "theme-dark" : "theme-light");
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}