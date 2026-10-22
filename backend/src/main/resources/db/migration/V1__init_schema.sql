-- =====================================================================
-- GymApp : schéma initial
-- Modèle (objectifs du coach) : routine, routine_workout, routine_exercise, routine_set
-- Exécution (réel du client)  : workout_session, session_exercise, session_set
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------
-- Utilisateurs
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           varchar(255) NOT NULL,
    password_hash   varchar(100) NOT NULL,
    role            varchar(10)  NOT NULL CHECK (role IN ('COACH', 'CLIENT')),
    first_name      varchar(100) NOT NULL,
    last_name       varchar(100) NOT NULL,
    coach_id        bigint REFERENCES app_user (id) ON DELETE RESTRICT,
    active          boolean      NOT NULL DEFAULT true,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_coach CHECK (
        (role = 'CLIENT' AND coach_id IS NOT NULL) OR
        (role = 'COACH'  AND coach_id IS NULL))
);
CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));
CREATE INDEX ix_app_user_coach ON app_user (coach_id) WHERE coach_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Catalogue d'exercices (import du dataset hasaneyldrm/exercises-dataset)
-- ---------------------------------------------------------------------
CREATE TABLE exercise (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id        varchar(10)  NOT NULL UNIQUE,
    name               varchar(200) NOT NULL,
    body_part          varchar(50)  NOT NULL,
    target             varchar(50),
    muscle_group       varchar(50),
    secondary_muscles  text[]       NOT NULL DEFAULT '{}',
    equipment          varchar(50),
    instructions       jsonb        NOT NULL DEFAULT '{}',
    instruction_steps  jsonb        NOT NULL DEFAULT '{}',
    image_path         varchar(255) NOT NULL,
    gif_path           varchar(255) NOT NULL,
    synced_at          timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_exercise_name_trgm ON exercise USING gin (name gin_trgm_ops);
CREATE INDEX ix_exercise_body_part ON exercise (body_part);
CREATE INDEX ix_exercise_equipment ON exercise (equipment);

-- ---------------------------------------------------------------------
-- Modèle : objectifs du coach
-- ---------------------------------------------------------------------
CREATE TABLE routine (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    client_id    bigint NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    coach_id     bigint NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    name         varchar(150) NOT NULL,
    description  text,
    status       varchar(10) NOT NULL DEFAULT 'DRAFT'
                 CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_routine_client ON routine (client_id);
CREATE INDEX ix_routine_coach  ON routine (coach_id);
CREATE UNIQUE INDEX ux_routine_one_active ON routine (client_id) WHERE status = 'ACTIVE';

CREATE TABLE routine_workout (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_id  bigint NOT NULL REFERENCES routine (id) ON DELETE CASCADE,
    position    smallint NOT NULL CHECK (position >= 1),
    name        varchar(150) NOT NULL,
    CONSTRAINT ux_routine_workout_pos UNIQUE (routine_id, position)
        DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE routine_exercise (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_workout_id  bigint NOT NULL REFERENCES routine_workout (id) ON DELETE CASCADE,
    exercise_id         bigint NOT NULL REFERENCES exercise (id) ON DELETE RESTRICT,
    position            smallint NOT NULL CHECK (position >= 1),
    rest_seconds        smallint NOT NULL CHECK (rest_seconds BETWEEN 0 AND 900),
    coach_tip           text,
    CONSTRAINT ux_routine_exercise_pos UNIQUE (routine_workout_id, position)
        DEFERRABLE INITIALLY DEFERRED
);
CREATE INDEX ix_routine_exercise_exercise ON routine_exercise (exercise_id);

CREATE TABLE routine_set (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    routine_exercise_id  bigint NOT NULL REFERENCES routine_exercise (id) ON DELETE CASCADE,
    set_number           smallint NOT NULL CHECK (set_number >= 1),
    target_reps          smallint NOT NULL CHECK (target_reps > 0),
    target_weight_kg     numeric(6,2) CHECK (target_weight_kg >= 0),
    CONSTRAINT ux_routine_set_num UNIQUE (routine_exercise_id, set_number)
        DEFERRABLE INITIALLY DEFERRED
);

-- ---------------------------------------------------------------------
-- Exécution : réel du client
-- ---------------------------------------------------------------------
CREATE TABLE workout_session (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    client_id           bigint NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    routine_workout_id  bigint REFERENCES routine_workout (id) ON DELETE SET NULL,
    workout_name        varchar(150) NOT NULL,
    status              varchar(12) NOT NULL DEFAULT 'IN_PROGRESS'
                        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABORTED')),
    started_at          timestamptz NOT NULL DEFAULT now(),
    ended_at            timestamptz,
    CONSTRAINT chk_session_end CHECK (
        (status = 'IN_PROGRESS' AND ended_at IS NULL) OR
        (status <> 'IN_PROGRESS' AND ended_at IS NOT NULL))
);
CREATE INDEX ix_session_client_started ON workout_session (client_id, started_at DESC);
CREATE INDEX ix_session_routine_workout ON workout_session (routine_workout_id);
CREATE UNIQUE INDEX ux_session_one_in_progress ON workout_session (client_id)
    WHERE status = 'IN_PROGRESS';

CREATE TABLE session_exercise (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id           bigint NOT NULL REFERENCES workout_session (id) ON DELETE CASCADE,
    routine_exercise_id  bigint REFERENCES routine_exercise (id) ON DELETE SET NULL,
    exercise_id          bigint NOT NULL REFERENCES exercise (id) ON DELETE RESTRICT,
    position             smallint NOT NULL,
    rest_seconds         smallint NOT NULL,
    coach_tip            text,
    CONSTRAINT ux_session_exercise_pos UNIQUE (session_id, position)
);
CREATE INDEX ix_session_exercise_exercise ON session_exercise (exercise_id);
CREATE INDEX ix_session_exercise_routine_exercise ON session_exercise (routine_exercise_id);

CREATE TABLE session_set (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_exercise_id  bigint NOT NULL REFERENCES session_exercise (id) ON DELETE CASCADE,
    set_number           smallint NOT NULL,
    target_reps          smallint NOT NULL,
    target_weight_kg     numeric(6,2),
    actual_reps          smallint CHECK (actual_reps >= 0),
    actual_weight_kg     numeric(6,2) CHECK (actual_weight_kg >= 0),
    completed            boolean NOT NULL DEFAULT false,
    completed_at         timestamptz,
    CONSTRAINT ux_session_set_num UNIQUE (session_exercise_id, set_number),
    CONSTRAINT chk_set_completed CHECK (completed = (completed_at IS NOT NULL))
);
