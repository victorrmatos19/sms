package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "usa_lote_validade", nullable = false)
    @Builder.Default
    private Boolean usaLoteValidade = false;

    @Column(name = "alerta_estoque_minimo", nullable = false)
    @Builder.Default
    private Boolean alertaEstoqueMinimo = true;

    @Column(name = "permite_venda_estoque_zero", nullable = false)
    @Builder.Default
    private Boolean permiteVendaEstoqueZero = false;

    @Column(name = "dias_validade_orcamento", nullable = false)
    @Builder.Default
    private Integer diasValidadeOrcamento = 30;

    @Column(name = "juros_mora_padrao", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal jurosMoraPadrao = BigDecimal.ZERO;

    @Column(name = "multa_padrao", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal multaPadrao = BigDecimal.ZERO;

    @Column(name = "impressora_padrao", length = 100)
    private String impressoraPadrao;

    @Column(name = "exibir_logotipo_impressao", nullable = false)
    @Builder.Default
    private Boolean exibirLogotipoImpressao = true;

    /** Tema da interface: "LIGHT" ou "DARK". */
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String tema = "DARK";

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
