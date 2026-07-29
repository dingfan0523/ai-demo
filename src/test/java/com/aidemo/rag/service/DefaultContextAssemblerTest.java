package com.aidemo.rag.service;

import com.aidemo.rag.dto.RagChunkHit;
import com.aidemo.rag.dto.RagSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultContextAssemblerTest {

    @Test
    void assembleAddsStableSourceIdAndSourceMetadata() {
        DefaultContextAssembler assembler = new DefaultContextAssembler();
        RagChunkHit hit = hit("chunk-1", "Redis 指南", "缓存穿透可以使用布隆过滤器缓解。");

        String context = assembler.assemble(List.of(hit), 2000);

        assertThat(context).contains("[source:chunk-1]");
        assertThat(context).contains("标题: Redis 指南");
        assertThat(context).contains("来源: redis.md");
        assertThat(context).contains("行号: 3-8");
        assertThat(context).contains("缓存穿透可以使用布隆过滤器缓解。");
    }

    private RagChunkHit hit(String chunkId, String title, String content) {
        RagSource source = new RagSource();
        source.setChunkId(chunkId);
        source.setDocumentId("doc-1");
        source.setTitle(title);
        source.setSourceUri("redis.md");
        source.setSectionTitle("缓存问题");
        source.setStartLine(3);
        source.setEndLine(8);

        RagChunkHit hit = new RagChunkHit();
        hit.setChunkId(chunkId);
        hit.setDocumentId("doc-1");
        hit.setTitle(title);
        hit.setContent(content);
        hit.setRerankScore(0.86d);
        hit.setSource(source);
        return hit;
    }
}
