package com.erp.ui;

import javafx.scene.control.ListView;
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
 * Testes de UI para o módulo de Grupos de Produto (acessado a partir de Produtos → Gerenciar Grupos).
 */
class GruposProdutoUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void navegarParaGrupos() {
        inserirDadosBase();
        fazerLogin();
        // Navega para Produtos e depois abre o modal de Grupos
        clickOn("Produtos");
        sleep(600);
        clickOn("Gerenciar Grupos");
        sleep(600);
    }

    // ---- 1. Modal de grupos abre e exibe a lista ----

    @Test
    void dado_modal_grupos_quando_abrir_entao_lista_grupos() {
        // O modal exibe lstGrupos (ListView de grupos)
        assertThat(lookup("#lstGrupos").tryQuery()).isPresent();
    }

    // ---- 2. Novo grupo salvo aparece na lista ----

    @Test
    void dado_novo_grupo_quando_salvar_entao_aparece_na_lista() {
        // Clica em "Novo" para limpar o formulário
        clickOn("Novo");
        sleep(300);

        // Preenche o nome do grupo
        clickOn("#txtNome").write("Grupo UI Teste");

        // Salva
        clickOn("Salvar");
        sleep(600);

        // O grupo deve aparecer na ListView
        aguardarElemento("#lstGrupos");
        @SuppressWarnings("unchecked")
        ListView<Object> lista = (ListView<Object>) lookup("#lstGrupos").query();
        assertThat(lista.getItems().stream()
                .anyMatch(item -> item.toString().contains("Grupo UI Teste")))
                .isTrue();
    }

    // ---- 3. Nome vazio exibe mensagem de erro ----

    @Test
    void dado_nome_grupo_vazio_quando_salvar_entao_erro() {
        // Clica em "Novo" para limpar o formulário
        clickOn("Novo");
        sleep(300);

        // Deixa o nome vazio e clica em Salvar
        clickOn("Salvar");
        sleep(400);

        // Deve exibir label de erro de nome
        FxAssert.verifyThat("#lblErrNome",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }
}
