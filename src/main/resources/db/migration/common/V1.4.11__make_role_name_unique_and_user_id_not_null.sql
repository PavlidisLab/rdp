alter table role add constraint uk_role_role unique (role);
alter table access_token modify column user_id integer not null;
alter table password_reset_token modify column user_id integer not null;
alter table verification_token modify column user_id integer not null;
alter table term modify column user_id integer not null;
