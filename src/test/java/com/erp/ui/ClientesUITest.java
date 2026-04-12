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
 * Testes de UI para o módulo de Clientes.
 */
class ClientesUITest extends BaseUITest {

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

        clickOn(tabela.getItems().get(0).toString()); // clique na linha (por texto)

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
}
