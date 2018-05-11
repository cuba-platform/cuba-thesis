-- $Id$
alter table SYS_SCHEDULED_TASK drop column PERMITTED_SERVERS^
alter table SYS_SCHEDULED_TASK add (PERMITTED_SERVERS varchar2(4000))^
alter table SYS_SCHEDULED_TASK modify (LAST_START_SERVER varchar2(512 char))^
alter table SYS_SCHEDULED_EXECUTION modify (SERVER varchar2(512 char))^