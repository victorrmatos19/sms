# CLAUDE.md — SMS Desktop (Simple Manage System)

Arquivo de contexto para agentes de código trabalharem neste projeto.

---

## Visão Geral

O projeto é um ERP desktop offline-first para pequenas empresas brasileiras.
O produto usa PostgreSQL embarcado local, UI JavaFX e Spring Boot sem servidor web.

**Nome do produto:** SMS — Simple Manage System
**Tipo:** aplicativo desktop local com distribuição planejada por assinatura
**Estado atual:** Fase 1 comercial implementada até Vendas, com financeiro iniciado por Caixa e Contas a Pagar

O foco de negócio atual é transformar o núcleo cadastral/estoque/compras/orçamentos/vendas/financeiro em um fluxo comercial confiável: cadastrar produtos e clientes, comprar, movimentar estoque, emitir orçamento, converter em venda, registrar venda direta, movimentar o caixa, baixar obrigações e refletir isso nos dashboards.

---

## Stack Técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 LTS |
| Interface | JavaFX 21 + AtlantaFX |
| Banco local | PostgreSQL embarcado via `io.zonky.test.db.postgres.embedded` |
| ORM | Hibernate + Spring Data JPA |
| DI / Core | Spring Boot 3.2.5 sem web |
| Migrations | Flyway |
| Relatórios | JasperReports 6.21.4 |
| Build | Maven Wrapper (`./mvnw`) |
| Ícones | Ikonli |
| Testes | JUnit 5, Mockito, AssertJ, TestFX |

---

## Estrutura

```text
src/main/java/com/erp/
├── config/       # EmbeddedPostgresConfig, JpaConfig, SecurityConfig
├── controller/   # Controllers JavaFX
├── model/        # Entidades JPA e DTOs
├── repository/   # Spring Data repositories
├── service/      # Regras de negócio
└── util/         # Helpers compartilhados, incluindo MoneyUtils

src/main/resources/
├── css/          # global.css
├── db/migration/ # Flyway migrations V1..V6
├── fxml/         # Telas JavaFX
├── images/       # Logo e ícones
└── reports/      # Templates JasperReports
```

Novos arquivos adicionados no módulo Contas a Receber:

- `src/main/java/com/erp/repository/ContaReceberRepository.java` (reescrito com JOIN FETCH e queries de resumo)
- `src/main/java/com/erp/model/dto/financeiro/ContasReceberResumoDTO.java`
- `src/main/java/com/erp/service/ContaReceberService.java`
- `src/main/java/com/erp/controller/ContasReceberController.java`
- `src/main/resources/fxml/contas-receber.fxml`
- `src/main/resources/db/migration/V6__mock_contas_receber.sql`

FXMLs principais existentes:

- Login e shell: `login.fxml`, `main.fxml`
- Dashboards: `dashboard-admin.fxml`, `dashboard-vendas.fxml`, `dashboard-financeiro.fxml`, `dashboard-estoque.fxml`
- Cadastros: `produtos.fxml`, `clientes.fxml`, `fornecedores.fxml`, `funcionarios.fxml`
- Formulários: `produto-form.fxml`, `cliente-form.fxml`, `fornecedor-form.fxml`, `funcionario-form.fxml`
- Auxiliares: `grupo-produto-form.fxml`, `unidade-medida-form.fxml`
- Estoque: `estoque.fxml`, `movimentacao-form.fxml`
- Compras: `compras.fxml`, `compra-form.fxml`
- Orçamentos: `orcamentos.fxml`, `orcamento-form.fxml`, `orcamento-conversao.fxml`
- Vendas: `vendas.fxml`, `venda-form.fxml`
- Financeiro: `caixa.fxml`, `contas-pagar.fxml`

---

## Estado Funcional Atual

### Autenticação e Shell

- Login com `AuthService`.
- Perfis usados para direcionar dashboard inicial: administrador, vendas, financeiro e estoque.
- `MainController` carrega módulos dentro do `StackPane#conteudoPane`.
- `StageManager` centraliza tema, CSS global e dark mode.

### Cadastros

