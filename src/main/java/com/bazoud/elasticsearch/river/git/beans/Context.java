package com.bazoud.elasticsearch.river.git.beans;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.elasticsearch.client.Client;

import com.google.common.base.Optional;
import lombok.Data;

/**
 * @author Olivier Bazoud
 */
@Data
@SuppressWarnings("PMD.UnusedPrivateField")
public class Context {
    private String name;
    private String uri;
    private File projectPath;
    private Repository repository;
    private String workingDir =
        System.getProperty("user.home") + File.separator + ".elasticsearch-river-git";
    private Collection<Ref> refs;
    private Client client;
    private Optional<Pattern> issuePattern = Optional.absent();
    private String issueRegex;
    private boolean indexingDiff;
    private long updateRate = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);
    private String type = "git";
    private String riverName;
    private String riverIndexName;
}
