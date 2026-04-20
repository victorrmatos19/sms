# CLAUDE.md — SMS Desktop (Simple Manage System)

Arquivo de contexto para agentes de código trabalharem neste projeto.

---

## Visão Geral

O projeto é um ERP desktop offline-first para pequenas empresas brasileiras.
O produto usa PostgreSQL embarcado local, UI JavaFX e Spring Boot sem servidor web.

**Nome do produto:** SMS — Simple Manage System
**Tipo:** aplicativo desktop local com distribuição planejada por assinatura
**Estado atual:** Fase 1 concluída e gaps residuais pré-Fase 2 resolvidos: núcleo comercial, financeiro, configurações, usuários, múltiplos perfis, relatórios básicos e refinamentos finais implementados

O foco de negócio atual é evoluir do ERP operacional para a Fase 2 fiscal: emissão fiscal básica, documentos fiscais e impressão operacional, mantendo o núcleo cadastral/estoque/compras/orçamentos/vendas/financeiro já utilizável em fluxo ponta a ponta.

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
├── css/          # global.css com design system Preto & Verde
├── db/migration/ # Flyway migrations V1..V9
├── fxml/         # Telas JavaFX
├── images/       # Logo e ícones
└── reports/      # Templates JasperReports

scripts/
├── build-installer.ps1 # Build Windows EXE + update.json
└── build-dmg.sh        # Build macOS DMG assinado/notarizado + update-mac.json
```

Novos arquivos adicionados no módulo Contas a Receber:

- `src/main/java/com/erp/repository/ContaReceberRepository.java` (reescrito com JOIN FETCH e queries de resumo)
- `src/main/java/com/erp/model/dto/financeiro/ContasReceberResumoDTO.java`
- `src/main/java/com/erp/service/ContaReceberService.java`
- `src/main/java/com/erp/controller/ContasReceberController.java`
- `src/main/resources/fxml/contas-receber.fxml`
- `src/main/resources/db/migration/V6__mock_contas_receber.sql`

Arquivos expandidos no módulo Contas a Pagar:

- `src/main/java/com/erp/repository/ContaPagarRepository.java` (filtros por período combinados com status)
- `src/main/java/com/erp/service/ContaPagarService.java` (edição de vencimento, baixa em lote e despesa avulsa)
- `src/main/java/com/erp/controller/ContasPagarController.java` (filtros por período, seleção múltipla e dialogs novos)
- `src/main/resources/fxml/contas-pagar.fxml` (novos filtros e botões operacionais)

Novos arquivos adicionados no módulo Configurações da Empresa:

- `src/main/java/com/erp/controller/ConfiguracoesController.java`
- `src/main/resources/fxml/configuracoes.fxml`

Arquivos expandidos para Configurações:

- `src/main/java/com/erp/service/ConfiguracaoService.java` (empresa, defaults de configuração e persistência de parâmetros)
- `src/main/java/com/erp/service/BackupService.java` (backup diário local e restauração assistida pré-startup)
- `src/main/java/com/erp/controller/MainController.java` (carregamento da tela e restrição ADMINISTRADOR)
- `src/main/java/com/erp/service/OrcamentoPdfService.java` (dados reais da empresa e logotipo configurável no PDF)
- `src/main/resources/reports/orcamento.jrxml` (parâmetros de endereço e logotipo)

Novos arquivos adicionados no módulo Usuários:

- `src/main/java/com/erp/repository/PerfilAcessoRepository.java`
- `src/main/java/com/erp/service/UsuarioService.java`
- `src/main/java/com/erp/controller/UsuariosController.java`
- `src/main/resources/fxml/usuarios.fxml`
- `src/main/resources/db/migration/V7__multiplos_perfis_usuario.sql`

Novos arquivos adicionados no módulo Relatórios Básicos:

- `src/main/java/com/erp/service/RelatorioService.java`
- `src/main/java/com/erp/service/RelatorioPdfService.java`
- `src/main/java/com/erp/controller/RelatoriosController.java`
- `src/main/java/com/erp/model/dto/relatorio/RelatorioVendasTotaisDTO.java`
- `src/main/java/com/erp/model/dto/relatorio/RelatorioEstoqueTotaisDTO.java`
- `src/main/java/com/erp/model/dto/relatorio/RelatorioFinanceiroTotaisDTO.java`
- `src/main/java/com/erp/model/dto/relatorio/RelatorioCaixaTotaisDTO.java`
- `src/main/resources/fxml/relatorios.fxml`
- `src/main/resources/reports/relatorio-vendas.jrxml`
- `src/main/resources/reports/relatorio-estoque.jrxml`
- `src/main/resources/reports/relatorio-financeiro.jrxml`
- `src/main/resources/reports/relatorio-caixa.jrxml`

Migrations recentes de limpeza pré-Fase 2:

- `src/main/resources/db/migration/V8__remove_perfil_id_legado.sql`
- `src/main/resources/db/migration/V9__remove_categoria_produto.sql`

Arquivos de distribuição:

- `scripts/build-installer.ps1` (Windows EXE via `jpackage`)
- `scripts/build-dmg.sh` (macOS DMG via perfil Maven `mac-dmg`, assinatura e notarização)
- `src/main/resources/images/icon.ico` (ícone Windows)
- `src/main/resources/images/icon.icns` (ícone macOS)

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
- Financeiro: `caixa.fxml`, `contas-pagar.fxml`, `contas-receber.fxml`
- Sistema: `usuarios.fxml`, `configuracoes.fxml`, `relatorios.fxml`

---

## Estado Funcional Atual

### Autenticação e Shell

- Login com `AuthService`.
- Usuários podem acumular múltiplos perfis; o perfil principal, por hierarquia, direciona o dashboard inicial.
- `AuthService` expõe `temPerfil`, `temQualquerPerfil` e `getPerfilPrincipal`.
- `MainController` carrega módulos dentro do `StackPane#conteudoPane`.
- Sidebar exibe dinamicamente apenas os módulos permitidos para os perfis do usuário logado.
- Topbar sem busca global; cada módulo mantém sua própria busca local. Busca global fica para versão futura.
- `StageManager` centraliza tema, CSS global e dark mode.

