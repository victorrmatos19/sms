package com.erp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuario", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"empresa_id", "login"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_perfil",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "perfil_id")
    )
    @Builder.Default
    private Set<PerfilAcesso> perfis = new LinkedHashSet<>();

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String login;

    @NotBlank
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash; // bcrypt

    @Column(length = 120)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

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

    public boolean temPerfil(String nomePerfil) {
        if (nomePerfil == null || perfis == null) {
            return false;
        }
        return perfis.stream().anyMatch(p -> nomePerfil.equals(p.getNome()));
    }

    public PerfilAcesso getPerfilPrincipal() {
        if (perfis == null || perfis.isEmpty()) {
            return null;
        }
        return perfis.stream()
            .min(Comparator.comparingInt(p -> ordemPerfil(p.getNome())))
            .orElse(null);
    }

    public List<PerfilAcesso> getPerfisOrdenados() {
        if (perfis == null) {
            return List.of();
        }
        return perfis.stream()
            .sorted(Comparator.comparingInt(p -> ordemPerfil(p.getNome())))
            .toList();
    }

    private int ordemPerfil(String nomePerfil) {
        return switch (nomePerfil != null ? nomePerfil : "") {
            case "ADMINISTRADOR" -> 0;
            case "GERENTE" -> 1;
            case "VENDAS" -> 2;
            case "FINANCEIRO" -> 3;
            case "ESTOQUE" -> 4;
            default -> 99;
        };
    }
}
