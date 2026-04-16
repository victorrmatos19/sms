package com.erp.controller;

import com.erp.model.dto.dashboard.DashboardFinanceiroDTO;
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
public class DashboardFinanceiroController implements Initializable {

    private final DashboardService dashboardService;
    private final AuthService authService;

    @FXML private Label lblContasAbertas;
    @FXML private Label lblTotalAberto;
    @FXML private Label lblContasVencidas;
    @FXML private Label lblTotalVencido;
    @FXML private Label lblComprasMes;
    @FXML private Label lblValorComprasMes;

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
            DashboardFinanceiroDTO dto = dashboardService.carregarFinanceiro(empresaId);
            Platform.runLater(() -> preencherUI(dto));
        } catch (Exception e) {
            log.error("Erro ao carregar dashboard financeiro", e);
        }
    }

    private void preencherUI(DashboardFinanceiroDTO dto) {
        lblContasAbertas.setText(String.valueOf(dto.contasPagarAbertas()));
        lblTotalAberto.setText(MoneyUtils.formatCurrency(dto.totalContasPagarAbertas()));
        lblContasVencidas.setText(String.valueOf(dto.contasPagarVencidas()));
        lblTotalVencido.setText(MoneyUtils.formatCurrency(dto.totalContasPagarVencidas()));
        lblComprasMes.setText(String.valueOf(dto.comprasMes()));
        lblValorComprasMes.setText(MoneyUtils.formatCurrency(dto.valorComprasMes()));
    }
}
