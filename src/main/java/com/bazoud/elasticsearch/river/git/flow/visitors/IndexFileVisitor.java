package com.bazoud.elasticsearch.river.git.flow.visitors;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexFile;
import com.bazoud.elasticsearch.river.git.guava.Visitor;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import static com.bazoud.elasticsearch.river.git.IndexedDocumentType.FILE;

/**
 * @author Olivier Bazoud
 */
public class IndexFileVisitor implements Visitor<RevCommit> {
    private static ESLogger logger = Loggers.getLogger(IndexFileVisitor.class);
    private Context context;
    private ImmutableList.Builder builder;

    public IndexFileVisitor(Context context, ImmutableList.Builder builder) {
        this.context = context;
        this.builder = builder;
    }

    @Override
    public void before() {
    }

    @Override
    public void visit(RevCommit revCommit) {
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
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }
    }

    @Override
    public void after() {
    }


    private IndexFile toIndexFile(Context context, TreeWalk treeWalk, RevCommit revCommit, ObjectId objectId) throws IOException {
        File file = new File(treeWalk.getPathString());
        String id = String.format("%s|%s|%s|%s", FILE.name().toLowerCase(), context.getName(), revCommit.name(), file.getPath());
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
