package com.bazoud.elasticsearch.river.git.guava.functions;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.google.common.base.Function;
import com.google.common.base.Throwables;

/**
 * @author Olivier Bazoud
 */
public class RefToRevCommit implements Function<Ref, RevCommit> {
    private static ESLogger logger = Loggers.getLogger(RefToRevCommit.class);
    private RevWalk walk;

    public RefToRevCommit(RevWalk walk) {
        this.walk = walk;
    }

    @Override
    public RevCommit apply(Ref ref) {
        try {
            return walk.parseCommit(ref.getObjectId());
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
            return null;
        }
    }
}
