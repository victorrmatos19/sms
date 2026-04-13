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
import javafx.scene.layout.VBox;
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
import java.util.Optional;
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
    @FXML private Label             errProduto;
    @FXML private Label             errQuantidade;
    @FXML private Label             lblCustoUnitario;
    @FXML private VBox              vboxCusto;
    @FXML private TextField         txtCustoUnitario;
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
        boolean isEntrada = "Entrada Manual".equals(cmbTipoMovimentacao.getValue());

        lblLote.setVisible(usaLote && isEntrada);    lblLote.setManaged(usaLote && isEntrada);
        txtLote.setVisible(usaLote && isEntrada);    txtLote.setManaged(usaLote && isEntrada);
        lblValidade.setVisible(usaLote && isEntrada); lblValidade.setManaged(usaLote && isEntrada);
        dpValidade.setVisible(usaLote && isEntrada);  dpValidade.setManaged(usaLote && isEntrada);
    }

    private void configurarTipoCombo() {
        cmbTipoMovimentacao.setItems(FXCollections.observableArrayList(
                "Entrada Manual", "Saída Manual", "Ajuste de Inventário"));
        cmbTipoMovimentacao.setValue("Entrada Manual");
        atualizarVisibilidadePorTipo("Entrada Manual");
        cmbTipoMovimentacao.valueProperty().addListener((obs, old, val) -> {
            atualizarVisibilidadePorTipo(val);
            // Re-avalia lote se produto selecionado
            Produto prod = cmbProduto.getValue();
            if (prod != null) atualizarSaldo(prod);
        });
    }

    private void atualizarVisibilidadePorTipo(String tipo) {
        if (tipo == null) return;
        boolean isAjuste  = "Ajuste de Inventário".equals(tipo);
        boolean isEntrada = "Entrada Manual".equals(tipo);

        lblQuantidadeLabel.setText(isAjuste ? "Quantidade real em estoque *" : "Quantidade *");

        // Custo Unitário apenas para entrada
        lblCustoUnitario.setVisible(isEntrada);
        lblCustoUnitario.setManaged(isEntrada);
        vboxCusto.setVisible(isEntrada);
        vboxCusto.setManaged(isEntrada);

        // Lote/Validade apenas para entrada (também dependem do produto)
        Produto prod = cmbProduto.getValue();
        boolean usaLote = prod != null && Boolean.TRUE.equals(prod.getUsaLoteValidade());
        lblLote.setVisible(usaLote && isEntrada);    lblLote.setManaged(usaLote && isEntrada);
        txtLote.setVisible(usaLote && isEntrada);    txtLote.setManaged(usaLote && isEntrada);
        lblValidade.setVisible(usaLote && isEntrada); lblValidade.setManaged(usaLote && isEntrada);
        dpValidade.setVisible(usaLote && isEntrada);  dpValidade.setManaged(usaLote && isEntrada);
    }

    @FXML
    private void registrar() {
        // Limpa erros
        esconderErro(errProduto);
        esconderErro(errQuantidade);
        esconderErro(errMotivo);

        boolean valido = true;

        Produto produto = cmbProduto.getValue();
        if (produto == null) {
            mostrarErro(errProduto, "Selecione um produto.");
            valido = false;
        }

        BigDecimal quantidade = null;
        try {
            String qtdText = txtQuantidade.getText() == null ? "" : txtQuantidade.getText().trim().replace(",", ".");
            if (qtdText.isEmpty()) throw new NumberFormatException("vazio");
            quantidade = new BigDecimal(qtdText);
            if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarErro(errQuantidade, "Quantidade deve ser maior que zero.");
                valido = false;
            }
        } catch (NumberFormatException e) {
            mostrarErro(errQuantidade, "Quantidade inválida.");
            valido = false;
        }

        if (txtMotivo.getText() == null || txtMotivo.getText().isBlank()) {
            mostrarErro(errMotivo, "Motivo é obrigatório.");
            valido = false;
        }

        if (!valido) return;

        // Confirmação para saída quando pode resultar em estoque negativo
        if ("Saída Manual".equals(cmbTipoMovimentacao.getValue()) && produto != null && quantidade != null) {
            BigDecimal saldo = produto.getEstoqueAtual() != null ? produto.getEstoqueAtual() : BigDecimal.ZERO;
            if (saldo.compareTo(quantidade) < 0) {
                Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION,
                        String.format("O saldo atual é %s e a saída de %s deixará o estoque negativo. Deseja continuar?",
                                QTD_FMT.format(saldo), QTD_FMT.format(quantidade)),
                        ButtonType.YES, ButtonType.NO);
                confirmacao.setHeaderText("Estoque insuficiente");
                Optional<ButtonType> resp = confirmacao.showAndWait();
                if (resp.isEmpty() || resp.get() != ButtonType.YES) return;
            }
        }

        Integer empresaId = authService.getEmpresaIdLogado();
        String tipo = cmbTipoMovimentacao.getValue();
        String motivo = txtMotivo.getText().trim();
        final BigDecimal qtdFinal = quantidade;

        try {
            switch (tipo) {
                case "Entrada Manual" -> {
                    if (Boolean.TRUE.equals(produto.getUsaLoteValidade()) && !txtLote.getText().isBlank()) {
                        estoqueService.registrarEntradaComLote(empresaId, produto.getId(),
                                txtLote.getText().trim(), dpValidade.getValue(), qtdFinal, motivo);
                    } else {
                        estoqueService.registrarEntrada(empresaId, produto.getId(), qtdFinal, motivo);
                    }
                }
                case "Saída Manual" ->
                        estoqueService.registrarSaidaPermitindoNegativo(empresaId, produto.getId(), qtdFinal, motivo);
                case "Ajuste de Inventário" ->
                        estoqueService.ajustarInventario(empresaId, produto.getId(), qtdFinal, motivo);
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

    private void mostrarErro(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void esconderErro(Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false);
    }
}
