package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidade_medida", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "sigla"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadeMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 10)
    private String sigla;

    @Column(length = 60)
    private String descricao;

    @Override
    public String toString() {
        return sigla + (descricao != null ? " — " + descricao : "");
    }
}
