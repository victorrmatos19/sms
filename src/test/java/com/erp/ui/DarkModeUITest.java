package com.erp.ui;

import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de UI para o modo escuro (dark/light toggle).
 *
 * Convenção de ícone no btnTema:
 *   "☀"  → tema atual é DARK (ícone indica "mudar para light")
 *   "☾"  → tema atual é LIGHT (ícone indica "mudar para dark")
 */
class DarkModeUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void garantirTemaDark() {
        // Garante que o tema salvo no banco para o admin seja DARK antes de cada teste
        try {
            javax.sql.DataSource ds = springCtx.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection conn = ds.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "UPDATE configuracao SET tema = 'DARK' " +
                    "WHERE empresa_id = 1"
                );
            }
        } catch (Exception e) {
            // Se a configuração ainda não existir o tema padrão já é DARK — ok
        }
    }

    // ---- 1. Dark mode é o padrão → btnTema exibe "☀" ----

    @Test
    void dado_dark_mode_padrao_quando_logar_entao_tema_escuro_ativo() {
        fazerLogin("admin", "admin123");
        sleep(800);

        aguardarElemento("#btnTema");
        Button btnTema = (Button) lookup("#btnTema").query();
        // Quando o tema é DARK, o botão mostra o ícone de sol (para mudar para light)
        assertThat(btnTema.getText()).isEqualTo("☀");
    }

    // ---- 2. Clicar no botão muda de DARK para LIGHT ----

    @Test
    void dado_dark_mode_quando_alternar_entao_muda_para_light() {
        fazerLogin("admin", "admin123");
        sleep(800);

        aguardarElemento("#btnTema");
        // Verifica que estamos em DARK
        Button btnTema = (Button) lookup("#btnTema").query();
        assertThat(btnTema.getText()).isEqualTo("☀");

        // Clica para mudar para LIGHT
        clickOn("#btnTema");
        sleep(400);

        // Agora deve exibir o ícone de lua (indica modo light ativo)
        assertThat(btnTema.getText()).isEqualTo("☾");
    }

    // ---- 3. Tema persiste ao navegar entre telas ----

    @Test
    void dado_dark_mode_quando_navegar_entre_telas_entao_mantem() {
        fazerLogin("admin", "admin123");
        sleep(800);

        // Muda para LIGHT
        clickOn("#btnTema");
        sleep(400);

        // Navega para Produtos
        clickOn("Produtos");
        sleep(600);

        // Volta para o dashboard
        clickOn("#imgLogoTopbar");
        sleep(600);

        // Tema ainda deve ser LIGHT (ícone de lua)
        Button btnTema = (Button) lookup("#btnTema").query();
        assertThat(btnTema.getText()).isEqualTo("☾");
    }

    // ---- UI-61: Alternar de LIGHT para DARK funciona ----

    @Test
    void dado_light_mode_quando_alternar_entao_muda_para_dark() {
        fazerLogin("admin", "admin123");
        sleep(800);

        // Muda para LIGHT primeiro
        clickOn("#btnTema");
        sleep(300);
        Button btnTema = (Button) lookup("#btnTema").query();
        assertThat(btnTema.getText()).isEqualTo("☾");

        // Muda de volta para DARK
        clickOn("#btnTema");
        sleep(300);
        assertThat(btnTema.getText()).isEqualTo("☀");
    }

    // ---- UI-62: Tema LIGHT persiste após mudar de módulo e voltar ----

    @Test
    void dado_light_mode_quando_navegar_clientes_e_voltar_entao_mantem_light() {
        fazerLogin("admin", "admin123");
        sleep(800);

        // Muda para LIGHT
        clickOn("#btnTema");
        sleep(400);

        // Navega para Clientes e volta
        clickOn("Clientes");
        sleep(500);
        clickOn("#imgLogoTopbar");
        sleep(600);

        Button btnTema = (Button) lookup("#btnTema").query();
        assertThat(btnTema.getText()).isEqualTo("☾");
    }
}
