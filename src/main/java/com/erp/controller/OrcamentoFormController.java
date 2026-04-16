package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.ClienteRepository;
import com.erp.repository.FuncionarioRepository;
import com.erp.repository.ProdutoRepository;
import com.erp.service.AuthService;
import com.erp.service.ConfiguracaoService;
import com.erp.service.OrcamentoPdfService;
import com.erp.service.OrcamentoService;
import com.erp.util.MoneyUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrcamentoFormController implements Initializable {

    private final OrcamentoService orcamentoService;
    private final OrcamentoPdfService orcamentoPdfService;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProdutoRepository produtoRepository;
    private final ConfiguracaoService configuracaoService;
    private final AuthService authService;
    private final ApplicationContext springContext;

    // Header
    @FXML private Label lblNumero;
    @FXML private Label bannerStatus;

    // Cabeçalho
    @FXML private ComboBox<Cliente>     cmbCliente;
    @FXML private ComboBox<Funcionario> cmbVendedor;
    @FXML private DatePicker            dpEmissao;
    @FXML private DatePicker            dpValidade;
    @FXML private TextArea              txtObservacoes;

    // Itens
    @FXML private VBox vboxItens;
    @FXML private Button btnAdicionarItem;

    // Totais
    @FXML private Label     lblSubtotal;
    @FXML private TextField txtDescontoGlobal;
    @FXML private Label     lblDesconto;
    @FXML private Label     lblTotal;

    // Botões
    @FXML private HBox   hboxBotoes;
    @FXML private Button btnSalvar;
    @FXML private Button btnSalvarImprimir;
    @FXML private Button btnVoltar;

    // Estado
    private Orcamento orcamentoAtual;
    private StackPane conteudoPane;
    private boolean somenteLeitura = false;
    private boolean filtrando = false;

    private List<Cliente>     todosClientes    = new ArrayList<>();
    private List<Funcionario> todosFuncionarios = new ArrayList<>();
    private List<Produto>     todosProdutos    = new ArrayList<>();
    private ObservableList<Cliente>     clientesDisplay;
    private ObservableList<Funcionario> funcionariosDisplay;
    private final List<ItemRow> itemRows = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarDados();
        configurarClienteCombo();
        configurarVendedorCombo();
        configurarTotaisListeners();
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosClientes    = clienteRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        todosFuncionarios = funcionarioRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        todosProdutos    = produtoRepository.findByEmpresaIdAndAtivoTrue(empresaId);
    }

    private void configurarClienteCombo() {
        clientesDisplay = FXCollections.observableArrayList(todosClientes);
        cmbCliente.setItems(clientesDisplay);
        cmbCliente.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Cliente c) {
                if (c == null) return "";
                return "PJ".equals(c.getTipoPessoa()) && c.getRazaoSocial() != null
                        ? c.getRazaoSocial() : c.getNome();
            }
            @Override public Cliente fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return todosClientes.stream()
                        .filter(c -> {
                            String nome = "PJ".equals(c.getTipoPessoa()) && c.getRazaoSocial() != null
                                    ? c.getRazaoSocial() : c.getNome();
                            return s.equals(nome);
                        })
                        .findFirst()
                        .orElse(cmbCliente.getValue());
            }
        });
        cmbCliente.setEditable(true);
        cmbCliente.getEditor().textProperty().addListener((obs, old, val) ->
                Platform.runLater(() -> filtrarClientes(val)));
    }

    private void filtrarClientes(String texto) {
        if (filtrando || clientesDisplay == null) return;
        Cliente selecionado = cmbCliente.getValue();
        if (selecionado != null && texto != null) {
            String nomeDisplay = "PJ".equals(selecionado.getTipoPessoa()) && selecionado.getRazaoSocial() != null
                    ? selecionado.getRazaoSocial() : selecionado.getNome();
            if (texto.equals(nomeDisplay)) return;
        }
        filtrando = true;
        try {
            if (texto == null || texto.isBlank()) {
                clientesDisplay.setAll(todosClientes);
                return;
            }
            String lower = texto.toLowerCase();
            List<Cliente> filtrados = todosClientes.stream()
                    .filter(c -> {
                        String nome = "PJ".equals(c.getTipoPessoa()) && c.getRazaoSocial() != null
                                ? c.getRazaoSocial() : c.getNome();
                        return nome != null && nome.toLowerCase().contains(lower);
                    })
                    .toList();
            clientesDisplay.setAll(filtrados);
            if (!filtrados.isEmpty() && !cmbCliente.isShowing()) cmbCliente.show();
        } finally {
            filtrando = false;
        }
    }

    private void configurarVendedorCombo() {
        funcionariosDisplay = FXCollections.observableArrayList(todosFuncionarios);
        cmbVendedor.setItems(funcionariosDisplay);
        cmbVendedor.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Funcionario f) {
                return f != null ? f.getNome() : "";
            }
            @Override public Funcionario fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return todosFuncionarios.stream()
                        .filter(f -> s.equals(f.getNome()))
                        .findFirst()
                        .orElse(cmbVendedor.getValue());
            }
        });
        cmbVendedor.setEditable(true);
    }

    private void configurarTotaisListeners() {
        txtDescontoGlobal.textProperty().addListener((obs, old, val) -> recalcularTotal());
    }

    // ================================================================
    // API pública
    // ================================================================

    public void setOrcamento(Orcamento orcamento) {
        this.orcamentoAtual = orcamento;
        Platform.runLater(this::preencherFormulario);
    }

    public void setConteudoPane(StackPane pane) {
        this.conteudoPane = pane;
    }

    // ================================================================
    // Preenchimento
    // ================================================================

    private void preencherFormulario() {
        Integer empresaId = authService.getEmpresaIdLogado();

        if (orcamentoAtual == null) {
            // Novo orçamento
            lblNumero.setText("(gerado automaticamente)");
            dpEmissao.setValue(LocalDate.now());

            // Data de validade padrão
            Configuracao config = configuracaoService.buscarPorEmpresa(empresaId);
            int diasValidade = config != null && config.getDiasValidadeOrcamento() != null
                    ? config.getDiasValidadeOrcamento() : 30;
            dpValidade.setValue(LocalDate.now().plusDays(diasValidade));

            // Vendedor padrão: funcionário vinculado ao usuário logado
            Usuario usuarioLogado = authService.getUsuarioLogado();
            if (usuarioLogado != null) {
                funcionarioRepository.findByEmpresaIdAndUsuarioId(empresaId, usuarioLogado.getId())
                        .ifPresent(f -> cmbVendedor.setValue(f));
            }
            bannerStatus.setVisible(false);
            bannerStatus.setManaged(false);
            return;
        }

        lblNumero.setText(orcamentoAtual.getNumero());
        cmbCliente.setValue(orcamentoAtual.getCliente());
        cmbVendedor.setValue(orcamentoAtual.getVendedor());
        dpEmissao.setValue(orcamentoAtual.getDataEmissao());
        dpValidade.setValue(orcamentoAtual.getDataValidade());
        txtObservacoes.setText(orcamentoAtual.getObservacoes() != null
                ? orcamentoAtual.getObservacoes() : "");
        txtDescontoGlobal.setText(formatDecimal(orcamentoAtual.getValorDesconto()));

        // Carregar itens
        vboxItens.getChildren().clear();
        itemRows.clear();
        for (OrcamentoItem item : orcamentoAtual.getItens()) {
            adicionarLinhaItem(item);
        }

        // Modo somente leitura para CONVERTIDO/CANCELADO/EXPIRADO
        String status = orcamentoAtual.getStatus();
        if (!"ABERTO".equals(status)) {
            ativarModoLeitura(status);
        } else {
            bannerStatus.setVisible(false);
            bannerStatus.setManaged(false);
        }

        recalcularTotal();
    }

    private void ativarModoLeitura(String status) {
        somenteLeitura = true;
        String texto = switch (status) {
            case "CONVERTIDO" -> "Orçamento CONVERTIDO — somente visualização";
            case "CANCELADO"  -> "Orçamento CANCELADO — somente visualização";
            case "EXPIRADO"   -> "Orçamento EXPIRADO — somente visualização";
            default -> "Orçamento " + status + " — somente visualização";
        };
        String estilo = switch (status) {
            case "CONVERTIDO" -> "badge-info";
            case "CANCELADO"  -> "badge-danger";
            default           -> "badge-neutral";
        };
        bannerStatus.setText(texto);
        bannerStatus.getStyleClass().removeIf(s -> s.startsWith("badge-"));
        bannerStatus.getStyleClass().add(estilo);
        bannerStatus.setVisible(true);
        bannerStatus.setManaged(true);

        cmbCliente.setDisable(true);
        cmbVendedor.setDisable(true);
        dpEmissao.setDisable(true);
        dpValidade.setDisable(true);
        txtObservacoes.setEditable(false);
        txtDescontoGlobal.setEditable(false);
        btnAdicionarItem.setDisable(true);

        hboxBotoes.getChildren().removeAll(btnSalvar, btnSalvarImprimir);
    }

    // ================================================================
    // Itens
    // ================================================================

    @FXML
    private void adicionarItem() {
        adicionarLinhaItem(null);
    }

    private void adicionarLinhaItem(OrcamentoItem itemExistente) {
        ItemRow ir = new ItemRow();
        ir.item = itemExistente != null ? itemExistente : new OrcamentoItem();

        // Produto ComboBox
        ObservableList<Produto> produtosDisplay = FXCollections.observableArrayList(todosProdutos);
        ir.cbProduto = new ComboBox<>(produtosDisplay);
        ir.cbProduto.setPromptText("Buscar produto...");
        ir.cbProduto.setPrefWidth(250);
        ir.cbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Produto p) {
                if (p == null) return "";
                String cod = p.getCodigoInterno() != null ? "[" + p.getCodigoInterno() + "] " : "";
                return cod + p.getDescricao();
            }
            @Override public Produto fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return todosProdutos.stream()
                        .filter(p -> {
                            String cod = p.getCodigoInterno() != null
                                    ? "[" + p.getCodigoInterno() + "] " : "";
                            return s.equals(cod + p.getDescricao());
                        })
                        .findFirst()
                        .orElse(ir.cbProduto.getValue());
            }
        });
        ir.cbProduto.setEditable(true);
        final boolean[] filtrandoProd = {false};
        ir.cbProduto.getEditor().textProperty().addListener((obs, old, val) ->
                Platform.runLater(() -> {
                    if (filtrandoProd[0]) return;
                    Produto sel = ir.cbProduto.getValue();
                    if (sel != null && val != null) {
                        String cod = sel.getCodigoInterno() != null
                                ? "[" + sel.getCodigoInterno() + "] " : "";
                        if (val.equals(cod + sel.getDescricao())) return;
                    }
                    filtrandoProd[0] = true;
                    try {
                        if (val == null || val.isBlank()) {
                            produtosDisplay.setAll(todosProdutos);
                            return;
                        }
                        String lower = val.toLowerCase();
                        List<Produto> filtrados = todosProdutos.stream()
                                .filter(p -> p.getDescricao().toLowerCase().contains(lower)
                                        || (p.getCodigoInterno() != null
                                            && p.getCodigoInterno().toLowerCase().contains(lower))
                                        || (p.getCodigoBarras() != null
                                            && p.getCodigoBarras().contains(val)))
                                .toList();
                        produtosDisplay.setAll(filtrados);
                        if (!filtrados.isEmpty() && !ir.cbProduto.isShowing()) ir.cbProduto.show();
                    } finally {
                        filtrandoProd[0] = false;
                    }
                }));
        ir.cbProduto.valueProperty().addListener((obs, old, prod) -> aoSelecionarProduto(ir, prod));

        // Descrição
        ir.txtDescricao = new TextField();
        ir.txtDescricao.setPromptText("Descrição");
        ir.txtDescricao.setPrefWidth(200);

        // Quantidade
        ir.txtQtd = new TextField("1");
        ir.txtQtd.setPrefWidth(70);
        ir.txtQtd.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        // Preço Unitário
        ir.txtPreco = new TextField("0,00");
        ir.txtPreco.setPrefWidth(100);
        ir.txtPreco.textProperty().addListener((obs, old, val) -> {
            verificarPrecoMinimo(ir);
            recalcularLinha(ir);
        });

        // Aviso preço mínimo
        ir.lblAvisoPreco = new Label();
        ir.lblAvisoPreco.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px;");
        ir.lblAvisoPreco.setVisible(false);
        ir.lblAvisoPreco.setManaged(false);

        // Desconto
        ir.txtDesconto = new TextField("0,00");
        ir.txtDesconto.setPrefWidth(90);
        ir.txtDesconto.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        // Total (read-only)
        ir.lblTotal = new Label("R$ 0,00");
        ir.lblTotal.setPrefWidth(110);
        ir.lblTotal.setStyle("-fx-font-weight: bold;");

        // Botão remover
        Button btnRemover = new Button("✕");
        btnRemover.getStyleClass().add("btn-danger");
        btnRemover.setOnAction(e -> removerLinhaItem(ir));

        // Montar linha
        HBox row = new HBox(6,
                ir.cbProduto, ir.txtDescricao, ir.txtQtd,
                ir.txtPreco, ir.txtDesconto, ir.lblTotal, btnRemover);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        VBox itemBox = new VBox(2, row, ir.lblAvisoPreco);
        ir.row = itemBox;

        // Pré-preencher se item existente
        if (itemExistente != null) {
            ir.cbProduto.setValue(itemExistente.getProduto());
            ir.txtDescricao.setText(nvl(itemExistente.getDescricao()));
            ir.txtQtd.setText(itemExistente.getQuantidade().toPlainString());
            ir.txtPreco.setText(formatDecimal(itemExistente.getPrecoUnitario()));
            ir.txtDesconto.setText(formatDecimal(itemExistente.getDesconto()));
            recalcularLinha(ir);
        }

        if (somenteLeitura) {
            ir.cbProduto.setDisable(true);
            ir.txtDescricao.setEditable(false);
            ir.txtQtd.setEditable(false);
            ir.txtPreco.setEditable(false);
            ir.txtDesconto.setEditable(false);
            btnRemover.setDisable(true);
        }

        vboxItens.getChildren().add(itemBox);
        itemRows.add(ir);
        recalcularTotal();
    }

    private void aoSelecionarProduto(ItemRow ir, Produto produto) {
        if (produto == null) return;
        ir.txtDescricao.setText(produto.getDescricao());
        ir.txtPreco.setText(formatDecimal(produto.getPrecoVenda()));
        ir.produtoAtual = produto;
        verificarPrecoMinimo(ir);
        recalcularLinha(ir);
    }

    private void verificarPrecoMinimo(ItemRow ir) {
        if (ir.produtoAtual == null || ir.lblAvisoPreco == null) return;
        BigDecimal preco = parseBD(ir.txtPreco.getText());
        BigDecimal minimo = ir.produtoAtual.getPrecoMinimo();
        if (minimo != null && preco.compareTo(minimo) < 0) {
            ir.lblAvisoPreco.setText("Abaixo do preço mínimo: " + MoneyUtils.formatCurrency(minimo));
            ir.lblAvisoPreco.setVisible(true);
            ir.lblAvisoPreco.setManaged(true);
        } else {
            ir.lblAvisoPreco.setVisible(false);
            ir.lblAvisoPreco.setManaged(false);
        }
    }

    private void removerLinhaItem(ItemRow ir) {
        vboxItens.getChildren().remove(ir.row);
        itemRows.remove(ir);
        recalcularTotal();
    }

    private void recalcularLinha(ItemRow ir) {
        BigDecimal qtd   = parseBD(ir.txtQtd.getText());
        BigDecimal preco = parseBD(ir.txtPreco.getText());
        BigDecimal desc  = parseBD(ir.txtDesconto.getText());
        BigDecimal total = qtd.multiply(preco).subtract(desc).max(BigDecimal.ZERO);
        ir.item.setQuantidade(qtd);
        ir.item.setPrecoUnitario(preco);
        ir.item.setDesconto(desc);
        ir.item.setValorTotal(total);
        ir.lblTotal.setText(MoneyUtils.formatCurrency(total));
        recalcularTotal();
    }

    private void recalcularTotal() {
        BigDecimal subtotal = itemRows.stream()
                .map(r -> r.item.getValorTotal() != null ? r.item.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descGlobal = parseBD(txtDescontoGlobal.getText());
        BigDecimal total = subtotal.subtract(descGlobal).max(BigDecimal.ZERO);
        lblSubtotal.setText(MoneyUtils.formatCurrency(subtotal));
        lblDesconto.setText(MoneyUtils.formatCurrency(descGlobal));
        lblTotal.setText(MoneyUtils.formatCurrency(total));
    }

    // ================================================================
    // Ações dos botões
    // ================================================================

    @FXML
    private void salvar() {
        try {
            Orcamento orc = montarOrcamento();
            Orcamento salvo = orcamentoService.salvar(orc);
            this.orcamentoAtual = orcamentoService.buscarComItens(salvo.getId()).orElse(salvo);
            lblNumero.setText(salvo.getNumero());
            new Alert(Alert.AlertType.INFORMATION,
                    "Orçamento salvo: " + salvo.getNumero(), ButtonType.OK).showAndWait();
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao salvar orçamento", e);
            new Alert(Alert.AlertType.ERROR, "Erro ao salvar: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void salvarEImprimir() {
        try {
            Orcamento orc = montarOrcamento();
            Orcamento salvo = orcamentoService.salvar(orc);
            this.orcamentoAtual = orcamentoService.buscarComItens(salvo.getId()).orElse(salvo);
            lblNumero.setText(salvo.getNumero());
            orcamentoPdfService.abrirPdfEmVisualizador(salvo.getId());
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao salvar/imprimir orçamento", e);
            new Alert(Alert.AlertType.ERROR, "Erro: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void voltar() {
        if (conteudoPane == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/orcamentos.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent view = loader.load();
            conteudoPane.getChildren().setAll(view);
        } catch (Exception e) {
            log.error("Erro ao voltar para lista de orçamentos", e);
        }
    }

    // ================================================================
    // Montar entidade
    // ================================================================

    private Orcamento montarOrcamento() {
        Orcamento orc = orcamentoAtual != null ? orcamentoAtual : new Orcamento();
        orc.setEmpresa(authService.getEmpresaLogada());
        orc.setCliente(cmbCliente.getValue());
        orc.setVendedor(cmbVendedor.getValue());
        orc.setDataEmissao(dpEmissao.getValue() != null ? dpEmissao.getValue() : LocalDate.now());
        orc.setDataValidade(dpValidade.getValue());
        orc.setObservacoes(txtObservacoes.getText().isBlank() ? null : txtObservacoes.getText());
        orc.setValorDesconto(parseBD(txtDescontoGlobal.getText()));

        // Montar itens
        orc.getItens().clear();
        for (ItemRow ir : itemRows) {
            if (ir.cbProduto.getValue() == null) continue;
            OrcamentoItem item = ir.item;
            item.setOrcamento(orc);
            item.setProduto(ir.cbProduto.getValue());
            item.setDescricao(ir.txtDescricao.getText().isBlank()
                    ? ir.cbProduto.getValue().getDescricao() : ir.txtDescricao.getText());
            item.setQuantidade(parseBD(ir.txtQtd.getText()));
            item.setPrecoUnitario(parseBD(ir.txtPreco.getText()));
            item.setDesconto(parseBD(ir.txtDesconto.getText()));
            item.recalcularTotal();
            orc.getItens().add(item);
        }

        return orc;
    }

    // ================================================================
    // Helpers
    // ================================================================

    private BigDecimal parseBD(String text) {
        return MoneyUtils.parse(text);
    }

    private String formatDecimal(BigDecimal v) {
        return MoneyUtils.formatInput(v);
    }

    private String nvl(String v) { return v != null ? v : ""; }

    // ================================================================
    // Inner class linha de item
    // ================================================================

    private static class ItemRow {
        OrcamentoItem   item;
        Produto         produtoAtual;
        ComboBox<Produto> cbProduto;
        TextField       txtDescricao;
        TextField       txtQtd;
        TextField       txtPreco;
        TextField       txtDesconto;
        Label           lblTotal;
        Label           lblAvisoPreco;
        VBox            row;
    }
}