- Produtos com grupo, categoria, unidade, preços, estoque mínimo/máximo, lote/validade e status ativo.
- Clientes PF/PJ com endereço, contato e limite de crédito.
- Fornecedores PF/PJ com dados bancários e PIX.
- Funcionários com perfil, cargo, contato e status.
- Grupos de produto e unidades de medida em modais auxiliares.

### Estoque

- Posição de estoque e histórico de movimentações.
- Entrada manual, saída manual, ajuste de inventário.
- Suporte a lote/validade quando o produto exige.
- Alertas de estoque zerado e abaixo do mínimo.
- Histórico foi ajustado para evitar `LazyInitializationException` ao exibir dados relacionados.

### Compras

- Listagem e formulário de compra.
- Compra em rascunho/confirmada/cancelada.
- Itens com custo unitário, desconto, lote e validade.
- Confirmação de compra atualiza estoque e gera contas a pagar.
- Busca de produtos em combo editável.

### Orçamentos

- Listagem, formulário e itens de orçamento.
- Validade, vendedor, cliente e desconto global.
- Validação de preço mínimo do produto.
- Conversão de orçamento em venda.
- Geração de PDF com JasperReports em `reports/orcamento.jrxml`.
- PDF abre via `Desktop.open` quando suportado e cai para comandos nativos (`open`, `xdg-open`, `rundll32`) quando necessário.
- Jasper recebe `REPORT_LOCALE` `pt-BR` para formatação brasileira.

### Vendas

- Listagem de vendas com métricas de mês, valor e finalizadas.
- Venda direta com cliente opcional, vendedor obrigatório, itens, desconto global, forma de pagamento e parcelas.
- Formas suportadas: dinheiro, PIX, cartão débito, cartão crédito, prazo e crediário.
- Vendas a prazo/crediário exigem cliente e geram contas a receber.
- Venda baixa estoque e registra movimentação de origem `VENDA`.
- Produto na linha da venda é pesquisável por descrição, código interno e código de barras.
- Respeita configuração de permitir ou bloquear venda com estoque insuficiente.

### Caixa

- Primeira versão operacional do módulo financeiro.
- Abertura de caixa com saldo inicial e operador logado.
- Bloqueio para impedir mais de um caixa aberto por empresa.
- Movimentações manuais de suprimento e sangria.
- Fechamento com saldo final informado, saldo calculado e diferença.
- Vendas à vista registram entrada no caixa quando existe caixa aberto.
- Vendas a prazo/crediário não entram no caixa no momento da venda.
- Tela exibe saldo atual, entradas, saídas, operador, data de abertura e histórico do caixa atual.

### Contas a Pagar

- Tela operacional para títulos gerados pela confirmação de compras.
- Listagem com fornecedor, descrição, parcela, vencimento, valor, valor pago, forma e status.
- Filtros por todas, abertas, vencidas, vencendo hoje, pagas e canceladas.
- Busca por fornecedor, descrição ou número da compra.
- Métricas de abertas, vencidas, vencendo hoje e pagas no mês.
- Baixa com valor pago, juros, multa, desconto, data de pagamento, forma e observações.
- Cancelamento de contas abertas.
- Baixas registram saída no caixa aberto como movimentação `PAGAMENTO`.

### Contas a Receber

- Tela operacional para recebíveis gerados por vendas a prazo/crediário e receitas avulsas.
- Listagem com cliente, descrição, parcela, vencimento, valor, valor recebido, forma e status.
- Filtros por todas, abertas, vencidas, vencendo hoje, recebidas e canceladas.
- Busca por cliente, descrição ou número da venda.
- Métricas de abertas, vencidas, vencendo hoje e recebidas no mês.
- Baixa (recebimento) com valor recebido, juros, multa, desconto, data, forma e observações.
- Cadastro avulso de receitas não vinculadas a vendas (cliente opcional).
- Cancelamento de contas abertas.
- Status da conta: ABERTA, RECEBIDA, VENCIDA (calculada na UI), CANCELADA.
- Baixas registram entrada no caixa aberto como movimentação `RECEBIMENTO` via `CaixaService`.
- `TODO`: incrementar `credito_disponivel` do cliente ao receber (aguarda método no `ClienteService`).

