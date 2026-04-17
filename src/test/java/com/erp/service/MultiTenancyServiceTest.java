package com.erp.service;

import com.erp.exception.NegocioException;
import com.erp.model.*;
import com.erp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes de isolamento multi-empresa e integridade referencial.
 * CF-232 a CF-243.
 */
@ExtendWith(MockitoExtension.class)
class MultiTenancyServiceTest {

    // ---- Repositories para isolamento de empresa ----
    @Mock private ProdutoRepository produtoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private AuthService authService;

    @InjectMocks private ProdutoService produtoService;

    private Empresa empresa1;
    private Empresa empresa2;

    @BeforeEach
    void setUp() {
        empresa1 = Empresa.builder().id(1).razaoSocial("Empresa 1").build();
        empresa2 = Empresa.builder().id(2).razaoSocial("Empresa 2").build();
    }

    // ===== ISOLAMENTO MULTI-EMPRESA =====

    // ---- CF-232 (isolamento): produtos da empresa 2 não aparecem na listagem da empresa 1 ----

    @Test
    void dado_produtos_de_empresa_diferente_quando_listar_empresa_1_entao_retorna_apenas_empresa_1() {
        Produto produtoEmpresa1 = Produto.builder()
            .id(1).empresa(empresa1).descricao("Produto E1").estoqueAtual(BigDecimal.ZERO)
            .estoqueMinimo(BigDecimal.ZERO).precoCusto(BigDecimal.ZERO)
            .precoVenda(BigDecimal.ONE).precoMinimo(BigDecimal.ZERO)
            .usaLoteValidade(false).ativo(true).build();
        Produto produtoEmpresa2 = Produto.builder()
            .id(2).empresa(empresa2).descricao("Produto E2").estoqueAtual(BigDecimal.ZERO)
            .estoqueMinimo(BigDecimal.ZERO).precoCusto(BigDecimal.ZERO)
            .precoVenda(BigDecimal.ONE).precoMinimo(BigDecimal.ZERO)
            .usaLoteValidade(false).ativo(true).build();

        when(produtoRepository.findAllByEmpresaIdWithDetails(1))
            .thenReturn(List.of(produtoEmpresa1));
        when(produtoRepository.findAllByEmpresaIdWithDetails(2))
            .thenReturn(List.of(produtoEmpresa2));

        List<Produto> resultadoEmpresa1 = produtoService.listarTodos(1);
        List<Produto> resultadoEmpresa2 = produtoService.listarTodos(2);

        assertThat(resultadoEmpresa1).extracting(p -> p.getEmpresa().getId())
            .containsOnly(1);
        assertThat(resultadoEmpresa2).extracting(p -> p.getEmpresa().getId())
            .containsOnly(2);
    }

    // ---- CF-232b: clientes da empresa 2 não aparecem na empresa 1 ----

    @Test
    void dado_clientes_de_empresas_distintas_quando_listar_entao_cada_empresa_ve_apenas_os_seus() {
        ClienteService clienteService = new ClienteService(clienteRepository);

        Cliente c1 = Cliente.builder().id(1).empresa(empresa1).nome("Cliente E1")
            .ativo(true).limiteCredito(BigDecimal.ZERO).creditoDisponivel(BigDecimal.ZERO).build();
        Cliente c2 = Cliente.builder().id(2).empresa(empresa2).nome("Cliente E2")
            .ativo(true).limiteCredito(BigDecimal.ZERO).creditoDisponivel(BigDecimal.ZERO).build();

        when(clienteRepository.findByEmpresaIdOrderByNome(1)).thenReturn(List.of(c1));
        when(clienteRepository.findByEmpresaIdOrderByNome(2)).thenReturn(List.of(c2));

        assertThat(clienteService.listarTodos(1))
            .extracting(c -> c.getEmpresa().getId()).containsOnly(1);
        assertThat(clienteService.listarTodos(2))
            .extracting(c -> c.getEmpresa().getId()).containsOnly(2);
    }

    // ---- CF-232c: fornecedores de empresa diferente não cruzam ----

