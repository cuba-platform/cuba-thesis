create table SYS_APP_FOLDER (
    FOLDER_ID varchar2(32 char) not null,
    FILTER_COMPONENT varchar2(200 char),
    FILTER_XML clob,
    VISIBILITY_SCRIPT clob,
    QUANTITY_SCRIPT clob,
    APPLY_DEFAULT char(1),
    primary key(FOLDER_ID)
)^

create table SYS_ATTR_VALUE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    CATEGORY_ATTR_ID varchar2(32 char),
    ENTITY_ID varchar2(32 char),
    STRING_VALUE clob,
    INTEGER_VALUE integer,
    DOUBLE_VALUE numeric,
    DATE_VALUE timestamp,
    BOOLEAN_VALUE char(1),
    ENTITY_VALUE varchar2(32 char),
    CODE varchar2(100 char),
    primary key(ID)
)^
create index IDX_SYS_ATTR_VALUE_ENTITY on SYS_ATTR_VALUE(ENTITY_ID)^

create table SYS_CATEGORY (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    NAME varchar2(255 char),
    SPECIAL varchar2(50 char),
    ENTITY_TYPE varchar2(30 char),
    IS_DEFAULT char(1),
    DISCRIMINATOR integer,
    primary key(ID)
)^

create table SYS_CATEGORY_ATTR (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    CATEGORY_ENTITY_TYPE varchar2(4000 char),
    NAME varchar2(255 char),
    CODE varchar2(100 char) not null,
    CATEGORY_ID varchar2(32 char),
    ENTITY_CLASS varchar2(500 char),
    DATA_TYPE varchar2(200 char),
    DEFAULT_STRING clob,
    DEFAULT_INT integer,
    DEFAULT_DOUBLE numeric,
    DEFAULT_DATE timestamp,
    DEFAULT_DATE_IS_CURRENT char(1),
    DEFAULT_BOOLEAN char(1),
    DEFAULT_ENTITY_VALUE varchar2(32 char),
    ENUMERATION varchar2(500 char),
    ORDER_NO integer,
    SCREEN varchar2(255 char),
    REQUIRED char(1),
    LOOKUP char(1),
    TARGET_SCREENS clob,
    WIDTH varchar2(20 char),
    ROWS_COUNT integer,
    primary key(ID)
)^
create index IDX_SYS_CATEGORY_ATTR_CATEGORY on SYS_CATEGORY_ATTR(CATEGORY_ID)^
create unique index IDX_CAT_ATTR_ENT_TYPE_AND_CODE on SYS_CATEGORY_ATTR (CATEGORY_ENTITY_TYPE, CODE, DELETE_TS)^

create table SYS_CONFIG (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    NAME varchar2(255 char),
    VALUE clob,
    primary key(ID)
)^
create unique index IDX_SYS_CONFIG_UNIQ_NAME on SYS_CONFIG(NAME)^

create table SYS_ENTITY_SNAPSHOT (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    ENTITY_META_CLASS varchar2(50 char) not null,
    ENTITY_ID varchar2(32 char) not null,
    AUTHOR_ID varchar2(32 char) not null,
    VIEW_XML clob not null,
    SNAPSHOT_XML clob not null,
    SNAPSHOT_DATE timestamp not null,
    primary key(ID)
)^
create index IDX_SYS_ENTITY_SNAPSHOT_ENT_ID on SYS_ENTITY_SNAPSHOT(ENTITY_ID)^

create table SYS_ENTITY_STATISTICS (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    NAME varchar2(50 char),
    INSTANCE_COUNT number,
    FETCH_UI integer,
    MAX_FETCH_UI integer,
    LAZY_COLLECTION_THRESHOLD integer,
    LOOKUP_SCREEN_THRESHOLD integer,
    primary key(ID)
)^
create unique index IDX_SYS_ENTITY_STA_UNI_NAM on SYS_ENTITY_STATISTICS(NAME)^

create table SYS_FILE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    NAME varchar2(500 char) not null,
    EXT varchar2(20 char),
    FILE_SIZE number(19),
    CREATE_DATE timestamp,
    primary key(ID)
)^

create table SYS_FOLDER (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    FOLDER_TYPE char(1),
    PARENT_ID varchar2(32 char),
    NAME varchar2(100 char),
    TAB_NAME varchar2(100 char),
    SORT_ORDER integer,
    primary key(ID)
)^

