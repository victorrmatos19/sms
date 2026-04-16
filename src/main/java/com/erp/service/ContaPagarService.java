package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.ContaPagar;
import com.erp.model.dto.financeiro.ContasPagarResumoDTO;
import com.erp.repository.ContaPagarRepository;
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
    private final AuthService authService;
    private final CaixaService caixaService;

    @Transactional(readOnly = true)
    public List<ContaPagar> listarPorEmpresa(Integer empresaId) {
        return contaPagarRepository.findByEmpresaIdComRelacionamentos(empresaId);
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

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String blankToNull(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
