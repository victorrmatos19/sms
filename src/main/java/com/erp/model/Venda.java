package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venda", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "numero"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Funcionario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    @Column(nullable = false, length = 20)
    private String numero;

    /** FINALIZADA, CANCELADA */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "FINALIZADA";

    @Column(name = "data_venda", nullable = false)
    @Builder.Default
    private LocalDateTime dataVenda = LocalDateTime.now();

    @Column(name = "valor_produtos", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorProdutos = BigDecimal.ZERO;

    @Column(name = "valor_desconto", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "percentual_comissao", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal percentualComissao = BigDecimal.ZERO;

    @Column(name = "valor_comissao", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorComissao = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VendaItem> itens = new ArrayList<>();

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VendaPagamento> pagamentos = new ArrayList<>();

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
