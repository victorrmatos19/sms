package com.erp.controller;

import com.erp.model.dto.dashboard.DashboardEstoqueDTO;
import com.erp.service.AuthService;
import com.erp.service.DashboardService;
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
public class DashboardEstoqueController implements Initializable {

    private final DashboardService dashboardService;
    private final AuthService authService;

    @FXML private Label lblTotalProdutos;
    @FXML private Label lblAbaixoMinimo;
    @FXML private Label lblEntradasMes;
    @FXML private Label lblSaidasMes;

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
            DashboardEstoqueDTO dto = dashboardService.carregarEstoque(empresaId);
            Platform.runLater(() -> preencherUI(dto));
        } catch (Exception e) {
            log.error("Erro ao carregar dashboard estoque", e);
        }
    }

    private void preencherUI(DashboardEstoqueDTO dto) {
        lblTotalProdutos.setText(String.valueOf(dto.totalProdutosAtivos()));
        lblAbaixoMinimo.setText(String.valueOf(dto.produtosAbaixoMinimo()));
        lblEntradasMes.setText(String.valueOf(dto.entradasMes()));
        lblSaidasMes.setText(String.valueOf(dto.saidasMes()));
    }
}
