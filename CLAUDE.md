# CLAUDE.md — SMS Desktop (Simple Manage System)

Arquivo lido automaticamente pelo Claude Code ao iniciar sessões neste projeto.

---

## Visão Geral do Projeto

ERP desktop para Windows voltado a pequenas empresas brasileiras.
Desenvolvido por um único desenvolvedor com suporte do Claude Code.
Distribuição via assinatura mensal (SaaS local), offline-first.

**Nome do produto:** SMS — Simple Manage System
**Documento de contexto completo:** `ERP_-_Descrição` (na raiz do projeto)

---

## Stack Técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 LTS |
| Interface | JavaFX + AtlantaFX (PrimerLight como base) |
| Banco local | PostgreSQL embarcado (io.zonky) |
| ORM | Hibernate + Spring Data JPA |
| DI / Core | Spring Boot 3.2.5 (sem web) |
| Migrations | Flyway |
| Relatórios | JasperReports |
| Build | Maven + jpackage |
| Ícones | Ikonli (Material Design 2 + Font Awesome 5) |
| Componentes UI | ControlsFX |

---

## Estrutura de Pacotes

```
src/main/java/com/erp/
├── config/       # Spring Boot config (EmbeddedPostgresConfig, JpaConfig, SecurityConfig)
├── model/        # Entidades JPA
├── repository/   # Spring Data repositories
├── service/      # Regras de negócio
├── controller/   # Controllers JavaFX (um por tela)
├── view/         # Utilitários de tela (StageManager está na raiz do pacote)
└── util/         # Helpers (GerarHash, etc.)

src/main/resources/
├── fxml/         # Layouts das telas JavaFX
├── css/          # global.css — design system completo
├── images/       # logo-light.svg, logo-dark.svg, icon.svg, icon.png
├── reports/      # Templates JasperReports (.jrxml)
└── db/migration/ # Scripts Flyway — V1__schema_fase1.sql (único arquivo ativo)
```

---

## Design System

Tema: **Navy & Dourado** com suporte a dark mode.

Cores principais:
- Navy primário: `#1a2744`
- Dourado acento: `#c9a84c`
- Dark mode base: `#0d1420`

**Regras críticas do CSS JavaFX:**
- JavaFX NÃO suporta variáveis CSS (`--minha-var`, `var()`, `:root`) — usar apenas propriedades `-fx-*`
- Dark mode via classes `.theme-light` e `.theme-dark` aplicadas na raiz pelo `StageManager`
- O `StageManager` aplica o tema e carrega o CSS global em toda troca de tela

Classes CSS disponíveis em `global.css`:
- Botões: `.btn-primary`, `.btn-secondary`, `.btn-outline`, `.btn-danger`, `.btn-topbar`, `.btn-login`
- Badges: `.badge-success`, `.badge-danger`, `.badge-warning`, `.badge-info`, `.badge-neutral`
- Cards: `.card`, `.card-title`, `.metric-card`, `.metric-card-value`, `.metric-card-value-accent`
- Formulários: `.form-label`, `.form-hint`, `.form-error`
- Sidebar: `.sidebar-btn`, `.sidebar-btn-active`, `.sidebar-section`, `.sidebar-divider`
- Página: `.page-title`, `.page-subtitle`

---

## Convenções de Código

