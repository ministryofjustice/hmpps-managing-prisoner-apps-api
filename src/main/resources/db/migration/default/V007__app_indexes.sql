create index ix_establishment_id_status_app_type on app using btree (establishment_id, status, app_type);

create index ix_establishment_id_status_requested_by on app using btree (establishment_id, status, requested_by);

create index ix_establishment_id_status_assigned_group on app using btree (establishment_id, status, assigned_group);

create index ix_establishment_id_status_app_type_requested_by on app using btree (establishment_id, status, app_type, requested_by);

create index ix_establishment_id_status_app_type_assigned_group on app using btree (establishment_id, status, app_type, assigned_group);

create index ix_establishment_id_status_requested_by_assigned_group on app using btree (establishment_id, status, requested_by, assigned_group);

create index ix_establishment_id_status_app_type_requested_by_assigned_group on app using btree (establishment_id, status, app_type, requested_by, assigned_group);
