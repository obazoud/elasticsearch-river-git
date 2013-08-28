package com.bazoud.elasticsearch.river.git.flow;

import java.io.File;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;

/**
 * @author Olivier Bazoud
 */
public class InitializeFunction extends MyFunction<Context, Context> {
    private static ESLogger logger = Loggers.getLogger(InitializeFunction.class);

    @Override
    public Context doApply(Context context) throws Throwable {
        File projectPath = new File(context.getWorkingDir() + File.separator + context.getName());
        logger.info("Creating directory {}", projectPath.getAbsolutePath());
        projectPath.mkdirs();
        context.setProjectPath(projectPath);
        return context;
    }
}
