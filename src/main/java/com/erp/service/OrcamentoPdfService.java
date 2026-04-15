package com.erp.service;

import com.erp.model.*;
import com.erp.repository.OrcamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrcamentoPdfService {

    private final OrcamentoRepository orcamentoRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public byte[] gerarPdf(Integer orcamentoId) {
        Orcamento orc = orcamentoRepository.findByIdWithItens(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Orçamento não encontrado: " + orcamentoId));
        try {
            InputStream templateStream = getClass().getResourceAsStream("/reports/orcamento.jrxml");
            if (templateStream == null) {
                throw new IllegalStateException("Template orcamento.jrxml não encontrado em /reports/.");
            }
            JasperReport compiled = JasperCompileManager.compileReport(templateStream);
            Map<String, Object> params = montarParametros(orc);
            List<OrcamentoItemReportDTO> itens = montarItens(orc);
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itens);
            JasperPrint print = JasperFillManager.fillReport(compiled, params, dataSource);
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            log.error("Erro ao gerar PDF do orçamento {}", orcamentoId, e);
            throw new RuntimeException("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    public void abrirPdfEmVisualizador(Integer orcamentoId) {
        byte[] pdf = gerarPdf(orcamentoId);
        Orcamento orc = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Orçamento não encontrado: " + orcamentoId));
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            String nomeArquivo = "sms_orc_" + orc.getNumero().replace("/", "-").replace(":", "-") + ".pdf";
            File arquivo = new File(tmpDir, nomeArquivo);
            java.nio.file.Files.write(arquivo.toPath(), pdf);

            abrirArquivo(arquivo);
        } catch (Exception e) {
            log.error("Erro ao abrir PDF do orçamento {}", orcamentoId, e);
            throw new RuntimeException(
                    "PDF gerado, mas não foi possível abrir automaticamente.\nArquivo salvo em: "
                    + new File(System.getProperty("java.io.tmpdir"),
                            "sms_orc_" + orc.getNumero().replace("/", "-").replace(":", "-") + ".pdf")
                            .getAbsolutePath()
                    + "\nMotivo: " + e.getMessage(), e);
        }
    }

    private void abrirArquivo(File arquivo) throws IOException {
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(arquivo);
                return;
            } catch (IOException e) {
                log.warn("Desktop.OPEN falhou para {}. Tentando comando nativo.",
                        arquivo.getAbsolutePath(), e);
            }
        }

        Optional<List<String>> comandoNativo = comandoAberturaNativo(arquivo);
        if (comandoNativo.isPresent()) {
            new ProcessBuilder(comandoNativo.get()).start();
            return;
        }

        log.warn("Nenhum visualizador de PDF disponível. PDF salvo em: {}", arquivo.getAbsolutePath());
        throw new UnsupportedOperationException("Visualizador de PDF não suportado.");
    }

    private Optional<List<String>> comandoAberturaNativo(File arquivo) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String path = arquivo.getAbsolutePath();

        if (os.contains("mac")) {
            return Optional.of(List.of("open", path));
        }
        if (os.contains("win")) {
            return Optional.of(List.of("rundll32", "url.dll,FileProtocolHandler", path));
        }
        if (os.contains("nux") || os.contains("nix")) {
            return Optional.of(List.of("xdg-open", path));
        }
        return Optional.empty();
    }

    private Map<String, Object> montarParametros(Orcamento orc) {
        Map<String, Object> p = new HashMap<>();
        Empresa empresa = orc.getEmpresa();
        p.put("EMPRESA_NOME",      empresa != null ? nvl(empresa.getRazaoSocial()) : "");
        p.put("EMPRESA_CNPJ",      empresa != null ? nvl(empresa.getCnpj()) : "");
        p.put("EMPRESA_TELEFONE",  empresa != null ? nvl(empresa.getTelefone()) : "");
        p.put("EMPRESA_EMAIL",     empresa != null ? nvl(empresa.getEmail()) : "");
        p.put("EMPRESA_LOGOTIPO",  empresa != null ? empresa.getLogotipo() : null);

        p.put("ORC_NUMERO",        nvl(orc.getNumero()));
        p.put("ORC_DATA_EMISSAO",  orc.getDataEmissao() != null ? orc.getDataEmissao().format(DATE_FMT) : "");
        p.put("ORC_DATA_VALIDADE", orc.getDataValidade() != null ? orc.getDataValidade().format(DATE_FMT) : "");
        p.put("ORC_OBSERVACOES",   nvl(orc.getObservacoes()));
        p.put("ORC_SUBTOTAL",      nvl(orc.getValorProdutos()));
        p.put("ORC_DESCONTO",      nvl(orc.getValorDesconto()));
        p.put("ORC_TOTAL",         nvl(orc.getValorTotal()));

        Cliente cliente = orc.getCliente();
        if (cliente != null) {
            String nome = "PJ".equals(cliente.getTipoPessoa()) && cliente.getRazaoSocial() != null
                    ? cliente.getRazaoSocial() : cliente.getNome();
            p.put("CLIENTE_NOME",     nvl(nome));
            p.put("CLIENTE_CPFCNPJ",  nvl(cliente.getCpfCnpj()));
            StringBuilder end = new StringBuilder();
            if (cliente.getLogradouro() != null) {
                end.append(cliente.getLogradouro());
                if (cliente.getNumero() != null) end.append(", ").append(cliente.getNumero());
                if (cliente.getBairro() != null) end.append(" - ").append(cliente.getBairro());
                if (cliente.getCidade() != null) end.append(", ").append(cliente.getCidade());
                if (cliente.getUf() != null) end.append("/").append(cliente.getUf());
            }
            p.put("CLIENTE_ENDERECO", end.toString());
        } else {
            p.put("CLIENTE_NOME",     "");
            p.put("CLIENTE_CPFCNPJ",  "");
            p.put("CLIENTE_ENDERECO", "");
        }

        Funcionario vendedor = orc.getVendedor();
        p.put("VENDEDOR_NOME", vendedor != null ? nvl(vendedor.getNome()) : "");

        return p;
    }

    private List<OrcamentoItemReportDTO> montarItens(Orcamento orc) {
        List<OrcamentoItemReportDTO> dtos = new ArrayList<>();
        int ordem = 1;
        for (OrcamentoItem item : orc.getItens()) {
            String unidade = "";
            if (item.getProduto() != null && item.getProduto().getUnidade() != null) {
                unidade = nvl(item.getProduto().getUnidade().getSigla());
            }
            dtos.add(new OrcamentoItemReportDTO(
                    ordem++,
                    nvl(item.getDescricao()),
                    nvl(item.getQuantidade()),
                    unidade,
                    nvl(item.getPrecoUnitario()),
                    nvl(item.getDesconto()),
                    nvl(item.getValorTotal())
            ));
        }
        return dtos;
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    // DTO para JRBeanCollectionDataSource
    public static class OrcamentoItemReportDTO {
        private final Integer ordem;
        private final String descricao;
        private final BigDecimal quantidade;
        private final String unidade;
        private final BigDecimal precoUnitario;
        private final BigDecimal desconto;
        private final BigDecimal total;

        public OrcamentoItemReportDTO(Integer ordem, String descricao, BigDecimal quantidade,
                                       String unidade, BigDecimal precoUnitario,
                                       BigDecimal desconto, BigDecimal total) {
            this.ordem = ordem;
            this.descricao = descricao;
            this.quantidade = quantidade;
            this.unidade = unidade;
            this.precoUnitario = precoUnitario;
            this.desconto = desconto;
            this.total = total;
        }

        public Integer getOrdem() { return ordem; }
        public String getDescricao() { return descricao; }
        public BigDecimal getQuantidade() { return quantidade; }
        public String getUnidade() { return unidade; }
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public BigDecimal getDesconto() { return desconto; }
        public BigDecimal getTotal() { return total; }
    }
}
