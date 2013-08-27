package com.bazoud.elasticsearch.river.git;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverIndexName;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.elasticsearch.threadpool.ThreadPool;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Optional;

import static com.bazoud.elasticsearch.river.git.guava.Maps.transformKeys;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Olivier Bazoud
 */
public class GitRiver extends AbstractRiverComponent implements River {
    private Client client;
    private volatile Thread indexerThread;
    private volatile boolean closed;
    @Inject
    private FunctionFlowFactory functionFlowFactory;

    private Context context = new Context();

    @Inject
    protected GitRiver(RiverName riverName, RiverSettings settings, @RiverIndexName String riverIndexName, Client client, ThreadPool threadPool) throws InvocationTargetException, IllegalAccessException {
        super(riverName, settings);
        this.client = client;
        logger.info("Creating Git river");

        if (settings.settings().containsKey("git")) {
            Map<String, Object> gitSettings = (Map<String, Object>) settings.settings().get("git");
            BeanUtilsBean2.getInstance().populate(context, transformKeys(gitSettings, new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, input);
                }
            }));
        }
        context.setRiverName(riverName.name());
        context.setRiverIndexName(riverIndexName);
        context.setClient(client);
        context.setIssuePattern(compilePattern(context.getIssueRegex()));
    }

    @Override
    public void start() {
        logger.info("Starting Git River: name '{}', uri '{}', updateRate '{}', indexing to '{}'/'{}'",
            context.getName(), context.getUri(), context.getUpdateRate(), context.getRiverName(), context.getType());

        try {
            client.admin().cluster().prepareHealth()
                .setWaitForYellowStatus()
                .execute().actionGet();

            client.admin().indices().prepareCreate(context.getRiverName()).execute().actionGet();
            logger.info("Index '{}'/'{}' created", context.getRiverName(), context.getType());

        } catch (Exception e) {
            if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
                // that's fine
            } else if (ExceptionsHelper.unwrapCause(e) instanceof ClusterBlockException) {
                // ok, not recovered yet..., lets start indexing and hope we recover by the first bulk
            } else {
                logger.warn("failed to create index '{}', disabling river...", e, context.getRiverName());
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

    private Optional<Pattern> compilePattern(String regex) {
        Optional<Pattern> issuePattern = Optional.absent();
        try {
            issuePattern = Optional.of(Pattern.compile(regex));
        } catch (Throwable e) {
            logger.warn("Git river exception occurred when parsing regex |{}|", issuePattern, e);
        }
        return issuePattern;
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
                    functionFlowFactory
                        .create()
                        .apply(context);

                } catch (Exception e) {
                    logger.warn("Git river exception occurred", e);
                }

                try {
                    logger.debug("Git river is going to sleep for {} ms", context.getUpdateRate());
                    Thread.sleep(context.getUpdateRate());
                } catch (InterruptedException e) {
                    // we shamefully swallow the interrupted exception
                    logger.warn("Git river interrupted");
                }
            }
        }

    }
}
