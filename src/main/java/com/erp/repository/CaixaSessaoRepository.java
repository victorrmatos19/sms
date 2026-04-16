package com.erp.repository;

import com.erp.model.CaixaSessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaixaSessaoRepository extends JpaRepository<CaixaSessao, Integer> {

    /** Mantido para retrocompatibilidade e para exibir estado atual no dashboard */
    Optional<CaixaSessao> findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(
            Integer empresaId, String status);

    /** Busca sessão aberta pelo operador logado (para múltiplos caixas simultâneos) */
    Optional<CaixaSessao> findByUsuarioIdAndStatus(Integer usuarioId, String status);

    /** Verifica se um caixa específico já tem sessão aberta */
    boolean existsByCaixaIdAndStatus(Integer caixaId, String status);
}