create table SYS_FTS_QUEUE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    ENTITY_ID varchar2(32 char),
    ENTITY_NAME varchar2(200 char),
    CHANGE_TYPE char(1),
    SOURCE_HOST varchar2(255 char),
    INDEXING_HOST varchar2(255 char),
    FAKE char(1),

    primary key(ID)
)^
create index IDX_SYS_FTS_QUEUE_IDXHOST_CRTS on SYS_FTS_QUEUE (INDEXING_HOST, CREATE_TS)^

create table SYS_JMX_INSTANCE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    NODE_NAME varchar2(255 char),
    ADDRESS varchar2(500 char) not null,
    LOGIN varchar2(50 char) not null,
    PASSWORD varchar2(255 char) not null,
    primary key(ID)
)^

create table SYS_LOCK_CONFIG (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    NAME varchar2(100 char),
    TIMEOUT_SEC integer,
    primary key(ID)
)^

create sequence SYS_QUERY_RESULT_SEQ
^

create table SYS_QUERY_RESULT (
    ID number not null,
    SESSION_ID varchar2(32 char) not null,
    QUERY_KEY integer not null,
    ENTITY_ID varchar2(32 char),
    primary key(ID)
)^
create index IDX_SYS_QUERY_RES_ENT_SES_KEY on SYS_QUERY_RESULT(ENTITY_ID, SESSION_ID, QUERY_KEY)^
create index IDX_SYS_QUERY_RESULT_SES_KEY on SYS_QUERY_RESULT(SESSION_ID, QUERY_KEY)^

create or replace trigger SYS_QUERY_RESULT_ID_GEN
before insert on SYS_QUERY_RESULT
for each row
begin
  select SYS_QUERY_RESULT_SEQ.nextval
  into   :new.id
  from   dual;
end;
^

create table SYS_SCHEDULED_EXECUTION (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    TASK_ID varchar2(32 char),
    SERVER varchar2(512 char),
    START_TIME timestamp,
    FINISH_TIME timestamp,
    RESULT clob,
    primary key(ID)
)^
create index IDX_SYS_SCH_EXE_TAS_STA_TIM on SYS_SCHEDULED_EXECUTION(TASK_ID, START_TIME)^

create table SYS_SCHEDULED_TASK (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    DEFINED_BY varchar2(1 char),
    CLASS_NAME varchar2(500 char),
    SCRIPT_NAME varchar2(500 char),
    BEAN_NAME varchar2(50 char),
    METHOD_NAME varchar2(50 char),
    METHOD_PARAMS varchar2(1000 char),
    USER_NAME varchar2(50 char),
    IS_SINGLETON char(1),
    IS_ACTIVE char(1),
    PERIOD integer,
    TIMEOUT integer,
    START_DATE timestamp,
    TIME_FRAME integer,
    START_DELAY integer,
    PERMITTED_SERVERS clob,
    LOG_START char(1),
    LOG_FINISH char(1),
    LAST_START_TIME timestamp,
    LAST_START_SERVER varchar2(512 char),
    DESCRIPTION varchar2(1000 char),
    CRON varchar2(100 char),
    SCHEDULING_TYPE varchar2(1 char) default 'P',
    primary key(ID)
)^
create unique index IDX_SYS_SCH_TAS_UNI_BEA_MET on SYS_SCHEDULED_TASK(BEAN_NAME, METHOD_NAME, METHOD_PARAMS, DELETE_TS)^

create table SYS_SENDING_ATTACHMENT (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),

    MESSAGE_ID varchar2(32 char),
    CONTENT blob,
    CONTENT_ID varchar2(50 char),
    CONTENT_FILE_ID varchar2(32 char),
    NAME varchar2(500 char),
    DISPOSITION varchar2(50 char),
    TEXT_ENCODING varchar2(50 char),

    primary key(ID)
)^
create index SYS_SENDING_ATTACHMENT_MES_IDX on SYS_SENDING_ATTACHMENT(MESSAGE_ID)^

