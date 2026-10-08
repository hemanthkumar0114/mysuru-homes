-- Baseline: the schema exactly as Hibernate's ddl-auto: update had built it on the live
-- database, including its generated constraint names, so every environment matches.
-- Existing databases are marked as already at V1 (baseline-on-migrate) and skip this file.

CREATE TABLE users (
    id            VARCHAR(255) NOT NULL,
    created_at    DATETIME(6),
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(255),
    role          ENUM('ADMIN','FIELD_AGENT','OWNER','TENANT') NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT UK6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email)
);

CREATE TABLE listings (
    id                VARCHAR(255) NOT NULL,
    address_line      VARCHAR(255) NOT NULL,
    bathrooms         INT,
    bedrooms          INT,
    created_at        DATETIME(6),
    last_confirmed_at DATETIME(6),
    lat               DOUBLE NOT NULL,
    lng               DOUBLE NOT NULL,
    locality          VARCHAR(255) NOT NULL,
    rent_amount       DECIMAL(38,2) NOT NULL,
    status            ENUM('DRAFT','EXPIRED','LIVE') NOT NULL,
    title             VARCHAR(255) NOT NULL,
    type              ENUM('PG','RENT') NOT NULL,
    verified_at       DATETIME(6),
    owner_id          VARCHAR(255) NOT NULL,
    verified_by_id    VARCHAR(255),
    CONSTRAINT pk_listings PRIMARY KEY (id),
    CONSTRAINT FKawdvt0xd3lqlqyku8qw6m5kou FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT FKf8in9opo4vdwi2o3783vu6dw2 FOREIGN KEY (verified_by_id) REFERENCES users (id)
);

CREATE INDEX idx_listing_status ON listings (status);
CREATE INDEX idx_listing_locality ON listings (locality);

CREATE TABLE listing_photos (
    id         VARCHAR(255) NOT NULL,
    sort_order INT,
    url        VARCHAR(255) NOT NULL,
    listing_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_listing_photos PRIMARY KEY (id),
    CONSTRAINT FK4rf0cj1qkl91h3lq3noemvo8r FOREIGN KEY (listing_id) REFERENCES listings (id)
);

CREATE TABLE enquiries (
    id         VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    listing_id VARCHAR(255) NOT NULL,
    tenant_id  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_enquiries PRIMARY KEY (id),
    CONSTRAINT uk_enquiry_listing_tenant UNIQUE (listing_id, tenant_id),
    CONSTRAINT FK73vvhal47wt5mvdfp2u6ortd1 FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT FKcssi0kqwm8et8tab7ujh09me3 FOREIGN KEY (tenant_id) REFERENCES users (id)
);

CREATE TABLE visit_bookings (
    id         VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    slot_time  DATETIME(6),
    status     ENUM('CANCELLED','COMPLETED','CONFIRMED','REQUESTED'),
    listing_id VARCHAR(255) NOT NULL,
    tenant_id  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_visit_bookings PRIMARY KEY (id),
    CONSTRAINT FKmgv9kjh056jt7p10ru5b665xp FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT FKr7tjo1ilhq7328an250t6qs6o FOREIGN KEY (tenant_id) REFERENCES users (id)
);

CREATE TABLE locality_pages (
    slug          VARCHAR(255) NOT NULL,
    avg_rent      DECIMAL(38,2),
    locality_name VARCHAR(255) NOT NULL,
    seo_content   ${long_text_type},
    CONSTRAINT pk_locality_pages PRIMARY KEY (slug)
);