### Módulo de Usuários

- Tela disponível no menu Sistema > Usuários apenas para perfil `ADMINISTRADOR`.
- `MainController` oculta o botão para perfis não administradores e também bloqueia acesso direto com Alert.
- `UsuarioService` centraliza criação, edição, alteração de senha e ativação/desativação lógica de usuários.
- Criação valida login (`[a-zA-Z0-9_]`, mínimo 3 caracteres), senha mínima de 6 caracteres, duplicidade por empresa e criptografa senha com `BCryptPasswordEncoder`.
- Listagem e busca usam query com `JOIN FETCH u.perfis` para evitar `LazyInitializationException` na coluna Perfil.
- `UsuariosController` implementa listagem, busca por nome/login, formulário de novo/editar em `Dialog`, alteração de senha e toggle ativo/inativo.
- Formulário de usuário usa CheckBoxes para múltiplos perfis, exige pelo menos um perfil marcado e impede conceder `ADMINISTRADOR` quando o usuário logado não é administrador.
- Coluna Perfil exibe todos os perfis do usuário separados por vírgula e ordenados por hierarquia.
- Login é imutável após criação: edição exibe login como texto somente leitura.
- Não há exclusão física de usuário; o fluxo usa apenas `ativo = false`.
- Proteção de segurança impede desativar o próprio usuário logado e impede desativar o único administrador ativo da empresa.

### Cadastros

- Produtos com grupo, unidade, preços, estoque mínimo/máximo, lote/validade e status ativo.
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
- Reimpressão/PDF fica disponível para qualquer status selecionado (`ABERTO`, `CONVERTIDO`, `EXPIRADO`, `CANCELADO`); editar, converter e cancelar continuam restritos a `ABERTO`.
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
- **Cancelamento de venda:** vendas `FINALIZADA` podem ser canceladas via botão na listagem com confirmação.
  - Estorna estoque de cada item (movimentação `ENTRADA` origem `CANCELAMENTO_VENDA`).
  - Cancela todas as parcelas `ABERTA` em `conta_receber`; parcelas `RECEBIDA` são preservadas (log.warn).
  - Registra estorno no caixa (`SAIDA` origem `CANCELAMENTO_VENDA`) se a sessão ainda estiver aberta.
  - Restaura `credito_disponivel` do cliente para vendas a prazo/crediário.
  - Toda a operação é atômica (`@Transactional`) — qualquer falha faz rollback completo.

### Caixa

