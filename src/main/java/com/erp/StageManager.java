package com.erp;

import atlantafx.base.theme.PrimerLight;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
    private Stage primaryStage;
    private boolean darkMode = false;

    public StageManager(ApplicationContext springContext) {
        this.springContext = springContext;
    }

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;

        javafx.application.Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        primaryStage.setTitle("SMS — Simple Manage System");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setMaximized(true);

        // Ícone da janela
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
            loader.setControllerFactory(springContext::getBean);

            Parent root = loader.load();
            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/css/global.css")
                    ).toExternalForm()
            );

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
