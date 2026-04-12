package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "funcionario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 14)
    private String cpf;

    @Column(length = 80)
    private String cargo;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /** Percentual de comissão — usado na Fase 3. */
    @Column(name = "percentual_comissao", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal percentualComissao = BigDecimal.ZERO;

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
