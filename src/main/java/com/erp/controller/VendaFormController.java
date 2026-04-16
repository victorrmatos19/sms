package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Cliente;
import com.erp.model.Funcionario;
import com.erp.model.Produto;
import com.erp.model.Usuario;
import com.erp.model.Venda;
import com.erp.model.VendaItem;
import com.erp.model.VendaPagamento;
import com.erp.repository.ClienteRepository;
import com.erp.repository.FuncionarioRepository;
import com.erp.repository.ProdutoRepository;
import com.erp.service.AuthService;
import com.erp.service.VendaService;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class VendaFormController implements Initializable {

    private final VendaService vendaService;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProdutoRepository produtoRepository;
    private final AuthService authService;
    private final ApplicationContext springContext;

    @FXML private Label lblTitulo;
    @FXML private Label lblNumero;
    @FXML private Label lblBannerStatus;

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<Funcionario> cmbVendedor;
    @FXML private DatePicker dpDataVenda;
    @FXML private ComboBox<String> cmbFormaPagamento;
    @FXML private Label lblParcelas;
    @FXML private Spinner<Integer> spnParcelas;
    @FXML private TextArea txtObservacoes;

    @FXML private VBox vboxItens;
    @FXML private Button btnAdicionarItem;

    @FXML private Label lblSubtotal;
    @FXML private TextField txtDescontoGlobal;
    @FXML private Label lblDesconto;
    @FXML private Label lblTotal;

    @FXML private HBox hboxBotoes;
    @FXML private Button btnFinalizar;

    private Venda vendaAtual;
    private StackPane conteudoPane;
    private boolean somenteLeitura = false;
    private boolean filtrandoCliente = false;

    private List<Cliente> todosClientes = new ArrayList<>();
    private List<Funcionario> todosFuncionarios = new ArrayList<>();
    private List<Produto> todosProdutos = new ArrayList<>();
    private ObservableList<Cliente> clientesDisplay;
    private ObservableList<Funcionario> funcionariosDisplay;
    private final List<ItemRow> itemRows = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarDados();
        configurarClienteCombo();
        configurarVendedorCombo();
        configurarFormaPagamento();
        configurarParcelas();
        txtDescontoGlobal.textProperty().addListener((obs, old, val) -> recalcularTotal());
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        todosClientes = clienteRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        todosFuncionarios = funcionarioRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        todosProdutos = produtoRepository.findByEmpresaIdAndAtivoTrue(empresaId);
    }

    private void configurarClienteCombo() {
        clientesDisplay = FXCollections.observableArrayList(todosClientes);
        cmbCliente.setItems(clientesDisplay);
        cmbCliente.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Cliente cliente) {
                if (cliente == null) return "";
                return "PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null
                        ? cliente.getRazaoSocial() : cliente.getNome();
            }

            @Override
            public Cliente fromString(String texto) {
                if (texto == null || texto.isBlank()) return null;
                return todosClientes.stream()
                        .filter(c -> texto.equals(nomeCliente(c)))
                        .findFirst()
                        .orElse(cmbCliente.getValue());
            }
        });
        cmbCliente.setEditable(true);
        cmbCliente.getEditor().textProperty().addListener((obs, old, val) ->
                Platform.runLater(() -> filtrarClientes(val)));
    }

    private void filtrarClientes(String texto) {
        if (filtrandoCliente || clientesDisplay == null) return;
        Cliente selecionado = cmbCliente.getValue();
        if (selecionado != null && texto != null && texto.equals(nomeCliente(selecionado))) return;

        filtrandoCliente = true;
        try {
            if (texto == null || texto.isBlank()) {
                clientesDisplay.setAll(todosClientes);
                return;
            }
            String lower = texto.toLowerCase();
            List<Cliente> filtrados = todosClientes.stream()
                    .filter(c -> nomeCliente(c).toLowerCase().contains(lower))
                    .toList();
            clientesDisplay.setAll(filtrados);
            if (!filtrados.isEmpty() && !cmbCliente.isShowing()) cmbCliente.show();
        } finally {
            filtrandoCliente = false;
        }
    }

    private void configurarVendedorCombo() {
        funcionariosDisplay = FXCollections.observableArrayList(todosFuncionarios);
        cmbVendedor.setItems(funcionariosDisplay);
        cmbVendedor.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Funcionario funcionario) {
                return funcionario != null ? funcionario.getNome() : "";
            }

            @Override
            public Funcionario fromString(String texto) {
                if (texto == null || texto.isBlank()) return null;
                return todosFuncionarios.stream()
                        .filter(f -> texto.equals(f.getNome()))
                        .findFirst()
                        .orElse(cmbVendedor.getValue());
            }
        });
        cmbVendedor.setEditable(true);
    }

    private void configurarFormaPagamento() {
        cmbFormaPagamento.setItems(FXCollections.observableArrayList(
                "DINHEIRO", "PIX", "CARTAO_DEBITO", "CARTAO_CREDITO", "PRAZO", "CREDIARIO"));
        cmbFormaPagamento.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String forma) {
                return formatarFormaPagamento(forma);
            }

            @Override
            public String fromString(String texto) {
                return texto;
            }
        });
        cmbFormaPagamento.setValue("DINHEIRO");
        cmbFormaPagamento.valueProperty().addListener((obs, old, val) -> atualizarVisibilidadeParcelas());
    }

    private void configurarParcelas() {
        spnParcelas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, 1));
        atualizarVisibilidadeParcelas();
    }

    private void atualizarVisibilidadeParcelas() {
        String forma = cmbFormaPagamento.getValue();
        boolean mostra = "CARTAO_CREDITO".equals(forma) || "PRAZO".equals(forma) || "CREDIARIO".equals(forma);
        lblParcelas.setVisible(mostra);
        lblParcelas.setManaged(mostra);
        spnParcelas.setVisible(mostra);
        spnParcelas.setManaged(mostra);
    }

    public void setVenda(Venda venda) {
        this.vendaAtual = venda;
        Platform.runLater(this::preencherFormulario);
    }

    public void setConteudoPane(StackPane pane) {
        this.conteudoPane = pane;
    }

    private void preencherFormulario() {
        if (vendaAtual == null) {
            lblTitulo.setText("Nova Venda");
            lblNumero.setText("(gerado automaticamente)");
            dpDataVenda.setValue(LocalDate.now());
            selecionarVendedorDoUsuarioLogado();
            lblBannerStatus.setVisible(false);
            lblBannerStatus.setManaged(false);
            if (itemRows.isEmpty()) adicionarLinhaItem(null);
            return;
        }

        lblTitulo.setText("Venda - " + nvl(vendaAtual.getNumero()));
        lblNumero.setText(nvl(vendaAtual.getNumero()));
        cmbCliente.setValue(vendaAtual.getCliente());
        cmbVendedor.setValue(vendaAtual.getVendedor());
        dpDataVenda.setValue(vendaAtual.getDataVenda() != null
                ? vendaAtual.getDataVenda().toLocalDate() : LocalDate.now());
        txtObservacoes.setText(vendaAtual.getObservacoes() != null ? vendaAtual.getObservacoes() : "");
        txtDescontoGlobal.setText(formatDecimal(vendaAtual.getValorDesconto()));

        if (vendaAtual.getPagamentos() != null && !vendaAtual.getPagamentos().isEmpty()) {
            VendaPagamento pagamento = vendaAtual.getPagamentos().get(0);
            cmbFormaPagamento.setValue(pagamento.getFormaPagamento());
            spnParcelas.getValueFactory().setValue(
                    pagamento.getParcelas() != null ? pagamento.getParcelas() : 1);
        }

        vboxItens.getChildren().clear();
        itemRows.clear();
        for (VendaItem item : vendaAtual.getItens()) {
            adicionarLinhaItem(item);
        }

        ativarModoLeitura(vendaAtual.getStatus());
        recalcularTotal();
    }

    private void selecionarVendedorDoUsuarioLogado() {
        Usuario usuarioLogado = authService.getUsuarioLogado();
        if (usuarioLogado == null) return;
        funcionarioRepository.findByEmpresaIdAndUsuarioId(authService.getEmpresaIdLogado(), usuarioLogado.getId())
                .ifPresent(cmbVendedor::setValue);
    }

    private void ativarModoLeitura(String status) {
        somenteLeitura = true;
        lblBannerStatus.setText("Venda " + nvl(status) + " - somente visualização");
        lblBannerStatus.getStyleClass().removeIf(s -> s.startsWith("badge-"));
        lblBannerStatus.getStyleClass().add("CANCELADA".equals(status) ? "badge-danger" : "badge-success");
        lblBannerStatus.setVisible(true);
        lblBannerStatus.setManaged(true);

        cmbCliente.setDisable(true);
        cmbVendedor.setDisable(true);
        dpDataVenda.setDisable(true);
        cmbFormaPagamento.setDisable(true);
        spnParcelas.setDisable(true);
        txtObservacoes.setEditable(false);
        txtDescontoGlobal.setEditable(false);
        btnAdicionarItem.setDisable(true);
        hboxBotoes.getChildren().remove(btnFinalizar);
    }

    @FXML
    private void adicionarItem() {
        adicionarLinhaItem(null);
    }

    private void adicionarLinhaItem(VendaItem itemExistente) {
        ItemRow ir = new ItemRow();
        ir.item = itemExistente != null ? itemExistente : new VendaItem();

        ObservableList<Produto> produtosDisplay = FXCollections.observableArrayList(todosProdutos);
        ir.cbProduto = new ComboBox<>(produtosDisplay);
        ir.cbProduto.setPromptText("Buscar produto...");
        ir.cbProduto.setPrefWidth(250);
        ir.cbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Produto produto) {
                if (produto == null) return "";
                String codigo = produto.getCodigoInterno() != null ? "[" + produto.getCodigoInterno() + "] " : "";
                return codigo + produto.getDescricao();
            }

            @Override
            public Produto fromString(String texto) {
                if (texto == null || texto.isBlank()) return null;
                return todosProdutos.stream()
                        .filter(p -> texto.equals(displayProduto(p)))
                        .findFirst()
                        .orElse(ir.cbProduto.getValue());
            }
        });
        ir.cbProduto.setEditable(true);
        ir.cbProduto.setVisibleRowCount(10);
        final boolean[] filtrandoProduto = {false};
        ir.cbProduto.getEditor().textProperty().addListener((obs, old, val) ->
                Platform.runLater(() -> filtrarProdutos(ir, produtosDisplay, filtrandoProduto, val)));
        ir.cbProduto.setOnShowing(e -> {
            if (ir.cbProduto.getValue() == null
                    && (ir.cbProduto.getEditor().getText() == null
                        || ir.cbProduto.getEditor().getText().isBlank())) {
                produtosDisplay.setAll(todosProdutos);
            }
        });
        ir.cbProduto.valueProperty().addListener((obs, old, produto) -> aoSelecionarProduto(ir, produto));

        ir.txtDescricao = new TextField();
        ir.txtDescricao.setPromptText("Descrição");
        ir.txtDescricao.setPrefWidth(200);

        ir.txtQtd = new TextField("1");
        ir.txtQtd.setPrefWidth(70);
        ir.txtQtd.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        ir.txtPreco = new TextField("0,00");
        ir.txtPreco.setPrefWidth(100);
        ir.txtPreco.textProperty().addListener((obs, old, val) -> {
            verificarPrecoMinimo(ir);
            recalcularLinha(ir);
        });

        ir.lblAvisoPreco = new Label();
        ir.lblAvisoPreco.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px;");
        ir.lblAvisoPreco.setVisible(false);
        ir.lblAvisoPreco.setManaged(false);

        ir.txtDesconto = new TextField("0,00");
        ir.txtDesconto.setPrefWidth(90);
        ir.txtDesconto.textProperty().addListener((obs, old, val) -> recalcularLinha(ir));

        ir.lblEstoque = new Label("Estoque: -");
        ir.lblEstoque.setPrefWidth(95);
        ir.lblEstoque.getStyleClass().add("form-label");

        ir.lblTotal = new Label("R$ 0,00");
        ir.lblTotal.setPrefWidth(110);
        ir.lblTotal.setStyle("-fx-font-weight: bold;");

        Button btnRemover = new Button("x");
        btnRemover.getStyleClass().add("btn-danger");
        btnRemover.setOnAction(e -> removerLinhaItem(ir));

        HBox row = new HBox(6,
                ir.cbProduto, ir.txtDescricao, ir.txtQtd,
                ir.txtPreco, ir.txtDesconto, ir.lblEstoque, ir.lblTotal, btnRemover);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        VBox itemBox = new VBox(2, row, ir.lblAvisoPreco);
        ir.row = itemBox;

        if (itemExistente != null) {
            preencherItemExistente(ir, itemExistente);
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

    private void preencherItemExistente(ItemRow ir, VendaItem itemExistente) {
        ir.carregandoItemExistente = true;
        try {
            ir.cbProduto.setValue(itemExistente.getProduto());
            ir.produtoAtual = itemExistente.getProduto();
            atualizarEstoque(ir, itemExistente.getProduto());
            ir.txtDescricao.setText(nvl(itemExistente.getDescricao()));
            ir.txtQtd.setText(nvl(itemExistente.getQuantidade()).toPlainString());
            ir.txtPreco.setText(formatDecimal(itemExistente.getPrecoUnitario()));
            ir.txtDesconto.setText(formatDecimal(itemExistente.getDesconto()));
        } finally {
            ir.carregandoItemExistente = false;
        }
        verificarPrecoMinimo(ir);
        recalcularLinha(ir);
    }

    private void filtrarProdutos(ItemRow ir, ObservableList<Produto> produtosDisplay,
                                 boolean[] filtrandoProduto, String texto) {
        if (filtrandoProduto[0]) return;
        Produto selecionado = ir.cbProduto.getValue();
        if (selecionado != null && texto != null && texto.equals(displayProduto(selecionado))) return;

        filtrandoProduto[0] = true;
        try {
            if (texto == null || texto.isBlank()) {
                produtosDisplay.setAll(todosProdutos);
                return;
            }
            String lower = texto.toLowerCase();
            List<Produto> filtrados = todosProdutos.stream()
                    .filter(p -> (p.getDescricao() != null && p.getDescricao().toLowerCase().contains(lower))
                            || (p.getCodigoInterno() != null && p.getCodigoInterno().toLowerCase().contains(lower))
                            || (p.getCodigoBarras() != null && p.getCodigoBarras().toLowerCase().contains(lower)))
                    .toList();
            produtosDisplay.setAll(filtrados);
            if (!filtrados.isEmpty() && !ir.cbProduto.isShowing() && ir.cbProduto.isFocused()) {
                ir.cbProduto.show();
            }
        } finally {
            filtrandoProduto[0] = false;
        }
    }

    private void aoSelecionarProduto(ItemRow ir, Produto produto) {
        if (produto == null) return;
        ir.produtoAtual = produto;
        atualizarEstoque(ir, produto);
        if (ir.carregandoItemExistente) {
            return;
        }
        ir.txtDescricao.setText(produto.getDescricao());
        ir.txtPreco.setText(formatDecimal(produto.getPrecoVenda()));
        verificarPrecoMinimo(ir);
        recalcularLinha(ir);
    }

    private void atualizarEstoque(ItemRow ir, Produto produto) {
        if (produto == null) {
            ir.lblEstoque.setText("Estoque: -");
            return;
        }
        ir.lblEstoque.setText("Estoque: " + nvl(produto.getEstoqueAtual()).stripTrailingZeros().toPlainString());
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
        BigDecimal qtd = parseBD(ir.txtQtd.getText());
        BigDecimal preco = parseBD(ir.txtPreco.getText());
        BigDecimal desconto = parseBD(ir.txtDesconto.getText());
        BigDecimal total = qtd.multiply(preco).subtract(desconto).max(BigDecimal.ZERO);
        ir.item.setQuantidade(qtd);
        ir.item.setPrecoUnitario(preco);
        ir.item.setDesconto(desconto);
        ir.item.setValorTotal(total);
        ir.lblTotal.setText(MoneyUtils.formatCurrency(total));
        recalcularTotal();
    }

    private void recalcularTotal() {
        BigDecimal subtotal = itemRows.stream()
                .map(r -> r.item.getValorTotal() != null ? r.item.getValorTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desconto = parseBD(txtDescontoGlobal.getText());
        lblSubtotal.setText(MoneyUtils.formatCurrency(subtotal));
        lblDesconto.setText(MoneyUtils.formatCurrency(desconto));
        lblTotal.setText(MoneyUtils.formatCurrency(subtotal.subtract(desconto).max(BigDecimal.ZERO)));
    }

    @FXML
    private void finalizarVenda() {
        try {
            Venda venda = montarVenda();
            Venda salva = vendaService.registrarVendaDireta(
                    venda, cmbFormaPagamento.getValue(), spnParcelas.getValue());
            new Alert(Alert.AlertType.INFORMATION,
                    "Venda finalizada: " + salva.getNumero()
                            + "\nEstoque atualizado e pagamento registrado.",
                    ButtonType.OK).showAndWait();
            voltar();
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao finalizar venda", e);
            new Alert(Alert.AlertType.ERROR, "Erro ao finalizar venda: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void voltar() {
        if (conteudoPane == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vendas.fxml"));
            loader.setClassLoader(getClass().getClassLoader());
            loader.setControllerFactory(springContext::getBean);
            Parent view = loader.load();
            conteudoPane.getChildren().setAll(view);
        } catch (Exception e) {
            log.error("Erro ao voltar para lista de vendas", e);
        }
    }

    private Venda montarVenda() {
        Venda venda = new Venda();
        venda.setEmpresa(authService.getEmpresaLogada());
        venda.setCliente(cmbCliente.getValue());
        venda.setVendedor(cmbVendedor.getValue());
        LocalDate data = dpDataVenda.getValue() != null ? dpDataVenda.getValue() : LocalDate.now();
        venda.setDataVenda(LocalDateTime.of(data, LocalTime.now()));
        venda.setObservacoes(txtObservacoes.getText().isBlank() ? null : txtObservacoes.getText());
        venda.setValorDesconto(parseBD(txtDescontoGlobal.getText()));

        for (ItemRow ir : itemRows) {
            if (ir.cbProduto.getValue() == null) continue;
            VendaItem item = new VendaItem();
            item.setVenda(venda);
            item.setProduto(ir.cbProduto.getValue());
            item.setDescricao(ir.txtDescricao.getText().isBlank()
                    ? ir.cbProduto.getValue().getDescricao() : ir.txtDescricao.getText());
            item.setQuantidade(parseBD(ir.txtQtd.getText()));
            item.setPrecoUnitario(parseBD(ir.txtPreco.getText()));
            item.setDesconto(parseBD(ir.txtDesconto.getText()));
            venda.getItens().add(item);
        }

        vendaService.recalcularTotais(venda);
        return venda;
    }

    private BigDecimal parseBD(String text) {
        return MoneyUtils.parse(text);
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String nvl(String valor) {
        return valor != null ? valor : "";
    }

    private String formatDecimal(BigDecimal valor) {
        return MoneyUtils.formatInput(valor);
    }

    private String nomeCliente(Cliente cliente) {
        if (cliente == null) return "";
        return "PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null
                ? cliente.getRazaoSocial() : nvl(cliente.getNome());
    }

    private String displayProduto(Produto produto) {
        if (produto == null) return "";
        String codigo = produto.getCodigoInterno() != null ? "[" + produto.getCodigoInterno() + "] " : "";
        return codigo + produto.getDescricao();
    }

    private String formatarFormaPagamento(String forma) {
        if (forma == null) return "";
        return switch (forma) {
            case "DINHEIRO" -> "Dinheiro";
            case "PIX" -> "PIX";
            case "CARTAO_CREDITO" -> "Cartão Crédito";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            case "PRAZO" -> "Prazo";
            case "CREDIARIO" -> "Crediário";
            default -> forma;
        };
    }

    private static class ItemRow {
        VendaItem item;
        Produto produtoAtual;
        boolean carregandoItemExistente;
        ComboBox<Produto> cbProduto;
        TextField txtDescricao;
        TextField txtQtd;
        TextField txtPreco;
        TextField txtDesconto;
        Label lblEstoque;
        Label lblTotal;
        Label lblAvisoPreco;
        VBox row;
    }
}
