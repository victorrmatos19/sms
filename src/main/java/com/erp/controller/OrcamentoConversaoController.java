package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Orcamento;
import com.erp.model.Venda;
import com.erp.service.OrcamentoService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrcamentoConversaoController implements Initializable {

    private final OrcamentoService orcamentoService;

    @FXML private Label         lblResumoOrcamento;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private Label         lblParcelasRow;
    @FXML private Spinner<Integer> spnParcelas;
    @FXML private TextArea      txtObservacoesVenda;
    @FXML private Button        btnConfirmarConversao;
    @FXML private Button        btnCancelar;
    @FXML private ProgressIndicator progressIndicator;

    private Orcamento orcamentoAtual;
    private Dialog<?> dialog;
    private Runnable onConversaoRealizada;

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFormaPagamento();
        configurarParcelas();
    }

    private void configurarFormaPagamento() {
        cmbFormaPagamento.setItems(FXCollections.observableArrayList(
                "DINHEIRO", "CARTAO_DEBITO", "CARTAO_CREDITO", "PIX", "PRAZO", "CREDIARIO"));
        cmbFormaPagamento.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String s) {
                if (s == null) return "";
                return switch (s) {
                    case "DINHEIRO"       -> "Dinheiro";
                    case "CARTAO_DEBITO"  -> "Cartão Débito";
                    case "CARTAO_CREDITO" -> "Cartão Crédito";
                    case "PIX"            -> "PIX";
                    case "PRAZO"          -> "A Prazo";
                    case "CREDIARIO"      -> "Crediário";
                    default -> s;
                };
            }
            @Override public String fromString(String s) { return s; }
        });
        cmbFormaPagamento.setValue("DINHEIRO");
        cmbFormaPagamento.valueProperty().addListener((obs, old, val) -> {
            boolean aPrazo = "PRAZO".equals(val) || "CREDIARIO".equals(val);
            lblParcelasRow.setVisible(aPrazo);
            lblParcelasRow.setManaged(aPrazo);
            spnParcelas.setVisible(aPrazo);
            spnParcelas.setManaged(aPrazo);
        });
    }

    private void configurarParcelas() {
        spnParcelas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1));
        spnParcelas.setVisible(false);
        spnParcelas.setManaged(false);
        lblParcelasRow.setVisible(false);
        lblParcelasRow.setManaged(false);
    }

    // ================================================================
    // API pública
    // ================================================================

    public void setOrcamento(Orcamento orcamento) {
        this.orcamentoAtual = orcamento;
        if (lblResumoOrcamento != null) {
            preencherResumo();
        }
    }

    public void setDialog(Dialog<?> dialog) {
        this.dialog = dialog;
    }

    public void setOnConversaoRealizada(Runnable callback) {
        this.onConversaoRealizada = callback;
    }

    private void preencherResumo() {
        if (orcamentoAtual == null) return;
        String cliente = orcamentoAtual.getCliente() != null
                ? orcamentoAtual.getCliente().getNome() : "(sem cliente)";
        String valor = orcamentoAtual.getValorTotal() != null
                ? CURRENCY_FMT.format(orcamentoAtual.getValorTotal()) : "R$ 0,00";
        lblResumoOrcamento.setText(
                "Orçamento: " + orcamentoAtual.getNumero()
                + "\nCliente: " + cliente
                + "\nValor Total: " + valor);
    }

    // ================================================================
    // Ações
    // ================================================================

    @FXML
    private void confirmarConversao() {
        if (orcamentoAtual == null) return;
        String forma = cmbFormaPagamento.getValue();
        if (forma == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecione a forma de pagamento.", ButtonType.OK).showAndWait();
            return;
        }
        int parcelas = spnParcelas.getValue() != null ? spnParcelas.getValue() : 1;
        String obs = txtObservacoesVenda.getText().isBlank() ? null : txtObservacoesVenda.getText();

        btnConfirmarConversao.setDisable(true);
        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
        }

        try {
            Venda venda = orcamentoService.converterEmVenda(
                    orcamentoAtual.getId(), forma, parcelas, obs);
            if (progressIndicator != null) progressIndicator.setVisible(false);
            new Alert(Alert.AlertType.INFORMATION,
                    "Venda gerada com sucesso!\nNúmero: " + venda.getNumero(),
                    ButtonType.OK).showAndWait();
            if (onConversaoRealizada != null) onConversaoRealizada.run();
        } catch (NegocioException e) {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            btnConfirmarConversao.setDisable(false);
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            btnConfirmarConversao.setDisable(false);
            log.error("Erro ao converter orçamento em venda", e);
            new Alert(Alert.AlertType.ERROR,
                    "Erro ao converter: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        if (dialog != null) dialog.close();
    }
}
