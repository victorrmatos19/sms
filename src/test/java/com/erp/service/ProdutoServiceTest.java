package com.erp.service;

import com.erp.model.*;
import com.erp.repository.MovimentacaoEstoqueRepository;
import com.erp.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do ProdutoService.
 * Cobre CF-06, CF-09, CF-12, CF-13, CF-14, CF-15 e calcularMargem.
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private AuthService authService;

    @InjectMocks private ProdutoService produtoService;

    private Empresa empresa;
    private UnidadeMedida unidade;
    private Produto produtoBase;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
            .id(1)
            .razaoSocial("Empresa Teste")
            .cnpj("00.000.000/0001-00")
            .regimeTributario("SIMPLES_NACIONAL")
            .build();

        unidade = UnidadeMedida.builder().id(1).sigla("UN").empresa(empresa).build();

        produtoBase = Produto.builder()
            .empresa(empresa)
            .descricao("Produto Teste")
            .unidade(unidade)
            .precoCusto(new BigDecimal("10.00"))
            .precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO)
            .estoqueAtual(BigDecimal.ZERO)
            .estoqueMinimo(BigDecimal.ZERO)
            .ativo(true)
            .build();
    }

    // ---- CF-09: Cálculo de margem ----

    @Test
    void dado_custo_10_venda_15_quando_calcular_margem_entao_retorna_50_por_cento() {
        BigDecimal margem = produtoService.calcularMargem(
            new BigDecimal("10.00"), new BigDecimal("15.00"));

        assertThat(margem).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void dado_custo_zero_quando_calcular_margem_entao_retorna_zero_sem_erro() {
        BigDecimal margem = produtoService.calcularMargem(
            BigDecimal.ZERO, new BigDecimal("15.00"));

        assertThat(margem).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dado_custo_nulo_quando_calcular_margem_entao_retorna_zero() {
        BigDecimal margem = produtoService.calcularMargem(null, new BigDecimal("15.00"));

        assertThat(margem).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dado_venda_menor_que_custo_quando_calcular_margem_entao_retorna_margem_negativa() {
        BigDecimal margem = produtoService.calcularMargem(
            new BigDecimal("15.00"), new BigDecimal("10.00"));

        assertThat(margem).isLessThan(BigDecimal.ZERO);
    }

    @Test
    void dado_custo_igual_a_venda_quando_calcular_margem_entao_retorna_zero() {
        BigDecimal margem = produtoService.calcularMargem(
            new BigDecimal("10.00"), new BigDecimal("10.00"));

        assertThat(margem).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dado_venda_nula_quando_calcular_margem_entao_retorna_zero() {
        BigDecimal margem = produtoService.calcularMargem(new BigDecimal("10.00"), null);

        assertThat(margem).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---- CF-06: Novo produto com estoque > 0 gera movimentação de ENTRADA ----

    @Test
    void dado_novo_produto_com_estoque_positivo_quando_salvar_entao_gera_movimentacao_entrada() {
        produtoBase.setEstoqueAtual(new BigDecimal("5.00"));

        Produto salvo = Produto.builder()
            .id(1)
            .empresa(empresa)
            .descricao("Produto Teste")
            .unidade(unidade)
            .precoCusto(new BigDecimal("10.00"))
            .precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO)
            .estoqueAtual(new BigDecimal("5.00"))
            .estoqueMinimo(BigDecimal.ZERO)
            .ativo(true)
            .build();

        when(produtoRepository.save(any())).thenReturn(salvo);

        produtoService.salvar(produtoBase);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getTipo()).isEqualTo("ENTRADA");
        assertThat(mov.getOrigem()).isEqualTo("AJUSTE_MANUAL");
        assertThat(mov.getQuantidade()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(mov.getSaldoAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void dado_novo_produto_com_estoque_zero_quando_salvar_entao_nao_gera_movimentacao() {
        produtoBase.setEstoqueAtual(BigDecimal.ZERO);

        Produto salvo = Produto.builder()
            .id(1).empresa(empresa).descricao("Produto Teste").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(BigDecimal.ZERO)
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        when(produtoRepository.save(any())).thenReturn(salvo);

        produtoService.salvar(produtoBase);

        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    // ---- CF-13: Editar produto — ajuste de estoque ----

    @Test
    void dado_produto_existente_quando_aumentar_estoque_entao_gera_movimentacao_entrada() {
        Produto existente = Produto.builder()
            .id(10).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(new BigDecimal("10.00"))
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        Produto atualizado = Produto.builder()
            .id(10).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(new BigDecimal("15.00"))
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        when(produtoRepository.findById(10)).thenReturn(Optional.of(existente));
        when(produtoRepository.save(any())).thenReturn(atualizado);

        produtoService.salvar(atualizado);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getTipo()).isEqualTo("ENTRADA");
        assertThat(mov.getQuantidade()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(mov.getSaldoAnterior()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void dado_produto_existente_quando_reduzir_estoque_entao_gera_movimentacao_saida() {
        Produto existente = Produto.builder()
            .id(10).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(new BigDecimal("10.00"))
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        Produto atualizado = Produto.builder()
            .id(10).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(new BigDecimal("8.00"))
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        when(produtoRepository.findById(10)).thenReturn(Optional.of(existente));
        when(produtoRepository.save(any())).thenReturn(atualizado);

        produtoService.salvar(atualizado);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque mov = captor.getValue();
        assertThat(mov.getTipo()).isEqualTo("SAIDA");
        assertThat(mov.getQuantidade()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(mov.getSaldoAnterior()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(mov.getSaldoPosterior()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    void dado_produto_existente_quando_nao_alterar_estoque_entao_nao_gera_movimentacao() {
        Produto existente = Produto.builder()
            .id(10).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("15.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(new BigDecimal("10.00"))
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        Produto atualizado = Produto.builder()
            .id(10).empresa(empresa).descricao("Produto Renomeado").unidade(unidade)
            .precoCusto(new BigDecimal("10.00")).precoVenda(new BigDecimal("18.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(new BigDecimal("10.00"))
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        when(produtoRepository.findById(10)).thenReturn(Optional.of(existente));
        when(produtoRepository.save(any())).thenReturn(atualizado);

        produtoService.salvar(atualizado);

        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    // ---- CF-12: Editar produto atualiza margemLucro ----

    @Test
    void dado_produto_salvo_quando_salvar_entao_margem_e_calculada_automaticamente() {
        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1);
            return p;
        });

        Produto resultado = produtoService.salvar(produtoBase);

        assertThat(resultado.getMargemLucro()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    // ---- CF-14: Inativar produto ----

    @Test
    void dado_produto_ativo_quando_inativar_entao_ativo_fica_false() {
        Produto produto = Produto.builder()
            .id(5).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(BigDecimal.ZERO).precoVenda(new BigDecimal("10.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(BigDecimal.ZERO)
            .estoqueMinimo(BigDecimal.ZERO).ativo(true).build();

        when(produtoRepository.findById(5)).thenReturn(Optional.of(produto));

        produtoService.inativar(5);

        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void dado_id_inexistente_quando_inativar_entao_nao_lanca_excecao() {
        when(produtoRepository.findById(999)).thenReturn(Optional.empty());

        // Não deve lançar exceção
        produtoService.inativar(999);

        verify(produtoRepository, never()).save(any());
    }

    // ---- CF-15: Ativar produto ----

    @Test
    void dado_produto_inativo_quando_ativar_entao_ativo_fica_true() {
        Produto produto = Produto.builder()
            .id(6).empresa(empresa).descricao("Produto").unidade(unidade)
            .precoCusto(BigDecimal.ZERO).precoVenda(new BigDecimal("10.00"))
            .precoMinimo(BigDecimal.ZERO).estoqueAtual(BigDecimal.ZERO)
            .estoqueMinimo(BigDecimal.ZERO).ativo(false).build();

        when(produtoRepository.findById(6)).thenReturn(Optional.of(produto));

        produtoService.ativar(6);

        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    // ---- CF-01: listarTodos delega ao repository ----

    @Test
    void quando_listar_todos_entao_delega_ao_repository() {
        List<Produto> lista = List.of(produtoBase);
        when(produtoRepository.findAllByEmpresaIdWithDetails(1)).thenReturn(lista);

        List<Produto> resultado = produtoService.listarTodos(1);

        assertThat(resultado).hasSize(1);
        verify(produtoRepository).findAllByEmpresaIdWithDetails(1);
    }

    // ---- contarAtivos / contarAbaixoMinimo ----

    @Test
    void quando_contar_ativos_entao_delega_ao_repository() {
        when(produtoRepository.countByEmpresaIdAndAtivoTrue(1)).thenReturn(7L);

        long total = produtoService.contarAtivos(1);

        assertThat(total).isEqualTo(7L);
    }

    @Test
    void quando_contar_abaixo_minimo_entao_retorna_quantidade_correta() {
        when(produtoRepository.findByEmpresaIdAndEstoqueAbaixoMinimo(1))
            .thenReturn(List.of(produtoBase, produtoBase));

        long total = produtoService.contarAbaixoMinimo(1);

        assertThat(total).isEqualTo(2L);
    }
}
