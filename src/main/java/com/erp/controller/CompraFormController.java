package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.FornecedorRepository;
import com.erp.repository.LoteRepository;
import com.erp.repository.ProdutoRepository;
import com.erp.service.AuthService;
import com.erp.service.CompraService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompraFormController implements Initializable {

    private final CompraService compraService;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoRepository produtoRepository;
    private final LoteRepository loteRepository;
    private final AuthService authService;
    private final ApplicationContext springContext;

    // ---- Header ----
    @FXML private Label lblTitulo;
    @FXML private Label lblBannerStatus;

    // ---- Cabeçalho da compra ----
    @FXML private ComboBox<Fornecedor>  cmbFornecedor;
    @FXML private TextField             txtNumeroDoc;
    @FXML private DatePicker            dpEmissao;
    @FXML private DatePicker            dpPrevisao;
    @FXML private ComboBox<String>      cmbCondicao;
    @FXML private Spinner<Integer>      spnParcelas;
    @FXML private Label                 lblParcelasRow;
    @FXML private TextArea              txtObservacoes;

    // ---- Itens ----
    @FXML private VBox                  vboxItens;

    // ---- Totais ----
    @FXML private Label                 lblValorProdutos;
    @FXML private TextField             txtFrete;
    @FXML private TextField             txtDescontoGlobal;
    @FXML private TextField             txtOutrasDespesas;
    @FXML private Label                 lblValorTotal;

    // ---- Botões ----
    @FXML private HBox                  hboxBotoes;
    @FXML private Button                btnSalvarRascunho;
    @FXML private Button                btnConfirmar;

    // ---- Estado ----
    private Compra compraAtual;
    private StackPane conteudoPane;
    private List<Fornecedor> todosFornecedores = new ArrayList<>();
    private List<Produto> todosProdutos = new ArrayList<>();
    private final List<ItemRow> itemRows = new ArrayList<>();
    private boolean somenteLeitura = false;

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // ================================================================
    // Inicialização
    // ================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarDados();
        configurarFornecedorCombo();
        configurarCondicaoCombo();
        configurarParcelas();
        configurarTotaisListeners();
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosFornecedores = fornecedorRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        todosProdutos = produtoRepository.findByEmpresaIdAndAtivoTrue(empresaId);
    }

    private void configurarFornecedorCombo() {
        cmbFornecedor.setItems(FXCollections.observableArrayList(todosFornecedores));
        cmbFornecedor.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Fornecedor f) {
                if (f == null) return "";
                return "PJ".equals(f.getTipoPessoa()) && f.getRazaoSocial() != null
                        ? f.getRazaoSocial() : f.getNome();
            }
            @Override public Fornecedor fromString(String s) { return null; }
        });
        cmbFornecedor.setEditable(true);
        cmbFornecedor.getEditor().textProperty().addListener((obs, old, val) -> filtrarFornecedores(val));
    }

    private void filtrarFornecedores(String texto) {
        if (texto == null || texto.isBlank()) {
            cmbFornecedor.setItems(FXCollections.observableArrayList(todosFornecedores));
            return;
        }
        String lower = texto.toLowerCase();
        var filtrados = todosFornecedores.stream()
                .filter(f -> {
                    String nome = "PJ".equals(f.getTipoPessoa()) && f.getRazaoSocial() != null
                            ? f.getRazaoSocial() : f.getNome();
                    return nome != null && nome.toLowerCase().contains(lower);
                })
                .collect(Collectors.toList());
        cmbFornecedor.setItems(FXCollections.observableArrayList(filtrados));
        if (!filtrados.isEmpty() && !cmbFornecedor.isShowing()) cmbFornecedor.show();
    }

    private void configurarCondicaoCombo() {
        cmbCondicao.setItems(FXCollections.observableArrayList(
                "A_VISTA", "30_DIAS", "60_DIAS", "90_DIAS",
                "30_60_DIAS", "30_60_90_DIAS", "PERSONALIZADO"));
        cmbCondicao.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String s) {
                if (s == null) return "";
                return switch (s) {
                    case "A_VISTA"       -> "À Vista";
                    case "30_DIAS"       -> "30 Dias";
                    case "60_DIAS"       -> "60 Dias";
                    case "90_DIAS"       -> "90 Dias";
                    case "30_60_DIAS"    -> "30/60 Dias";
                    case "30_60_90_DIAS" -> "30/60/90 Dias";
                    case "PERSONALIZADO" -> "Personalizado";
                    default -> s;
                };
            }
            @Override public String fromString(String s) { return s; }
        });
        cmbCondicao.setValue("A_VISTA");
        cmbCondicao.valueProperty().addListener((obs, old, val) -> {
            boolean mostraParcelas = val != null && !val.equals("A_VISTA");
            lblParcelasRow.setVisible(mostraParcelas);
            lblParcelasRow.setManaged(mostraParcelas);
            spnParcelas.setVisible(mostraParcelas);
            spnParcelas.setManaged(mostraParcelas);
        });
    }

    private void configurarParcelas() {
        spnParcelas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1));
        spnParcelas.setVisible(false);
        spnParcelas.setManaged(false);
        lblParcelasRow.setVisible(false);
        lblParcelasRow.setManaged(false);
    }

    private void configurarTotaisListeners() {
        txtFrete.textProperty().addListener((obs, old, val) -> recalcularTotal());
        txtDescontoGlobal.textProperty().addListener((obs, old, val) -> recalcularTotal());
        txtOutrasDespesas.textProperty().addListener((obs, old, val) -> recalcularTotal());
    }

    // ================================================================
    // API pública
    // ================================================================

    public void setCompra(Compra compra) {
        this.compraAtual = compra;
        Platform.runLater(this::preencherFormulario);
    }

    public void setConteudoPane(StackPane pane) {
        this.conteudoPane = pane;
    }

    // ================================================================
    // Preenchimento
    // ================================================================

    private void preencherFormulario() {
        if (compraAtual == null) {
            lblTitulo.setText("Nova Compra");
            dpEmissao.setValue(LocalDate.now());
            lblBannerStatus.setVisible(false);
            lblBannerStatus.setManaged(false);
            return;
        }

        lblTitulo.setText("Compra — " + nvl(compraAtual.getNumeroDocumento()));
        cmbFornecedor.setValue(compraAtual.getFornecedor());
        txtNumeroDoc.setText(nvl(compraAtual.getNumeroDocumento()));
        dpEmissao.setValue(compraAtual.getDataEmissao());
        dpPrevisao.setValue(compraAtual.getDataPrevisao());
        cmbCondicao.setValue(compraAtual.getCondicaoPagamento());
        txtObservacoes.setText(compraAtual.getObservacoes() != null ? compraAtual.getObservacoes() : "");
        txtFrete.setText(formatDecimal(compraAtual.getValorFrete()));
        txtDescontoGlobal.setText(formatDecimal(compraAtual.getValorDesconto()));
        txtOutrasDespesas.setText(formatDecimal(compraAtual.getValorOutras()));

        // Carregar itens
        vboxItens.getChildren().clear();
        itemRows.clear();
        for (CompraItem item : compraAtual.getItens()) {
            adicionarLinhaItem(item);
        }

        // Modo somente leitura para CONFIRMADA / CANCELADA
        if (!"RASCUNHO".equals(compraAtual.getStatus())) {
            ativarModoLeitura(compraAtual.getStatus());
        } else {
            lblBannerStatus.setVisible(false);
            lblBannerStatus.setManaged(false);
        }

        recalcularTotal();
    }

    private void ativarModoLeitura(String status) {
        somenteLeitura = true;
        String cor = "CONFIRMADA".equals(status) ? "badge-success" : "badge-danger";
        String texto = "CONFIRMADA".equals(status)
                ? "Compra CONFIRMADA — somente visualização"
                : "Compra CANCELADA — somente visualização";
        lblBannerStatus.setText(texto);
        lblBannerStatus.getStyleClass().addAll(cor);
        lblBannerStatus.setVisible(true);
        lblBannerStatus.setManaged(true);

        cmbFornecedor.setDisable(true);
        txtNumeroDoc.setEditable(false);
        dpEmissao.setDisable(true);
        dpPrevisao.setDisable(true);
        cmbCondicao.setDisable(true);
        spnParcelas.setDisable(true);
        txtObservacoes.setEditable(false);
        txtFrete.setEditable(false);
        txtDescontoGlobal.setEditable(false);
        txtOutrasDespesas.setEditable(false);
        hboxBotoes.setVisible(false);
        hboxBotoes.setManaged(false);
    }

    // ================================================================
    // Itens da compra
    // ================================================================

    @FXML
    private void adicionarItem() {
        adicionarLinhaItem(null);
    }

    private void adicionarLinhaItem(CompraItem itemExistente) {
        ItemRow ir = new ItemRow();
        ir.item = itemExistente != null ? itemExistente : new CompraItem();

        // --- Produto ComboBox ---
        ir.cbProduto = new ComboBox<>(FXCollections.observableArrayList(todosProdutos));
        ir.cbProduto.setPromptText("Buscar produto...");
        ir.cbProduto.setPrefWidth(260);
        ir.cbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Produto p) {
                if (p == null) return "";
                String cod = p.getCodigoInterno() != null ? "[" + p.getCodigoInterno() + "] " : "";
                return cod + p.getDescricao();
            }
            @Override public Produto fromString(String s) { return null; }
        });
        ir.cbProduto.setEditable(true);
        ir.cbProduto.getEditor().textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                ir.cbProduto.setItems(FXCollections.observableArrayList(todosProdutos));
                return;
            }
            String lower = val.toLowerCase();
            var filtrados = todosProdutos.stream()
                    .filter(p -> p.getDescricao().toLowerCase().contains(lower)
                            || (p.getCodigoInterno() != null && p.getCodigoInterno().toLowerCase().contains(lower))
                            || (p.getCodigoBarras() != null && p.getCodigoBarras().contains(val)))
                    .collect(Collectors.toList());
            ir.cbProduto.setItems(FXCollections.observableArrayList(filtrados));
            if (!filtrados.isEmpty() && !ir.cbProduto.isShowing()) ir.cbProduto.show();
        });
        ir.cbProduto.valueProperty().addListener((obs, old, prod) -> aoSelecionarProduto(ir, prod));

        // --- Lote ---
        ir.txtLote = new TextField();
        ir.txtLote.setPromptText("Lote");
        ir.txtLote.setPrefWidth(100);
        ir.txtLote.setVisible(false);
        ir.txtLote.setManaged(false);

        // --- Validade ---
        ir.dpValidade = new DatePicker();
        ir.dpValidade.setPromptText("Validade");
        ir.dpValidade.setPrefWidth(130);
        ir.dpValidade.setVisible(false);
        ir.dpValidade.setManaged(false);

        // --- Quantidade ---
        ir.txtQtd = new TextField("1");
        ir.txtQtd.setPrefWidth(80);
        ir.txtQtd.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        // --- Custo ---
        ir.txtCusto = new TextField("0");
        ir.txtCusto.setPrefWidth(100);
        ir.txtCusto.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        // --- Desconto ---
        ir.txtDesconto = new TextField("0");
        ir.txtDesconto.setPrefWidth(90);
        ir.txtDesconto.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        // --- Total (read-only) ---
        ir.lblTotal = new Label("R$ 0,00");
        ir.lblTotal.setPrefWidth(110);
        ir.lblTotal.setStyle("-fx-font-weight: bold;");

        // --- Botão remover ---
        Button btnRemover = new Button("✕");
        btnRemover.getStyleClass().add("btn-danger");
        btnRemover.setOnAction(e -> removerLinhaItem(ir));

        // --- Montar HBox ---
        ir.row = new HBox(8,
                ir.cbProduto, ir.txtLote, ir.dpValidade,
                ir.txtQtd, ir.txtCusto, ir.txtDesconto,
                ir.lblTotal, btnRemover);
        ir.row.setAlignment(Pos.CENTER_LEFT);
        ir.row.setPadding(new Insets(4, 0, 4, 0));

        // Pré-preencher se item existente
        if (itemExistente != null) {
            ir.cbProduto.setValue(itemExistente.getProduto());
            ir.txtQtd.setText(itemExistente.getQuantidade().toPlainString());
            ir.txtCusto.setText(itemExistente.getCustoUnitario().toPlainString());
            ir.txtDesconto.setText(itemExistente.getDesconto().toPlainString());
            if (itemExistente.getLote() != null) {
                ir.txtLote.setText(itemExistente.getLote().getNumeroLote());
                ir.dpValidade.setValue(itemExistente.getLote().getDataValidade());
            }
            recalcularLinha(ir);
            // Mostrar campos de lote se produto usa lote
            if (itemExistente.getProduto() != null
                    && Boolean.TRUE.equals(itemExistente.getProduto().getUsaLoteValidade())) {
                ir.txtLote.setVisible(true); ir.txtLote.setManaged(true);
                ir.dpValidade.setVisible(true); ir.dpValidade.setManaged(true);
            }
        }

        if (somenteLeitura) {
            ir.cbProduto.setDisable(true);
            ir.txtLote.setEditable(false);
            ir.dpValidade.setDisable(true);
            ir.txtQtd.setEditable(false);
            ir.txtCusto.setEditable(false);
            ir.txtDesconto.setEditable(false);
            btnRemover.setDisable(true);
        }

        vboxItens.getChildren().add(ir.row);
        itemRows.add(ir);
        recalcularTotal();
    }

    private void aoSelecionarProduto(ItemRow ir, Produto produto) {
        if (produto == null) return;
        // Mostrar/ocultar campos de lote
        boolean usaLote = Boolean.TRUE.equals(produto.getUsaLoteValidade());
        ir.txtLote.setVisible(usaLote); ir.txtLote.setManaged(usaLote);
        ir.dpValidade.setVisible(usaLote); ir.dpValidade.setManaged(usaLote);

        // Buscar último custo
        Integer empresaId = authService.getEmpresaIdLogado();
        BigDecimal ultimoCusto = compraService.buscarUltimoCusto(produto.getId(), empresaId)
                .orElse(produto.getPrecoCusto());
        ir.txtCusto.setText(ultimoCusto.toPlainString());
        recalcularLinha(ir);
    }

    private void removerLinhaItem(ItemRow ir) {
        vboxItens.getChildren().remove(ir.row);
        itemRows.remove(ir);
        recalcularTotal();
    }

    private void recalcularLinha(ItemRow ir) {
        BigDecimal qtd = parseBD(ir.txtQtd.getText());
        BigDecimal custo = parseBD(ir.txtCusto.getText());
        BigDecimal desc = parseBD(ir.txtDesconto.getText());
        BigDecimal total = qtd.multiply(custo).subtract(desc).max(BigDecimal.ZERO);
        ir.item.setQuantidade(qtd);
        ir.item.setCustoUnitario(custo);
        ir.item.setDesconto(desc);
        ir.item.setValorTotal(total);
        ir.lblTotal.setText(CURRENCY_FMT.format(total));
        recalcularTotal();
    }

    private void recalcularTotal() {
        BigDecimal valorProdutos = itemRows.stream()
                .map(r -> r.item.getValorTotal() != null ? r.item.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal frete = parseBD(txtFrete.getText());
        BigDecimal descGlobal = parseBD(txtDescontoGlobal.getText());
        BigDecimal outros = parseBD(txtOutrasDespesas.getText());
        BigDecimal total = valorProdutos.add(frete).add(outros).subtract(descGlobal).max(BigDecimal.ZERO);
        lblValorProdutos.setText(CURRENCY_FMT.format(valorProdutos));
        lblValorTotal.setText(CURRENCY_FMT.format(total));
    }

    // ================================================================
    // Ações dos botões
    // ================================================================

    @FXML
    private void salvarRascunho() {
        try {
            Compra compra = montarCompra();
            Compra salva = compraService.salvarRascunho(compra);
            this.compraAtual = salva;
            lblTitulo.setText("Compra — " + salva.getNumeroDocumento());
            new Alert(Alert.AlertType.INFORMATION,
                    "Rascunho salvo: " + salva.getNumeroDocumento(), ButtonType.OK).showAndWait();
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao salvar rascunho", e);
            new Alert(Alert.AlertType.ERROR, "Erro ao salvar: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void confirmarCompra() {
        try {
            Compra compra = montarCompra();
            Compra rascunho = compraService.salvarRascunho(compra);
            int parcelas = spnParcelas.getValue() != null ? spnParcelas.getValue() : 1;
            compraService.confirmarCompra(rascunho.getId(), parcelas);
            new Alert(Alert.AlertType.INFORMATION,
                    "Compra confirmada com sucesso!\nEstoque atualizado e contas a pagar geradas.",
                    ButtonType.OK).showAndWait();
            voltar();
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao confirmar compra", e);
            new Alert(Alert.AlertType.ERROR, "Erro ao confirmar: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void voltar() {
        if (conteudoPane == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/compras.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent view = loader.load();
            conteudoPane.getChildren().setAll(view);
        } catch (Exception e) {
            log.error("Erro ao voltar para lista de compras", e);
        }
    }

    // ================================================================
    // Montar entidade a partir do formulário
    // ================================================================

    private Compra montarCompra() {
        Compra compra = compraAtual != null ? compraAtual : new Compra();
        compra.setEmpresa(authService.getEmpresaLogada());
        compra.setFornecedor(cmbFornecedor.getValue());
        compra.setNumeroDocumento(txtNumeroDoc.getText().isBlank() ? null : txtNumeroDoc.getText().trim());
        compra.setDataEmissao(dpEmissao.getValue() != null ? dpEmissao.getValue() : LocalDate.now());
        compra.setDataPrevisao(dpPrevisao.getValue());
        compra.setCondicaoPagamento(cmbCondicao.getValue());
        compra.setObservacoes(txtObservacoes.getText().isBlank() ? null : txtObservacoes.getText());
        compra.setValorFrete(parseBD(txtFrete.getText()));
        compra.setValorDesconto(parseBD(txtDescontoGlobal.getText()));
        compra.setValorOutras(parseBD(txtOutrasDespesas.getText()));

        // Montar itens
        compra.getItens().clear();
        for (ItemRow ir : itemRows) {
            if (ir.cbProduto.getValue() == null) continue;
            CompraItem ci = ir.item;
            ci.setCompra(compra);
            ci.setProduto(ir.cbProduto.getValue());

            // Lote
            if (Boolean.TRUE.equals(ir.cbProduto.getValue().getUsaLoteValidade())
                    && ir.txtLote.getText() != null && !ir.txtLote.getText().isBlank()) {
                String numeroLote = ir.txtLote.getText().trim();
                Produto prod = ir.cbProduto.getValue();
                Lote lote = loteRepository.findByProdutoIdAndNumeroLote(prod.getId(), numeroLote)
                        .orElseGet(() -> {
                            Lote novo = new Lote();
                            novo.setProduto(prod);
                            novo.setNumeroLote(numeroLote);
                            novo.setDataValidade(ir.dpValidade.getValue());
                            novo.setQuantidade(BigDecimal.ZERO);
                            return loteRepository.save(novo);
                        });
                lote.setDataValidade(ir.dpValidade.getValue());
                ci.setLote(lote);
            }

            ci.setQuantidade(parseBD(ir.txtQtd.getText()));
            ci.setCustoUnitario(parseBD(ir.txtCusto.getText()));
            ci.setDesconto(parseBD(ir.txtDesconto.getText()));
            ci.recalcularTotal();
            compra.getItens().add(ci);
        }

        compraService.recalcularTotais(compra);
        return compra;
    }

    // ================================================================
    // Helpers
    // ================================================================

    private BigDecimal parseBD(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatDecimal(BigDecimal v) {
        return v != null ? v.toPlainString() : "0";
    }

    private String nvl(String v) { return v != null ? v : ""; }

    // ================================================================
    // Inner class para linha de item
    // ================================================================

    private static class ItemRow {
        CompraItem item;
        ComboBox<Produto> cbProduto;
        TextField txtLote;
        DatePicker dpValidade;
        TextField txtQtd;
        TextField txtCusto;
        TextField txtDesconto;
        Label lblTotal;
        HBox row;
    }
}
