CREATE TABLE public.dyson_shipment_location
(
    dyson_shipment_location_id     uuid           NOT NULL,
    archive_date                   timestamptz(6) NULL,
    archived                       bool           NOT NULL,
    creation_date                  timestamptz(6) NOT NULL,
    "name"                         varchar(255)   NOT NULL,
    searchable_concatenated_fields text           NOT NULL,
    CONSTRAINT dyson_shipment_location_pkey PRIMARY KEY (dyson_shipment_location_id)
);

ALTER TABLE public.cost_request_line
    ADD tooling_markup numeric(25, 6) NULL;

ALTER TABLE public.cost_request_line
    ADD tooling_strategy varchar(255) NOT NULL DEFAULT 'AMORTIZED';

ALTER TABLE public.cost_request_line
    ADD CONSTRAINT cost_request_line_tooling_strategy_check CHECK (((tooling_strategy)::text = ANY
                                                                    ((ARRAY ['AMORTIZED'::character varying, 'SEPARATED'::character varying])::text[])));

ALTER TABLE public.other_cost_line
    ADD shipment_to_customer_line boolean NOT NULL DEFAULT false;

ALTER TABLE public.other_cost_line
    ADD dyson_shipment_location_id uuid NULL;

ALTER TABLE public.other_cost_line
    ADD CONSTRAINT fk_other_cost_line_dyson_shipment_location FOREIGN KEY (dyson_shipment_location_id) REFERENCES dyson_shipment_location (dyson_shipment_location_id);

ALTER TABLE public.cost_request_line
    ADD total_cost_in_target_currency_without_tooling_cost numeric(25, 6) NULL;
ALTER TABLE public.cost_request_line
    ADD total_tooling_cost_with_markup_in_target_currency numeric(25, 6) NULL;

-- END ----------------------------------
--START
alter table other_cost_line
    add column editable_line boolean default false;

alter table process
    rename column cost_per_second to cost_per_minute;

ALTER TABLE public.cost_request_line
    ADD total_material_cost_in_target_currency_with_yield numeric(25, 6) NULL;
ALTER TABLE public.cost_request_line
    ADD yield_applied numeric(25, 6) NULL;

ALTER TABLE public.cost_request_line_per_cost_request_quantity
    ADD total_material_cost_in_target_currency_with_yield numeric(25, 6) NULL;
ALTER TABLE public.cost_request_line_per_cost_request_quantity
    ADD yield_applied numeric(25, 6) NULL;

ALTER TABLE public.material_line_draft
    ADD marked_not_used_for_quote boolean DEFAULT false NOT NULL;
ALTER TABLE public.material_line
    ADD marked_not_used_for_quote boolean DEFAULT false NOT NULL;

ALTER TABLE public.material
    ADD draft_category_name varchar(255) NULL;
ALTER TABLE public.material
    ADD draft_manufacturer_name varchar(255) NULL;
ALTER TABLE public.material
    ADD draft_unit_name varchar(255) NULL;

ALTER TABLE public.material_line_draft
    ADD draft_category_name varchar(255) NULL;
ALTER TABLE public.material_line_draft
    ADD draft_manufacturer_name varchar(255) NULL;
ALTER TABLE public.material_line_draft
    ADD draft_unit_name varchar(255) NULL;

ALTER TABLE public.material_line_draft
    DROP COLUMN material_exists_in_database;


-- END

--START

alter table cost_request
    add column cloned_from_reference_number varchar(255);

--END

--START
alter table cost_request
    drop constraint cost_request_status_check;

alter table cost_request
    add constraint cost_request_status_check
        check ((status)::text = ANY
               ((ARRAY ['NEW_REVISION_CREATED'::character varying,'PENDING_INFORMATION'::character varying, 'READY_FOR_REVIEW'::character varying, 'READY_TO_ESTIMATE'::character varying, 'ESTIMATED'::character varying, 'READY_FOR_MARKUP'::character varying, 'PENDING_APPROVAL'::character varying, 'PRICE_APPROVED'::character varying, 'PRICE_REJECTED'::character varying, 'PENDING_REESTIMATION'::character varying, 'READY_TO_QUOTE'::character varying, 'ACTIVE'::character varying, 'WON'::character varying, 'LOST'::character varying, 'ABORTED'::character varying])::text[]));
alter table cost_request_line
    drop constraint cost_request_line_status_check;

alter table cost_request_line
    add constraint cost_request_line_status_check
        check ((status)::text = ANY
               ((ARRAY ['NEW_REVISION_CREATED'::character varying,'PENDING_INFORMATION'::character varying, 'READY_FOR_REVIEW'::character varying, 'READY_TO_ESTIMATE'::character varying, 'ESTIMATED'::character varying, 'READY_FOR_MARKUP'::character varying, 'PENDING_APPROVAL'::character varying, 'PRICE_APPROVED'::character varying, 'PRICE_REJECTED'::character varying, 'PENDING_REESTIMATION'::character varying, 'READY_TO_QUOTE'::character varying, 'ACTIVE'::character varying, 'WON'::character varying, 'LOST'::character varying, 'ABORTED'::character varying])::text[]));

--END
--START
CREATE UNIQUE INDEX unique_idx_material_system_id
    ON material (system_id)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_cost_request_ref_rev_not_arch
    ON cost_request (cost_request_reference_number, cost_request_revision)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_currency_code
    ON currency (code)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_supplier_name
    ON supplier (name)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_customer_name
    ON customer (name)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_customer_code
    ON customer (code)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_manufacturer_code
    ON manufacturer (code)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_product_name_code
    ON product_name (code)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_material_category_code
    ON material_category (name)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_shipment_method_name
    ON shipment_method (name)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_unit_name
    ON unit (name)
    WHERE archived = false;
CREATE SEQUENCE cost_request_reference_code_seq START 1 MAXVALUE 99999 CYCLE;
--END
--START
ALTER TABLE cost_request_line
    ADD COLUMN IF NOT EXISTS total_cost_with_markup_in_target_currency NUMERIC(19, 6);
ALTER TABLE cost_request_line_per_cost_request_quantity
    ADD COLUMN IF NOT EXISTS total_cost_with_markup_in_target_currency NUMERIC(19, 6);
--END