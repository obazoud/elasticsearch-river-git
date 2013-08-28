package com.bazoud.elasticsearch.river.git.flow;

import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.flow.functions.ObjectToJsonFunction;
import com.bazoud.elasticsearch.river.git.flow.functions.TagToIndexTag;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import static com.bazoud.elasticsearch.river.git.es.Bulk.execute;

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
                .transform(new ObjectToJsonFunction(bulk, context.getRiverName(), TYPE_TAG))
                .toList();

            execute(bulk);

        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }

        return context;
    }

}