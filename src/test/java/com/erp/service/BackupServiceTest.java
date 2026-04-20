package com.erp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void naoCriaBackupAutomaticoQuandoBaseAindaNaoExiste() throws Exception {
        BackupService service = new BackupService(tempDir);

        assertThat(service.criarBackupAutomaticoDiarioSeNecessario()).isEmpty();
        assertThat(service.listarBackups()).isEmpty();
    }

    @Test
    void criaBackupAutomaticoUmaVezPorDia() throws Exception {
        criarBase("conteudo inicial");
        BackupService service = new BackupService(tempDir);

        assertThat(service.criarBackupAutomaticoDiarioSeNecessario()).isPresent();
        assertThat(service.criarBackupAutomaticoDiarioSeNecessario()).isEmpty();

        List<BackupService.BackupInfo> backups = service.listarBackups();
        assertThat(backups).hasSize(1);
        assertThat(backups.get(0).tipo()).isEqualTo("AUTO_DAILY");
        assertThat(backups.get(0).shaValido()).isTrue();
    }

    @Test
    void removeBackupsAntigosPelaRetencaoDeTrintaDias() throws Exception {
        criarBase("conteudo inicial");
        BackupService service = new BackupService(tempDir);
        service.criarBackup("AUTO_DAILY", LocalDateTime.now().minusDays(40));
        service.criarBackup("AUTO_DAILY", LocalDateTime.now().minusDays(5));

        service.aplicarRetencao();

        List<BackupService.BackupInfo> backups = service.listarBackups();
        assertThat(backups).hasSize(1);
        assertThat(backups.get(0).criadoEm()).contains(LocalDateTime.now().minusDays(5).toLocalDate().toString());
    }

    @Test
    void restauraBackupAgendadoAntesDoStartup() throws Exception {
        criarBase("antes da venda");
        BackupService service = new BackupService(tempDir);
        BackupService.BackupInfo backup = service.criarBackup("AUTO_DAILY", LocalDateTime.now().minusDays(1))
                .orElseThrow();

        Files.writeString(tempDir.resolve("data").resolve("arquivo.txt"), "depois da venda");
        service.agendarRestauracao(backup.path(), "admin");

        service.executarPreStartup();

        assertThat(Files.readString(tempDir.resolve("data").resolve("arquivo.txt"))).isEqualTo("antes da venda");
        assertThat(Files.exists(tempDir.resolve("restore-request.json"))).isFalse();
        assertThat(service.lerUltimoStatus()).isPresent();
        assertThat(service.lerUltimoStatus().get().status()).isEqualTo("SUCESSO");
        assertThat(service.listarBackups())
                .anyMatch(info -> "PRE_RESTORE".equals(info.tipo()) && info.shaValido());
    }

    private void criarBase(String conteudoArquivo) throws Exception {
        Path dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir.resolve("base"));
        Files.writeString(dataDir.resolve("PG_VERSION"), "16");
        Files.writeString(dataDir.resolve("arquivo.txt"), conteudoArquivo);
        Files.writeString(dataDir.resolve("base").resolve("1"), "arquivo interno");
    }
}
