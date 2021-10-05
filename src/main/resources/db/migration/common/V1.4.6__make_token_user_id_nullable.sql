-- I'm not exactly sure why this is necessary, but Hibernate set this column to NULL before removing it
alter table access_token modify column user_id integer null;
alter table password_reset_token modify column user_id integer null;
alter table verification_token modify column user_id integer null;