### Controllers JavaFX
- Sempre `@Component` (bean Spring, não instanciado pelo FXMLLoader)
- Sempre `@RequiredArgsConstructor` para injeção via construtor
- Injeção de telas via `StageManager.showScene()`
- Campos FXML anotados com `@FXML`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MeuController implements Initializable {
    private final MeuService meuService;
    private final StageManager stageManager;

    @FXML private TextField txtCampo;

    @Override
    public void initialize(URL url, ResourceBundle rb) { }
}
```

### Entidades JPA
- Sempre `@Builder`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `@PrePersist` para setar `criadoEm` e `atualizadoEm`
- `@UpdateTimestamp` em `atualizadoEm`
- Campos monetários: `NUMERIC(15,2)` no banco, `BigDecimal` no Java
- Campos de preço/quantidade: `NUMERIC(15,4)` para 4 casas decimais

### Repositories
- Sempre interface extendendo `JpaRepository<Entidade, Integer>`
- Usar `@Query` com JPQL apenas quando os métodos derivados não forem suficientes

### Services
- `@Service` + `@RequiredArgsConstructor`
- Transações: `@Transactional` apenas nos métodos que escrevem no banco
- Leitura: `@Transactional(readOnly = true)`

### FXML
- Um arquivo `.fxml` por tela/módulo
- Sempre declarar `fx:controller` apontando para o pacote correto
- Usar `styleClass` com as classes do `global.css`, nunca `style` inline

---

## Banco de Dados

**PostgreSQL embarcado** sobe automaticamente com a aplicação.
Dados persistidos em: `~/erp-desktop/data/`

**Para recriar o banco do zero:**
```bash
rm -rf ~/erp-desktop/data
mvn javafx:run
```

**Migrations Flyway** em `src/main/resources/db/migration/`:
- `V1__schema_fase1.sql` — schema completo da Fase 1 + dados iniciais
- Novas migrations sempre no formato `V2__descricao.sql`, `V3__descricao.sql`, etc.
- **Nunca alterar** um arquivo de migration já executado — sempre criar um novo

**Usuário padrão:**
- Login: `admin`
- Senha: `admin123`

---

## Plano de Fases

### Fase 1 — Core Comercial (EM DESENVOLVIMENTO)
Cadastros, Estoque, Compras, Orçamentos, Vendas, Caixa, Contas a Pagar/Receber, Relatórios básicos

### Fase 2 — Fiscal Básico
NFC-e, NF-e via API Focus NFe ou WebMania, código de barras, tabela de preços, recibos

### Fase 3 — Financeiro Avançado
Fluxo de caixa, Boletos (Efi Bank), PIX QR Code, e-mail de cobrança, comissões

### Fase 4 — Fiscal Avançado e PDV
CF-e SAT, balança integrada, XML, TEF

### Fase 5 — Gestão e Relatórios
Relatórios analíticos, atendimento, recados, funcionários completo

---

## Próximos Módulos a Implementar (Fase 1)

Ordem sugerida:
1. **Cadastro de Produtos** — listagem, formulário, busca por código de barras
2. **Cadastro de Clientes** — listagem, formulário PF/PJ, busca
3. **Cadastro de Fornecedores** — listagem, formulário, dados bancários
4. **Controle de Estoque** — movimentações, alertas de mínimo
5. **Orçamentos** — emissão, itens, conversão em venda
6. **Vendas** — PDV simples, múltiplas formas de pagamento
7. **Caixa** — abertura, fechamento, sangria, suprimento
8. **Contas a Pagar/Receber** — lançamentos, baixas, alertas

---

## Padrão de Tela para Módulos

Cada módulo segue o padrão:
1. **Listagem** com TableView, busca e botão "Novo"
2. **Formulário** em dialog ou painel lateral para CRUD
3. **Métricas** com cards de resumo no topo quando aplicável

O conteúdo é carregado no `StackPane#conteudoPane` do `main.fxml`.
Usar `FXMLLoader` dentro do `MainController` para carregar os sub-módulos.

---

## Comandos Úteis

```bash
# Rodar em desenvolvimento
mvn javafx:run

# Gerar hash bcrypt para senha
mvn compile exec:java -Dexec.mainClass="com.erp.util.GerarHash"

# Limpar cache Maven se houver JARs corrompidos
rm -rf ~/.m2/repository/xml-apis
mvn javafx:run

# Recriar banco do zero
rm -rf ~/erp-desktop/data && mvn javafx:run
```

---

## Problemas Conhecidos e Soluções

| Problema | Causa | Solução |
|---|---|---|
| CSS Error: Expected RBRACE | JavaFX não suporta variáveis CSS | Usar apenas `-fx-*`, sem `var()` ou `:root` |
| Missing postgres binaries (arm_64) | Falta binário para Mac Apple Silicon | Adicionar `embedded-postgres-binaries-darwin-arm64v8` no pom.xml |
| Logo SVG não renderiza no JavaFX | JavaFX tem suporte limitado a SVG | Converter para PNG com Batik ou carregar via WebView |
| BCryptPasswordEncoder not found | spring-security-crypto não declarado | Adicionar `spring-security-crypto` no pom.xml explicitamente |