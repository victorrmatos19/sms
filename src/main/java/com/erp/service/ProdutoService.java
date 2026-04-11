package com.erp.service;

import com.erp.model.*;
import com.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<Produto> listarTodos(Integer empresaId) {
        return produtoRepository.findAllByEmpresaIdWithDetails(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Produto> buscarPorId(Integer id) {
        return produtoRepository.findById(id);
    }

    @Transactional
    public Produto salvar(Produto produto) {
        boolean isNovo = produto.getId() == null;
        BigDecimal estoqueAnterior = BigDecimal.ZERO;

        if (!isNovo) {
            estoqueAnterior = produtoRepository.findById(produto.getId())
                .map(Produto::getEstoqueAtual)
                .orElse(BigDecimal.ZERO);
        }

        produto.setMargemLucro(calcularMargem(produto.getPrecoCusto(), produto.getPrecoVenda()));

        Produto salvo = produtoRepository.save(produto);

        if (isNovo && salvo.getEstoqueAtual().compareTo(BigDecimal.ZERO) > 0) {
            registrarMovimentacao(salvo, "ENTRADA", "AJUSTE_MANUAL",
                salvo.getEstoqueAtual(), BigDecimal.ZERO, salvo.getEstoqueAtual());
        } else if (!isNovo && salvo.getEstoqueAtual().compareTo(estoqueAnterior) != 0) {
            BigDecimal diferenca = salvo.getEstoqueAtual().subtract(estoqueAnterior);
            String tipo = diferenca.compareTo(BigDecimal.ZERO) > 0 ? "ENTRADA" : "SAIDA";
            registrarMovimentacao(salvo, tipo, "AJUSTE_MANUAL",
                diferenca.abs(), estoqueAnterior, salvo.getEstoqueAtual());
        }

        log.info("{} produto id={} descrição='{}'",
            isNovo ? "Criado" : "Atualizado", salvo.getId(), salvo.getDescricao());
        return salvo;
    }

    @Transactional
    public void inativar(Integer id) {
        produtoRepository.findById(id).ifPresent(p -> {
            p.setAtivo(false);
            produtoRepository.save(p);
            log.info("Produto inativado: id={} '{}'", p.getId(), p.getDescricao());
        });
    }

    @Transactional
    public void ativar(Integer id) {
        produtoRepository.findById(id).ifPresent(p -> {
            p.setAtivo(true);
            produtoRepository.save(p);
            log.info("Produto ativado: id={} '{}'", p.getId(), p.getDescricao());
        });
    }

    @Transactional(readOnly = true)
    public List<Produto> buscarComFiltros(Integer empresaId, String busca,
                                          Integer grupoId, boolean mostrarInativos) {
        return produtoRepository.buscarComFiltros(empresaId, busca, grupoId, mostrarInativos);
    }

    @Transactional(readOnly = true)
    public long contarAtivos(Integer empresaId) {
        return produtoRepository.countByEmpresaIdAndAtivoTrue(empresaId);
    }

    @Transactional(readOnly = true)
    public long contarAbaixoMinimo(Integer empresaId) {
        return produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(empresaId).size();
    }

    public BigDecimal calcularMargem(BigDecimal precoCusto, BigDecimal precoVenda) {
        if (precoCusto == null || precoCusto.compareTo(BigDecimal.ZERO) == 0
                || precoVenda == null) {
            return BigDecimal.ZERO;
        }
        return precoVenda.subtract(precoCusto)
            .divide(precoCusto, 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private void registrarMovimentacao(Produto produto, String tipo, String origem,
                                       BigDecimal quantidade,
                                       BigDecimal saldoAnterior, BigDecimal saldoPosterior) {
        var mov = MovimentacaoEstoque.builder()
            .empresa(produto.getEmpresa())
            .produto(produto)
            .tipo(tipo)
            .origem(origem)
            .quantidade(quantidade)
            .custoUnitario(produto.getPrecoCusto())
            .saldoAnterior(saldoAnterior)
            .saldoPosterior(saldoPosterior)
            .usuario(authService.getUsuarioLogado())
            .build();
        movimentacaoEstoqueRepository.save(mov);
    }
}
