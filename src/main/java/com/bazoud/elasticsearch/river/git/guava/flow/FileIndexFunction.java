package com.bazoud.elasticsearch.river.git.guava.flow;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexFile;
import com.bazoud.elasticsearch.river.git.guava.functions.RefToRevCommit;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import static com.bazoud.elasticsearch.river.git.es.Bulk.execute;
import static com.bazoud.elasticsearch.river.git.json.Json.toJson;
import static org.elasticsearch.client.Requests.indexRequest;

/**
 * @author Olivier Bazoud
 */
public class FileIndexFunction implements Function<Context, Context> {
    public static final String TYPE_FILE = "file";
    private static ESLogger logger = Loggers.getLogger(FileIndexFunction.class);

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

            final ImmutableList.Builder builder = ImmutableList.<IndexFile>builder();
            FluentIterable
                .from(walk)
                .transform(new Function<RevCommit, RevCommit>() {
                    @Override
                    public RevCommit apply(RevCommit revCommit) {
                        try {
                            RevTree revTree = revCommit.getTree();
                            TreeWalk treeWalk = new TreeWalk(context.getRepository());
                            treeWalk.addTree(revTree);
                            treeWalk.setRecursive(true);
                            while (treeWalk.next()) {
                                ObjectId objectId = treeWalk.getObjectId(0);
                                if (objectId != ObjectId.zeroId()) {
                                    builder.add(toIndexFile(context, treeWalk, revCommit, objectId));
                                }
                            }
                            return revCommit;
                        } catch (Throwable e) {
                            logger.error(this.getClass().getName(), e);
                            Throwables.propagate(e);
                            return revCommit;
                        }
                    }
                })
                .toList();
            ImmutableList<IndexFile> files = builder.build();
            logger.info("Found {} index file", files.size());

            final BulkRequestBuilder bulk = context.getClient().prepareBulk();

            FluentIterable
                .from(files)
                .transform(new Function<IndexFile, IndexFile>() {
                    @Override
                    public IndexFile apply(IndexFile indexFile) {
                        try {
                            bulk.add(indexRequest(context.getRiverName())
                                .type(TYPE_FILE)
                                .id(indexFile.getId())
                                .source(toJson(indexFile)));
                            return indexFile;
                        } catch (Throwable e) {
                            logger.error(this.getClass().getName(), e);
                            Throwables.propagate(e);
                            return indexFile;
                        }
                    }
                })
                .toList();

            execute(bulk);

        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }

        return context;
    }

    private IndexFile toIndexFile(Context context, TreeWalk treeWalk, RevCommit revCommit, ObjectId objectId) throws IOException {
        File file = new File(treeWalk.getPathString());
        String id = String.format("%s|%s|%s|%s", TYPE_FILE, context.getName(), revCommit.name(), file.getPath());
        logger.debug("Indexing file with id {}", id);
        return IndexFile.indexFile()
            .id(id)
            .commit(revCommit.name())
            .project(context.getName())
            .path(file.getPath())
            .name(file.getName())
            .extension(Files.getFileExtension(file.getAbsolutePath()))
            .content(new String(context.getRepository().open(objectId).getBytes()))
            .build();
    }

}
