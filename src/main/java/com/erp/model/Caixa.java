package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caixa", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "nome"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
