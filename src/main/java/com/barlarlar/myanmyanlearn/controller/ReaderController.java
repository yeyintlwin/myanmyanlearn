package com.barlarlar.myanmyanlearn.controller;

import com.barlarlar.myanmyanlearn.model.Content;
import com.barlarlar.myanmyanlearn.model.Course;
import com.barlarlar.myanmyanlearn.model.Subcontent;
import com.barlarlar.myanmyanlearn.service.CourseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class ReaderController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;

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
        ReaderPageData data = buildReaderPageData(courseId, chapterOrder, subOrder);
        if (data.course == null) {
            model.addAttribute("error", data.errorMessage == null ? "Course not found" : data.errorMessage);
            return "reader";
        }
        model.addAttribute("course", data.course);
        if (data.content != null) {
            model.addAttribute("content", data.content);
        }
        if (data.sub != null) {
            model.addAttribute("sub", data.sub);
        }
        if (data.errorMessage != null) {
            model.addAttribute("error", data.errorMessage);
        }
        model.addAttribute("markdownPath",
                "/reader/md?courseId=" + courseId + "&ch=" + chapterOrder + "&sc=" + subOrder);
        model.addAttribute("hasPrev", data.prevCh != null && data.prevSc != null);
        model.addAttribute("prevCh", data.prevCh);
        model.addAttribute("prevSc", data.prevSc);
        model.addAttribute("hasNext", data.nextCh != null && data.nextSc != null);
        model.addAttribute("nextCh", data.nextCh);
        model.addAttribute("nextSc", data.nextSc);
        return "reader";
    }

    @GetMapping(value = "/reader/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReaderPageResponse> readerPage(
            @RequestParam(name = "courseId") String courseId,
            @RequestParam(name = "ch") Integer chapterOrder,
            @RequestParam(name = "sc") Integer subOrder) {
        ReaderPageData data = buildReaderPageData(courseId, chapterOrder, subOrder);
        ReaderPageResponse resp = new ReaderPageResponse();
        resp.courseId = courseId;
        resp.ch = chapterOrder;
        resp.sc = subOrder;
        resp.markdownPath = "/reader/md?courseId=" + courseId + "&ch=" + chapterOrder + "&sc=" + subOrder;

        if (data.course == null) {
            resp.error = data.errorMessage == null ? "Course not found" : data.errorMessage;
            return ResponseEntity.status(404).body(resp);
        }
        resp.courseTitle = data.course.getTitle();
        resp.courseLanguage = data.course.getLanguage();
        if (data.content == null) {
            resp.error = data.errorMessage == null ? "Chapter not found" : data.errorMessage;
            return ResponseEntity.status(404).body(resp);
        }
        resp.chapterTitle = data.content.getTitle();
        resp.chapterLabel = "Chapter " + data.content.getOrder();
        if (data.sub == null) {
            resp.error = data.errorMessage == null ? "Section not found" : data.errorMessage;
            return ResponseEntity.status(404).body(resp);
        }
        resp.subTitle = data.sub.getTitle();
        resp.hasPrev = data.prevCh != null && data.prevSc != null;
        resp.prevCh = data.prevCh;
        resp.prevSc = data.prevSc;
        resp.hasNext = data.nextCh != null && data.nextSc != null;
        resp.nextCh = data.nextCh;
        resp.nextSc = data.nextSc;
        CourseService.SubchapterMarkdownInfo mdInfo = courseService.findSubchapterMarkdownInfoFromDatabase(courseId,
                chapterOrder, subOrder);
        resp.markdown = mdInfo == null ? null : mdInfo.markdown;
        resp.updatedAt = mdInfo == null || mdInfo.updatedAt == null ? null : mdInfo.updatedAt.toString();
        if (resp.markdown == null) {
            resp.error = "Markdown not found";
            return ResponseEntity.status(404).body(resp);
        }
        return ResponseEntity.ok(resp);
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
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (!courseService.canAccessCourse(courseId, auth)) {
            return ResponseEntity.notFound().build();
        }
        String markdown = courseService.findMarkdownFromDatabase(courseId, chapterOrder, subOrder);
        if (markdown == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(markdown);
    }

    private ReaderPageData buildReaderPageData(String courseId, Integer chapterOrder, Integer subOrder) {
        ReaderPageData data = new ReaderPageData();
        if (courseId == null || courseId.isBlank() || chapterOrder == null || subOrder == null) {
            data.errorMessage = "Missing parameters";
            return data;
        }
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        Course course = courseService.findAccessibleByIdFromDatabase(courseId, auth);
        if (course == null) {
            data.errorMessage = "Course not found";
            return data;
        }
        data.course = course;
        Optional<Content> contentOpt = Optional.ofNullable(course.getContents()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getOrder() == chapterOrder)
                .findFirst();
        if (contentOpt.isEmpty()) {
            data.errorMessage = "Chapter not found";
            return data;
        }
        Content content = contentOpt.get();
        data.content = content;
        Optional<Subcontent> subOpt = Optional.ofNullable(content.getSubcontents()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getOrder() == subOrder)
                .findFirst();
        if (subOpt.isEmpty()) {
            data.errorMessage = "Section not found";
            return data;
        }
        Subcontent sub = subOpt.get();
        data.sub = sub;

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

        Integer prevCh = null;
        Integer prevSc = null;
        Integer nextCh = null;
        Integer nextSc = null;

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

        data.prevCh = prevCh;
        data.prevSc = prevSc;
        data.nextCh = nextCh;
        data.nextSc = nextSc;
        return data;
    }

    private static class ReaderPageData {
        public Course course;
        public Content content;
        public Subcontent sub;
        public Integer prevCh;
        public Integer prevSc;
        public Integer nextCh;
        public Integer nextSc;
        public String errorMessage;
    }

    public static class ReaderPageResponse {
        public String courseId;
        public Integer ch;
        public Integer sc;
        public String courseTitle;
        public String courseLanguage;
        public String chapterLabel;
        public String chapterTitle;
        public String subTitle;
        public boolean hasPrev;
        public Integer prevCh;
        public Integer prevSc;
        public boolean hasNext;
        public Integer nextCh;
        public Integer nextSc;
        public String markdown;
        public String updatedAt;
        public String markdownPath;
        public String error;
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
        prompt += "Keep technical terms in English (e.g., programming keywords, API names, product names, "
                + "identifiers, and code symbols). "
                + "Do NOT translate or change enumerations like 'ア', 'イ', 'ウ', 'エ', 'オ' or corresponding numbering (e.g., maintain '1.', '2.', 'A.', 'B.', etc.). "
                + "Preserve Markdown formatting, links, code blocks, and inline code. "
                + "Do NOT render HTML/CSS code. Keep all code blocks explicitly wrapped in Markdown code fences (```). "
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

    @PostMapping(value = "/reader/translate/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> translateBatch(@RequestBody BatchTranslateRequest req) {
        if (req == null || req.items == null || req.items.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String apiKey = googleStudioApiKey == null ? "" : googleStudioApiKey.trim();
        if (apiKey.isEmpty()) {
            return ResponseEntity.status(503).build();
        }
        String sourceLang = normalizeLang(req.sourceLang);
        String targetLang = normalizeLang(req.targetLang);
        if (targetLang == null || (sourceLang != null && sourceLang.equals(targetLang))) {
            return ResponseEntity.ok(req.items);
        }

        try {
            // We serve a single prompt with all items as a JSON object
            // to get a JSON object back with translated values.
            ObjectNode root = objectMapper.createObjectNode();
            req.items.forEach(root::put);
            String jsonToTranslate = objectMapper.writeValueAsString(root);

            String prompt = "Translate the values of the following JSON object to " + languageLabel(targetLang) + ". "
                    + (sourceLang != null ? "Source language is " + languageLabel(sourceLang) + ". " : "")
                    + "Do NOT translate keys. Keep the JSON structure exactly the same. "
                    + "Keep technical terms in English (e.g., programming keywords, API names, product names, identifiers, and code symbols). "
                    + "Do NOT translate or change enumerations like 'ア', 'イ', 'ウ', 'エ', 'オ' or corresponding numbering (e.g., maintain '1.', '2.', 'A.', 'B.', etc.). "
                    + "Preserve Markdown formatting. "
                    + "Return ONLY the valid JSON object.\n\n" + jsonToTranslate;

            String translatedJson = translateTextGeneric(apiKey, prompt);

            // Clean up code fences if the model adds them
            translatedJson = translatedJson.trim();
            if (translatedJson.startsWith("```json")) {
                translatedJson = translatedJson.substring(7);
            } else if (translatedJson.startsWith("```")) {
                translatedJson = translatedJson.substring(3);
            }
            if (translatedJson.endsWith("```")) {
                translatedJson = translatedJson.substring(0, translatedJson.length() - 3);
            }

            JsonNode resultNode = objectMapper.readTree(translatedJson);
            Map<String, String> result = new java.util.HashMap<>();
            if (resultNode.isObject()) {
                resultNode.fieldNames().forEachRemaining(fieldName -> {
                    result.put(fieldName, resultNode.get(fieldName).asText());
                });
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Batch translation error: " + e.getMessage());
            return ResponseEntity.status(502).build();
        }
    }

    private String translateTextGeneric(String apiKey, String prompt) throws Exception {
        String modelName = getGenerateContentModel(apiKey);
        String url = "https://generativelanguage.googleapis.com/v1beta/" + modelName + ":generateContent?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

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
            System.err.println("Google AI API Error: " + resp.statusCode() + " " + resp.body());
            throw new IOException("HTTP " + resp.statusCode());
        }

        try {
            JsonNode data = objectMapper.readTree(resp.body());
            JsonNode partsNode = data.path("candidates").path(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            if (partsNode.isArray()) {
                for (JsonNode p : partsNode) {
                    sb.append(p.path("text").asText(""));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            System.err.println("Failed to parse Google AI response: " + resp.body());
            throw e;
        }
    }

    private static class TranslateRequest {
        public String text;
        public String sourceLang;
        public String targetLang;
    }

    private static class BatchTranslateRequest {
        public Map<String, String> items;
        public String sourceLang;
        public String targetLang;
    }
}
