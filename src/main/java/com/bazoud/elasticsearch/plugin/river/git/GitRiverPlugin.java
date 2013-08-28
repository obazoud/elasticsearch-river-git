package com.bazoud.elasticsearch.plugin.river.git;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.river.RiversModule;

import com.bazoud.elasticsearch.river.git.GitRiverModule;

/**
 * @author Olivier Bazoud
 */
public class GitRiverPlugin extends AbstractPlugin {
    public static final String RIVER_TYPE = "git";

    @Override
    public String name() {
        return "git-river";
    }

    @Override
    public String description() {
        return "Git River Plugin";
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof RiversModule) {
            ((RiversModule) module).registerRiver(RIVER_TYPE, GitRiverModule.class);
        }
    }
}
