package com.erp;

import com.erp.service.BackupService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Ponto de entrada da aplicação.
 * Inicializa o Spring Boot e depois o JavaFX na sequência correta.
 */
@SpringBootApplication
public class MainApp extends Application {

    private static ConfigurableApplicationContext springContext;
    private static String[] savedArgs;

    public static void main(String[] args) {
        savedArgs = args;
        // JavaFX precisa ser o thread principal
        launch(args);
    }

    @Override
    public void init() {
        // Backup/restauração precisam rodar antes do Spring iniciar o PostgreSQL embarcado.
        BackupService.executarManutencaoPreStartup();

        // Spring sobe antes da janela aparecer (carrega BD, migrations, etc.)
        springContext = SpringApplication.run(MainApp.class, savedArgs);
    }

    @Override
    public void start(Stage primaryStage) {
        // Obtém o StageManager do contexto Spring e abre a tela inicial
        StageManager stageManager = springContext.getBean(StageManager.class);
        stageManager.init(primaryStage);
        stageManager.showLoginScreen();
    }

    @Override
    public void stop() {
        // Encerra Spring ao fechar a janela
        springContext.close();
        Platform.exit();
    }
}
