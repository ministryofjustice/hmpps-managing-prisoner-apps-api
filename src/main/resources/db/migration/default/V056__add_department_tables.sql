CREATE TABLE if not EXISTS department (
    id uuid NOT NULL,
    "name" varchar(255) NOT NULL,
    slug varchar(255) NOT NULL,
    CONSTRAINT department_id_not_null NOT NULL id,
    CONSTRAINT department_name_key UNIQUE (name),
    CONSTRAINT department_name_not_null NOT NULL name,
    CONSTRAINT department_pkey PRIMARY KEY (id),
    CONSTRAINT department_slug_key UNIQUE (slug),
    CONSTRAINT department_slug_not_null NOT NULL slug
);

CREATE TABLE if not exists establishment_departments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    department_id uuid NULL,
    establishment_id varchar(255) NOT NULL,
    active bool DEFAULT false NOT NULL,
    CONSTRAINT establishment_departments_active_not_null NOT NULL active,
    CONSTRAINT establishment_departments_establishment_id_not_null NOT NULL establishment_id,
    CONSTRAINT establishment_departments_id_not_null NOT NULL id,
    CONSTRAINT establishment_departments_pkey PRIMARY KEY (id)
);

ALTER TABLE if exists establishment_departments DROP CONSTRAINT if exists establishment_departments_department_fk;
ALTER TABLE if exists establishment_departments DROP CONSTRAINT if exists establishment_departments_establishment_fk;
ALTER TABLE if exists establishment_departments DROP CONSTRAINT if exists establishment_departments_unique;

ALTER TABLE if EXISTS establishment_departments ADD CONSTRAINT establishment_departments_department_fk FOREIGN KEY (department_id) REFERENCES public.department(id);
ALTER TABLE if EXISTS establishment_departments ADD CONSTRAINT establishment_departments_establishment_fk FOREIGN KEY (establishment_id) REFERENCES public.establishment(id);
ALTER TABLE if EXISTS establishment_departments ADD CONSTRAINT establishment_departments_unique UNIQUE (department_id,establishment_id);