### Dashboards

- Dashboard admin exibe cards de vendas hoje, contas a pagar hoje, caixa atual, ticket médio, gráfico de vendas dos últimos 7 dias, alertas e top produtos vendidos no mês.
- O card `VENDAS HOJE` está implementado: mostra valor/quantidade de vendas finalizadas no dia e abre modal com horário, número, cliente, vendedor, forma de pagamento e valor.
- Dashboard de vendas exibe orçamentos, vendas e compras do mês.
- Dashboard financeiro exibe contas a pagar abertas/vencidas e compras do mês.
- Dashboard estoque exibe produtos, abaixo do mínimo, entradas e saídas do mês.

### Relatórios

- Orçamento em PDF via JasperReports.
- Template atual: `src/main/resources/reports/orcamento.jrxml`.
- Dados do PDF são montados em `OrcamentoPdfService` com DTO interno para itens.

---

## Mudanças Recentes Importantes

### Formatação Monetária

Foi criado `com.erp.util.MoneyUtils` como ponto único para moeda.

Regras:

- Exibição monetária: `MoneyUtils.formatCurrency(BigDecimal)` retorna padrão brasileiro com `R$` e duas casas.
- Campos editáveis de dinheiro: `MoneyUtils.formatInput(BigDecimal)` retorna `#,##0.00` em `pt-BR`.
- Parsing: `MoneyUtils.parse(String)` aceita `24,90`, `24.90`, `R$ 1.234,56`, `1.234` e evita transformar `24.90` em `2490`.

Usar `MoneyUtils` em qualquer novo campo monetário. Não reintroduzir parsers locais como:

```java
text.replace(".", "").replace(",", ".")
```

Esse padrão antigo causava cálculo incorreto quando o usuário digitava ponto decimal.

Quantidade e estoque não são moeda. Para quantidade, manter `NumberFormat` próprio com até 4 casas quando necessário.

### Dashboard Admin / Vendas Hoje

Foram adicionados dados reais de venda ao dashboard admin:

- `DashboardAdminDTO` agora inclui `vendasHoje`, `valorVendasHoje`, `ticketMedioUltimos7Dias`, `vendasSemana` e `vendasHojeDetalhes`.
- `VendaResumoDTO` representa linhas do modal de vendas do dia.
- `VendaRepository` possui queries para vendas finalizadas por período e top produtos vendidos.
- `DashboardService` calcula vendas de hoje, gráfico semanal, ticket médio e ranking por vendas.
- `DashboardAdminController` abre modal ao clicar no card `VENDAS HOJE`.
- O card `CAIXA ATUAL` usa o resumo real do `CaixaService`, mostrando saldo atual e, quando aberto, entradas e saídas do caixa.

### Módulo Caixa

- `CaixaService` centraliza abertura, fechamento, suprimento, sangria, resumo atual e integração com vendas.
- `CaixaController` e `caixa.fxml` implementam a tela inicial de operação do caixa.
- `MainController#abrirCaixa` agora carrega a tela real do módulo.
- `VendaService` registra movimentação de caixa para vendas à vista quando há caixa aberto.
- `CaixaService` também registra saída de caixa para baixa de contas a pagar quando há caixa aberto.
- As tabelas `caixa`, `caixa_sessao` e `caixa_movimentacao` já existiam na migration inicial, então não foi necessária nova migration.

### Módulo Contas a Pagar

- `ContaPagarService` centraliza listagem, resumo, baixa e cancelamento.
- `ContasPagarController` e `contas-pagar.fxml` implementam a tela financeira de contas a pagar.
- `ContaPagarRepository` possui consultas com `JOIN FETCH` para evitar lazy loading na UI.
- `MainController#abrirContasPagar` agora carrega a tela real do módulo.
- A tabela `conta_pagar` já existia na migration inicial e é alimentada por compras confirmadas.

### Módulo Contas a Receber

