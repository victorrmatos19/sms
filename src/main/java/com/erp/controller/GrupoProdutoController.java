package com.erp.controller;

import com.erp.model.Empresa;
import com.erp.model.GrupoProduto;
import com.erp.repository.GrupoProdutoRepository;
import com.erp.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller do modal de gerenciamento de grupos de produtos.
 * Layout split: lista à esquerda, formulário à direita.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrupoProdutoController implements Initializable {

    private final GrupoProdutoRepository grupoProdutoRepository;
    private final AuthService authService;

    @FXML private ListView<GrupoProduto> lstGrupos;
    @FXML private Label   lblFormTitulo;
    @FXML private TextField txtNome;
    @FXML private Label   lblErrNome;
    @FXML private TextArea txtDescricao;
    @FXML private ToggleButton btnStatus;

    private GrupoProduto grupoAtual;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarLista();
        configurarToggle();
        limparFormulario();
    }

    private void configurarLista() {
        lstGrupos.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GrupoProduto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNome() + (Boolean.FALSE.equals(item.getAtivo()) ? " (inativo)" : ""));
                }
            }
        });

        lstGrupos.getSelectionModel().selectedItemProperty().addListener((obs, old, grupo) -> {
            if (grupo != null) preencherFormulario(grupo);
        });

        carregarLista();
    }

    private void configurarToggle() {
        btnStatus.selectedProperty().addListener((obs, old, selected) ->
            btnStatus.setText(Boolean.TRUE.equals(selected) ? "Ativo" : "Inativo"));
    }

    private void carregarLista() {
        Integer empresaId = authService.getEmpresaIdLogado();
        List<GrupoProduto> grupos = grupoProdutoRepository.findByEmpresaIdOrderByNome(empresaId);
        lstGrupos.getItems().setAll(grupos);
    }

    private void preencherFormulario(GrupoProduto grupo) {
        grupoAtual = grupo;
        lblFormTitulo.setText("Editar Grupo");
        txtNome.setText(grupo.getNome());
        txtDescricao.setText(grupo.getDescricao() != null ? grupo.getDescricao() : "");
        btnStatus.setSelected(Boolean.TRUE.equals(grupo.getAtivo()));
        esconderErro(lblErrNome);
    }

    private void limparFormulario() {
        grupoAtual = null;
        lblFormTitulo.setText("Novo Grupo");
        txtNome.clear();
        txtDescricao.clear();
        btnStatus.setSelected(true);
        esconderErro(lblErrNome);
    }

    @FXML
    private void novoGrupo() {
        lstGrupos.getSelectionModel().clearSelection();
        limparFormulario();
        txtNome.requestFocus();
    }

    @FXML
    private void excluirGrupo() {
        GrupoProduto selecionado = lstGrupos.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Excluir o grupo '" + selecionado.getNome() + "'?\nProdutos vinculados perderão o grupo.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmar exclusão");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    grupoProdutoRepository.delete(selecionado);
                    carregarLista();
                    limparFormulario();
                } catch (Exception e) {
                    log.error("Erro ao excluir grupo", e);
                    new Alert(Alert.AlertType.ERROR,
                        "Não foi possível excluir.\nVerifique se há produtos vinculados.",
                        ButtonType.OK).showAndWait();
                }
            }
        });
    }

    @FXML
    private void salvar() {
        if (!validar()) return;

        Empresa empresa = authService.getEmpresaLogada();
        GrupoProduto grupo = grupoAtual != null
            ? grupoAtual
            : GrupoProduto.builder().empresa(empresa).build();

        grupo.setNome(txtNome.getText().trim());
        String desc = txtDescricao.getText().trim();
        grupo.setDescricao(desc.isEmpty() ? null : desc);
        grupo.setAtivo(btnStatus.isSelected());

        try {
            grupoProdutoRepository.save(grupo);
            carregarLista();
            // Reselect saved item
            lstGrupos.getItems().stream()
                .filter(g -> g.getNome().equals(grupo.getNome()))
                .findFirst()
                .ifPresent(g -> {
                    lstGrupos.getSelectionModel().select(g);
                    lstGrupos.scrollTo(g);
                });
        } catch (Exception e) {
            log.error("Erro ao salvar grupo", e);
            new Alert(Alert.AlertType.ERROR,
                "Erro ao salvar grupo:\n" + e.getMessage(),
                ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        GrupoProduto selecionado = lstGrupos.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            preencherFormulario(selecionado);
        } else {
            limparFormulario();
        }
    }

    private boolean validar() {
        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            mostrarErro(lblErrNome, "Nome é obrigatório");
            return false;
        }
        esconderErro(lblErrNome);
        return true;
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
}
