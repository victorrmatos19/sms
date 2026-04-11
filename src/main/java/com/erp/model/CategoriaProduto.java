package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private GrupoProduto grupo;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(length = 200)
    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Override
    public String toString() {
        return nome;
    }
}
