package com.erp.ui;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * Testes de UI para a tela de Login.
 */
class LoginUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    // ---- Autenticação bem-sucedida ----

    @Test
    void dado_credenciais_validas_quando_logar_entao_abre_tela_principal() {
        clickOn("#txtLogin").write("admin");
        clickOn("#txtSenha").write("admin123");
        clickOn("#btnEntrar");
        sleep(1000);

        // Após login, a tela de login some — label de erro não está visível
        // e a main screen carrega (btnTema é exclusivo do main)
        assertThat(lookup("#btnTema").tryQuery()).isPresent();
    }

    // ---- Credenciais inválidas ----

    @Test
    void dado_senha_errada_quando_logar_entao_exibe_mensagem_de_erro() {
        clickOn("#txtLogin").write("admin");
        clickOn("#txtSenha").write("senhaerrada");
        clickOn("#btnEntrar");
        sleep(400);

        FxAssert.verifyThat("#lblErro",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    @Test
    void dado_login_inexistente_quando_logar_entao_exibe_mensagem_de_erro() {
        clickOn("#txtLogin").write("usuario_nao_existe");
        clickOn("#txtSenha").write("qualquer");
        clickOn("#btnEntrar");
        sleep(400);

        FxAssert.verifyThat("#lblErro",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- Campos em branco ----

    @Test
    void dado_campos_em_branco_quando_logar_entao_exibe_mensagem_de_erro() {
        clickOn("#btnEntrar");
        sleep(400);

        FxAssert.verifyThat("#lblErro",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }
}
