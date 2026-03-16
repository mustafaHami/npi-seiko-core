-- Deployment steps

-- Database

CREATE UNIQUE INDEX unique_idx_customer_shipment_location_id
    ON customer_shipment_location (customer_id, shipment_location_id, currency_id);

CREATE UNIQUE INDEX unique_idx_cost_request_frozen_shipment_location_id
    ON cost_request_frozen_shipment_location (cost_request_id, shipment_location_id, currency_code);

CREATE UNIQUE INDEX unique_idx_shipment_location_id
    ON shipment_location (UPPER(name))
    WHERE archived = false;

CREATE UNIQUE INDEX unique_idx_material_system_id
    ON material (UPPER(system_id))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_cost_request_ref_rev_not_arch
    ON cost_request (UPPER(cost_request_reference_number), cost_request_revision)
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_currency_code
    ON currency (UPPER(code))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_supplier_manufacturer_code
    ON supplier_manufacturer (UPPER(code))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_supplier_manufacturer_name
    ON supplier_manufacturer (UPPER(name))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_customer_name
    ON customer (UPPER(name))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_customer_code
    ON customer (UPPER(code))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_product_name_code
    ON product_name (UPPER(code))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_material_category_code
    ON material_category (UPPER(name))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_shipment_method_name
    ON shipment_method (UPPER(name))
    WHERE archived = false;
CREATE UNIQUE INDEX unique_idx_unit_name
    ON unit (UPPER(name))
    WHERE archived = false;
CREATE SEQUENCE cost_request_reference_code_seq START 1 MAXVALUE 99999 CYCLE;