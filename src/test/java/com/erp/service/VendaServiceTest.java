package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Cliente;
import com.erp.model.Configuracao;
import com.erp.model.ContaReceber;
import com.erp.model.Empresa;
import com.erp.model.Funcionario;
import com.erp.model.MovimentacaoEstoque;
import com.erp.model.Produto;
import com.erp.model.Venda;
import com.erp.model.VendaItem;
import com.erp.repository.ConfiguracaoRepository;
import com.erp.repository.ContaReceberRepository;
import com.erp.repository.MovimentacaoEstoqueRepository;
import com.erp.repository.ProdutoRepository;
import com.erp.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock private VendaRepository vendaRepository;
    @Mock private ContaReceberRepository contaReceberRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private ConfiguracaoRepository configuracaoRepository;
    @Mock private AuthService authService;
    @Mock private CaixaService caixaService;
    @Mock private ClienteService clienteService;

    @InjectMocks private VendaService vendaService;

    private Empresa empresa;
    private Cliente cliente;
    private Funcionario vendedor;
    private Produto produto;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(1)
                .razaoSocial("Empresa Teste")
                .cnpj("00.000.000/0001-00")
                .regimeTributario("SIMPLES_NACIONAL")
                .build();

        cliente = Cliente.builder()
                .id(1)
                .empresa(empresa)
                .tipoPessoa("PF")
                .nome("Cliente Teste")
                .ativo(true)
                .build();

        vendedor = Funcionario.builder()
                .id(1)
                .empresa(empresa)
                .nome("Vendedor Teste")
                .ativo(true)
                .build();

        produto = Produto.builder()
                .id(1)
                .empresa(empresa)
                .descricao("Produto Teste")
                .precoVenda(new BigDecimal("20.00"))
                .precoCusto(new BigDecimal("12.00"))
                .estoqueAtual(new BigDecimal("10"))
                .estoqueMinimo(BigDecimal.ZERO)
                .ativo(true)
                .build();
    }

    @Test
    void dado_venda_a_vista_quando_registrar_entao_baixa_estoque_e_registra_pagamento() {
        Venda venda = novaVenda(new BigDecimal("2"));

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(
                Configuracao.builder().empresa(empresa).permiteVendaEstoqueZero(false).build()));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(vendaRepository.countByEmpresaId(1)).thenReturn(0L);
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> {
            Venda salva = inv.getArgument(0);
            salva.setId(7);
            return salva;
        });
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Venda salva = vendaService.registrarVendaDireta(venda, "DINHEIRO", 1);

        assertThat(salva.getNumero()).startsWith("V");
        assertThat(salva.getStatus()).isEqualTo("FINALIZADA");
        assertThat(salva.getValorTotal()).isEqualByComparingTo("40.00");
        assertThat(produto.getEstoqueAtual()).isEqualByComparingTo("8");
        assertThat(salva.getPagamentos()).hasSize(1);
        assertThat(salva.getPagamentos().get(0).getFormaPagamento()).isEqualTo("DINHEIRO");

        ArgumentCaptor<MovimentacaoEstoque> movCaptor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getOrigem()).isEqualTo("VENDA");
        assertThat(movCaptor.getValue().getOrigemId()).isEqualTo(7);
        assertThat(movCaptor.getValue().getTipo()).isEqualTo("SAIDA");
        verify(contaReceberRepository, never()).save(any());
        verify(caixaService).registrarVendaSeCaixaAberto(salva, "DINHEIRO");
    }

    @Test
    void dado_venda_a_prazo_sem_cliente_quando_registrar_entao_lanca_negocio_exception() {
        Venda venda = novaVenda(new BigDecimal("1"));
        venda.setCliente(null);

        assertThatThrownBy(() -> vendaService.registrarVendaDireta(venda, "PRAZO", 2))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Cliente");

        verify(vendaRepository, never()).save(any());
    }

    @Test
    void dado_venda_crediario_quando_registrar_entao_gera_contas_a_receber() {
        Venda venda = novaVenda(new BigDecimal("3"));

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.empty());
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));
        when(vendaRepository.countByEmpresaId(1)).thenReturn(4L);
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> {
            Venda salva = inv.getArgument(0);
            salva.setId(8);
            return salva;
        });
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(contaReceberRepository.save(any(ContaReceber.class))).thenAnswer(inv -> inv.getArgument(0));

        Venda salva = vendaService.registrarVendaDireta(venda, "CREDIARIO", 3);

        assertThat(salva.getNumero()).endsWith("00005");
        assertThat(salva.getValorTotal()).isEqualByComparingTo("60.00");
        verify(contaReceberRepository, times(3)).save(any(ContaReceber.class));
    }

    @Test
    void dado_mesmo_produto_em_duas_linhas_quando_total_excede_estoque_entao_bloqueia() {
        Venda venda = novaVenda(new BigDecimal("6"));
        VendaItem segundoItem = VendaItem.builder()
                .produto(produto)
                .descricao("Produto Teste")
                .quantidade(new BigDecimal("5"))
                .precoUnitario(new BigDecimal("20.00"))
                .desconto(BigDecimal.ZERO)
                .build();
        venda.getItens().add(segundoItem);

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.empty());
        when(produtoRepository.findById(1)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> vendaService.registrarVendaDireta(venda, "DINHEIRO", 1))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(vendaRepository, never()).save(any());
    }

    private Venda novaVenda(BigDecimal quantidade) {
        VendaItem item = VendaItem.builder()
                .produto(produto)
                .descricao("Produto Teste")
                .quantidade(quantidade)
                .precoUnitario(new BigDecimal("20.00"))
                .desconto(BigDecimal.ZERO)
                .build();

        Venda venda = Venda.builder()
                .empresa(empresa)
                .cliente(cliente)
                .vendedor(vendedor)
                .valorDesconto(BigDecimal.ZERO)
                .itens(new ArrayList<>(List.of(item)))
                .build();
        item.setVenda(venda);
        return venda;
    }
}
