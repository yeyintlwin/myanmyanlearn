package com.barlarlar.myanmyanlearn.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private final Path baseDir;
    private final String publicBaseUrl;

    public LocalStorageService(
            @Value("${app.storage.local.base-dir:${user.dir}/.myanmyanlearn/uploads}") String baseDir,
            @Value("${app.storage.public-base-url:/uploads}") String publicBaseUrl) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
        this.publicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
    }

    @Override
    public StoredObject put(String key, MultipartFile file) throws IOException {
        if (key == null || key.isBlank()) {
            throw new IOException("Invalid storage key.");
        }
        Files.createDirectories(baseDir);

        Path targetPath = baseDir.resolve(key).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new IOException("Invalid upload path.");
        }
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String url = publicBaseUrl + "/" + trimLeadingSlash(key);
        return new StoredObject(key, url);
    }

    @Override
    public List<StoredObject> list(String prefix) throws IOException {
        String p = prefix != null ? prefix.trim() : "";
        if (p.isBlank()) {
            return List.of();
        }
        p = trimLeadingSlash(p);
        Path dir = baseDir.resolve(p).normalize();
        if (!dir.startsWith(baseDir)) {
            throw new IOException("Invalid list path.");
        }
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(Files::isRegularFile).forEach(files::add);
        }
        files.sort(Comparator.comparingLong((Path x) -> {
            try {
                return Files.getLastModifiedTime(x).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }).reversed()
                .thenComparing(Path::toString));

        List<StoredObject> out = new ArrayList<>(files.size());
        for (Path f : files) {
            Path rel = baseDir.relativize(f);
            String key = rel.toString().replace('\\', '/');
            String url = publicBaseUrl + "/" + trimLeadingSlash(key);
            out.add(new StoredObject(key, url));
        }
        return out;
    }

    @Override
    public byte[] getBytes(String key) throws IOException {
        String k = key != null ? key.trim() : "";
        if (k.isBlank()) {
            throw new IOException("Invalid storage key.");
        }
        k = trimLeadingSlash(k);
        Path targetPath = baseDir.resolve(k).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new IOException("Invalid read path.");
        }
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            throw new IOException("File not found.");
        }
        return Files.readAllBytes(targetPath);
    }

    @Override
    public void delete(String key) throws IOException {
        String k = key != null ? key.trim() : "";
        if (k.isBlank()) {
            throw new IOException("Invalid storage key.");
        }
        k = trimLeadingSlash(k);
        Path targetPath = baseDir.resolve(k).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new IOException("Invalid delete path.");
        }
        if (!Files.exists(targetPath)) {
            return;
        }
        if (Files.isDirectory(targetPath)) {
            throw new IOException("Refusing to delete directory.");
        }
        Files.delete(targetPath);
        Path parent = targetPath.getParent();
        while (parent != null && !parent.equals(baseDir)) {
            try (Stream<Path> s = Files.list(parent)) {
                if (s.findAny().isPresent()) {
                    break;
                }
            } catch (IOException e) {
                break;
            }
            try {
                Files.delete(parent);
            } catch (IOException e) {
                break;
            }
            parent = parent.getParent();
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    private static String trimLeadingSlash(String v) {
        if (v == null) {
            return "";
        }
        int i = 0;
        while (i < v.length() && v.charAt(i) == '/') {
            i++;
        }
        return v.substring(i);
    }

    private static String normalizePublicBaseUrl(String v) {
        String s = v == null ? "" : v.trim();
        if (s.isBlank()) {
            return "/uploads";
        }
        if (!s.startsWith("/")) {
            s = "/" + s;
        }
        while (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
