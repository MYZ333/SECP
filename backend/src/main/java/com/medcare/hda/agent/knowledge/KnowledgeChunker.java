package com.medcare.hda.agent.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节感知的 Embedding 语义分块器。
 * <p>标题是强边界；正文先拆成句子，再比较相邻上下文窗口的余弦距离，
 * 以文档内距离分位数作为自适应断点。最大长度只是防止异常超长块，不是主要切分依据。</p>
 */
@Slf4j
@Component
public class KnowledgeChunker {
    static final int FALLBACK_TARGET_SIZE = 700;
    static final int FALLBACK_OVERLAP = 100;
    private static final int EMBEDDING_BATCH_SIZE = 8;
    private static final int SEMANTIC_OVERLAP_MAX_CHARS = 180;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^。！？；!?\\n]+[。！？；!?]?|\\n+");

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final boolean semanticEnabled;
    private final int minChars;
    private final int maxChars;
    private final double breakpointPercentile;
    private final double minimumBreakDistance;

    public KnowledgeChunker(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                            @Value("${hda.agent.rag.chunking.semantic-enabled:true}") boolean semanticEnabled,
                            @Value("${hda.agent.rag.chunking.min-chars:280}") int minChars,
                            @Value("${hda.agent.rag.chunking.max-chars:1100}") int maxChars,
                            @Value("${hda.agent.rag.chunking.breakpoint-percentile:0.80}") double breakpointPercentile,
                            @Value("${hda.agent.rag.chunking.minimum-break-distance:0.12}") double minimumBreakDistance) {
        this.embeddingModelProvider = embeddingModelProvider;
        this.semanticEnabled = semanticEnabled;
        this.minChars = Math.max(80, minChars);
        this.maxChars = Math.max(this.minChars + 100, maxChars);
        this.breakpointPercentile = Math.max(0.5, Math.min(0.95, breakpointPercentile));
        this.minimumBreakDistance = Math.max(0D, Math.min(1D, minimumBreakDistance));
    }

    public record Chunk(int number, String section, String content) {}

    private record Section(String title, String content) {}

    public List<Chunk> chunk(String text) {
        if (!StringUtils.hasText(text)) return List.of();
        EmbeddingModel embeddingModel = semanticEnabled ? embeddingModelProvider.getIfAvailable() : null;
        if (embeddingModel == null) return fallbackChunk(text);
        try {
            List<Chunk> result = new ArrayList<>();
            for (Section section : sections(text)) {
                semanticChunkSection(result, section, embeddingModel);
            }
            return renumber(result);
        } catch (Exception error) {
            log.warn("语义分块不可用，降级到章节/标点分块: {}", error.getMessage());
            return fallbackChunk(text);
        }
    }

    private void semanticChunkSection(List<Chunk> result, Section section, EmbeddingModel embeddingModel) {
        List<String> units = sentenceUnits(section.content());
        if (units.isEmpty()) return;
        if (units.size() == 1) {
            appendLongUnit(result, section.title(), units.getFirst());
            return;
        }

        List<String> contextualUnits = contextualize(units);
        List<float[]> vectors = embedBatches(embeddingModel, contextualUnits);
        if (vectors.size() != units.size()) throw new IllegalStateException("Embedding 返回数量与句子数量不一致");

        double[] distances = new double[vectors.size() - 1];
        for (int i = 0; i < distances.length; i++) distances[i] = cosineDistance(vectors.get(i), vectors.get(i + 1));
        double threshold = percentile(distances, breakpointPercentile);
        buildChunksAtBreakpoints(result, section.title(), units, distances, threshold);
    }

    private List<String> sentenceUnits(String text) {
        List<String> raw = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String unit = matcher.group().replaceAll("\\s+", " ").trim();
            if (StringUtils.hasText(unit)) raw.add(unit);
        }
        List<String> merged = new ArrayList<>();
        StringBuilder shortBuffer = new StringBuilder();
        for (String unit : raw) {
            if (shortBuffer.length() > 0) shortBuffer.append(unit);
            else shortBuffer.append(unit);
            if (shortBuffer.length() >= 40) {
                merged.add(shortBuffer.toString());
                shortBuffer.setLength(0);
            }
        }
        if (!shortBuffer.isEmpty()) {
            if (merged.isEmpty()) merged.add(shortBuffer.toString());
            else merged.set(merged.size() - 1, merged.getLast() + shortBuffer);
        }
        return merged;
    }

    /** 给当前句拼接前后各一句，减少短句向量的不稳定性。 */
    private List<String> contextualize(List<String> units) {
        List<String> result = new ArrayList<>(units.size());
        for (int i = 0; i < units.size(); i++) {
            StringBuilder context = new StringBuilder();
            if (i > 0) context.append(units.get(i - 1));
            context.append(units.get(i));
            if (i + 1 < units.size()) context.append(units.get(i + 1));
            result.add(context.toString());
        }
        return result;
    }

    private List<float[]> embedBatches(EmbeddingModel model, List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(texts.size(), start + EMBEDDING_BATCH_SIZE);
            vectors.addAll(model.embed(texts.subList(start, end)));
        }
        return vectors;
    }

    private void buildChunksAtBreakpoints(List<Chunk> result, String section, List<String> units,
                                          double[] distances, double threshold) {
        List<String> current = new ArrayList<>();
        int currentChars = 0;
        for (int i = 0; i < units.size(); i++) {
            String unit = units.get(i);
            if (unit.length() > maxChars) {
                flushSemantic(result, section, current);
                currentChars = 0;
                appendLongUnit(result, section, unit);
                continue;
            }
            current.add(unit);
            currentChars += unit.length();
            if (i == units.size() - 1) break;

            boolean semanticBreak = currentChars >= minChars
                    && distances[i] >= threshold && distances[i] >= minimumBreakDistance;
            boolean maximumGuard = currentChars + units.get(i + 1).length() > maxChars;
            if (semanticBreak || maximumGuard) {
                String overlap = current.getLast();
                flushSemantic(result, section, current);
                current = new ArrayList<>();
                currentChars = 0;
                if (overlap.length() <= SEMANTIC_OVERLAP_MAX_CHARS) {
                    current.add(overlap);
                    currentChars = overlap.length();
                }
            }
        }
        flushSemantic(result, section, current);
        mergeTinyTail(result, section);
    }

    private void flushSemantic(List<Chunk> result, String section, List<String> units) {
        if (units.isEmpty()) return;
        String content = String.join("", units).trim();
        if (StringUtils.hasText(content)) result.add(new Chunk(result.size() + 1, section, content));
        units.clear();
    }

    private void mergeTinyTail(List<Chunk> result, String section) {
        if (result.size() < 2) return;
        Chunk tail = result.getLast();
        Chunk previous = result.get(result.size() - 2);
        if (!tail.section().equals(section) || !previous.section().equals(section) || tail.content().length() >= minChars / 2) return;
        if (previous.content().length() + tail.content().length() > maxChars) return;
        result.set(result.size() - 2, new Chunk(previous.number(), section,
                mergeWithoutDuplicate(previous.content(), tail.content())));
        result.removeLast();
    }

    private String mergeWithoutDuplicate(String first, String second) {
        int max = Math.min(Math.min(first.length(), second.length()), SEMANTIC_OVERLAP_MAX_CHARS);
        for (int size = max; size >= 1; size--) {
            if (first.regionMatches(first.length() - size, second, 0, size)) return first + second.substring(size);
        }
        return first + second;
    }

    private void appendLongUnit(List<Chunk> result, String section, String unit) {
        int start = 0;
        while (start < unit.length()) {
            String remaining = unit.substring(start);
            int cut = remaining.length() <= maxChars
                    ? remaining.length() : preferredCut(new StringBuilder(remaining), maxChars);
            String content = remaining.substring(0, cut).trim();
            if (!content.isEmpty()) result.add(new Chunk(result.size() + 1, section, content));
            if (start + cut >= unit.length()) break;
            start += Math.max(1, cut - FALLBACK_OVERLAP);
        }
    }

    private double cosineDistance(float[] first, float[] second) {
        if (first == null || second == null || first.length == 0 || first.length != second.length) return 1D;
        double dot = 0D, normA = 0D, normB = 0D;
        for (int i = 0; i < first.length; i++) {
            dot += first[i] * second[i];
            normA += first[i] * first[i];
            normB += second[i] * second[i];
        }
        if (normA == 0D || normB == 0D) return 1D;
        double similarity = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        return 1D - Math.max(-1D, Math.min(1D, similarity));
    }

    private double percentile(double[] values, double percentile) {
        if (values.length == 0) return Double.POSITIVE_INFINITY;
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    private List<Section> sections(String text) {
        List<Section> result = new ArrayList<>();
        String title = "正文";
        StringBuilder content = new StringBuilder();
        for (String paragraph : text.split("\\n+")) {
            String part = paragraph.trim();
            if (!StringUtils.hasText(part)) continue;
            if (isHeading(part)) {
                if (!content.isEmpty()) result.add(new Section(title, content.toString()));
                title = part.replaceFirst("^#+\\s*", "");
                content.setLength(0);
            } else {
                if (!content.isEmpty()) content.append('\n');
                content.append(part);
            }
        }
        if (!content.isEmpty()) result.add(new Section(title, content.toString()));
        return result;
    }

    private List<Chunk> fallbackChunk(String text) {
        List<Chunk> result = new ArrayList<>();
        for (Section section : sections(text)) {
            StringBuilder buffer = new StringBuilder(section.content());
            while (buffer.length() >= FALLBACK_TARGET_SIZE) {
                int cut = preferredCut(buffer, FALLBACK_TARGET_SIZE);
                result.add(new Chunk(result.size() + 1, section.title(), buffer.substring(0, cut).trim()));
                buffer.delete(0, Math.max(1, cut - FALLBACK_OVERLAP));
            }
            if (!buffer.isEmpty()) result.add(new Chunk(result.size() + 1, section.title(), buffer.toString().trim()));
        }
        return renumber(result);
    }

    private boolean isHeading(String text) {
        return text.startsWith("#") || (text.length() <= 40 && !text.matches(".*[。！？；]$"));
    }

    private int preferredCut(StringBuilder text, int target) {
        int from = Math.min(target, text.length());
        for (int i = from - 1; i >= Math.max(80, from - 180); i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '；' || c == '！' || c == '？' || c == '\n') return i + 1;
        }
        return Math.max(1, from);
    }

    private List<Chunk> renumber(List<Chunk> chunks) {
        List<Chunk> result = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            result.add(new Chunk(i + 1, chunk.section(), chunk.content()));
        }
        return result;
    }
}
