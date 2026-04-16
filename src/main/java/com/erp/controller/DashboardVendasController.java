package com.erp.controller;

import com.erp.model.dto.dashboard.DashboardVendasDTO;
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

    @FXML private Label lblOrcamentosMes;
    @FXML private Label lblValorOrcamentos;
    @FXML private Label lblVendasMes;
    @FXML private Label lblValorVendas;
    @FXML private Label lblComprasMes;
    @FXML private Label lblValorCompras;

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
            DashboardVendasDTO dto = dashboardService.carregarVendas(empresaId);
            Platform.runLater(() -> preencherUI(dto));
        } catch (Exception e) {
            log.error("Erro ao carregar dashboard vendas", e);
        }
    }

    private void preencherUI(DashboardVendasDTO dto) {
        lblOrcamentosMes.setText(String.valueOf(dto.orcamentosMes()));
        lblValorOrcamentos.setText(MoneyUtils.formatCurrency(dto.valorOrcamentosMes()));
        lblVendasMes.setText(String.valueOf(dto.vendasMes()));
        lblValorVendas.setText(MoneyUtils.formatCurrency(dto.valorVendasMes()));
        lblComprasMes.setText(String.valueOf(dto.comprasMes()));
        lblValorCompras.setText(MoneyUtils.formatCurrency(dto.valorComprasMes()));
    }
}