create table SYS_SENDING_MESSAGE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    ADDRESS_TO clob,
    ADDRESS_FROM varchar2(100 char),
    CAPTION varchar2(500 char),
    EMAIL_HEADERS varchar2(500 char),
    CONTENT_TEXT clob,
    CONTENT_TEXT_FILE_ID varchar2(32 char),
    DEADLINE timestamp,
    STATUS integer,
    DATE_SENT timestamp,
    ATTEMPTS_COUNT integer,
    ATTEMPTS_MADE integer,
    ATTACHMENTS_NAME clob,
    primary key(ID)
)^
create index IDX_SYS_SENDING_MES_DAT_SEN on SYS_SENDING_MESSAGE(DATE_SENT)^
create index IDX_SYS_SENDING_MESSAGE_STATUS on SYS_SENDING_MESSAGE(STATUS)^
create index IDX_SYS_SENDING_MES_UPDATE_TS on SYS_SENDING_MESSAGE (UPDATE_TS)^

create table SYS_SERVER (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    NAME varchar2(255 char),
    IS_RUNNING char(1),
    DATA clob,
    primary key(ID)
)^
create unique index IDX_SYS_SERVER_UNIQ_NAME on SYS_SERVER(NAME)^

create table SEC_CONSTRAINT (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    ENTITY_NAME varchar2(50 char),
    JOIN_CLAUSE varchar2(500 char),
    WHERE_CLAUSE varchar2(1000 char),
    GROUP_ID varchar2(32 char),
    primary key(ID)
)^
create index IDX_SEC_CONSTRAINT_GROUP on SEC_CONSTRAINT(GROUP_ID)^

create table SEC_ENTITY_LOG (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    EVENT_TS timestamp,
    USER_ID varchar2(32 char),
    CHANGE_TYPE char(1),
    ENTITY varchar2(100 char),
    ENTITY_ID varchar2(32 char),
    CHANGES clob,
    primary key(ID)
)^
create index IDX_SEC_ENTITY_LOG_ENTITY_ID on SEC_ENTITY_LOG(ENTITY_ID)^

create table SEC_ENTITY_LOG_ATTR (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    ITEM_ID varchar2(32 char),
    NAME varchar2(50 char),
    VALUE varchar2(1500 char),
    VALUE_ID varchar2(32 char),
    MESSAGES_PACK varchar2(200 char),
    primary key(ID)
)^
create index IDX_SEC_ENTITY_LOG_ATTR_ITEM on SEC_ENTITY_LOG_ATTR(ITEM_ID)^

create table SEC_FILTER (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    COMPONENT varchar2(200 char),
    NAME varchar2(255 char),
    CODE varchar2(200 char),
    XML clob,
    USER_ID varchar2(32 char),
    primary key(ID)
)^
create index IDX_SEC_FILTER_COMPONENT_USER on SEC_FILTER(COMPONENT, USER_ID)^

create table SEC_GROUP (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    NAME varchar2(255 char),
    PARENT_ID varchar2(32 char),
    primary key(ID)
)^

create table SEC_GROUP_HIERARCHY (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    GROUP_ID varchar2(32 char),
    PARENT_ID varchar2(32 char),
    HIERARCHY_LEVEL integer,
    primary key(ID)
)^

create table SEC_LOGGED_ATTR (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    ENTITY_ID varchar2(32 char),
    NAME varchar2(50 char),
    primary key(ID)
)^
create unique index SEC_LOGGED_ATTR_UNIQ_NAME on SEC_LOGGED_ATTR(ENTITY_ID, NAME)^
create index IDX_SEC_LOGGED_ATTR_ENTITY on SEC_LOGGED_ATTR(ENTITY_ID)^

create table SEC_LOGGED_ENTITY (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    NAME varchar2(100 char),
    AUTO char(1),
    MANUAL char(1),
    primary key(ID)
)^
create unique index SEC_LOGGED_ENTITY_UNIQ_NAME on SEC_LOGGED_ENTITY(NAME)^

create table SEC_PERMISSION (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    PERMISSION_TYPE integer,
    TARGET varchar2(100 char),
    VALUE integer,
    ROLE_ID varchar2(32 char),
    primary key(ID)
)^
create unique index IDX_SEC_PERMISSION_UNIQUE on SEC_PERMISSION(ROLE_ID, PERMISSION_TYPE, TARGET, DELETE_TS)^

