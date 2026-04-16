package com.erp.service;

import com.erp.model.*;
import com.erp.model.dto.relatorio.RelatorioCaixaTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioEstoqueTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioFinanceiroTotaisDTO;
import com.erp.model.dto.relatorio.RelatorioVendasTotaisDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private static final List<String> TIPOS_ENTRADA = List.of("SUPRIMENTO", "VENDA", "RECEBIMENTO");
    private static final List<String> TIPOS_SAIDA = List.of("SANGRIA", "PAGAMENTO");

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Venda> relatorioVendas(Integer empresaId, LocalDate inicio, LocalDate fim,
                                       Integer clienteId, Integer vendedorId, String status) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT v FROM Venda v
                LEFT JOIN FETCH v.cliente
                LEFT JOIN FETCH v.vendedor
                LEFT JOIN FETCH v.pagamentos
                WHERE v.empresa.id = :empresaId
                  AND v.dataVenda BETWEEN :inicio AND :fim
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("empresaId", empresaId);
        params.put("inicio", inicio.atStartOfDay());
        params.put("fim", fim.atTime(LocalTime.MAX));

        if (clienteId != null) {
            jpql.append(" AND v.cliente.id = :clienteId");
            params.put("clienteId", clienteId);
        }
        if (vendedorId != null) {
            jpql.append(" AND v.vendedor.id = :vendedorId");
            params.put("vendedorId", vendedorId);
        }
        if (status != null && !"TODAS".equals(status)) {
            jpql.append(" AND v.status = :status");
            params.put("status", status);
        }
        jpql.append(" ORDER BY v.dataVenda DESC, v.id DESC");

        return aplicarParametros(entityManager.createQuery(jpql.toString(), Venda.class), params).getResultList();
    }

    public RelatorioVendasTotaisDTO totaisVendas(List<Venda> vendas) {
        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal descontos = BigDecimal.ZERO;
        BigDecimal liquido = BigDecimal.ZERO;
        for (Venda venda : vendas) {
            bruto = bruto.add(nvl(venda.getValorProdutos()));
            descontos = descontos.add(nvl(venda.getValorDesconto()));
            liquido = liquido.add(nvl(venda.getValorTotal()));
        }
        return new RelatorioVendasTotaisDTO(vendas.size(), bruto, descontos, liquido);
    }

    @Transactional(readOnly = true)
    public List<Produto> relatorioPosicaoEstoque(Integer empresaId, Integer grupoId,
                                                 String termoBusca, String situacao,
                                                 boolean incluirInativos) {
        String busca = termoBusca == null ? "" : termoBusca.trim();
        List<Produto> produtos = entityManager.createQuery("""
                SELECT DISTINCT p FROM Produto p
                LEFT JOIN FETCH p.grupo
                LEFT JOIN FETCH p.unidade
                WHERE p.empresa.id = :empresaId
                  AND (:incluirInativos = true OR p.ativo = true)
                  AND (:grupoId IS NULL OR p.grupo.id = :grupoId)
                  AND (:busca = ''
                       OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                       OR LOWER(COALESCE(p.codigoInterno, '')) LIKE LOWER(CONCAT('%', :busca, '%'))
                       OR LOWER(COALESCE(p.codigoBarras, '')) LIKE LOWER(CONCAT('%', :busca, '%')))
                ORDER BY p.descricao
                """, Produto.class)
                .setParameter("empresaId", empresaId)
                .setParameter("incluirInativos", incluirInativos)
                .setParameter("grupoId", grupoId)
                .setParameter("busca", busca)
                .getResultList();

        if (situacao == null || "TODOS".equals(situacao)) {
            return produtos;
        }
        return produtos.stream()
                .filter(produto -> situacao.equals(situacaoEstoque(produto)))
                .toList();
    }

    public RelatorioEstoqueTotaisDTO totaisEstoque(List<Produto> produtos) {
        long ativos = produtos.stream().filter(p -> Boolean.TRUE.equals(p.getAtivo())).count();
        long normais = produtos.stream().filter(p -> "NORMAL".equals(situacaoEstoque(p))).count();
        long abaixo = produtos.stream().filter(p -> "ABAIXO_MINIMO".equals(situacaoEstoque(p))).count();
        long zerados = produtos.stream().filter(p -> "ZERADO_NEGATIVO".equals(situacaoEstoque(p))).count();
        BigDecimal valor = produtos.stream()
                .map(p -> nvl(p.getEstoqueAtual()).multiply(nvl(p.getPrecoCusto())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatorioEstoqueTotaisDTO(ativos, normais, abaixo, zerados, valor);
    }

    @Transactional(readOnly = true)
    public List<ContaPagar> relatorioContasPagar(Integer empresaId, LocalDate inicio, LocalDate fim,
                                                 Integer fornecedorId, String status) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT cp FROM ContaPagar cp
                LEFT JOIN FETCH cp.fornecedor
                LEFT JOIN FETCH cp.compra
                WHERE cp.empresa.id = :empresaId
                  AND cp.dataVencimento BETWEEN :inicio AND :fim
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("empresaId", empresaId);
        params.put("inicio", inicio);
        params.put("fim", fim);
        if (fornecedorId != null) {
            jpql.append(" AND cp.fornecedor.id = :fornecedorId");
            params.put("fornecedorId", fornecedorId);
        }
        if (status != null && !"TODOS".equals(status)) {
            if ("VENCIDOS".equals(status)) {
                jpql.append(" AND cp.status = 'ABERTA' AND cp.dataVencimento < :hoje");
                params.put("hoje", LocalDate.now());
            } else {
                jpql.append(" AND cp.status = :status");
                params.put("status", status);
            }
        }
        jpql.append(" ORDER BY cp.dataVencimento ASC, cp.id ASC");
        return aplicarParametros(entityManager.createQuery(jpql.toString(), ContaPagar.class), params).getResultList();
    }

    @Transactional(readOnly = true)
    public List<ContaReceber> relatorioContasReceber(Integer empresaId, LocalDate inicio, LocalDate fim,
                                                     Integer clienteId, String status) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT cr FROM ContaReceber cr
                LEFT JOIN FETCH cr.cliente
                LEFT JOIN FETCH cr.venda
                WHERE cr.empresa.id = :empresaId
                  AND cr.dataVencimento BETWEEN :inicio AND :fim
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("empresaId", empresaId);
        params.put("inicio", inicio);
        params.put("fim", fim);
        if (clienteId != null) {
            jpql.append(" AND cr.cliente.id = :clienteId");
            params.put("clienteId", clienteId);
        }
        if (status != null && !"TODOS".equals(status)) {
            if ("VENCIDOS".equals(status)) {
                jpql.append(" AND cr.status = 'ABERTA' AND cr.dataVencimento < :hoje");
                params.put("hoje", LocalDate.now());
            } else {
                jpql.append(" AND cr.status = :status");
                params.put("status", status);
            }
        }
        jpql.append(" ORDER BY cr.dataVencimento ASC, cr.id ASC");
        return aplicarParametros(entityManager.createQuery(jpql.toString(), ContaReceber.class), params).getResultList();
    }

    public RelatorioFinanceiroTotaisDTO totaisFinanceiro(List<ContaPagar> pagar, List<ContaReceber> receber) {
        LocalDate hoje = LocalDate.now();
        BigDecimal pagarAberto = pagar.stream()
                .filter(c -> "ABERTA".equals(c.getStatus()))
                .map(ContaPagar::getValor)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagarVencido = pagar.stream()
                .filter(c -> "ABERTA".equals(c.getStatus()) && c.getDataVencimento() != null && c.getDataVencimento().isBefore(hoje))
                .map(ContaPagar::getValor)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagarPago = pagar.stream()
                .filter(c -> "PAGA".equals(c.getStatus()))
                .map(ContaPagar::getValorPago)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receberAberto = receber.stream()
                .filter(c -> "ABERTA".equals(c.getStatus()))
                .map(ContaReceber::getValor)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receberVencido = receber.stream()
                .filter(c -> "ABERTA".equals(c.getStatus()) && c.getDataVencimento() != null && c.getDataVencimento().isBefore(hoje))
                .map(ContaReceber::getValor)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receberRecebido = receber.stream()
                .filter(c -> "RECEBIDA".equals(c.getStatus()))
                .map(ContaReceber::getValorPago)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatorioFinanceiroTotaisDTO(
                pagarAberto, pagarVencido, pagarPago, receberAberto, receberVencido, receberRecebido);
    }

    @Transactional(readOnly = true)
    public List<CaixaSessao> relatorioCaixa(Integer empresaId, LocalDate inicio, LocalDate fim,
                                            Integer caixaId, Integer operadorId, String status) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT s FROM CaixaSessao s
                JOIN FETCH s.caixa c
                JOIN FETCH s.usuario u
                WHERE c.empresa.id = :empresaId
                  AND s.dataAbertura BETWEEN :inicio AND :fim
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("empresaId", empresaId);
        params.put("inicio", inicio.atStartOfDay());
        params.put("fim", fim.atTime(LocalTime.MAX));
        if (caixaId != null) {
            jpql.append(" AND c.id = :caixaId");
            params.put("caixaId", caixaId);
        }
        if (operadorId != null) {
            jpql.append(" AND u.id = :operadorId");
            params.put("operadorId", operadorId);
        }
        if (status != null && !"TODOS".equals(status)) {
            jpql.append(" AND s.status = :status");
            params.put("status", status);
        }
        jpql.append(" ORDER BY s.dataAbertura DESC, s.id DESC");
        return aplicarParametros(entityManager.createQuery(jpql.toString(), CaixaSessao.class), params).getResultList();
    }

    @Transactional(readOnly = true)
    public List<CaixaMovimentacao> relatorioMovimentacoesCaixa(Integer sessaoId) {
        return entityManager.createQuery("""
                SELECT m FROM CaixaMovimentacao m
                LEFT JOIN FETCH m.usuario
                WHERE m.sessao.id = :sessaoId
                ORDER BY m.criadoEm ASC, m.id ASC
                """, CaixaMovimentacao.class)
                .setParameter("sessaoId", sessaoId)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public RelatorioCaixaTotaisDTO totaisCaixa(List<CaixaSessao> sessoes) {
        BigDecimal entradas = BigDecimal.ZERO;
        BigDecimal saidas = BigDecimal.ZERO;
        for (CaixaSessao sessao : sessoes) {
            for (CaixaMovimentacao mov : relatorioMovimentacoesCaixa(sessao.getId())) {
                if (TIPOS_ENTRADA.contains(mov.getTipo())) {
                    entradas = entradas.add(nvl(mov.getValor()));
                } else if (TIPOS_SAIDA.contains(mov.getTipo())) {
                    saidas = saidas.add(nvl(mov.getValor()));
                }
            }
        }
        return new RelatorioCaixaTotaisDTO(sessoes.size(), entradas, saidas, entradas.subtract(saidas));
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarClientes(Integer empresaId) {
        return entityManager.createQuery("""
                SELECT c FROM Cliente c
                WHERE c.empresa.id = :empresaId AND c.ativo = true
                ORDER BY c.nome
                """, Cliente.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Funcionario> listarVendedores(Integer empresaId) {
        return entityManager.createQuery("""
                SELECT f FROM Funcionario f
                WHERE f.empresa.id = :empresaId AND f.ativo = true
                ORDER BY f.nome
                """, Funcionario.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<GrupoProduto> listarGrupos(Integer empresaId) {
        return entityManager.createQuery("""
                SELECT g FROM GrupoProduto g
                WHERE g.empresa.id = :empresaId AND g.ativo = true
                ORDER BY g.nome
                """, GrupoProduto.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarFornecedores(Integer empresaId) {
        return entityManager.createQuery("""
                SELECT f FROM Fornecedor f
                WHERE f.empresa.id = :empresaId AND f.ativo = true
                ORDER BY f.nome
                """, Fornecedor.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Caixa> listarCaixas(Integer empresaId) {
        return entityManager.createQuery("""
                SELECT c FROM Caixa c
                WHERE c.empresa.id = :empresaId AND c.ativo = true
                ORDER BY c.nome
                """, Caixa.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios(Integer empresaId) {
        return entityManager.createQuery("""
                SELECT DISTINCT u FROM Usuario u
                LEFT JOIN FETCH u.perfis
                WHERE u.empresa.id = :empresaId AND u.ativo = true
                ORDER BY u.nome
                """, Usuario.class)
                .setParameter("empresaId", empresaId)
                .getResultList();
    }

    public String situacaoEstoque(Produto produto) {
        BigDecimal atual = nvl(produto.getEstoqueAtual());
        if (atual.compareTo(BigDecimal.ZERO) <= 0) {
            return "ZERADO_NEGATIVO";
        }
        if (atual.compareTo(nvl(produto.getEstoqueMinimo())) < 0) {
            return "ABAIXO_MINIMO";
        }
        return "NORMAL";
    }

    private <T> TypedQuery<T> aplicarParametros(TypedQuery<T> query, Map<String, Object> params) {
        params.forEach(query::setParameter);
        return query;
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
