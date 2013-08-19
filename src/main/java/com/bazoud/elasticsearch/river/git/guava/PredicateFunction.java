package com.bazoud.elasticsearch.river.git.guava;

import java.io.Serializable;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * @author Olivier Bazoud
 */
public class PredicateFunction<F, T> implements Function<F, T>, Serializable {
    private static ESLogger logger = Loggers.getLogger(PredicateFunction.class);
    private Function<F, T> left;
    private Function<F, T> right;
    private Predicate<F> predicate;

    public PredicateFunction(Function<F, T> left, Function<F, T> right, Predicate<F> predicate) {
        this.left = left;
        this.right = right;
        this.predicate = predicate;
    }

    @Override
    public T apply(F input) {
        if (this.predicate.apply(input)) {
            logger.info(left.getClass().getName());
            return left.apply(input);
        } else {
            logger.info(right.getClass().getName());
            return right.apply(input);
        }
    }
}
