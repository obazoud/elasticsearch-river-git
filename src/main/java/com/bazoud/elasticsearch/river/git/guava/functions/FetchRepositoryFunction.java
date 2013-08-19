package com.bazoud.elasticsearch.river.git.guava.functions;

import java.util.Collection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.jgit.LoggingProgressMonitor;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

import static org.eclipse.jgit.lib.RefUpdate.Result.NO_CHANGE;

/**
 * @author Olivier Bazoud
 */
public class FetchRepositoryFunction implements Function<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(FetchRepositoryFunction.class);

    @Override
    public Context apply(Context context) {
        try {
            final Repository repository = new FileRepositoryBuilder()
                .setGitDir(context.getProjectPath())
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir()
                .setMustExist(true)
                .build();
            context.setRepository(repository);

            logger.info("Fetching '{}' repository at path: '{}'", context.getName(), context.getProjectPath());

            Git git = new Git(repository);

            FetchResult fetchResult = git.fetch()
                .setProgressMonitor(new LoggingProgressMonitor(logger))
                .call();


            Collection<Ref> refs = FluentIterable
                .from(fetchResult.getTrackingRefUpdates())
                .filter(Predicates.not(new Predicate<TrackingRefUpdate>() {
                    @Override
                    public boolean apply(TrackingRefUpdate input) {
                        return NO_CHANGE.equals(input.getResult());
                    }
                }))
                .transform(new Function<TrackingRefUpdate, Ref>() {
                    @Override
                    public Ref apply(TrackingRefUpdate input) {
                        try {
                            return repository.getRef(input.getLocalName());
                        } catch(Throwable e) {
                            logger.error(this.getClass().getName(), e);
                            return null;
                        }
                    }
                })
                .toList();

            context.setRefs(refs);
            logger.info("Found {} refs to process.", refs.size());
        } catch(Throwable e) {
            logger.error(this.getClass().getName(), e);
        }

        return context;
    }
}
