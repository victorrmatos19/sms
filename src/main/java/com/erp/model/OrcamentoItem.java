package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamento_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(name = "preco_unitario", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    public void recalcularTotal() {
        if (quantidade != null && precoUnitario != null) {
            BigDecimal subtotal = quantidade.multiply(precoUnitario);
            BigDecimal desc = desconto != null ? desconto : BigDecimal.ZERO;
            valorTotal = subtotal.subtract(desc).max(BigDecimal.ZERO);
        }
    }
}
