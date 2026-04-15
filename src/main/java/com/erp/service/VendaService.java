package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Configuracao;
import com.erp.model.ContaReceber;
import com.erp.model.MovimentacaoEstoque;
import com.erp.model.Produto;
import com.erp.model.Venda;
import com.erp.model.VendaItem;
import com.erp.model.VendaPagamento;
import com.erp.repository.ConfiguracaoRepository;
import com.erp.repository.ContaReceberRepository;
import com.erp.repository.MovimentacaoEstoqueRepository;
import com.erp.repository.ProdutoRepository;
import com.erp.repository.VendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendaService {

    private static final String STATUS_FINALIZADA = "FINALIZADA";
    private static final String FORMA_PRAZO = "PRAZO";
    private static final String FORMA_CREDIARIO = "CREDIARIO";

    private final VendaRepository vendaRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ConfiguracaoRepository configuracaoRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<Venda> listarPorEmpresa(Integer empresaId) {
        return vendaRepository.findByEmpresaIdOrderByDataVendaDesc(empresaId);
    }

    @Transactional(readOnly = true)
    public Optional<Venda> buscarComItens(Integer id) {
        Optional<Venda> venda = vendaRepository.findByIdWithItens(id);
        venda.ifPresent(v -> vendaRepository.findByIdWithPagamentos(id));
        return venda;
    }

    @Transactional(readOnly = true)
    public long contarVendasMes(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX);
        return vendaRepository.countByEmpresaIdAndStatusAndDataVendaBetween(
                empresaId, STATUS_FINALIZADA, inicio, fim);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularValorTotalMes(Integer empresaId) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fim = hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX);
        return vendaRepository.sumValorTotalByEmpresaIdAndStatusAndDataVendaBetween(
                empresaId, STATUS_FINALIZADA, inicio, fim);
    }

    @Transactional(readOnly = true)
    public long contarFinalizadas(Integer empresaId) {
        return vendaRepository.countByEmpresaIdAndStatus(empresaId, STATUS_FINALIZADA);
    }

    @Transactional
    public Venda registrarVendaDireta(Venda venda, String formaPagamento, Integer parcelas) {
        validarVenda(venda, formaPagamento);

        Integer empresaId = venda.getEmpresa().getId();
        boolean permiteEstoqueZero = permiteVendaComEstoqueInsuficiente(empresaId);
        List<Produto> produtos = carregarProdutosEValidarEstoque(venda, permiteEstoqueZero);

        if (venda.getNumero() == null || venda.getNumero().isBlank()) {
            venda.setNumero(gerarNumeroVenda(empresaId));
        }
        venda.setStatus(STATUS_FINALIZADA);
        venda.setDataVenda(venda.getDataVenda() != null ? venda.getDataVenda() : LocalDateTime.now());
        venda.setUsuario(authService.getUsuarioLogado());
        recalcularTotais(venda);
        prepararItens(venda, produtos);
        prepararPagamento(venda, formaPagamento, parcelas);

        Venda vendaSalva = vendaRepository.save(venda);
        baixarEstoque(vendaSalva, produtos, permiteEstoqueZero);
        gerarContasAReceber(vendaSalva, formaPagamento, normalizarParcelas(parcelas));

        log.info("Venda direta registrada: id={} numero='{}' valor={}",
                vendaSalva.getId(), vendaSalva.getNumero(), vendaSalva.getValorTotal());
        return vendaSalva;
    }

    public void recalcularTotais(Venda venda) {
        BigDecimal valorProdutos = BigDecimal.ZERO;
        for (VendaItem item : venda.getItens()) {
            BigDecimal quantidade = nvl(item.getQuantidade());
            BigDecimal preco = nvl(item.getPrecoUnitario());
            BigDecimal desconto = nvl(item.getDesconto());
            BigDecimal totalItem = quantidade.multiply(preco).subtract(desconto).max(BigDecimal.ZERO);
            item.setValorTotal(totalItem);
            valorProdutos = valorProdutos.add(totalItem);
        }
        venda.setValorProdutos(valorProdutos);
        BigDecimal descontoGlobal = nvl(venda.getValorDesconto());
        venda.setValorTotal(valorProdutos.subtract(descontoGlobal).max(BigDecimal.ZERO));
    }

    public String gerarNumeroVenda(Integer empresaId) {
        long total = vendaRepository.countByEmpresaId(empresaId);
        int ano = LocalDate.now().getYear();
        return String.format("V%d-%05d", ano, total + 1);
    }

    private void validarVenda(Venda venda, String formaPagamento) {
        if (venda.getEmpresa() == null || venda.getEmpresa().getId() == null) {
            throw new NegocioException("Empresa é obrigatória.");
        }
        if (venda.getVendedor() == null) {
            throw new NegocioException("Vendedor é obrigatório.");
        }
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new NegocioException("A venda deve ter pelo menos um item.");
        }
        if (formaPagamento == null || formaPagamento.isBlank()) {
            throw new NegocioException("Forma de pagamento é obrigatória.");
        }
        if ((FORMA_PRAZO.equals(formaPagamento) || FORMA_CREDIARIO.equals(formaPagamento))
                && venda.getCliente() == null) {
            throw new NegocioException("Cliente é obrigatório para venda a prazo.");
        }
        for (VendaItem item : venda.getItens()) {
            if (item.getProduto() == null || item.getProduto().getId() == null) {
                throw new NegocioException("Todos os itens devem possuir produto.");
            }
            if (item.getQuantidade() == null || item.getQuantidade().compareTo(BigDecimal.ZERO) <= 0) {
                throw new NegocioException("Todos os itens devem ter quantidade maior que zero.");
            }
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new NegocioException("Todos os itens devem ter preço unitário maior que zero.");
            }
        }
    }

    private boolean permiteVendaComEstoqueInsuficiente(Integer empresaId) {
        Configuracao config = configuracaoRepository.findByEmpresaId(empresaId).orElse(null);
        return config != null && Boolean.TRUE.equals(config.getPermiteVendaEstoqueZero());
    }

    private List<Produto> carregarProdutosEValidarEstoque(Venda venda, boolean permiteEstoqueZero) {
        List<Produto> produtos = new ArrayList<>();
        Map<Integer, Produto> produtosPorId = new LinkedHashMap<>();
        Map<Integer, BigDecimal> quantidadePorProduto = new LinkedHashMap<>();
        List<String> semEstoque = new ArrayList<>();

        for (VendaItem item : venda.getItens()) {
            Integer produtoId = item.getProduto().getId();
            Produto produto = produtosPorId.computeIfAbsent(produtoId, id ->
                    produtoRepository.findById(id)
                            .orElseThrow(() -> new NegocioException("Produto não encontrado: " + id)));
            produtos.add(produto);
            quantidadePorProduto.merge(produtoId, item.getQuantidade(), BigDecimal::add);
        }

        if (!permiteEstoqueZero) {
            for (Map.Entry<Integer, BigDecimal> entry : quantidadePorProduto.entrySet()) {
                Produto produto = produtosPorId.get(entry.getKey());
                if (nvl(produto.getEstoqueAtual()).compareTo(entry.getValue()) < 0) {
                    semEstoque.add(produto.getDescricao() + " (disponível: "
                            + nvl(produto.getEstoqueAtual()).stripTrailingZeros().toPlainString() + ")");
                }
            }
        }

        if (!semEstoque.isEmpty()) {
            throw new NegocioException("Estoque insuficiente para:\n" + String.join("\n", semEstoque));
        }
        return produtos;
    }

    private void prepararItens(Venda venda, List<Produto> produtos) {
        for (int i = 0; i < venda.getItens().size(); i++) {
            VendaItem item = venda.getItens().get(i);
            Produto produto = produtos.get(i);
            item.setVenda(venda);
            item.setProduto(produto);
            if (item.getDescricao() == null || item.getDescricao().isBlank()) {
                item.setDescricao(produto.getDescricao());
            }
            item.setCustoUnitario(nvl(produto.getPrecoCusto()));
            item.setDesconto(nvl(item.getDesconto()));
        }
    }

    private void prepararPagamento(Venda venda, String formaPagamento, Integer parcelas) {
        venda.getPagamentos().clear();
        VendaPagamento pagamento = VendaPagamento.builder()
                .venda(venda)
                .formaPagamento(formaPagamento)
                .valor(venda.getValorTotal())
                .parcelas(normalizarParcelas(parcelas))
                .build();
        venda.getPagamentos().add(pagamento);
    }

    private void baixarEstoque(Venda venda, List<Produto> produtos, boolean permiteEstoqueZero) {
        for (int i = 0; i < venda.getItens().size(); i++) {
            VendaItem item = venda.getItens().get(i);
            Produto produto = produtos.get(i);
            BigDecimal saldoAnterior = nvl(produto.getEstoqueAtual());
            BigDecimal saldoPosterior = saldoAnterior.subtract(item.getQuantidade());
            if (!permiteEstoqueZero || saldoPosterior.compareTo(BigDecimal.ZERO) < 0) {
                saldoPosterior = saldoPosterior.max(BigDecimal.ZERO);
            }

            produto.setEstoqueAtual(saldoPosterior);
            produtoRepository.save(produto);

            MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                    .empresa(venda.getEmpresa())
                    .produto(produto)
                    .lote(item.getLote())
                    .tipo("SAIDA")
                    .origem("VENDA")
                    .origemId(venda.getId())
                    .quantidade(item.getQuantidade())
                    .custoUnitario(nvl(produto.getPrecoCusto()))
                    .saldoAnterior(saldoAnterior)
                    .saldoPosterior(saldoPosterior)
                    .usuario(authService.getUsuarioLogado())
                    .build();
            movimentacaoEstoqueRepository.save(mov);
        }
    }

    private void gerarContasAReceber(Venda venda, String formaPagamento, int parcelas) {
        if (!FORMA_PRAZO.equals(formaPagamento) && !FORMA_CREDIARIO.equals(formaPagamento)) {
            return;
        }

        BigDecimal valorParcela = venda.getValorTotal()
                .divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
        for (int i = 0; i < parcelas; i++) {
            ContaReceber conta = ContaReceber.builder()
                    .empresa(venda.getEmpresa())
                    .cliente(venda.getCliente())
                    .venda(venda)
                    .descricao("Venda " + venda.getNumero() + " - Parcela " + (i + 1) + "/" + parcelas)
                    .numeroParcela(i + 1)
                    .totalParcelas(parcelas)
                    .valor(valorParcela)
                    .dataEmissao(LocalDate.now())
                    .dataVencimento(LocalDate.now().plusDays(30L * (i + 1)))
                    .status("ABERTA")
                    .usuario(authService.getUsuarioLogado())
                    .build();
            contaReceberRepository.save(conta);
        }
    }

    private int normalizarParcelas(Integer parcelas) {
        return parcelas != null && parcelas > 0 ? parcelas : 1;
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
