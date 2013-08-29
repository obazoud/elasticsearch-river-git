package com.bazoud.elasticsearch.river.git.flow.functions;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexFile;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.google.common.io.Files;

import static com.bazoud.elasticsearch.river.git.IndexedDocumentType.FILE;

/**
 * @author Olivier Bazoud
 */
public class RevCommitToIndexFile extends MyFunction<RevCommit, IndexFile> {
    private static ESLogger logger = Loggers.getLogger(RevCommitToIndexFile.class);
    private Context context;

    public RevCommitToIndexFile(Context context) {
        this.context = context;
    }

    @Override
    public IndexFile doApply(RevCommit revCommit) throws Throwable {
        RevTree revTree = revCommit.getTree();
        TreeWalk treeWalk = new TreeWalk(context.getRepository());
        treeWalk.addTree(revTree);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            if (objectId != ObjectId.zeroId()) {
                return toIndexFile(context, treeWalk, revCommit, objectId);
            }
        }
        return null;
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
            .content(safeContent(context, objectId))
            .build();
    }

    private String safeContent(Context context, ObjectId objectId) {
        String content;
        try {
            content = new String(context.getRepository().open(objectId).getBytes());
        } catch (LargeObjectException e) {
            content = null;
        } catch (Exception e) {
            content = null;
        }
        return content;
    }

}
