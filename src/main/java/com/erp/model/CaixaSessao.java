package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caixa_sessao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaixaSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caixa_id", nullable = false)
    private Caixa caixa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ABERTO";

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Column(name = "saldo_inicial", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_final_informado", precision = 15, scale = 2)
    private BigDecimal saldoFinalInformado;

    @Column(name = "saldo_final_calculado", precision = 15, scale = 2)
    private BigDecimal saldoFinalCalculado;

    @Column(precision = 15, scale = 2)
    private BigDecimal diferenca;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @PrePersist
    protected void onCreate() {
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now();
        }
        if (status == null) {
            status = "ABERTO";
        }
        if (saldoInicial == null) {
            saldoInicial = BigDecimal.ZERO;
        }
    }
}
