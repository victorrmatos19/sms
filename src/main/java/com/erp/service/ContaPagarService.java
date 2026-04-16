package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.ContaPagar;
import com.erp.model.Empresa;
import com.erp.model.dto.financeiro.ContasPagarResumoDTO;
import com.erp.repository.ContaPagarRepository;
import com.erp.repository.EmpresaRepository;
import com.erp.repository.FornecedorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContaPagarService {

    private static final String STATUS_ABERTA = "ABERTA";
    private static final String STATUS_PAGA = "PAGA";
    private static final String STATUS_CANCELADA = "CANCELADA";

    private final ContaPagarRepository contaPagarRepository;
    private final EmpresaRepository empresaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final AuthService authService;
    private final CaixaService caixaService;

    @Transactional(readOnly = true)
    public List<ContaPagar> listarPorEmpresa(Integer empresaId) {
        return contaPagarRepository.findByEmpresaIdComRelacionamentos(empresaId);
    }

    @Transactional(readOnly = true)
    public List<ContaPagar> listarPorPeriodoVencimento(
            Integer empresaId, String status, LocalDate dataInicio, LocalDate dataFim) {
        return contaPagarRepository.findByEmpresaIdAndStatusAndVencimentoBetween(
                empresaId, status, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public List<ContaPagar> listarPorPeriodoEmissao(
            Integer empresaId, String status, LocalDate dataInicio, LocalDate dataFim) {
        return contaPagarRepository.findByEmpresaIdAndStatusAndEmissaoBetween(
                empresaId, status, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public ContasPagarResumoDTO obterResumo(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        return new ContasPagarResumoDTO(
                contaPagarRepository.countByEmpresaIdAndStatus(empresaId, STATUS_ABERTA),
                nvl(contaPagarRepository.sumValorByEmpresaIdAndStatus(empresaId, STATUS_ABERTA)),
                contaPagarRepository.countVencidas(empresaId, hoje),
                nvl(contaPagarRepository.sumValorVencidas(empresaId, hoje)),
                contaPagarRepository.countVencendoHoje(empresaId, hoje),
                nvl(contaPagarRepository.sumValorVencendoHoje(empresaId, hoje)),
                contaPagarRepository.countPagasPeriodo(empresaId, inicioMes, fimMes),
                nvl(contaPagarRepository.sumValorPagoPeriodo(empresaId, inicioMes, fimMes))
        );
    }

    @Transactional
    public ContaPagar baixar(Integer contaId, BigDecimal valorPago, BigDecimal juros,
                             BigDecimal multa, BigDecimal desconto, LocalDate dataPagamento,
                             String formaPagamento, String observacoes) {
        ContaPagar conta = contaPagarRepository.findByIdComRelacionamentos(contaId)
                .orElseThrow(() -> new NegocioException("Conta a pagar não encontrada."));

        if (STATUS_PAGA.equals(conta.getStatus())) {
            throw new NegocioException("Esta conta já está paga.");
        }
        if (STATUS_CANCELADA.equals(conta.getStatus())) {
            throw new NegocioException("Conta cancelada não pode ser baixada.");
        }

        BigDecimal jurosNorm = nvl(juros);
        BigDecimal multaNorm = nvl(multa);
        BigDecimal descontoNorm = nvl(desconto);
        BigDecimal totalComAcrescimos = nvl(conta.getValor()).add(jurosNorm).add(multaNorm);
        if (descontoNorm.compareTo(totalComAcrescimos) > 0) {
            throw new NegocioException("Desconto não pode ser maior que o valor da conta.");
        }

        BigDecimal valorCalculado = totalComAcrescimos.subtract(descontoNorm);
        BigDecimal valorPagoNorm = valorPago != null ? valorPago : valorCalculado;
        if (valorPagoNorm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Valor pago deve ser maior que zero.");
        }
        if (formaPagamento == null || formaPagamento.isBlank()) {
            throw new NegocioException("Forma de pagamento é obrigatória.");
        }

        conta.setValorPago(valorPagoNorm);
        conta.setJuros(jurosNorm);
        conta.setMulta(multaNorm);
        conta.setDesconto(descontoNorm);
        conta.setDataPagamento(dataPagamento != null ? dataPagamento : LocalDate.now());
        conta.setFormaPagamento(formaPagamento);
        conta.setObservacoes(blankToNull(observacoes));
        conta.setStatus(STATUS_PAGA);
        conta.setUsuario(authService.getUsuarioLogado());

        ContaPagar salva = contaPagarRepository.save(conta);
        caixaService.registrarPagamentoContaPagarSeCaixaAberto(salva, valorPagoNorm, formaPagamento);
        log.info("Conta a pagar baixada: id={} valorPago={}", salva.getId(), salva.getValorPago());
        return salva;
    }

    @Transactional
    public ContaPagar editarVencimento(Integer contaId, LocalDate novaDataVencimento) {
        ContaPagar conta = contaPagarRepository.findById(contaId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));
        if (!STATUS_ABERTA.equals(conta.getStatus())) {
            throw new IllegalStateException("Apenas contas ABERTAS podem ter o vencimento alterado.");
        }
        if (novaDataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento não pode ser nula.");
        }

        conta.setDataVencimento(novaDataVencimento);
        ContaPagar salva = contaPagarRepository.save(conta);
        log.info("Vencimento de conta a pagar alterado: id={} novoVencimento={}",
                salva.getId(), salva.getDataVencimento());
        return salva;
    }

    @Transactional
    public void baixarEmLote(List<Integer> contaIds, LocalDate dataPagamento, String formaPagamento) {
        if (contaIds == null || contaIds.isEmpty()) {
            return;
        }
        for (Integer id : contaIds) {
            try {
                ContaPagar conta = contaPagarRepository.findById(id).orElse(null);
                if (conta == null || !STATUS_ABERTA.equals(conta.getStatus())) {
                    continue;
                }

                BigDecimal valorRestante = nvl(conta.getValor()).subtract(nvl(conta.getValorPago()));
                baixar(id, valorRestante, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, dataPagamento, formaPagamento, "Baixa em lote");
            } catch (Exception e) {
                log.warn("Erro ao baixar conta {} em lote: {}", id, e.getMessage());
            }
        }
    }

    @Transactional
    public ContaPagar cadastrarAvulsa(Integer empresaId, Integer fornecedorId,
                                      String descricao, BigDecimal valor,
                                      LocalDate dataEmissao, LocalDate dataVencimento,
                                      Integer numeroParcela, Integer totalParcelas,
                                      String observacoes) {
        validarDespesaAvulsa(descricao, valor, dataVencimento, numeroParcela, totalParcelas);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Empresa não encontrada"));

        ContaPagar conta = new ContaPagar();
        conta.setEmpresa(empresa);
        conta.setDescricao(descricao.trim());
        conta.setValor(valor);
        conta.setValorPago(BigDecimal.ZERO);
        conta.setJuros(BigDecimal.ZERO);
        conta.setMulta(BigDecimal.ZERO);
        conta.setDesconto(BigDecimal.ZERO);
        conta.setDataEmissao(dataEmissao != null ? dataEmissao : LocalDate.now());
        conta.setDataVencimento(dataVencimento);
        conta.setNumeroParcela(numeroParcela != null ? numeroParcela : 1);
        conta.setTotalParcelas(totalParcelas != null ? totalParcelas : 1);
        conta.setStatus(STATUS_ABERTA);
        conta.setObservacoes(blankToNull(observacoes));
        conta.setUsuario(authService.getUsuarioLogado());
        conta.setCompra(null);

        if (fornecedorId != null) {
            fornecedorRepository.findById(fornecedorId).ifPresent(conta::setFornecedor);
        }

        ContaPagar salva = contaPagarRepository.save(conta);
        log.info("Despesa avulsa cadastrada: id={} valor={}", salva.getId(), salva.getValor());
        return salva;
    }

    @Transactional
    public ContaPagar cancelar(Integer contaId, String observacoes) {
        ContaPagar conta = contaPagarRepository.findByIdComRelacionamentos(contaId)
                .orElseThrow(() -> new NegocioException("Conta a pagar não encontrada."));

        if (STATUS_PAGA.equals(conta.getStatus())) {
            throw new NegocioException("Conta paga não pode ser cancelada.");
        }
        if (STATUS_CANCELADA.equals(conta.getStatus())) {
            throw new NegocioException("Esta conta já está cancelada.");
        }

        conta.setStatus(STATUS_CANCELADA);
        conta.setObservacoes(blankToNull(observacoes));
        conta.setUsuario(authService.getUsuarioLogado());
        ContaPagar salva = contaPagarRepository.save(conta);
        log.info("Conta a pagar cancelada: id={}", salva.getId());
        return salva;
    }

    private void validarDespesaAvulsa(String descricao, BigDecimal valor, LocalDate dataVencimento,
                                      Integer numeroParcela, Integer totalParcelas) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new NegocioException("Descrição é obrigatória.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Valor deve ser maior que zero.");
        }
        if (dataVencimento == null) {
            throw new NegocioException("Data de vencimento é obrigatória.");
        }
        int parcela = numeroParcela != null ? numeroParcela : 1;
        int total = totalParcelas != null ? totalParcelas : 1;
        if (parcela < 1 || total < 1 || parcela > total) {
            throw new NegocioException("Número da parcela deve ser menor ou igual ao total de parcelas.");
        }
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String blankToNull(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
