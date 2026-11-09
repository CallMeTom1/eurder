create table refresh_token
(
    id             uuid                     primary key,
    credentials_id uuid                     not null references credentials (id) on delete cascade,
    family_id      uuid                     not null,
    token_hash     varchar(64)              not null unique,
    expires_at     timestamp with time zone not null,
    revoked_at     timestamp with time zone
);

create index idx_refresh_token_family_id on refresh_token (family_id);