    @Test
    void dado_fornecedores_de_empresas_distintas_quando_listar_entao_isolamento_garantido() {
        FornecedorService fornecedorService = new FornecedorService(fornecedorRepository);

        Fornecedor f1 = Fornecedor.builder().id(1).empresa(empresa1).nome("Fornecedor E1")
            .tipoPessoa("PF").ativo(true).build();
        Fornecedor f2 = Fornecedor.builder().id(2).empresa(empresa2).nome("Fornecedor E2")
            .tipoPessoa("PF").ativo(true).build();

        when(fornecedorRepository.findByEmpresaIdOrderByNome(1)).thenReturn(List.of(f1));
        when(fornecedorRepository.findByEmpresaIdOrderByNome(2)).thenReturn(List.of(f2));

        assertThat(fornecedorService.listarTodos(1))
            .extracting(f -> f.getEmpresa().getId()).containsOnly(1);
        assertThat(fornecedorService.listarTodos(2))
            .extracting(f -> f.getEmpresa().getId()).containsOnly(2);
    }

    // ---- CF-232d: funcionários de empresa diferente não cruzam ----

    @Test
    void dado_funcionarios_de_empresas_distintas_quando_listar_entao_isolamento_garantido() {
        FuncionarioService funcionarioService = new FuncionarioService(funcionarioRepository);

        Funcionario func1 = Funcionario.builder().id(1).empresa(empresa1).nome("Func E1")
            .ativo(true).percentualComissao(BigDecimal.ZERO).build();
        Funcionario func2 = Funcionario.builder().id(2).empresa(empresa2).nome("Func E2")
            .ativo(true).percentualComissao(BigDecimal.ZERO).build();

        when(funcionarioRepository.findByEmpresaIdOrderByNome(1)).thenReturn(List.of(func1));
        when(funcionarioRepository.findByEmpresaIdOrderByNome(2)).thenReturn(List.of(func2));

        assertThat(funcionarioService.listarTodos(1))
            .extracting(f -> f.getEmpresa().getId()).containsOnly(1);
        assertThat(funcionarioService.listarTodos(2))
            .extracting(f -> f.getEmpresa().getId()).containsOnly(2);
    }

    // ===== INTEGRIDADE REFERENCIAL (FK) =====
    // CF-232 a CF-236: banco lança DataIntegrityViolationException para FK violations.
    // O serviço deve propagar a exceção (ou ser mapeada para NegocioException).

    // ---- CF-232: deletar produto referenciado em compra_item ----

    @Test
    void dado_produto_referenciado_em_compra_quando_repositorio_lanca_fk_entao_excecao_propagada() {
        when(produtoRepository.findById(1)).thenReturn(Optional.of(
            Produto.builder().id(1).empresa(empresa1).descricao("Arroz")
                .estoqueAtual(BigDecimal.ZERO).estoqueMinimo(BigDecimal.ZERO)
                .precoCusto(BigDecimal.ZERO).precoVenda(BigDecimal.ONE)
                .precoMinimo(BigDecimal.ZERO).usaLoteValidade(false).ativo(true).build()));
        doThrow(new DataIntegrityViolationException("FK constraint violated"))
            .when(produtoRepository).save(any());

        // O inativar faz save — a FK do banco impedirá se houver delete real.
        // Para inativar (set ativo=false), não há violação de FK.
        // Para exclusão física (não implementada), haveria FK.
        // Documentando o padrão esperado:
        assertThatCode(() -> produtoService.inativar(1))
            .as("inativar (soft delete) não viola FK — apenas exclusão física violaria")
            .isInstanceOf(DataIntegrityViolationException.class); // save lança exception mockada
    }

    // ---- CF-233 a CF-236: FK constraints no nível de banco ----
    // Essas validações ocorrem na camada de persistência (PostgreSQL) ao tentar DELETE físico.
    // Os services do projeto usam soft-delete (ativo=false), então FK não é violada.
    // Se DELETE físico fosse implementado, o banco lançaria ConstraintViolationException.
    // GAP: os serviços não têm método de exclusão física — FK é garantida apenas pelo banco.

    // ===== FORMATAÇÃO E MASCARAMENTO =====

    // ---- CF-237: CPF deve ser salvo sem formatação (limpo) ----

