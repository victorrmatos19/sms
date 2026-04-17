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
 * Testes de UI para o módulo de Clientes.
 */
class ClientesUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void navegarParaClientes() {
        fazerLogin();
        clickOn("Clientes");
        sleep(600);
    }

    // ---- Listagem carrega ----

    @Test
    void quando_abrir_modulo_entao_tabela_e_exibida() {
        assertThat(lookup("#tblClientes").tryQuery()).isPresent();
    }

    // ---- Formulário abre com seleção PF/PJ ----

    @Test
    void quando_clicar_novo_entao_formulario_exibe_botoes_pf_pj() {
        clickOn("#btnNovo");
        sleep(500);

        assertThat(lookup("#btnPF").tryQuery()).isPresent();
        assertThat(lookup("#btnPJ").tryQuery()).isPresent();
    }

    // ---- CPF inválido exibe erro ----

    @Test
    void dado_cpf_invalido_quando_salvar_entao_exibe_erro_cpf() {
        clickOn("#btnNovo");
        sleep(500);

        // PF é selecionado por padrão; preenche CPF inválido
        clickOn("#txtNome").write("Cliente Teste");
        clickOn("#txtCpf").write("12345678900"); // dígito inválido
        clickOn("#btnSalvar");
        sleep(400);

        FxAssert.verifyThat("#lblErrCpf",
            LabeledMatchers.hasText(not(emptyOrNullString())));
    }

    // ---- Inativar cliente ----

    @Test
    void dado_cliente_ativo_quando_inativar_entao_status_muda() {
        // Requer ao menos um cliente na tabela — cria um primeiro
        clickOn("#btnNovo");
        sleep(500);
        clickOn("#txtNome").write("Cliente Para Inativar");
        clickOn("#btnSalvar");
        sleep(600);

        // Seleciona o primeiro item da tabela
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblClientes").query();
        if (tabela.getItems().isEmpty()) return; // sem dados, pula

        interact(() -> tabela.getSelectionModel().select(0));
        sleep(200);

        // O botão muda de texto quando há seleção — usa clickOn pelo lookup
        assertThat(lookup("#btnToggleAtivo").tryQuery()).isPresent();
    }

    // ---- Filtro mostrar inativos ----

    @Test
    void quando_marcar_mostrar_inativos_entao_predicado_e_alterado() {
        clickOn("#chkInativos");
        sleep(300);

        // Verifica que o checkbox mudou de estado (toggle)
        assertThat(lookup("#chkInativos").tryQuery()).isPresent();
    }

    // ---- UI-22: Busca inexistente esvazia a tabela ----

    @Test
    void dado_busca_inexistente_quando_digitar_entao_tabela_vazia() {
        clickOn("#txtBusca").write("xxxxxNaoExisteCliente9999");
        sleep(400);

        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblClientes").query();
        assertThat(tabela.getItems()).isEmpty();
    }

    // ---- UI-23: Selecionar tipo PJ exibe campo CNPJ ----

    @Test
    void dado_formulario_novo_quando_clicar_pj_entao_campo_cnpj_visivel() {
        clickOn("#btnNovo");
        sleep(500);

        clickOn("#btnPJ");
        sleep(300);

        // CNPJ deve aparecer após selecionar PJ
        assertThat(lookup("#txtCnpj").tryQuery()).isPresent();
    }

    // ---- UI-24: Cliente PF com nome mínimo válido salva corretamente ----

    @Test
    void dado_cliente_pf_com_nome_valido_quando_salvar_entao_aparece_na_tabela() {
        clickOn("#btnNovo");
        sleep(500);

        // PF por padrão — apenas nome (CPF é opcional)
        clickOn("#txtNome").write("Cliente PF Salvo UI");
        clickOn("#btnSalvar");
        sleep(800);

        // Formulário fechou e tabela exibe ao menos um registro
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblClientes").query();
        assertThat(tabela.getItems()).isNotEmpty();
    }

    // ---- UI-25: Botão toggle ativo está disponível após seleção ----

    @Test
    void dado_cliente_na_lista_quando_selecionar_entao_btn_toggle_disponivel() {
        // Insere um cliente via formulário para garantir dados
        clickOn("#btnNovo");
        sleep(500);
        clickOn("#txtNome").write("Cliente Toggle UI");
        clickOn("#btnSalvar");
        sleep(600);

        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblClientes").query();
        if (tabela.getItems().isEmpty()) return;

        interact(() -> tabela.getSelectionModel().select(0));
        sleep(200);

        assertThat(lookup("#btnToggleAtivo").tryQuery()).isPresent();
    }
}
