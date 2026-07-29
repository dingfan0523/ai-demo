package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.model.ParsedDocument;
import com.aidemo.rag.security.BasicRagContentSanitizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfDocumentParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parseTextPdfKeepsPageDiagnosticsAndContent() throws IOException {
        Path pdf = tempDir.resolve("redis-guide.pdf");
        writePdf(pdf, List.of("Redis cache penetration uses bloom filter.", "Redis hot key needs local cache."));
        PdfDocumentParser parser = new PdfDocumentParser(new BasicRagContentSanitizer());

        ParsedDocument parsed = parser.parse(request(pdf));

        assertThat(parsed.getDocument().getSourceType()).isEqualTo("pdf");
        assertThat(parsed.getContent()).contains("## 第 1 页");
        assertThat(parsed.getContent()).contains("Redis cache penetration uses bloom filter.");
        assertThat(parsed.getDiagnostics()).containsEntry("pageCount", 2);
        assertThat(parsed.getDiagnostics()).containsEntry("textPageCount", 2);
        assertThat(parsed.getDiagnostics()).containsKey("pageLineRanges");
        assertThat(parsed.getWarnings()).isEmpty();
    }

    @Test
    void parseBlankPdfReturnsScannedPdfWarning() throws IOException {
        Path pdf = tempDir.resolve("blank.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }
        PdfDocumentParser parser = new PdfDocumentParser(new BasicRagContentSanitizer());

        ParsedDocument parsed = parser.parse(request(pdf));

        assertThat(parsed.getWarnings())
                .anySatisfy(warning -> assertThat(warning).contains("可能是扫描件或图片型 PDF"));
        assertThat(parsed.getDiagnostics()).containsEntry("textPageCount", 0);
    }

    private RagIngestRequest request(Path pdf) {
        RagIngestRequest request = new RagIngestRequest();
        request.setSourceType("pdf");
        request.setSourcePath(pdf.toString());
        request.setTags(List.of("pdf-test"));
        return request;
    }

    private void writePdf(Path path, List<String> pageTexts) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(72, 720);
                    contentStream.showText(pageText);
                    contentStream.endText();
                }
            }
            document.save(path.toFile());
        }
    }
}
