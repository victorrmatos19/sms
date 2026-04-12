package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "numero_documento", length = 60)
    private String numeroDocumento;

    /** RASCUNHO, CONFIRMADA, CANCELADA */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "RASCUNHO";

    @Column(name = "data_emissao", nullable = false)
    @Builder.Default
    private LocalDate dataEmissao = LocalDate.now();

    @Column(name = "data_previsao")
    private LocalDate dataPrevisao;

    @Column(name = "data_recebimento")
    private LocalDate dataRecebimento;

    @Column(name = "valor_produtos", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorProdutos = BigDecimal.ZERO;

    @Column(name = "valor_frete", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @Column(name = "valor_desconto", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_outras", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorOutras = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /** A_VISTA, 30_DIAS, 60_DIAS, 90_DIAS, 30_60_DIAS, 30_60_90_DIAS, PERSONALIZADO */
    @Column(name = "condicao_pagamento", length = 30)
    @Builder.Default
    private String condicaoPagamento = "A_VISTA";

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompraItem> itens = new ArrayList<>();

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

    /** Campo transiente para uso no formulário (não persiste no banco) */
    @Transient
    private int numeroParcelas = 1;

    public void addItem(CompraItem item) {
        itens.add(item);
        item.setCompra(this);
    }

    public void removeItem(CompraItem item) {
        itens.remove(item);
        item.setCompra(null);
    }
}
