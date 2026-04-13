package com.erp.ui;

import com.erp.controller.MainController;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de UI para os dashboards de cada perfil de usuário.
 */
class DashboardUITest extends BaseUITest {

    @Start
    void start(Stage stage) throws Exception {
        startLoginScreen(stage);
    }

    @BeforeEach
    void criarUsuariosDeTest() {
        try {
            javax.sql.DataSource ds = springCtx.getBean(javax.sql.DataSource.class);
            try (java.sql.Connection conn = ds.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {

                // Usuário com perfil VENDAS — senha: admin123
                stmt.execute(
                    "INSERT INTO usuario (empresa_id, perfil_id, nome, login, senha_hash, ativo) " +
                    "SELECT 1, (SELECT id FROM perfil_acesso WHERE nome='VENDAS'), " +
                    "'Pedro Vendas', 'pedro.vendas', " +
                    "'$2a$10$Vn0lZc73XD9DHdsPQ2zJOOIqQ.EUdFjZZGfqeP.j8y7m1OsFRAZO6', true " +
                    "ON CONFLICT (empresa_id, login) DO NOTHING"
                );

                // Usuário com perfil FINANCEIRO — senha: admin123
                stmt.execute(
                    "INSERT INTO usuario (empresa_id, perfil_id, nome, login, senha_hash, ativo) " +
                    "SELECT 1, (SELECT id FROM perfil_acesso WHERE nome='FINANCEIRO'), " +
                    "'Maria Financeiro', 'maria.financeiro', " +
                    "'$2a$10$Vn0lZc73XD9DHdsPQ2zJOOIqQ.EUdFjZZGfqeP.j8y7m1OsFRAZO6', true " +
                    "ON CONFLICT (empresa_id, login) DO NOTHING"
                );

                // Usuário com perfil ESTOQUE — senha: admin123
                stmt.execute(
                    "INSERT INTO usuario (empresa_id, perfil_id, nome, login, senha_hash, ativo) " +
                    "SELECT 1, (SELECT id FROM perfil_acesso WHERE nome='ESTOQUE'), " +
                    "'Joao Estoque', 'joao.estoque', " +
                    "'$2a$10$Vn0lZc73XD9DHdsPQ2zJOOIqQ.EUdFjZZGfqeP.j8y7m1OsFRAZO6', true " +
                    "ON CONFLICT (empresa_id, login) DO NOTHING"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar usuários de teste", e);
        }
    }

    // ---- 1. Login admin → dashboard-admin carrega ----

    @Test
    void dado_login_admin_quando_entrar_entao_carrega_dashboard_admin() {
        fazerLogin("admin", "admin123");
        sleep(1000);

        // Dashboard admin contém o gráfico de compras
        assertThat(lookup("#chartCompras").tryQuery()).isPresent();
    }

    // ---- 2. Login vendas → dashboard-vendas carrega ----

    @Test
    void dado_login_vendas_quando_entrar_entao_carrega_dashboard_vendas() {
        fazerLogin("pedro.vendas", "admin123");
        sleep(1000);

        // Dashboard vendas contém lblComprasMes
        assertThat(lookup("#lblComprasMes").tryQuery()).isPresent();
    }

    // ---- 3. Login financeiro → dashboard-financeiro carrega ----

    @Test
    void dado_login_financeiro_quando_entrar_entao_carrega_dashboard_financeiro() {
        fazerLogin("maria.financeiro", "admin123");
        sleep(1000);

        // Dashboard financeiro contém lblContasAbertas
        assertThat(lookup("#lblContasAbertas").tryQuery()).isPresent();
    }

    // ---- 4. Login estoque → dashboard-estoque carrega ----

    @Test
    void dado_login_estoque_quando_entrar_entao_carrega_dashboard_estoque() {
        fazerLogin("joao.estoque", "admin123");
        sleep(1000);

        // Dashboard estoque contém lblTotalProdutos
        assertThat(lookup("#lblTotalProdutos").tryQuery()).isPresent();
    }

    // ---- 5. Navegar para módulo e voltar → dashboard recarrega ----
    //
    // O clique no ImageView (logo) usa onMouseClicked que não dispara eventos
    // de mouse de forma confiável no Monocle headless. Para garantir estabilidade
    // do teste, disparamos carregarDashboard() via Platform.runLater — que é
    // exatamente o que irParaDashboard() executa quando o logo é clicado.

    @Test
    void dado_dashboard_aberto_quando_navegar_e_voltar_entao_dashboard_recarrega()
            throws InterruptedException {
        fazerLogin("admin", "admin123");

        // Navega para Produtos
        clickOn("Produtos");
        aguardarElemento("#tblProdutos");
        assertThat(lookup("#tblProdutos").tryQuery()).isPresent();

        // Volta ao dashboard pelo mesmo caminho que o logo acionaria
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                springCtx.getBean(MainController.class).carregarDashboard();
            } finally {
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);

        // Dashboard admin deve ter sido recarregado
        aguardarElemento("#chartCompras");
        assertThat(lookup("#chartCompras").tryQuery()).isPresent();
    }
}
