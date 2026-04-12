package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final LoteRepository loteRepository;
    private final AuthService authService;

    // ---- Consultas ----

    @Transactional(readOnly = true)
    public List<Produto> listarPosicaoEstoque(Integer empresaId) {
        return produtoRepository.findByEmpresaIdAndAtivoTrue(empresaId);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> buscarHistorico(Integer empresaId) {
        return movimentacaoEstoqueRepository.findByEmpresaIdOrderByCriadoEmDesc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> buscarHistoricoPorPeriodo(
            Integer empresaId, LocalDateTime inicio, LocalDateTime fim) {
        return movimentacaoEstoqueRepository
                .findByEmpresaIdAndCriadoEmBetweenOrderByCriadoEmDesc(empresaId, inicio, fim);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularValorTotalEstoque(Integer empresaId) {
        return produtoRepository.findByEmpresaIdAndAtivoTrue(empresaId).stream()
                .map(p -> {
                    BigDecimal estoque = p.getEstoqueAtual() != null ? p.getEstoqueAtual() : BigDecimal.ZERO;
                    BigDecimal custo   = p.getPrecoCusto()   != null ? p.getPrecoCusto()   : BigDecimal.ZERO;
                    return estoque.multiply(custo);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---- Movimentações manuais ----

    @Transactional
    public MovimentacaoEstoque registrarEntrada(Integer empresaId, Integer produtoId,
                                                BigDecimal quantidade, String motivo) {
        validarQuantidade(quantidade);
        Produto produto = buscarProduto(produtoId);

        BigDecimal saldoAnterior = produto.getEstoqueAtual();
        BigDecimal saldoPosterior = saldoAnterior.add(quantidade);

        produto.setEstoqueAtual(saldoPosterior);
        produtoRepository.save(produto);

        MovimentacaoEstoque mov = criarMovimentacao(produto, "ENTRADA", "AJUSTE_MANUAL",
                quantidade, saldoAnterior, saldoPosterior, motivo, null);
        MovimentacaoEstoque salvo = movimentacaoEstoqueRepository.save(mov);
        log.info("Entrada manual: produto={} qtd={} saldo={}", produtoId, quantidade, saldoPosterior);
        return salvo;
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(Integer empresaId, Integer produtoId,
                                              BigDecimal quantidade, String motivo) {
        validarQuantidade(quantidade);
        Produto produto = buscarProduto(produtoId);

        BigDecimal saldoAnterior = produto.getEstoqueAtual();
        if (saldoAnterior.compareTo(quantidade) < 0) {
            throw new NegocioException("Estoque insuficiente. Saldo atual: " + saldoAnterior);
        }
        BigDecimal saldoPosterior = saldoAnterior.subtract(quantidade);

        produto.setEstoqueAtual(saldoPosterior);
        produtoRepository.save(produto);

        MovimentacaoEstoque mov = criarMovimentacao(produto, "SAIDA", "AJUSTE_MANUAL",
                quantidade, saldoAnterior, saldoPosterior, motivo, null);
        MovimentacaoEstoque salvo = movimentacaoEstoqueRepository.save(mov);
        log.info("Saída manual: produto={} qtd={} saldo={}", produtoId, quantidade, saldoPosterior);
        return salvo;
    }

    @Transactional
    public Optional<MovimentacaoEstoque> ajustarInventario(Integer empresaId, Integer produtoId,
                                                           BigDecimal quantidadeReal, String motivo) {
        Produto produto = buscarProduto(produtoId);
        BigDecimal saldoAnterior = produto.getEstoqueAtual();
        BigDecimal diferenca = quantidadeReal.subtract(saldoAnterior);

        if (diferenca.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Ajuste de inventário sem diferença: produto={}", produtoId);
            return Optional.empty();
        }

        String tipo = diferenca.compareTo(BigDecimal.ZERO) > 0 ? "AJUSTE_POSITIVO" : "AJUSTE_NEGATIVO";
        BigDecimal quantidade = diferenca.abs();

        produto.setEstoqueAtual(quantidadeReal);
        produtoRepository.save(produto);

        MovimentacaoEstoque mov = criarMovimentacao(produto, tipo, "AJUSTE_MANUAL",
                quantidade, saldoAnterior, quantidadeReal, motivo, null);
        MovimentacaoEstoque salvo = movimentacaoEstoqueRepository.save(mov);
        log.info("Ajuste de inventário: produto={} tipo={} qtd={}", produtoId, tipo, quantidade);
        return Optional.of(salvo);
    }

    @Transactional
    public MovimentacaoEstoque registrarEntradaComLote(Integer empresaId, Integer produtoId,
                                                       String numeroLote, LocalDate dataValidade,
                                                       BigDecimal quantidade, String motivo) {
        validarQuantidade(quantidade);
        Produto produto = buscarProduto(produtoId);
        if (!Boolean.TRUE.equals(produto.getUsaLoteValidade())) {
            throw new NegocioException("Produto não utiliza lote e validade.");
        }

        Lote lote = loteRepository.findByProdutoIdAndNumeroLote(produtoId, numeroLote)
                .orElseGet(() -> {
                    Lote novo = new Lote();
                    novo.setProduto(produto);
                    novo.setNumeroLote(numeroLote);
                    novo.setDataValidade(dataValidade);
                    novo.setQuantidade(BigDecimal.ZERO);
                    return loteRepository.save(novo);
                });
        lote.setQuantidade(lote.getQuantidade().add(quantidade));
        loteRepository.save(lote);

        BigDecimal saldoAnterior = produto.getEstoqueAtual();
        BigDecimal saldoPosterior = saldoAnterior.add(quantidade);
        produto.setEstoqueAtual(saldoPosterior);
        produtoRepository.save(produto);

        MovimentacaoEstoque mov = criarMovimentacao(produto, "ENTRADA", "AJUSTE_MANUAL",
                quantidade, saldoAnterior, saldoPosterior, motivo, lote);
        return movimentacaoEstoqueRepository.save(mov);
    }

    // ---- Helpers ----

    private Produto buscarProduto(Integer produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new NegocioException("Produto não encontrado: " + produtoId));
    }

    private void validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Quantidade deve ser maior que zero.");
        }
    }

    private MovimentacaoEstoque criarMovimentacao(Produto produto, String tipo, String origem,
                                                   BigDecimal quantidade,
                                                   BigDecimal saldoAnterior, BigDecimal saldoPosterior,
                                                   String observacoes, Lote lote) {
        return MovimentacaoEstoque.builder()
                .empresa(produto.getEmpresa())
                .produto(produto)
                .lote(lote)
                .tipo(tipo)
                .origem(origem)
                .quantidade(quantidade)
                .custoUnitario(produto.getPrecoCusto())
                .saldoAnterior(saldoAnterior)
                .saldoPosterior(saldoPosterior)
                .observacoes(observacoes)
                .usuario(authService.getUsuarioLogado())
                .build();
    }
}
