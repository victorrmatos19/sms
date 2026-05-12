package com.erp.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Inicializa o PostgreSQL embarcado.
 * O banco de dados fica armazenado em: <user.home>/erp-desktop/data/
 * Isso garante que os dados persistam entre reinicializações.
 */
@Slf4j
@Configuration
public class EmbeddedPostgresConfig {

    @Value("${erp.postgres.port:5433}")
    private int postgresPort;

    @Value("${erp.postgres.database:erp_db}")
    private String databaseName;

    @Value("${erp.postgres.datadir:#{null}}")
    private String dataDirOverride;

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        Path dataDir = dataDirOverride != null && !dataDirOverride.isBlank()
            ? Paths.get(dataDirOverride)
            : Paths.get(System.getProperty("user.home"), "erp-desktop", "data");
        dataDir.toFile().mkdirs();

        log.info("Iniciando PostgreSQL embarcado em: {}", dataDir);
        log.info("Porta: {}", postgresPort);

        // Tenta iniciar normalmente; se falhar por cache de extração corrompido,
        // limpa o diretório temporário e tenta uma segunda vez.
        try {
            return iniciarPostgres(dataDir);
        } catch (Exception primeiroErro) {
            log.warn("Falha ao iniciar PostgreSQL embarcado ({}). Limpando cache de extração e tentando novamente...",
                    primeiroErro.getMessage());
            limparCacheExtracaoPg();
            return iniciarPostgres(dataDir);
        }
    }

    /**
     * Constrói e inicia a instância do EmbeddedPostgres.
     * Separado para permitir retry limpo após falha de extração.
     */
    private EmbeddedPostgres iniciarPostgres(Path dataDir) throws IOException {
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setPort(postgresPort)
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .start();

        // Cria o banco de dados da aplicação se não existir.
        // O Postgres embarcado inicia com o banco "postgres" por padrão.
        criarBancoSeNaoExistir(pg);

        return pg;
    }

    /**
     * Remove o diretório de cache de extração dos binários do Postgres embarcado
     * ({java.io.tmpdir}/embedded-pg). Isso é seguro enquanto apenas uma instância
     * do aplicativo estiver em execução.
     */
    private void limparCacheExtracaoPg() {
        try {
            Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "embedded-pg");
            if (cacheDir.toFile().exists()) {
                log.info("Removendo cache de extração do PostgreSQL embarcado: {}", cacheDir);
                deletarDiretorioRecursivo(cacheDir.toFile());
                log.info("Cache removido com sucesso.");
            }
        } catch (Exception e) {
            log.warn("Não foi possível remover cache de extração do PostgreSQL: {}", e.getMessage());
        }
    }

    private void deletarDiretorioRecursivo(File dir) {
        if (dir.isDirectory()) {
            File[] filhos = dir.listFiles();
            if (filhos != null) {
                for (File filho : filhos) {
                    deletarDiretorioRecursivo(filho);
                }
            }
        }
        if (!dir.delete()) {
            log.warn("Não foi possível deletar: {}", dir.getAbsolutePath());
        }
    }

    private void criarBancoSeNaoExistir(EmbeddedPostgres pg) {
        // Conecta no banco padrão "postgres" para verificar/criar o banco da aplicação
        String urlPostgres = pg.getJdbcUrl("postgres", "postgres");

        try (Connection conn = DriverManager.getConnection(urlPostgres, "postgres", "");
             Statement stmt = conn.createStatement()) {

            // Verifica se o banco já existe
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'"
            );

            if (!rs.next()) {
                log.info("Criando banco de dados '{}'...", databaseName);
                stmt.execute("CREATE DATABASE " + databaseName);
                log.info("Banco de dados '{}' criado com sucesso.", databaseName);
            } else {
                log.info("Banco de dados '{}' já existe.", databaseName);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar banco de dados '" + databaseName + "'", e);
        }
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        String url = embeddedPostgres.getJdbcUrl("postgres", databaseName);
        log.info("DataSource configurado: {}", url);

        return DataSourceBuilder.create()
                .url(url)
                .username("postgres")
                .password("")
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}