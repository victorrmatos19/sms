package com.erp.controller;

import com.erp.model.dto.dashboard.DashboardVendasDTO;
import com.erp.service.AppNavigationService;
import com.erp.service.AuthService;
import com.erp.service.DashboardService;
import com.erp.util.MoneyUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardVendasController implements Initializable {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final AppNavigationService appNavigationService;

    @FXML private Label lblVendasHoje;
    @FXML private Label lblValorVendasHoje;
    @FXML private Label lblOrcamentosMes;
    @FXML private Label lblValorOrcamentos;
    @FXML private Label lblOrcamentosAbertos;
    @FXML private Label lblOrcamentosConvertidos;
    @FXML private Label lblVendasMes;
    @FXML private Label lblValorVendas;
    @FXML private Label lblVendasTimeMes;
    @FXML private Label lblOrcamentosTimeMes;

    private Timeline autoRefresh;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarDados();
        autoRefresh = new Timeline(new KeyFrame(Duration.minutes(5), e -> carregarDados()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    public void parar() {
        if (autoRefresh != null) autoRefresh.stop();
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        try {
            DashboardVendasDTO dto = dashboardService.carregarVendas(empresaId, authService.getUsuarioLogado());
            Platform.runLater(() -> preencherUI(dto));
        } catch (Exception e) {
            log.error("Erro ao carregar dashboard vendas", e);
        }
    }

    private void preencherUI(DashboardVendasDTO dto) {
        lblVendasHoje.setText(String.valueOf(dto.minhasVendasHoje()));
        lblValorVendasHoje.setText(MoneyUtils.formatCurrency(dto.valorMinhasVendasHoje()));
        lblVendasMes.setText(String.valueOf(dto.minhasVendasMes()));
        lblValorVendas.setText(MoneyUtils.formatCurrency(dto.valorMinhasVendasMes()));
        lblOrcamentosAbertos.setText(String.valueOf(dto.meusOrcamentosAbertos()));
        lblOrcamentosMes.setText(String.valueOf(dto.meusOrcamentosMes()));
        lblValorOrcamentos.setText(MoneyUtils.formatCurrency(dto.valorMeusOrcamentosMes()));
        lblOrcamentosConvertidos.setText(dto.meusOrcamentosConvertidosMes() + " convertidos no mês");
        lblVendasTimeMes.setText(String.valueOf(dto.vendasTimeMes()));
        lblOrcamentosTimeMes.setText(String.valueOf(dto.orcamentosTimeMes()));
    }

    @FXML
    private void abrirVendas() {
        appNavigationService.navigateTo(AppNavigationService.Destination.VENDAS);
    }

    @FXML
    private void abrirOrcamentos() {
        appNavigationService.navigateTo(AppNavigationService.Destination.ORCAMENTOS);
    }
}
