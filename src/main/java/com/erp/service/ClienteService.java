package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Cliente;
import com.erp.repository.ClienteRepository;
import com.erp.util.CpfCnpjValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos(Integer empresaId) {
        return clienteRepository.findByEmpresaIdOrderByNome(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarAtivos(Integer empresaId) {
        return clienteRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(Integer id) {
        return clienteRepository.findById(id);
    }

    @Transactional
    public Cliente salvar(Cliente cliente) {
        validar(cliente);

        Cliente salvo = clienteRepository.save(cliente);
        log.info("{} cliente id={} nome='{}'",
            cliente.getId() == null ? "Criado" : "Atualizado",
            salvo.getId(), salvo.getNome());
        return salvo;
    }

    @Transactional
    public void inativar(Integer id) {
        clienteRepository.findById(id).ifPresent(c -> {
            c.setAtivo(false);
            clienteRepository.save(c);
            log.info("Cliente inativado: id={} '{}'", c.getId(), c.getNome());
        });
    }

    @Transactional
    public void ativar(Integer id) {
        clienteRepository.findById(id).ifPresent(c -> {
            c.setAtivo(true);
            clienteRepository.save(c);
            log.info("Cliente ativado: id={} '{}'", c.getId(), c.getNome());
        });
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarComFiltros(Integer empresaId, String busca, boolean mostrarInativos) {
        return clienteRepository.buscarComFiltros(empresaId,
            busca == null ? "" : busca.trim(), mostrarInativos);
    }

    /**
     * Verifica se o valor da venda está dentro do limite de crédito do cliente.
     *
     * @return true se pode prosseguir (limite zero = sem limite), false se ultrapassar
     */
    @Transactional(readOnly = true)
    public boolean verificarLimiteCredito(Integer clienteId, BigDecimal valorVenda) {
        return clienteRepository.findById(clienteId)
            .map(c -> {
                if (c.getLimiteCredito().compareTo(BigDecimal.ZERO) == 0) return true;
                return c.getCreditoDisponivel().compareTo(valorVenda) >= 0;
            })
            .orElse(true);
    }

    @Transactional(readOnly = true)
    public long contarAtivos(Integer empresaId) {
        return clienteRepository.countByEmpresaIdAndAtivoTrue(empresaId);
    }

    @Transactional(readOnly = true)
    public long contarComLimite(Integer empresaId) {
        return clienteRepository.countByEmpresaIdAndLimiteCreditoGreaterThan(empresaId, BigDecimal.ZERO);
    }

    // ---- Validação ----

    private void validar(Cliente cliente) {
        Integer empresaId = cliente.getEmpresa().getId();
        String cpfCnpj = CpfCnpjValidator.limpar(cliente.getCpfCnpj());

        if (cpfCnpj.isEmpty()) return; // CPF/CNPJ é opcional se não informado

        // Validar formato
        if ("PF".equals(cliente.getTipoPessoa())) {
            if (!CpfCnpjValidator.validarCpf(cpfCnpj)) {
                throw new NegocioException("CPF inválido: " + cliente.getCpfCnpj());
            }
        } else {
            if (!CpfCnpjValidator.validarCnpj(cpfCnpj)) {
                throw new NegocioException("CNPJ inválido: " + cliente.getCpfCnpj());
            }
        }

        // Verificar duplicidade
        if (cliente.getId() == null) {
            if (clienteRepository.existsByEmpresaIdAndCpfCnpj(empresaId, cpfCnpj)) {
                throw new NegocioException(
                    ("PF".equals(cliente.getTipoPessoa()) ? "CPF" : "CNPJ")
                    + " já cadastrado para outro cliente.");
            }
        } else {
            if (clienteRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(
                    empresaId, cpfCnpj, cliente.getId())) {
                throw new NegocioException(
                    ("PF".equals(cliente.getTipoPessoa()) ? "CPF" : "CNPJ")
                    + " já cadastrado para outro cliente.");
            }
        }
    }
}
