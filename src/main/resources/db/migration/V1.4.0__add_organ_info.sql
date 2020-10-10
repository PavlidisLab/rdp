alter table user add column researcher_position varchar(255);
alter table user add column researcher_category varchar(255);
alter table term drop column frequency;
alter table term drop column size;
alter table gene add column user_privacy_level integer default NULL;
create table organ_info (id integer not null auto_increment, description TEXT, name TEXT, uberon_id varchar(14), active bit, ordering integer, primary key (id));
create table user_organ (id integer not null auto_increment, description TEXT, name TEXT, uberon_id varchar(14), user_id integer not null, primary key (id));
alter table organ_info add constraint UK3gxhitw1572dhfctt2mjulc45 unique (uberon_id);
alter table user_organ add constraint UKt09aoc8fqrshueqnau3m5423i unique (user_id, uberon_id);
alter table user_organ add constraint FKtmll6kbthwqqh53034wn9i220 foreign key (user_id) references user (user_id);
alter table ortholog drop primary key;
alter table ortholog add primary key (source_gene, target_gene);
alter table ortholog drop column target_taxon;

-- Default organ systems
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0000026', 'Limb/Appendage', true, 1);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0004535', 'Cardiovascular', true, 2);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0000970', 'Eye and Ear', true, 3);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0000033', 'Head and Neck', true, 4);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0001015', 'Musculature', true, 5);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0001016', 'Nervous System', true, 6);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0001004', 'Respiratory System', true, 7);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0004755', 'Skeletal and Connective Tissue', true, 8);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0000178', 'Blood and Immune', true, 9);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0000949', 'Endocrine System', true, 10);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0002199', 'Intergument', true, 11);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0001007', 'Digestive System', true, 12);
insert into organ_info (uberon_id, name, active, ordering) values ('UBERON:0004122', 'Genitourinary', true, 13);