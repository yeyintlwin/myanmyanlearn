package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.model.Content;
import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.model.Subcontent;
import com.barlarlar.myanmyanlearn.service.CourseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Comparator;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Controller
public class ReaderController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${google.studio.api-key:}")
    private String googleStudioApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile String cachedModelName;

    @GetMapping("/reader")
    public String reader(
            Model model,
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "sc") Integer subOrder) {
        Course course = courseService.findById(courseId);
        if (course == null) {
            model.addAttribute("error", "Course not found");
            return "reader";
        }
        Optional<Content> contentOpt = course.getContents().stream()
                .filter(c -> c.getOrder() == chapterOrder)
                .findFirst();
        if (contentOpt.isEmpty()) {
            model.addAttribute("course", course);
            model.addAttribute("error", "Chapter not found");
            return "reader";
        }
        Content content = contentOpt.get();
        Optional<Subcontent> subOpt = content.getSubcontents().stream()
                .filter(s -> s.getOrder() == subOrder)
                .findFirst();
        if (subOpt.isEmpty()) {
            model.addAttribute("course", course);
            model.addAttribute("content", content);
            model.addAttribute("error", "Section not found");
            return "reader";
        }
        Subcontent sub = subOpt.get();

        Integer prevCh = null;
        Integer prevSc = null;
        Integer nextCh = null;
        Integer nextSc = null;

        List<Content> orderedContents = Optional.ofNullable(course.getContents()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getSubcontents() != null && !c.getSubcontents().isEmpty())
                .sorted(Comparator.comparingInt(Content::getOrder))
                .toList();

        int currentChapterIdx = -1;
        for (int i = 0; i < orderedContents.size(); i++) {
            if (orderedContents.get(i).getOrder() == chapterOrder) {
                currentChapterIdx = i;
                break;
            }
        }

        List<Subcontent> orderedSubs = Optional.ofNullable(content.getSubcontents()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(Subcontent::getOrder))
                .toList();

        Subcontent prevSub = null;
        Subcontent nextSub = null;
        for (Subcontent s : orderedSubs) {
            int o = s.getOrder();
            if (o < subOrder && (prevSub == null || o > prevSub.getOrder())) {
                prevSub = s;
            }
            if (o > subOrder && (nextSub == null || o < nextSub.getOrder())) {
                nextSub = s;
            }
        }

        if (prevSub != null) {
            prevCh = chapterOrder;
            prevSc = prevSub.getOrder();
        } else if (currentChapterIdx > 0) {
            Content prevChapter = orderedContents.get(currentChapterIdx - 1);
            List<Subcontent> prevSubs = Optional.ofNullable(prevChapter.getSubcontents()).orElse(List.of()).stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(Subcontent::getOrder))
                    .toList();
            if (!prevSubs.isEmpty()) {
                prevCh = prevChapter.getOrder();
                prevSc = prevSubs.get(prevSubs.size() - 1).getOrder();
            }
        }

        if (nextSub != null) {
            nextCh = chapterOrder;
            nextSc = nextSub.getOrder();
        } else if (currentChapterIdx >= 0 && currentChapterIdx < orderedContents.size() - 1) {
            Content nextChapter = orderedContents.get(currentChapterIdx + 1);
            List<Subcontent> nextSubs = Optional.ofNullable(nextChapter.getSubcontents()).orElse(List.of()).stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(Subcontent::getOrder))
                    .toList();
            if (!nextSubs.isEmpty()) {
                nextCh = nextChapter.getOrder();
                nextSc = nextSubs.get(0).getOrder();
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("content", content);
        model.addAttribute("sub", sub);
        model.addAttribute("markdownPath", sub.getMarkdownPath());
        model.addAttribute("hasPrev", prevCh != null && prevSc != null);
        model.addAttribute("prevCh", prevCh);
        model.addAttribute("prevSc", prevSc);
        model.addAttribute("hasNext", nextCh != null && nextSc != null);
        model.addAttribute("nextCh", nextCh);
        model.addAttribute("nextSc", nextSc);
        return "reader";
    }

    @PostMapping(value = "/reader/translate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE
            + ";charset=UTF-8")
    public ResponseEntity<String> translate(@RequestBody TranslateRequest req) {
        if (req == null || req.text == null) {
            return ResponseEntity.badRequest().body("Missing text.");
        }
        String apiKey = googleStudioApiKey == null ? "" : googleStudioApiKey.trim();
        if (apiKey.isEmpty()) {
            return ResponseEntity.status(503).body("Translation API key is not configured.");
        }
        String sourceLang = normalizeLang(req.sourceLang);
        String targetLang = normalizeLang(req.targetLang);
        if (targetLang == null || (sourceLang != null && sourceLang.equals(targetLang))) {
            return ResponseEntity.ok(req.text);
        }
        try {
            String translated = translateMarkdown(apiKey, sourceLang, targetLang, req.text);
            return ResponseEntity.ok(translated);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Translation failed." : e.getMessage();
            return ResponseEntity.status(502).body(msg);
        }
    }

    @GetMapping(value = "/reader/md", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> markdown(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "sc") Integer subOrder) {
        Course course = courseService.findById(courseId);
        if (course == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<Content> contentOpt = course.getContents().stream()
                .filter(c -> c.getOrder() == chapterOrder)
                .findFirst();
        if (contentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Content content = contentOpt.get();
        Optional<Subcontent> subOpt = content.getSubcontents().stream()
                .filter(s -> s.getOrder() == subOrder)
                .findFirst();
        if (subOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String path = subOpt.get().getMarkdownPath();
        try {
            ClassPathResource res = new ClassPathResource(
                    Objects.requireNonNull(path.startsWith("/") ? path.substring(1) : path));
            byte[] bytes = res.getInputStream().readAllBytes();
            return ResponseEntity.ok(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String normalizeLang(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        if (v.isEmpty()) {
            return null;
        }
        if ("jp".equals(v)) {
            return "ja";
        }
        if ("mm".equals(v)) {
            return "my";
        }
        return v;
    }

    private String languageLabel(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            return "";
        }
        String v = langCode.trim().toLowerCase();
        if ("my".equals(v)) {
            return "Burmese (Myanmar)";
        }
        if ("ms".equals(v)) {
            return "Malay";
        }
        if ("ja".equals(v)) {
            return "Japanese";
        }
        if ("en".equals(v)) {
            return "English";
        }
        return v;
    }

    private String translateMarkdown(String apiKey, String sourceLang, String targetLang, String text)
            throws Exception {
        String modelName = getGenerateContentModel(apiKey);
        String url = "https://generativelanguage.googleapis.com/v1beta/" + modelName + ":generateContent?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String sourceLabel = sourceLang == null ? null : languageLabel(sourceLang);
        String targetLabel = languageLabel(targetLang);
        String prompt = (sourceLang == null || sourceLang.isBlank())
                ? "Translate the following Markdown to " + targetLabel + ". "
                : "Translate the following Markdown from " + sourceLabel + " to " + targetLabel + ". ";
        prompt += "Keep technical terms in English (e.g., programming keywords, API names, product names, identifiers, and code symbols). "
                + "Preserve Markdown formatting, links, code blocks, and inline code. "
                + "Return only the translated Markdown.\n\n" + text;

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode content = contents.addObject();
        content.put("role", "user");
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("HTTP " + resp.statusCode() + " " + safeTrim(resp.body(), 300));
        }
        JsonNode data = objectMapper.readTree(resp.body());
        JsonNode partsNode = data.path("candidates").path(0).path("content").path("parts");
        if (!partsNode.isArray()) {
            throw new IOException("Unexpected response from translation service.");
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : partsNode) {
            String t = p.path("text").asText("");
            if (!t.isEmpty()) {
                sb.append(t);
            }
        }
        String out = sb.toString().trim();
        if (out.isEmpty()) {
            throw new IOException("Empty translation result.");
        }
        return out;
    }

    private String getGenerateContentModel(String apiKey) throws Exception {
        String cached = cachedModelName;
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("HTTP " + resp.statusCode() + " " + safeTrim(resp.body(), 300));
        }
        JsonNode data = objectMapper.readTree(resp.body());
        JsonNode models = data.path("models");
        if (!models.isArray()) {
            throw new IOException("No models available.");
        }
        List<String> preferred = List.of(
                "models/gemini-1.5-flash",
                "models/gemini-1.5-flash-002",
                "models/gemini-1.5-flash-001",
                "models/gemini-1.5-pro",
                "models/gemini-1.5-pro-002",
                "models/gemini-1.5-pro-001",
                "models/gemini-pro");
        String picked = null;
        for (String p : preferred) {
            if (hasGenerateContentModel(models, p)) {
                picked = p;
                break;
            }
        }
        if (picked == null) {
            for (JsonNode m : models) {
                String name = m.path("name").asText("");
                if (name.isEmpty()) {
                    continue;
                }
                JsonNode methods = m.path("supportedGenerationMethods");
                if (methods.isArray()) {
                    for (JsonNode method : methods) {
                        if ("generateContent".equals(method.asText(""))) {
                            picked = name;
                            break;
                        }
                    }
                }
                if (picked != null) {
                    break;
                }
            }
        }
        if (picked == null) {
            throw new IOException("No generateContent-capable model available.");
        }
        cachedModelName = picked;
        return picked;
    }

    private boolean hasGenerateContentModel(JsonNode models, String name) {
        for (JsonNode m : models) {
            if (!name.equals(m.path("name").asText(""))) {
                continue;
            }
            JsonNode methods = m.path("supportedGenerationMethods");
            if (!methods.isArray()) {
                return false;
            }
            for (JsonNode method : methods) {
                if ("generateContent".equals(method.asText(""))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private String safeTrim(String value, int max) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, max);
    }

    private static class TranslateRequest {
        public String text;
        public String sourceLang;
        public String targetLang;
    }
}
