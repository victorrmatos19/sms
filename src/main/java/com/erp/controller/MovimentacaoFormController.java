package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Produto;
import com.erp.repository.ProdutoRepository;
import com.erp.service.AuthService;
import com.erp.service.EstoqueService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovimentacaoFormController implements Initializable {

    private final EstoqueService estoqueService;
    private final ProdutoRepository produtoRepository;
    private final AuthService authService;

    @FXML private ComboBox<Produto> cmbProduto;
    @FXML private ComboBox<String>  cmbTipoMovimentacao;
    @FXML private Label             lblSaldoAtual;
    @FXML private Label             lblQuantidadeLabel;
    @FXML private TextField         txtQuantidade;
    @FXML private Label             lblLote;
    @FXML private TextField         txtLote;
    @FXML private Label             lblValidade;
    @FXML private DatePicker        dpValidade;
    @FXML private TextField         txtMotivo;
    @FXML private Label             errMotivo;
    @FXML private Button            btnRegistrar;

    private List<Produto> todosProdutos = new ArrayList<>();
    private ObservableList<Produto> produtosDisplay;
    private boolean filtrandoProduto = false;
    private Runnable onSucessoCallback;

    private static final NumberFormat QTD_FMT = NumberFormat.getNumberInstance(new Locale("pt", "BR"));

    static {
        QTD_FMT.setMaximumFractionDigits(4);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosProdutos = produtoRepository.findByEmpresaIdAndAtivoTrue(empresaId);

        configurarProdutoCombo();
        configurarTipoCombo();
    }

    public void setOnSucessoCallback(Runnable callback) {
        this.onSucessoCallback = callback;
    }

    private void configurarProdutoCombo() {
        produtosDisplay = FXCollections.observableArrayList(todosProdutos);
        cmbProduto.setItems(produtosDisplay);
        cmbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Produto p) {
                if (p == null) return "";
                String cod = p.getCodigoInterno() != null ? "[" + p.getCodigoInterno() + "] " : "";
                return cod + p.getDescricao();
            }
            /**
             * Chamado pelo JavaFX ao fazer commitValue() quando o editor perde o foco.
             * Retornar null aqui fazia setValue(null) → seleção sumia ao clicar fora.
             * A correção: buscar o produto cujo nome display coincide com o texto;
             * se nenhum coincidir, preservar o valor já selecionado.
             */
            @Override public Produto fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return todosProdutos.stream()
                        .filter(p -> {
                            String cod = p.getCodigoInterno() != null
                                    ? "[" + p.getCodigoInterno() + "] " : "";
                            String display = cod + p.getDescricao();
                            return s.equals(display);
                        })
                        .findFirst()
                        .orElse(cmbProduto.getValue()); // mantém seleção se texto parcial
            }
        });
        cmbProduto.setEditable(true);
        cmbProduto.getEditor().textProperty().addListener((obs, old, val) ->
                Platform.runLater(() -> filtrarProdutos(val)));
        cmbProduto.valueProperty().addListener((obs, old, prod) -> {
            if (prod != null) atualizarSaldo(prod);
        });
    }

    private void filtrarProdutos(String texto) {
        if (filtrandoProduto || produtosDisplay == null) return;

        // Guard: se o texto é exatamente o nome display do produto já selecionado,
        // ele veio do próprio setValue() → converter.toString(), não da digitação.
        // Chamar setAll() aqui emitiria REPLACED → setValue(null) silencioso.
        Produto selecionado = cmbProduto.getValue();
        if (selecionado != null && texto != null) {
            String cod = selecionado.getCodigoInterno() != null
                    ? "[" + selecionado.getCodigoInterno() + "] " : "";
            String nomeDisplay = cod + selecionado.getDescricao();
            if (texto.equals(nomeDisplay)) return;
        }

        filtrandoProduto = true;
        try {
            if (texto == null || texto.isBlank()) {
                produtosDisplay.setAll(todosProdutos);
                return;
            }
            String lower = texto.toLowerCase();
            List<Produto> filtrados = todosProdutos.stream()
                    .filter(p -> p.getDescricao().toLowerCase().contains(lower)
                            || (p.getCodigoInterno() != null && p.getCodigoInterno().toLowerCase().contains(lower)))
                    .toList();
            produtosDisplay.setAll(filtrados);
        } finally {
            filtrandoProduto = false;
        }
    }

    private void atualizarSaldo(Produto produto) {
        BigDecimal saldo = produto.getEstoqueAtual() != null ? produto.getEstoqueAtual() : BigDecimal.ZERO;
        String unidade   = produto.getUnidade() != null ? " " + produto.getUnidade().getSigla() : "";
        lblSaldoAtual.setText(QTD_FMT.format(saldo) + unidade);

        boolean usaLote = Boolean.TRUE.equals(produto.getUsaLoteValidade());
        lblLote.setVisible(usaLote);    lblLote.setManaged(usaLote);
        txtLote.setVisible(usaLote);    txtLote.setManaged(usaLote);
        lblValidade.setVisible(usaLote); lblValidade.setManaged(usaLote);
        dpValidade.setVisible(usaLote);  dpValidade.setManaged(usaLote);
    }

    private void configurarTipoCombo() {
        cmbTipoMovimentacao.setItems(FXCollections.observableArrayList(
                "Entrada Manual", "Saída Manual", "Ajuste de Inventário"));
        cmbTipoMovimentacao.setValue("Entrada Manual");
        cmbTipoMovimentacao.valueProperty().addListener((obs, old, val) -> {
            if ("Ajuste de Inventário".equals(val)) {
                lblQuantidadeLabel.setText("Quantidade real em estoque *");
            } else {
                lblQuantidadeLabel.setText("Quantidade *");
            }
        });
    }

    @FXML
    private void registrar() {
        errMotivo.setVisible(false);
        errMotivo.setManaged(false);

        // Motivo validado primeiro — exibe label inline sem Alert
        if (txtMotivo.getText() == null || txtMotivo.getText().isBlank()) {
            errMotivo.setText("Motivo é obrigatório.");
            errMotivo.setVisible(true);
            errMotivo.setManaged(true);
            return;
        }

        Produto produto = cmbProduto.getValue();
        if (produto == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione um produto.", ButtonType.OK).showAndWait();
            return;
        }

        BigDecimal quantidade;
        try {
            quantidade = new BigDecimal(txtQuantidade.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Quantidade inválida.", ButtonType.OK).showAndWait();
            return;
        }

        Integer empresaId = authService.getEmpresaIdLogado();
        String tipo = cmbTipoMovimentacao.getValue();
        String motivo = txtMotivo.getText().trim();

        try {
            switch (tipo) {
                case "Entrada Manual" -> {
                    if (Boolean.TRUE.equals(produto.getUsaLoteValidade()) && !txtLote.getText().isBlank()) {
                        estoqueService.registrarEntradaComLote(empresaId, produto.getId(),
                                txtLote.getText().trim(), dpValidade.getValue(), quantidade, motivo);
                    } else {
                        estoqueService.registrarEntrada(empresaId, produto.getId(), quantidade, motivo);
                    }
                }
                case "Saída Manual" ->
                        estoqueService.registrarSaida(empresaId, produto.getId(), quantidade, motivo);
                case "Ajuste de Inventário" ->
                        estoqueService.ajustarInventario(empresaId, produto.getId(), quantidade, motivo);
            }
            if (onSucessoCallback != null) onSucessoCallback.run();
            fechar();
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao registrar movimentação", e);
            new Alert(Alert.AlertType.ERROR, "Erro ao registrar: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelar() { fechar(); }

    private void fechar() {
        ((Stage) btnRegistrar.getScene().getWindow()).close();
    }
}
