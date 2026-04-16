package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.CaixaMovimentacao;
import com.erp.model.dto.caixa.CaixaResumoDTO;
import com.erp.service.AuthService;
import com.erp.service.CaixaService;
import com.erp.util.MoneyUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaixaController implements Initializable {

    private final CaixaService caixaService;
    private final AuthService authService;

    @FXML private Label lblStatus;
    @FXML private Label lblOperador;
    @FXML private Label lblAbertura;
    @FXML private Label lblSaldoAtual;
    @FXML private Label lblEntradas;
    @FXML private Label lblSaidas;

    @FXML private TextField txtSaldoInicial;
    @FXML private Button btnAbrir;

    @FXML private ComboBox<String> cmbTipoMovimentacao;
    @FXML private TextField txtValorMovimentacao;
    @FXML private TextField txtDescricaoMovimentacao;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private Button btnRegistrarMovimentacao;

    @FXML private TextField txtSaldoFinal;
    @FXML private TextArea txtObservacoesFechamento;
    @FXML private Button btnFechar;

    @FXML private TableView<CaixaMovimentacao> tblMovimentacoes;
    @FXML private TableColumn<CaixaMovimentacao, String> colDataHora;
    @FXML private TableColumn<CaixaMovimentacao, String> colTipo;
    @FXML private TableColumn<CaixaMovimentacao, String> colDescricao;
    @FXML private TableColumn<CaixaMovimentacao, String> colForma;
    @FXML private TableColumn<CaixaMovimentacao, String> colValor;
    @FXML private TableColumn<CaixaMovimentacao, String> colOrigem;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarCombos();
        configurarTabela();
        carregarDados();
    }

    private void configurarCombos() {
        cmbTipoMovimentacao.setItems(FXCollections.observableArrayList("SUPRIMENTO", "SANGRIA"));
        cmbTipoMovimentacao.setValue("SUPRIMENTO");
        cmbFormaPagamento.setItems(FXCollections.observableArrayList(
                "DINHEIRO", "PIX", "CARTAO_DEBITO", "CARTAO_CREDITO", "OUTRO"));
        cmbFormaPagamento.setValue("DINHEIRO");
    }

    private void configurarTabela() {
        colDataHora.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCriadoEm() != null ? c.getValue().getCriadoEm().format(DATE_TIME_FMT) : "—"));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(formatarTipo(c.getValue().getTipo())));
        colDescricao.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDescricao())));
        colForma.setCellValueFactory(c -> new SimpleStringProperty(formatarForma(c.getValue().getFormaPagamento())));
        colValor.setCellValueFactory(c -> new SimpleStringProperty(MoneyUtils.formatCurrency(c.getValue().getValor())));
        colOrigem.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getOrigem())));
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        CaixaResumoDTO resumo = caixaService.obterResumoAtual(empresaId);
        preencherResumo(resumo);
        tblMovimentacoes.setItems(FXCollections.observableArrayList(
                caixaService.listarMovimentacoesAtuais(empresaId)));
    }

    private void preencherResumo(CaixaResumoDTO resumo) {
        boolean aberto = resumo.aberto();
        lblStatus.setText(aberto ? "ABERTO" : "FECHADO");
        lblStatus.getStyleClass().removeAll("badge-success", "badge-neutral");
        lblStatus.getStyleClass().add(aberto ? "badge-success" : "badge-neutral");
        lblOperador.setText(aberto ? resumo.operadorNome() : "—");
        lblAbertura.setText(resumo.dataAbertura() != null ? resumo.dataAbertura().format(DATE_TIME_FMT) : "—");
        lblSaldoAtual.setText(MoneyUtils.formatCurrency(resumo.saldoAtual()));
        lblEntradas.setText(MoneyUtils.formatCurrency(resumo.totalEntradas()));
        lblSaidas.setText(MoneyUtils.formatCurrency(resumo.totalSaidas()));
        txtSaldoFinal.setText(MoneyUtils.formatInput(resumo.saldoAtual()));

        btnAbrir.setDisable(aberto);
        txtSaldoInicial.setDisable(aberto);
        btnRegistrarMovimentacao.setDisable(!aberto);
        cmbTipoMovimentacao.setDisable(!aberto);
        cmbFormaPagamento.setDisable(!aberto);
        txtValorMovimentacao.setDisable(!aberto);
        txtDescricaoMovimentacao.setDisable(!aberto);
        btnFechar.setDisable(!aberto);
        txtSaldoFinal.setDisable(!aberto);
        txtObservacoesFechamento.setDisable(!aberto);
    }

    @FXML
    private void abrirCaixa() {
        try {
            caixaService.abrirCaixa(authService.getEmpresaIdLogado(),
                    MoneyUtils.parse(txtSaldoInicial.getText()), null);
            txtSaldoInicial.setText("0,00");
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao abrir caixa", e);
            mostrarErro("Erro ao abrir caixa: " + e.getMessage());
        }
    }

    @FXML
    private void registrarMovimentacao() {
        try {
            caixaService.registrarMovimentacaoManual(authService.getEmpresaIdLogado(),
                    cmbTipoMovimentacao.getValue(),
                    MoneyUtils.parse(txtValorMovimentacao.getText()),
                    txtDescricaoMovimentacao.getText(),
                    cmbFormaPagamento.getValue());
            txtValorMovimentacao.clear();
            txtDescricaoMovimentacao.clear();
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao registrar movimentação de caixa", e);
            mostrarErro("Erro ao registrar movimentação: " + e.getMessage());
        }
    }

    @FXML
    private void fecharCaixa() {
        try {
            caixaService.fecharCaixa(authService.getEmpresaIdLogado(),
                    MoneyUtils.parse(txtSaldoFinal.getText()),
                    txtObservacoesFechamento.getText());
            txtObservacoesFechamento.clear();
            carregarDados();
        } catch (NegocioException e) {
            mostrarAviso(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao fechar caixa", e);
            mostrarErro("Erro ao fechar caixa: " + e.getMessage());
        }
    }

    private void mostrarAviso(String mensagem) {
        new Alert(Alert.AlertType.WARNING, mensagem, ButtonType.OK).showAndWait();
    }

    private void mostrarErro(String mensagem) {
        new Alert(Alert.AlertType.ERROR, mensagem, ButtonType.OK).showAndWait();
    }

    private String formatarTipo(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo) {
            case "SUPRIMENTO" -> "Suprimento";
            case "SANGRIA" -> "Sangria";
            case "VENDA" -> "Venda";
            case "RECEBIMENTO" -> "Recebimento";
            case "PAGAMENTO" -> "Pagamento";
            default -> tipo;
        };
    }

    private String formatarForma(String forma) {
        if (forma == null || forma.isBlank()) return "—";
        return switch (forma) {
            case "DINHEIRO" -> "Dinheiro";
            case "PIX" -> "PIX";
            case "CARTAO_CREDITO" -> "Cartão Crédito";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            default -> forma;
        };
    }

    private String nvl(String valor) {
        return valor != null && !valor.isBlank() ? valor : "—";
    }
}
