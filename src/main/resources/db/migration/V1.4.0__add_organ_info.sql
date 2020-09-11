alter table user add column privacy_level integer;
alter table gene add column user_privacy_level integer default NULL;
create table organ_info (id integer not null auto_increment, description TEXT, name TEXT, uberon_id varchar(14), active bit, ordering integer, primary key (id));
create table user_organ (id integer not null auto_increment, description TEXT, name TEXT, uberon_id varchar(14), user_id integer not null, primary key (id));
alter table organ_info add constraint UK3gxhitw1572dhfctt2mjulc45 unique (uberon_id);
alter table user_organ add constraint UKt09aoc8fqrshueqnau3m5423i unique (user_id, uberon_id);
alter table user_organ add constraint FKtmll6kbthwqqh53034wn9i220 foreign key (user_id) references user (user_id);
alter table ortholog drop column target_taxon;