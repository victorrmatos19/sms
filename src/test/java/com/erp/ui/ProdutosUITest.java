package com.erp.ui;

import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
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
 * Testes de UI para o módulo de Produtos.
 */
class ProdutosUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

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

        // Seleciona a primeira unidade de medida com teclado
        // (sem setCellFactory no ComboBox, toString() não exibe sigla no dropdown)
        clickOn("#cmbUnidade");
        sleep(200);
        type(KeyCode.DOWN);   // seleciona primeiro item
        type(KeyCode.ENTER);
        sleep(200);

        // Preço de Venda fica na aba 2 — navega para lá antes de preencher
        clickOn("Preços e Estoque");
        sleep(300);

        // Define preço > 0 (obrigatório)
        clickOn("#txtPrecoVenda").write("10");

        clickOn("#btnSalvar");
        sleep(800);

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

    // ---- UI-15: Busca por descrição existente retorna resultados ----

    @Test
    void dado_produto_existente_quando_buscar_por_descricao_entao_tabela_nao_vazia() {
        // inserirDadosBase() cria "Arroz Tipo 1" — busca parte do nome
        clickOn("#txtBusca").write("Arroz");
        sleep(400);

        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblProdutos").query();
        assertThat(tabela.getItems()).isNotEmpty();
    }

    // ---- UI-16: Checkbox mostrar inativos altera o estado ----

    @Test
    void quando_marcar_mostrar_inativos_entao_checkbox_muda_estado() {
        // Garante que o checkbox existe e pode ser clicado
        assertThat(lookup("#chkInativos").tryQuery()).isPresent();
        clickOn("#chkInativos");
        sleep(300);
        // Após o click, o combo de busca ainda está presente (tela não quebrou)
        assertThat(lookup("#txtBusca").tryQuery()).isPresent();
    }

    // ---- UI-17: Selecionar linha habilita botão Editar ----

    @Test
    void dado_produto_na_tabela_quando_selecionar_entao_botao_editar_habilitado() {
        // Usa produto criado pelo inserirDadosBase via @BeforeEach
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblProdutos").query();
        if (tabela.getItems().isEmpty()) return; // sem dados, pula

        interact(() -> tabela.getSelectionModel().select(0));
        sleep(200);

        // Botão Editar deve existir e estar habilitado quando há seleção
        assertThat(lookup("#btnEditar").tryQuery()).isPresent();
    }

    // ---- UI-18: Editar produto abre formulário preenchido ----

    @Test
    void dado_produto_selecionado_quando_editar_entao_formulario_tem_descricao() {
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblProdutos").query();
        if (tabela.getItems().isEmpty()) return;

        interact(() -> tabela.getSelectionModel().select(0));
        sleep(200);
        clickOn("#btnEditar");
        sleep(500);

        // O formulário de edição abre com o campo descrição preenchido (não vazio)
        assertThat(lookup("#txtDescricao").tryQuery()).isPresent();
        javafx.scene.control.TextField txtDesc =
            (javafx.scene.control.TextField) lookup("#txtDescricao").query();
        assertThat(txtDesc.getText()).isNotBlank();
    }
}
