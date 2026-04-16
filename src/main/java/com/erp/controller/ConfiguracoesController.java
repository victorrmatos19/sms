package com.erp.controller;

import com.erp.model.Configuracao;
import com.erp.model.Empresa;
import com.erp.service.AuthService;
import com.erp.service.ConfiguracaoService;
import com.erp.service.ViaCepService;
import com.erp.util.CpfCnpjValidator;
import com.erp.util.MoneyUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracoesController implements Initializable {

    private final ConfiguracaoService configuracaoService;
    private final AuthService authService;
    private final ViaCepService viaCepService;

    @FXML private TextField txtRazaoSocial;
    @FXML private TextField txtNomeFantasia;
    @FXML private TextField txtCnpj;
    @FXML private TextField txtInscricaoEstadual;
    @FXML private TextField txtInscricaoMunicipal;
    @FXML private ComboBox<String> cmbRegimeTributario;
    @FXML private TextField txtCep;
    @FXML private Label lblBuscandoCep;
    @FXML private TextField txtLogradouro;
    @FXML private TextField txtNumero;
    @FXML private TextField txtComplemento;
    @FXML private TextField txtBairro;
    @FXML private TextField txtCidade;
    @FXML private ComboBox<String> cmbUf;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtSite;
    @FXML private ImageView imgLogotipo;
    @FXML private Label lblLogotipo;

    @FXML private CheckBox chkUsaLoteValidade;
    @FXML private CheckBox chkAlertaEstoqueMinimo;
    @FXML private CheckBox chkPermiteVendaEstoqueZero;
    @FXML private Spinner<Integer> spnDiasValidadeOrcamento;
    @FXML private TextField txtJurosMoraPadrao;
    @FXML private TextField txtMultaPadrao;
    @FXML private TextField txtImpressoraPadrao;
    @FXML private CheckBox chkExibirLogotipoImpressao;

    private static final List<String> ESTADOS = List.of(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
        "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
        "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private Empresa empresaAtual;
    private Configuracao configuracaoAtual;
    private byte[] logotipoSelecionado;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarCampos();
        carregarDados();
    }

    private void configurarCampos() {
        cmbRegimeTributario.getItems().setAll("SIMPLES_NACIONAL", "LUCRO_PRESUMIDO", "LUCRO_REAL");
        cmbUf.getItems().setAll(ESTADOS);
        spnDiasValidadeOrcamento.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 30));
        spnDiasValidadeOrcamento.setEditable(true);

        txtCnpj.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) txtCnpj.setText(formatarCnpj(txtCnpj.getText()));
        });
        txtCep.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) txtCep.setText(formatarCep(txtCep.getText()));
        });
        txtJurosMoraPadrao.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) txtJurosMoraPadrao.setText(MoneyUtils.formatInput(parsePercentual(txtJurosMoraPadrao.getText())));
        });
        txtMultaPadrao.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) txtMultaPadrao.setText(MoneyUtils.formatInput(parsePercentual(txtMultaPadrao.getText())));
        });
    }

    private void carregarDados() {
        Integer empresaId = authService.getEmpresaIdLogado();
        if (empresaId == null) {
            mostrarErro("Sessão inválida", "Nenhuma empresa está associada ao usuário logado.");
            return;
        }

        empresaAtual = configuracaoService.buscarEmpresa(empresaId)
            .orElseThrow(() -> new IllegalStateException("Empresa não encontrada: " + empresaId));
        configuracaoAtual = configuracaoService.buscarConfiguracao(empresaId)
            .orElseThrow(() -> new IllegalStateException("Configuração não encontrada: " + empresaId));

        preencherEmpresa();
        preencherConfiguracao();
    }

    private void preencherEmpresa() {
        txtRazaoSocial.setText(nvl(empresaAtual.getRazaoSocial()));
        txtNomeFantasia.setText(nvl(empresaAtual.getNomeFantasia()));
        txtCnpj.setText(formatarCnpj(empresaAtual.getCnpj()));
        txtInscricaoEstadual.setText(nvl(empresaAtual.getInscricaoEstadual()));
        txtInscricaoMunicipal.setText(nvl(empresaAtual.getInscricaoMunicipal()));
        cmbRegimeTributario.setValue(nvl(empresaAtual.getRegimeTributario()).isBlank()
            ? "SIMPLES_NACIONAL" : empresaAtual.getRegimeTributario());

        txtCep.setText(formatarCep(empresaAtual.getCep()));
        txtLogradouro.setText(nvl(empresaAtual.getLogradouro()));
        txtNumero.setText(nvl(empresaAtual.getNumero()));
        txtComplemento.setText(nvl(empresaAtual.getComplemento()));
        txtBairro.setText(nvl(empresaAtual.getBairro()));
        txtCidade.setText(nvl(empresaAtual.getCidade()));
        cmbUf.setValue(empresaAtual.getUf());

        txtTelefone.setText(nvl(empresaAtual.getTelefone()));
        txtEmail.setText(nvl(empresaAtual.getEmail()));
        txtSite.setText(nvl(empresaAtual.getSite()));

        logotipoSelecionado = empresaAtual.getLogotipo();
        atualizarPreviewLogotipo();
    }

    private void preencherConfiguracao() {
        chkUsaLoteValidade.setSelected(Boolean.TRUE.equals(configuracaoAtual.getUsaLoteValidade()));
        chkAlertaEstoqueMinimo.setSelected(Boolean.TRUE.equals(configuracaoAtual.getAlertaEstoqueMinimo()));
        chkPermiteVendaEstoqueZero.setSelected(Boolean.TRUE.equals(configuracaoAtual.getPermiteVendaEstoqueZero()));
        spnDiasValidadeOrcamento.getValueFactory().setValue(
            configuracaoAtual.getDiasValidadeOrcamento() != null
                ? configuracaoAtual.getDiasValidadeOrcamento() : 30);
        txtJurosMoraPadrao.setText(MoneyUtils.formatInput(configuracaoAtual.getJurosMoraPadrao()));
        txtMultaPadrao.setText(MoneyUtils.formatInput(configuracaoAtual.getMultaPadrao()));
        txtImpressoraPadrao.setText(nvl(configuracaoAtual.getImpressoraPadrao()));
        chkExibirLogotipoImpressao.setSelected(
            !Boolean.FALSE.equals(configuracaoAtual.getExibirLogotipoImpressao()));
    }

    @FXML
    private void buscarCep() {
        String cep = CpfCnpjValidator.limpar(txtCep.getText());
        if (cep.length() != 8) {
            mostrarErro("CEP inválido", "Informe um CEP com 8 dígitos.");
            return;
        }
        buscarCepAsync(cep);
    }

    private void buscarCepAsync(String cep) {
        lblBuscandoCep.setVisible(true);
        lblBuscandoCep.setManaged(true);

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
                mostrarErro("CEP não encontrado", "Não foi possível localizar esse CEP no ViaCEP.");
            }
        });

        task.setOnFailed(e -> {
            lblBuscandoCep.setVisible(false);
            lblBuscandoCep.setManaged(false);
            log.warn("Falha ao buscar CEP", task.getException());
            mostrarErro("Erro ao buscar CEP", "Não foi possível consultar o ViaCEP agora.");
        });

        new Thread(task, "viacep-configuracoes-lookup").start();
    }

    private void preencherEndereco(ViaCepService.EnderecoViaCep endereco) {
        txtCep.setText(formatarCep(endereco.cep()));
        txtLogradouro.setText(endereco.logradouro());
        txtBairro.setText(endereco.bairro());
        txtCidade.setText(endereco.cidade());
        if (endereco.uf() != null && ESTADOS.contains(endereco.uf())) {
            cmbUf.setValue(endereco.uf());
        }
        Platform.runLater(txtNumero::requestFocus);
    }

    @FXML
    private void selecionarLogotipo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Logotipo");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));

        File arquivo = fileChooser.showOpenDialog(txtRazaoSocial.getScene().getWindow());
        if (arquivo == null) {
            return;
        }

        try {
            logotipoSelecionado = Files.readAllBytes(arquivo.toPath());
            atualizarPreviewLogotipo();
        } catch (Exception e) {
            log.warn("Erro ao carregar logotipo {}", arquivo.getAbsolutePath(), e);
            mostrarErro("Erro ao carregar imagem", "Não foi possível carregar o arquivo selecionado.");
        }
    }

    @FXML
    private void salvar() {
        if (!validar()) {
            return;
        }

        aplicarEmpresa();
        Empresa empresaSalva = configuracaoService.salvarEmpresa(empresaAtual);
        aplicarConfiguracao(empresaSalva);
        configuracaoAtual = configuracaoService.salvarConfiguracao(configuracaoAtual);
        empresaAtual = empresaSalva;

        mostrarInfo("Configurações salvas", "Os dados da empresa e os parâmetros do sistema foram atualizados.");
    }

    @FXML
    private void cancelar() {
        carregarDados();
    }

    private boolean validar() {
        if (txtRazaoSocial.getText() == null || txtRazaoSocial.getText().trim().isEmpty()) {
            mostrarErro("Campo obrigatório", "Informe a razão social da empresa.");
            txtRazaoSocial.requestFocus();
            return false;
        }

        String cnpj = CpfCnpjValidator.limpar(txtCnpj.getText());
        if (cnpj.length() != 14) {
            mostrarErro("CNPJ obrigatório", "Informe um CNPJ com 14 dígitos.");
            txtCnpj.requestFocus();
            return false;
        }

        if (cmbRegimeTributario.getValue() == null || cmbRegimeTributario.getValue().isBlank()) {
            mostrarErro("Campo obrigatório", "Selecione o regime tributário.");
            cmbRegimeTributario.requestFocus();
            return false;
        }

        return true;
    }

    private void aplicarEmpresa() {
        empresaAtual.setRazaoSocial(trim(txtRazaoSocial.getText()));
        empresaAtual.setNomeFantasia(blankToNull(txtNomeFantasia.getText()));
        empresaAtual.setCnpj(formatarCnpj(txtCnpj.getText()));
        empresaAtual.setInscricaoEstadual(blankToNull(txtInscricaoEstadual.getText()));
        empresaAtual.setInscricaoMunicipal(blankToNull(txtInscricaoMunicipal.getText()));
        empresaAtual.setRegimeTributario(cmbRegimeTributario.getValue());

        empresaAtual.setCep(blankToNull(formatarCep(txtCep.getText())));
        empresaAtual.setLogradouro(blankToNull(txtLogradouro.getText()));
        empresaAtual.setNumero(blankToNull(txtNumero.getText()));
        empresaAtual.setComplemento(blankToNull(txtComplemento.getText()));
        empresaAtual.setBairro(blankToNull(txtBairro.getText()));
        empresaAtual.setCidade(blankToNull(txtCidade.getText()));
        empresaAtual.setUf(cmbUf.getValue());

        empresaAtual.setTelefone(blankToNull(txtTelefone.getText()));
        empresaAtual.setEmail(blankToNull(txtEmail.getText()));
        empresaAtual.setSite(blankToNull(txtSite.getText()));
        empresaAtual.setLogotipo(logotipoSelecionado);
    }

    private void aplicarConfiguracao(Empresa empresaSalva) {
        configuracaoAtual.setEmpresa(empresaSalva);
        configuracaoAtual.setUsaLoteValidade(chkUsaLoteValidade.isSelected());
        configuracaoAtual.setAlertaEstoqueMinimo(chkAlertaEstoqueMinimo.isSelected());
        configuracaoAtual.setPermiteVendaEstoqueZero(chkPermiteVendaEstoqueZero.isSelected());
        configuracaoAtual.setDiasValidadeOrcamento(spnDiasValidadeOrcamento.getValue());
        configuracaoAtual.setJurosMoraPadrao(parsePercentual(txtJurosMoraPadrao.getText()));
        configuracaoAtual.setMultaPadrao(parsePercentual(txtMultaPadrao.getText()));
        configuracaoAtual.setImpressoraPadrao(blankToNull(txtImpressoraPadrao.getText()));
        configuracaoAtual.setExibirLogotipoImpressao(chkExibirLogotipoImpressao.isSelected());
    }

    private void atualizarPreviewLogotipo() {
        if (logotipoSelecionado == null || logotipoSelecionado.length == 0) {
            imgLogotipo.setImage(null);
            lblLogotipo.setText("Nenhuma imagem selecionada");
            return;
        }

        imgLogotipo.setImage(new Image(new ByteArrayInputStream(logotipoSelecionado)));
        lblLogotipo.setText("Logotipo carregado");
    }

    private BigDecimal parsePercentual(String text) {
        return MoneyUtils.parse(text).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatarCnpj(String cnpj) {
        return CpfCnpjValidator.formatarCnpj(nvl(cnpj));
    }

    private String formatarCep(String cep) {
        String digits = CpfCnpjValidator.limpar(cep);
        if (digits.length() != 8) return nvl(cep);
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private void mostrarInfo(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void mostrarErro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
