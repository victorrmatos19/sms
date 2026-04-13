package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venda_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(precision = 15, scale = 4, nullable = false)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", precision = 15, scale = 4, nullable = false)
    private BigDecimal precoUnitario;

    @Column(name = "custo_unitario", precision = 15, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;
}