- Primeira versão operacional do módulo financeiro.
- Abertura de caixa com saldo inicial e operador logado.
- Bloqueio por caixa: cada caixa cadastrado só pode ter uma sessão `ABERTO` por vez; operadores diferentes podem abrir caixas distintos simultaneamente.
- Sessão buscada pelo usuário logado (não pelo primeiro caixa aberto da empresa): cada operador vê e opera apenas seu próprio caixa.
- Movimentações manuais de suprimento e sangria.
- Fechamento com saldo final informado, saldo calculado e diferença.
- Vendas à vista registram entrada no caixa quando existe caixa aberto.
- Vendas a prazo/crediário não entram no caixa no momento da venda.
- Tela exibe saldo atual, entradas, saídas, operador, data de abertura e histórico do caixa atual.

### Contas a Pagar

- Tela operacional para títulos gerados pela confirmação de compras e despesas avulsas.
- Listagem com fornecedor, descrição, parcela, vencimento, valor, valor pago, forma e status.
- Filtros por todas, abertas, vencidas, vencendo hoje, pagas e canceladas.
- Filtros por período de vencimento ou emissão combinam com status e busca textual.
- Busca por fornecedor, descrição ou número da compra.
- Métricas de abertas, vencidas, vencendo hoje e pagas no mês.
- Baixa com valor pago, juros, multa, desconto, data de pagamento, forma e observações.
- Baixa em lote permite selecionar múltiplas contas abertas e baixa cada título de forma independente, sem abortar o lote por erro individual.
- Edição controlada de vencimento permitida apenas para contas `ABERTA`.
- Cadastro de despesa avulsa cria conta a pagar sem vínculo com compra (`compra_id` nulo), com fornecedor opcional.
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
- Baixa de parcela incrementa `credito_disponivel` do cliente pelo valor recebido (`ClienteService.ajustarCreditoDisponivel`).
- Cancelamento de conta aberta restaura `credito_disponivel` do cliente pelo valor original da parcela.

### Configurações da Empresa

- Tela disponível no menu Sistema > Configurações apenas para perfil `ADMINISTRADOR`.
- `MainController` oculta o botão para perfis não administradores e também bloqueia acesso direto com Alert.
- Aba Dados da Empresa permite editar razão social, nome fantasia, CNPJ, inscrições, regime tributário, endereço, contato e logotipo.
- Busca de CEP reutiliza `ViaCepService`, seguindo o padrão dos formulários de cliente/fornecedor.
- Logotipo é selecionado via `FileChooser`, preview em `ImageView` e persistido como `BYTEA` em `Empresa.logotipo`.
- Aba Parâmetros do Sistema edita `Configuracao`: lote/validade, alerta de estoque mínimo, venda com estoque zero, validade do orçamento, juros, multa, impressora padrão e exibição de logotipo.
- `ConfiguracaoService` centraliza busca/salvamento de `Empresa` e `Configuracao`, mantendo criação automática de defaults quando a configuração da empresa ainda não existe.
- PDFs de orçamento usam dados reais da tabela `empresa` e respeitam `exibir_logotipo_impressao` para mostrar ou ocultar o logotipo.
- Aba Backup e Restauração lista backups locais, mostra status da última restauração, abre a pasta de backups e permite agendar restauração com confirmação forte (`RESTAURAR`).

### Backup e Restauração

- `BackupService` executa manutenção antes do Spring iniciar o PostgreSQL embarcado, chamado em `MainApp.init`.
- Backup automático local é físico/frio: compacta `~/erp-desktop/data` uma vez por dia ao abrir o app, antes do banco subir.
- Backups ficam em `~/erp-desktop/backups` com nome `sms-backup-YYYYMMDD-HHmmss.zip` e sidecar `.metadata.json` contendo tipo, data, versão, tamanho e SHA-256.
- Retenção automática remove backups com mais de 30 dias, preservando backups recentes e o backup criado no dia.
- Restauração é assistida: a UI cria `~/erp-desktop/restore-request.json`, fecha o app, e a próxima abertura valida SHA, cria backup `PRE_RESTORE`, substitui a pasta `data` e registra `restore-status.json`.
- Após extrair backup físico, o restore aplica permissões POSIX seguras para PostgreSQL: diretórios `700` e arquivos `600`. Sem isso o Postgres pode recusar a pasta restaurada e falhar com timeout de startup.
- Se a restauração falhar, a base atual é preservada, o erro fica registrado em `restore-status.json` e o pedido é arquivado como `restore-request.failed-*.json`.

### Dashboards

