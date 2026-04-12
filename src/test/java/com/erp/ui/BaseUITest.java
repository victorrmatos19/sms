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

            // Impede que o JavaFX Platform encerre o FX thread quando a última
            // janela é fechada durante o cleanup do TestFX entre os testes.
            // Sem isso, afterEach lança "FX Application Thread not running" e
            // os testes subsequentes não conseguem criar uma nova Stage.
            javafx.application.Platform.setImplicitExit(false);

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
        loader.setClassLoader(getClass().getClassLoader()); // FX thread pode ter classLoader null no Surefire
        loader.setControllerFactory(springCtx::getBean);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        root.getStyleClass().add("theme-dark");

        stage.setScene(scene);
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.show();
        stage.toFront();
        stage.requestFocus();

        // Força layout no Monocle headless (pulse não dispara automaticamente)
        root.applyCss();
        root.layout();
    }

    @BeforeEach
    void resetarEstado() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // No macOS, o FX thread corre na AppKit main thread e é nomeado "AppKit Thread".
            // TestFX.isFXApplicationThreadRunning() procura pelo nome exato
            // "JavaFX Application Thread" — sem esse rename, cleanupAfterTest() lança
            // TimeoutException em todo afterEach, marcando os testes como ERROR.
            if (!Thread.currentThread().getName().equals("JavaFX Application Thread")) {
                Thread.currentThread().setName("JavaFX Application Thread");
            }
            try {
                springCtx.getBean(AuthService.class).logout();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        sleep(200);
    }

    protected void inserirDadosBase() {
        try {
            javax.sql.DataSource ds = springCtx.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection conn = ds.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {

                // Grupo de produto
                stmt.execute(
                    "INSERT INTO grupo_produto (empresa_id, nome, ativo) " +
                    "VALUES (1, 'Alimentos', true) " +
                    "ON CONFLICT (empresa_id, nome) DO NOTHING"
                );

                // Unidade de medida
                stmt.execute(
                    "INSERT INTO unidade_medida (empresa_id, sigla, descricao) " +
                    "VALUES (1, 'UN', 'Unidade') " +
                    "ON CONFLICT (empresa_id, sigla) DO NOTHING"
                );

                // Produto sem lote
                stmt.execute(
                    "INSERT INTO produto " +
                    "(empresa_id, descricao, codigo_interno, preco_custo, preco_venda, preco_minimo, " +
                    "margem_lucro, estoque_atual, estoque_minimo, ativo, usa_lote_validade, unidade_id) " +
                    "SELECT 1, 'Arroz Tipo 1', 'ALC001', 18.50, 24.90, 19.43, 34.59, 50, 20, " +
                    "true, false, id FROM unidade_medida WHERE sigla='UN' AND empresa_id=1 " +
                    "ON CONFLICT DO NOTHING"
                );

                // Produto com lote
                stmt.execute(
                    "INSERT INTO produto " +
                    "(empresa_id, descricao, codigo_interno, preco_custo, preco_venda, preco_minimo, " +
                    "margem_lucro, estoque_atual, estoque_minimo, ativo, usa_lote_validade, unidade_id) " +
                    "SELECT 1, 'Leite Integral', 'ALB001', 3.80, 5.99, 3.99, 57.63, 30, 10, " +
                    "true, true, id FROM unidade_medida WHERE sigla='UN' AND empresa_id=1 " +
                    "ON CONFLICT DO NOTHING"
                );

                // Fornecedor
                stmt.execute(
                    "INSERT INTO fornecedor " +
                    "(empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj, ativo) " +
                    "VALUES (1, 'PJ', 'Distribuidora Teste', 'Distribuidora Teste Ltda', " +
                    "'11.222.333/0001-81', true) " +
                    "ON CONFLICT DO NOTHING"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao inserir dados de teste", e);
        }
    }

    protected void aguardarElemento(String query) {
        int tentativas = 0;
        while (tentativas < 20) {
            if (lookup(query).tryQuery().isPresent()) return;
            sleep(200);
            tentativas++;
        }
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
