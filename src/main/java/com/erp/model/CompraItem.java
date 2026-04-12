package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(name = "custo_unitario", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /** Recalcula valorTotal = (quantidade * custoUnitario) - desconto */
    public void recalcularTotal() {
        BigDecimal bruto = quantidade.multiply(custoUnitario);
        valorTotal = bruto.subtract(desconto).max(BigDecimal.ZERO);
    }
}
