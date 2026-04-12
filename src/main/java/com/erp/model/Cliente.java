package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "cpf_cnpj"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** "PF" = Pessoa Física, "PJ" = Pessoa Jurídica */
    @Column(name = "tipo_pessoa", nullable = false, length = 2)
    private String tipoPessoa;

    /** Nome completo (PF) ou nome fantasia (PJ) */
    @Column(nullable = false, length = 150)
    private String nome;

    /** Razão social — preenchido apenas para PJ */
    @Column(name = "razao_social", length = 150)
    private String razaoSocial;

    /** CPF (PF) ou CNPJ (PJ) — sem formatação */
    @Column(name = "cpf_cnpj", length = 18)
    private String cpfCnpj;

    /** RG (PF) ou Inscrição Estadual (PJ) */
    @Column(name = "rg_ie", length = 30)
    private String rgIe;

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

    @Column(length = 20)
    private String celular;

    @Column(length = 120)
    private String email;

    // Crédito
    @Column(name = "limite_credito", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @Column(name = "credito_disponivel", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal creditoDisponivel = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

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
