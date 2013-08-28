package com.bazoud.elasticsearch.river.git.guava;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Olivier Bazoud
 */
public class FunctionFlow<C> {
    private static ESLogger logger = Loggers.getLogger(FunctionFlow.class);

    private ImmutableList<Function<C, C>> functions;

    public FunctionFlow(FunctionFlowBuilder functionFlowBuilder) {
        functions = ImmutableList.copyOf(functionFlowBuilder.functions);
    }

    public void apply(C context) {
        logger.info("Applying {} functions...", functions.size());
        Stopwatch stopwatch = new Stopwatch();
        Stopwatch globalStopwatch = new Stopwatch().start();
        for (Function<C, C> function : functions) {
            logger.info("Starting {} ...", function.getClass().getName());
            stopwatch.reset().start();
            context = function.apply(context);
            stopwatch.stop();
            logger.info("{} done. Tooks {} ms.", function.getClass().getName(), stopwatch.elapsed(MILLISECONDS));
        }
        globalStopwatch.stop();
        logger.info("Apply done. Tooks {} ms.", globalStopwatch.elapsed(MILLISECONDS));
    }

    public static <C> FunctionFlowBuilder flow() {
        return new FunctionFlowBuilder<C>();
    }

    public static class FunctionFlowBuilder<C> {
        private List<Function<C, C>> functions = new ArrayList<Function<C, C>>();

        public FunctionFlow build() {
            return new FunctionFlow<C>(this);
        }

        public FunctionFlowBuilder add(Function function) {
            functions.add(function);
            return this;
        }
    }

}
