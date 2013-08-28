package com.bazoud.elasticsearch.river.git.flow;

import java.util.Collection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.bazoud.elasticsearch.river.git.jgit.LoggingProgressMonitor;

import static org.eclipse.jgit.lib.Constants.R_HEADS;

/**
 * @author Olivier Bazoud
 */
public class CloneRepositoryFunction extends MyFunction<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(CloneRepositoryFunction.class);

    @Override
    public Context doApply(Context context) throws Throwable {
        logger.info("Cloning the '{}' repository at path: '{}'", context.getName(), context.getProjectPath());
        Git git = Git.cloneRepository()
            .setBare(true)
            .setNoCheckout(true)
            .setCloneAllBranches(true)
            .setDirectory(context.getProjectPath())
            .setURI(context.getUri())
            .setProgressMonitor(new LoggingProgressMonitor(logger))
            .call();
        Repository repository = git.getRepository();
        context.setRepository(repository);

        Collection<Ref> refs = repository.getRefDatabase().getRefs(R_HEADS).values();
        context.setRefs(refs);
        logger.info("Found {} refs to process.", refs.size());
        return context;
    }
}