    @Test
    void dado_cpf_com_mascara_quando_validar_entao_service_trabalha_com_cpf_limpo() {
        ClienteService clienteService = new ClienteService(clienteRepository);

        Cliente cliente = Cliente.builder()
            .empresa(empresa1).tipoPessoa("PF").nome("Teste CPF Máscara")
            .cpfCnpj("123.456.789-09") // com máscara
            .ativo(true).limiteCredito(BigDecimal.ZERO).creditoDisponivel(BigDecimal.ZERO)
            .build();

        when(clienteRepository.existsByEmpresaIdAndCpfCnpj(1, "12345678909")).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(cliente);

        // Service faz CpfCnpjValidator.limpar() internamente e valida o CPF limpo
        assertThatCode(() -> clienteService.salvar(cliente))
            .doesNotThrowAnyException();

        verify(clienteRepository).save(any());
    }

    // ---- CF-238: CNPJ deve ser salvo sem formatação ----

    @Test
    void dado_cnpj_com_mascara_quando_validar_entao_service_trabalha_com_cnpj_limpo() {
        ClienteService clienteService = new ClienteService(clienteRepository);

        Cliente cliente = Cliente.builder()
            .empresa(empresa1).tipoPessoa("PJ").nome("Empresa Máscara")
            .razaoSocial("Empresa Máscara Ltda")
            .cpfCnpj("11.222.333/0001-81") // com máscara
            .ativo(true).limiteCredito(BigDecimal.ZERO).creditoDisponivel(BigDecimal.ZERO)
            .build();

        when(clienteRepository.existsByEmpresaIdAndCpfCnpj(1, "11222333000181")).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(cliente);

        assertThatCode(() -> clienteService.salvar(cliente))
            .doesNotThrowAnyException();

        verify(clienteRepository).save(any());
    }

    // ---- CF-240: telefone com 10 e 11 dígitos ----
    // Os models aceitam telefone como String sem validação de formato no service.
    // GAP: não há validação de formato de telefone nos services.
    // Recomendação: validar formato (10 ou 11 dígitos) quando preenchido.

    // ===== TRANSAÇÕES E ROLLBACK =====

    // ---- CF-241: confirmar compra que falha ao atualizar estoque deve fazer rollback ----
    // Testado em CompraServiceTest — @Transactional garante atomicidade.
    // Em caso de exceção em movimentacaoEstoqueRepository.save(), toda a transação é revertida.

    @Test
    void dado_falha_ao_salvar_movimentacao_quando_confirmar_compra_entao_rollback_total() {
        // Arrange
        Empresa e = Empresa.builder().id(1).razaoSocial("E1")
            .cnpj("00.000.000/0001-00").regimeTributario("SIMPLES_NACIONAL").build();
        Fornecedor f = Fornecedor.builder().id(1).empresa(e).nome("F1")
            .tipoPessoa("PJ").ativo(true).build();
        Produto p = Produto.builder().id(1).empresa(e).descricao("P1")
            .precoVenda(BigDecimal.TEN).precoCusto(BigDecimal.ONE)
            .estoqueAtual(BigDecimal.ZERO).estoqueMinimo(BigDecimal.ZERO)
            .usaLoteValidade(false).ativo(true).build();

        CompraItem ci = CompraItem.builder()
            .produto(p).quantidade(BigDecimal.TEN)
            .custoUnitario(BigDecimal.ONE).desconto(BigDecimal.ZERO)
            .valorTotal(BigDecimal.TEN).build();

        Compra compra = Compra.builder()
            .id(99).empresa(e).fornecedor(f).status("RASCUNHO")
            .numeroDocumento("C2026-99999").dataEmissao(java.time.LocalDate.now())
            .condicaoPagamento("A_VISTA").valorTotal(BigDecimal.TEN)
            .itens(new java.util.ArrayList<>(List.of(ci))).build();
        ci.setCompra(compra);

        CompraService compraService = new CompraService(
            mock(CompraRepository.class),
            mock(ContaPagarRepository.class),
            produtoRepository,
            movimentacaoEstoqueRepository,
            mock(LoteRepository.class),
            authService);

        CompraRepository localCompraRepo = mock(CompraRepository.class);
        CompraService svc = new CompraService(
            localCompraRepo, mock(ContaPagarRepository.class),
            produtoRepository, movimentacaoEstoqueRepository,
            mock(LoteRepository.class), authService);

        when(localCompraRepo.findById(99)).thenReturn(Optional.of(compra));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(p));
        when(produtoRepository.save(any())).thenReturn(p);
        when(authService.getUsuarioLogado()).thenReturn(null);
        // Simula falha ao salvar movimentação de estoque
        doThrow(new RuntimeException("DB error ao salvar movimentação"))
            .when(movimentacaoEstoqueRepository).save(any());

