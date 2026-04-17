package com.erp.controller;

import com.erp.StageManager;
import com.erp.model.dto.dashboard.AlertaDTO;
import com.erp.model.dto.dashboard.DashboardAdminDTO;
import com.erp.model.dto.dashboard.TopProdutoDTO;
import com.erp.model.dto.dashboard.VendaDiariaDTO;
import com.erp.model.dto.dashboard.VendaResumoDTO;
import com.erp.service.AuthService;
import com.erp.service.DashboardService;
import com.erp.util.MoneyUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
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
    private final StageManager stageManager;

    // Cabeçalho
    @FXML private Label lblSaudacao;
    @FXML private Label lblData;
    @FXML private Label lblBadgePerfil;

    // Cards de métrica
    @FXML private VBox cardVendasHoje;
    @FXML private VBox cardReceberHoje;
    @FXML private VBox cardPagarHoje;
    @FXML private VBox cardCaixa;
    @FXML private VBox cardTicketMedio;

    // Cards de seção
    @FXML private VBox cardGrafico;
    @FXML private VBox cardAlertas;
    @FXML private VBox cardTopProdutos;
    @FXML private VBox cardTopClientes;

    // Labels dinâmicos
    @FXML private Label lblVendasHoje;
    @FXML private Label lblSubVendasHoje;
    @FXML private Label lblValorAReceberHoje;
    @FXML private Label lblQtdAReceberHoje;
    @FXML private Label lblValorAPagarHoje;
    @FXML private Label lblQtdAPagarHoje;
    @FXML private Label lblCaixaAtual;
    @FXML private Label lblSubCaixaAtual;
    @FXML private Label lblTicketMedio;
    @FXML private Label lblSubTicketMedio;

    // Gráfico
    @FXML private BarChart<String, Number> chartCompras;

    // Painéis construídos programaticamente
    @FXML private VBox alertasPane;
    @FXML private VBox topProdutosPane;

    private Timeline autoRefresh;
    private DashboardAdminDTO dashboardAtual;

    private static final DateTimeFormatter DIA_SEMANA =
            DateTimeFormatter.ofPattern("EEE", new Locale("pt", "BR"));
    private static final DateTimeFormatter DATA_EXTENSO =
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chartCompras.setLegendVisible(false);
        chartCompras.setAnimated(false);

        preencherCabecalho();
        configurarCardVendasHoje();
        carregarDados();

        new Timeline(
                new KeyFrame(Duration.millis(200), e -> aplicarEstilosCards())
        ).play();

        stageManager.darkModeProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                aplicarEstilosCards();
                // se houver outros elementos com cor programática, atualizar aqui também
            });
        });

        autoRefresh = new Timeline(new KeyFrame(Duration.minutes(5), e -> carregarDados()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    private void aplicarEstilosCards() {
        boolean dark = stageManager.isDarkMode();
        String bgCard    = dark ? "#1e1e1e" : "#ffffff";
        String bgSection = dark ? "#1e1e1e" : "#ffffff";
        String styleCard    = "-fx-background-color: " + bgCard    + "; -fx-background-radius: 8; -fx-padding: 16 20 16 20;";
        String styleSection = "-fx-background-color: " + bgSection + "; -fx-background-radius: 8; -fx-padding: 20;";

        log.info("aplicarEstilosCards dark={} cards: cVH={} cRH={} cPH={} cC={} cTM={} cG={} cA={} cTP={} cTC={}",
            dark, cardVendasHoje, cardReceberHoje, cardPagarHoje, cardCaixa, cardTicketMedio,
            cardGrafico, cardAlertas, cardTopProdutos, cardTopClientes);

        for (VBox c : new VBox[]{cardVendasHoje, cardReceberHoje, cardPagarHoje, cardCaixa, cardTicketMedio}) {
            if (c != null) {
                c.getStyleClass().removeIf(s -> s.startsWith("dash-card"));
                c.setStyle(styleCard);
                log.info("  -> setStyle({}) em {}", styleCard, c.getId());
            }
        }
        for (VBox c : new VBox[]{cardGrafico, cardAlertas, cardTopProdutos, cardTopClientes}) {
            if (c != null) {
                c.getStyleClass().removeIf(s -> s.startsWith("dash-section"));
                c.setStyle(styleSection);
            }
        }
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

        String perfil = (usuario != null && usuario.getPerfilPrincipal() != null)
                ? usuario.getPerfilPrincipal().getNome() : "";
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
        dashboardAtual = dto;

        // Card VENDAS HOJE
        lblVendasHoje.setText(MoneyUtils.formatCurrency(dto.valorVendasHoje()));
        lblSubVendasHoje.setText(dto.vendasHoje() == 0
                ? "nenhuma venda hoje"
                : dto.vendasHoje() + (dto.vendasHoje() == 1 ? " venda hoje" : " vendas hoje"));

        // Card A RECEBER HOJE
        lblValorAReceberHoje.setText(MoneyUtils.formatCurrency(dto.valorContasReceberHoje()));
        lblQtdAReceberHoje.setText(dto.contasReceberHoje() == 0
                ? "nenhuma hoje"
                : dto.contasReceberHoje() + (dto.contasReceberHoje() == 1 ? " conta" : " contas"));

        // Card A PAGAR HOJE
        lblValorAPagarHoje.setText(MoneyUtils.formatCurrency(dto.valorContasPagarHoje()));
        lblQtdAPagarHoje.setText(dto.contasPagarHoje() == 0
                ? "nenhuma hoje"
                : dto.contasPagarHoje() + (dto.contasPagarHoje() == 1 ? " conta" : " contas"));

        // Card CAIXA ATUAL
        lblCaixaAtual.setText(MoneyUtils.formatCurrency(dto.saldoCaixaAtual()));
        lblSubCaixaAtual.setText(dto.caixaAberto()
                ? MoneyUtils.formatCurrency(dto.entradasCaixaAtual()) + " entradas / "
                    + MoneyUtils.formatCurrency(dto.saidasCaixaAtual()) + " saídas"
                : "caixa fechado");

        lblTicketMedio.setText(MoneyUtils.formatCurrency(dto.ticketMedioUltimos7Dias()));
        lblSubTicketMedio.setText("Últimos 7 dias");

        // Gráfico
        renderizarGrafico(dto.vendasSemana());

        // Painéis
        montarAlertasPane(dto.alertas());
        montarTopProdutos(dto.topProdutos());
    }

    private void configurarCardVendasHoje() {
        cardVendasHoje.setCursor(Cursor.HAND);
        Tooltip.install(cardVendasHoje, new Tooltip("Ver vendas finalizadas hoje"));
        cardVendasHoje.setOnMouseClicked(e -> abrirModalVendasHoje());
    }

    @FXML
    private void abrirModalVendasHoje() {
        List<VendaResumoDTO> vendas = dashboardAtual != null
                ? dashboardAtual.vendasHojeDetalhes()
                : List.of();
        long quantidade = dashboardAtual != null ? dashboardAtual.vendasHoje() : 0L;
        String valorTotal = dashboardAtual != null
                ? MoneyUtils.formatCurrency(dashboardAtual.valorVendasHoje())
                : "R$ 0,00";

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Vendas Hoje");
        dialog.setHeaderText(null);
        if (cardVendasHoje.getScene() != null && cardVendasHoje.getScene().getWindow() != null) {
            dialog.initOwner(cardVendasHoje.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(cardVendasHoje.getScene().getStylesheets());
        }
        dialog.getDialogPane().getStyleClass().add(stageManager.isDarkMode() ? "theme-dark" : "theme-light");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(760);

        Label titulo = new Label("Vendas finalizadas hoje");
        titulo.getStyleClass().add("dash-section-title");

        Label resumo = new Label(valorTotal + " em " + quantidade
                + (quantidade == 1 ? " venda" : " vendas"));
        resumo.getStyleClass().add("dash-val-green");

        TableView<VendaResumoDTO> tabela = criarTabelaVendasHoje(vendas);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        content.getChildren().addAll(titulo, resumo, tabela);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private TableView<VendaResumoDTO> criarTabelaVendasHoje(List<VendaResumoDTO> vendas) {
        TableView<VendaResumoDTO> tabela = new TableView<>();
        tabela.setPrefHeight(360);
        tabela.setPlaceholder(new Label("Nenhuma venda finalizada hoje."));
        tabela.setItems(FXCollections.observableArrayList(vendas));

        TableColumn<VendaResumoDTO, String> colHora = new TableColumn<>("HORA");
        colHora.setPrefWidth(70);
        colHora.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().dataVenda() != null ? c.getValue().dataVenda().format(HORA) : "—"));

        TableColumn<VendaResumoDTO, String> colNumero = new TableColumn<>("NÚMERO");
        colNumero.setPrefWidth(115);
        colNumero.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().numero())));

        TableColumn<VendaResumoDTO, String> colCliente = new TableColumn<>("CLIENTE");
        colCliente.setPrefWidth(185);
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().cliente())));

        TableColumn<VendaResumoDTO, String> colVendedor = new TableColumn<>("VENDEDOR");
        colVendedor.setPrefWidth(150);
        colVendedor.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().vendedor())));

        TableColumn<VendaResumoDTO, String> colForma = new TableColumn<>("FORMA");
        colForma.setPrefWidth(120);
        colForma.setCellValueFactory(c -> new SimpleStringProperty(
                formatarFormaPagamento(c.getValue().formaPagamento())));

        TableColumn<VendaResumoDTO, String> colValor = new TableColumn<>("VALOR");
        colValor.setPrefWidth(110);
        colValor.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyUtils.formatCurrency(c.getValue().valorTotal())));

        tabela.getColumns().setAll(colHora, colNumero, colCliente, colVendedor, colForma, colValor);
        return tabela;
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
            Label valor = new Label(MoneyUtils.formatCurrency(p.valor()));
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

    private String formatarFormaPagamento(String forma) {
        if (forma == null || forma.isBlank()) return "—";
        return switch (forma) {
            case "DINHEIRO" -> "Dinheiro";
            case "PIX" -> "PIX";
            case "CARTAO_CREDITO" -> "Cartão Crédito";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            case "PRAZO" -> "Prazo";
            case "CREDIARIO" -> "Crediário";
            default -> forma;
        };
    }

    private String nvl(String valor) {
        return valor != null && !valor.isBlank() ? valor : "—";
    }
}
