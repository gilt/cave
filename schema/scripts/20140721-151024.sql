--------------------
-- Organizations ---
--------------------
create table organizations (
     id                   bigserial not null,
     name                 text not null,
     email                text not null,
     notification_url     text not null,
     cluster              text,
     primary key (id)
);

select schema_evolution_manager.create_basic_audit_data('public', 'organizations');
create unique index organizations_name_not_deleted_un_idx
    on organizations (name) where deleted_at is null;

--------------------
--     Teams     ---
--------------------
create table teams (
     id                   bigserial not null,
     organization_id      bigint not null,
     name                 text not null,
     cluster              text,
     primary key (id)
);

select schema_evolution_manager.create_basic_audit_data('public', 'teams');
create unique index teams_name_per_org_not_deleted_un_idx
    on teams (name, organization_id) where deleted_at is null;

--------------------
--    Tokens     ---
--------------------
create table tokens (
     id                   bigserial not null,
     organization_id      bigint not null,
     team_id              bigint,
     description          text not null,
     value                text not null,
     primary key (id)
);

select schema_evolution_manager.create_basic_audit_data('public', 'tokens');
create unique index tokens_id_not_deleted_un_idx
    on tokens (id) where deleted_at is null;
create index tokens_by_organization_not_deleted_idx
    on tokens (organization_id) where team_id is null and deleted_at is null;
create index tokens_by_team_not_deleted_idx
    on tokens (team_id) where deleted_at is null;

--------------------
--    Alerts     ---
--------------------
create table alerts (
     id                   bigserial not null,
     organization_id      bigint not null,
     team_id              bigint,
     description          text not null,
     status               bool,
     handbook_url         text,
     condition            text not null,
     period               text not null,
     routing              text,
     primary key (id)
);

select schema_evolution_manager.create_basic_audit_data('public', 'alerts');
create unique index alerts_id_not_deleted_un_idx
    on alerts (id) where deleted_at is null;
create index alerts_by_organization_not_deleted_idx
    on alerts (organization_id) where team_id is null and deleted_at is null;
create index alerts_by_team_not_deleted_idx
    on alerts (team_id) where deleted_at is null;


--------------------
--    Queries    ---
--------------------
create table queries (
     id                   bigserial not null,
     name                 text not null,
     primary key (id)
);

comment on column queries.name is '
  This is the generated name for a query. We look at the metric
  definition and create a name that is valid for influx db. The name
  will contain the original metric name, all tags, and any aggregator
  function / period.

  An example name would be response_time.app:product_service.p99.5m
';

select schema_evolution_manager.create_basic_audit_data('public', 'queries');
create unique index queries_id on queries (id);
create unique index queries_name_not_deleted_un_idx
    on queries(name) where deleted_at is null;

--------------------
-- Alert Queries  --
--------------------
create table alert_queries (
     id                   bigserial not null,
     alert_id             bigint not null,
     query_id             bigint not null,
     primary key (id)
);

select schema_evolution_manager.create_basic_audit_data('public', 'alert_queries');
create index alert_queries_alert_id on alert_queries (alert_id);
create index alert_queries_query_id on alert_queries (query_id);
create unique index alert_queries_alert_id_query_id_not_deleted_un_idx
    on alert_queries(alert_id, query_id) where deleted_at is null;


--------------------
--   Schedulers    --
--------------------
create table schedulers (
     id                   bigserial not null,
     name                 text not null,
     created_at           timestamp with time zone not null,
     primary key (id)
);


----------------
--   Users    --
----------------
create table users (
     id                   bigserial not null,
     first_name           text not null,
     last_name            text not null,
     email                text not null,
     password             text not null,
     salt                 text,
     primary key (id)
);

------------------------
--   Session Tokens   --
------------------------
create table session_tokens (
     id                   bigserial not null,
     user_id              bigint not null,
     token                text not null,
     creation_time        timestamp with time zone not null,
     expiration_time      timestamp with time zone not null,
     primary key (id)
);

create index session_tokens_user_id on session_tokens (user_id);

------------------------------
--   Confirmation Tokens    --
------------------------------
create table confirmation_tokens (
     id                   bigserial not null,
     uuid                 text not null,
     email                text not null,
     creation_time        timestamp with time zone not null,
     expiration_time      timestamp with time zone not null,
     is_sign_up           bool not null,
     primary key (id)
);


-------------------------
-- Organization Users  --
-------------------------
create table organization_users (
     id                   bigserial not null,
     organization_id      bigint not null,
     user_id              bigint not null,
     role                 text not null,
     primary key (id)
);

create index organization_users_organization_id on organization_users (organization_id);
create index organization_users_user_id on organization_users (user_id);
create unique index organization_users_organization_id_user_id_un_idx
    on organization_users(organization_id, user_id);


-----------------
-- Team Users  --
-----------------
create table team_users (
     id                   bigserial not null,
     team_id              bigint not null,
     user_id              bigint not null,
     role                 text not null,
     primary key (id)
);

create index team_users_alert_id on team_users (team_id);
create index team_users_user_id on team_users (user_id);
create unique index team_users_team_id_user_id_un_idx
    on team_users(team_id, user_id);


--------------
--- Status ---
--------------
create table status (
     id                   bigserial not null,
     description          text not null,
     since                timestamp with time zone not null,
     until                timestamp with time zone,
     primary key (id)
);

--------------------
-- Relationships  --
--------------------
alter table teams
  add constraint team_organization_fk
  foreign key (organization_id) references organizations (id);

alter table tokens
  add constraint token_team_fk
  foreign key (team_id) references teams (id);

alter table tokens
  add constraint token_organization_fk
  foreign key (organization_id) references organizations (id);

alter table alerts
  add constraint alert_organization_fk
  foreign key (organization_id) references organizations (id);

alter table alerts
  add constraint alert_team_fk
  foreign key (team_id) references teams (id);

alter table alert_queries
  add constraint alertquery_alert_fk
  foreign key (alert_id) references alerts (id);

alter table alert_queries
  add constraint alertquery_query_fk
  foreign key (query_id) references queries (id);

alter table session_tokens
  add constraint session_tokens_users_fk
  foreign key (user_id) references users (id);

alter table organization_users
  add constraint organization_user_organization_fk
  foreign key (organization_id) references organizations (id);

alter table organization_users
  add constraint organization_user_user_fk
  foreign key (user_id) references users (id);

alter table team_users
  add constraint team_user_team_fk
  foreign key (team_id) references teams (id);

alter table team_users
  add constraint team_user_user_fk
  foreign key (user_id) references users (id);
