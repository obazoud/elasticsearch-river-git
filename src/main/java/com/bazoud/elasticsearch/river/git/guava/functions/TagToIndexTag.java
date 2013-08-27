package com.bazoud.elasticsearch.river.git.guava.functions;

import java.util.Map;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.IndexTag;
import com.bazoud.elasticsearch.river.git.guava.flow.TagIndexFunction;
import com.google.common.base.Function;
import com.google.common.base.Throwables;

/**
 * @author Olivier Bazoud
 */
public class TagToIndexTag implements Function<Map.Entry<String, Ref>, IndexTag> {
    private static ESLogger logger = Loggers.getLogger(TagToIndexTag.class);
    private Context context;
    private final RevWalk walk;

    public TagToIndexTag(Context context, RevWalk walk) {
        this.context = context;
        this.walk = walk;
    }

    @Override
    public IndexTag apply(Map.Entry<String, Ref> tag) {
        try {

            RevCommit revCommit = walk.parseCommit(tag.getValue().getObjectId());
            String id = String.format("%s|%s|%s", TagIndexFunction.TYPE_TAG, context.getName(), revCommit.name());
            return IndexTag.indexTag()
                .id(id)
                .tag(tag.getKey())
                .ref(tag.getValue().getName())
                .sha1(revCommit.name())
                .build();
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
            return null;
        }
    }
}
