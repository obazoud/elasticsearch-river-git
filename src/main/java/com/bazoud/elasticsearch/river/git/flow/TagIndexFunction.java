package com.bazoud.elasticsearch.river.git.flow;

import org.eclipse.jgit.revwalk.RevWalk;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.flow.functions.TagToIndexTag;
import com.bazoud.elasticsearch.river.git.flow.visitors.BulkVisitor;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.bazoud.elasticsearch.river.git.guava.Visitors;
import com.google.common.collect.FluentIterable;

import static com.bazoud.elasticsearch.river.git.IndexedDocumentType.TAG;

/**
 * @author Olivier Bazoud
 */
public class TagIndexFunction extends MyFunction<Context, Context> {

    @Override
    public Context doApply(Context context) throws Throwable {
        RevWalk walk = new RevWalk(context.getRepository());

        Visitors.visit(
            FluentIterable
                .from(context.getRepository().getTags().entrySet())
                .transform(new TagToIndexTag(context, walk)),
            new BulkVisitor(context, TAG.name().toLowerCase())
        );

        return context;
    }

}