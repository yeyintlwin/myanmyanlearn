package com.barlarlar.myanmyanlearn.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@ControllerAdvice(assignableTypes = MarkdownEditorController.class)
public class MarkdownEditorExceptionAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSize(MaxUploadSizeExceededException e) {
        Map<String, Object> out = new HashMap<>();
        out.put("ok", false);
        out.put("message", "Image too large.");
        return ResponseEntity.status(413).contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)).body(out);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException e) {
        Map<String, Object> out = new HashMap<>();
        out.put("ok", false);
        out.put("message", "Upload failed.");
        return ResponseEntity.badRequest().contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)).body(out);
    }
}
