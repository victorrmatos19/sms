package com.erp.ui;

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

    // ---- 8. Dashboard Admin carrega após login ----

    @Test
    void smoke_dashboard_admin_carrega() {
        fazerLogin("admin", "admin123");
        sleep(1000);

        // Dashboard admin exibe o gráfico de compras da semana
        assertThat(lookup("#chartCompras").tryQuery()).isPresent();
    }
}
