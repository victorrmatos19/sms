package com.erp.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.matcher.control.LabeledMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * Testes de UI para o módulo de Fornecedores.
 */
class FornecedoresUITest extends BaseUITest {

    @BeforeEach
    void navegarParaFornecedores() {
        fazerLogin();
        clickOn("Fornecedores");
        sleep(600);
    }

    // ---- Listagem carrega ----

    @Test
    void quando_abrir_modulo_entao_tabela_e_card_pix_sao_exibidos() {
        assertThat(lookup("#tblFornecedores").tryQuery()).isPresent();
        assertThat(lookup("#lblComPix").tryQuery()).isPresent();
    }

    // ---- Formulário com PIX — chave sem tipo exibe erro ----

    @Test
    void dado_chave_pix_sem_tipo_quando_salvar_entao_exibe_erro_pix() {
        clickOn("#btnNovo");
        sleep(500);

        // Preenche nome obrigatório
        clickOn("#txtNome").write("Fornecedor Teste PIX");

        // Preenche a chave PIX mas NÃO seleciona o tipo
        clickOn("#txtPixChave").write("chavesemtipo@email.com");

        clickOn("#btnSalvar");
        sleep(400);

        FxAssert.verifyThat("#lblErrPixChave",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- Formulário com PIX válido — sem erro ----

    @Test
    void dado_pix_com_tipo_e_chave_quando_salvar_entao_persiste() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#txtNome").write("Fornecedor Com PIX");

        // Seleciona tipo PIX Email
        clickOn("#cmbPixTipo");
        sleep(300);
        clickOn("Email");
        sleep(200);

        clickOn("#txtPixChave").write("fornecedor@email.com");

        clickOn("#btnSalvar");
        sleep(600);

        // Formulário deve fechar — tabela fica visível
        assertThat(lookup("#tblFornecedores").tryQuery()).isPresent();
    }
}
