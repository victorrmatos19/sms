package com.erp.service;

import com.erp.model.Configuracao;
import com.erp.model.Empresa;
import com.erp.repository.ConfiguracaoRepository;
import com.erp.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do ConfiguracaoService.
 */
@ExtendWith(MockitoExtension.class)
class ConfiguracaoServiceTest {

    @Mock private ConfiguracaoRepository configuracaoRepository;
    @Mock private EmpresaRepository empresaRepository;

    @InjectMocks private ConfiguracaoService configuracaoService;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(1)
                .razaoSocial("Empresa Teste")
                .build();
    }

    // ---- 1. Sem config existente → cria padrão com tema DARK ----

    @Test
    void dado_empresa_sem_config_quando_buscar_entao_cria_padrao() {
        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.empty());
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));

        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        Configuracao salva = Configuracao.builder().empresa(empresa).build();
        when(configuracaoRepository.save(any())).thenReturn(salva);

        configuracaoService.buscarPorEmpresa(1);

        verify(configuracaoRepository).save(captor.capture());
        Configuracao nova = captor.getValue();
        // O builder default para tema é "DARK" (conforme definido na entidade)
        assertThat(nova.getTema()).isEqualTo("DARK");
    }

    // ---- 2. Config existente → retorna sem novo save ----

    @Test
    void dado_empresa_com_config_quando_buscar_entao_retorna_existente() {
        Configuracao existente = Configuracao.builder()
                .id(1)
                .empresa(empresa)
                .tema("LIGHT")
                .build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(existente));

        Configuracao resultado = configuracaoService.buscarPorEmpresa(1);

        verify(configuracaoRepository, never()).save(any());
        assertThat(resultado.getTema()).isEqualTo("LIGHT");
    }

    // ---- 3. salvarTema → persiste LIGHT ----

    @Test
    void dado_tema_light_quando_salvar_entao_persiste() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        when(configuracaoRepository.save(any())).thenReturn(config);

        configuracaoService.salvarTema(1, "LIGHT");

        verify(configuracaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTema()).isEqualTo("LIGHT");
    }

    // ---- 4. salvarTema → persiste DARK ----

    @Test
    void dado_tema_dark_quando_salvar_entao_persiste() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("LIGHT").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        ArgumentCaptor<Configuracao> captor = ArgumentCaptor.forClass(Configuracao.class);
        when(configuracaoRepository.save(any())).thenReturn(config);

        configuracaoService.salvarTema(1, "DARK");

        verify(configuracaoRepository).save(captor.capture());
        assertThat(captor.getValue().getTema()).isEqualTo("DARK");
    }

    // ---- 5. isModoEscuro → true quando tema DARK ----

    @Test
    void dado_tema_dark_quando_verificar_modo_escuro_entao_true() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("DARK").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        assertThat(configuracaoService.isModoEscuro(1)).isTrue();
    }

    // ---- 6. isModoEscuro → false quando tema LIGHT ----

    @Test
    void dado_tema_light_quando_verificar_modo_escuro_entao_false() {
        Configuracao config = Configuracao.builder()
                .id(1).empresa(empresa).tema("LIGHT").build();

        when(configuracaoRepository.findByEmpresaId(1)).thenReturn(Optional.of(config));

        assertThat(configuracaoService.isModoEscuro(1)).isFalse();
    }
}
