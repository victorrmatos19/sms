package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Funcionario;
import com.erp.repository.FuncionarioRepository;
import com.erp.util.CpfCnpjValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$");

    @Transactional(readOnly = true)
    public List<Funcionario> listarTodos(Integer empresaId) {
        return funcionarioRepository.findByEmpresaIdOrderByNome(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Funcionario> listarAtivos(Integer empresaId) {
        return funcionarioRepository.findByEmpresaIdAndAtivoTrueOrderByNome(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Funcionario> buscarPorId(Integer id) {
        return funcionarioRepository.findById(id);
    }

    @Transactional
    public Funcionario salvar(Funcionario funcionario) {
        validar(funcionario);
        Funcionario salvo = funcionarioRepository.save(funcionario);
        log.info("{} funcionário id={} nome='{}'",
            funcionario.getId() == null ? "Criado" : "Atualizado",
            salvo.getId(), salvo.getNome());
        return salvo;
    }

    @Transactional
    public void inativar(Integer id) {
        funcionarioRepository.findById(id).ifPresent(f -> {
            f.setAtivo(false);
            funcionarioRepository.save(f);
            log.info("Funcionário inativado: id={} '{}'", f.getId(), f.getNome());
        });
    }

    @Transactional
    public void ativar(Integer id) {
        funcionarioRepository.findById(id).ifPresent(f -> {
            f.setAtivo(true);
            funcionarioRepository.save(f);
            log.info("Funcionário ativado: id={} '{}'", f.getId(), f.getNome());
        });
    }

    @Transactional(readOnly = true)
    public List<Funcionario> buscarComFiltros(Integer empresaId, String busca,
                                               boolean mostrarInativos) {
        return funcionarioRepository.buscarComFiltros(empresaId,
            busca == null ? "" : busca.trim(), mostrarInativos);
    }

    @Transactional(readOnly = true)
    public long contarAtivos(Integer empresaId) {
        return funcionarioRepository.countByEmpresaIdAndAtivoTrue(empresaId);
    }

    @Transactional(readOnly = true)
    public long contarComAcesso(Integer empresaId) {
        return funcionarioRepository.countByEmpresaIdAndUsuarioIdIsNotNull(empresaId);
    }

    // ---- Validação ----

    private void validar(Funcionario funcionario) {
        Integer empresaId = funcionario.getEmpresa().getId();

        // CPF — opcional, validar apenas se preenchido
        String cpf = funcionario.getCpf();
        if (cpf != null && !cpf.isBlank()) {
            String cpfLimpo = CpfCnpjValidator.limpar(cpf);
            if (!CpfCnpjValidator.validarCpf(cpfLimpo)) {
                throw new NegocioException("CPF inválido: " + cpf);
            }

            if (funcionario.getId() == null) {
                if (funcionarioRepository.existsByEmpresaIdAndCpf(empresaId, cpfLimpo)) {
                    throw new NegocioException("CPF já cadastrado para outro funcionário.");
                }
            } else {
                if (funcionarioRepository.existsByEmpresaIdAndCpfAndIdNot(
                        empresaId, cpfLimpo, funcionario.getId())) {
                    throw new NegocioException("CPF já cadastrado para outro funcionário.");
                }
            }
        }

        // Email — opcional, validar formato se preenchido
        String email = funcionario.getEmail();
        if (email != null && !email.isBlank()
                && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new NegocioException("Email inválido: " + email);
        }
    }
}
