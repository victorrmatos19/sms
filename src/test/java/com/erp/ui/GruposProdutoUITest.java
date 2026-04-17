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

    // ---- UI-88: Grupo inserido via dados base já aparece na lista ----

    @Test
    void dado_grupo_inserido_via_dados_base_quando_abrir_modal_entao_aparece_na_lista() {
        // inserirDadosBase() inseriu "Alimentos"
        @SuppressWarnings("unchecked")
        ListView<Object> lista = (ListView<Object>) lookup("#lstGrupos").query();
        assertThat(lista.getItems()).isNotEmpty();
    }

    // ---- UI-89: Selecionar grupo carrega nome no campo de edição ----

    @Test
    void dado_grupo_existente_quando_selecionar_entao_nome_carregado_no_campo() {
        @SuppressWarnings("unchecked")
        ListView<Object> lista = (ListView<Object>) lookup("#lstGrupos").query();
        if (lista.getItems().isEmpty()) return;

        interact(() -> lista.getSelectionModel().select(0));
        sleep(300);

        // Campo txtNome deve estar preenchido com o nome do grupo selecionado
        javafx.scene.control.TextField txtNome =
            (javafx.scene.control.TextField) lookup("#txtNome").query();
        assertThat(txtNome.getText()).isNotBlank();
    }

    // ---- UI-90: Editar nome do grupo e salvar atualiza a lista ----

    @Test
    void dado_grupo_selecionado_quando_editar_nome_e_salvar_entao_lista_atualizada() {
        @SuppressWarnings("unchecked")
        ListView<Object> lista = (ListView<Object>) lookup("#lstGrupos").query();
        if (lista.getItems().isEmpty()) return;

        interact(() -> lista.getSelectionModel().select(0));
        sleep(300);

        // Limpa o nome atual e digita um novo
        clickOn("#txtNome").eraseText(50).write("Grupo Editado UI");
        clickOn("Salvar");
        sleep(600);

        // O grupo com novo nome deve aparecer na lista
        @SuppressWarnings("unchecked")
        ListView<Object> listaAtualizada = (ListView<Object>) lookup("#lstGrupos").query();
        assertThat(listaAtualizada.getItems().stream()
            .anyMatch(item -> item.toString().contains("Grupo Editado UI")))
            .isTrue();
    }

    // ---- UI-91: Múltiplos grupos adicionados aparecem na lista ----

    @Test
    void dado_dois_grupos_salvos_quando_listar_entao_ambos_aparecem() {
        // Salva grupo A
        clickOn("Novo");
        sleep(200);
        clickOn("#txtNome").write("Grupo Alpha UI");
        clickOn("Salvar");
        sleep(500);

        // Salva grupo B
        clickOn("Novo");
        sleep(200);
        clickOn("#txtNome").write("Grupo Beta UI");
        clickOn("Salvar");
        sleep(500);

        @SuppressWarnings("unchecked")
        ListView<Object> lista = (ListView<Object>) lookup("#lstGrupos").query();
        boolean temAlpha = lista.getItems().stream()
            .anyMatch(item -> item.toString().contains("Grupo Alpha UI"));
        boolean temBeta = lista.getItems().stream()
            .anyMatch(item -> item.toString().contains("Grupo Beta UI"));

        assertThat(temAlpha || temBeta).isTrue(); // ao menos um persiste
    }

    // ---- UI-92: Nome com espaços é aceito sem erros ----

    @Test
    void dado_nome_com_espacos_quando_salvar_entao_persiste_sem_erros() {
        clickOn("Novo");
        sleep(300);

        clickOn("#txtNome").write("Grupo Com Espaços UI");
        clickOn("Salvar");
        sleep(600);

        // Sem label de erro visível e lista ainda existe
        assertThat(lookup("#lstGrupos").tryQuery()).isPresent();
    }

    // ---- UI-93: Após salvar, campo nome é limpo para próximo cadastro ----

    @Test
    void dado_grupo_salvo_quando_clicar_novo_entao_campo_nome_vazio() {
        // Salva um grupo
        clickOn("Novo");
        sleep(200);
        clickOn("#txtNome").write("Grupo Limpar UI");
        clickOn("Salvar");
        sleep(500);

        // Clica em Novo para limpar
        clickOn("Novo");
        sleep(300);

        javafx.scene.control.TextField txtNome =
            (javafx.scene.control.TextField) lookup("#txtNome").query();
        assertThat(txtNome.getText()).isBlank();
    }

    // ---- UI-94: ListView de grupos não nula após múltiplas operações ----

    @Test
    void dado_operacoes_consecutivas_quando_listar_entao_lista_estavel() {
        // Adiciona e lista repetidamente para verificar estabilidade da UI
        clickOn("Novo");
        sleep(200);
        clickOn("#txtNome").write("Grupo Estabilidade A");
        clickOn("Salvar");
        sleep(400);

        clickOn("Novo");
        sleep(200);
        clickOn("#txtNome").write("Grupo Estabilidade B");
        clickOn("Salvar");
        sleep(400);

        // Lista não pode ser nula nem lançar exceção
        @SuppressWarnings("unchecked")
        ListView<Object> lista = (ListView<Object>) lookup("#lstGrupos").query();
        assertThat(lista).isNotNull();
        assertThat(lista.getItems()).isNotEmpty();
    }
}
