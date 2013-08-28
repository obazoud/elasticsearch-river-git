package com.bazoud.elasticsearch.river.git.flow.functions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import com.bazoud.elasticsearch.river.git.beans.Context;
import com.bazoud.elasticsearch.river.git.beans.Identity;
import com.bazoud.elasticsearch.river.git.beans.IndexCommit;
import com.bazoud.elasticsearch.river.git.beans.Parent;
import com.bazoud.elasticsearch.river.git.guava.MyFunction;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import static com.bazoud.elasticsearch.river.git.IndexedDocumentType.COMMIT;

/**
 * @author Olivier Bazoud
 */
public class RevCommitToIndexCommit extends MyFunction<RevCommit, IndexCommit> {
    private static ESLogger logger = Loggers.getLogger(RevCommitToIndexCommit.class);

    private RevWalk walk;
    private Context context;

    public RevCommitToIndexCommit(Context context, RevWalk walk) {
        this.context = context;
        this.walk = walk;
    }

    @Override
    public IndexCommit doApply(RevCommit revCommit) throws Throwable {
        return IndexCommit.indexCommit()
            .id(String.format("%s|%s|%s", COMMIT.name().toLowerCase(), context.getName(), revCommit.getId().name()))
            .sha1(revCommit.getId().name())
            .project(context.getName())
            .author(
                Identity.identity()
                    .name(revCommit.getAuthorIdent().getName())
                    .emailAddress(revCommit.getAuthorIdent().getEmailAddress())
                    .when(revCommit.getAuthorIdent().getWhen())
                    .timeZone(revCommit.getAuthorIdent().getTimeZone())
                    .timeZoneOffset(revCommit.getAuthorIdent().getTimeZoneOffset())
                    .build()
            )
            .committer(
                Identity.identity()
                    .name(revCommit.getCommitterIdent().getName())
                    .emailAddress(revCommit.getCommitterIdent().getEmailAddress())
                    .when(revCommit.getCommitterIdent().getWhen())
                    .timeZone(revCommit.getCommitterIdent().getTimeZone())
                    .timeZoneOffset(revCommit.getCommitterIdent().getTimeZoneOffset())
                    .build()
            )
            .subject(revCommit.getShortMessage())
            .messsage(revCommit.getFullMessage())
            .encoding(revCommit.getEncoding().name())
            .issues(parseIssues(context, revCommit.getFullMessage()))
            .parents(
                FluentIterable
                    .from(Arrays.asList(revCommit.getParents()))
                    .transform(new Function<RevCommit, Parent>() {
                        @Override
                        public Parent apply(RevCommit commit) {
                            try {
                                return Parent.parent()
                                    .id(commit.getId().getName())
                                    .build();
                            } catch (Throwable e) {
                                logger.error(this.getClass().getName(), e);
                                return null;
                            }
                        }
                    })
                    .toList()
            )
            .diff(parseDiff(context, walk, revCommit))
            .build();
    }

    private String parseDiff(Context context, RevWalk walk, RevCommit revCommit) throws Exception {
        if (context.isIndexingDiff() && revCommit.getParentCount() > 0) {
            RevCommit parentCommit = walk.parseCommit(revCommit.getParent(0).getId());
            ByteArrayOutputStream diffOutputStream = new ByteArrayOutputStream();
            DiffFormatter diffFormatter = new DiffFormatter(diffOutputStream);
            diffFormatter.setRepository(context.getRepository());
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            diffFormatter.format(parentCommit.getTree(), revCommit.getTree());
            return new String(diffOutputStream.toByteArray());
        } else {
            return null;
        }
    }

    private List<String> parseIssues(Context context, String message) {
        if (!context.getIssuePattern().isPresent()) {
            return Collections.emptyList();
        }

        Matcher matcher = context.getIssuePattern().get().matcher(message);
        List<String> issues = new ArrayList<String>();
        while (matcher.find()) {
            issues.add(matcher.group(1));
        }

        return Ordering.natural().sortedCopy(issues);
    }
}