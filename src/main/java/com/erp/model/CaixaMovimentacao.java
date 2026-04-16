package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caixa_movimentacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaixaMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private CaixaSessao sessao;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal valor;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento;

    @Column(length = 30)
    private String origem;

    @Column(name = "origem_id")
    private Integer origemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