create table SEC_PRESENTATION (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    COMPONENT varchar2(200 char),
    NAME varchar2(255 char),
    XML clob,
    USER_ID varchar2(32 char),
    IS_AUTO_SAVE char(1),
    primary key(ID)
)^
create index IDX_SEC_PRESENTATION_COM_USE on SEC_PRESENTATION(COMPONENT, USER_ID)^

create table SEC_ROLE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    NAME varchar2(255 char),
    LOC_NAME varchar2(255 char),
    DESCRIPTION varchar2(1000 char),
    IS_DEFAULT_ROLE char(1),
    ROLE_TYPE integer,
    primary key(ID)
)^
create unique index IDX_SEC_ROLE_UNIQ_NAME on SEC_ROLE(NAME, DELETE_TS)^

create table SEC_SCREEN_HISTORY (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    USER_ID varchar2(32 char),
    CAPTION varchar2(255 char),
    URL clob,
    ENTITY_ID varchar2(32 char),
    SUBSTITUTED_USER_ID varchar2(32 char),
    primary key(ID)
)^
create index IDX_SEC_SCREEN_HISTORY_USER on SEC_SCREEN_HISTORY(USER_ID)^
create index IDX_SEC_SCREEN_HIST_SUB_USER on SEC_SCREEN_HISTORY(SUBSTITUTED_USER_ID)^

create table SEC_SEARCH_FOLDER (
    FOLDER_ID varchar2(32 char) not null,
    FILTER_COMPONENT varchar2(200 char),
    FILTER_XML clob,
    USER_ID varchar2(32 char),
    PRESENTATION_ID varchar2(32 char),
    APPLY_DEFAULT char(1),
    IS_SET char(1),
    ENTITY_TYPE varchar2(50 char),
    primary key(FOLDER_ID)
)^
create index IDX_SEC_SEARCH_FOLDER_USER on SEC_SEARCH_FOLDER(USER_ID)^

create table SEC_SESSION_ATTR (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    NAME varchar2(50 char),
    STR_VALUE varchar2(1000 char),
    DATATYPE varchar2(20 char),
    GROUP_ID varchar2(32 char),
    primary key(ID)
)^
create index IDX_SEC_SESSION_ATTR_GROUP on SEC_SESSION_ATTR(GROUP_ID)^

create table SEC_USER (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    LOGIN varchar2(50 char) not null,
    LOGIN_LC varchar2(50 char) not null,
    PASSWORD varchar2(255 char),
    NAME varchar2(255 char),
    FIRST_NAME varchar2(255 char),
    LAST_NAME varchar2(255 char),
    MIDDLE_NAME varchar2(255 char),
    POSITION_ varchar2(255 char),
    EMAIL varchar2(100 char),
    LANGUAGE_ varchar2(20 char),
    TIME_ZONE varchar2(50 char),
    TIME_ZONE_AUTO char(1),
    ACTIVE char(1),
    GROUP_ID varchar2(32 char),
    IP_MASK varchar2(200 char),
    CHANGE_PASSWORD_AT_LOGON char(1),
    primary key(ID)
)^
create unique index IDX_SEC_USER_UNIQ_LOGIN on SEC_USER(LOGIN_LC, DELETE_TS)^

create table SEC_USER_ROLE (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    USER_ID varchar2(32 char),
    ROLE_ID varchar2(32 char),
    primary key(ID)
)^
create unique index IDX_SEC_USER_ROLE_UNIQ_ROLE on SEC_USER_ROLE(USER_ID, ROLE_ID, DELETE_TS)^

create table SEC_USER_SETTING (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    USER_ID varchar2(32 char),
    CLIENT_TYPE char(1),
    NAME varchar2(255 char),
    VALUE clob,
    primary key(ID)
)^
create unique index SEC_USER_SETTING_UNIQ on SEC_USER_SETTING(USER_ID, NAME, CLIENT_TYPE)^

create table SEC_USER_SUBSTITUTION (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50 char),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50 char),
    USER_ID varchar2(32 char),
    SUBSTITUTED_USER_ID varchar2(32 char),
    START_DATE timestamp,
    END_DATE timestamp,
    primary key(ID)
)^
create index IDX_SEC_USER_SUBSTITUTION_USER on SEC_USER_SUBSTITUTION(USER_ID)^

