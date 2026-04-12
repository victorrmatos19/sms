package com.erp.ui;

import com.erp.MainApp;
import com.erp.StageManager;
import com.erp.service.AuthService;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base para todos os testes de UI TestFX.
 * Inicia o contexto Spring uma única vez (banco na porta 5434).
 * Cada teste recebe uma tela de login limpa via @Start.
 */
@ExtendWith(ApplicationExtension.class)
public abstract class BaseUITest extends FxRobot {

    protected static ConfigurableApplicationContext springCtx;
    private static Path testDataDir;

    @BeforeAll
    static void initSpring() throws Exception {
        if (springCtx != null) return;

        testDataDir = Files.createTempDirectory("sms_test_data_");

        System.setProperty("erp.postgres.port", "5434");
        System.setProperty("erp.postgres.database", "sms_test");
        System.setProperty("erp.postgres.datadir", testDataDir.toString());

        springCtx = SpringApplication.run(MainApp.class, new String[0]);
    }

    @Start
    void start(Stage stage) throws Exception {
        StageManager sm = springCtx.getBean(StageManager.class);
        sm.init(stage);
        sm.showLoginScreen();
    }

    @BeforeEach
    void resetarEstado() throws Exception {
        // Garante logout antes de cada teste
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                springCtx.getBean(AuthService.class).logout();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void encerrarSpring() {
        if (springCtx != null) {
            springCtx.close();
            springCtx = null;
        }
    }

    /** Faz login com as credenciais padrão (admin / admin123). */
    protected void fazerLogin() {
        fazerLogin("admin", "admin123");
    }

    protected void fazerLogin(String login, String senha) {
        clickOn("#txtLogin").write(login);
        clickOn("#txtSenha").write(senha);
        clickOn("#btnEntrar");
        sleep(800);
    }

    /** Navega para um módulo pelo ID do botão do menu lateral. */
    protected void abrirModulo(String btnMenuId) {
        clickOn(btnMenuId);
        sleep(600);
    }

    protected JdbcTemplate jdbcTemplate() {
        return springCtx.getBean(JdbcTemplate.class);
    }
}
