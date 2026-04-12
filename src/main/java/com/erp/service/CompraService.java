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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final LoteRepository loteRepository;
    private final AuthService authService;

    // ---- Consultas ----

    @Transactional(readOnly = true)
    public List<Compra> listarPorEmpresa(Integer empresaId) {
        return compraRepository.findByEmpresaIdOrderByDataEmissaoDesc(empresaId);
    }

    @Transactional(readOnly = true)
    public List<Compra> listarPorStatus(Integer empresaId, String status) {
        return compraRepository.findByEmpresaIdAndStatus(empresaId, status);
    }

    @Transactional(readOnly = true)
    public Optional<Compra> buscarPorId(Integer id) {
        return compraRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public long contarRascunhos(Integer empresaId) {
        return compraRepository.countByEmpresaIdAndStatus(empresaId, "RASCUNHO");
    }

    @Transactional(readOnly = true)
    public long contarComprasMes(Integer empresaId) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        return compraRepository.countByEmpresaIdAndStatusAndDataEmissaoBetween(
                empresaId, "CONFIRMADA", inicio, fim);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularValorTotalMes(Integer empresaId) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);
        List<Compra> compras = compraRepository.findByEmpresaIdAndStatus(empresaId, "CONFIRMADA");
        return compras.stream()
                .filter(c -> !c.getDataEmissao().isBefore(inicio) && !c.getDataEmissao().isAfter(fim))
                .map(Compra::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> buscarUltimoCusto(Integer produtoId, Integer empresaId) {
        return compraRepository.findUltimoCustoProduto(empresaId, produtoId);
    }

    // ---- Persistência ----

    @Transactional
    public Compra salvarRascunho(Compra compra) {
        validarCompra(compra);

        if (compra.getNumeroDocumento() == null || compra.getNumeroDocumento().isBlank()) {
            compra.setNumeroDocumento(gerarNumeroCompra(compra.getEmpresa().getId()));
        }

        compra.setStatus("RASCUNHO");
        compra.setUsuario(authService.getUsuarioLogado());
        recalcularTotais(compra);

        Compra salva = compraRepository.save(compra);
        log.info("{} compra id={} numero='{}' status=RASCUNHO",
                salva.getId() == null ? "Criada" : "Atualizada",
                salva.getId(), salva.getNumeroDocumento());
        return salva;
    }

    @Transactional
    public Compra confirmarCompra(Integer compraId, int numeroParcelas) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new NegocioException("Compra não encontrada: " + compraId));

        if (!"RASCUNHO".equals(compra.getStatus())) {
            throw new NegocioException("Somente compras em Rascunho podem ser confirmadas.");
        }
        if (compra.getItens().isEmpty()) {
            throw new NegocioException("A compra não possui itens.");
        }

        // Validar lotes obrigatórios
        for (CompraItem item : compra.getItens()) {
            if (Boolean.TRUE.equals(item.getProduto().getUsaLoteValidade())
                    && item.getLote() == null) {
                throw new NegocioException("Produto '" + item.getProduto().getDescricao()
                        + "' exige lote e validade.");
            }
        }

        // Processar cada item: estoque + lote + custo
        for (CompraItem item : compra.getItens()) {
            Produto produto = produtoRepository.findById(item.getProduto().getId())
                    .orElseThrow(() -> new NegocioException("Produto não encontrado: " + item.getProduto().getId()));

            BigDecimal saldoAnterior = produto.getEstoqueAtual();
            BigDecimal saldoPosterior = saldoAnterior.add(item.getQuantidade());

            produto.setEstoqueAtual(saldoPosterior);
            produto.setPrecoCusto(item.getCustoUnitario());
            produtoRepository.save(produto);

            // Registrar movimentação
            MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                    .empresa(compra.getEmpresa())
                    .produto(produto)
                    .lote(item.getLote())
                    .tipo("ENTRADA")
                    .origem("COMPRA")
                    .origemId(compra.getId())
                    .quantidade(item.getQuantidade())
                    .custoUnitario(item.getCustoUnitario())
                    .saldoAnterior(saldoAnterior)
                    .saldoPosterior(saldoPosterior)
                    .usuario(authService.getUsuarioLogado())
                    .build();
            movimentacaoEstoqueRepository.save(mov);

            // Atualizar lote se necessário
            if (item.getLote() != null) {
                Lote lote = item.getLote();
                lote.setQuantidade(lote.getQuantidade().add(item.getQuantidade()));
                loteRepository.save(lote);
            }
        }

        // Gerar parcelas em Contas a Pagar
        List<LocalDate> vencimentos = calcularVencimentos(
                LocalDate.now(), compra.getCondicaoPagamento(), numeroParcelas);
        int totalParcelas = vencimentos.size();
        BigDecimal valorParcela = compra.getValorTotal()
                .divide(new BigDecimal(totalParcelas), 2, RoundingMode.HALF_UP);

        for (int i = 0; i < totalParcelas; i++) {
            ContaPagar parcela = ContaPagar.builder()
                    .empresa(compra.getEmpresa())
                    .fornecedor(compra.getFornecedor())
                    .compra(compra)
                    .descricao("Compra " + compra.getNumeroDocumento()
                            + " - Parcela " + (i + 1) + "/" + totalParcelas)
                    .numeroParcela(i + 1)
                    .totalParcelas(totalParcelas)
                    .valor(valorParcela)
                    .dataEmissao(LocalDate.now())
                    .dataVencimento(vencimentos.get(i))
                    .status("ABERTA")
                    .usuario(authService.getUsuarioLogado())
                    .build();
            contaPagarRepository.save(parcela);
        }

        compra.setStatus("CONFIRMADA");
        compra.setDataRecebimento(LocalDate.now());
        Compra confirmada = compraRepository.save(compra);
        log.info("Compra confirmada: id={} numero='{}' parcelas={}",
                confirmada.getId(), confirmada.getNumeroDocumento(), totalParcelas);
        return confirmada;
    }

    @Transactional
    public void cancelarCompra(Integer compraId) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new NegocioException("Compra não encontrada: " + compraId));

        if (!"RASCUNHO".equals(compra.getStatus())) {
            throw new NegocioException("Somente compras em Rascunho podem ser canceladas.");
        }

        compra.setStatus("CANCELADA");
        compraRepository.save(compra);
        log.info("Compra cancelada: id={} numero='{}'", compra.getId(), compra.getNumeroDocumento());
    }

    // ---- Utilitários ----

    public List<LocalDate> calcularVencimentos(LocalDate dataBase, String condicao, int parcelas) {
        List<LocalDate> datas = new ArrayList<>();
        if (condicao == null) condicao = "A_VISTA";

        switch (condicao) {
            case "A_VISTA"       -> datas.add(dataBase);
            case "30_DIAS"       -> datas.add(dataBase.plusDays(30));
            case "60_DIAS"       -> datas.add(dataBase.plusDays(60));
            case "90_DIAS"       -> datas.add(dataBase.plusDays(90));
            case "30_60_DIAS"    -> { datas.add(dataBase.plusDays(30)); datas.add(dataBase.plusDays(60)); }
            case "30_60_90_DIAS" -> { datas.add(dataBase.plusDays(30)); datas.add(dataBase.plusDays(60)); datas.add(dataBase.plusDays(90)); }
            default -> { // PERSONALIZADO
                int count = Math.max(1, parcelas);
                for (int i = 1; i <= count; i++) {
                    datas.add(dataBase.plusDays(30L * i));
                }
            }
        }
        return datas;
    }

    public String gerarNumeroCompra(Integer empresaId) {
        long total = compraRepository.countByEmpresaId(empresaId);
        int ano = LocalDate.now().getYear();
        return String.format("C%d-%05d", ano, total + 1);
    }

    public void recalcularTotais(Compra compra) {
        BigDecimal valorProdutos = compra.getItens().stream()
                .map(CompraItem::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        compra.setValorProdutos(valorProdutos);

        BigDecimal frete   = nvl(compra.getValorFrete());
        BigDecimal outros  = nvl(compra.getValorOutras());
        BigDecimal descGlobal = nvl(compra.getValorDesconto());

        compra.setValorTotal(valorProdutos.add(frete).add(outros).subtract(descGlobal)
                .max(BigDecimal.ZERO));
    }

    private void validarCompra(Compra compra) {
        if (compra.getFornecedor() == null) {
            throw new NegocioException("Fornecedor é obrigatório.");
        }
        if (compra.getItens().isEmpty()) {
            throw new NegocioException("A compra deve ter pelo menos um item.");
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