create table SEC_REMEMBER_ME (
    ID varchar2(32 char) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50 char),
    VERSION integer,
    --
    USER_ID varchar2(32 char) not null,
    TOKEN varchar2(32 char) not null,
    --
    primary key (ID)
)^
create index IDX_SEC_REMEMBER_ME_USER on SEC_REMEMBER_ME(USER_ID)^
create index IDX_SEC_REMEMBER_ME_TOKEN on SEC_REMEMBER_ME(TOKEN)^

alter table SYS_APP_FOLDER add constraint FK_SYS_APP_FOLDER_FOLDER foreign key (FOLDER_ID) references SYS_FOLDER(ID)^

alter table SYS_ATTR_VALUE add constraint SYS_ATTR_VALUE_CATEGORY_ATT_ID foreign key (CATEGORY_ATTR_ID) references SYS_CATEGORY_ATTR(ID)^

alter table SYS_CATEGORY_ATTR add constraint SYS_CATEGORY_ATTR_CATEGORY_ID foreign key (CATEGORY_ID) references SYS_CATEGORY(ID)^

alter table SYS_ENTITY_SNAPSHOT add constraint FK_SYS_ENTITY_SNAPSHOT_AUT_ID foreign key (AUTHOR_ID) references SEC_USER(ID)^

alter table SYS_FOLDER add constraint FK_SYS_FOLDER_PARENT foreign key (PARENT_ID) references SYS_FOLDER(ID)^

alter table SYS_SCHEDULED_EXECUTION add constraint SYS_SCHEDULED_EXECUTION_TASK foreign key (TASK_ID) references SYS_SCHEDULED_TASK(ID)^

alter table SYS_SENDING_MESSAGE add constraint FK_SYS_SENDING_MESSAGE_FILE foreign key (CONTENT_TEXT_FILE_ID) references SYS_FILE(ID)^

alter table SYS_SENDING_ATTACHMENT add constraint FK_SYS_SENDING_ATT_SEN_MES foreign key (MESSAGE_ID) references SYS_SENDING_MESSAGE(ID)^

alter table SYS_SENDING_ATTACHMENT add constraint FK_SYS_SENDING_ATTACH_FILE foreign key (CONTENT_FILE_ID) references SYS_FILE (ID)^

alter table SEC_CONSTRAINT add constraint SEC_CONSTRAINT_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID)^

alter table SEC_ENTITY_LOG add constraint FK_SEC_ENTITY_LOG_USER foreign key (USER_ID) references SEC_USER(ID)^

alter table SEC_ENTITY_LOG_ATTR add constraint FK_SEC_ENTITY_LOG_ATTR_ITEM foreign key (ITEM_ID) references SEC_ENTITY_LOG(ID)^

alter table SEC_FILTER add constraint FK_SEC_FILTER_USER foreign key (USER_ID) references SEC_USER(ID)^

alter table SEC_GROUP add constraint SEC_GROUP_PARENT foreign key (PARENT_ID) references SEC_GROUP(ID)^

alter table SEC_GROUP_HIERARCHY add constraint SEC_GROUP_HIERARCHY_PARENT foreign key (PARENT_ID) references SEC_GROUP(ID)^
alter table SEC_GROUP_HIERARCHY add constraint SEC_GROUP_HIERARCHY_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID)^

alter table SEC_LOGGED_ATTR add constraint FK_SEC_LOGGED_ATTR_ENTITY foreign key (ENTITY_ID) references SEC_LOGGED_ENTITY(ID)^

alter table SEC_PERMISSION add constraint SEC_PERMISSION_ROLE foreign key (ROLE_ID) references SEC_ROLE(ID)^

alter table SEC_PRESENTATION add constraint SEC_PRESENTATION_USER foreign key (USER_ID) references SEC_USER(ID)^

alter table SEC_SCREEN_HISTORY add constraint FK_SEC_HISTORY_USER foreign key (USER_ID) references SEC_USER(ID)^
alter table SEC_SCREEN_HISTORY add constraint FK_SEC_HISTORY_SUB_USER foreign key (SUBSTITUTED_USER_ID) references SEC_USER(ID)^

