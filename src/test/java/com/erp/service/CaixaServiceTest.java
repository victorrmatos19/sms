package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.Caixa;
import com.erp.model.CaixaMovimentacao;
import com.erp.model.CaixaSessao;
import com.erp.model.ContaPagar;
import com.erp.model.Empresa;
import com.erp.model.Usuario;
import com.erp.model.Venda;
import com.erp.repository.CaixaMovimentacaoRepository;
import com.erp.repository.CaixaRepository;
import com.erp.repository.CaixaSessaoRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaixaServiceTest {

    @Mock private CaixaRepository caixaRepository;
    @Mock private CaixaSessaoRepository caixaSessaoRepository;
    @Mock private CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    @Mock private AuthService authService;

    @InjectMocks private CaixaService caixaService;

    private Empresa empresa;
    private Usuario usuario;
    private Caixa caixa;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder().id(1).razaoSocial("Empresa Teste").build();
        usuario = Usuario.builder().id(1).nome("Operador").empresa(empresa).build();
        caixa = Caixa.builder().id(1).empresa(empresa).nome("Caixa Principal").ativo(true).build();
    }

    @Test
    void dado_sem_caixa_aberto_quando_abrir_entao_cria_sessao() {
        when(caixaSessaoRepository.findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(1, "ABERTO"))
                .thenReturn(Optional.empty());
        when(caixaRepository.findFirstByEmpresaIdAndAtivoTrueOrderByIdAsc(1)).thenReturn(Optional.of(caixa));
        when(authService.getUsuarioLogado()).thenReturn(usuario);
        when(caixaSessaoRepository.save(any(CaixaSessao.class))).thenAnswer(inv -> inv.getArgument(0));

        CaixaSessao sessao = caixaService.abrirCaixa(1, new BigDecimal("100.00"), "troco");

        assertThat(sessao.getCaixa()).isEqualTo(caixa);
        assertThat(sessao.getUsuario()).isEqualTo(usuario);
        assertThat(sessao.getStatus()).isEqualTo("ABERTO");
        assertThat(sessao.getSaldoInicial()).isEqualByComparingTo("100.00");
    }

    @Test
    void dado_caixa_aberto_quando_abrir_novo_entao_bloqueia() {
        CaixaSessao aberta = CaixaSessao.builder().id(10).caixa(caixa).usuario(usuario).status("ABERTO").build();
        when(caixaSessaoRepository.findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(1, "ABERTO"))
                .thenReturn(Optional.of(aberta));

        assertThatThrownBy(() -> caixaService.abrirCaixa(1, BigDecimal.ZERO, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Já existe");

        verify(caixaSessaoRepository, never()).save(any());
    }

    @Test
    void dado_movimentacoes_quando_fechar_entao_calcula_diferenca() {
        CaixaSessao aberta = CaixaSessao.builder()
                .id(10)
                .caixa(caixa)
                .usuario(usuario)
                .status("ABERTO")
                .saldoInicial(new BigDecimal("100.00"))
                .build();
        when(caixaSessaoRepository.findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(1, "ABERTO"))
                .thenReturn(Optional.of(aberta));
        when(caixaMovimentacaoRepository.sumValorBySessaoIdAndTipoIn(eq(10), eq(List.of("SUPRIMENTO", "VENDA", "RECEBIMENTO"))))
                .thenReturn(new BigDecimal("50.00"));
        when(caixaMovimentacaoRepository.sumValorBySessaoIdAndTipoIn(eq(10), eq(List.of("SANGRIA", "PAGAMENTO"))))
                .thenReturn(new BigDecimal("20.00"));
        when(caixaSessaoRepository.save(any(CaixaSessao.class))).thenAnswer(inv -> inv.getArgument(0));

        CaixaSessao fechada = caixaService.fecharCaixa(1, new BigDecimal("125.00"), "ok");

        assertThat(fechada.getStatus()).isEqualTo("FECHADO");
        assertThat(fechada.getSaldoFinalCalculado()).isEqualByComparingTo("130.00");
        assertThat(fechada.getSaldoFinalInformado()).isEqualByComparingTo("125.00");
        assertThat(fechada.getDiferenca()).isEqualByComparingTo("-5.00");
    }

    @Test
    void dado_caixa_aberto_quando_registrar_venda_entao_cria_entrada() {
        CaixaSessao aberta = CaixaSessao.builder().id(10).caixa(caixa).usuario(usuario).status("ABERTO").build();
        Venda venda = Venda.builder()
                .id(7)
                .empresa(empresa)
                .numero("V2026-00001")
                .valorTotal(new BigDecimal("80.00"))
                .usuario(usuario)
                .build();
        when(caixaSessaoRepository.findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(1, "ABERTO"))
                .thenReturn(Optional.of(aberta));
        when(caixaMovimentacaoRepository.save(any(CaixaMovimentacao.class))).thenAnswer(inv -> inv.getArgument(0));

        caixaService.registrarVendaSeCaixaAberto(venda, "PIX");

        ArgumentCaptor<CaixaMovimentacao> captor = ArgumentCaptor.forClass(CaixaMovimentacao.class);
        verify(caixaMovimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("VENDA");
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("80.00");
        assertThat(captor.getValue().getOrigem()).isEqualTo("VENDA");
        assertThat(captor.getValue().getOrigemId()).isEqualTo(7);
        assertThat(captor.getValue().getFormaPagamento()).isEqualTo("PIX");
    }

    @Test
    void dado_caixa_aberto_quando_registrar_pagamento_conta_pagar_entao_cria_saida() {
        CaixaSessao aberta = CaixaSessao.builder().id(10).caixa(caixa).usuario(usuario).status("ABERTO").build();
        ContaPagar conta = ContaPagar.builder()
                .id(12)
                .empresa(empresa)
                .descricao("Compra C2026-00001 - Parcela 1/1")
                .usuario(usuario)
                .build();
        when(caixaSessaoRepository.findFirstByCaixaEmpresaIdAndStatusOrderByDataAberturaDesc(1, "ABERTO"))
                .thenReturn(Optional.of(aberta));
        when(caixaMovimentacaoRepository.save(any(CaixaMovimentacao.class))).thenAnswer(inv -> inv.getArgument(0));

        caixaService.registrarPagamentoContaPagarSeCaixaAberto(conta, new BigDecimal("45.50"), "BOLETO");

        ArgumentCaptor<CaixaMovimentacao> captor = ArgumentCaptor.forClass(CaixaMovimentacao.class);
        verify(caixaMovimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo("PAGAMENTO");
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("45.50");
        assertThat(captor.getValue().getOrigem()).isEqualTo("CONTA_PAGAR");
        assertThat(captor.getValue().getOrigemId()).isEqualTo(12);
        assertThat(captor.getValue().getFormaPagamento()).isEqualTo("BOLETO");
    }
}