- `ContaReceberService` centraliza listagem, resumo, baixa, cancelamento e cadastro avulso.
- `ContasReceberController` e `contas-receber.fxml` implementam a tela financeira de contas a receber.
- `ContaReceberRepository` reescrito com `JOIN FETCH` para cliente, venda e usuário, além de queries de contagem e soma por status/período.
- `CaixaService` ganhou o método `registrarRecebimentoContaReceberSeCaixaAberto` para integrar recebimentos ao caixa aberto.
- `MainController#abrirContasReceber` agora carrega a tela real do módulo.
- `V6__mock_contas_receber.sql` inseriu 14 contas mock: abertas, vencidas, vencendo hoje, recebidas e cancelada.
- A tabela `conta_receber` já existia na migration V1 e é alimentada por vendas a prazo/crediário via `VendaService`.

### PDF de Orçamento

- JasperReports está em versão 6.21.4.
- O template `orcamento.jrxml` foi mantido em formato compatível com Jasper 6.
- `OrcamentoPdfService` compila o `.jrxml`, preenche parâmetros, exporta bytes e tenta abrir o PDF.
- Para macOS, há fallback para comando nativo `open`.

### Visualizador de PDF

Em ambientes onde `Desktop.Action.OPEN` não é suportado, o serviço tenta comandos nativos por sistema operacional.
Se ainda assim falhar, o PDF permanece salvo no diretório temporário e a exceção informa o caminho.

### PostgreSQL Embarcado

O log abaixo pode aparecer sem quebrar a aplicação:

```text
FileAlreadyExistsException: .../T/embedded-pg/.../fix-CVE-2024-4317.sql
```

Isso vem do cache de extração dos binários do PostgreSQL embarcado em `/var/folders/.../T/embedded-pg`.
Normalmente é ruído da lib Zonky tentando descompactar arquivo já existente. Não é o banco persistente do ERP.

Para limpar com a aplicação fechada:

```bash
rm -rf /var/folders/vb/s6_m_53s1315y206glb3xdwc0000gn/T/embedded-pg
```

Os dados reais ficam em `~/erp-desktop/data`, não nessa pasta temporária.

---

## Convenções de Código

### Controllers JavaFX

- Controllers devem ser beans Spring com `@Component`.
- Preferir `@RequiredArgsConstructor` para dependências.
- Campos de tela com `@FXML`.
- FXML deve usar `fx:controller` e o carregamento deve passar por `FXMLLoader` com `springContext::getBean`.
- Telas de módulo entram no `StackPane#conteudoPane`.
- Formulários longos podem substituir o conteúdo do `StackPane`; modais auxiliares usam `Stage`/`Dialog`.

Exemplo:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MeuController implements Initializable {
    private final MeuService meuService;

    @FXML private TextField txtCampo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // configurar tela
    }
}
```

### Entidades JPA

- Usar `@Builder`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` quando seguir o padrão existente.
- `criadoEm` com `@PrePersist`.
- `atualizadoEm` com `@UpdateTimestamp`.
- Monetários: `NUMERIC(15,2)` no banco e `BigDecimal` no Java.
- Preço unitário/custo unitário/quantidade: `NUMERIC(15,4)` quando há necessidade de 4 casas.
- Evitar acessar entidades lazy diretamente na UI fora de transação; criar queries com `JOIN FETCH` quando necessário.

### Repositories

- Interface extendendo `JpaRepository<Entidade, Integer>`.
- Métodos derivados quando bastarem.
- JPQL com `@Query` para telas que precisam carregar relacionamentos ou agregados.
- Para tabelas JavaFX, prefira query que já traga relacionamentos usados nas colunas.

### Services

- `@Service` + `@RequiredArgsConstructor`.
- Escrita: `@Transactional`.
- Leitura: `@Transactional(readOnly = true)`.
- Regras de negócio ficam no service, não no controller.
- Controllers devem montar tela, validar UX básica e chamar service.

### FXML e CSS

- Preferir classes do `global.css`.
- JavaFX não suporta variáveis CSS (`--var`, `var()`, `:root`).
- Dark mode usa `.theme-light` e `.theme-dark` aplicadas pelo `StageManager`.
- Evitar refatoração ampla de CSS/FXML sem necessidade; algumas telas antigas ainda têm estilos inline pontuais.

Classes úteis:

