package com.aidemo.rag.service;

import com.aidemo.rag.config.RagProperties;
import com.aidemo.rag.dto.RagIngestRequest;
import com.aidemo.rag.dto.RagIngestResponse;
import com.aidemo.rag.repository.InMemoryKnowledgeChunkRepository;
import com.aidemo.rag.repository.InMemoryKnowledgeDocumentRepository;
import com.aidemo.rag.security.BasicRagContentSanitizer;
import com.aidemo.rag.vector.InMemoryVectorStore;
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

class DefaultRagIngestPdfServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void ingestPdfDirectoryAndKeepsPageSource() throws IOException {
        Path pdf = tempDir.resolve("redis-guide.pdf");
        writePdf(pdf, "Redis persistence includes RDB and AOF.");
        DefaultRagIngestService service = service();
        RagIngestRequest request = new RagIngestRequest();
        request.setSourceType("pdf");
        request.setSourcePath(tempDir.toString());
        request.setTags(List.of("redis", "pdf"));

        RagIngestResponse response = service.ingest(request);

        assertThat(response.getDocumentCount()).isEqualTo(1);
        assertThat(response.getChunkCount()).isGreaterThan(0);
        assertThat(response.getDiagnostics()).isNotEmpty();
        assertThat(response.getDiagnostics().get(0)).containsEntry("parser", "pdfbox");
        assertThat(response.getChunks()).isNotEmpty();
        assertThat(response.getChunks().get(0).getSource().getSourceUri()).contains("redis-guide.pdf");
        assertThat(response.getChunks().get(0).getSource().getPageStart()).isEqualTo(1);
        assertThat(response.getChunks().get(0).getSource().getPageEnd()).isEqualTo(1);
        assertThat(response.getChunks().get(0).getMetadata()).containsEntry("pageStart", 1);
    }

    private DefaultRagIngestService service() {
        RagProperties properties = new RagProperties();
        properties.setChunkSize(300);
        properties.setChunkOverlap(20);
        BasicRagContentSanitizer sanitizer = new BasicRagContentSanitizer();
        PdfDocumentParser parser = new PdfDocumentParser(sanitizer);
        MarkdownDocumentChunker chunker = new MarkdownDocumentChunker(properties);
        LocalHashEmbeddingService embeddingService = new LocalHashEmbeddingService(properties, new RagTextTokenizer());
        return new DefaultRagIngestService(
                List.of(parser),
                chunker,
                new InMemoryKnowledgeDocumentRepository(),
                new InMemoryKnowledgeChunkRepository(),
                embeddingService,
                new InMemoryVectorStore(),
                properties
        );
    }

    private void writePdf(Path path, String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(path.toFile());
        }
    }
}