- Dashboard admin exibe cards de vendas hoje, contas a pagar hoje, caixa atual, ticket médio, gráfico de vendas dos últimos 7 dias, alertas e top produtos vendidos no mês.
- O card `VENDAS HOJE` está implementado: mostra valor/quantidade de vendas finalizadas no dia e abre modal com horário, número, cliente, vendedor, forma de pagamento e valor.
- Dashboard de vendas exibe orçamentos, vendas e compras do mês.
- Dashboard financeiro exibe contas a pagar abertas/vencidas e compras do mês.
- Dashboard estoque exibe produtos, abaixo do mínimo, entradas e saídas do mês.

### Relatórios

- Orçamento em PDF via JasperReports.
- Relatórios básicos implementados em tela única `relatorios.fxml` com `TabPane`.
- Abas disponíveis: Vendas por Período, Posição de Estoque, Financeiro e Resumo de Caixa.
- Cada aba possui filtros próprios, cards de totais, TableView e botão de exportação PDF.
- `RelatorioService` centraliza queries de relatório com `JOIN FETCH` para evitar lazy loading em TableViews.
- `RelatorioPdfService` gera PDFs via JasperReports, salva em temporário e abre externamente usando o mesmo padrão/fallback de `OrcamentoPdfService`.
- Templates Jasper atuais: `orcamento.jrxml`, `relatorio-vendas.jrxml`, `relatorio-estoque.jrxml`, `relatorio-financeiro.jrxml`, `relatorio-caixa.jrxml`.
- Dados de empresa/logotipo dos PDFs vêm das Configurações da Empresa.

### Distribuição e Atualizações

- Windows mantém o fluxo existente com `jpackage` gerando instalador `EXE` e `scripts/build-installer.ps1` gerando `target/dist/update.json`.
- macOS possui perfil Maven `mac-dmg`, que usa `jpackage` com `type=DMG`, ícone `icon.icns`, bundle identifier `br.com.sms.desktop`, assinatura `Developer ID Application` e `macPackageName=SMS`.
- `scripts/build-dmg.sh` valida macOS, JDK 21 com `jpackage`, certificado de assinatura, gera o JAR, gera o DMG, envia para notarização com `notarytool`, aplica `stapler`, valida com Gatekeeper e gera `target/dist/update-mac.json`.
- Para builds locais sem notarização, o script aceita `--no-notarize`, mas ainda exige assinatura Developer ID porque o perfil `mac-dmg` é voltado para distribuição.
- `UpdateService` reaproveita download e validação SHA-256 para Windows e macOS; no Windows agenda o `.exe` via PowerShell, no macOS agenda abertura do `.dmg` via comando nativo `open` após o app encerrar.

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
- O gráfico `VENDAS — ÚLTIMOS 7 DIAS` usa as queries simples de soma/contagem por dia (`countBy...DataVendaBetween` e `sumValorTotalBy...DataVendaBetween`), evitando depender de listas com `JOIN FETCH` para montar a série diária.
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

### Gaps de Contas a Pagar

- `ContaPagarRepository` passou a ter queries de período por vencimento e emissão, combinadas com status.
- `ContaPagarService` ganhou filtro por período, edição controlada de vencimento, baixa em lote e cadastro de despesa avulsa.
- `ContasPagarController` agora usa seleção múltipla na tabela para baixa em lote, mantendo ações individuais habilitadas apenas com uma linha selecionada.
- `contas-pagar.fxml` recebeu filtros de período, botão de nova despesa, edição de vencimento e baixa em lote.
- Campos monetários novos usam `MoneyUtils.parse`/`MoneyUtils.formatCurrency`; DatePickers novos usam conversor `dd/MM/yyyy`.
- Não foi necessária nova migration: `conta_pagar.compra_id` já permite `null`.

### Módulo Contas a Receber

- `ContaReceberService` centraliza listagem, resumo, baixa, cancelamento e cadastro avulso.
- `ContasReceberController` e `contas-receber.fxml` implementam a tela financeira de contas a receber.
- `ContaReceberRepository` reescrito com `JOIN FETCH` para cliente, venda e usuário, além de queries de contagem e soma por status/período.
- `CaixaService` ganhou o método `registrarRecebimentoContaReceberSeCaixaAberto` para integrar recebimentos ao caixa aberto.
- `MainController#abrirContasReceber` agora carrega a tela real do módulo.
- `V6__mock_contas_receber.sql` inseriu 14 contas mock: abertas, vencidas, vencendo hoje, recebidas e cancelada.
- A tabela `conta_receber` já existia na migration V1 e é alimentada por vendas a prazo/crediário via `VendaService`.

