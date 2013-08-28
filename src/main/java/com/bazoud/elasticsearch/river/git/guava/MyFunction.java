package com.bazoud.elasticsearch.river.git.guava;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.google.common.base.Function;
import com.google.common.base.Throwables;

/**
 * @author Olivier Bazoud
 */
public abstract class MyFunction<F, T> implements Function<F, T> {
    private static ESLogger logger = Loggers.getLogger(MyFunction.class);

    @Override
    public T apply(F input) {
        T output = null;
        try {
            output = doApply(input);
        } catch (Throwable e) {
            logger.error(this.getClass().getName(), e);
            Throwables.propagate(e);
        }
        return output;
    }

    public abstract T doApply(F input) throws Throwable;
}
