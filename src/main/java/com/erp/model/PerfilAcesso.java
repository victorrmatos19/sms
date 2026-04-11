package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfil_acesso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nome; // ADMINISTRADOR, GERENTE, VENDAS, FINANCEIRO, ESTOQUE

    @Column(length = 200)
    private String descricao;
}