### Módulo Configurações da Empresa

- `ConfiguracaoService` foi expandido para centralizar empresa e parâmetros operacionais sem criar service novo.
- `ConfiguracoesController` e `configuracoes.fxml` implementam edição de dados reais da empresa, logotipo e parâmetros do sistema.
- `MainController#abrirConfiguracoes` agora carrega a tela real, oculta o item para perfis não administradores e mantém bloqueio com Alert.
- `OrcamentoPdfService` passou a buscar a empresa pelo `ConfiguracaoService`, montar endereço real, passar logotipo como `java.awt.Image` e respeitar `exibir_logotipo_impressao`.
- `reports/orcamento.jrxml` recebeu parâmetros de endereço e logotipo no cabeçalho do relatório.

### Módulo de Usuários

- `PerfilAcessoRepository` expõe busca por nome e listagem ordenada dos perfis.
- `UsuarioRepository` ganhou consultas com `JOIN FETCH u.perfis`, busca por termo e verificação de login duplicado por empresa.
- `UsuarioService` cria usuários com BCrypt, edita dados sem alterar login, altera senha e protege o último administrador ativo.
- `UsuariosController` e `usuarios.fxml` implementam gestão de usuários em tela de módulo, com dialogs para novo/editar, múltiplos perfis por CheckBox e alteração de senha.
- `MainController#abrirUsuarios` carrega a tela real somente para administradores.

### Gaps pré-Fase 2

Três correções funcionais aplicadas após conclusão da Fase 1 comercial:

**GAP 1 — Cancelamento de Venda**
- `VendaService.cancelarVenda()` implementado com transação única que estorna estoque, cancela parcelas abertas, registra estorno no caixa e restaura crédito do cliente.
- `VendaController` e `vendas.fxml` ganharam botão "Cancelar Venda" habilitado apenas para status `FINALIZADA`, com Alert de confirmação detalhando o que será revertido.
- `ContaReceberRepository.findByVendaIdOrderByNumeroParcela` adicionado para buscar parcelas de uma venda.
- `CaixaMovimentacaoRepository.findByOrigemAndOrigemId` adicionado para buscar movimentações de caixa de uma venda.

**GAP 2 — Crédito Disponível do Cliente**
- `ClienteService.ajustarCreditoDisponivel(clienteId, delta)` centraliza o ajuste: delta positivo incrementa, negativo reduz. Permite crédito negativo com log.warn, sem bloquear.
- `VendaService.gerarContasAReceber()` agora reduz `credito_disponivel` ao gerar parcelas (`-valorTotal`).
- `ContaReceberService.baixar()` agora incrementa `credito_disponivel` ao receber (`+valorPago`).
- `ContaReceberService.cancelar()` agora restaura `credito_disponivel` ao cancelar (`+valor`).
- `VendaService.cancelarVenda()` restaura `credito_disponivel` para vendas a prazo/crediário (`+valorTotal`).

**GAP 3 — Múltiplos Caixas Simultâneos**
- `CaixaSessaoRepository` ganhou `findByUsuarioIdAndStatus` e `existsByCaixaIdAndStatus`.
- `CaixaService.buscarSessaoAberta()` passa a buscar pelo usuário logado, não por empresa.
- `CaixaService.abrirCaixa()` valida unicidade por caixa (`existsByCaixaIdAndStatus`) em vez de empresa.
- `registrarVendaSeCaixaAberto()` loga quando o operador não tem caixa aberto e retorna sem registrar no caixa de outro operador.

### Controle de Acesso e Múltiplos Perfis

- `Usuario` deixou de depender de um único `ManyToOne perfil` como regra funcional e passou a usar `ManyToMany perfis`.
- `V7__multiplos_perfis_usuario.sql` criou a tabela `usuario_perfil`, migrou os dados legados de `usuario.perfil_id` e tornou a coluna antiga nullable.
- `V8__remove_perfil_id_legado.sql` removeu a coluna antiga `usuario.perfil_id`; `usuario_perfil` agora é a única fonte de verdade.
- `Usuario#getPerfilPrincipal()` escolhe o perfil de maior hierarquia para seleção do dashboard: Administrador, Gerente, Vendas, Financeiro e Estoque.
- `AuthService#temQualquerPerfil()` centraliza checagens de acesso quando um módulo aceita mais de um perfil.
- Sidebar do `MainController` agora oculta botões e seções sem acesso para o usuário logado.
- Formulário de usuários usa CheckBoxes de perfil, permitindo acumular departamentos no mesmo login.

