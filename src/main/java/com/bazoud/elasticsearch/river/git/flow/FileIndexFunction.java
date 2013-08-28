package com.bazoud.elasticsearch.river.git.flow;

import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexFile;
import com.bazoud.elasticsearch.river.git.flow.functions.RefToRevCommit;
import com.bazoud.elasticsearch.river.git.flow.visitors.BulkVisitor;
import com.bazoud.elasticsearch.river.git.flow.visitors.IndexFileVisitor;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.bazoud.elasticsearch.river.git.guava.Visitors;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import static com.bazoud.elasticsearch.river.git.IndexedDocumentType.FILE;

/**
 * @author Olivier Bazoud
 */
public class FileIndexFunction extends MyFunction<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(FileIndexFunction.class);

    @Override
    public Context doApply(Context context) throws Throwable {
        RevWalk walk = new RevWalk(context.getRepository());

        walk.markStart(
            FluentIterable
                .from(context.getRefs())
                .transform(new RefToRevCommit(walk))
                .toList()
        );

        ImmutableList.Builder builder = ImmutableList.<IndexFile>builder();
        Visitors.visit(
            walk,
            new IndexFileVisitor(context, builder)
        );

        ImmutableList<IndexFile> files = builder.build();
        logger.info("Found {} index file", files.size());

        Visitors.visit(
            files,
            new BulkVisitor(context, FILE.name().toLowerCase())
        );

        return context;
    }
}
