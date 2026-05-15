drop index if exists ix_establishment_id_status_app_type;

drop index if exists ix_establishment_id_status_app_type_requested_by;

drop index if exists ix_establishment_id_status_app_type_assigned_group;

drop index if exists ix_establishment_id_status_app_type_requested_by_assigned_group;



create index if not exists ix_establishment_id_status_application_type on app using btree (establishment_id, status, application_type);

create index if not exists ix_establishment_id_status_application_type_requested_by on app using btree (establishment_id, status, application_type, requested_by);

create index if not exists ix_establishment_id_status_application_type_assigned_group on app using btree (establishment_id, status, application_type, assigned_group);

create index if not exists ix_establishment_id_status_application_type_requested_by_assigned_group on app using btree (establishment_id, status, application_type, requested_by, assigned_group);
