package com.medcare.hda.agent.config;

import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Keeps knowledge and personal memories in independent Chroma collections. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "chroma", matchIfMissing = true)
public class ChromaStoresConfig {

    @Bean
    @Primary
    public ChromaVectorStore knowledgeVectorStore(
            ChromaApi chromaApi,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.chroma.tenant-name:default_tenant}") String tenant,
            @Value("${spring.ai.vectorstore.chroma.database-name:default_database}") String database,
            @Value("${hda.agent.rag.knowledge-collection:${spring.ai.vectorstore.chroma.collection-name:medical_knowledge}}") String collection,
            @Value("${spring.ai.vectorstore.chroma.initialize-schema:true}") boolean initializeSchema) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName(tenant)
                .databaseName(database)
                .collectionName(collection)
                .initializeSchema(initializeSchema)
                .build();
    }

    @Bean
    public ChromaVectorStore longTermMemoryVectorStore(
            ChromaApi chromaApi,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.chroma.tenant-name:default_tenant}") String tenant,
            @Value("${spring.ai.vectorstore.chroma.database-name:default_database}") String database,
            @Value("${hda.agent.memory.collection-name:long_term_memory}") String collection,
            @Value("${spring.ai.vectorstore.chroma.initialize-schema:true}") boolean initializeSchema) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName(tenant)
                .databaseName(database)
                .collectionName(collection)
                .initializeSchema(initializeSchema)
                .build();
    }
}
