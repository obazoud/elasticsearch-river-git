package com.bazoud.elasticsearch.river.git.flow;

import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.flow.functions.ObjectToJsonFunction;
import com.bazoud.elasticsearch.river.git.flow.functions.RefToRevCommit;
import com.bazoud.elasticsearch.river.git.flow.functions.RevCommitToIndexCommit;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import static com.bazoud.elasticsearch.river.git.es.Bulk.execute;

/**
 * @author Olivier Bazoud
 */
public class CommitIndexFunction implements Function<Context, Context> {
    public static final String TYPE_COMMIT = "commit";
    private static ESLogger logger = Loggers.getLogger(CommitIndexFunction.class);

    @Override
    public Context apply(final Context context) {
        try {
            final RevWalk walk = new RevWalk(context.getRepository());

            walk.markStart(
                FluentIterable
                    .from(context.getRefs())
                    .transform(new RefToRevCommit(walk))
                    .toList()
            );

            final BulkRequestBuilder bulk = context.getClient().prepareBulk();
            FluentIterable
                .from(walk)
                .transform(new RevCommitToIndexCommit(context, walk))
                .transform(new ObjectToJsonFunction(bulk, context.getRiverName(), TYPE_COMMIT))
                .toList();

            execute(bulk);

        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }

        return context;
    }

}
