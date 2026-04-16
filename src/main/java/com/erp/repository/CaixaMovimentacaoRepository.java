package com.erp.repository;

import com.erp.model.CaixaMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface CaixaMovimentacaoRepository extends JpaRepository<CaixaMovimentacao, Integer> {

    List<CaixaMovimentacao> findBySessaoIdOrderByCriadoEmDesc(Integer sessaoId);

    @Query("""
            SELECT COALESCE(SUM(m.valor), 0)
            FROM CaixaMovimentacao m
            WHERE m.sessao.id = :sessaoId
              AND m.tipo IN :tipos
            """)
    BigDecimal sumValorBySessaoIdAndTipoIn(@Param("sessaoId") Integer sessaoId,
                                           @Param("tipos") Collection<String> tipos);
}
