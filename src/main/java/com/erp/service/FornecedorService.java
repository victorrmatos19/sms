package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Fornecedor;
import com.erp.repository.FornecedorRepository;
import com.erp.util.CpfCnpjValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos(Integer empresaId) {
        return fornecedorRepository.findByEmpresaIdOrderByNome(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarAtivos(Integer empresaId) {
        return fornecedorRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Fornecedor> buscarPorId(Integer id) {
        return fornecedorRepository.findById(id);
    }

    @Transactional
    public Fornecedor salvar(Fornecedor fornecedor) {
        validar(fornecedor);
        Fornecedor salvo = fornecedorRepository.save(fornecedor);
        log.info("{} fornecedor id={} nome='{}'",
            fornecedor.getId() == null ? "Criado" : "Atualizado",
            salvo.getId(), salvo.getNome());
        return salvo;
    }

    @Transactional
    public void inativar(Integer id) {
        fornecedorRepository.findById(id).ifPresent(f -> {
            f.setAtivo(false);
            fornecedorRepository.save(f);
            log.info("Fornecedor inativado: id={} '{}'", f.getId(), f.getNome());
        });
    }

    @Transactional
    public void ativar(Integer id) {
        fornecedorRepository.findById(id).ifPresent(f -> {
            f.setAtivo(true);
            fornecedorRepository.save(f);
            log.info("Fornecedor ativado: id={} '{}'", f.getId(), f.getNome());
        });
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> buscarComFiltros(Integer empresaId, String busca,
                                              boolean mostrarInativos) {
        return fornecedorRepository.buscarComFiltros(empresaId,
            busca == null ? "" : busca.trim(), mostrarInativos);
    }

    @Transactional(readOnly = true)
    public long contarAtivos(Integer empresaId) {
        return fornecedorRepository.countByEmpresaIdAndAtivoTrue(empresaId);
    }

    @Transactional(readOnly = true)
    public long contarComPix(Integer empresaId) {
        return fornecedorRepository
            .countByEmpresaIdAndBancoPixChaveIsNotNullAndBancoPixChaveNot(empresaId, "");
    }

    // ---- Validação ----

    private void validar(Fornecedor fornecedor) {
        Integer empresaId = fornecedor.getEmpresa().getId();
        String cpfCnpj = CpfCnpjValidator.limpar(fornecedor.getCpfCnpj());

        if (cpfCnpj.isEmpty()) return;

        if ("PF".equals(fornecedor.getTipoPessoa())) {
            if (!CpfCnpjValidator.validarCpf(cpfCnpj)) {
                throw new NegocioException("CPF inválido: " + fornecedor.getCpfCnpj());
            }
        } else {
            if (!CpfCnpjValidator.validarCnpj(cpfCnpj)) {
                throw new NegocioException("CNPJ inválido: " + fornecedor.getCpfCnpj());
            }
        }

        if (fornecedor.getId() == null) {
            if (fornecedorRepository.existsByEmpresaIdAndCpfCnpj(empresaId, cpfCnpj)) {
                throw new NegocioException(
                    ("PF".equals(fornecedor.getTipoPessoa()) ? "CPF" : "CNPJ")
                    + " já cadastrado para outro fornecedor.");
            }
        } else {
            if (fornecedorRepository.existsByEmpresaIdAndCpfCnpjAndIdNot(
                    empresaId, cpfCnpj, fornecedor.getId())) {
                throw new NegocioException(
                    ("PF".equals(fornecedor.getTipoPessoa()) ? "CPF" : "CNPJ")
                    + " já cadastrado para outro fornecedor.");
            }
        }
    }
}
