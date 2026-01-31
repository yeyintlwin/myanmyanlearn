package com.barlarlar.myanmyanlearn.service.storage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {
    private final Object s3;
    private final String bucket;
    private final String keyPrefix;
    private final String publicUrlPrefix;
    private final boolean publicRead;

    public S3StorageService(
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.region:}") String region,
            @Value("${app.storage.s3.key-prefix:}") String keyPrefix,
            @Value("${app.storage.s3.public-url-prefix:}") String publicUrlPrefix,
            @Value("${app.storage.s3.public-read:true}") boolean publicRead) {
        this.bucket = bucket != null ? bucket.trim() : "";
        if (this.bucket.isBlank()) {
            throw new IllegalArgumentException("Missing app.storage.s3.bucket");
        }
        this.keyPrefix = normalizeKeyPrefix(keyPrefix);
        this.publicUrlPrefix = normalizeUrlPrefix(publicUrlPrefix);
        this.publicRead = publicRead;

        this.s3 = createClient(region);
    }

    @Override
    public StoredObject put(String key, MultipartFile file) throws IOException {
        String logicalKey = normalizeObjectKey(trimLeadingSlash(key));
        String objectKey = normalizeObjectKey(keyPrefix + logicalKey);
        putObject(objectKey, file);
        String url = buildPublicUrl(objectKey);
        return new StoredObject(logicalKey, url);
    }

    @Override
    public List<StoredObject> list(String prefix) throws IOException {
        String p = prefix != null ? prefix.trim() : "";
        if (p.isBlank()) {
            return List.of();
        }
        String logicalPrefix = normalizeObjectKey(trimLeadingSlash(p));
        String objectPrefix = normalizeObjectKey(keyPrefix + logicalPrefix);
        List<StoredObject> out = new ArrayList<>();
        String token = null;
        while (true) {
            try {
                Object reqBuilder = invokeStatic(
                        "software.amazon.awssdk.services.s3.model.ListObjectsV2Request",
                        "builder");
                invoke(reqBuilder, "bucket", new Class<?>[] { String.class }, new Object[] { bucket });
                invoke(reqBuilder, "prefix", new Class<?>[] { String.class }, new Object[] { objectPrefix });
                if (token != null && !token.isBlank()) {
                    invoke(reqBuilder, "continuationToken", new Class<?>[] { String.class }, new Object[] { token });
                }
                Object req = invoke(reqBuilder, "build");
                Object resp = invoke(s3, "listObjectsV2", new Class<?>[] { req.getClass() }, new Object[] { req });
                Object contents = invoke(resp, "contents");
                if (contents instanceof Iterable<?> it) {
                    for (Object o : it) {
                        if (o == null) {
                            continue;
                        }
                        Object keyObj = invoke(o, "key");
                        String objectKey = keyObj != null ? String.valueOf(keyObj) : "";
                        if (objectKey.isBlank()) {
                            continue;
                        }
                        String logicalKey = objectKey;
                        if (keyPrefix != null && !keyPrefix.isBlank() && logicalKey.startsWith(keyPrefix)) {
                            logicalKey = logicalKey.substring(keyPrefix.length());
                        }
                        out.add(new StoredObject(logicalKey, buildPublicUrl(objectKey)));
                    }
                }
                Object truncatedObj = invoke(resp, "isTruncated");
                boolean truncated = truncatedObj instanceof Boolean b ? b.booleanValue() : false;
                if (!truncated) {
                    break;
                }
                Object tokenObj = invoke(resp, "nextContinuationToken");
                token = tokenObj != null ? String.valueOf(tokenObj) : null;
                if (token == null || token.isBlank()) {
                    break;
                }
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return out;
    }

    @Override
    public void delete(String key) throws IOException {
        String k = key != null ? key.trim() : "";
        if (k.isBlank()) {
            throw new IOException("Invalid storage key.");
        }
        String logicalKey = normalizeObjectKey(trimLeadingSlash(k));
        String objectKey = normalizeObjectKey(keyPrefix + logicalKey);
        try {
            Object reqBuilder = invokeStatic(
                    "software.amazon.awssdk.services.s3.model.DeleteObjectRequest",
                    "builder");
            invoke(reqBuilder, "bucket", new Class<?>[] { String.class }, new Object[] { bucket });
            invoke(reqBuilder, "key", new Class<?>[] { String.class }, new Object[] { objectKey });
            Object req = invoke(reqBuilder, "build");
            invoke(s3, "deleteObject", new Class<?>[] { req.getClass() }, new Object[] { req });
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private String buildPublicUrl(String objectKey) {
        if (publicUrlPrefix != null && !publicUrlPrefix.isBlank()) {
            return publicUrlPrefix + "/" + objectKey;
        }
        try {
            Object utilities = invoke(s3, "utilities");
            Object getUrlReqBuilder = invokeStatic("software.amazon.awssdk.services.s3.model.GetUrlRequest", "builder");
            invoke(getUrlReqBuilder, "bucket", new Class<?>[] { String.class }, new Object[] { bucket });
            invoke(getUrlReqBuilder, "key", new Class<?>[] { String.class }, new Object[] { objectKey });
            Object getUrlReq = invoke(getUrlReqBuilder, "build");
            Object url = invoke(utilities, "getUrl", new Class<?>[] { getUrlReq.getClass() },
                    new Object[] { getUrlReq });
            return url != null ? url.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Object createClient(String region) {
        try {
            Object builder = invokeStatic("software.amazon.awssdk.services.s3.S3Client", "builder");
            if (region != null && !region.isBlank()) {
                Object regionObj = invokeStatic(
                        "software.amazon.awssdk.regions.Region",
                        "of",
                        new Class<?>[] { String.class },
                        new Object[] { region.trim() });
                invoke(builder, "region", new Class<?>[] { regionObj.getClass() }, new Object[] { regionObj });
            }
            return invoke(builder, "build");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create S3 client: " + e.getMessage(), e);
        }
    }

    private void putObject(String objectKey, MultipartFile file) throws IOException {
        try {
            Object reqBuilder = invokeStatic("software.amazon.awssdk.services.s3.model.PutObjectRequest", "builder");
            invoke(reqBuilder, "bucket", new Class<?>[] { String.class }, new Object[] { bucket });
            invoke(reqBuilder, "key", new Class<?>[] { String.class }, new Object[] { objectKey });
            invoke(reqBuilder, "contentType", new Class<?>[] { String.class }, new Object[] { file.getContentType() });
            if (publicRead) {
                Object acl = getStaticField("software.amazon.awssdk.services.s3.model.ObjectCannedACL", "PUBLIC_READ");
                invoke(reqBuilder, "acl", new Class<?>[] { acl.getClass() }, new Object[] { acl });
            }
            Object req = invoke(reqBuilder, "build");

            Object requestBody = invokeStatic(
                    "software.amazon.awssdk.core.sync.RequestBody",
                    "fromInputStream",
                    new Class<?>[] { java.io.InputStream.class, long.class },
                    new Object[] { file.getInputStream(), file.getSize() });
            invoke(s3, "putObject", new Class<?>[] { req.getClass(), requestBody.getClass() },
                    new Object[] { req, requestBody });
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static Object invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        return m.invoke(target);
    }

    private static Object invoke(Object target, String name, Class<?>[] paramTypes, Object[] args) throws Exception {
        Method m = target.getClass().getMethod(name, paramTypes);
        return m.invoke(target, args);
    }

    private static Object invokeStatic(String className, String name) throws Exception {
        Class<?> c = Class.forName(className);
        Method m = c.getMethod(name);
        return m.invoke(null);
    }

    private static Object invokeStatic(String className, String name, Class<?>[] paramTypes, Object[] args)
            throws Exception {
        Class<?> c = Class.forName(className);
        Method m = c.getMethod(name, paramTypes);
        return m.invoke(null, args);
    }

    private static Object getStaticField(String className, String fieldName) throws Exception {
        Class<?> c = Class.forName(className);
        return c.getField(fieldName).get(null);
    }

    private static String normalizeKeyPrefix(String v) {
        String s = v == null ? "" : v.trim();
        if (s.isBlank()) {
            return "";
        }
        s = trimLeadingSlash(s);
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.isBlank() ? "" : (s + "/");
    }

    private static String normalizeObjectKey(String v) {
        String s = v == null ? "" : v.trim();
        s = s.replace('\\', '/');
        s = trimLeadingSlash(s);
        while (s.contains("//")) {
            s = s.replace("//", "/");
        }
        return s;
    }

    private static String trimLeadingSlash(String v) {
        if (v == null) {
            return "";
        }
        int i = 0;
        while (i < v.length() && (v.charAt(i) == '/' || v.charAt(i) == '\\')) {
            i++;
        }
        return v.substring(i);
    }

    private static String normalizeUrlPrefix(String v) {
        String s = v == null ? "" : v.trim();
        if (s.isBlank()) {
            return "";
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
