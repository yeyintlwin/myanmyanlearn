package com.barlarlar.myanmyanlearn.service.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredObject put(String key, MultipartFile file) throws IOException;

    default StoredObject putBytes(String key, String originalFilename, String contentType, byte[] bytes)
            throws IOException {
        String name = originalFilename != null ? originalFilename : "file";
        String ct = contentType != null ? contentType : "application/octet-stream";
        byte[] data = bytes != null ? bytes : new byte[0];
        return put(key, new InMemoryMultipartFile(name, ct, data));
    }

    List<StoredObject> list(String prefix) throws IOException;

    default byte[] getBytes(String key) throws IOException {
        throw new IOException("Storage read not supported.");
    }

    void delete(String key) throws IOException;

    record StoredObject(String key, String url) {
    }

    record InMemoryMultipartFile(String originalFilename, String contentType, byte[] bytes) implements MultipartFile {
        @Override
        @org.springframework.lang.NonNull
        public String getName() {
            return "file";
        }

        @Override
        @org.springframework.lang.NonNull
        public String getOriginalFilename() {
            return originalFilename != null ? originalFilename : "";
        }

        @Override
        @org.springframework.lang.NonNull
        public String getContentType() {
            return contentType != null ? contentType : "";
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes != null ? bytes.length : 0L;
        }

        @Override
        @org.springframework.lang.NonNull
        public byte[] getBytes() throws IOException {
            return bytes != null ? bytes : new byte[0];
        }

        @Override
        @org.springframework.lang.NonNull
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(getBytes());
        }

        @Override
        public void transferTo(@org.springframework.lang.NonNull java.io.File dest)
                throws IOException, IllegalStateException {
            Objects.requireNonNull(dest, "dest");
            java.nio.file.Files.write(dest.toPath(), getBytes());
        }
    }
}
