create table item_unit
(
    id            uuid        primary key,
    item_id       uuid        not null references item (id),
    state         varchar(20) not null,
    item_group_id uuid
);

create index item_unit_item_state on item_unit (item_id, state);
create index item_unit_item_group on item_unit (item_group_id);

insert into item_unit (id, item_id, state)
with recursive units (item_id, remaining) as (
    select id, stock from item where stock > 0
    union all
    select item_id, remaining - 1 from units where remaining > 1
)
select gen_random_uuid(), item_id, 'PRISTINE' from units;
