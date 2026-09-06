package com.meetchat.ai.rag;

import com.meetchat.ai.embedding.EmbeddingResult;
import com.meetchat.ai.embedding.EmbeddingService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.IndexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * Milvus向量存储实现
 * 使用Milvus V1 Java SDK (milvus-sdk-java 2.4.6)
 */
@Component("milvusVectorStore")
public class MilvusVectorStore implements VectorStore {

    private static final Logger logger = LoggerFactory.getLogger(MilvusVectorStore.class);

    /** 集合名称 */
    private static final String COLLECTION_NAME = "rag_documents";
    /** 向量维度（通义千问text-embedding-v2） */
    private static final int VECTOR_DIM = 1536;
    /** 主键字段名 */
    private static final String ID_FIELD = "id";
    /** 向量字段名 */
    private static final String VECTOR_FIELD = "vector";
    /** 标题字段名 */
    private static final String TITLE_FIELD = "title";
    /** 内容字段名 */
    private static final String CONTENT_FIELD = "content";
    /** 内容最大长度（超过则截断） */
    private static final int MAX_CONTENT_LENGTH = 2000;

    @Resource
    private EmbeddingService embeddingService;

    @Resource(name = "embeddingExecutor")
    private EmbeddingExecutor embeddingExecutor;

    private MilvusServiceClient milvusClient;

    @Value("${rag.milvus.host:localhost}")
    private String host;

    @Value("${rag.milvus.port:19530}")
    private int port;

    @Value("${rag.milvus.collection-name:rag_documents}")
    private String collectionName;

    @PostConstruct
    public void init() {
        try {
            // 连接Milvus (V1 API)
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            milvusClient = new MilvusServiceClient(connectParam);
            logger.info("Milvus连接成功: {}:{}", host, port);

            // 创建集合（如果不存在）
            createCollectionIfNotExists();

        } catch (Exception e) {
            logger.warn("Milvus初始化失败，RAG向量检索将不可用: {}", e.getMessage());
            milvusClient = null;
        }
    }

