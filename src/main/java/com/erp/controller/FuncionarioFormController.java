package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Empresa;
import com.erp.model.Funcionario;
import com.erp.service.AuthService;
import com.erp.service.FuncionarioService;
import com.erp.util.CpfCnpjValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
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
public class FuncionarioFormController implements Initializable {

    private final FuncionarioService funcionarioService;
    private final AuthService authService;

    @FXML private Label         lblTitulo;
    @FXML private TextField     txtNome;
    @FXML private Label         lblErrNome;
    @FXML private TextField     txtCpf;
    @FXML private Label         lblErrCpf;
    @FXML private TextField     txtCargo;
    @FXML private TextField     txtTelefone;
    @FXML private TextField     txtEmail;
    @FXML private Label         lblErrEmail;
    @FXML private DatePicker    dpAdmissao;
    @FXML private ToggleButton  btnStatus;
    @FXML private Label         lblAcesso;

    private Funcionario funcionarioAtual;
    private Runnable onSaved;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        aplicarMascaraCpf();
        aplicarMascaraTelefone();
        configurarValidacaoCpf();
    }

    // ---- API pública ----

    public void setFuncionario(Funcionario funcionario) {
        this.funcionarioAtual = funcionario;

        if (funcionario == null) {
            lblTitulo.setText("Novo Funcionário");
            btnStatus.setSelected(true);
            btnStatus.setText("Ativo");
            lblAcesso.setText(
                "Este funcionário não possui acesso ao sistema. " +
                "Para criar um login, acesse o módulo de Usuários.");
        } else {
            lblTitulo.setText("Editar Funcionário — " + funcionario.getNome());
            txtNome.setText(funcionario.getNome());
            txtCpf.setText(funcionario.getCpf() != null
                ? CpfCnpjValidator.formatarCpf(funcionario.getCpf()) : "");
            txtCargo.setText(nvl(funcionario.getCargo()));
            txtTelefone.setText(nvl(funcionario.getTelefone()));
            txtEmail.setText(nvl(funcionario.getEmail()));
            dpAdmissao.setValue(funcionario.getDataAdmissao());

            boolean ativo = Boolean.TRUE.equals(funcionario.getAtivo());
            btnStatus.setSelected(ativo);
            btnStatus.setText(ativo ? "Ativo" : "Inativo");

            if (funcionario.getUsuario() != null) {
                lblAcesso.setText("Acesso ao sistema vinculado: " + funcionario.getUsuario().getLogin());
            } else {
                lblAcesso.setText(
                    "Este funcionário não possui acesso ao sistema. " +
                    "Para criar um login, acesse o módulo de Usuários.");
            }
        }

        // Atualiza texto do ToggleButton ao clicar
        btnStatus.selectedProperty().addListener((obs, old, val) ->
            btnStatus.setText(val ? "Ativo" : "Inativo"));
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    // ---- Ações ----

    @FXML
    private void salvar() {
        if (!validarFormulario()) return;

        try {
            Empresa empresa = authService.getEmpresaLogada();
            String cpfLimpo = CpfCnpjValidator.limpar(txtCpf.getText());

            Funcionario f = funcionarioAtual != null ? funcionarioAtual : Funcionario.builder()
                .empresa(empresa)
                .percentualComissao(BigDecimal.ZERO)
                .build();

            f.setEmpresa(empresa);
            f.setNome(txtNome.getText().trim());
            f.setCpf(cpfLimpo.isEmpty() ? null : cpfLimpo);
            f.setCargo(txtCargo.getText().isBlank() ? null : txtCargo.getText().trim());
            f.setTelefone(txtTelefone.getText().isBlank() ? null : txtTelefone.getText().trim());
            f.setEmail(txtEmail.getText().isBlank() ? null : txtEmail.getText().trim());
            f.setDataAdmissao(dpAdmissao.getValue());
            f.setAtivo(btnStatus.isSelected());
            if (f.getPercentualComissao() == null) f.setPercentualComissao(BigDecimal.ZERO);

            funcionarioService.salvar(f);

            if (onSaved != null) onSaved.run();
            fechar();

        } catch (NegocioException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao salvar funcionário", e);
            mostrarErro("Erro inesperado ao salvar. Tente novamente.");
        }
    }

    @FXML
    private void cancelar() {
        fechar();
    }

    // ---- Validação do formulário ----

    private boolean validarFormulario() {
        boolean valido = true;

        // Nome obrigatório
        if (txtNome.getText() == null || txtNome.getText().isBlank()) {
            lblErrNome.setText("Nome é obrigatório.");
            lblErrNome.setVisible(true);
            lblErrNome.setManaged(true);
            valido = false;
        } else {
            lblErrNome.setVisible(false);
            lblErrNome.setManaged(false);
        }

        // CPF — opcional mas validar se preenchido
        String cpfTexto = txtCpf.getText();
        if (cpfTexto != null && !cpfTexto.isBlank()) {
            String cpfLimpo = CpfCnpjValidator.limpar(cpfTexto);
            if (!CpfCnpjValidator.validarCpf(cpfLimpo)) {
                lblErrCpf.setText("CPF inválido.");
                lblErrCpf.setVisible(true);
                lblErrCpf.setManaged(true);
                valido = false;
            } else {
                lblErrCpf.setVisible(false);
                lblErrCpf.setManaged(false);
            }
        } else {
            lblErrCpf.setVisible(false);
            lblErrCpf.setManaged(false);
        }

        // Email — opcional mas validar formato se preenchido
        String email = txtEmail.getText();
        if (email != null && !email.isBlank()) {
            if (!email.trim().matches("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$")) {
                lblErrEmail.setText("E-mail inválido.");
                lblErrEmail.setVisible(true);
                lblErrEmail.setManaged(true);
                valido = false;
            } else {
                lblErrEmail.setVisible(false);
                lblErrEmail.setManaged(false);
            }
        } else {
            lblErrEmail.setVisible(false);
            lblErrEmail.setManaged(false);
        }

        return valido;
    }

    // ---- Máscaras ----

    private void aplicarMascaraCpf() {
        txtCpf.textProperty().addListener((obs, old, novo) -> {
            if (novo == null) return;
            String digits = novo.replaceAll("\\D", "");
            if (digits.length() > 11) digits = digits.substring(0, 11);
            String formatted = formatarCpf(digits);
            if (!formatted.equals(novo)) {
                String f = formatted;
                Platform.runLater(() -> {
                    txtCpf.setText(f);
                    txtCpf.positionCaret(f.length());
                });
            }
        });
    }

    private void aplicarMascaraTelefone() {
        txtTelefone.textProperty().addListener((obs, old, novo) -> {
            if (novo == null) return;
            String digits = novo.replaceAll("\\D", "");
            if (digits.length() > 10) digits = digits.substring(0, 10);
            String formatted = formatarTelefone(digits);
            if (!formatted.equals(novo)) {
                String f = formatted;
                Platform.runLater(() -> {
                    txtTelefone.setText(f);
                    txtTelefone.positionCaret(f.length());
                });
            }
        });
    }

    private void configurarValidacaoCpf() {
        txtCpf.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String cpf = CpfCnpjValidator.limpar(txtCpf.getText());
                if (!cpf.isEmpty() && !CpfCnpjValidator.validarCpf(cpf)) {
                    lblErrCpf.setText("CPF inválido.");
                    lblErrCpf.setVisible(true);
                    lblErrCpf.setManaged(true);
                } else {
                    lblErrCpf.setVisible(false);
                    lblErrCpf.setManaged(false);
                }
            }
        });
    }

    private String formatarCpf(String digits) {
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 6) return digits.substring(0, 3) + "." + digits.substring(3);
        if (digits.length() <= 9) return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6);
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    private String formatarTelefone(String digits) {
        if (digits.length() <= 2) return digits.isEmpty() ? "" : "(" + digits;
        if (digits.length() <= 6) return "(" + digits.substring(0, 2) + ") " + digits.substring(2);
        return "(" + digits.substring(0, 2) + ") " + digits.substring(2, 6) + "-" + digits.substring(6);
    }

    private void fechar() {
        ((Stage) txtNome.getScene().getWindow()).close();
    }

    private void mostrarErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Erro");
        alert.showAndWait();
    }

    private String nvl(String v) { return v != null ? v : ""; }
}
