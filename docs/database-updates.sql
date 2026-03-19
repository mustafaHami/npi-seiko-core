alter table users
    drop constraint users_role_check;

alter table users
    add constraint users_role_check
        check ((role)::text = ANY
               ((ARRAY ['ENGINEERING'::character varying, 'PROCUREMENT'::character varying,'ADMINISTRATOR'::character varying, 'SUPER_ADMINISTRATOR'::character varying])::text[]));


--START
CREATE UNIQUE INDEX unique_idx_customer_name
    ON customer (name)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_customer_code
    ON customer (code)
    WHERE archived = false;