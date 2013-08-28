package com.bazoud.elasticsearch.river.git.flow.functions;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.guava.MyFunction;

/**
 * @author Olivier Bazoud
 */
public class RefToRevCommit extends MyFunction<Ref, RevCommit> {
    private static ESLogger logger = Loggers.getLogger(RefToRevCommit.class);
    private RevWalk walk;

    public RefToRevCommit(RevWalk walk) {
        this.walk = walk;
    }

    @Override
    public RevCommit doApply(Ref ref) throws Exception {
        return walk.parseCommit(ref.getObjectId());
    }
}
