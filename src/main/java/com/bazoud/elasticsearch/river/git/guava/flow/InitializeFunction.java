package com.bazoud.elasticsearch.river.git.guava.flow;

import java.io.File;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.google.common.base.Function;

/**
 * @author Olivier Bazoud
 */
public class InitializeFunction implements Function<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(InitializeFunction.class);

    @Override
    public Context apply(Context context) {
        File projectPath = new File(context.getWorkingDir() + File.separator + context.getName());
        logger.info("Creating directory {}", projectPath.getAbsolutePath());
        projectPath.mkdirs();
        context.setProjectPath(projectPath);
        return context;
    }
}
