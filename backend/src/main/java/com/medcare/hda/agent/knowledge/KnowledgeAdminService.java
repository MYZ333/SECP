package com.medcare.hda.agent.knowledge;

import com.medcare.hda.common.PageResult;
import com.medcare.hda.exception.BusinessException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeAdminService {
    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeDocumentParser parser;
    private final KnowledgeChunker chunker;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Value("${hda.agent.rag.storage-dir:../data/rag/documents}")
    private String storageDir;
    @Value("${hda.agent.rag.seed-dir:../data/rag/input}")
    private String seedDir;

    public KnowledgeAdminService(JdbcTemplate jdbcTemplate, KnowledgeDocumentParser parser, KnowledgeChunker chunker,
                                 @Qualifier("knowledgeVectorStore") ObjectProvider<VectorStore> vectorStoreProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.parser = parser;
        this.chunker = chunker;
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public PageResult<KnowledgeDocumentView> page(long pageNum, long pageSize, String keyword, String status) {
        return page(pageNum, pageSize, keyword, status, "HEALTH");
    }

    public PageResult<KnowledgeDocumentView> page(long pageNum, long pageSize, String keyword, String status, String agentType) {
        long current = Math.max(1, pageNum);
        long size = Math.min(100, Math.max(1, pageSize));
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE deleted=0 ");
        String normalizedAgentType = normalizeAgentType(agentType);
        where.append(" AND agent_type=? ");
        params.add(normalizedAgentType);
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (title LIKE ? OR source_org LIKE ? OR category LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (StringUtils.hasText(status)) { where.append(" AND status=? "); params.add(status); }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_document" + where,
                Long.class, params.toArray());
        params.add(size); params.add((current - 1) * size);
        List<KnowledgeDocumentView> records = jdbcTemplate.query("""
                SELECT id,agent_type,title,source_org,source_url,published_date,version_no,category,file_name,status,
                       chunk_count,failure_reason,create_time,update_time
                FROM knowledge_document
                """ + where + " ORDER BY update_time DESC LIMIT ? OFFSET ?", (rs, rowNum) -> new KnowledgeDocumentView(
                rs.getLong("id"), rs.getString("agent_type"), rs.getString("title"), rs.getString("source_org"), rs.getString("source_url"),
                rs.getDate("published_date") == null ? null : rs.getDate("published_date").toLocalDate(),
                rs.getString("version_no"), rs.getString("category"), rs.getString("file_name"), rs.getString("status"),
                rs.getInt("chunk_count"), rs.getString("failure_reason"), rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()), params.toArray());
        PageResult<KnowledgeDocumentView> result = new PageResult<>();
        result.setTotal(total == null ? 0 : total); result.setRecords(records); result.setCurrent(current); result.setSize(size);
        return result;
    }

    public List<KnowledgeChunkView> chunks(Long documentId) {
        return jdbcTemplate.query("""
                SELECT id,document_id,chunk_no,section_title,content,status FROM knowledge_chunk
                WHERE document_id=? AND deleted=0 ORDER BY chunk_no
                """, (rs, rowNum) -> new KnowledgeChunkView(rs.getLong("id"), rs.getLong("document_id"),
                rs.getInt("chunk_no"), rs.getString("section_title"), rs.getString("content"), rs.getString("status")), documentId);
    }

    @Transactional
    public Long upload(MultipartFile file, String title, String sourceOrg, String sourceUrl,
                       LocalDate publishedDate, String versionNo, String category) {
        return upload(file, title, sourceOrg, sourceUrl, publishedDate, versionNo, category, "HEALTH");
    }

    @Transactional
    public Long upload(MultipartFile file, String title, String sourceOrg, String sourceUrl,
                       LocalDate publishedDate, String versionNo, String category, String agentType) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择知识文档");
        if (!StringUtils.hasText(title) || !StringUtils.hasText(sourceOrg) || !StringUtils.hasText(sourceUrl)
                || !StringUtils.hasText(category)) throw new BusinessException("标题、来源机构、官方链接和分类不能为空");
        try {
            Path root = Path.of(storageDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            String original = Path.of(file.getOriginalFilename() == null ? "document.txt" : file.getOriginalFilename()).getFileName().toString();
            Path target = root.resolve(UUID.randomUUID() + "-" + original).normalize();
            if (!target.startsWith(root)) throw new BusinessException("非法文件名");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return createDocument(target, original, title, sourceOrg, sourceUrl, publishedDate, versionNo, category,
                    normalizeAgentType(agentType));
        } catch (IOException e) {
            throw new BusinessException("知识文档保存失败：" + e.getMessage());
        }
    }

    @Transactional
    public int importSeeds() {
        return importSeeds("HEALTH");
    }

    @Transactional
    public int importSeeds(String agentType) {
        String normalizedAgentType = normalizeAgentType(agentType);
        Path root = Path.of("APPLICATION".equals(normalizedAgentType) ? seedDir + "/app-assistant" : seedDir)
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new BusinessException("首批语料目录不存在：" + root);
        int imported = 0;
        try (var files = Files.list(root)) {
            for (Path path : files.filter(Files::isRegularFile).toList()) {
                String checksum = sha256(Files.readAllBytes(path));
                Integer existing = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM knowledge_document WHERE checksum=? AND deleted=0",
                        Integer.class, checksum);
                if (existing != null && existing > 0) continue;
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                String title = firstHeading(raw, path.getFileName().toString());
                String org = metadata(raw, "来源机构：", "中华人民共和国国家卫生健康委员会");
                String url = metadata(raw, "官方来源：", "https://www.nhc.gov.cn/");
                String category = metadata(raw, "资料类型：", "健康科普");
                try {
                    createDocument(path, path.getFileName().toString(), title, org, url, null, "seed-1", category, normalizedAgentType);
                    imported++;
                } catch (DuplicateKeyException ignored) { }
            }
        } catch (IOException e) {
            throw new BusinessException("首批语料读取失败：" + e.getMessage());
        }
        return imported;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void publish(Long documentId, Long reviewerId) {
        Map<String, Object> doc = document(documentId);
        List<KnowledgeChunkView> chunks = chunks(documentId);
        if (chunks.isEmpty()) throw new BusinessException("文档没有可发布的切片");
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) throw new BusinessException("Chroma 向量库未连接，请确认本机 Chroma 服务已启动");
        jdbcTemplate.update("UPDATE knowledge_document SET status='INDEXING',failure_reason=NULL WHERE id=?", documentId);
        try {
            List<Document> documents = chunks.stream().map(chunk -> Document.builder()
                    .id("knowledge-" + chunk.id()).text(chunk.content())
                    .metadata(Map.of(
                            "documentId", documentId,
                            "title", doc.get("title"),
                            "sourceOrg", doc.get("source_org"),
                            "sourceUrl", doc.get("source_url"),
                            "publishedDate", doc.get("published_date") == null ? "" : doc.get("published_date").toString(),
                            "category", doc.get("category"),
                            "agentType", doc.get("agent_type"),
                            "section", chunk.sectionTitle() == null ? "正文" : chunk.sectionTitle()))
                    .build()).toList();
            addDocumentsBatched(vectorStore, documents);
            jdbcTemplate.update("UPDATE knowledge_chunk SET vector_id=CONCAT('knowledge-',id),status='PUBLISHED' WHERE document_id=? AND deleted=0", documentId);
            jdbcTemplate.update("UPDATE knowledge_document SET status='PUBLISHED',reviewer_id=? WHERE id=?", reviewerId, documentId);
        } catch (Exception e) {
            jdbcTemplate.update("UPDATE knowledge_document SET status='FAILED',failure_reason=? WHERE id=?",
                    abbreviate(e.getMessage(), 900), documentId);
            throw new BusinessException("知识索引构建失败：" + e.getMessage());
        }
    }

    @Transactional
    public void inactive(Long documentId) {
        document(documentId);
        clearIndexKeys(documentId);
        jdbcTemplate.update("UPDATE knowledge_document SET status='INACTIVE' WHERE id=? AND deleted=0", documentId);
    }

    @Transactional
    public void delete(Long documentId) {
        Map<String, Object> doc = document(documentId);
        String status = String.valueOf(doc.get("status"));
        if (!"INACTIVE".equals(status)) {
            throw new BusinessException("只有已停用的文档才能删除，当前状态为" + status);
        }
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE document_id=?", documentId);
        jdbcTemplate.update("DELETE FROM knowledge_document WHERE id=?", documentId);
        deleteUploadedFile(doc.get("file_path"));
    }

    private void clearIndexKeys(Long documentId) {
        List<String> vectorIds = jdbcTemplate.query("""
                        SELECT COALESCE(NULLIF(vector_id,''),CONCAT('knowledge-',id))
                        FROM knowledge_chunk WHERE document_id=? AND deleted=0
                        """,
                (rs, rowNum) -> rs.getString(1), documentId);
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (!vectorIds.isEmpty()) {
            if (vectorStore == null) throw new BusinessException("Chroma 向量库未连接，无法确认索引已删除");
            vectorStore.delete(vectorIds);
        }
        jdbcTemplate.update("UPDATE knowledge_chunk SET vector_id=NULL,status='INACTIVE' WHERE document_id=? AND deleted=0", documentId);
    }

    public BatchResult publishBatch(List<Long> documentIds, Long reviewerId) {
        return executeBatch(documentIds, id -> {
            requireDocumentStatus(id, "DRAFT", "FAILED", "INACTIVE");
            publish(id, reviewerId);
        });
    }

    public BatchResult inactiveBatch(List<Long> documentIds) {
        return executeBatch(documentIds, id -> {
            requireDocumentStatus(id, "PUBLISHED", "INACTIVE");
            inactive(id);
        });
    }

    private BatchResult executeBatch(List<Long> documentIds, BatchAction action) {
        List<Long> ids = normalizeBatchIds(documentIds);
        List<BatchFailure> failures = new ArrayList<>();
        int succeeded = 0;
        for (Long id : ids) {
            try {
                action.execute(id);
                succeeded++;
            } catch (Exception error) {
                failures.add(new BatchFailure(id, abbreviate(error.getMessage(), 200)));
            }
        }
        return new BatchResult(ids.size(), succeeded, failures.size(), failures);
    }

    private List<Long> normalizeBatchIds(List<Long> documentIds) {
        if (documentIds == null) throw new BusinessException("请选择要操作的资料");
        List<Long> ids = documentIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) throw new BusinessException("请选择要操作的资料");
        if (ids.size() > 100) throw new BusinessException("单次最多批量操作100份资料");
        return ids;
    }

    private Map<String, Object> requireDocumentStatus(Long documentId, String... allowedStatuses) {
        Map<String, Object> doc = document(documentId);
        String status = String.valueOf(doc.get("status"));
        if (!List.of(allowedStatuses).contains(status)) {
            throw new BusinessException("资料当前状态为" + status + "，不支持此操作");
        }
        return doc;
    }

    private void deleteUploadedFile(Object filePathValue) {
        if (filePathValue == null || !StringUtils.hasText(storageDir)) return;
        try {
            Path storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
            Path filePath = Path.of(String.valueOf(filePathValue)).toAbsolutePath().normalize();
            // 首批语料指向只读种子目录；这里只删除上传目录内的原文件。
            if (filePath.startsWith(storageRoot)) Files.deleteIfExists(filePath);
        } catch (IOException error) {
            throw new BusinessException("文档原文件删除失败：" + error.getMessage());
        }
    }

    @FunctionalInterface
    private interface BatchAction {
        void execute(Long documentId);
    }

    public record BatchFailure(Long documentId, String reason) {}

    public record BatchResult(int total, int succeeded, int failed, List<BatchFailure> failures) {}

    private Long createDocument(Path path, String fileName, String title, String sourceOrg, String sourceUrl,
                                LocalDate publishedDate, String versionNo, String category, String agentType) throws IOException {
        String checksum = sha256(Files.readAllBytes(path));
        String content = parser.parse(path);
        List<KnowledgeChunker.Chunk> chunks = chunker.chunk(content);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO knowledge_document(agent_type,title,source_org,source_url,published_date,version_no,category,file_name,file_path,checksum,status,chunk_count)
                    VALUES(?,?,?,?,?,?,?,?,?,?,'DRAFT',?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, agentType); ps.setString(2, title); ps.setString(3, sourceOrg); ps.setString(4, sourceUrl);
            ps.setDate(5, publishedDate == null ? null : Date.valueOf(publishedDate)); ps.setString(6, versionNo);
            ps.setString(7, category); ps.setString(8, fileName); ps.setString(9, path.toAbsolutePath().toString());
            ps.setString(10, checksum); ps.setInt(11, chunks.size()); return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new BusinessException("知识文档创建失败");
        long documentId = key.longValue();
        insertChunks(documentId, chunks);
        return documentId;
    }

    private void insertChunks(long documentId, List<KnowledgeChunker.Chunk> chunks) {
        for (KnowledgeChunker.Chunk chunk : chunks) {
            jdbcTemplate.update("""
                    INSERT INTO knowledge_chunk(document_id,chunk_no,section_title,content,checksum,status)
                    VALUES(?,?,?,?,?,'DRAFT')
                    """, documentId, chunk.number(), chunk.section(), chunk.content(), sha256(chunk.content().getBytes(StandardCharsets.UTF_8)));
        }
    }

    /** text-embedding-v4 accepts at most 10 texts per request. */
    private void addDocumentsBatched(VectorStore vectorStore, List<Document> documents) {
        for (int start = 0; start < documents.size(); start += 10) {
            vectorStore.add(documents.subList(start, Math.min(documents.size(), start + 10)));
        }
    }

    private Map<String, Object> document(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM knowledge_document WHERE id=? AND deleted=0", id);
        if (rows.isEmpty()) throw new BusinessException("知识文档不存在");
        return rows.getFirst();
    }

    private String firstHeading(String text, String fallback) {
        for (String line : text.split("\n")) if (line.startsWith("# ")) return line.substring(2).trim();
        return fallback;
    }
    private String metadata(String text, String prefix, String fallback) {
        for (String line : text.split("\n")) if (line.startsWith(prefix)) return line.substring(prefix.length()).trim();
        return fallback;
    }
    private String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String abbreviate(String text, int max) {
        if (text == null) return "未知错误"; return text.substring(0, Math.min(text.length(), max));
    }

    private String normalizeAgentType(String agentType) {
        return "APPLICATION".equalsIgnoreCase(agentType) ? "APPLICATION" : "HEALTH";
    }
}
