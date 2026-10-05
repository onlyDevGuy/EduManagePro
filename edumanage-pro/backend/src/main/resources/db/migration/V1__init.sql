-- =============================================================
-- EduManage Pro — initial schema
-- Multi-tenancy model: shared schema, tenant_id discriminator.
-- Every tenant-owned table carries tenant_id and is indexed on it.
-- =============================================================

CREATE TABLE tenants (
    id                 VARCHAR(36)  PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    slug               VARCHAR(80)  NOT NULL,
    stripe_customer_id VARCHAR(120),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_tenants_slug UNIQUE (slug)
);

CREATE TABLE students (
    id             VARCHAR(36) PRIMARY KEY,
    tenant_id      VARCHAR(36) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(180),
    date_of_birth  DATE,
    guardian_name  VARCHAR(200),
    guardian_email VARCHAR(180),
    guardian_phone VARCHAR(40),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_students_tenant ON students (tenant_id);
CREATE INDEX ix_students_tenant_name ON students (tenant_id, last_name, first_name);

CREATE TABLE users (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    student_id    VARCHAR(36)  REFERENCES students(id) ON DELETE SET NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email)
);
CREATE INDEX ix_users_tenant ON users (tenant_id);

CREATE TABLE courses (
    id          VARCHAR(36) PRIMARY KEY,
    tenant_id   VARCHAR(36) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code        VARCHAR(40)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    teacher_id  VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    fee_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'USD',
    start_date  DATE,
    end_date    DATE,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_courses_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX ix_courses_tenant ON courses (tenant_id);

CREATE TABLE enrollments (
    id          VARCHAR(36) PRIMARY KEY,
    tenant_id   VARCHAR(36) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    student_id  VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id   VARCHAR(36) NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enrolled_on DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_enrollments UNIQUE (tenant_id, student_id, course_id)
);
CREATE INDEX ix_enrollments_course  ON enrollments (tenant_id, course_id);
CREATE INDEX ix_enrollments_student ON enrollments (tenant_id, student_id);

CREATE TABLE attendance_records (
    id           VARCHAR(36) PRIMARY KEY,
    tenant_id    VARCHAR(36) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    course_id    VARCHAR(36) NOT NULL REFERENCES courses(id)  ON DELETE CASCADE,
    student_id   VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    session_date DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    note         VARCHAR(300),
    recorded_by  VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_attendance UNIQUE (tenant_id, course_id, student_id, session_date)
);
CREATE INDEX ix_attendance_session ON attendance_records (tenant_id, course_id, session_date);
CREATE INDEX ix_attendance_student ON attendance_records (tenant_id, student_id);

CREATE TABLE invoices (
    id                           VARCHAR(36) PRIMARY KEY,
    tenant_id                    VARCHAR(36) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    number                       VARCHAR(40) NOT NULL,
    student_id                   VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    description                  VARCHAR(300) NOT NULL,
    amount                       NUMERIC(12,2) NOT NULL,
    currency                     VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status                       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    issue_date                   DATE          NOT NULL,
    due_date                     DATE          NOT NULL,
    paid_at                      TIMESTAMPTZ,
    stripe_checkout_session_id   VARCHAR(200),
    stripe_payment_intent_id     VARCHAR(200),
    created_at                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_invoices_tenant_number UNIQUE (tenant_id, number)
);
CREATE INDEX ix_invoices_tenant_status  ON invoices (tenant_id, status);
CREATE INDEX ix_invoices_tenant_student ON invoices (tenant_id, student_id);
CREATE UNIQUE INDEX ux_invoices_checkout_session
    ON invoices (stripe_checkout_session_id) WHERE stripe_checkout_session_id IS NOT NULL;