### Relatórios Básicos

- `RelatorioService` concentra os relatórios de vendas, estoque, financeiro e caixa, retornando os mesmos dados usados em tela e no PDF.
- `RelatoriosController` implementa a tela hub com abas lazy, filtros por relatório, cards de totais, tabelas e botões de PDF.
- Vendas por período filtra por data, cliente, vendedor e status, com totais bruto/desconto/líquido.
- Posição de estoque mostra situação atual sem período, com filtros por grupo, termo, situação e inativos.
- Financeiro combina contas a pagar e receber por vencimento, status e pessoa relacionada.
- Caixa lista sessões por período, caixa, operador e status, exibindo movimentações da sessão selecionada.
- `RelatorioPdfService` compila os quatro templates JasperReports novos e usa cabeçalho padrão com empresa/logotipo.
- `MainController#abrirRelatorios` agora carrega a tela real do módulo.
- Templates de relatórios foram ajustados para a ordem de bandas compatível com JasperReports 6.21.4 (`pageFooter` antes de `summary`).

### Refinamentos Pré-Fase 2

- Busca global decorativa removida do topbar; buscas permanecem locais em cada módulo.
- `V8__remove_perfil_id_legado.sql` remove a coluna `usuario.perfil_id`; `usuario_perfil` é a única fonte de verdade para perfis.
- `Usuario` permanece apenas com `Set<PerfilAcesso> perfis`, `temPerfil`, `getPerfilPrincipal` e `getPerfisOrdenados`.
- `V9__remove_categoria_produto.sql` remove `produto.categoria_id` e a tabela `categoria_produto`.
- Cadastro de produto não exibe nem persiste categoria; classificação usa apenas Grupo.
- `global.css` foi alinhado ao design system Preto & Verde, com dark mode padrão, light mode alternativo e remoção de sobras Navy/Dourado.

### Distribuição macOS

- `pom.xml` ganhou o perfil `mac-dmg`, mantendo o fluxo Windows padrão intacto.
- `src/main/resources/images/icon.icns` foi criado para o pacote macOS.
- `scripts/build-dmg.sh` automatiza build do DMG assinado/notarizado, staple, validação Gatekeeper e geração do manifest `update-mac.json`.
- `UpdateService` agora escolhe extensão padrão por sistema operacional (`.exe` no Windows, `.dmg` no macOS) quando a URL não traz nome de arquivo e abre o DMG com `open` no fluxo de atualização.

### Backup Diário Local

- `BackupService` foi adicionado como serviço independente de banco para rodar antes do Spring/Flyway/Postgres.
- `MainApp.init` chama a manutenção pré-startup antes de `SpringApplication.run`.
- `configuracoes.fxml` ganhou a aba administrativa "Backup e Restauração".
- `ConfiguracoesController` lista backups, mostra validação SHA e agenda restauração para o próximo startup.
- `BackupServiceTest` cobre primeira instalação sem base, backup diário único, retenção de 30 dias e restauração assistida.

### PDF de Orçamento

- JasperReports está em versão 6.21.4.
- O template `orcamento.jrxml` foi mantido em formato compatível com Jasper 6.
- `OrcamentoPdfService` compila o `.jrxml`, preenche parâmetros, exporta bytes e tenta abrir o PDF.
- Dados da empresa vêm da tabela `empresa`, não de strings hardcoded ou apenas do seed inicial.
- O logotipo da empresa é exibido quando existir imagem cadastrada e a configuração `exibir_logotipo_impressao` estiver ativa.
- Para macOS, há fallback para comando nativo `open`.

### PDFs de Relatórios

- Relatórios básicos usam JasperReports 6.21.4 e `JRBeanCollectionDataSource`.
- PDFs são salvos no diretório temporário com prefixo `sms_relatorio-*` e abertos por `Desktop.open` ou comando nativo (`open`, `xdg-open`, `rundll32`).
- Botões de PDF ficam desabilitados quando a aba não possui dados para exportar.

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

## Decisões Funcionais Relevantes

