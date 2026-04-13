package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final VendaRepository vendaRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final AuthService authService;

    // ---- Consultas ----

    @Transactional(readOnly = true)
    public List<Orcamento> listarPorEmpresa(Integer empresaId) {
        return orcamentoRepository.findByEmpresaIdOrderByDataEmissaoDesc(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Orcamento> buscarComItens(Integer id) {
        return orcamentoRepository.findByIdWithItens(id);
    }

    @Transactional(readOnly = true)
    public List<Orcamento> buscarComFiltros(Integer empresaId, String busca, String status,
                                             Integer vendedorId, boolean apenasDoVendedor) {
        String statusParam = (status == null || "Todos".equals(status)) ? null : status.toUpperCase();
        Integer vendedorParam = apenasDoVendedor ? vendedorId : null;
        List<Orcamento> lista = orcamentoRepository.findComFiltros(empresaId, statusParam, vendedorParam);
        if (busca != null && !busca.isBlank()) {
            String lower = busca.toLowerCase();
            lista = lista.stream()
                    .filter(o -> {
                        boolean matchNumero = o.getNumero() != null
                                && o.getNumero().toLowerCase().contains(lower);
                        boolean matchCliente = o.getCliente() != null
                                && o.getCliente().getNome() != null
                                && o.getCliente().getNome().toLowerCase().contains(lower);
                        return matchNumero || matchCliente;
                    })
                    .toList();
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public long contarAbertos(Integer empresaId) {
        return orcamentoRepository.countByEmpresaIdAndStatus(empresaId, "ABERTO");
    }

    @Transactional(readOnly = true)
    public long contarAVencerHojeAmanha(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);
        List<Orcamento> abertos = orcamentoRepository.findByEmpresaIdAndStatus(empresaId, "ABERTO");
        return abertos.stream()
                .filter(o -> o.getDataValidade() != null
                        && !o.getDataValidade().isBefore(hoje)
                        && !o.getDataValidade().isAfter(amanha))
                .count();
    }

    @Transactional(readOnly = true)
    public BigDecimal valorConvertidosMes(Integer empresaId) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        List<Orcamento> convertidos = orcamentoRepository.findByEmpresaIdAndStatus(empresaId, "CONVERTIDO");
        return convertidos.stream()
                .filter(o -> o.getDataEmissao() != null
                        && !o.getDataEmissao().isBefore(inicio)
                        && !o.getDataEmissao().isAfter(fim))
                .map(Orcamento::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTaxaConversaoMes(Integer empresaId) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        long convertidos = orcamentoRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                empresaId, "CONVERTIDO", inicio, fim);
        long abertos = orcamentoRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                empresaId, "ABERTO", inicio, fim);
        long total = convertidos + abertos;
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(convertidos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    // ---- Persistência ----

    @Transactional
    public Orcamento salvar(Orcamento orcamento) {
        validarOrcamento(orcamento);
        if (orcamento.getNumero() == null || orcamento.getNumero().isBlank()) {
            orcamento.setNumero(gerarNumeroOrcamento(orcamento.getEmpresa().getId()));
        }
        if (orcamento.getStatus() == null) {
            orcamento.setStatus("ABERTO");
        }
        orcamento.setUsuario(authService.getUsuarioLogado());
        recalcularTotais(orcamento);

        for (OrcamentoItem item : orcamento.getItens()) {
            item.setOrcamento(orcamento);
        }

        Orcamento salvo = orcamentoRepository.save(orcamento);
        log.info("{} orçamento id={} numero='{}' status={}",
                salvo.getId() == null ? "Criado" : "Atualizado",
                salvo.getId(), salvo.getNumero(), salvo.getStatus());
        return salvo;
    }

    @Transactional
    public void cancelar(Integer orcamentoId) {
        Orcamento orc = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new NegocioException("Orçamento não encontrado: " + orcamentoId));
        if (!"ABERTO".equals(orc.getStatus())) {
            throw new NegocioException("Apenas orçamentos ABERTOS podem ser cancelados.");
        }
        orc.setStatus("CANCELADO");
        orcamentoRepository.save(orc);
        log.info("Orçamento cancelado: id={} numero='{}'", orc.getId(), orc.getNumero());
    }

    @Transactional
    public int expirarVencidos(Integer empresaId) {
        int qtd = orcamentoRepository.expirarVencidos(empresaId, LocalDate.now());
        if (qtd > 0) {
            log.info("Expirados {} orçamento(s) para empresa id={}", qtd, empresaId);
        }
        return qtd;
    }

    @Transactional
    public Venda converterEmVenda(Integer orcamentoId, String formaPagamento,
                                   Integer parcelas, String observacoesVenda) {
        Orcamento orc = orcamentoRepository.findByIdWithItens(orcamentoId)
                .orElseThrow(() -> new NegocioException("Orçamento não encontrado: " + orcamentoId));

        if (!"ABERTO".equals(orc.getStatus())) {
            throw new NegocioException("Apenas orçamentos ABERTOS podem ser convertidos em venda.");
        }
        if (orc.getItens().isEmpty()) {
            throw new NegocioException("O orçamento não possui itens.");
        }

        // Verificar estoque
        Integer empresaId = orc.getEmpresa().getId();
        Configuracao config = configuracaoRepository.findByEmpresaId(empresaId).orElse(null);
        boolean permiteEstoqueZero = config != null && Boolean.TRUE.equals(config.getPermiteVendaEstoqueZero());

        if (!permiteEstoqueZero) {
            List<String> semEstoque = new ArrayList<>();
            for (OrcamentoItem item : orc.getItens()) {
                Produto prod = produtoRepository.findById(item.getProduto().getId())
                        .orElseThrow(() -> new NegocioException("Produto não encontrado."));
                if (prod.getEstoqueAtual().compareTo(item.getQuantidade()) < 0) {
                    semEstoque.add(prod.getDescricao() + " (disponível: " + prod.getEstoqueAtual() + ")");
                }
            }
            if (!semEstoque.isEmpty()) {
                throw new NegocioException("Estoque insuficiente para:\n" + String.join("\n", semEstoque));
            }
        }

        // Criar venda
        String numeroVenda = gerarNumeroVenda(empresaId);
        Venda venda = Venda.builder()
                .empresa(orc.getEmpresa())
                .cliente(orc.getCliente())
                .vendedor(orc.getVendedor())
                .usuario(authService.getUsuarioLogado())
                .orcamento(orc)
                .numero(numeroVenda)
                .status("FINALIZADA")
                .dataVenda(LocalDateTime.now())
                .valorProdutos(orc.getValorProdutos())
                .valorDesconto(orc.getValorDesconto())
                .valorTotal(orc.getValorTotal())
                .observacoes(observacoesVenda)
                .build();

        // Criar itens e movimentações de estoque
        for (OrcamentoItem oi : orc.getItens()) {
            Produto produto = produtoRepository.findById(oi.getProduto().getId())
                    .orElseThrow(() -> new NegocioException(
                            "Produto não encontrado: " + oi.getProduto().getId()));

            BigDecimal saldoAnterior = produto.getEstoqueAtual();
            BigDecimal saldoPosterior = saldoAnterior.subtract(oi.getQuantidade()).max(BigDecimal.ZERO);
            produto.setEstoqueAtual(saldoPosterior);
            produtoRepository.save(produto);

            MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                    .empresa(orc.getEmpresa())
                    .produto(produto)
                    .tipo("SAIDA")
                    .origem("VENDA")
                    .quantidade(oi.getQuantidade())
                    .custoUnitario(produto.getPrecoCusto())
                    .saldoAnterior(saldoAnterior)
                    .saldoPosterior(saldoPosterior)
                    .usuario(authService.getUsuarioLogado())
                    .build();
            movimentacaoEstoqueRepository.save(mov);

            VendaItem vi = VendaItem.builder()
                    .venda(venda)
                    .produto(produto)
                    .descricao(oi.getDescricao())
                    .quantidade(oi.getQuantidade())
                    .precoUnitario(oi.getPrecoUnitario())
                    .custoUnitario(produto.getPrecoCusto())
                    .desconto(oi.getDesconto())
                    .valorTotal(oi.getValorTotal())
                    .build();
            venda.getItens().add(vi);
        }

        // Criar pagamento
        int numParcelas = (parcelas != null && parcelas > 0) ? parcelas : 1;
        VendaPagamento pgto = VendaPagamento.builder()
                .venda(venda)
                .formaPagamento(formaPagamento)
                .valor(orc.getValorTotal())
                .parcelas(numParcelas)
                .build();
        venda.getPagamentos().add(pgto);

        Venda vendaSalva = vendaRepository.save(venda);

        // Gerar parcelas a receber se pagamento a prazo
        if ("PRAZO".equals(formaPagamento) || "CREDIARIO".equals(formaPagamento)) {
            BigDecimal valorParcela = orc.getValorTotal()
                    .divide(BigDecimal.valueOf(numParcelas), 2, RoundingMode.HALF_UP);
            for (int i = 0; i < numParcelas; i++) {
                ContaReceber cr = ContaReceber.builder()
                        .empresa(orc.getEmpresa())
                        .cliente(orc.getCliente())
                        .venda(vendaSalva)
                        .descricao("Venda " + numeroVenda + " - Parcela " + (i + 1) + "/" + numParcelas)
                        .numeroParcela(i + 1)
                        .totalParcelas(numParcelas)
                        .valor(valorParcela)
                        .dataEmissao(LocalDate.now())
                        .dataVencimento(LocalDate.now().plusDays(30L * (i + 1)))
                        .status("ABERTA")
                        .usuario(authService.getUsuarioLogado())
                        .build();
                contaReceberRepository.save(cr);
            }
        }

        // Marcar orçamento como convertido
        orc.setStatus("CONVERTIDO");
        orcamentoRepository.save(orc);

        log.info("Orçamento {} convertido em Venda {} (parcelas={})",
                orc.getNumero(), numeroVenda, numParcelas);
        return vendaSalva;
    }

    // ---- Utilitários ----

    public void recalcularTotais(Orcamento orc) {
        BigDecimal valorProdutos = orc.getItens().stream()
                .map(OrcamentoItem::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orc.setValorProdutos(valorProdutos);
        BigDecimal descGlobal = orc.getValorDesconto() != null ? orc.getValorDesconto() : BigDecimal.ZERO;
        orc.setValorTotal(valorProdutos.subtract(descGlobal).max(BigDecimal.ZERO));
    }

    public String gerarNumeroOrcamento(Integer empresaId) {
        long total = orcamentoRepository.countByEmpresaId(empresaId);
        int ano = LocalDate.now().getYear();
        return String.format("ORC-%d-%05d", ano, total + 1);
    }

    private String gerarNumeroVenda(Integer empresaId) {
        long total = vendaRepository.countByEmpresaId(empresaId);
        int ano = LocalDate.now().getYear();
        return String.format("V%d-%05d", ano, total + 1);
    }

    private void validarOrcamento(Orcamento orc) {
        if (orc.getVendedor() == null) {
            throw new NegocioException("Vendedor é obrigatório.");
        }
        if (orc.getDataValidade() == null) {
            throw new NegocioException("Data de validade é obrigatória.");
        }
        if (orc.getDataEmissao() != null && orc.getDataValidade() != null
                && orc.getDataValidade().isBefore(orc.getDataEmissao())) {
            throw new NegocioException("Data de validade não pode ser anterior à data de emissão.");
        }
        if (orc.getItens().isEmpty()) {
            throw new NegocioException("O orçamento deve ter pelo menos um item.");
        }
        for (OrcamentoItem item : orc.getItens()) {
            if (item.getQuantidade() == null || item.getQuantidade().compareTo(BigDecimal.ZERO) <= 0) {
                throw new NegocioException("Todos os itens devem ter quantidade maior que zero.");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new NegocioException("Todos os itens devem ter preço unitário maior que zero.");
            }
        }
    }
}
