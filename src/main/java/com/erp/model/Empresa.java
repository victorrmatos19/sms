package com.erp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    @Size(max = 150)
    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    @NotBlank
    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(name = "inscricao_estadual", length = 30)
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = 30)
    private String inscricaoMunicipal;

    @NotBlank
    @Column(name = "regime_tributario", nullable = false, length = 30)
    private String regimeTributario; // SIMPLES_NACIONAL, LUCRO_PRESUMIDO, LUCRO_REAL

    // Endereço
    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 80)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 9)
    private String cep;

    // Contato
    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(length = 120)
    private String site;

    // Logotipo armazenado como blob
    @Lob
    @Column(columnDefinition = "BYTEA")
    private byte[] logotipo;

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
