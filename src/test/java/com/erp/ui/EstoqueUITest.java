package com.erp.ui;

import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de UI para o módulo de Movimentações de Estoque.
 */
class EstoqueUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void navegarParaEstoque() {
        inserirDadosBase();
        fazerLogin();
        clickOn("Movimentações");
        sleep(600);
    }

    // ---- Teste 1: Tela carrega ----

    @Test
    void dado_tela_estoque_quando_abrir_entao_tabela_posicao_visivel() {
        assertThat(lookup("#tabelaPosicao").tryQuery()).isPresent();
    }

    // ---- Teste 2: Produto aparece na posição ----

    @Test
    void dado_produto_cadastrado_quando_listar_estoque_entao_produto_visivel() {
        aguardarElemento("#tabelaPosicao");
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tabelaPosicao").query();
        assertThat(tabela.getItems()).isNotEmpty();
    }

    // ---- Teste 3: Botão registrar movimentação abre formulário ----

    @Test
    void dado_tela_estoque_quando_clicar_registrar_entao_modal_abre() {
        aguardarElemento("#btnRegistrarMovimentacao");
        clickOn("#btnRegistrarMovimentacao");
        sleep(500);
        assertThat(lookup("#cmbProduto").tryQuery()).isPresent();
    }

    // ---- Teste 4: Motivo obrigatório ----
    // Motivo é validado antes do produto — não é necessário selecionar produto

    @Test
    void dado_modal_movimentacao_sem_motivo_quando_registrar_entao_erro_motivo_visivel() {
        aguardarElemento("#btnRegistrarMovimentacao");
        clickOn("#btnRegistrarMovimentacao");
        sleep(500);
        // Preencher quantidade mas deixar motivo vazio
        clickOn("#txtQuantidade").eraseText(10).write("5");
        // Clicar em registrar sem motivo — o check de motivo vem antes do check de produto
        clickOn("#btnRegistrar");
        sleep(400);
        // Deve mostrar label de erro de motivo
        assertThat(lookup("#errMotivo").tryQuery()).isPresent();
        FxAssert.verifyThat("#errMotivo", NodeMatchers.isVisible());
    }

    // ---- Teste 5: Aba Histórico ----

    @Test
    void dado_tela_estoque_quando_clicar_historico_entao_tabela_historico_visivel() {
        clickOn("Histórico");
        sleep(300);
        assertThat(lookup("#tabelaHistorico").tryQuery()).isPresent();
    }

    // ---- Teste 6: Filtro de busca ----

    @Test
    void dado_tela_estoque_quando_buscar_texto_inexistente_entao_tabela_vazia() {
        aguardarElemento("#txtBuscaEstoque");
        clickOn("#txtBuscaEstoque").write("xxxxxxxNaoExiste999");
        sleep(300);
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tabelaPosicao").query();
        assertThat(tabela.getItems()).isEmpty();
    }

    // ---- Teste 7: Filtro de situação ----

    @Test
    void dado_tela_estoque_quando_filtrar_por_normal_entao_combo_aceita_selecao() {
        aguardarElemento("#cmbFiltroEstoque");
        clickOn("#cmbFiltroEstoque");
        sleep(200);
        clickOn("Normal");
        sleep(300);
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tabelaPosicao").query();
        assertThat(tabela).isNotNull();
    }
}
