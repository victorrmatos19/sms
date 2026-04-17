package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do EstoqueService.
 * CF-139 a CF-147.
 */
@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock private ProdutoRepository             produtoRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private LoteRepository                loteRepository;
    @Mock private AuthService                   authService;

    @InjectMocks private EstoqueService estoqueService;

    private Empresa empresa;
    private Produto produto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(1).razaoSocial("Empresa Teste").build();

        produto = Produto.builder()
                .id(1).empresa(empresa)
                .descricao("Arroz Tipo 1")
                .estoqueAtual(new BigDecimal("50"))
                .estoqueMinimo(new BigDecimal("20"))
                .precoCusto(new BigDecimal("18.50"))
                .usaLoteValidade(false)
                .ativo(true).build();

        usuario = Usuario.builder()
                .id(1).nome("Admin").build();
    }

    // ---- CF-139: Entrada manual ----

    @Test
    void dado_produto_estoque_50_quando_entrada_20_entao_saldo_posterior_70() {
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        estoqueService.registrarEntrada(1, 1, new BigDecimal("20"), "Reposição");

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getSaldoAnterior()).isEqualByComparingTo("50");
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo("70");
        assertThat(mov.getTipo()).isEqualTo("ENTRADA");
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo("70");
    }

    // ---- CF-140: Saída manual ----

    @Test
    void dado_produto_estoque_50_quando_saida_15_entao_saldo_posterior_35() {
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        estoqueService.registrarSaida(1, 1, new BigDecimal("15"), "Venda balcão");

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getSaldoAnterior()).isEqualByComparingTo("50");
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo("35");
        assertThat(mov.getTipo()).isEqualTo("SAIDA");
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo("35");
    }

    // ---- CF-142: Ajuste inventário positivo ----

    @Test
    void dado_estoque_30_quando_ajustar_para_45_entao_ajuste_positivo_quantidade_15() {
        produto.setEstoqueAtual(new BigDecimal("30"));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        Optional<MovimentacaoEstoque> resultado =
                estoqueService.ajustarInventario(1, 1, new BigDecimal("45"), "Inventário");

        assertThat(resultado).isPresent();

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getTipo()).isEqualTo("AJUSTE_POSITIVO");
        assertThat(mov.getQuantidade()).isEqualByComparingTo("15");
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo("45");
    }

    // ---- CF-143: Ajuste inventário negativo ----

    @Test
    void dado_estoque_30_quando_ajustar_para_22_entao_ajuste_negativo_quantidade_8() {
        produto.setEstoqueAtual(new BigDecimal("30"));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        Optional<MovimentacaoEstoque> resultado =
                estoqueService.ajustarInventario(1, 1, new BigDecimal("22"), "Inventário");

        assertThat(resultado).isPresent();

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getTipo()).isEqualTo("AJUSTE_NEGATIVO");
        assertThat(mov.getQuantidade()).isEqualByComparingTo("8");
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo("22");
    }

    // ---- CF-144: Ajuste sem diferença ----

    @Test
    void dado_estoque_30_quando_ajustar_para_30_entao_sem_movimentacao() {
        produto.setEstoqueAtual(new BigDecimal("30"));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));

        Optional<MovimentacaoEstoque> resultado =
                estoqueService.ajustarInventario(1, 1, new BigDecimal("30"), "Inventário");

        assertThat(resultado).isEmpty();
        verify(movimentacaoEstoqueRepository, never()).save(any());
        verify(produtoRepository, never()).save(any());
    }

    // ---- CF-145: Valor total do estoque ----

    @Test
    void dado_tres_produtos_quando_calcular_valor_total_entao_retorna_150() {
        Produto a = Produto.builder().id(1).empresa(empresa).descricao("A")
                .estoqueAtual(new BigDecimal("10")).precoCusto(new BigDecimal("5.00"))
                .estoqueMinimo(BigDecimal.ZERO).usaLoteValidade(false).ativo(true).build();
        Produto b = Produto.builder().id(2).empresa(empresa).descricao("B")
                .estoqueAtual(new BigDecimal("5")).precoCusto(new BigDecimal("20.00"))
                .estoqueMinimo(BigDecimal.ZERO).usaLoteValidade(false).ativo(true).build();
        Produto c = Produto.builder().id(3).empresa(empresa).descricao("C")
                .estoqueAtual(BigDecimal.ZERO).precoCusto(new BigDecimal("15.00"))
                .estoqueMinimo(BigDecimal.ZERO).usaLoteValidade(false).ativo(true).build();

        when(produtoRepository.findByEmpresaIdAndAtivoTrue(1)).thenReturn(List.of(a, b, c));

        BigDecimal total = estoqueService.calcularValorTotalEstoque(1);

        // A: 10*5=50, B: 5*20=100, C: 0*15=0 → total=150
        assertThat(total).isEqualByComparingTo("150.00");
    }

    // ---- CF-146: Histórico com filtro de período ----

    @Test
    void dado_filtro_de_periodo_quando_buscar_historico_entao_chama_repositorio_com_datas_corretas() {
        LocalDateTime inicio = LocalDate.of(2026, 1, 1).atStartOfDay();
        LocalDateTime fim    = LocalDate.of(2026, 12, 31).atTime(23, 59, 59);

        when(movimentacaoEstoqueRepository
                .findByEmpresaIdAndCriadoEmBetweenOrderByCriadoEmDesc(1, inicio, fim))
                .thenReturn(List.of());

        List<MovimentacaoEstoque> resultado =
                estoqueService.buscarHistoricoPorPeriodo(1, inicio, fim);

        verify(movimentacaoEstoqueRepository)
                .findByEmpresaIdAndCriadoEmBetweenOrderByCriadoEmDesc(1, inicio, fim);
        assertThat(resultado).isEmpty();
    }

    // ---- CF-190: entrada com quantidade zero deve lançar exceção ----

    @Test
    void dado_quantidade_zero_quando_registrar_entrada_entao_lanca_negocio_exception() {
        assertThatThrownBy(() -> estoqueService.registrarEntrada(1, 1, BigDecimal.ZERO, "Teste"))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("maior que zero");

        verify(produtoRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    // ---- CF-191: entrada com quantidade negativa deve lançar exceção ----

    @Test
    void dado_quantidade_negativa_quando_registrar_entrada_entao_lanca_negocio_exception() {
        assertThatThrownBy(() ->
                estoqueService.registrarEntrada(1, 1, new BigDecimal("-5"), "Teste"))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("maior que zero");

        verify(produtoRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    // ---- CF-192/193: saída que leva estoque a negativo com config false ----

    @Test
    void dado_estoque_zero_quando_registrar_saida_entao_lanca_negocio_exception() {
        produto.setEstoqueAtual(BigDecimal.ZERO);
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() ->
                estoqueService.registrarSaida(1, 1, new BigDecimal("1"), "Saída"))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Estoque insuficiente");

        verify(produtoRepository, never()).save(any());
    }

    @Test
    void dado_estoque_insuficiente_quando_registrar_saida_entao_lanca_negocio_exception() {
        produto.setEstoqueAtual(new BigDecimal("5"));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() ->
                estoqueService.registrarSaida(1, 1, new BigDecimal("10"), "Saída"))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Estoque insuficiente");
    }

    // ---- CF-194: saída permitindo negativo (registrarSaidaPermitindoNegativo) ----

    @Test
    void dado_estoque_zero_quando_registrar_saida_permitindo_negativo_entao_saldo_fica_negativo() {
        produto.setEstoqueAtual(BigDecimal.ZERO);
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        estoqueService.registrarSaidaPermitindoNegativo(1, 1, new BigDecimal("3"), "Saída forçada");

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        assertThat(captor.getValue().getSaldoAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getSaldoPosterior()).isEqualByComparingTo(new BigDecimal("-3"));
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo(new BigDecimal("-3"));
    }

    // ---- CF-195: ajuste com quantidade igual ao estoque atual não gera movimentação ----
    // Já coberto por CF-144: dado_estoque_30_quando_ajustar_para_30_entao_sem_movimentacao

    // ---- CF-196: movimentação registra saldo_anterior e saldo_posterior corretamente ----
    // Já coberto por CF-139 e CF-140

    // ---- CF-197: movimentação manual registra usuario_id do usuário logado ----

    @Test
    void dado_usuario_logado_quando_registrar_entrada_entao_movimentacao_tem_usuario() {
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        estoqueService.registrarEntrada(1, 1, new BigDecimal("10"), "Entrada manual");

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(1);
    }

    // ---- CF-198: movimentação via compra registra origem='COMPRA' ----
    // Testado em CompraServiceTest CF-125

    // ---- CF-199: histórico por período respeita datas ----
    // Já coberto por CF-146

    // ---- CF-200: busca de histórico por tipo ----

    @Test
    void dado_filtro_por_produto_quando_buscar_historico_entao_delega_ao_repository() {
        when(movimentacaoEstoqueRepository
                .findByEmpresaIdAndProdutoIdOrderByCriadoEmDesc(1, 1))
                .thenReturn(List.of(new MovimentacaoEstoque()));

        List<MovimentacaoEstoque> resultado =
                estoqueService.buscarHistoricoPorProduto(1, 1);

        verify(movimentacaoEstoqueRepository)
                .findByEmpresaIdAndProdutoIdOrderByCriadoEmDesc(1, 1);
        assertThat(resultado).hasSize(1);
    }

    // ---- CF-161: produto sem usaLoteValidade não aceita entrada com lote ----

    @Test
    void dado_produto_sem_lote_quando_registrar_entrada_com_lote_entao_lanca_negocio_exception() {
        produto.setUsaLoteValidade(false);
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() ->
                estoqueService.registrarEntradaComLote(1, 1, "L001",
                        LocalDate.now().plusMonths(6), new BigDecimal("10"), "Entrada"))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("lote e validade");

        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    // ---- CF-147: Lote criado na movimentação ----

    @Test
    void dado_produto_com_lote_quando_entrada_entao_lote_salvo_e_movimentacao_com_lote() {
        produto.setUsaLoteValidade(true);
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenReturn(produto);

        Lote lote = new Lote();
        lote.setNumeroLote("L001");
        lote.setDataValidade(LocalDate.of(2026, 12, 31));
        lote.setQuantidade(BigDecimal.ZERO);
        lote.setProduto(produto);

        when(loteRepository.findByProdutoIdAndNumeroLote(1, "L001"))
                .thenReturn(Optional.empty());
        when(loteRepository.save(any())).thenReturn(lote);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(authService.getUsuarioLogado()).thenReturn(usuario);

        estoqueService.registrarEntradaComLote(1, 1, "L001",
                LocalDate.of(2026, 12, 31), new BigDecimal("20"), "Entrada com lote");

        ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
        // save is called twice: once to create, once to update quantity
        verify(loteRepository, atLeastOnce()).save(loteCaptor.capture());
        List<Lote> lotesSalvos = loteCaptor.getAllValues();
        assertThat(lotesSalvos).anyMatch(l -> "L001".equals(l.getNumeroLote()));

        ArgumentCaptor<MovimentacaoEstoque> movCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getLote()).isNotNull();
    }
}
