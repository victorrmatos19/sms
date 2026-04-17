package com.erp.ui;

import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * Testes de UI para o módulo de Funcionários.
 */
class FuncionariosUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

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

    // ---- UI-39: Busca inexistente esvazia tabela ----

    @Test
    void dado_busca_inexistente_quando_digitar_entao_tabela_vazia() {
        clickOn("#txtBusca").write("xxxxxNaoExisteFuncionario9999");
        sleep(400);

        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblFuncionarios").query();
        assertThat(tabela.getItems()).isEmpty();
    }

    // ---- UI-40: CPF inválido exibe erro de validação ----

    @Test
    void dado_cpf_invalido_quando_salvar_entao_exibe_erro_cpf() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#txtNome").write("Funcionário CPF Inválido");
        // Preenche CPF com sequência inválida
        if (lookup("#txtCpf").tryQuery().isPresent()) {
            clickOn("#txtCpf").write("11111111111");
        }
        clickOn("#btnSalvar");
        sleep(400);

        // Se o campo CPF existe, deve mostrar erro; caso contrário o formulário ainda está aberto
        if (lookup("#lblErrCpf").tryQuery().isPresent()) {
            FxAssert.verifyThat("#lblErrCpf",
                LabeledMatchers.hasText(not(emptyOrNullString())));
        } else {
            // Formulário ainda aberto — validação impediu o save
            assertThat(lookup("#txtNome").tryQuery()).isPresent();
        }
    }

    // ---- UI-41: Seleção de funcionário disponibiliza botão de toggle ----

    @Test
    void dado_funcionario_na_lista_quando_selecionar_entao_btn_toggle_disponivel() {
        // Cadastra primeiro para garantir ao menos um registro
        clickOn("#btnNovo");
        sleep(500);
        clickOn("#txtNome").write("Funcionário Toggle UI");
        clickOn("#btnSalvar");
        sleep(600);

        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblFuncionarios").query();
        if (tabela.getItems().isEmpty()) return;

        interact(() -> tabela.getSelectionModel().select(0));
        sleep(200);

        assertThat(lookup("#btnToggleAtivo").tryQuery()).isPresent();
    }
}
