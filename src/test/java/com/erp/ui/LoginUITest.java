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

    // ---- UI-05: Apenas espaços no login não autentica ----

    @Test
    void dado_login_apenas_espacos_quando_logar_entao_exibe_erro() {
        clickOn("#txtLogin").write("   ");
        clickOn("#txtSenha").write("admin123");
        clickOn("#btnEntrar");
        sleep(400);

        FxAssert.verifyThat("#lblErro",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- UI-06: Login preenchido mas senha em branco ----

    @Test
    void dado_login_preenchido_senha_vazia_quando_logar_entao_exibe_erro() {
        clickOn("#txtLogin").write("admin");
        // senha permanece vazia
        clickOn("#btnEntrar");
        sleep(400);

        FxAssert.verifyThat("#lblErro",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- UI-07: Usuário inativo não pode logar ----

    @Test
    void dado_usuario_inativo_quando_logar_entao_exibe_erro() {
        // Cria usuário inativo via JDBC
        try {
            javax.sql.DataSource ds = springCtx.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection conn = ds.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "INSERT INTO usuario (empresa_id, nome, login, senha_hash, ativo) " +
                    "VALUES (1, 'Inativo Teste', 'inativo.ui', " +
                    "'$2a$10$Vn0lZc73XD9DHdsPQ2zJOOIqQ.EUdFjZZGfqeP.j8y7m1OsFRAZO6', false) " +
                    "ON CONFLICT (empresa_id, login) DO UPDATE SET ativo = false"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        clickOn("#txtLogin").write("inativo.ui");
        clickOn("#txtSenha").write("admin123");
        clickOn("#btnEntrar");
        sleep(400);

        FxAssert.verifyThat("#lblErro",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- UI-08: Após login bem-sucedido o campo de erro não está visível ----

    @Test
    void dado_credenciais_validas_quando_logar_entao_lblErro_nao_visivel() {
        clickOn("#txtLogin").write("admin");
        clickOn("#txtSenha").write("admin123");
        clickOn("#btnEntrar");
        sleep(1000);

        // A tela de login desaparece — lblErro não é mais acessível
        assertThat(lookup("#lblErro").tryQuery()).isEmpty();
    }
}
