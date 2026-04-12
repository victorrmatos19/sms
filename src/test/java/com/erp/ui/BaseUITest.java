package com.erp.ui;

import com.erp.MainApp;
import com.erp.StageManager;
import com.erp.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base para todos os testes de UI TestFX.
 *
 * Cada subclasse deve declarar seu próprio @Start que chama startLoginScreen(stage).
 * Isso é necessário porque TestFX não encontra @Start em classes pai via herança.
 */
@ExtendWith(ApplicationExtension.class)
public abstract class BaseUITest extends FxRobot {

    protected static final ConfigurableApplicationContext springCtx;

    static {
        try {
            Path testDataDir = Files.createTempDirectory("sms_test_data_");
            System.setProperty("erp.postgres.port", "5434");
            System.setProperty("erp.postgres.database", "sms_test");
            System.setProperty("erp.postgres.datadir", testDataDir.toString());

            springCtx = SpringApplication.run(MainApp.class, new String[0]);

            Runtime.getRuntime().addShutdownHook(
                new Thread(() -> springCtx.close(), "spring-shutdown"));

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Carrega a tela de login com dimensões explícitas.
     * Deve ser chamado por um método @Start na subclasse concreta.
     */
    protected void startLoginScreen(Stage stage) throws Exception {
        StageManager sm = springCtx.getBean(StageManager.class);
        sm.setPrimaryStage(stage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        loader.setControllerFactory(springCtx::getBean);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        root.getStyleClass().add("theme-dark");

        stage.setScene(scene);
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.show();

        // Força layout no Monocle headless (pulse não dispara automaticamente)
        root.applyCss();
        root.layout();
    }

    @BeforeEach
    void resetarEstado() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                springCtx.getBean(AuthService.class).logout();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        sleep(200);
    }

    protected void fazerLogin() {
        fazerLogin("admin", "admin123");
    }

    protected void fazerLogin(String login, String senha) {
        clickOn("#txtLogin").write(login);
        clickOn("#txtSenha").write(senha);
        clickOn("#btnEntrar");
        sleep(800);
    }
}
