package com.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class BackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter ISO_TIMESTAMP = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int RETENCAO_DIAS = 30;
    private static final Set<PosixFilePermission> PERMISSOES_DIRETORIO_POSTGRES = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
    );
    private static final Set<PosixFilePermission> PERMISSOES_ARQUIVO_POSTGRES = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );

    private final ObjectMapper objectMapper;
    private final Path appDir;
    private final Path dataDir;
    private final Path backupDir;
    private final Path restoreRequestPath;
    private final Path restoreStatusPath;

    public BackupService() {
        this(defaultAppDir(), new ObjectMapper());
    }

    BackupService(Path appDir) {
        this(appDir, new ObjectMapper());
    }

    private BackupService(Path appDir, ObjectMapper objectMapper) {
        this.appDir = appDir;
        this.dataDir = appDir.resolve("data");
        this.backupDir = appDir.resolve("backups");
        this.restoreRequestPath = appDir.resolve("restore-request.json");
        this.restoreStatusPath = appDir.resolve("restore-status.json");
        this.objectMapper = objectMapper;
    }

    public static void executarManutencaoPreStartup() {
        try {
            new BackupService().executarPreStartup();
        } catch (Exception e) {
            log.error("Falha na manutenção pré-startup de backup/restauração", e);
        }
    }

    public void executarPreStartup() {
        try {
            Files.createDirectories(backupDir);
            boolean restaurou = processarSolicitacaoRestauracao();
            if (!restaurou) {
                criarBackupAutomaticoDiarioSeNecessario();
            }
            aplicarRetencao();
        } catch (Exception e) {
            log.error("Erro na rotina de backup/restauração pré-startup", e);
            salvarStatus(new RestoreStatus("ERRO", LocalDateTime.now().format(ISO_TIMESTAMP),
                    "Erro na rotina pré-startup: " + e.getMessage(), null, null));
        }
    }

    public Optional<BackupInfo> criarBackupAutomaticoDiarioSeNecessario() throws IOException {
        if (!baseExiste()) {
            log.info("Backup automático ignorado: base de dados ainda não existe em {}", dataDir);
            return Optional.empty();
        }

        LocalDate hoje = LocalDate.now();
        boolean jaExisteHoje = listarBackups(false).stream()
                .anyMatch(info -> "AUTO_DAILY".equals(info.tipo())
                        && dataCriacao(info).map(hoje::equals).orElse(false));

        if (jaExisteHoje) {
            log.info("Backup automático ignorado: já existe backup diário de {}", hoje);
            return Optional.empty();
        }

        return criarBackup("AUTO_DAILY", LocalDateTime.now());
    }

    public List<BackupInfo> listarBackups() {
        return listarBackups(true);
    }

    private List<BackupInfo> listarBackups(boolean validarSha) {
        if (!Files.exists(backupDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .map(path -> lerBackupInfo(path, validarSha))
                    .sorted(Comparator.comparing(BackupInfo::criadoEm).reversed())
                    .toList();
        } catch (IOException e) {
            log.warn("Não foi possível listar backups: {}", e.getMessage());
            return List.of();
        }
    }

    public Optional<RestoreStatus> lerUltimoStatus() {
        if (!Files.exists(restoreStatusPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(restoreStatusPath.toFile(), RestoreStatus.class));
        } catch (Exception e) {
            log.warn("Status de restauração inválido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Path getBackupDir() {
        return backupDir;
    }

    public void agendarRestauracao(Path backupPath, String usuarioSolicitante) throws IOException {
        if (backupPath == null || !Files.exists(backupPath)) {
            throw new IllegalArgumentException("Arquivo de backup não encontrado.");
        }

        BackupInfo info = lerBackupInfo(backupPath, true);
        if (!info.shaValido()) {
            throw new IllegalStateException("Backup inválido: SHA-256 não confere.");
        }

        Files.createDirectories(appDir);
        RestoreRequest request = new RestoreRequest(
                backupPath.toAbsolutePath().toString(),
                info.sha256(),
                usuarioSolicitante,
                LocalDateTime.now().format(ISO_TIMESTAMP)
        );

        Path tmp = restoreRequestPath.resolveSibling(restoreRequestPath.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), request);
        moverAtomico(tmp, restoreRequestPath);
        salvarStatus(new RestoreStatus("PENDENTE", LocalDateTime.now().format(ISO_TIMESTAMP),
                "Restauração agendada para o próximo início do sistema.",
                backupPath.toAbsolutePath().toString(), usuarioSolicitante));
    }

    Optional<BackupInfo> criarBackup(String tipo, LocalDateTime agora) throws IOException {
        if (!baseExiste()) {
            return Optional.empty();
        }

        Files.createDirectories(backupDir);

        String timestamp = agora.format(FILE_TIMESTAMP);
        String nomeBase = "sms-backup-" + timestamp;
        Path tmpZip = backupDir.resolve(nomeBase + ".zip.tmp");
        Path finalZip = backupDir.resolve(nomeBase + ".zip");
        Path tmpMetadata = metadataPath(finalZip).resolveSibling(metadataPath(finalZip).getFileName() + ".tmp");
        Path finalMetadata = metadataPath(finalZip);

        Files.deleteIfExists(tmpZip);
        Files.deleteIfExists(tmpMetadata);

        zipDataDir(tmpZip);
        moverAtomico(tmpZip, finalZip);

        String sha256 = sha256(finalZip);
        BackupMetadata metadata = new BackupMetadata(
                finalZip.getFileName().toString(),
                tipo,
                agora.format(ISO_TIMESTAMP),
                Files.size(finalZip),
                sha256,
                dataDir.toAbsolutePath().toString(),
                loadCurrentVersion()
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmpMetadata.toFile(), metadata);
        moverAtomico(tmpMetadata, finalMetadata);

        log.info("Backup {} criado em {}", tipo, finalZip);
        return Optional.of(lerBackupInfo(finalZip, true));
    }

    boolean processarSolicitacaoRestauracao() {
        if (!Files.exists(restoreRequestPath)) {
            return false;
        }

        RestoreRequest request = null;
        try {
            request = objectMapper.readValue(restoreRequestPath.toFile(), RestoreRequest.class);
            Path backupPath = Paths.get(request.backupPath());
            validarBackupParaRestauracao(backupPath, request.sha256());

            Optional<BackupInfo> preRestore = criarBackup("PRE_RESTORE", LocalDateTime.now());
            restaurarBackupFisico(backupPath);

            Files.deleteIfExists(restoreRequestPath);
            salvarStatus(new RestoreStatus("SUCESSO", LocalDateTime.now().format(ISO_TIMESTAMP),
                    "Backup restaurado com sucesso.",
                    backupPath.toAbsolutePath().toString(), request.requestedBy()));
            preRestore.ifPresent(info -> log.info("Backup de segurança pré-restauração criado em {}", info.path()));
            return true;
        } catch (Exception e) {
            log.error("Falha ao restaurar backup solicitado", e);
            salvarStatus(new RestoreStatus("ERRO", LocalDateTime.now().format(ISO_TIMESTAMP),
                    "Falha ao restaurar backup: " + e.getMessage(),
                    request != null ? request.backupPath() : null,
                    request != null ? request.requestedBy() : null));
            preservarSolicitacaoFalha();
            return false;
        }
    }

    void aplicarRetencao() {
        LocalDate limite = LocalDate.now().minusDays(RETENCAO_DIAS);
        for (BackupInfo info : listarBackups(false)) {
            Optional<LocalDate> data = dataCriacao(info);
            if (data.isPresent()
                    && data.get().isBefore(limite)
                    && !data.get().isEqual(LocalDate.now())) {
                try {
                    Files.deleteIfExists(info.path());
                    Files.deleteIfExists(metadataPath(info.path()));
                    log.info("Backup antigo removido pela retenção: {}", info.path());
                } catch (IOException e) {
                    log.warn("Não foi possível remover backup antigo {}: {}", info.path(), e.getMessage());
                }
            }
        }
    }

    private void validarBackupParaRestauracao(Path backupPath, String shaEsperado) throws IOException {
        if (!Files.exists(backupPath)) {
            throw new IOException("Arquivo de backup não encontrado: " + backupPath);
        }
        String shaAtual = sha256(backupPath);
        if (shaEsperado == null || !shaAtual.equalsIgnoreCase(shaEsperado.trim())) {
            throw new IOException("SHA-256 do backup não confere.");
        }
    }

    private void restaurarBackupFisico(Path backupPath) throws IOException {
        Path backupAtualDir = appDir.resolve("data-before-restore-" + LocalDateTime.now().format(FILE_TIMESTAMP));
        Path partialDataDir = appDir.resolve("data-restoring-" + LocalDateTime.now().format(FILE_TIMESTAMP));

        if (Files.exists(partialDataDir)) {
            deleteRecursively(partialDataDir);
        }

        if (Files.exists(dataDir)) {
            moverAtomico(dataDir, backupAtualDir);
        }

        try {
            Files.createDirectories(partialDataDir);
            unzip(backupPath, partialDataDir);
            aplicarPermissoesPostgres(partialDataDir);
            moverAtomico(partialDataDir, dataDir);
            if (Files.exists(backupAtualDir)) {
                deleteRecursively(backupAtualDir);
            }
        } catch (Exception e) {
            if (Files.exists(partialDataDir)) {
                deleteRecursively(partialDataDir);
            }
            if (Files.exists(backupAtualDir) && !Files.exists(dataDir)) {
                moverAtomico(backupAtualDir, dataDir);
            }
            throw e;
        }
    }

    private BackupInfo lerBackupInfo(Path zipPath, boolean validarSha) {
        BackupMetadata metadata = lerMetadata(zipPath).orElseGet(() -> metadataInferido(zipPath));
        boolean shaValido = false;
        if (validarSha) {
            try {
                shaValido = metadata.sha256() != null && sha256(zipPath).equalsIgnoreCase(metadata.sha256());
            } catch (Exception e) {
                log.warn("Não foi possível validar SHA do backup {}: {}", zipPath, e.getMessage());
            }
        }

        return new BackupInfo(
                zipPath,
                zipPath.getFileName().toString(),
                metadata.tipo(),
                metadata.criadoEm(),
                metadata.tamanhoBytes(),
                metadata.sha256(),
                shaValido
        );
    }

    private Optional<BackupMetadata> lerMetadata(Path zipPath) {
        Path metadataPath = metadataPath(zipPath);
        if (!Files.exists(metadataPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(metadataPath.toFile(), BackupMetadata.class));
        } catch (Exception e) {
            log.warn("Metadata inválido para backup {}: {}", zipPath, e.getMessage());
            return Optional.empty();
        }
    }

    private BackupMetadata metadataInferido(Path zipPath) {
        long tamanho = 0L;
        try {
            tamanho = Files.size(zipPath);
        } catch (IOException ignored) {
            // tamanho zero apenas indica arquivo inacessível
        }
        return new BackupMetadata(zipPath.getFileName().toString(), "DESCONHECIDO",
                "1970-01-01T00:00:00", tamanho, null, dataDir.toString(), loadCurrentVersion());
    }

    private Optional<LocalDate> dataCriacao(BackupInfo info) {
        try {
            return Optional.of(LocalDateTime.parse(info.criadoEm(), ISO_TIMESTAMP).toLocalDate());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void zipDataDir(Path zipPath) throws IOException {
        try (OutputStream output = Files.newOutputStream(zipPath);
             ZipOutputStream zip = new ZipOutputStream(output);
             Stream<Path> paths = Files.walk(dataDir)) {
            for (Path path : paths.sorted().toList()) {
                if (path.equals(dataDir)) {
                    continue;
                }
                String entryName = dataDir.relativize(path).toString().replace('\\', '/');
                if (Files.isDirectory(path)) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    zip.putNextEntry(new ZipEntry(entryName));
                    zip.closeEntry();
                } else {
                    zip.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    private void unzip(Path zipPath, Path targetDir) throws IOException {
        try (InputStream input = Files.newInputStream(zipPath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = targetDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(targetDir.normalize())) {
                    throw new IOException("Entrada inválida no ZIP: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private void aplicarPermissoesPostgres(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted().toList()) {
                if (Files.isDirectory(path)) {
                    Files.setPosixFilePermissions(path, PERMISSOES_DIRETORIO_POSTGRES);
                } else {
                    Files.setPosixFilePermissions(path, PERMISSOES_ARQUIVO_POSTGRES);
                }
            }
        } catch (UnsupportedOperationException e) {
            log.debug("Sistema de arquivos não suporta permissões POSIX; ajuste de permissões ignorado.");
        }
    }

    private void preservarSolicitacaoFalha() {
        try {
            if (!Files.exists(restoreRequestPath)) {
                return;
            }
            Path failedPath = appDir.resolve("restore-request.failed-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".json");
            moverAtomico(restoreRequestPath, failedPath);
        } catch (Exception e) {
            log.warn("Não foi possível preservar solicitação de restauração com falha: {}", e.getMessage());
        }
    }

    private void salvarStatus(RestoreStatus status) {
        try {
            Files.createDirectories(appDir);
            Path tmp = restoreStatusPath.resolveSibling(restoreStatusPath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), status);
            moverAtomico(tmp, restoreStatusPath);
        } catch (Exception e) {
            log.warn("Não foi possível salvar status de restauração: {}", e.getMessage());
        }
    }

    private boolean baseExiste() {
        return Files.exists(dataDir.resolve("PG_VERSION"));
    }

    private Path metadataPath(Path zipPath) {
        return zipPath.resolveSibling(zipPath.getFileName() + ".metadata.json");
    }

    private void moverAtomico(Path origem, Path destino) throws IOException {
        try {
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IOException("Não foi possível calcular SHA-256.", e);
        }
    }

    private static Path defaultAppDir() {
        return Paths.get(System.getProperty("user.home"), "erp-desktop");
    }

    private static String loadCurrentVersion() {
        try (InputStream input = BackupService.class.getResourceAsStream("/version.properties")) {
            if (input == null) {
                return "dev";
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("app.version", "dev");
        } catch (IOException e) {
            return "dev";
        }
    }

    public record BackupInfo(
            Path path,
            String arquivo,
            String tipo,
            String criadoEm,
            long tamanhoBytes,
            String sha256,
            boolean shaValido
    ) {
    }

    public record BackupMetadata(
            String arquivo,
            String tipo,
            String criadoEm,
            long tamanhoBytes,
            String sha256,
            String dataDir,
            String appVersion
    ) {
    }

    public record RestoreRequest(
            String backupPath,
            String sha256,
            String requestedBy,
            String requestedAt
    ) {
    }

    public record RestoreStatus(
            String status,
            String updatedAt,
            String message,
            String backupPath,
            String requestedBy
    ) {
    }
}