- Botões: `.btn-primary`, `.btn-secondary`, `.btn-outline`, `.btn-danger`, `.btn-topbar`, `.btn-login`
- Badges: `.badge-success`, `.badge-danger`, `.badge-warning`, `.badge-info`, `.badge-neutral`
- Cards: `.card`, `.metric-card`, `.dash-card`, `.dash-section-card`
- Formulários: `.form-label`, `.form-hint`, `.form-error`
- Dashboard: `.dash-val-green`, `.dash-val-blue`, `.dash-val-red`, `.dash-top-row`, `.dash-empty-msg`

---

## Banco de Dados

PostgreSQL embarcado sobe automaticamente com a aplicação.

Dados persistidos em:

```text
~/erp-desktop/data
```

Para recriar banco do zero:

```bash
rm -rf ~/erp-desktop/data
./mvnw javafx:run
```

Migrations atuais:

- `V1__schema_fase1.sql`
- `V2__unidades_medida_iniciais.sql`
- `V3__unidades_adicionais.sql`
- `V4__add_tema_configuracao.sql`
- `V5__mock_dados_cadastrais.sql`
- `V6__mock_contas_receber.sql`

Nunca alterar migration já executada. Criar sempre nova `V6__descricao.sql`, `V7__descricao.sql`, etc.

Usuário padrão:

- Login: `admin`
- Senha: `admin123`

---

## Testes e Verificações

Comandos mais usados:

```bash
# Compilar sem testes
./mvnw -q -DskipTests compile

# Rodar testes unitários
./mvnw test -q

# Rodar teste específico
./mvnw -q -Dtest=DashboardServiceTest test

# Rodar smoke UI headless
./mvnw -q -P ui-tests -Dtest=RegressaoGeralUITest#smoke_dashboard_admin_carrega test

# Verificar whitespace/diff
git diff --check
```

Testes de UI usam TestFX/Monocle e podem emitir logs longos do PostgreSQL embarcado. O importante é o exit code do Maven.

---

## Comandos Úteis

```bash
# Rodar em desenvolvimento
./mvnw javafx:run

# Gerar hash bcrypt para senha
./mvnw compile exec:java -Dexec.mainClass="com.erp.util.GerarHash"

# Recriar banco do zero
rm -rf ~/erp-desktop/data && ./mvnw javafx:run

# Limpar cache de extração do Postgres embarcado no macOS
rm -rf /var/folders/vb/s6_m_53s1315y206glb3xdwc0000gn/T/embedded-pg
```

---

## Próximas Prioridades Prováveis

A Fase 1 já tem o caminho comercial principal até vendas. Próximas prioridades de negócio sugeridas:

1. Evoluir Caixa: relatório de sessões fechadas, reabertura controlada, conferência por forma de pagamento e filtros históricos.
2. Evoluir Contas a Pagar: filtros por período, edição controlada de vencimento e baixa em lote.
3. Evoluir Contas a Receber: baixa parcial, filtros por período, crédito do cliente ao receber (`incrementar credito_disponivel`).
4. Relatórios gerenciais: vendas por período, produtos vendidos, compras, estoque, caixa e financeiro.
5. Melhorias no dashboard: top clientes do mês e contas a receber/pagar hoje.

---

## Problemas Conhecidos

| Problema | Causa | Solução |
|---|---|---|
| `FileAlreadyExistsException` em `/T/embedded-pg/.../fix-CVE-2024-4317.sql` | Cache temporário da extração dos binários do Postgres embarcado | Fechar app e remover pasta `embedded-pg` temporária |
| CSS error por `var()` ou `:root` | JavaFX CSS não suporta variáveis CSS web | Usar somente propriedades `-fx-*` |
| `LazyInitializationException` em tabelas JavaFX | Coluna acessando entidade lazy fora da sessão Hibernate | Repositório com `JOIN FETCH` ou DTO pronto para tela |
| PDF não abre automaticamente | `Desktop.open` pode não ser suportado no ambiente | Usar fallback nativo ou informar caminho do arquivo salvo |
| Valor monetário calculado errado após digitar `24.90` | Parser antigo removia pontos como milhar | Usar sempre `MoneyUtils.parse` |
