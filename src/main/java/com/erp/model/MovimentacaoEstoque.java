package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    /** ENTRADA, SAIDA, AJUSTE */
    @Column(nullable = false, length = 20)
    private String tipo;

    /** AJUSTE_MANUAL, COMPRA, VENDA, ORCAMENTO, etc. */
    @Column(nullable = false, length = 30)
    private String origem;

    @Column(name = "origem_id")
    private Integer origemId;

    @Column(precision = 15, scale = 4, nullable = false)
    private BigDecimal quantidade;

    @Column(name = "custo_unitario", precision = 15, scale = 4)
    private BigDecimal custoUnitario;

    @Column(name = "saldo_anterior", precision = 15, scale = 4, nullable = false)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_posterior", precision = 15, scale = 4, nullable = false)
    private BigDecimal saldoPosterior;

    @Column(length = 200)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
    }
}
