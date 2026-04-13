package com.erp.controller;

import com.erp.model.dto.dashboard.AlertaDTO;
import com.erp.model.dto.dashboard.DashboardAdminDTO;
import com.erp.model.dto.dashboard.TopProdutoDTO;
import com.erp.model.dto.dashboard.VendaDiariaDTO;
import com.erp.service.AuthService;
import com.erp.service.DashboardService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardAdminController implements Initializable {

    private final DashboardService dashboardService;
    private final AuthService authService;

    // Cabeçalho
    @FXML private Label lblSaudacao;
    @FXML private Label lblData;
    @FXML private Label lblBadgePerfil;

    // Cards — apenas os dinâmicos (os demais são placeholders estáticos no FXML)
    @FXML private Label lblVendasHoje;
    @FXML private Label lblSubVendasHoje;
    @FXML private Label lblValorAPagarHoje;
    @FXML private Label lblQtdAPagarHoje;

    // Gráfico
    @FXML private BarChart<String, Number> chartCompras;

    // Painéis construídos programaticamente
    @FXML private VBox alertasPane;
    @FXML private VBox topProdutosPane;

    private Timeline autoRefresh;

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DIA_SEMANA =
            DateTimeFormatter.ofPattern("EEE", new Locale("pt", "BR"));
    private static final DateTimeFormatter DATA_EXTENSO =
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chartCompras.setLegendVisible(false);
        chartCompras.setAnimated(false);

        preencherCabecalho();
        carregarDados();

        autoRefresh = new Timeline(new KeyFrame(Duration.minutes(5), e -> carregarDados()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    public void parar() {
        if (autoRefresh != null) autoRefresh.stop();
    }

    // -----------------------------------------------------------------------
    // Cabeçalho
    // -----------------------------------------------------------------------

    private void preencherCabecalho() {
        var usuario = authService.getUsuarioLogado();
        int hora = LocalTime.now().getHour();

        String saudacao = hora < 12 ? "Bom dia" : hora < 18 ? "Boa tarde" : "Boa noite";
        String emoji    = hora < 12 ? " 👋" : hora < 18 ? " ☀️" : " 🌙";
        String primeiro = usuario != null && usuario.getNome() != null
                ? usuario.getNome().trim().split("\\s+")[0] : "Usuário";

        lblSaudacao.setText(saudacao + ", " + primeiro + emoji);
        lblData.setText(capitalize(LocalDate.now().format(DATA_EXTENSO)));

        String perfil = (usuario != null && usuario.getPerfil() != null)
                ? usuario.getPerfil().getNome() : "";
        lblBadgePerfil.setText(perfil);
        lblBadgePerfil.setVisible(!perfil.isEmpty());
        lblBadgePerfil.setManaged(!perfil.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Dados
    // -----------------------------------------------------------------------

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        try {
            DashboardAdminDTO dto = dashboardService.carregarAdmin(empresaId);
            Platform.runLater(() -> preencherUI(dto));
        } catch (Exception e) {
            log.error("Erro ao carregar dashboard admin", e);
        }
    }

    private void preencherUI(DashboardAdminDTO dto) {
        // Card VENDAS HOJE — placeholder (módulo não implementado)
        lblVendasHoje.setText("—");
        lblSubVendasHoje.setText("módulo em breve");

        // Card A PAGAR HOJE — dados reais
        lblValorAPagarHoje.setText(dto.contasPagarHoje() > 0
                ? CURRENCY.format(dto.valorContasPagarHoje()) : "R$ 0,00");
        lblQtdAPagarHoje.setText(dto.contasPagarHoje() == 0
                ? "nenhuma hoje"
                : dto.contasPagarHoje() + (dto.contasPagarHoje() == 1 ? " conta" : " contas"));

        // Gráfico
        renderizarGrafico(dto.comprasSemana());

        // Painéis
        montarAlertasPane(dto.alertas());
        montarTopProdutos(dto.topProdutos());
    }

    // -----------------------------------------------------------------------
    // Gráfico — barras verdes, "Hoje" em verde brilhante
    // -----------------------------------------------------------------------

    private void renderizarGrafico(List<VendaDiariaDTO> dados) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        LocalDate hoje = LocalDate.now();

        for (VendaDiariaDTO d : dados) {
            String label = d.data().equals(hoje)
                    ? "Hoje"
                    : capitalize(d.data().format(DIA_SEMANA));
            series.getData().add(new XYChart.Data<>(label, d.total()));
        }
        chartCompras.getData().setAll(series);

        // Aplica cores depois que o JavaFX cria os nós das barras
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    boolean isHoje = "Hoje".equals(d.getXValue());
                    d.getNode().setStyle(isHoje
                            ? "-fx-bar-fill: #22c55e; -fx-background-radius: 4 4 0 0;"
                            : "-fx-bar-fill: #15803d; -fx-background-radius: 4 4 0 0;");
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    // Painel Requer Atenção
    // -----------------------------------------------------------------------

    private void montarAlertasPane(List<AlertaDTO> alertas) {
        alertasPane.getChildren().clear();
        for (AlertaDTO a : alertas) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-alerta-row");

            Label dot = new Label("●");
            dot.getStyleClass().add("dash-dot-" + a.nivel().toLowerCase());

            Label texto = new Label(a.mensagem());
            texto.getStyleClass().add("dash-alerta-texto");
            texto.setWrapText(true);
            HBox.setHgrow(texto, Priority.ALWAYS);

            row.getChildren().addAll(dot, texto);

            if (a.valor() != null && !a.valor().isEmpty()) {
                Label val = new Label(a.valor());
                val.getStyleClass().add("dash-alerta-valor");
                row.getChildren().add(val);
            }
            alertasPane.getChildren().add(row);
        }
    }

    // -----------------------------------------------------------------------
    // Top 5 Produtos — nome com underline verde, igual ao mockup
    // -----------------------------------------------------------------------

    private void montarTopProdutos(List<TopProdutoDTO> produtos) {
        topProdutosPane.getChildren().clear();

        if (produtos.isEmpty()) {
            Label vazio = new Label("Nenhuma compra confirmada este mês.");
            vazio.getStyleClass().add("dash-empty-msg");
            topProdutosPane.getChildren().add(vazio);
            return;
        }

        for (int i = 0; i < produtos.size(); i++) {
            TopProdutoDTO p = produtos.get(i);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-top-row");

            // Número do ranking
            Label rank = new Label(String.valueOf(i + 1));
            rank.getStyleClass().add("dash-top-rank");
            rank.setMinWidth(22);
            rank.setAlignment(Pos.CENTER);

            // Nome + underline verde (igual ao mockup)
            VBox nomeBox = new VBox(3);
            HBox.setHgrow(nomeBox, Priority.ALWAYS);

            Label nome = new Label(p.nome());
            nome.getStyleClass().add("dash-top-nome");

            Region underline = new Region();
            underline.getStyleClass().add("dash-top-underline");
            underline.setPrefHeight(2);
            underline.setMaxWidth(Double.MAX_VALUE);

            nomeBox.getChildren().addAll(nome, underline);

            // Valor
            Label valor = new Label(CURRENCY.format(p.valor()));
            valor.getStyleClass().add("dash-top-valor");

            row.getChildren().addAll(rank, nomeBox, valor);
            topProdutosPane.getChildren().add(row);
        }
    }

    // -----------------------------------------------------------------------

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