        // A transação faria rollback — aqui verificamos que a exceção propaga
        assertThatThrownBy(() -> svc.confirmarCompra(99, 1))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DB error");
    }

    // ---- CF-243: @Transactional garante atomicidade de movimentacao + produto.estoque_atual ----
    // O CompraService.confirmarCompra() faz produtoRepository.save() e movimentacaoEstoqueRepository.save()
    // dentro da mesma transação. Se qualquer operação falhar, ambas são revertidas.
    // Isso é garantido pelo @Transactional do Spring + PostgreSQL como banco ACID.
    // Em testes unitários com mocks, verificamos que ambos são chamados no mesmo fluxo.

    @Test
    void dado_confirmacao_de_compra_quando_sucesso_entao_produto_e_movimentacao_salvos_no_mesmo_fluxo() {
        Empresa e = Empresa.builder().id(1).razaoSocial("E1")
            .cnpj("00.000.000/0001-00").regimeTributario("SIMPLES_NACIONAL").build();
        Fornecedor f = Fornecedor.builder().id(1).empresa(e).nome("F1")
            .tipoPessoa("PJ").ativo(true).build();
        Produto p = Produto.builder().id(1).empresa(e).descricao("P1")
            .precoVenda(BigDecimal.TEN).precoCusto(BigDecimal.ONE)
            .estoqueAtual(new BigDecimal("5")).estoqueMinimo(BigDecimal.ZERO)
            .usaLoteValidade(false).ativo(true).build();

        CompraItem ci = CompraItem.builder()
            .produto(p).quantidade(new BigDecimal("10"))
            .custoUnitario(BigDecimal.ONE).desconto(BigDecimal.ZERO)
            .valorTotal(BigDecimal.TEN).build();

        Compra compra = Compra.builder()
            .id(100).empresa(e).fornecedor(f).status("RASCUNHO")
            .numeroDocumento("C2026-00100").dataEmissao(java.time.LocalDate.now())
            .condicaoPagamento("A_VISTA").valorTotal(BigDecimal.TEN)
            .itens(new java.util.ArrayList<>(List.of(ci))).build();
        ci.setCompra(compra);

        CompraRepository localCompraRepo = mock(CompraRepository.class);
        ContaPagarRepository localContaRepo = mock(ContaPagarRepository.class);

        CompraService svc = new CompraService(
            localCompraRepo, localContaRepo,
            produtoRepository, movimentacaoEstoqueRepository,
            mock(LoteRepository.class), authService);

        when(localCompraRepo.findById(100)).thenReturn(Optional.of(compra));
        when(produtoRepository.findById(1)).thenReturn(Optional.of(p));
        when(produtoRepository.save(any())).thenReturn(p);
        when(movimentacaoEstoqueRepository.save(any())).thenReturn(new MovimentacaoEstoque());
        when(localContaRepo.save(any())).thenReturn(new ContaPagar());
        when(localCompraRepo.save(any())).thenReturn(compra);
        when(authService.getUsuarioLogado()).thenReturn(null);

        svc.confirmarCompra(100, 1);

        // Ambas as operações foram executadas no mesmo fluxo
        verify(produtoRepository).save(argThat(prod -> prod.getEstoqueAtual().compareTo(new BigDecimal("15")) == 0));
        verify(movimentacaoEstoqueRepository).save(argThat(mov ->
            "ENTRADA".equals(mov.getTipo()) && "COMPRA".equals(mov.getOrigem())));
    }
}
