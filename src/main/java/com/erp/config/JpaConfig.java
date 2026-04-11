package com.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.erp.repository")
public class JpaConfig {
    // Configurações adicionais de JPA podem ser adicionadas aqui conforme necessário
}
