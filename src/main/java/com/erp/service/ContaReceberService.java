package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.ContaReceber;
import com.erp.model.dto.financeiro.ContasReceberResumoDTO;
import com.erp.repository.ContaReceberRepository;
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
public class ContaReceberService {

    private static final String STATUS_ABERTA     = "ABERTA";
    private static final String STATUS_RECEBIDA   = "RECEBIDA";
    private static final String STATUS_CANCELADA  = "CANCELADA";

    private final ContaReceberRepository contaReceberRepository;
    private final AuthService authService;
    private final CaixaService caixaService;
    private final ClienteService clienteService;

    @Transactional(readOnly = true)
    public List<ContaReceber> listarPorEmpresa(Integer empresaId) {
        return contaReceberRepository.findByEmpresaIdComRelacionamentos(empresaId);
    }

    @Transactional(readOnly = true)
    public ContasReceberResumoDTO obterResumo(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes    = inicioMes.plusMonths(1).minusDays(1);

        return new ContasReceberResumoDTO(
                contaReceberRepository.countByEmpresaIdAndStatus(empresaId, STATUS_ABERTA),
                nvl(contaReceberRepository.sumValorByEmpresaIdAndStatus(empresaId, STATUS_ABERTA)),
                contaReceberRepository.countVencidas(empresaId, hoje),
                nvl(contaReceberRepository.sumValorVencidas(empresaId, hoje)),
                contaReceberRepository.countVencendoHoje(empresaId, hoje),
                nvl(contaReceberRepository.sumValorVencendoHoje(empresaId, hoje)),
                contaReceberRepository.countRecebidasPeriodo(empresaId, inicioMes, fimMes),
                nvl(contaReceberRepository.sumValorRecebidoPeriodo(empresaId, inicioMes, fimMes))
        );
    }

    @Transactional
    public ContaReceber baixar(Integer contaId, BigDecimal valorPago, BigDecimal juros,
                               BigDecimal multa, BigDecimal desconto, LocalDate dataRecebimento,
                               String formaRecebimento, String observacoes) {
        ContaReceber conta = contaReceberRepository.findByIdComRelacionamentos(contaId)
                .orElseThrow(() -> new NegocioException("Conta a receber não encontrada."));

        if (STATUS_RECEBIDA.equals(conta.getStatus())) {
            throw new NegocioException("Esta conta já foi recebida.");
        }
        if (STATUS_CANCELADA.equals(conta.getStatus())) {
            throw new NegocioException("Conta cancelada não pode ser baixada.");
        }

        BigDecimal jurosNorm   = nvl(juros);
        BigDecimal multaNorm   = nvl(multa);
        BigDecimal descontoNorm = nvl(desconto);
        BigDecimal totalComAcrescimos = nvl(conta.getValor()).add(jurosNorm).add(multaNorm);
        if (descontoNorm.compareTo(totalComAcrescimos) > 0) {
            throw new NegocioException("Desconto não pode ser maior que o valor da conta.");
        }

        BigDecimal valorCalculado = totalComAcrescimos.subtract(descontoNorm);
        BigDecimal valorPagoNorm  = valorPago != null ? valorPago : valorCalculado;
        if (valorPagoNorm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Valor recebido deve ser maior que zero.");
        }
        if (formaRecebimento == null || formaRecebimento.isBlank()) {
            throw new NegocioException("Forma de recebimento é obrigatória.");
        }

        conta.setValorPago(valorPagoNorm);
        conta.setJuros(jurosNorm);
        conta.setMulta(multaNorm);
        conta.setDesconto(descontoNorm);
        conta.setDataRecebimento(dataRecebimento != null ? dataRecebimento : LocalDate.now());
        conta.setFormaRecebimento(formaRecebimento);
        conta.setObservacoes(blankToNull(observacoes));
        conta.setStatus(STATUS_RECEBIDA);
        conta.setUsuario(authService.getUsuarioLogado());

        ContaReceber salva = contaReceberRepository.save(conta);
        caixaService.registrarRecebimentoContaReceberSeCaixaAberto(salva, valorPagoNorm, formaRecebimento);
        if (salva.getCliente() != null && salva.getCliente().getId() != null) {
            clienteService.ajustarCreditoDisponivel(salva.getCliente().getId(), valorPagoNorm);
        }
        log.info("Conta a receber baixada: id={} valorRecebido={}", salva.getId(), salva.getValorPago());
        return salva;
    }

    @Transactional
    public ContaReceber cancelar(Integer contaId, String observacoes) {
        ContaReceber conta = contaReceberRepository.findByIdComRelacionamentos(contaId)
                .orElseThrow(() -> new NegocioException("Conta a receber não encontrada."));

        if (STATUS_RECEBIDA.equals(conta.getStatus())) {
            throw new NegocioException("Conta recebida não pode ser cancelada.");
        }
        if (STATUS_CANCELADA.equals(conta.getStatus())) {
            throw new NegocioException("Esta conta já está cancelada.");
        }

        conta.setStatus(STATUS_CANCELADA);
        conta.setObservacoes(blankToNull(observacoes));
        conta.setUsuario(authService.getUsuarioLogado());
        ContaReceber salva = contaReceberRepository.save(conta);
        if (salva.getCliente() != null && salva.getCliente().getId() != null) {
            clienteService.ajustarCreditoDisponivel(salva.getCliente().getId(), nvl(salva.getValor()));
        }
        log.info("Conta a receber cancelada: id={}", salva.getId());
        return salva;
    }

    @Transactional
    public ContaReceber cadastrarAvulsa(ContaReceber conta) {
        if (conta.getDescricao() == null || conta.getDescricao().isBlank()) {
            throw new NegocioException("Descrição é obrigatória.");
        }
        if (conta.getDataVencimento() == null) {
            throw new NegocioException("Data de vencimento é obrigatória.");
        }
        if (conta.getValor() == null || conta.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Valor deve ser maior que zero.");
        }
        conta.setStatus(STATUS_ABERTA);
        conta.setUsuario(authService.getUsuarioLogado());
        ContaReceber salva = contaReceberRepository.save(conta);
        log.info("Conta a receber avulsa criada: id={}", salva.getId());
        return salva;
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String blankToNull(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
