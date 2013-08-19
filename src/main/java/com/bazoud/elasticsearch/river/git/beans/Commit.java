package com.bazoud.elasticsearch.river.git.beans;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * @author Olivier Bazoud
 */
@Data
@Builder
public class Commit {
    private String id;
    private String project;
    private Identity author;
    private Identity committer;
    private String subject;
    private String messsage;
    private String encoding;
    private List<String> issues = new ArrayList<String>();
    private List<Parent> parents = new ArrayList<Parent>();
    private String diff;
}
