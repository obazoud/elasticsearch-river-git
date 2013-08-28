package com.bazoud.elasticsearch.river.git.flow.predicates;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;

/**
 * @author Olivier Bazoud
 */
public class FetchOrCloneRepositoryPredicate implements Predicate<Context> {
    private static ESLogger logger = Loggers.getLogger(FetchOrCloneRepositoryPredicate.class);

    @Override
    public boolean apply(Context context) {
        try {
            final Repository repository = new FileRepositoryBuilder()
                .setGitDir(context.getProjectPath())
                .readEnvironment()
                .findGitDir()
                .setMustExist(false)
                .build();
            return repository.getObjectDatabase().exists();
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
            return false;
        }
    }

}
