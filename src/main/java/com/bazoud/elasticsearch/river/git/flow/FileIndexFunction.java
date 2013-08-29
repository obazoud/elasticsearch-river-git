package com.bazoud.elasticsearch.river.git.flow;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexFile;
import com.bazoud.elasticsearch.river.git.flow.functions.RefToRevCommit;
import com.bazoud.elasticsearch.river.git.flow.functions.RevCommitToIndexFile;
import com.bazoud.elasticsearch.river.git.flow.visitors.BulkVisitor;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.bazoud.elasticsearch.river.git.guava.Visitors;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;

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

        Iterator<List<IndexFile>> partitions = Iterators.partition(
            FluentIterable
                .from(walk)
                .transform(new RevCommitToIndexFile(context))
                .iterator(),
            100);

        while (partitions.hasNext()) {
            List<IndexFile> indexFiles = partitions.next();
            Visitors.visit(
                indexFiles,
                new BulkVisitor(context, FILE.name().toLowerCase())
            );
        }

        return context;
    }
}
