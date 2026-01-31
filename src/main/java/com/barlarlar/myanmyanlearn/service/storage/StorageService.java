package com.barlarlar.myanmyanlearn.service.storage;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredObject put(String key, MultipartFile file) throws IOException;

    List<StoredObject> list(String prefix) throws IOException;

    void delete(String key) throws IOException;

    record StoredObject(String key, String url) {
    }
}
