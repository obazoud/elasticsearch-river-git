package com.bazoud.elasticsearch.river.git.guava.flow;

import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.IndexCommit;
import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.guava.functions.RefToRevCommit;
import com.bazoud.elasticsearch.river.git.guava.functions.RevCommitToIndexCommit;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import static com.bazoud.elasticsearch.river.git.es.Bulk.execute;
import static com.bazoud.elasticsearch.river.git.json.Json.toJson;
import static org.elasticsearch.client.Requests.indexRequest;

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
                .transform(new Function<IndexCommit, IndexCommit>() {
                    @Override
                    public IndexCommit apply(IndexCommit commit) {
                        try {
                            bulk.add(indexRequest(context.getRiverName())
                                .type(TYPE_COMMIT)
                                .id(commit.getId())
                                .source(toJson(commit)));
                            return commit;
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

}
