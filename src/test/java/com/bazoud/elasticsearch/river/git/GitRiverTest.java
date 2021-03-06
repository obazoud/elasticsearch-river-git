package com.bazoud.elasticsearch.river.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.github.tlrx.elasticsearch.test.annotations.ElasticsearchAdminClient;
import com.github.tlrx.elasticsearch.test.annotations.ElasticsearchClient;
import com.github.tlrx.elasticsearch.test.annotations.ElasticsearchIndex;
import com.github.tlrx.elasticsearch.test.annotations.ElasticsearchNode;
import com.github.tlrx.elasticsearch.test.support.junit.runners.ElasticsearchRunner;

import static com.bazoud.elasticsearch.plugin.river.git.GitRiverPlugin.RIVER_TYPE;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * @author Olivier Bazoud
 */
@RunWith(ElasticsearchRunner.class)
@ElasticsearchNode
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GitRiverTest {
    private static final String RIVER_KEYWORD = "_river";
    private String gitRepository;

    @ElasticsearchClient()
    Client client1;
    @ElasticsearchAdminClient
    AdminClient adminClient;

    @Before
    public void setUp() throws URISyntaxException {
        gitRepository = currentThread().getContextClassLoader()
            .getResource("git_tests_junit")
            .toURI()
            .toString();
    }

    @Test
    @ElasticsearchIndex(indexName = RIVER_KEYWORD)
    public void test00CreateIndex() {
        // Checks if the index has been created
        IndicesExistsResponse existResponse = adminClient.indices()
            .prepareExists(RIVER_KEYWORD)
            .execute().actionGet();

        Assert.assertTrue("Index must exist", existResponse.isExists());
    }

    @Test
    @ElasticsearchIndex(indexName = RIVER_KEYWORD)
    public void test10IndexingFromRevision() throws IOException {
        // Index a new document
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("name", "git_repo_tests");
        json.put("uri", gitRepository);
        json.put("working_dir", "./target/river-workingdir_" + UUID.randomUUID().toString());
        json.put("issue_regex", "#(\\d*)");
        json.put("indexing_diff", false);

        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field("type", RIVER_TYPE)
            .field("git", json)
            .endObject();

        IndexResponse indexResponse = client1.prepareIndex(RIVER_KEYWORD, "gitriver01", "_meta")
            .setSource(builder)
            .execute()
            .actionGet();

        Assert.assertTrue("Indexing must return a version >= 1",
            indexResponse.getVersion() >= 1);
    }

    @Test
    @ElasticsearchIndex(indexName = RIVER_KEYWORD)
    public void test20Searching() {
        String waitingIndex = System.getProperty("waitingIndex", "20000");
        System.out.println("Wait for the indexing to take place: " + waitingIndex);
        try {
            sleep(Long.parseLong(waitingIndex));
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }

        SearchResponse searchResponse = client1.prepareSearch("gitriver01")
            .setQuery(QueryBuilders.matchPhrasePrefixQuery("project", "git_repo_tests"))
            .execute()
            .actionGet();

        for (SearchHit hit : searchResponse.getHits()) {
            System.out.println("Search result index [" + hit.index()
                + "] type [" + hit.type()
                + "] id [" + hit.id() + "]"
            );
            System.out.println("Search result source:" + hit.sourceAsString());
        }

        Assert.assertTrue("There should be at least a 'git_repo_tests' mention in the repository",
            searchResponse.getHits().totalHits() > 0);

    }

}
