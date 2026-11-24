package com.example.story.security;

public final class Permissions {

    public static final String CONTEST_READ = "contest:read";
    public static final String CONTEST_MANAGE = "contest:manage";
    public static final String EVENT_READ = "event:read";
    public static final String APPLICATION_READ_OWN = "application:read:own";
    public static final String APPLICATION_WRITE_OWN = "application:write:own";
    public static final String WORK_READ_OWN = "work:read:own";
    public static final String WORK_WRITE_OWN = "work:write:own";
    public static final String USER_MANAGE = "user:manage";
    public static final String ROLE_MANAGE = "role:manage";
    public static final String APPLICATION_READ_ANY = "application:read:any";
    public static final String APPLICATION_MODERATE = "application:moderate";
    public static final String WORK_READ_ANY = "work:read:any";
    public static final String WORK_RATE = "work:rate";
    public static final String EVENT_MANAGE = "event:manage";
    public static final String REPORT_EXPORT = "report:export";

    private Permissions() {
    }
}
