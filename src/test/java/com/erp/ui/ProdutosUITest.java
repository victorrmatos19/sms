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
 * Testes de UI para o módulo de Produtos.
 */
class ProdutosUITest extends BaseUITest {

    @BeforeEach
    void navegarParaProdutos() {
        fazerLogin();
        clickOn("Produtos");
        sleep(600);
    }

    // ---- Listagem carrega ----

    @Test
    void quando_abrir_modulo_entao_tabela_e_exibida() {
        assertThat(lookup("#tblProdutos").tryQuery()).isPresent();
    }

    // ---- Botão Novo abre formulário ----

    @Test
    void quando_clicar_novo_entao_formulario_e_aberto() {
        clickOn("#btnNovo");
        sleep(500);

        // Formulário abre em dialog — txtDescricao é campo obrigatório
        assertThat(lookup("#txtDescricao").tryQuery()).isPresent();
    }

    // ---- Validação: descrição obrigatória ----

    @Test
    void dado_formulario_vazio_quando_salvar_entao_exibe_erro_descricao() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#btnSalvar");
        sleep(400);

        FxAssert.verifyThat("#lblErrDescricao",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- Cadastro com dados válidos ----

    @Test
    void dado_produto_valido_quando_salvar_entao_aparece_na_tabela() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#txtDescricao").write("Produto Teste UI");

        clickOn("#btnSalvar");
        sleep(600);

        // Formulário fechou — tabela está visível e produto foi inserido
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblProdutos").query();
        assertThat(tabela.getItems()).isNotEmpty();
    }

    // ---- Filtro de busca ----

    @Test
    void quando_digitar_busca_entao_tabela_e_filtrada() {
        clickOn("#txtBusca").write("xxxxxxxxxnaoexiste");
        sleep(400);

        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblProdutos").query();
        assertThat(tabela.getItems()).isEmpty();
    }
}
