alter table customer add column country varchar(20) not null default 'BELGIUM';
alter table customer alter column country drop default;
