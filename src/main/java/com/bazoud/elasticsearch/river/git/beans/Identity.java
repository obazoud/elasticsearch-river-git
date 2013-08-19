package com.bazoud.elasticsearch.river.git.beans;

import java.util.Date;
import java.util.TimeZone;

import lombok.Builder;
import lombok.Data;

/**
 * @author Olivier Bazoud
 */
@Data
@Builder
public class Identity {
    private String name;
    private String emailAddress;
    private Date when;
    private TimeZone timeZone;
    private int timeZoneOffset;
}