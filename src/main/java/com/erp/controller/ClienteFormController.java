package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Cliente;
import com.erp.service.AuthService;
import com.erp.service.ClienteService;
import com.erp.service.ViaCepService;
import com.erp.util.CpfCnpjValidator;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Controller do modal de cadastro/edição de cliente.
 * Suporta PF e PJ com alternância dinâmica de campos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteFormController implements Initializable {

    private final ClienteService clienteService;
    private final ViaCepService viaCepService;
    private final AuthService authService;

    // --- Cabeçalho ---
    @FXML private Label lblTitulo;

    // --- Seletor PF/PJ ---
    @FXML private ToggleButton btnPF;
    @FXML private ToggleButton btnPJ;

    // --- Painéis exclusivos ---
    @FXML private VBox panePF;
    @FXML private VBox panePJ;

    // --- Campos PF ---
    @FXML private TextField txtCpf;
    @FXML private Label     lblErrCpf;
    @FXML private TextField txtRg;

    // --- Campos PJ ---
    @FXML private TextField txtCnpj;
    @FXML private Label     lblErrCnpj;
    @FXML private TextField txtRazaoSocial;
    @FXML private Label     lblErrRazaoSocial;
    @FXML private TextField txtIe;

    // --- Campos comuns ---
    @FXML private Label     lblNome;
    @FXML private TextField txtNome;
    @FXML private Label     lblErrNome;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtCelular;
    @FXML private TextField txtEmail;
    @FXML private Label     lblErrEmail;
    @FXML private TextField txtLimiteCredito;
    @FXML private TextArea  txtObservacoes;
    @FXML private ToggleButton btnStatus;

    // --- Endereço ---
    @FXML private TextField txtCep;
    @FXML private Label     lblErrCep;
    @FXML private Label     lblBuscandoCep;
    @FXML private TextField txtLogradouro;
    @FXML private TextField txtNumero;
    @FXML private TextField txtComplemento;
    @FXML private TextField txtBairro;
    @FXML private TextField txtCidade;
    @FXML private ComboBox<String> cmbUf;

    private static final List<String> ESTADOS = List.of(
        "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
        "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
        "RS","RO","RR","SC","SP","SE","TO"
    );

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$");

    private Cliente clienteAtual;
    private Runnable onSaved;

    // ---- Configuração pública ----

    public void setCliente(Cliente cliente) {
        this.clienteAtual = cliente;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    // ---- Inicialização ----

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarUf();
        configurarTogglePFPJ();
        configurarToggleStatus();
        configurarMascaras();
        configurarValidacaoFocoCpfCnpj();
        configurarBuscaCep();

        Platform.runLater(this::preencherFormulario);
    }

    private void configurarUf() {
        cmbUf.getItems().addAll(ESTADOS);
    }

    private void configurarTogglePFPJ() {
        btnPF.setOnAction(e -> alternarTipo("PF"));
        btnPJ.setOnAction(e -> alternarTipo("PJ"));

        // Impede deselecionar sem selecionar o outro
        btnPF.setOnMouseClicked(e -> { if (!btnPF.isSelected()) btnPF.setSelected(true); });
        btnPJ.setOnMouseClicked(e -> { if (!btnPJ.isSelected()) btnPJ.setSelected(true); });
    }

    private void configurarToggleStatus() {
        btnStatus.selectedProperty().addListener((obs, old, sel) ->
            btnStatus.setText(Boolean.TRUE.equals(sel) ? "Ativo" : "Inativo"));
    }

    private void configurarMascaras() {
        // CPF: 000.000.000-00
        txtCpf.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("\\D", "");
            if (digits.length() > 11) digits = digits.substring(0, 11);
            String masked = aplicarMascaraCpf(digits);
            if (!masked.equals(val)) {
                String finalDigits = digits;
                Platform.runLater(() -> {
                    txtCpf.setText(aplicarMascaraCpf(finalDigits));
                    txtCpf.positionCaret(txtCpf.getText().length());
                });
            }
        });

        // CNPJ: 00.000.000/0000-00
        txtCnpj.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("\\D", "");
            if (digits.length() > 14) digits = digits.substring(0, 14);
            String masked = aplicarMascaraCnpj(digits);
            if (!masked.equals(val)) {
                String finalDigits = digits;
                Platform.runLater(() -> {
                    txtCnpj.setText(aplicarMascaraCnpj(finalDigits));
                    txtCnpj.positionCaret(txtCnpj.getText().length());
                });
            }
        });

        // Telefone: (00) 0000-0000
        txtTelefone.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("\\D", "");
            if (digits.length() > 10) digits = digits.substring(0, 10);
            String masked = aplicarMascaraTelefone(digits);
            if (!masked.equals(val)) {
                String finalDigits = digits;
                Platform.runLater(() -> {
                    txtTelefone.setText(aplicarMascaraTelefone(finalDigits));
                    txtTelefone.positionCaret(txtTelefone.getText().length());
                });
            }
        });

        // Celular: (00) 00000-0000
        txtCelular.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("\\D", "");
            if (digits.length() > 11) digits = digits.substring(0, 11);
            String masked = aplicarMascaraCelular(digits);
            if (!masked.equals(val)) {
                String finalDigits = digits;
                Platform.runLater(() -> {
                    txtCelular.setText(aplicarMascaraCelular(finalDigits));
                    txtCelular.positionCaret(txtCelular.getText().length());
                });
            }
        });

        // CEP: 00000-000
        txtCep.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("\\D", "");
            if (digits.length() > 8) digits = digits.substring(0, 8);
            String masked = digits.length() > 5
                ? digits.substring(0, 5) + "-" + digits.substring(5)
                : digits;
            if (!masked.equals(val)) {
                String finalMasked = masked;
                Platform.runLater(() -> {
                    txtCep.setText(finalMasked);
                    txtCep.positionCaret(txtCep.getText().length());
                });
            }
        });
    }

    private void configurarValidacaoFocoCpfCnpj() {
        txtCpf.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && !txtCpf.getText().trim().isEmpty()) {
                String cpf = CpfCnpjValidator.limpar(txtCpf.getText());
                if (!CpfCnpjValidator.validarCpf(cpf)) {
                    mostrarErro(lblErrCpf, "CPF inválido");
                } else {
                    esconderErro(lblErrCpf);
                }
            }
        });

        txtCnpj.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && !txtCnpj.getText().trim().isEmpty()) {
                String cnpj = CpfCnpjValidator.limpar(txtCnpj.getText());
                if (!CpfCnpjValidator.validarCnpj(cnpj)) {
                    mostrarErro(lblErrCnpj, "CNPJ inválido");
                } else {
                    esconderErro(lblErrCnpj);
                }
            }
        });
    }

    private void configurarBuscaCep() {
        txtCep.textProperty().addListener((obs, old, val) -> {
            String digits = CpfCnpjValidator.limpar(val); // remove traços
            if (digits.length() == 8) {
                buscarCepAsync(digits);
            }
        });
    }

    // ---- Preenchimento ----

    private void preencherFormulario() {
        if (clienteAtual == null) {
            lblTitulo.setText("Novo Cliente");
            btnStatus.setSelected(true);
            alternarTipo("PF");
            return;
        }

        lblTitulo.setText("Editar Cliente — " + clienteAtual.getNome());
        boolean isPj = "PJ".equals(clienteAtual.getTipoPessoa());

        btnPF.setSelected(!isPj);
        btnPJ.setSelected(isPj);
        alternarTipo(clienteAtual.getTipoPessoa());

        txtNome.setText(nvl(clienteAtual.getNome()));
        btnStatus.setSelected(Boolean.TRUE.equals(clienteAtual.getAtivo()));

        if (isPj) {
            txtCnpj.setText(clienteAtual.getCpfCnpj() != null
                ? CpfCnpjValidator.formatarCnpj(clienteAtual.getCpfCnpj()) : "");
            txtRazaoSocial.setText(nvl(clienteAtual.getRazaoSocial()));
            txtIe.setText(nvl(clienteAtual.getRgIe()));
        } else {
            txtCpf.setText(clienteAtual.getCpfCnpj() != null
                ? CpfCnpjValidator.formatarCpf(clienteAtual.getCpfCnpj()) : "");
            txtRg.setText(nvl(clienteAtual.getRgIe()));
        }

        txtTelefone.setText(nvl(clienteAtual.getTelefone()));
        txtCelular.setText(nvl(clienteAtual.getCelular()));
        txtEmail.setText(nvl(clienteAtual.getEmail()));
        txtLimiteCredito.setText(clienteAtual.getLimiteCredito() != null
            ? clienteAtual.getLimiteCredito().toPlainString().replace(".", ",") : "0,00");
        txtObservacoes.setText(nvl(clienteAtual.getObservacoes()));

        // Endereço
        txtCep.setText(nvl(clienteAtual.getCep()));
        txtLogradouro.setText(nvl(clienteAtual.getLogradouro()));
        txtNumero.setText(nvl(clienteAtual.getNumero()));
        txtComplemento.setText(nvl(clienteAtual.getComplemento()));
        txtBairro.setText(nvl(clienteAtual.getBairro()));
        txtCidade.setText(nvl(clienteAtual.getCidade()));
        if (clienteAtual.getUf() != null) {
            cmbUf.setValue(clienteAtual.getUf());
        }
    }

    private void alternarTipo(String tipo) {
        boolean isPj = "PJ".equals(tipo);

        panePF.setVisible(!isPj);
        panePF.setManaged(!isPj);
        panePJ.setVisible(isPj);
        panePJ.setManaged(isPj);

        lblNome.setText(isPj ? "Nome Fantasia *" : "Nome Completo *");

        // Limpar campos do tipo anterior
        if (isPj) {
            txtCpf.clear();
            txtRg.clear();
            esconderErro(lblErrCpf);
        } else {
            txtCnpj.clear();
            txtRazaoSocial.clear();
            txtIe.clear();
            esconderErro(lblErrCnpj);
            esconderErro(lblErrRazaoSocial);
        }
    }

    // ---- Busca de CEP ----

    private void buscarCepAsync(String cep) {
        lblBuscandoCep.setVisible(true);
        lblBuscandoCep.setManaged(true);
        esconderErro(lblErrCep);

        Task<Optional<ViaCepService.EnderecoViaCep>> task = new Task<>() {
            @Override
            protected Optional<ViaCepService.EnderecoViaCep> call() {
                return viaCepService.buscarCep(cep);
            }
        };

        task.setOnSucceeded(e -> {
            lblBuscandoCep.setVisible(false);
            lblBuscandoCep.setManaged(false);
            Optional<ViaCepService.EnderecoViaCep> resultado = task.getValue();
            if (resultado.isPresent()) {
                preencherEndereco(resultado.get());
            } else {
                mostrarErro(lblErrCep, "CEP não encontrado");
            }
        });

        task.setOnFailed(e -> {
            lblBuscandoCep.setVisible(false);
            lblBuscandoCep.setManaged(false);
            log.warn("Falha ao buscar CEP", task.getException());
        });

        new Thread(task, "viacep-lookup").start();
    }

    private void preencherEndereco(ViaCepService.EnderecoViaCep end) {
        txtLogradouro.setText(end.logradouro());
        txtBairro.setText(end.bairro());
        txtCidade.setText(end.cidade());
        if (end.uf() != null && ESTADOS.contains(end.uf())) {
            cmbUf.setValue(end.uf());
        }
        // Foca no campo Número após preencher
        Platform.runLater(txtNumero::requestFocus);
    }

    // ---- Ações ----

    @FXML
    private void salvar() {
        if (!validar()) return;

        boolean isPj = btnPJ.isSelected();

        Cliente cliente = clienteAtual != null
            ? clienteAtual
            : Cliente.builder().empresa(authService.getEmpresaLogada()).build();

        cliente.setTipoPessoa(isPj ? "PJ" : "PF");
        cliente.setNome(txtNome.getText().trim());

        if (isPj) {
            cliente.setCpfCnpj(CpfCnpjValidator.limpar(txtCnpj.getText()));
            cliente.setRazaoSocial(emptyToNull(txtRazaoSocial.getText()));
            cliente.setRgIe(emptyToNull(txtIe.getText()));
        } else {
            cliente.setCpfCnpj(CpfCnpjValidator.limpar(txtCpf.getText()));
            cliente.setRgIe(emptyToNull(txtRg.getText()));
        }

        cliente.setTelefone(emptyToNull(CpfCnpjValidator.limpar(txtTelefone.getText())));
        cliente.setCelular(emptyToNull(CpfCnpjValidator.limpar(txtCelular.getText())));
        cliente.setEmail(emptyToNull(txtEmail.getText()));
        cliente.setLimiteCredito(parseBigDecimal(txtLimiteCredito.getText()));
        // Para novo cliente, crédito disponível = limite
        if (clienteAtual == null) {
            cliente.setCreditoDisponivel(cliente.getLimiteCredito());
        }
        cliente.setObservacoes(emptyToNull(txtObservacoes.getText()));
        cliente.setAtivo(btnStatus.isSelected());

        // Endereço
        String cepLimpo = CpfCnpjValidator.limpar(txtCep.getText());
        cliente.setCep(cepLimpo.isEmpty() ? null : cepLimpo);
        cliente.setLogradouro(emptyToNull(txtLogradouro.getText()));
        cliente.setNumero(emptyToNull(txtNumero.getText()));
        cliente.setComplemento(emptyToNull(txtComplemento.getText()));
        cliente.setBairro(emptyToNull(txtBairro.getText()));
        cliente.setCidade(emptyToNull(txtCidade.getText()));
        cliente.setUf(cmbUf.getValue());

        try {
            clienteService.salvar(cliente);
            if (onSaved != null) onSaved.run();
            fechar();
        } catch (NegocioException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            log.error("Erro ao salvar cliente", e);
            new Alert(Alert.AlertType.ERROR,
                "Erro ao salvar cliente:\n" + e.getMessage(),
                ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void cancelar() {
        fechar();
    }

    private void fechar() {
        Stage stage = (Stage) txtNome.getScene().getWindow();
        stage.close();
    }

    // ---- Validação ----

    private boolean validar() {
        boolean valido = true;
        boolean isPj = btnPJ.isSelected();

        // Nome
        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            mostrarErro(lblErrNome, isPj ? "Nome fantasia é obrigatório" : "Nome é obrigatório");
            valido = false;
        } else {
            esconderErro(lblErrNome);
        }

        if (isPj) {
            // CNPJ
            String cnpj = CpfCnpjValidator.limpar(txtCnpj.getText());
            if (cnpj.isEmpty()) {
                mostrarErro(lblErrCnpj, "CNPJ é obrigatório");
                valido = false;
            } else if (!CpfCnpjValidator.validarCnpj(cnpj)) {
                mostrarErro(lblErrCnpj, "CNPJ inválido");
                valido = false;
            } else {
                esconderErro(lblErrCnpj);
            }
            // Razão Social
            if (txtRazaoSocial.getText() == null || txtRazaoSocial.getText().trim().isEmpty()) {
                mostrarErro(lblErrRazaoSocial, "Razão social é obrigatória");
                valido = false;
            } else {
                esconderErro(lblErrRazaoSocial);
            }
        } else {
            // CPF
            String cpf = CpfCnpjValidator.limpar(txtCpf.getText());
            if (cpf.isEmpty()) {
                mostrarErro(lblErrCpf, "CPF é obrigatório");
                valido = false;
            } else if (!CpfCnpjValidator.validarCpf(cpf)) {
                mostrarErro(lblErrCpf, "CPF inválido");
                valido = false;
            } else {
                esconderErro(lblErrCpf);
            }
        }

        // E-mail (opcional, mas validar formato se preenchido)
        String email = txtEmail.getText();
        if (email != null && !email.trim().isEmpty()
                && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            mostrarErro(lblErrEmail, "E-mail inválido");
            valido = false;
        } else {
            esconderErro(lblErrEmail);
        }

        return valido;
    }

    // ---- Helpers de máscara ----

    private String aplicarMascaraCpf(String digits) {
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 6) return digits.substring(0,3) + "." + digits.substring(3);
        if (digits.length() <= 9) return digits.substring(0,3) + "." + digits.substring(3,6) + "." + digits.substring(6);
        return digits.substring(0,3) + "." + digits.substring(3,6) + "." + digits.substring(6,9) + "-" + digits.substring(9);
    }

    private String aplicarMascaraCnpj(String digits) {
        if (digits.length() <= 2) return digits;
        if (digits.length() <= 5) return digits.substring(0,2) + "." + digits.substring(2);
        if (digits.length() <= 8) return digits.substring(0,2) + "." + digits.substring(2,5) + "." + digits.substring(5);
        if (digits.length() <= 12) return digits.substring(0,2) + "." + digits.substring(2,5) + "." + digits.substring(5,8) + "/" + digits.substring(8);
        return digits.substring(0,2) + "." + digits.substring(2,5) + "." + digits.substring(5,8) + "/" + digits.substring(8,12) + "-" + digits.substring(12);
    }

    private String aplicarMascaraTelefone(String digits) {
        if (digits.length() <= 2) return digits.isEmpty() ? "" : "(" + digits;
        if (digits.length() <= 6) return "(" + digits.substring(0,2) + ") " + digits.substring(2);
        return "(" + digits.substring(0,2) + ") " + digits.substring(2,6) + "-" + digits.substring(6);
    }

    private String aplicarMascaraCelular(String digits) {
        if (digits.length() <= 2) return digits.isEmpty() ? "" : "(" + digits;
        if (digits.length() <= 7) return "(" + digits.substring(0,2) + ") " + digits.substring(2);
        return "(" + digits.substring(0,2) + ") " + digits.substring(2,7) + "-" + digits.substring(7);
    }

    // ---- Helpers gerais ----

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
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

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
