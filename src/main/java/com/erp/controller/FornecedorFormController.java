package com.erp.controller;

import com.erp.exception.NegocioException;
import com.erp.model.Fornecedor;
import com.erp.service.AuthService;
import com.erp.service.FornecedorService;
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

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FornecedorFormController implements Initializable {

    private final FornecedorService fornecedorService;
    private final ViaCepService viaCepService;
    private final AuthService authService;

    // --- Cabeçalho ---
    @FXML private Label lblTitulo;

    // --- Seletor PF/PJ ---
    @FXML private ToggleButton btnPF;
    @FXML private ToggleButton btnPJ;
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

    // --- Campos comuns Aba 1 ---
    @FXML private Label     lblNome;
    @FXML private TextField txtNome;
    @FXML private Label     lblErrNome;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtCelular;
    @FXML private TextField txtEmail;
    @FXML private Label     lblErrEmail;
    @FXML private TextField txtSite;
    @FXML private TextField txtContatoNome;
    @FXML private TextArea  txtObservacoes;
    @FXML private ToggleButton btnStatus;

    // --- Endereço (Aba 2) ---
    @FXML private TextField txtCep;
    @FXML private Label     lblErrCep;
    @FXML private Label     lblBuscandoCep;
    @FXML private TextField txtLogradouro;
    @FXML private TextField txtNumero;
    @FXML private TextField txtComplemento;
    @FXML private TextField txtBairro;
    @FXML private TextField txtCidade;
    @FXML private ComboBox<String> cmbUf;

    // --- Dados Bancários (Aba 3) ---
    @FXML private TextField txtBancoNome;
    @FXML private ComboBox<String> cmbTipoConta;
    @FXML private TextField txtAgencia;
    @FXML private TextField txtConta;
    @FXML private ComboBox<String> cmbPixTipo;
    @FXML private Label     lblErrPixTipo;
    @FXML private TextField txtPixChave;
    @FXML private Label     lblErrPixChave;

    private static final List<String> ESTADOS = List.of(
        "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
        "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
        "RS","RO","RR","SC","SP","SE","TO"
    );

    private static final List<String> TIPOS_CONTA = List.of("Corrente", "Poupança");
    private static final List<String> TIPOS_PIX = List.of(
        "CPF", "CNPJ", "E-mail", "Telefone", "Chave Aleatória");

    /** Placeholders por tipo de chave PIX (key = label do ComboBox). */
    private static final Map<String, String> PIX_PLACEHOLDER = Map.of(
        "CPF",           "000.000.000-00",
        "CNPJ",          "00.000.000/0000-00",
        "E-mail",        "email@exemplo.com",
        "Telefone",      "(00) 00000-0000",
        "Chave Aleatória", "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    );

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$");

    private Fornecedor fornecedorAtual;
    private Runnable onSaved;

    // ---- API pública ----

    public void setFornecedor(Fornecedor fornecedor) { this.fornecedorAtual = fornecedor; }
    public void setOnSaved(Runnable onSaved)         { this.onSaved = onSaved; }

    // ---- Inicialização ----

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbUf.getItems().addAll(ESTADOS);
        cmbTipoConta.getItems().addAll(TIPOS_CONTA);
        cmbPixTipo.getItems().addAll(TIPOS_PIX);

        configurarTogglePFPJ();
        configurarToggleStatus();
        configurarMascaras();
        configurarValidacaoFocoCpfCnpj();
        configurarBuscaCep();
        configurarPixDinamico();

        Platform.runLater(this::preencherFormulario);
    }

    private void configurarTogglePFPJ() {
        btnPF.setOnAction(e -> alternarTipo("PF"));
        btnPJ.setOnAction(e -> alternarTipo("PJ"));
        btnPF.setOnMouseClicked(e -> { if (!btnPF.isSelected()) btnPF.setSelected(true); });
        btnPJ.setOnMouseClicked(e -> { if (!btnPJ.isSelected()) btnPJ.setSelected(true); });
    }

    private void configurarToggleStatus() {
        btnStatus.selectedProperty().addListener((obs, old, sel) ->
            btnStatus.setText(Boolean.TRUE.equals(sel) ? "Ativo" : "Inativo"));
    }

    private void configurarMascaras() {
        txtCpf.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("\\D", "");
            if (d.length() > 11) d = d.substring(0, 11);
            String m = mascaraCpf(d);
            if (!m.equals(val)) { String f = d; Platform.runLater(() -> {
                txtCpf.setText(mascaraCpf(f)); txtCpf.positionCaret(txtCpf.getText().length()); }); }
        });

        txtCnpj.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("\\D", "");
            if (d.length() > 14) d = d.substring(0, 14);
            String m = mascaraCnpj(d);
            if (!m.equals(val)) { String f = d; Platform.runLater(() -> {
                txtCnpj.setText(mascaraCnpj(f)); txtCnpj.positionCaret(txtCnpj.getText().length()); }); }
        });

        txtTelefone.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("\\D", "");
            if (d.length() > 10) d = d.substring(0, 10);
            String m = mascaraTelefone(d);
            if (!m.equals(val)) { String f = d; Platform.runLater(() -> {
                txtTelefone.setText(mascaraTelefone(f)); txtTelefone.positionCaret(txtTelefone.getText().length()); }); }
        });

        txtCelular.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("\\D", "");
            if (d.length() > 11) d = d.substring(0, 11);
            String m = mascaraCelular(d);
            if (!m.equals(val)) { String f = d; Platform.runLater(() -> {
                txtCelular.setText(mascaraCelular(f)); txtCelular.positionCaret(txtCelular.getText().length()); }); }
        });

        txtCep.textProperty().addListener((obs, old, val) -> {
            String d = val.replaceAll("\\D", "");
            if (d.length() > 8) d = d.substring(0, 8);
            String m = d.length() > 5 ? d.substring(0, 5) + "-" + d.substring(5) : d;
            if (!m.equals(val)) { String f = m; Platform.runLater(() -> {
                txtCep.setText(f); txtCep.positionCaret(txtCep.getText().length()); }); }
        });
    }

    private void configurarValidacaoFocoCpfCnpj() {
        txtCpf.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && !txtCpf.getText().trim().isEmpty()) {
                if (!CpfCnpjValidator.validarCpf(CpfCnpjValidator.limpar(txtCpf.getText())))
                    mostrarErro(lblErrCpf, "CPF inválido");
                else esconderErro(lblErrCpf);
            }
        });

        txtCnpj.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && !txtCnpj.getText().trim().isEmpty()) {
                if (!CpfCnpjValidator.validarCnpj(CpfCnpjValidator.limpar(txtCnpj.getText())))
                    mostrarErro(lblErrCnpj, "CNPJ inválido");
                else esconderErro(lblErrCnpj);
            }
        });
    }

    private void configurarBuscaCep() {
        txtCep.textProperty().addListener((obs, old, val) -> {
            if (CpfCnpjValidator.limpar(val).length() == 8)
                buscarCepAsync(CpfCnpjValidator.limpar(val));
        });
    }

    private void configurarPixDinamico() {
        cmbPixTipo.valueProperty().addListener((obs, old, tipo) -> {
            txtPixChave.setDisable(tipo == null);
            txtPixChave.setPromptText(tipo != null
                ? PIX_PLACEHOLDER.getOrDefault(tipo, "") : "Selecione o tipo primeiro");
            txtPixChave.clear();
            esconderErro(lblErrPixTipo);
            esconderErro(lblErrPixChave);
        });
    }

    // ---- Preenchimento ----

    private void preencherFormulario() {
        if (fornecedorAtual == null) {
            lblTitulo.setText("Novo Fornecedor");
            btnStatus.setSelected(true);
            alternarTipo("PF");
            return;
        }

        lblTitulo.setText("Editar Fornecedor — " + fornecedorAtual.getNome());
        boolean isPj = "PJ".equals(fornecedorAtual.getTipoPessoa());
        btnPF.setSelected(!isPj);
        btnPJ.setSelected(isPj);
        alternarTipo(fornecedorAtual.getTipoPessoa());

        txtNome.setText(nvl(fornecedorAtual.getNome()));
        btnStatus.setSelected(Boolean.TRUE.equals(fornecedorAtual.getAtivo()));

        if (isPj) {
            txtCnpj.setText(fornecedorAtual.getCpfCnpj() != null
                ? CpfCnpjValidator.formatarCnpj(fornecedorAtual.getCpfCnpj()) : "");
            txtRazaoSocial.setText(nvl(fornecedorAtual.getRazaoSocial()));
            txtIe.setText(nvl(fornecedorAtual.getInscricaoEstadual()));
        } else {
            txtCpf.setText(fornecedorAtual.getCpfCnpj() != null
                ? CpfCnpjValidator.formatarCpf(fornecedorAtual.getCpfCnpj()) : "");
            txtRg.setText(nvl(fornecedorAtual.getInscricaoEstadual()));
        }

        txtTelefone.setText(nvl(fornecedorAtual.getTelefone()));
        txtCelular.setText(nvl(fornecedorAtual.getCelular()));
        txtEmail.setText(nvl(fornecedorAtual.getEmail()));
        txtSite.setText(nvl(fornecedorAtual.getSite()));
        txtContatoNome.setText(nvl(fornecedorAtual.getContatoNome()));
        txtObservacoes.setText(nvl(fornecedorAtual.getObservacoes()));

        // Endereço
        txtCep.setText(nvl(fornecedorAtual.getCep()));
        txtLogradouro.setText(nvl(fornecedorAtual.getLogradouro()));
        txtNumero.setText(nvl(fornecedorAtual.getNumero()));
        txtComplemento.setText(nvl(fornecedorAtual.getComplemento()));
        txtBairro.setText(nvl(fornecedorAtual.getBairro()));
        txtCidade.setText(nvl(fornecedorAtual.getCidade()));
        if (fornecedorAtual.getUf() != null) cmbUf.setValue(fornecedorAtual.getUf());

        // Bancário
        txtBancoNome.setText(nvl(fornecedorAtual.getBancoNome()));
        if (fornecedorAtual.getBancoTipoConta() != null)
            cmbTipoConta.setValue(fornecedorAtual.getBancoTipoConta());
        txtAgencia.setText(nvl(fornecedorAtual.getBancoAgencia()));
        txtConta.setText(nvl(fornecedorAtual.getBancoConta()));

        // PIX — converter enum interno para label do ComboBox
        if (fornecedorAtual.getBancoPixTipo() != null) {
            String label = pixTipoParaLabel(fornecedorAtual.getBancoPixTipo());
            cmbPixTipo.setValue(label);
        }
        txtPixChave.setText(nvl(fornecedorAtual.getBancoPixChave()));
    }

    private void alternarTipo(String tipo) {
        boolean isPj = "PJ".equals(tipo);
        panePF.setVisible(!isPj);
        panePF.setManaged(!isPj);
        panePJ.setVisible(isPj);
        panePJ.setManaged(isPj);
        lblNome.setText(isPj ? "Nome Fantasia *" : "Nome Completo *");

        if (isPj) { txtCpf.clear(); txtRg.clear(); esconderErro(lblErrCpf); }
        else { txtCnpj.clear(); txtRazaoSocial.clear(); txtIe.clear();
               esconderErro(lblErrCnpj); esconderErro(lblErrRazaoSocial); }
    }

    // ---- Busca CEP ----

    private void buscarCepAsync(String cep) {
        lblBuscandoCep.setVisible(true);
        lblBuscandoCep.setManaged(true);
        esconderErro(lblErrCep);

        Task<Optional<ViaCepService.EnderecoViaCep>> task = new Task<>() {
            @Override protected Optional<ViaCepService.EnderecoViaCep> call() {
                return viaCepService.buscarCep(cep);
            }
        };
        task.setOnSucceeded(e -> {
            lblBuscandoCep.setVisible(false);
            lblBuscandoCep.setManaged(false);
            task.getValue().ifPresentOrElse(
                this::preencherEndereco,
                () -> mostrarErro(lblErrCep, "CEP não encontrado")
            );
        });
        task.setOnFailed(e -> {
            lblBuscandoCep.setVisible(false);
            lblBuscandoCep.setManaged(false);
        });
        new Thread(task, "viacep-lookup").start();
    }

    private void preencherEndereco(ViaCepService.EnderecoViaCep end) {
        txtLogradouro.setText(end.logradouro());
        txtBairro.setText(end.bairro());
        txtCidade.setText(end.cidade());
        if (end.uf() != null && ESTADOS.contains(end.uf())) cmbUf.setValue(end.uf());
        Platform.runLater(txtNumero::requestFocus);
    }

    // ---- Salvar / Cancelar ----

    @FXML
    private void salvar() {
        if (!validar()) return;

        boolean isPj = btnPJ.isSelected();

        Fornecedor f = fornecedorAtual != null
            ? fornecedorAtual
            : Fornecedor.builder().empresa(authService.getEmpresaLogada()).build();

        f.setTipoPessoa(isPj ? "PJ" : "PF");
        f.setNome(txtNome.getText().trim());

        if (isPj) {
            f.setCpfCnpj(CpfCnpjValidator.limpar(txtCnpj.getText()));
            f.setRazaoSocial(emptyToNull(txtRazaoSocial.getText()));
            f.setInscricaoEstadual(emptyToNull(txtIe.getText()));
        } else {
            f.setCpfCnpj(CpfCnpjValidator.limpar(txtCpf.getText()));
            f.setInscricaoEstadual(emptyToNull(txtRg.getText()));
        }

        f.setTelefone(emptyToNull(CpfCnpjValidator.limpar(txtTelefone.getText())));
        f.setCelular(emptyToNull(CpfCnpjValidator.limpar(txtCelular.getText())));
        f.setEmail(emptyToNull(txtEmail.getText()));
        f.setSite(emptyToNull(txtSite.getText()));
        f.setContatoNome(emptyToNull(txtContatoNome.getText()));
        f.setObservacoes(emptyToNull(txtObservacoes.getText()));
        f.setAtivo(btnStatus.isSelected());

        // Endereço
        String cepLimpo = CpfCnpjValidator.limpar(txtCep.getText());
        f.setCep(cepLimpo.isEmpty() ? null : cepLimpo);
        f.setLogradouro(emptyToNull(txtLogradouro.getText()));
        f.setNumero(emptyToNull(txtNumero.getText()));
        f.setComplemento(emptyToNull(txtComplemento.getText()));
        f.setBairro(emptyToNull(txtBairro.getText()));
        f.setCidade(emptyToNull(txtCidade.getText()));
        f.setUf(cmbUf.getValue());

        // Bancário
        f.setBancoNome(emptyToNull(txtBancoNome.getText()));
        f.setBancoTipoConta(cmbTipoConta.getValue());
        f.setBancoAgencia(emptyToNull(txtAgencia.getText()));
        f.setBancoConta(emptyToNull(txtConta.getText()));

        String pixTipoLabel = cmbPixTipo.getValue();
        f.setBancoPixTipo(pixTipoLabel != null ? pixLabelParaTipo(pixTipoLabel) : null);
        f.setBancoPixChave(emptyToNull(txtPixChave.getText()));

        try {
            fornecedorService.salvar(f);
            if (onSaved != null) onSaved.run();
            fechar();
        } catch (NegocioException ex) {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        } catch (Exception ex) {
            log.error("Erro ao salvar fornecedor", ex);
            new Alert(Alert.AlertType.ERROR,
                "Erro ao salvar:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML private void cancelar() { fechar(); }

    private void fechar() {
        ((Stage) txtNome.getScene().getWindow()).close();
    }

    // ---- Validação ----

    private boolean validar() {
        boolean ok = true;
        boolean isPj = btnPJ.isSelected();

        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            mostrarErro(lblErrNome, isPj ? "Nome fantasia é obrigatório" : "Nome é obrigatório");
            ok = false;
        } else esconderErro(lblErrNome);

        if (isPj) {
            String cnpj = CpfCnpjValidator.limpar(txtCnpj.getText());
            if (cnpj.isEmpty()) { mostrarErro(lblErrCnpj, "CNPJ é obrigatório"); ok = false; }
            else if (!CpfCnpjValidator.validarCnpj(cnpj)) { mostrarErro(lblErrCnpj, "CNPJ inválido"); ok = false; }
            else esconderErro(lblErrCnpj);

            if (txtRazaoSocial.getText() == null || txtRazaoSocial.getText().trim().isEmpty()) {
                mostrarErro(lblErrRazaoSocial, "Razão social é obrigatória"); ok = false;
            } else esconderErro(lblErrRazaoSocial);
        } else {
            String cpf = CpfCnpjValidator.limpar(txtCpf.getText());
            if (cpf.isEmpty()) { mostrarErro(lblErrCpf, "CPF é obrigatório"); ok = false; }
            else if (!CpfCnpjValidator.validarCpf(cpf)) { mostrarErro(lblErrCpf, "CPF inválido"); ok = false; }
            else esconderErro(lblErrCpf);
        }

        String email = txtEmail.getText();
        if (email != null && !email.trim().isEmpty()
                && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            mostrarErro(lblErrEmail, "E-mail inválido"); ok = false;
        } else esconderErro(lblErrEmail);

        // Validação cruzada PIX
        String pixTipo  = cmbPixTipo.getValue();
        String pixChave = txtPixChave.getText();
        boolean temTipo  = pixTipo != null;
        boolean temChave = pixChave != null && !pixChave.trim().isEmpty();

        if (temTipo && !temChave) {
            mostrarErro(lblErrPixChave, "Chave PIX é obrigatória quando o tipo está selecionado");
            ok = false;
        } else esconderErro(lblErrPixChave);

        if (temChave && !temTipo) {
            mostrarErro(lblErrPixTipo, "Selecione o tipo da chave PIX");
            ok = false;
        } else if (!temTipo) esconderErro(lblErrPixTipo);

        return ok;
    }

    // ---- Helpers de máscara (mesmos algoritmos de ClienteFormController) ----

    private String mascaraCpf(String d) {
        if (d.length() <= 3) return d;
        if (d.length() <= 6) return d.substring(0,3)+"."+d.substring(3);
        if (d.length() <= 9) return d.substring(0,3)+"."+d.substring(3,6)+"."+d.substring(6);
        return d.substring(0,3)+"."+d.substring(3,6)+"."+d.substring(6,9)+"-"+d.substring(9);
    }

    private String mascaraCnpj(String d) {
        if (d.length() <= 2) return d;
        if (d.length() <= 5) return d.substring(0,2)+"."+d.substring(2);
        if (d.length() <= 8) return d.substring(0,2)+"."+d.substring(2,5)+"."+d.substring(5);
        if (d.length() <= 12) return d.substring(0,2)+"."+d.substring(2,5)+"."+d.substring(5,8)+"/"+d.substring(8);
        return d.substring(0,2)+"."+d.substring(2,5)+"."+d.substring(5,8)+"/"+d.substring(8,12)+"-"+d.substring(12);
    }

    private String mascaraTelefone(String d) {
        if (d.length() <= 2) return d.isEmpty() ? "" : "("+d;
        if (d.length() <= 6) return "("+d.substring(0,2)+") "+d.substring(2);
        return "("+d.substring(0,2)+") "+d.substring(2,6)+"-"+d.substring(6);
    }

    private String mascaraCelular(String d) {
        if (d.length() <= 2) return d.isEmpty() ? "" : "("+d;
        if (d.length() <= 7) return "("+d.substring(0,2)+") "+d.substring(2);
        return "("+d.substring(0,2)+") "+d.substring(2,7)+"-"+d.substring(7);
    }

    // ---- Conversão PIX label ↔ valor interno ----

    private String pixLabelParaTipo(String label) {
        return switch (label) {
            case "CPF"            -> "CPF";
            case "CNPJ"           -> "CNPJ";
            case "E-mail"         -> "EMAIL";
            case "Telefone"       -> "TELEFONE";
            case "Chave Aleatória"-> "ALEATORIA";
            default               -> label;
        };
    }

    private String pixTipoParaLabel(String tipo) {
        return switch (tipo) {
            case "CPF"       -> "CPF";
            case "CNPJ"      -> "CNPJ";
            case "EMAIL"     -> "E-mail";
            case "TELEFONE"  -> "Telefone";
            case "ALEATORIA" -> "Chave Aleatória";
            default          -> tipo;
        };
    }

    // ---- Helpers gerais ----

    private void mostrarErro(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void esconderErro(Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false);
    }

    private String nvl(String v) { return v != null ? v : ""; }

    private String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
