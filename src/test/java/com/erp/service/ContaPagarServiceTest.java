package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.ContaPagar;
import com.erp.model.Empresa;
import com.erp.model.Fornecedor;
import com.erp.model.Usuario;
import com.erp.model.dto.financeiro.ContasPagarResumoDTO;
import com.erp.repository.ContaPagarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaPagarServiceTest {

    @Mock private ContaPagarRepository contaPagarRepository;
    @Mock private AuthService authService;
    @Mock private CaixaService caixaService;

    @InjectMocks private ContaPagarService contaPagarService;

    private Empresa empresa;
    private Usuario usuario;
    private ContaPagar conta;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder().id(1).razaoSocial("Empresa Teste").build();
        usuario = Usuario.builder().id(1).nome("Financeiro").empresa(empresa).build();
        Fornecedor fornecedor = Fornecedor.builder()
                .id(1)
                .empresa(empresa)
                .tipoPessoa("PJ")
                .nome("Fornecedor")
                .razaoSocial("Fornecedor Ltda")
                .build();
        conta = ContaPagar.builder()
                .id(10)
                .empresa(empresa)
                .fornecedor(fornecedor)
                .descricao("Compra C2026-00001 - Parcela 1/1")
                .numeroParcela(1)
                .totalParcelas(1)
                .valor(new BigDecimal("100.00"))
                .dataEmissao(LocalDate.now())
                .dataVencimento(LocalDate.now())
                .status("ABERTA")
                .build();
    }

    @Test
    void dado_conta_aberta_quando_baixar_entao_marca_paga_e_registra_caixa() {
        when(contaPagarRepository.findByIdComRelacionamentos(10)).thenReturn(Optional.of(conta));
        when(authService.getUsuarioLogado()).thenReturn(usuario);
        when(contaPagarRepository.save(any(ContaPagar.class))).thenAnswer(inv -> inv.getArgument(0));

        ContaPagar paga = contaPagarService.baixar(10, new BigDecimal("103.00"),
                new BigDecimal("2.00"), new BigDecimal("1.00"), BigDecimal.ZERO,
                LocalDate.now(), "PIX", "ok");

        assertThat(paga.getStatus()).isEqualTo("PAGA");
        assertThat(paga.getValorPago()).isEqualByComparingTo("103.00");
        assertThat(paga.getJuros()).isEqualByComparingTo("2.00");
        assertThat(paga.getMulta()).isEqualByComparingTo("1.00");
        assertThat(paga.getFormaPagamento()).isEqualTo("PIX");
        assertThat(paga.getUsuario()).isEqualTo(usuario);
        verify(caixaService).registrarPagamentoContaPagarSeCaixaAberto(paga, new BigDecimal("103.00"), "PIX");
    }

    @Test
    void dado_desconto_maior_que_valor_quando_baixar_entao_bloqueia() {
        when(contaPagarRepository.findByIdComRelacionamentos(10)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> contaPagarService.baixar(10, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("101.00"),
                LocalDate.now(), "PIX", null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Desconto");

        verify(contaPagarRepository, never()).save(any());
        verify(caixaService, never()).registrarPagamentoContaPagarSeCaixaAberto(any(), any(), any());
    }

    @Test
    void dado_conta_paga_quando_cancelar_entao_bloqueia() {
        conta.setStatus("PAGA");
        when(contaPagarRepository.findByIdComRelacionamentos(10)).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> contaPagarService.cancelar(10, "erro"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("paga");

        verify(contaPagarRepository, never()).save(any());
    }

    @Test
    void dado_repositorio_quando_obter_resumo_entao_retorna_metricas() {
        when(contaPagarRepository.countByEmpresaIdAndStatus(1, "ABERTA")).thenReturn(3L);
        when(contaPagarRepository.sumValorByEmpresaIdAndStatus(1, "ABERTA"))
                .thenReturn(new BigDecimal("300.00"));
        when(contaPagarRepository.countVencidas(any(), any())).thenReturn(1L);
        when(contaPagarRepository.sumValorVencidas(any(), any())).thenReturn(new BigDecimal("80.00"));
        when(contaPagarRepository.countVencendoHoje(any(), any())).thenReturn(2L);
        when(contaPagarRepository.sumValorVencendoHoje(any(), any())).thenReturn(new BigDecimal("150.00"));
        when(contaPagarRepository.countPagasPeriodo(any(), any(), any())).thenReturn(4L);
        when(contaPagarRepository.sumValorPagoPeriodo(any(), any(), any())).thenReturn(new BigDecimal("500.00"));

        ContasPagarResumoDTO resumo = contaPagarService.obterResumo(1);

        assertThat(resumo.abertas()).isEqualTo(3L);
        assertThat(resumo.totalAbertas()).isEqualByComparingTo("300.00");
        assertThat(resumo.vencidas()).isEqualTo(1L);
        assertThat(resumo.totalVencidas()).isEqualByComparingTo("80.00");
        assertThat(resumo.vencendoHoje()).isEqualTo(2L);
        assertThat(resumo.totalVencendoHoje()).isEqualByComparingTo("150.00");
        assertThat(resumo.pagasMes()).isEqualTo(4L);
        assertThat(resumo.totalPagasMes()).isEqualByComparingTo("500.00");
    }
}
