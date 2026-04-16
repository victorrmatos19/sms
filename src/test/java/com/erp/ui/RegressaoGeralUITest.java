package com.erp.ui;

import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests de regressão geral — verifica que todos os módulos principais
 * carregam sem erros após login com admin.
 *
 * Cada teste abre um módulo e verifica que o elemento principal da tela está presente.
 */
class RegressaoGeralUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void prepararDados() {
        inserirDadosBase();
    }

    // ---- 1. Login smoke ----

    @Test
    void smoke_login_admin_abre_tela_principal() {
        fazerLogin("admin", "admin123");
        sleep(800);

        // Tela principal carregou — btnTema é exclusivo do main.fxml
        assertThat(lookup("#btnTema").tryQuery()).isPresent();
    }

    // ---- 2. Módulo Produtos ----

    @Test
    void smoke_modulo_produtos_carrega() {
        fazerLogin();
        clickOn("Produtos");
        sleep(600);

        assertThat(lookup("#tblProdutos").tryQuery()).isPresent();
    }

    // ---- 3. Módulo Clientes ----

    @Test
    void smoke_modulo_clientes_carrega() {
        fazerLogin();
        clickOn("Clientes");
        sleep(600);

        assertThat(lookup("#tblClientes").tryQuery()).isPresent();
    }

    // ---- 4. Módulo Fornecedores ----

    @Test
    void smoke_modulo_fornecedores_carrega() {
        fazerLogin();
        clickOn("Fornecedores");
        sleep(600);

        assertThat(lookup("#tblFornecedores").tryQuery()).isPresent();
    }

    // ---- 5. Módulo Funcionários ----

    @Test
    void smoke_modulo_funcionarios_carrega() {
        fazerLogin();
        clickOn("Funcionários");
        sleep(600);

        assertThat(lookup("#tblFuncionarios").tryQuery()).isPresent();
    }

    // ---- 6. Módulo Estoque ----

    @Test
    void smoke_modulo_estoque_carrega() {
        fazerLogin();
        clickOn("Movimentações");
        sleep(600);

        assertThat(lookup("#tabelaPosicao").tryQuery()).isPresent();
    }

    // ---- 7. Módulo Compras ----

    @Test
    void smoke_modulo_compras_carrega() {
        fazerLogin();
        clickOn("Compras");
        sleep(600);

        assertThat(lookup("#tblCompras").tryQuery()).isPresent();
    }

    // ---- 8. Módulo Vendas ----

    @Test
    void smoke_modulo_vendas_carrega() {
        fazerLogin();
        clickOn("Vendas");
        sleep(600);

        assertThat(lookup("#tblVendas").tryQuery()).isPresent();
    }

    @Test
    void smoke_venda_finalizada_preserva_quantidade_ao_visualizar() {
        inserirVendaFinalizadaComQuantidade();

        fazerLogin();
        clickOn("Vendas");
        aguardarElemento("#tblVendas");
        clickOn("VTEST-QTD");
        clickOn("#btnVisualizar");
        sleep(600);

        assertThat(lookup(node -> node instanceof TextField txt
                && "3.0000".equals(txt.getText())).tryQuery()).isPresent();
    }

    // ---- 9. Módulo Caixa ----

    @Test
    void smoke_modulo_caixa_carrega() {
        fazerLogin();
        clickOn("Caixa");
        sleep(600);

        assertThat(lookup("#tblMovimentacoes").tryQuery()).isPresent();
    }

    // ---- 10. Módulo Contas a Pagar ----

    @Test
    void smoke_modulo_contas_pagar_carrega() {
        fazerLogin();
        clickOn("Contas a Pagar");
        sleep(600);

        assertThat(lookup("#tblContas").tryQuery()).isPresent();
    }

    // ---- 11. Dashboard Admin carrega após login ----

    @Test
    void smoke_dashboard_admin_carrega() {
        fazerLogin("admin", "admin123");
        sleep(1000);

        // Dashboard admin exibe o gráfico de compras da semana
        assertThat(lookup("#chartCompras").tryQuery()).isPresent();
    }

    private void inserirVendaFinalizadaComQuantidade() {
        try {
            javax.sql.DataSource ds = springCtx.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection conn = ds.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {

                stmt.execute("""
                        DELETE FROM venda_pagamento
                        WHERE venda_id IN (
                            SELECT id FROM venda WHERE empresa_id = 1 AND numero = 'VTEST-QTD'
                        )
                        """);
                stmt.execute("""
                        DELETE FROM venda_item
                        WHERE venda_id IN (
                            SELECT id FROM venda WHERE empresa_id = 1 AND numero = 'VTEST-QTD'
                        )
                        """);
                stmt.execute("DELETE FROM venda WHERE empresa_id = 1 AND numero = 'VTEST-QTD'");
                stmt.execute("""
                        INSERT INTO funcionario (empresa_id, nome, cargo, ativo)
                        SELECT 1, 'Vendedor Teste UI', 'Vendedor', true
                        WHERE NOT EXISTS (
                            SELECT 1 FROM funcionario
                            WHERE empresa_id = 1 AND nome = 'Vendedor Teste UI'
                        )
                        """);
                stmt.execute("""
                        INSERT INTO venda (
                            empresa_id, vendedor_id, usuario_id, numero, status, data_venda,
                            valor_produtos, valor_desconto, valor_total,
                            percentual_comissao, valor_comissao
                        )
                        SELECT 1, f.id, u.id, 'VTEST-QTD', 'FINALIZADA', NOW(),
                               74.70, 0.00, 74.70, 0.00, 0.00
                        FROM funcionario f
                        CROSS JOIN usuario u
                        WHERE f.empresa_id = 1
                          AND f.nome = 'Vendedor Teste UI'
                          AND u.empresa_id = 1
                          AND u.login = 'admin'
                        ORDER BY f.id, u.id
                        LIMIT 1
                        """);
                stmt.execute("""
                        INSERT INTO venda_item (
                            venda_id, produto_id, descricao, quantidade, preco_unitario,
                            custo_unitario, desconto, valor_total
                        )
                        SELECT v.id, p.id, p.descricao, 3.0000, 24.9000,
                               18.5000, 0.00, 74.70
                        FROM venda v
                        JOIN produto p ON p.empresa_id = v.empresa_id
                        WHERE v.empresa_id = 1
                          AND v.numero = 'VTEST-QTD'
                          AND p.codigo_interno = 'ALC001'
                        LIMIT 1
                        """);
                stmt.execute("""
                        INSERT INTO venda_pagamento (venda_id, forma_pagamento, valor, parcelas)
                        SELECT id, 'DINHEIRO', 74.70, 1
                        FROM venda
                        WHERE empresa_id = 1 AND numero = 'VTEST-QTD'
                        """);
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao inserir venda de regressão", e);
        }
    }
}
