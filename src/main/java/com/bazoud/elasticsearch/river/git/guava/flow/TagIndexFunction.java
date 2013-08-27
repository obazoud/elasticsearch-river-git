package com.bazoud.elasticsearch.river.git.guava.flow;

import java.util.Map;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexCommit;
import com.bazoud.elasticsearch.river.git.beans.IndexTag;
import com.bazoud.elasticsearch.river.git.guava.functions.TagToIndexTag;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import static com.bazoud.elasticsearch.river.git.json.Json.toJson;
import static org.elasticsearch.client.Requests.indexRequest;

/**
 * @author Olivier Bazoud
 */
public class TagIndexFunction implements Function<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(TagIndexFunction.class);
    public static final String TYPE_TAG = "tag";

    @Override
    public Context apply(final Context context) {
        try {
            final RevWalk walk = new RevWalk(context.getRepository());
            final BulkRequestBuilder bulk = context.getClient().prepareBulk();
            FluentIterable
                .from(context.getRepository().getTags().entrySet())
                .transform(new TagToIndexTag(context, walk))
                .transform(new Function<IndexTag, IndexTag>() {
                    @Override
                    public IndexTag apply(IndexTag tag) {
                        try {
                            bulk.add(indexRequest(context.getRiverName())
                                .type(TYPE_TAG)
                                .id(tag.getId())
                                .source(toJson(tag)));
                            return tag;
                        } catch (Throwable e) {
                            logger.error(this.getClass().getName(), e);
                            Throwables.propagate(e);
                            return null;
                        }
                    }
                })
                .toList();

            execute(bulk);

        } catch(Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }

        return context;
    }

    private void execute(BulkRequestBuilder bulk) {
        logger.info("Executing bulk {} actions", bulk.numberOfActions());
        if (bulk.numberOfActions() > 0) {
            BulkResponse response = bulk.execute().actionGet();
            logger.info("Bulk actions tooks {} ms", response.getTookInMillis());
            if (response.hasFailures()) {
                logger.warn("failed to execute bulk: {}", response.buildFailureMessage());
            }
        } else {
            logger.info("Sorry nothing to do");
        }
    }
}