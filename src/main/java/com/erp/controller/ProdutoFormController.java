package com.erp.controller;

import com.erp.model.*;
import com.erp.repository.CategoriaProdutoRepository;
import com.erp.repository.GrupoProdutoRepository;
import com.erp.repository.UnidadeMedidaRepository;
import com.erp.service.AuthService;
import com.erp.service.ProdutoService;
import com.erp.util.MoneyUtils;
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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller do modal de cadastro/edição de produto.
 * Recebe o produto via {@link #setProduto(Produto)} e notifica via {@link #setOnSaved(Runnable)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProdutoFormController implements Initializable {

    private final ProdutoService produtoService;
    private final GrupoProdutoRepository grupoProdutoRepository;
    private final CategoriaProdutoRepository categoriaProdutoRepository;
    private final UnidadeMedidaRepository unidadeMedidaRepository;
    private final AuthService authService;

    // --- FXML: Cabeçalho ---
    @FXML private Label lblTitulo;

    // --- FXML: Aba Dados Gerais ---
    @FXML private TextField txtDescricao;
    @FXML private Label     lblErrDescricao;
    @FXML private TextField txtDescricaoReduzida;
    @FXML private TextField txtCodigoInterno;
    @FXML private TextField txtCodigoBarras;
    @FXML private ComboBox<GrupoProduto>     cmbGrupo;
    @FXML private ComboBox<CategoriaProduto> cmbCategoria;
    @FXML private ComboBox<UnidadeMedida>    cmbUnidade;
    @FXML private Label                      lblErrUnidade;
    @FXML private ToggleButton               btnStatus;

    // --- FXML: Aba Preços e Estoque ---
    @FXML private TextField txtPrecoCusto;
    @FXML private TextField txtPrecoVenda;
    @FXML private Label     lblErrPrecoVenda;
    @FXML private TextField txtPrecoMinimo;
    @FXML private Label     lblMargem;
    @FXML private TextField txtEstoqueAtual;
    @FXML private TextField txtEstoqueMinimo;
    @FXML private TextField txtEstoqueMaximo;
    @FXML private TextField txtLocalizacao;
    @FXML private CheckBox  chkUsaLote;

    // --- FXML: Aba Fiscal ---
    @FXML private TextField txtNcm;
    @FXML private TextField txtCest;
    @FXML private ComboBox<String> cmbOrigem;
    @FXML private TextArea  txtObservacoes;

    // --- Botões ---
    @FXML private Button btnSalvar;
    @FXML private Button btnCancelar;

    private Produto produtoAtual;
    private Runnable onSaved;

    private static final NumberFormat QUANTITY_INPUT_FORMAT =
        NumberFormat.getInstance(new Locale("pt", "BR"));

    static {
        QUANTITY_INPUT_FORMAT.setMaximumFractionDigits(4);
    }

    // ---- Configuração pública ----

    public void setProduto(Produto produto) {
        this.produtoAtual = produto;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    // ---- Inicialização ----

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        carregarCombos();
        configurarListeners();
        configurarToggleStatus();

        // Preenchimento feito via setProduto() antes do showAndWait(),
        // mas initialize() é chamado no load() — então usamos runLater para garantir.
        javafx.application.Platform.runLater(this::preencherFormulario);
    }

    private void carregarCombos() {
        Integer empresaId = authService.getEmpresaIdLogado();

        // Grupos
        List<GrupoProduto> grupos = grupoProdutoRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
        cmbGrupo.getItems().add(null);
        cmbGrupo.getItems().addAll(grupos);
        cmbGrupo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GrupoProduto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Selecione um grupo..." : item.getNome());
            }
        });

        // Categorias — populadas quando grupo é selecionado
        cmbCategoria.getItems().add(null);
        cmbCategoria.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CategoriaProduto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Selecione uma categoria..." : item.getNome());
            }
        });

        // Unidades
        List<UnidadeMedida> unidades = unidadeMedidaRepository.findByEmpresaIdOrderBySigla(empresaId);
        cmbUnidade.getItems().addAll(unidades);
        cmbUnidade.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(UnidadeMedida item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Selecione a unidade..." : item.getSigla());
            }
        });

        // Origem fiscal
        cmbOrigem.getItems().addAll(
            "0 — Nacional",
            "1 — Estrangeira (importada diretamente)",
            "2 — Estrangeira (adquirida no mercado interno)"
        );
    }

    private void configurarListeners() {
        // Ao mudar grupo, filtra categorias e desabilita se não houver nenhuma
        cmbGrupo.valueProperty().addListener((obs, old, grupo) -> {
            cmbCategoria.getItems().clear();
            cmbCategoria.getItems().add(null);
            if (grupo != null) {
                Integer empresaId = authService.getEmpresaIdLogado();
                List<CategoriaProduto> cats = categoriaProdutoRepository
                    .findByEmpresaIdAndGrupoIdAndAtivoTrueOrderByNome(empresaId, grupo.getId());
                cmbCategoria.getItems().addAll(cats);
                cmbCategoria.setDisable(cats.isEmpty());
            } else {
                cmbCategoria.setDisable(true);
            }
            cmbCategoria.setValue(null);
        });
        cmbCategoria.setDisable(true); // desabilitada até um grupo ser selecionado

        // Recalcula margem ao mudar preço de custo ou venda
        txtPrecoCusto.textProperty().addListener((obs, old, val) -> atualizarMargem());
        txtPrecoVenda.textProperty().addListener((obs, old, val) -> atualizarMargem());
    }

    private void configurarToggleStatus() {
        btnStatus.selectedProperty().addListener((obs, old, selected) ->
            btnStatus.setText(Boolean.TRUE.equals(selected) ? "Ativo" : "Inativo"));
    }

    private void preencherFormulario() {
        if (produtoAtual == null) {
            lblTitulo.setText("Novo Produto");
            btnStatus.setSelected(true);
            return;
        }

        lblTitulo.setText("Editar Produto — " + produtoAtual.getDescricao());

        // Aba 1
        txtDescricao.setText(nvl(produtoAtual.getDescricao()));
        txtDescricaoReduzida.setText(nvl(produtoAtual.getDescricaoReduzida()));
        txtCodigoInterno.setText(nvl(produtoAtual.getCodigoInterno()));
        txtCodigoBarras.setText(nvl(produtoAtual.getCodigoBarras()));

        selecionarNoComboPorId(cmbGrupo, produtoAtual.getGrupo() != null
            ? produtoAtual.getGrupo().getId() : null);

        // Categoria é preenchida após grupo ser setado (listener atualiza os itens)
        if (produtoAtual.getCategoria() != null) {
            final Integer catId = produtoAtual.getCategoria().getId();
            javafx.application.Platform.runLater(() ->
                cmbCategoria.getItems().stream()
                    .filter(c -> c != null && c.getId().equals(catId))
                    .findFirst()
                    .ifPresent(cmbCategoria::setValue));
        }

        selecionarNoComboPorId(cmbUnidade, produtoAtual.getUnidade() != null
            ? produtoAtual.getUnidade().getId() : null);

        btnStatus.setSelected(Boolean.TRUE.equals(produtoAtual.getAtivo()));

        // Aba 2
        txtPrecoCusto.setText(formatarDecimal(produtoAtual.getPrecoCusto()));
        txtPrecoVenda.setText(formatarDecimal(produtoAtual.getPrecoVenda()));
        txtPrecoMinimo.setText(formatarDecimal(produtoAtual.getPrecoMinimo()));
        txtEstoqueAtual.setText(formatarQuantidade(produtoAtual.getEstoqueAtual()));
        txtEstoqueMinimo.setText(formatarQuantidade(produtoAtual.getEstoqueMinimo()));
        txtEstoqueMaximo.setText(produtoAtual.getEstoqueMaximo() != null
            ? formatarQuantidade(produtoAtual.getEstoqueMaximo()) : "");
        txtLocalizacao.setText(nvl(produtoAtual.getLocalizacao()));
        chkUsaLote.setSelected(Boolean.TRUE.equals(produtoAtual.getUsaLoteValidade()));

        // Aba 3
        txtNcm.setText(nvl(produtoAtual.getNcm()));
        txtCest.setText(nvl(produtoAtual.getCest()));
        if (produtoAtual.getOrigem() != null && !produtoAtual.getOrigem().isEmpty()) {
            int origemIdx = Integer.parseInt(produtoAtual.getOrigem());
            if (origemIdx < cmbOrigem.getItems().size()) {
                cmbOrigem.getSelectionModel().select(origemIdx);
            }
        }
        txtObservacoes.setText(nvl(produtoAtual.getObservacoes()));

        atualizarMargem();
    }

    private void atualizarMargem() {
        BigDecimal custo = parseBigDecimal(txtPrecoCusto.getText());
        BigDecimal venda = parseBigDecimal(txtPrecoVenda.getText());
        BigDecimal margem = produtoService.calcularMargem(custo, venda);

        lblMargem.getStyleClass().removeAll("badge-success", "form-error", "form-hint");
        if (margem.compareTo(BigDecimal.ZERO) > 0) {
            lblMargem.setText(formatarDecimal(margem) + "%");
            lblMargem.getStyleClass().add("badge-success");
        } else if (margem.compareTo(BigDecimal.ZERO) < 0) {
            lblMargem.setText(formatarDecimal(margem) + "% ⚠ margem negativa");
            lblMargem.getStyleClass().add("form-error");
        } else {
            lblMargem.setText("—");
            lblMargem.getStyleClass().add("form-hint");
        }
    }

    // ---- Ações ----

    @FXML
    private void salvar() {
        if (!validar()) return;

        Produto produto = produtoAtual != null
            ? produtoAtual
            : Produto.builder().empresa(authService.getEmpresaLogada()).build();

        // Aba 1
        produto.setDescricao(txtDescricao.getText().trim());
        produto.setDescricaoReduzida(emptyToNull(txtDescricaoReduzida.getText()));
        produto.setCodigoInterno(emptyToNull(txtCodigoInterno.getText()));
        produto.setCodigoBarras(emptyToNull(txtCodigoBarras.getText()));
        produto.setGrupo(cmbGrupo.getValue());
        produto.setCategoria(cmbCategoria.getValue());
        produto.setUnidade(cmbUnidade.getValue());
        produto.setAtivo(btnStatus.isSelected());

        // Aba 2
        produto.setPrecoCusto(parseBigDecimal(txtPrecoCusto.getText()));
        produto.setPrecoVenda(parseBigDecimal(txtPrecoVenda.getText()));
        produto.setPrecoMinimo(parseBigDecimal(txtPrecoMinimo.getText()));
        produto.setEstoqueAtual(parseBigDecimal(txtEstoqueAtual.getText()));
        produto.setEstoqueMinimo(parseBigDecimal(txtEstoqueMinimo.getText()));
        String estoqueMaxStr = txtEstoqueMaximo.getText().trim();
        produto.setEstoqueMaximo(estoqueMaxStr.isEmpty() ? null : parseBigDecimal(estoqueMaxStr));
        produto.setLocalizacao(emptyToNull(txtLocalizacao.getText()));
        produto.setUsaLoteValidade(chkUsaLote.isSelected());

        // Aba 3
        produto.setNcm(emptyToNull(txtNcm.getText()));
        produto.setCest(emptyToNull(txtCest.getText()));
        int origemIdx = cmbOrigem.getSelectionModel().getSelectedIndex();
        produto.setOrigem(origemIdx >= 0 ? String.valueOf(origemIdx) : null);
        produto.setObservacoes(emptyToNull(txtObservacoes.getText()));

        try {
            produtoService.salvar(produto);
            if (onSaved != null) onSaved.run();
            fechar();
        } catch (Exception e) {
            log.error("Erro ao salvar produto", e);
            new Alert(Alert.AlertType.ERROR,
                "Erro ao salvar produto:\n" + e.getMessage(),
                ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        fechar();
    }

    private void fechar() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    // ---- Validação ----

    private boolean validar() {
        boolean valido = true;

        // Descrição
        if (txtDescricao.getText() == null || txtDescricao.getText().trim().isEmpty()) {
            mostrarErro(lblErrDescricao, "Descrição é obrigatória");
            valido = false;
        } else {
            esconderErro(lblErrDescricao);
        }

        // Unidade
        if (cmbUnidade.getValue() == null) {
            mostrarErro(lblErrUnidade, "Unidade de medida é obrigatória");
            valido = false;
        } else {
            esconderErro(lblErrUnidade);
        }

        // Preço de Venda
        BigDecimal precoVenda = parseBigDecimal(txtPrecoVenda.getText());
        if (precoVenda.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarErro(lblErrPrecoVenda, "Preço de venda deve ser maior que zero");
            valido = false;
        } else {
            esconderErro(lblErrPrecoVenda);
        }

        return valido;
    }

    private void mostrarErro(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void esconderErro(Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    // ---- Helpers ----

    private <T> void selecionarNoComboPorId(ComboBox<T> combo, Integer id) {
        if (id == null) return;
        for (T item : combo.getItems()) {
            if (item == null) continue;
            try {
                var getId = item.getClass().getMethod("getId");
                if (id.equals(getId.invoke(item))) {
                    combo.setValue(item);
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    private BigDecimal parseBigDecimal(String text) {
        return MoneyUtils.parse(text);
    }

    private String formatarDecimal(BigDecimal value) {
        return MoneyUtils.formatInput(value);
    }

    private String formatarQuantidade(BigDecimal value) {
        return value != null ? QUANTITY_INPUT_FORMAT.format(value) : "0";
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
