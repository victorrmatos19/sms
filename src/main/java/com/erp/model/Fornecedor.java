package com.erp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fornecedor", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "cpf_cnpj"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fornecedor {

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

    @Column(name = "razao_social", length = 150)
    private String razaoSocial;

    /** CPF (PF) ou CNPJ (PJ) — sem formatação */
    @Column(name = "cpf_cnpj", length = 18)
    private String cpfCnpj;

    @Column(name = "inscricao_estadual", length = 30)
    private String inscricaoEstadual;

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

    @Column(length = 120)
    private String site;

    @Column(name = "contato_nome", length = 100)
    private String contatoNome;

    // Dados bancários
    @Column(name = "banco_nome", length = 60)
    private String bancoNome;

    @Column(name = "banco_agencia", length = 10)
    private String bancoAgencia;

    @Column(name = "banco_conta", length = 20)
    private String bancoConta;

    /** "Corrente" ou "Poupança" */
    @Column(name = "banco_tipo_conta", length = 20)
    private String bancoTipoConta;

    @Column(name = "banco_pix_chave", length = 150)
    private String bancoPixChave;

    /** "CPF", "CNPJ", "EMAIL", "TELEFONE", "ALEATORIA" */
    @Column(name = "banco_pix_tipo", length = 20)
    private String bancoPixTipo;

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
