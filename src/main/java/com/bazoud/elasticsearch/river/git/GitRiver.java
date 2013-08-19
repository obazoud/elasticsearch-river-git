package com.bazoud.elasticsearch.river.git;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverIndexName;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.elasticsearch.threadpool.ThreadPool;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.guava.FunctionFlow;
import com.bazoud.elasticsearch.river.git.guava.PredicateFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.CloneRepositoryFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.CommitIndexFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.FetchRepositoryFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.InitializeFunction;
import com.bazoud.elasticsearch.river.git.guava.predicates.FetchOrCloneRepositoryPredicate;
import com.google.common.base.Optional;

/**
 * @author Olivier Bazoud
 */
public class GitRiver extends AbstractRiverComponent implements River {
    private ThreadPool threadPool;
    private Client client;

    private volatile Thread indexerThread;
    private volatile boolean closed;

    private String name;
    private String uri;
    private int updateRate;
    private String indexName;
    private String typeName;
    private String workingDir;
    private String issueRegex;
    private boolean indexingDiff;

    public final static String INDEX_NAME = "git";
    public final static String TYPE_NAME = "git";

    @Inject
    protected GitRiver(RiverName riverName, RiverSettings settings, @RiverIndexName String riverIndexName, Client client, ThreadPool threadPool) {
        super(riverName, settings);
        this.client = client;
        this.threadPool = threadPool;

        logger.info("Creating Git river");

        if (settings.settings().containsKey("git")) {
            Map<String, Object> gitSettings = (Map<String, Object>) settings.settings().get("git");
            this.name = XContentMapValues.nodeStringValue(gitSettings.get("name"), null);
            this.uri = XContentMapValues.nodeStringValue(gitSettings.get("uri"), null);
            this.updateRate = XContentMapValues.nodeIntegerValue(gitSettings.get("update_rate"), 15 * 60 * 1000);
            this.typeName = XContentMapValues.nodeStringValue(gitSettings.get("type"), TYPE_NAME);
            this.workingDir = XContentMapValues.nodeStringValue(gitSettings.get("working_dir"), System.getProperty("user.home") + File.separator + ".elasticsearch-river-git");
            this.issueRegex = XContentMapValues.nodeStringValue(gitSettings.get("issue_regex"), null);
            this.indexingDiff = XContentMapValues.nodeBooleanValue(gitSettings.get("indexing_diff"), false);
            this.indexName = riverName.name();
        }
    }

    @Override
    public void start() {
        logger.info("Starting Git River: name '{}', uri '{}', updateRate '{}', indexing to '{}'/'{}'",
            this.name, this.uri, this.updateRate, this.indexName, typeName);

        try {
            client.admin().cluster().prepareHealth()
                .setWaitForYellowStatus()
                .execute().actionGet();

            client.admin().indices().prepareCreate(indexName).execute().actionGet();
            logger.info("Index '{}'/'{}' created", this.indexName, typeName);

        } catch (Exception e) {
            if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
                // that's fine
            } else if (ExceptionsHelper.unwrapCause(e) instanceof ClusterBlockException) {
                // ok, not recovered yet..., lets start indexing and hope we recover by the first bulk
            } else {
                logger.warn("failed to create index '{}', disabling river...", e, indexName);
                return;
            }
        }

        indexerThread = EsExecutors.daemonThreadFactory(settings.globalSettings(), "git_river_indexer").newThread(new GitIndexer());
        indexerThread.start();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        logger.info("Stopping Git River");
        indexerThread.interrupt();
        closed = true;
    }

    /**
     * @author Olivier Bazoud
     */
    public class GitIndexer implements Runnable {

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            while (true) {
                if (closed) {
                    return;
                }

                try {
                    Context context = Context.context()
                        .name(name)
                        .uri(uri)
                        .workingDir(workingDir)
                        .client(client)
                        .indexName(indexName)
                        .issuePattern(compilePattern(issueRegex))
                        .indexingDiff(indexingDiff)
                        .build();

                    FunctionFlow.<Context>flow()
                        .add(new InitializeFunction())
                        .add(
                            new PredicateFunction<Context, Context>(
                                new FetchRepositoryFunction(),
                                new CloneRepositoryFunction(),
                                new FetchOrCloneRepositoryPredicate()
                            )
                        )
                        .add(new CommitIndexFunction())
                        .build()
                        .apply(context);

                } catch (Exception e) {
                    logger.warn("Git river exception occurred", e);
                }

                try {
                    logger.debug("Git river is going to sleep for {} ms", updateRate);
                    Thread.sleep(updateRate);
                } catch (InterruptedException e) {
                    // we shamefully swallow the interrupted exception
                    logger.warn("Git river interrupted");
                }
            }
        }

        private Optional<Pattern> compilePattern(String regex) {
            Optional<Pattern> issuePattern = Optional.absent();
            try {
                issuePattern = Optional.of(Pattern.compile(regex));
            } catch (Throwable e) {
                logger.warn("Git river exception occurred when parsing regex |{}|", issuePattern, e);
            }
            return issuePattern;
        }

    }
}