    /**
     * 创建集合和索引
     */
    private void createCollectionIfNotExists() {
        try {
            // 检查集合是否存在
            R<Boolean> hasResp = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());

            if (hasResp.getData() == Boolean.TRUE) {
                logger.info("集合已存在: {}", collectionName);
                return;
            }

            // 创建集合Schema (V1 API: FieldType + CollectionSchemaParam)
            FieldType idField = FieldType.newBuilder()
                    .withName(ID_FIELD)
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName(VECTOR_FIELD)
                    .withDataType(DataType.FloatVector)
                    .withDimension(VECTOR_DIM)
                    .build();

            FieldType titleField = FieldType.newBuilder()
                    .withName(TITLE_FIELD)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(500)
                    .build();

            FieldType contentField = FieldType.newBuilder()
                    .withName(CONTENT_FIELD)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(4000)
                    .build();

            List<FieldType> fields = Arrays.asList(idField, vectorField, titleField, contentField);

            CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                    .withFieldTypes(fields)
                    .build();

            R<RpcStatus> createResp = milvusClient.createCollection(
                    CreateCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withSchema(schema)
                            .build());

            if (createResp.getStatus() != R.Status.Success.getCode()) {
                logger.error("创建集合失败: {}", createResp.getMessage());
                return;
            }
            logger.info("集合创建成功: {}", collectionName);

            // 创建向量索引（HNSW）
            R<RpcStatus> indexResp = milvusClient.createIndex(
                    CreateIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFieldName(VECTOR_FIELD)
                            .withIndexType(IndexType.HNSW)
                            .withMetricType(MetricType.COSINE)
                            .withExtraParam("{\"M\":16,\"efConstruction\":256}")
                            .build());

            if (indexResp.getStatus() != R.Status.Success.getCode()) {
                logger.error("创建索引失败: {}", indexResp.getMessage());
            } else {
                logger.info("索引创建成功: HNSW, M=16, efConstruction=256");
            }

        } catch (Exception e) {
            logger.error("创建集合或索引失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查R响应是否成功
     */
    private <T> boolean isSuccess(R<T> resp) {
        return resp != null && resp.getStatus() != null && resp.getStatus() == R.Status.Success.getCode();
    }

    @Override
    public void addDocument(String docId, String title, String content) {
        if (milvusClient == null) {
            logger.warn("Milvus未初始化，无法添加文档: docId={}", docId);
            return;
        }

        try {
            // 同步向量化
            EmbeddingResult embedding = embeddingService.embed(content);
            if (embedding == null || embedding.getVector() == null) {
                logger.warn("Embedding生成失败，跳过Milvus入库: docId={}", docId);
                return;
            }

            doInsert(docId, title, content, embedding.getVector());
            logger.debug("文档添加成功: docId={}, title={}", docId, title);

        } catch (Exception e) {
            logger.error("添加文档失败: docId={}", docId, e);
        }
    }

    /**
     * 批量添加文档（异步向量化，不阻塞主流程）
     * 使用批量Embedding API，大幅提升入库速度
     */
    public void addDocumentsBatch(List<String> docIds, List<String> titles, List<String> contents) {
        if (milvusClient == null || docIds == null || docIds.isEmpty()) {
            logger.warn("Milvus未初始化或文档列表为空，跳过批量入库");
            return;
        }

        // 提交到异步线程池，立即返回，不阻塞调用方
        embeddingExecutor.submit(() -> {
            try {
                long startTime = System.currentTimeMillis();
                logger.info("开始异步批量向量化入库: count={}, queue={}",
                        docIds.size(), embeddingExecutor.getQueueSize());

                // 1. 准备批量Embedding输入（截断过长内容）
                List<String> truncatedContents = new ArrayList<>(contents.size());
                for (String content : contents) {
                    if (content == null) {
                        truncatedContents.add("");
                    } else if (content.length() > MAX_CONTENT_LENGTH) {
                        truncatedContents.add(content.substring(0, MAX_CONTENT_LENGTH));
                    } else {
                        truncatedContents.add(content);
                    }
                }

                // 2. 批量调用Embedding API（一次API调用处理多条）
                List<EmbeddingResult> embeddings = embeddingService.embedBatch(truncatedContents);
                logger.info("批量Embedding完成: total={}, success={}",
                        truncatedContents.size(),
                        embeddings != null ? embeddings.stream().filter(e -> e != null).count() : 0);

                // 3. 收集成功的结果
                List<Long> ids = new ArrayList<>();
                List<float[]> vectors = new ArrayList<>();
                List<String> titlesList = new ArrayList<>();
                List<String> contentsList = new ArrayList<>();

                for (int i = 0; i < embeddings.size() && i < docIds.size(); i++) {
                    EmbeddingResult embedding = embeddings.get(i);
                    if (embedding != null && embedding.getVector() != null) {
                        try {
                            ids.add(Long.parseLong(docIds.get(i)));
                            vectors.add(embedding.getVector());
                            titlesList.add(titles.get(i));
                            contentsList.add(truncatedContents.get(i));
                        } catch (NumberFormatException e) {
                            logger.warn("docId格式错误，跳过: docId={}", docIds.get(i));
                        }
                    } else {
                        logger.warn("Embedding失败，跳过docId={}", docIds.get(i));
                    }
                }

                if (ids.isEmpty()) {
                    logger.warn("批量添加文档：所有Embedding失败");
                    return;
                }

                // 4. 批量插入Milvus
                doBatchInsert(ids, titlesList, contentsList, vectors);
                long cost = System.currentTimeMillis() - startTime;
                logger.info("异步批量文档添加成功: count={}, cost={}ms", ids.size(), cost);

            } catch (Exception e) {
                logger.error("异步批量添加文档失败", e);
            }
        });
    }

    /**
     * 执行批量插入 (V1 API)
     */
    private void doBatchInsert(List<Long> ids, List<String> titles, List<String> contents, List<float[]> vectors) {
        try {
            // 将float[]转换为List<Float>
            List<List<Float>> vectorList = new ArrayList<>(vectors.size());
            for (float[] vec : vectors) {
                List<Float> floatList = new ArrayList<>(vec.length);
                for (float v : vec) {
                    floatList.add(v);
                }
                vectorList.add(floatList);
            }

            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field(ID_FIELD, ids));
            fields.add(new InsertParam.Field(VECTOR_FIELD, vectorList));
            fields.add(new InsertParam.Field(TITLE_FIELD, titles));
            fields.add(new InsertParam.Field(CONTENT_FIELD, contents));

            InsertParam param = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            R<io.milvus.grpc.MutationResult> resp = milvusClient.insert(param);
            if (!isSuccess(resp)) {
                throw new RuntimeException("Milvus批量插入失败: " + resp.getMessage());
            }
            logger.info("Milvus批量插入成功: count={}, collection={}", ids.size(), collectionName);
        } catch (Exception e) {
            logger.error("Milvus批量插入失败", e);
            throw e;
        }
    }

    /**
     * 执行单条插入 (V1 API)
     */
    private void doInsert(String docId, String title, String content, float[] vector) {
        // 将float[]转换为List<Float>
        List<Float> floatList = new ArrayList<>(vector.length);
        for (float v : vector) {
            floatList.add(v);
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field(ID_FIELD, Collections.singletonList(Long.parseLong(docId))));
        fields.add(new InsertParam.Field(VECTOR_FIELD, Collections.singletonList(floatList)));
        fields.add(new InsertParam.Field(TITLE_FIELD, Collections.singletonList(title)));
        fields.add(new InsertParam.Field(CONTENT_FIELD, Collections.singletonList(content)));

        InsertParam param = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<io.milvus.grpc.MutationResult> resp = milvusClient.insert(param);
        if (!isSuccess(resp)) {
            throw new RuntimeException("Milvus单条插入失败: " + resp.getMessage());
        }
    }

    @Override
    public void deleteDocument(String docId) {
        if (milvusClient == null) {
            return;
        }

        try {
            R<io.milvus.grpc.MutationResult> resp = milvusClient.delete(
                    DeleteParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withExpr(String.format("%s == %s", ID_FIELD, docId))
                            .build());

            if (!isSuccess(resp)) {
                logger.warn("删除文档可能失败: docId={}, msg={}", docId, resp.getMessage());
            } else {
                logger.debug("文档删除成功: docId={}", docId);
            }
        } catch (Exception e) {
            logger.error("删除文档失败: docId={}", docId, e);
        }
    }

    @Override
    public List<RetrievedChunk> search(float[] queryVector, int topK) {
        if (milvusClient == null || queryVector == null) {
            return Collections.emptyList();
        }

        try {
            // V1 SearchParam 接受 List<List<Float>>
            List<Float> floatList = new ArrayList<>(queryVector.length);
            for (float v : queryVector) {
                floatList.add(v);
            }

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectorFieldName(VECTOR_FIELD)
                    .withVectors(Collections.singletonList(floatList))
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withOutFields(Arrays.asList(ID_FIELD, TITLE_FIELD, CONTENT_FIELD))
                    .build();

            R<SearchResults> resp = milvusClient.search(searchParam);
            if (!isSuccess(resp) || resp.getData() == null) {
                logger.warn("向量检索失败: {}", resp.getMessage());
                return Collections.emptyList();
            }

            SearchResults results = resp.getData();
            io.milvus.grpc.SearchResultData resultData = results.getResults();

            // 从结果中提取ID列表
            List<Long> idList = resultData.getIds().getIntId().getDataList();

            // 从fieldsData中提取title和content
            Map<String, List<?>> fieldDataMap = new HashMap<>();
            for (io.milvus.grpc.FieldData fieldData : resultData.getFieldsDataList()) {
                String fieldName = fieldData.getFieldName();
                if (fieldData.hasScalars()) {
                    io.milvus.grpc.ScalarField scalars = fieldData.getScalars();
                    if (scalars.hasStringData()) {
                        fieldDataMap.put(fieldName, scalars.getStringData().getDataList());
                    } else if (scalars.hasLongData()) {
                        fieldDataMap.put(fieldName, scalars.getLongData().getDataList());
                    }
                }
            }

            List<String> titleList = (List<String>) fieldDataMap.getOrDefault(TITLE_FIELD, Collections.emptyList());
            List<String> contentList = (List<String>) fieldDataMap.getOrDefault(CONTENT_FIELD, Collections.emptyList());

            // 组装结果
            List<RetrievedChunk> chunks = new ArrayList<>();
            int count = Math.min(idList.size(), Math.min(titleList.size(), contentList.size()));
            for (int i = 0; i < count; i++) {
                long id = idList.get(i);
                String docTitle = titleList.get(i);
                String docContent = contentList.get(i);
                float score = resultData.getScores(i);

                chunks.add(new RetrievedChunk(docContent, String.valueOf(id), docTitle, score));
            }

            return chunks;

        } catch (Exception e) {
            logger.error("向量检索失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable() {
        return milvusClient != null;
    }
}
