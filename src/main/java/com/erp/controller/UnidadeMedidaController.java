package com.erp.controller;

import com.erp.model.Empresa;
import com.erp.model.UnidadeMedida;
import com.erp.repository.UnidadeMedidaRepository;
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
 * Controller do modal de gerenciamento de unidades de medida.
 * Layout split: lista à esquerda, formulário à direita.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnidadeMedidaController implements Initializable {

    private final UnidadeMedidaRepository unidadeMedidaRepository;
    private final AuthService authService;

    @FXML private ListView<UnidadeMedida> lstUnidades;
    @FXML private Label     lblFormTitulo;
    @FXML private TextField txtSigla;
    @FXML private Label     lblErrSigla;
    @FXML private TextField txtDescricao;

    private UnidadeMedida unidadeAtual;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarLista();
        limparFormulario();
    }

    private void configurarLista() {
        lstUnidades.getSelectionModel().selectedItemProperty().addListener((obs, old, unidade) -> {
            if (unidade != null) preencherFormulario(unidade);
        });

        carregarLista();
    }

    private void carregarLista() {
        Integer empresaId = authService.getEmpresaIdLogado();
        List<UnidadeMedida> unidades = unidadeMedidaRepository.findByEmpresaIdOrderBySigla(empresaId);
        lstUnidades.getItems().setAll(unidades);
    }

    private void preencherFormulario(UnidadeMedida unidade) {
        unidadeAtual = unidade;
        lblFormTitulo.setText("Editar Unidade");
        txtSigla.setText(unidade.getSigla());
        txtSigla.setDisable(true); // sigla é chave — não permite alterar
        txtDescricao.setText(unidade.getDescricao() != null ? unidade.getDescricao() : "");
        esconderErro(lblErrSigla);
    }

    private void limparFormulario() {
        unidadeAtual = null;
        lblFormTitulo.setText("Nova Unidade");
        txtSigla.clear();
        txtSigla.setDisable(false);
        txtDescricao.clear();
        esconderErro(lblErrSigla);
    }

    @FXML
    private void novaUnidade() {
        lstUnidades.getSelectionModel().clearSelection();
        limparFormulario();
        txtSigla.requestFocus();
    }

    @FXML
    private void excluirUnidade() {
        UnidadeMedida selecionada = lstUnidades.getSelectionModel().getSelectedItem();
        if (selecionada == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Excluir a unidade '" + selecionada.getSigla() + "'?\nProdutos vinculados ficarão sem unidade.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmar exclusão");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    unidadeMedidaRepository.delete(selecionada);
                    carregarLista();
                    limparFormulario();
                } catch (Exception e) {
                    log.error("Erro ao excluir unidade", e);
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
        UnidadeMedida unidade = unidadeAtual != null
            ? unidadeAtual
            : UnidadeMedida.builder().empresa(empresa).build();

        if (unidadeAtual == null) {
            unidade.setSigla(txtSigla.getText().trim().toUpperCase());
        }
        String desc = txtDescricao.getText().trim();
        unidade.setDescricao(desc.isEmpty() ? null : desc);

        try {
            unidadeMedidaRepository.save(unidade);
            carregarLista();
            final String sigla = unidade.getSigla();
            lstUnidades.getItems().stream()
                .filter(u -> u.getSigla().equals(sigla))
                .findFirst()
                .ifPresent(u -> {
                    lstUnidades.getSelectionModel().select(u);
                    lstUnidades.scrollTo(u);
                });
        } catch (Exception e) {
            log.error("Erro ao salvar unidade", e);
            new Alert(Alert.AlertType.ERROR,
                "Erro ao salvar unidade:\n" + e.getMessage(),
                ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        UnidadeMedida selecionada = lstUnidades.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            preencherFormulario(selecionada);
        } else {
            limparFormulario();
        }
    }

    private boolean validar() {
        if (txtSigla.getText() == null || txtSigla.getText().trim().isEmpty()) {
            mostrarErro(lblErrSigla, "Sigla é obrigatória");
            return false;
        }
        if (txtSigla.getText().trim().length() > 10) {
            mostrarErro(lblErrSigla, "Sigla deve ter no máximo 10 caracteres");
            return false;
        }
        esconderErro(lblErrSigla);
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
