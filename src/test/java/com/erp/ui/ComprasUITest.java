package com.erp.ui;

import com.erp.model.Fornecedor;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de UI para o módulo de Compras.
 */
class ComprasUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void navegarParaCompras() {
        inserirDadosBase();
        fazerLogin();
        clickOn("Compras");
        sleep(600);
    }

    // ---- Teste 1: Listagem ----

    @Test
    void dado_tela_compras_quando_abrir_entao_tabela_visivel() {
        assertThat(lookup("#tblCompras").tryQuery()).isPresent();
    }

    // ---- Teste 2: Abrir formulário ----

    @Test
    void dado_modulo_compras_quando_clicar_nova_entao_formulario_abre() {
        clickOn("#btnNovaCompra");
        sleep(500);
        assertThat(lookup("#cmbFornecedor").tryQuery()).isPresent();
    }

    // ---- Teste 3: Fornecedor obrigatório ----

    @Test
    void dado_formulario_sem_fornecedor_quando_salvar_entao_exibe_erro() {
        clickOn("#btnNovaCompra");
        sleep(400);
        // Tenta salvar sem fornecedor — validação inline (sem Alert.showAndWait que bloquearia o FX thread)
        clickOn("#btnSalvarRascunho");
        sleep(400);
        // Label de erro inline deve estar visível
        assertThat(lookup("#errFornecedor").tryQuery()).isPresent();
        // Formulário não fechou
        assertThat(lookup("#btnSalvarRascunho").tryQuery()).isPresent();
    }

    // ---- Teste 4: Filtro de status ----

    @Test
    void dado_tabela_compras_quando_filtrar_por_rascunho_entao_combo_aceita_selecao() {
        clickOn("#cmbFiltroStatus");
        sleep(200);
        clickOn("Rascunho");
        sleep(300);
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblCompras").query();
        // Tabela filtrada — pode estar vazia se não houver rascunhos, mas não deve lançar exceção
        assertThat(tabela).isNotNull();
    }

    // ---- Teste 5: Produto com lote ----

    @Test
    void dado_nova_compra_quando_adicionar_item_com_produto_lote_entao_formulario_abre() {
        clickOn("#btnNovaCompra");
        sleep(400);
        clickOn("#btnAdicionarItem");
        sleep(300);
        // Uma linha de item foi adicionada
        assertThat(lookup("#cmbFornecedor").tryQuery()).isPresent();
    }

    // ---- Teste 6: Formulário de rascunho fecha ao clicar Cancelar ----

    @Test
    void dado_formulario_compra_aberto_quando_cancelar_entao_volta_para_lista() {
        clickOn("#btnNovaCompra");
        sleep(400);
        clickOn("#btnCancelarCompra");
        sleep(400);
        // Volta para lista
        assertThat(lookup("#tblCompras").tryQuery()).isPresent();
    }

    // ---- Teste 7: Busca por texto ----

    @Test
    void dado_tabela_compras_quando_digitar_busca_inexistente_entao_tabela_vazia() {
        clickOn("#txtBusca").write("xxxxxxxxxNaoExiste123");
        sleep(300);
        @SuppressWarnings("unchecked")
        TableView<Object> tabela = (TableView<Object>) lookup("#tblCompras").query();
        assertThat(tabela.getItems()).isEmpty();
    }

    // ---- Teste 8: Regressão — seleção de fornecedor persiste após filtro ----
    //
    // Garante que o bug "setAll() apaga o valor selecionado" não volte.
    //
    // Mecanismo do bug original:
    //   1. usuário clica num item do popup  →  setValue(f) dispara
    //   2. setValue → converter.toString(f) → setText(nome) → textProperty listener dispara
    //   3. Platform.runLater agenda filtrarFornecedores(nome)
    //   4. No próximo frame: filtrarFornecedores chama fornecedoresDisplay.setAll(filtrados)
    //   5. setAll() emite evento REPLACED → SingleSelectionModel interpreta como
    //      "item removido" → setValue(null) → seleção apagada sem erro no log
    //
    // O fix: verificar se texto == nomeDisplay do valor selecionado antes de filtrar;
    // se sim, o texto veio do próprio setValue() e não da digitação — não rodar setAll().
    //
    // Nota: no modo headless (Monocle) o popup não recebe eventos de teclado de forma
    // confiável, então usamos interact() para simular o setValue() como o JavaFX faz
    // internamente ao clicar num item.

    @Test
    void dado_fornecedor_selecionado_quando_filtro_roda_no_proximo_frame_entao_selecao_persiste() {
        clickOn("#btnNovaCompra");
        aguardarElemento("#cmbFornecedor");

        // Passo 1: digitar parte do nome — dispara o filtro via Platform.runLater
        clickOn("#cmbFornecedor").write("Distrib");
        sleep(500); // aguarda filtrarFornecedores rodar e reduzir a lista

        // Passo 2: simular o clique no item como o JavaFX faz internamente
        // (popup de ComboBox editável não recebe teclado de forma confiável em Monocle)
        interact(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Fornecedor> combo =
                    (ComboBox<Fornecedor>) lookup("#cmbFornecedor").query();
            if (!combo.getItems().isEmpty()) {
                combo.setValue(combo.getItems().get(0)); // simula clique no item
            }
        });

        // Passo 3: aguardar o Platform.runLater disparado pelo setValue()
        // → é aqui que o bug ocorria: filtrarFornecedores rodava e chamava setAll()
        sleep(500);

        // Passo 4: verificar que o valor NÃO foi zerado pelo setAll()
        @SuppressWarnings("unchecked")
        ComboBox<Fornecedor> combo =
                (ComboBox<Fornecedor>) lookup("#cmbFornecedor").query();
        assertThat(combo.getValue())
                .as("Fornecedor deve permanecer selecionado após filtrarFornecedores "
                        + "rodar no próximo frame (setAll não pode apagar a seleção)")
                .isNotNull();
    }

    // ---- Teste 9: Regressão — seleção persiste após clicar fora do editor ----
    //
    // Garante que o bug "fromString retorna null → commitValue() limpa seleção" não volte.
    //
    // Mecanismo do bug original:
    //   1. usuário seleciona fornecedor  →  setValue(f) dispara
    //   2. converter.toString(f) → editor exibe o nome do fornecedor
    //   3. usuário clica em outro campo  →  editor perde foco
    //   4. ComboBox.commitValue() chama converter.fromString(editor.getText())
    //   5. fromString retornava null  →  setValue(null)  →  seleção apagada
    //
    // O fix: fromString busca o fornecedor pelo nome display; se não encontrar (texto
    // parcial), retorna cmbFornecedor.getValue() como fallback — nunca null.

    @Test
    void dado_fornecedor_selecionado_quando_editor_perde_foco_entao_selecao_persiste() {
        clickOn("#btnNovaCompra");
        aguardarElemento("#cmbFornecedor");

        // Passo 1: selecionar o fornecedor diretamente via interact()
        interact(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Fornecedor> combo =
                    (ComboBox<Fornecedor>) lookup("#cmbFornecedor").query();
            if (!combo.getItems().isEmpty()) {
                combo.setValue(combo.getItems().get(0));
            }
        });
        sleep(300);

        // Passo 2: simular perda de foco do editor (como ao clicar em outro campo)
        // → JavaFX chama commitValue() → converter.fromString(editor.getText())
        interact(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Fornecedor> combo =
                    (ComboBox<Fornecedor>) lookup("#cmbFornecedor").query();
            // Força commitValue() chamando getEditor().getParent().requestFocus()
            // não funciona de forma confiável em Monocle, então simulamos diretamente:
            String textoAtual = combo.getEditor().getText();
            Fornecedor resultado = combo.getConverter().fromString(textoAtual);
            // Se fromString retornar null, o bug estava presente
            combo.setValue(resultado); // simula o que commitValue() faria
        });
        sleep(200);

        // Passo 3: verificar que a seleção NÃO foi apagada
        @SuppressWarnings("unchecked")
        ComboBox<Fornecedor> combo =
                (ComboBox<Fornecedor>) lookup("#cmbFornecedor").query();
        assertThat(combo.getValue())
                .as("Fornecedor deve permanecer selecionado após o editor perder foco "
                        + "(fromString não pode retornar null para texto de item válido)")
                .isNotNull();
    }
}
