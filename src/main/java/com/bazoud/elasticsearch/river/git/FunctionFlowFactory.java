package com.bazoud.elasticsearch.river.git;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.flow.CloneRepositoryFunction;
import com.bazoud.elasticsearch.river.git.flow.CommitIndexFunction;
import com.bazoud.elasticsearch.river.git.flow.FetchRepositoryFunction;
import com.bazoud.elasticsearch.river.git.flow.FileIndexFunction;
import com.bazoud.elasticsearch.river.git.flow.InitializeFunction;
import com.bazoud.elasticsearch.river.git.flow.TagIndexFunction;
import com.bazoud.elasticsearch.river.git.flow.predicates.FetchOrCloneRepositoryPredicate;
import com.bazoud.elasticsearch.river.git.guava.FunctionFlow;
import com.bazoud.elasticsearch.river.git.guava.PredicateFunction;

/**
 * @author Olivier Bazoud
 */
public class FunctionFlowFactory {
    public FunctionFlow<Context> create() {
        return FunctionFlow.<Context>flow()
            .add(new InitializeFunction())
            .add(
                new PredicateFunction<Context, Context>(
                    new FetchRepositoryFunction(),
                    new CloneRepositoryFunction(),
                    new FetchOrCloneRepositoryPredicate()
                )
            )
            .add(new CommitIndexFunction())
            .add(new TagIndexFunction())
            .add(new FileIndexFunction())
            .build();
    }
}
