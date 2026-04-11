package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produto", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "codigo_interno"}),
    @UniqueConstraint(columnNames = {"empresa_id", "codigo_barras"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private GrupoProduto grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaProduto categoria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unidade_id")
    private UnidadeMedida unidade;

    @Column(name = "codigo_interno", length = 30)
    private String codigoInterno;

    @Column(name = "codigo_barras", length = 60)
    private String codigoBarras;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "descricao_reduzida", length = 60)
    private String descricaoReduzida;

    @Column(name = "preco_custo", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal precoCusto = BigDecimal.ZERO;

    @Column(name = "margem_lucro", precision = 7, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal margemLucro = BigDecimal.ZERO;

    @Column(name = "preco_venda", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal precoVenda = BigDecimal.ZERO;

    @Column(name = "preco_minimo", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal precoMinimo = BigDecimal.ZERO;

    @Column(name = "estoque_atual", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal estoqueAtual = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(name = "estoque_maximo", precision = 15, scale = 4)
    private BigDecimal estoqueMaximo;

    @Column(length = 60)
    private String localizacao;

    @Column(length = 10)
    private String ncm;

    @Column(length = 10)
    private String cest;

    @Column(length = 1)
    private String origem;

    @Column(name = "usa_lote_validade", nullable = false)
    @Builder.Default
    private Boolean usaLoteValidade = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }
}