- Múltiplos perfis por usuário: um usuário pode acumular departamentos; dashboard e badge principal são determinados pelo perfil de maior hierarquia.
- Perfis continuam departamentais e sem matriz granular de permissões por ação nesta fase.
- Usuários nunca são excluídos fisicamente; desativação lógica preserva histórico operacional.
- Dados reais da empresa são editáveis em Configurações e são a fonte para PDFs e parâmetros operacionais.
- Backup local usa cópia física fria da pasta do PostgreSQL embarcado; não usar cópia da pasta `data` enquanto o banco estiver rodando.
- Restauração sempre é aplicada no próximo startup, antes do Postgres subir, nunca durante a sessão atual.
- Categoria de produto removida do escopo; classificação de produtos usa apenas Grupos.
- Busca global removida do topbar; cada módulo tem busca própria. Busca global planejada para versão futura.
- Design system oficial é Preto & Verde: dark mode padrão com acento verde e light mode alternativo.
- Distribuição macOS usa bundle identifier estável `br.com.sms.desktop`; trocar apenas se houver decisão definitiva de domínio/empresa antes da primeira release pública.

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
- Campos binários em PostgreSQL `BYTEA`, como `Empresa.logotipo`, devem usar `byte[]` sem `@Lob`; com Hibernate/PostgreSQL, `@Lob` tende a persistir como `oid`.
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
- `global.css` deve manter as classes existentes e usar valores hardcoded `-fx-*`.
- Paleta atual: dark `#000000`/`#121212`/`#1a1a1a` com acento `#1db954`; light `#f0f0f0`/`#fafafa`/`#ffffff` com acento `#16a34a`.
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
- `V7__multiplos_perfis_usuario.sql`
- `V8__remove_perfil_id_legado.sql`
- `V9__remove_categoria_produto.sql`

Nunca alterar migration já executada. Criar sempre nova `V10__descricao.sql`, `V11__descricao.sql`, etc.

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

# Rodar testes de backup/restauração
./mvnw -q -Dtest=BackupServiceTest test

# Rodar smoke UI headless
./mvnw -q -P ui-tests -Dtest=RegressaoGeralUITest#smoke_dashboard_admin_carrega test

# Verificar whitespace/diff
git diff --check

# Validar sintaxe do script de DMG
bash -n scripts/build-dmg.sh
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

# Gerar instalador Windows + update.json
powershell -ExecutionPolicy Bypass -File scripts/build-installer.ps1 -SkipTests

# Gerar DMG macOS assinado/notarizado + update-mac.json
MAC_SIGNING_KEY_USER_NAME="Developer ID Application: Sua Empresa (TEAMID)" \
APPLE_ID="apple-id@empresa.com" \
APPLE_TEAM_ID="TEAMID" \
APPLE_APP_PASSWORD="app-specific-password" \
./scripts/build-dmg.sh --skip-tests

# Gerar DMG macOS assinado, sem notarizar, para validação local
MAC_SIGNING_KEY_USER_NAME="Developer ID Application: Sua Empresa (TEAMID)" \
./scripts/build-dmg.sh --skip-tests --no-notarize
```

---

## Próximas Prioridades Prováveis

A Fase 1 está concluída e os gaps residuais pré-Fase 2 foram resolvidos. Próximas prioridades de negócio sugeridas para a Fase 2:

1. Recibos/romaneios/impressões operacionais simples e impressão de tabela de preços.
2. Fiscal básico: NFC-e via API externa, como Focus NF-e ou WebMania.
3. NF-e para vendas e operações que exigirem documento fiscal.

---

## Problemas Conhecidos

| Problema | Causa | Solução |
|---|---|---|
| `FileAlreadyExistsException` em `/T/embedded-pg/.../fix-CVE-2024-4317.sql` | Cache temporário da extração dos binários do Postgres embarcado | Fechar app e remover pasta `embedded-pg` temporária |
| CSS error por `var()` ou `:root` | JavaFX CSS não suporta variáveis CSS web | Usar somente propriedades `-fx-*` |
| `LazyInitializationException` em tabelas JavaFX | Coluna acessando entidade lazy fora da sessão Hibernate | Repositório com `JOIN FETCH` ou DTO pronto para tela |
| PDF não abre automaticamente | `Desktop.open` pode não ser suportado no ambiente | Usar fallback nativo ou informar caminho do arquivo salvo |
| Valor monetário calculado errado após digitar `24.90` | Parser antigo removia pontos como milhar | Usar sempre `MoneyUtils.parse` |
