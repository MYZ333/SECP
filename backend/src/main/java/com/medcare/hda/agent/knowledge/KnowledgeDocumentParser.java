package com.medcare.hda.agent.knowledge;

import com.medcare.hda.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class KnowledgeDocumentParser {

    public String parse(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            String text;
            if (name.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(path.toFile())) {
                    text = new PDFTextStripper().getText(document);
                }
            } else if (name.endsWith(".html") || name.endsWith(".htm")) {
                text = Jsoup.parse(path.toFile(), StandardCharsets.UTF_8.name()).text();
            } else if (name.endsWith(".txt") || name.endsWith(".md")) {
                text = Files.readString(path, StandardCharsets.UTF_8);
            } else {
                throw new BusinessException("仅支持 PDF、HTML、Markdown 和 TXT 文档");
            }
            String normalized = text.replace("\r\n", "\n")
                    .replaceAll("[\\t\\x0B\\f]+", " ")
                    .replaceAll("[ ]{2,}", " ")
                    .replaceAll("\n{3,}", "\n\n")
                    .trim();
            if (normalized.length() < 40) {
                throw new BusinessException("文档正文过短或解析失败");
            }
            return normalized;
        } catch (IOException e) {
            throw new BusinessException("文档解析失败：" + e.getMessage());
        }
    }
}
