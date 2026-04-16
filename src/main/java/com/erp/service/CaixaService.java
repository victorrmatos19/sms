package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Caixa;
import com.erp.model.CaixaMovimentacao;
import com.erp.model.CaixaSessao;
import com.erp.model.Empresa;
import com.erp.model.Usuario;
import com.erp.model.Venda;
import com.erp.repository.CaixaMovimentacaoRepository;
import com.erp.repository.CaixaRepository;
import com.erp.repository.CaixaSessaoRepository;
import com.erp.model.dto.caixa.CaixaResumoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CaixaService {

    private static final String STATUS_ABERTO = "ABERTO";
    private static final String STATUS_FECHADO = "FECHADO";
    private static final List<String> TIPOS_ENTRADA = List.of("SUPRIMENTO", "VENDA", "RECEBIMENTO");
    private static final List<String> TIPOS_SAIDA = List.of("SANGRIA", "PAGAMENTO");

    private final CaixaRepository caixaRepository;
    private final CaixaSessaoRepository caixaSessaoRepository;
    private final CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public CaixaResumoDTO obterResumoAtual(Integer empresaId) {
        Optional<CaixaSessao> sessao = buscarSessaoAberta(empresaId);
        return sessao.map(this::montarResumo)
                .orElse(new CaixaResumoDTO(false, null, "Caixa Principal", "—", null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        null, null));
    }

    @Transactional(readOnly = true)
    public List<CaixaMovimentacao> listarMovimentacoesAtuais(Integer empresaId) {
        return buscarSessaoAberta(empresaId)
                .map(s -> caixaMovimentacaoRepository.findBySessaoIdOrderByCriadoEmDesc(s.getId()))
                .orElse(List.of());
    }

    @Transactional
    public CaixaSessao abrirCaixa(Integer empresaId, BigDecimal saldoInicial, String observacoes) {
        if (buscarSessaoAberta(empresaId).isPresent()) {
            throw new NegocioException("Já existe um caixa aberto.");
        }

        Caixa caixa = obterOuCriarCaixaPrincipal(empresaId);
        Usuario usuario = authService.getUsuarioLogado();
        if (usuario == null) {
            throw new NegocioException("Usuário logado não encontrado.");
        }

        CaixaSessao sessao = CaixaSessao.builder()
                .caixa(caixa)
                .usuario(usuario)
                .status(STATUS_ABERTO)
                .dataAbertura(LocalDateTime.now())
                .saldoInicial(nvl(saldoInicial))
                .observacoes(blankToNull(observacoes))
                .build();
        return caixaSessaoRepository.save(sessao);
    }

    @Transactional
    public CaixaMovimentacao registrarMovimentacaoManual(Integer empresaId, String tipo,
                                                         BigDecimal valor, String descricao,
                                                         String formaPagamento) {
        if (!"SUPRIMENTO".equals(tipo) && !"SANGRIA".equals(tipo)) {
            throw new NegocioException("Tipo de movimentação inválido.");
        }
        CaixaSessao sessao = buscarSessaoAberta(empresaId)
                .orElseThrow(() -> new NegocioException("Abra o caixa antes de registrar movimentações."));
        validarValor(valor);
        if (descricao == null || descricao.isBlank()) {
            throw new NegocioException("Descrição é obrigatória.");
        }

        CaixaMovimentacao mov = CaixaMovimentacao.builder()
                .sessao(sessao)
                .tipo(tipo)
                .descricao(descricao.trim())
                .valor(valor)
                .formaPagamento(blankToNull(formaPagamento))
                .origem("MANUAL")
                .usuario(authService.getUsuarioLogado())
                .build();
        return caixaMovimentacaoRepository.save(mov);
    }

    @Transactional
    public CaixaSessao fecharCaixa(Integer empresaId, BigDecimal saldoFinalInformado, String observacoes) {
        CaixaSessao sessao = buscarSessaoAberta(empresaId)
                .orElseThrow(() -> new NegocioException("Não há caixa aberto para fechar."));
        BigDecimal calculado = calcularSaldoAtual(sessao);
        BigDecimal informado = nvl(saldoFinalInformado);

        sessao.setStatus(STATUS_FECHADO);
        sessao.setDataFechamento(LocalDateTime.now());
        sessao.setSaldoFinalCalculado(calculado);
        sessao.setSaldoFinalInformado(informado);
        sessao.setDiferenca(informado.subtract(calculado));
        if (observacoes != null && !observacoes.isBlank()) {
            sessao.setObservacoes(observacoes.trim());
        }
        return caixaSessaoRepository.save(sessao);
    }

    @Transactional
    public void registrarVendaSeCaixaAberto(Venda venda, String formaPagamento) {
        if (venda == null || venda.getEmpresa() == null || venda.getEmpresa().getId() == null) {
            return;
        }
        if ("PRAZO".equals(formaPagamento) || "CREDIARIO".equals(formaPagamento)) {
            return;
        }
        BigDecimal valor = nvl(venda.getValorTotal());
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        buscarSessaoAberta(venda.getEmpresa().getId()).ifPresent(sessao ->
                caixaMovimentacaoRepository.save(CaixaMovimentacao.builder()
                        .sessao(sessao)
                        .tipo("VENDA")
                        .descricao("Venda " + nvl(venda.getNumero()))
                        .valor(valor)
                        .formaPagamento(blankToNull(formaPagamento))
                        .origem("VENDA")
                        .origemId(venda.getId())
                        .usuario(venda.getUsuario() != null ? venda.getUsuario() : authService.getUsuarioLogado())
                        .build()));
    }

    private Optional<CaixaSessao> buscarSessaoAberta(Integer empresaId) {
        return caixaSessaoRepository.findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(
                empresaId, STATUS_ABERTO);
    }

    private Caixa obterOuCriarCaixaPrincipal(Integer empresaId) {
        return caixaRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(empresaId)
                .orElseGet(() -> caixaRepository.save(Caixa.builder()
                        .empresa(Empresa.builder().id(empresaId).build())
                        .nome("Caixa Principal")
                        .ativo(true)
                        .build()));
    }

    private CaixaResumoDTO montarResumo(CaixaSessao sessao) {
        BigDecimal entradas = totalEntradas(sessao.getId());
        BigDecimal saidas = totalSaidas(sessao.getId());
        BigDecimal saldoAtual = nvl(sessao.getSaldoInicial()).add(entradas).subtract(saidas);
        String operador = sessao.getUsuario() != null ? nvl(sessao.getUsuario().getNome()) : "—";
        String caixaNome = sessao.getCaixa() != null ? nvl(sessao.getCaixa().getNome()) : "Caixa";
        return new CaixaResumoDTO(true, sessao.getId(), caixaNome, operador,
                sessao.getDataAbertura(), sessao.getDataFechamento(), nvl(sessao.getSaldoInicial()),
                entradas, saidas, saldoAtual, sessao.getSaldoFinalInformado(), sessao.getDiferenca());
    }

    private BigDecimal calcularSaldoAtual(CaixaSessao sessao) {
        return nvl(sessao.getSaldoInicial()).add(totalEntradas(sessao.getId())).subtract(totalSaidas(sessao.getId()));
    }

    private BigDecimal totalEntradas(Integer sessaoId) {
        return nvl(caixaMovimentacaoRepository.sumValorBySessaoIdAndTipoIn(sessaoId, TIPOS_ENTRADA));
    }

    private BigDecimal totalSaidas(Integer sessaoId) {
        return nvl(caixaMovimentacaoRepository.sumValorBySessaoIdAndTipoIn(sessaoId, TIPOS_SAIDA));
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Valor deve ser maior que zero.");
        }
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String nvl(String valor) {
        return valor != null ? valor : "";
    }

    private String blankToNull(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
