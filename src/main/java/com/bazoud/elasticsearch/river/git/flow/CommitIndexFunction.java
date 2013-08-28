package com.bazoud.elasticsearch.river.git.flow;

import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.flow.functions.RefToRevCommit;
import com.bazoud.elasticsearch.river.git.flow.functions.RevCommitToIndexCommit;
import com.bazoud.elasticsearch.river.git.flow.visitors.BulkVisitor;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.bazoud.elasticsearch.river.git.guava.Visitors;
import com.google.common.collect.FluentIterable;

import static com.bazoud.elasticsearch.river.git.IndexedDocumentType.COMMIT;


/**
 * @author Olivier Bazoud
 */
public class CommitIndexFunction extends MyFunction<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(CommitIndexFunction.class);

    @Override
    public Context doApply(Context context) throws Throwable {
        RevWalk walk = new RevWalk(context.getRepository());
        walk.markStart(
            FluentIterable
                .from(context.getRefs())
                .transform(new RefToRevCommit(walk))
                .toList()
        );

        Visitors.visit(
            FluentIterable
                .from(walk)
                .transform(new RevCommitToIndexCommit(context, walk)),
            new BulkVisitor(context, COMMIT.name().toLowerCase())
        );

        return context;
    }

}