alter table SEC_SEARCH_FOLDER add constraint FK_SEC_SEARCH_FOLDER_PRE foreign key (PRESENTATION_ID) references SEC_PRESENTATION(ID)^
alter table SEC_SEARCH_FOLDER add constraint FK_SEC_SEARCH_FOLDER_USER foreign key (USER_ID) references SEC_USER(ID)^
alter table SEC_SEARCH_FOLDER add constraint FK_SEC_SEARCH_FOLDER_FOLDER foreign key (FOLDER_ID) references SYS_FOLDER(ID)^

alter table SEC_SESSION_ATTR add constraint SEC_SESSION_ATTR_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID)^

alter table SEC_USER add constraint SEC_USER_GROUP foreign key (GROUP_ID) references SEC_GROUP(ID)^

alter table SEC_USER_ROLE add constraint SEC_USER_ROLE_ROLE foreign key (ROLE_ID) references SEC_ROLE(ID)^
alter table SEC_USER_ROLE add constraint SEC_USER_ROLE_PROFILE foreign key (USER_ID) references SEC_USER(ID)^

alter table SEC_USER_SETTING add constraint SEC_USER_SETTING_USER foreign key (USER_ID) references SEC_USER(ID)^

alter table SEC_USER_SUBSTITUTION add constraint FK_SEC_USER_SUB_SUB_USE foreign key (SUBSTITUTED_USER_ID) references SEC_USER(ID)^
alter table SEC_USER_SUBSTITUTION add constraint FK_SEC_USER_SUBSTITUTION_USER foreign key (USER_ID) references SEC_USER(ID)^

alter table SEC_REMEMBER_ME add constraint FK_SEC_REMEMBER_ME_USER foreign key (USER_ID) references SEC_USER(ID)^

--------------------------------------------------------------------------------------------------------------

create or replace function NEWID return varchar2
is
begin
  return lower(rawtohex(sys_guid()));
end;
^

--------------------------------------------------------------------------------------------------------------

create or replace procedure SET_SEQ_VAL(seqname varchar2, newvalue number)
as
    ln number;
    ib number;
begin
    select LAST_NUMBER, INCREMENT_BY
    into ln, ib
    from USER_SEQUENCES
    where SEQUENCE_NAME = upper(seqname);

    execute immediate 'alter sequence ' || seqname || ' increment by ' || (newvalue - ln);

    execute immediate 'select ' || seqname || '.nextval from dual' into ln;

    execute immediate 'alter sequence ' || seqname || ' increment by ' || ib;
end;
^

create or replace function GET_SEQ_VAL(seqname varchar2) return NUMBER
as
    ln number;
    ib number;
begin
    select LAST_NUMBER, INCREMENT_BY
    into ln, ib
    from USER_SEQUENCES
    where SEQUENCE_NAME = upper(seqname);

    return ln - ib;
end;
^
--------------------------------------------------------------------------------------------------------------

insert into SEC_GROUP (ID, CREATE_TS, VERSION, NAME, PARENT_ID)
values ('0fa2b1a51d684d699fbddff348347f93', current_timestamp, 0, 'Company', null)^

insert into SEC_USER (ID, CREATE_TS, VERSION, LOGIN, LOGIN_LC, PASSWORD, NAME, GROUP_ID, ACTIVE)
values ('608859871b61424794c7dff348347f93', current_timestamp, 0, 'admin', 'admin',
'cc2229d1b8a052423d9e1c9ef0113b850086586a',
'Administrator', '0fa2b1a51d684d699fbddff348347f93', 1)^

insert into SEC_ROLE (ID, CREATE_TS, VERSION, NAME, ROLE_TYPE)
values ('0c018061b26f4de2a5bedff348347f93', current_timestamp, 0, 'Administrators', 10)^

insert into SEC_FILTER (ID,CREATE_TS,CREATED_BY,VERSION,COMPONENT,NAME,XML,USER_ID)
values (newid(), current_timestamp, 'admin', 0, '[sec$User.browse].genericFilter', 'Search by role',
'<?xml version="1.0" encoding="UTF-8"?>
<filter>
<and>
<c name="UrMxpkfMGn" class="com.haulmont.cuba.security.entity.Role" type="CUSTOM" locCaption="Role" entityAlias="u" join="join u.userRoles ur">ur.role.id = :component$genericFilter.UrMxpkfMGn32565
<param name="component$genericFilter.UrMxpkfMGn32565">NULL</param>
</c>
</and>
</filter>',
'608859871b61424794c7dff348347f93'
)^
