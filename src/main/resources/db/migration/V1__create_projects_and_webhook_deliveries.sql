CREATE TABLE projects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    github_repo_id BIGINT NOT NULL UNIQUE,
    repo_name VARCHAR(100) NOT NULL UNIQUE,
    html_url VARCHAR(255) NOT NULL,
    title JSONB NOT NULL,
    description JSONB NOT NULL,
    stack JSONB NOT NULL DEFAULT '[]'::jsonb,
    demo_url VARCHAR(255),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    pushed_at TIMESTAMPTZ NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_projects_title_languages CHECK (title ? 'es' AND title ? 'en'),
    CONSTRAINT ck_projects_description_languages CHECK (description ? 'es' AND description ? 'en'),
    CONSTRAINT ck_projects_stack_is_array CHECK (jsonb_typeof(stack) = 'array')
);

CREATE TABLE webhook_deliveries (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    delivery_id UUID NOT NULL UNIQUE,
    event VARCHAR(50) NOT NULL,
    github_repo_id BIGINT,
    repo_name VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,

    CONSTRAINT ck_webhook_deliveries_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED'))
);
