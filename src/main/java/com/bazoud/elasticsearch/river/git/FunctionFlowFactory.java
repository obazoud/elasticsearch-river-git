package com.bazoud.elasticsearch.river.git;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.guava.FunctionFlow;
import com.bazoud.elasticsearch.river.git.guava.PredicateFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.CloneRepositoryFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.CommitIndexFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.FetchRepositoryFunction;
import com.bazoud.elasticsearch.river.git.guava.functions.InitializeFunction;
import com.bazoud.elasticsearch.river.git.guava.predicates.FetchOrCloneRepositoryPredicate;

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
            .build();
    }
}
