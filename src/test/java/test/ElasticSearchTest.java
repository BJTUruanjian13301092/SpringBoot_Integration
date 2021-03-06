package test;

import org.apache.lucene.queries.mlt.MoreLikeThisQuery;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.validate.query.*;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.lucene.search.FilteredCollector;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.*;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms.Bucket;
import org.junit.Test;

import javax.management.Query;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class ElasticSearchTest {

    public final static String HOST = "127.0.0.1";

    //http请求的端口是9200，客户端是9300
    public final static int PORT = 9300;

    TransportClient client;


    /**
     * 利用TransportClient连接集群，程序主入口
     * @throws Exception
     */
    @SuppressWarnings("resource")
    @Test
    public void connectToES() throws Exception {

        //TODO 注意 mapping属性 ignore_above 默认为256，超过256默认不进行分析，如果文本过大请重新设置为 32766 / 3 = 10922 因为UTF-8字符最多占3个字节

        Settings settings = Settings.builder().put("cluster.name", "es-yitongliu").build();
        client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(HOST), PORT));

        System.out.println(client.toString());

        //增删改查操作

        //createMapping();
        //alterMapping();
        //createIndex();
        //createIndexByJson();
        //addIndexValue();
        //getIndex();
        //updateIndex();
        //deleteIndexID();
        //deleteIndex();

        //设置查询条件
        /**
         * matchAllQuery : 查询所有
         */
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();

        /**
         * matPhraseQuery : 多字段匹配，要求字段先后顺序
         * 参数1 slop : 数字, 该参数控制词条相隔多远仍然能将文档视为匹配, 也就是为了让查询和文档匹配你需要移动词条多少次, 例如为了让quick fox匹配quick brown fox, 需要slop=1, 也就是挪动fox一次
         */
        QueryBuilder queryBuilder1 = QueryBuilders.matchPhraseQuery("sex", "male");

        /**
         * matchQuery : 多字段匹配
         * 参数1 Operator : AND, OR; 方法默认值为OR; AND让所有词项都必须匹配
         * 参数2 Minimum_should_match : 数字 百分比; 例如 75%, 意味着需要最少匹配查询词汇中的百分之75
         */
        QueryBuilder queryBuilder2 = QueryBuilders.matchQuery("sex", "male");

        /**
         * termQuery : 单字段匹配
         */
        QueryBuilder queryBuilder3 = QueryBuilders.termQuery("sex", "male");

        /**
         * prefixQuery : 前缀匹配
         */
        QueryBuilder queryBuilder4 = QueryBuilders.prefixQuery("sex", "fe");

        /**
         * wildcardQuery : 通配符匹配
         * 同理还有regexpQuery,处理更为复杂的正则表达式
         */
        QueryBuilder queryBuilder5 = QueryBuilders.wildcardQuery("sex", "*male");

        /**
         * rangeQuery : 范围查询
         */
        QueryBuilder queryBuilder6 = QueryBuilders.rangeQuery("age").from("22").to("23");

        /**
         * boolQuery : 组合查询
         * must : 必须包含该条件
         * must_not : 必须不包含该条件
         * should : 不必须包含该条件，但是包含了该条件的话评分结果会更高,当boolQuery没有must时，至少匹配一个should条件
         * 参数1 Minimum_should_match : 数字 百分比; 针对should而言, 至少满足多少条should语句, 例如 : 2
         * 参数2 Boost : 数字; 例如 3; 控制某一查询语句的相对权重, 默认值为1, 大于1会提升一个查询语句的相对权重; 注意, boost对于权重的提升不是线性的,会经过归一化处理, 0-15之间
         */
        BoolQueryBuilder queryBuilder7 = QueryBuilders.boolQuery();
        queryBuilder7.should(queryBuilder1);
        queryBuilder7.should(queryBuilder6);

        /**
         * fuzzyQuery : 模糊查询
         */
        QueryBuilder queryBuilder8 = QueryBuilders.fuzzyQuery("sex", "fe-male");

        /**
         * moreLikeThisQuery : 相似查询 //TODO
         */
        String[] fields = {"sex"};
        String[] texts = {"female"};
        QueryBuilder queryBuilder9 = QueryBuilders.moreLikeThisQuery(fields, texts, null).minTermFreq(1).maxQueryTerms(12).minDocFreq(1);

        /**
         * constantScoreQuery : 将打分设成常量查询, 取消TF-IDF计算, 提升效率
         */
        ConstantScoreQueryBuilder queryBuilder10 = QueryBuilders.constantScoreQuery(queryBuilder1);

        /**
         * disMaxQuery : 分离最大化查询 只将最佳匹配的评分作为查询的评分结果返回、
         * 参数1 tie_breaker : 该参数将其他匹配语句的评分也考虑进来, 将其他匹配语句的评分结果与tie_breaker相乘, 0-1之间的浮点数，0代表dis_max的普通逻辑, 1代表所有匹配同等重要, 合理值应该与0接近, 0.1-0.4
         */
        DisMaxQueryBuilder queryBuilder11 = QueryBuilders.disMaxQuery();
        queryBuilder11.add(queryBuilder3);
        queryBuilder11.add(queryBuilder6);

        /**
         * boostingQuery : 权重提升查询, 可以保持positive的查询条件, 给negative的查询条件降低权重, 从而达到理想的排序效果
         * 参数1 negative_boost : 浮点型数字; 该参数可以设置negative查询条件的权重值 范围0-1
         * 注意 : boostingQuery(Positive, Negative) positive的查询条件为第一个参数, negative为第二个参数
         */
        BoostingQueryBuilder queryBuilder12 = QueryBuilders.boostingQuery(queryBuilder5, queryBuilder4).negativeBoost(0.5f);

        /**
         * functionScoreQuery : function_score查询 为每个主查询匹配的文档应用一个函数, 以达到改变甚至完全替换原始查询的评分
         *
         * weight : 为每个文档应用一个简单而不被规范化的权重提升值: 当 weight 为 2 时，最终结果为 2 * _score
         * field_value_factor : 使用这个值来修改 _score , 如将 popularity 或 votes （受欢迎或赞）作为考虑因素。
         * random_score : 为每个用户都使用一个不同的随机评分对结果排序, 但对某一具体用户来说，看到的顺序始终是一致的。
         * linear_decay : 线性衰减 —— 将浮动值结合到评分 _score 中
         * exp_decay : 指数衰减 —— 将浮动值结合到评分 _score 中
         * gauss_decay : 高斯衰减 —— 将浮动值结合到评分 _score 中
         * script_score : 如果需求超出以上范围时, 用自定义脚本可以完全控制评分计算，实现所需逻辑。
         */
        ScoreFunctionBuilder scoreFunctionBuilder = ScoreFunctionBuilders.fieldValueFactorFunction("age").modifier(FieldValueFactorFunction.Modifier.LOG1P).factor(1);
        QueryBuilder queryBuilder13 = QueryBuilders.functionScoreQuery(queryBuilder2, scoreFunctionBuilder).boostMode(CombineFunction.MULTIPLY);

        FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                        QueryBuilders.matchQuery("sex", "male"),
                        ScoreFunctionBuilders.gaussDecayFunction("age",15L, 1L)),
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                        ScoreFunctionBuilders.weightFactorFunction(1.0f)
                )
        };

        /**
         * functionScoreQuery : 带FilterFunctionBuilder的多条件查询, 通过过滤集提升权重
         */
        QueryBuilder queryBuilder16 = QueryBuilders.functionScoreQuery(functions);

        /**
         * matchPhrasePrefixQuery : 即时性带前缀的matchPhrase查询, 将查询字符串的最后一个词汇作为前缀查询
         * 参数1 slop : 和matchPhrase查询一致
         * 参数2 max_expansions : 控制可以与前缀匹配的词的数量, 通常设置为50
         */
        QueryBuilder queryBuilder14 = QueryBuilders.matchPhrasePrefixQuery("sex", "male");

        /**
         * nestedQuery : 嵌套查询, 由于嵌套对象被索引在独立隐藏的文档中, 我们无法直接查询他们, 所以需要使用嵌套查询去获取他们
         * path : 嵌套对象名称
         * query : 相应的查询方法
         * scoremode : 使用的评分模式
         */
        QueryBuilder queryBuilder15 = QueryBuilders.nestedQuery("comment", QueryBuilders.termQuery("comments.name", "jack"), ScoreMode.None);

        //验证查询语句的正确性
        ValidateQueryResponse validateQueryResponse = new ValidateQueryRequestBuilder(client, ValidateQueryAction.INSTANCE)
                .setQuery(queryBuilder9).setExplain(true).get();

        System.out.println("查询结果反馈:");
        System.out.println("--------------------------------------------------------");
        for (QueryExplanation qe : validateQueryResponse.getQueryExplanation()) {
            System.out.println(String.format("索引:%s", qe.getIndex()));
            System.out.println(String.format("解释:%s", qe.getExplanation()));
            System.out.println(String.format("错误信息：%s", qe.getError()));
        }

        //设置聚合条件
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("aggSex").field("sex");
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("aggAge").field("age.keyword");
        aggregationBuilder.subAggregation(ageAgg);

        //设置排序条件
        SortBuilder sortBuilder = SortBuilders.fieldSort("age.keyword").order(SortOrder.ASC).unmappedType("long");

        //searchIndex(sortBuilder, aggregationBuilder, queryBuilder2, "school", "student");
    }

    /**
     * 创建索引
     * @throws IOException
     */
    public void createIndex() throws IOException{

    int[] a = {100, 200};
        for(int i=0;i<10;i++){

            IndexResponse response = client.prepareIndex("library", "book", String.valueOf(i))
                    .setSource(jsonBuilder().
                            startObject()
                            .field("book_name", "ElasticSearch入门")
                            .field("author", "Kobe")
                            .field("publish_time", "2017-09-09")
                            .field("describe", "这是一本非常好的书")
                            .field("price", a)
                            .endObject())
                    .get();
        }
    }

    /**
     * 通过Json创建索引(利用BulkRequestBuilder进行批量操作)
     * @throws IOException
     */
    public void createIndexByJson() throws Exception {

        BulkRequestBuilder bulkRequest = client.prepareBulk();

        String json1 = "{\"name\":\"BBC\", \"channel\":\"1\", \"context\":\"中国驻洛杉矶领事馆遭亚裔男子枪击 嫌犯已自首\"}";
        String json2 = "{\"name\":\"CCTV\", \"channel\":\"2\", \"context\":\"中韩渔警冲突调查：韩警平均每天扣1艘中国渔船\"}";
        List<String> listJson = new ArrayList<>();
        listJson.add(json1);
        listJson.add(json2);

        for(int j=0;j<listJson.size();j++){
            //requestBuilder.setSource(listJson.get(j)).execute().actionGet();
            String id = String.valueOf(j);
            bulkRequest.add(client.prepareIndex("testmapping", "testmapping", id).setSource(listJson.get(j))
            );
        }

        bulkRequest.execute().actionGet();

    }

    /**
     * 创建映射
     * @throws IOException
     */
    public void createMapping() throws Exception {
        //先创建索引
        CreateIndexRequest request = new CreateIndexRequest("library");
        client.admin().indices().create(request).actionGet();
        //创建mapping
        PutMappingRequest mapping = Requests.putMappingRequest("library").type("book")
                .source(getMapping("describe", "ik_max_word"));
        client.admin().indices().putMapping(mapping).actionGet();

    }

    /**
     * 更新映射
     * @throws IOException
     */
    public void alterMapping() throws Exception {
        //创建mapping
        PutMappingRequest mapping = Requests.putMappingRequest("library").type("book")
                .source(getMapping("describe", "true"));
        client.admin().indices().putMapping(mapping).actionGet();

    }

    /**
     * 通过Json添加索引内容(利用BulkRequestBuilder进行批量操作)
     * @throws IOException
     */
    public void addIndexValue() throws IOException{

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        String json1 = "{\"name\":\"frank\", \"sex\":\"unknown male female\", \"age\":\"90\"}";

        bulkRequest.add(client.prepareIndex("school", "student", "11").setSource(json1));
        bulkRequest.execute().actionGet();
    }

    /**
     * 创建索引的映射列表
     * @throws IOException
     */
    public XContentBuilder getMapping(String indexField, String var) throws IOException {
        XContentBuilder mapping = null;
        mapping = jsonBuilder()
                .startObject()
                .startObject("properties")
                .startObject(indexField)
                .field("type","text")
                .field("analyzer", var)
                .endObject()
                .endObject()
                .endObject();

        return mapping;
    }

    /**
     * 获取索引
     */
    public void getIndex(){

//        //Get一个
//        GetResponse getResponse = client.prepareGet("school", "student", "0").execute().actionGet();
//        System.out.println(getResponse.getSourceAsString());

        //Get多个
        MultiGetResponse multiGetResponse = client.prepareMultiGet()
                .add("school", "student", "0")
                .add("school", "student", "1").execute().actionGet();
        MultiGetItemResponse[] itemResponses = multiGetResponse.getResponses();
        for(MultiGetItemResponse item : itemResponses){

            System.out.println(item.getResponse().getSourceAsString());
        }

    }

    /**
     * 更新索引
     * @throws IOException
     */
    public void updateIndex() throws IOException{

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("library");
        updateRequest.type("book");
        updateRequest.id("1");
        updateRequest.doc(jsonBuilder().startObject().field("author", "James").field("publish_time", "2018-01-01").endObject());
        try {
            client.update(updateRequest).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        GetResponse getResponse = client.prepareGet("library", "book", "1").execute().actionGet();
        System.out.println(getResponse.getSourceAsString());
    }

    /**
     * 删除索引中的一个ID
     */
    public void deleteIndexID(){

        //删除一个
        //DeleteResponse response = client.prepareDelete("library", "book", "1").get();

        //批量删除,通过add添加
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        bulkRequest.add(client.prepareDelete("library", "book", "1"));
        bulkRequest.execute().actionGet();
    }

    /**
     * 删除索引
     */
    public void deleteIndex(){

        //删除索引
        DeleteIndexResponse dResponse = client.admin().indices().prepareDelete("library").execute().actionGet();

        //判断索引是否存在
        IndicesExistsRequest inExistsRequest = new IndicesExistsRequest("library");

        IndicesExistsResponse inExistsResponse = client.admin().indices().exists(inExistsRequest).actionGet();

        System.out.println(inExistsResponse.toString());
    }

    /**
     * 搜索方法
     */
    public void searchIndex(SortBuilder sortBuilder, TermsAggregationBuilder aggregationBuilder, QueryBuilder queryBuilder, String index, String type){

        SearchRequestBuilder searchRequest = client.prepareSearch(index).setTypes(type)
                .setQuery(queryBuilder)
                //.addAggregation(aggregationBuilder)
                //.addSort(sortBuilder)
                .setFrom(0)                 //分页技术，设置起始位置
                .setSize(100)               //设置（每一页）最大的显示数量，size默认是10
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        System.out.println("SearchRequest is: ");
        System.out.println(searchRequest.toString());
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        //Search区块
        SearchHits hits = searchResponse.getHits();
        System.out.println("查询到记录数 = " + hits.getTotalHits());
        SearchHit[] searchHists = hits.getHits();
        if(searchHists.length > 0){

            for(SearchHit hit : searchHists){

                String id = hit.getId();
                String sex = (String)hit.getSource().get("sex");
                String name = (String)hit.getSource().get("name");
                String age = (String)hit.getSource().get("age");
                float score = hit.getScore();
                System.out.println(hit.getSourceAsString());
                System.out.println("id = " + id + " name = " + name + " sex = " + sex + " age = " + age + " score = " + score);
            }
        }

        System.out.println("--------------------------------------------------------");

        //Aggregation区块
        Map<String, Aggregation> aggMap = searchResponse.getAggregations().asMap();
        System.out.println(searchResponse.toString());
        StringTerms gradeTerms = (StringTerms) aggMap.get("aggSex");
        Iterator<Bucket> gradeBucketIt = gradeTerms.getBuckets().iterator();
        while(gradeBucketIt.hasNext()){

            Bucket gradeBucket = gradeBucketIt.next();
            System.out.println(gradeBucket.getKey() + ": " + gradeBucket.getDocCount());

            //得到所有子聚合
            Map<String, Aggregation> subMap = gradeBucket.getAggregations().asMap();
            StringTerms ageTerms = (StringTerms) subMap.get("aggAge");
            Iterator<Bucket> ageBucketIt = ageTerms.getBuckets().iterator();

            System.out.print("Age : ");
            while(ageBucketIt.hasNext()){

                Bucket ageBucket = ageBucketIt.next();
                System.out.print(" " + ageBucket.getKeyAsString());
            }
            System.out.print("\n\n");
        }
    }

}