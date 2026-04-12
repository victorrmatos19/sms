package com.erp.ui;

import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.matcher.control.LabeledMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * Testes de UI para o módulo de Funcionários.
 */
class FuncionariosUITest extends BaseUITest {

    @BeforeEach
    void navegarParaFuncionarios() {
        fazerLogin();
        clickOn("Funcionários");
        sleep(600);
    }

    // ---- Listagem carrega ----

    @Test
    void quando_abrir_modulo_entao_tabela_e_exibida() {
        assertThat(lookup("#tblFuncionarios").tryQuery()).isPresent();
    }

    // ---- Formulário abre ----

    @Test
    void quando_clicar_novo_entao_formulario_e_aberto() {
        clickOn("#btnNovo");
        sleep(500);

        assertThat(lookup("#txtNome").tryQuery()).isPresent();
    }

    // ---- Validação: nome obrigatório ----

    @Test
    void dado_formulario_vazio_quando_salvar_entao_exibe_erro_nome() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#btnSalvar");
        sleep(400);

        FxAssert.verifyThat("#lblErrNome",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- Cadastro com nome válido ----

    @Test
    void dado_nome_valido_quando_salvar_entao_funcionario_aparece_na_tabela() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#txtNome").write("Funcionário Teste UI");

        clickOn("#btnSalvar");
        sleep(600);

        // Formulário fechou — tabela contém ao menos um registro
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblFuncionarios").query();
        assertThat(tabela.getItems()).isNotEmpty();
    }
}
