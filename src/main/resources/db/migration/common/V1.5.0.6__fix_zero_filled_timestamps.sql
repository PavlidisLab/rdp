-- There's a bug in the 1.5.0.4 migration from the releases prior to 1.5.4 that resulted in timestamp columns being
-- initialized with either CURRENT_TIMESTAMP() or zeroes on MySQL. For MySQL 5.7, since only one column can default to
-- CURRENT_TIMESTAMP(), the migration is broken. This is fixed in the 1.5.4 release by explicitly defaulting to NULL.
-- However, if a user ran the that buggy migration, the database will contain zeroed timestamps.

alter table user
    modify column created_at timestamp null default null;
alter table user
    modify column modified_at timestamp null default null;
alter table user
    modify column enabled_at timestamp null default null;
alter table user
    modify column contact_email_verified_at timestamp null default null;
alter table gene
    modify column created_at timestamp null default null;
alter table user_ontology_term
    modify column created_at timestamp null default null;
alter table user_organ
    modify column created_at timestamp null default null;
alter table term
    modify column created_at timestamp null default null;
alter table access_token
    modify column created_at timestamp null default null;
alter table verification_token
    modify column created_at timestamp null default null;
alter table password_reset_token
    modify column created_at timestamp null default null;

-- unfortunately, we cannot fix the other columns
update user
set created_at                = null,
    modified_at               = null,
    enabled_at                = null,
    contact_email_verified_at = null;
update gene
set created_at = null;
update user_ontology_term
set created_at = null;
update user_organ
set created_at = null;
update term
set created_at = null;
update access_token
set created_at = null;
update verification_token
set created_at = null;
update password_reset_token
set created_at = null;