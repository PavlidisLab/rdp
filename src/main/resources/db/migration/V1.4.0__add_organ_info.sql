create table gene_info (id integer not null auto_increment, synonyms TEXT, gene_id integer, modification_date date, description TEXT, symbol varchar(63), taxon_id integer not null, primary key (id));
create index IDXsu8xm3u7b6aod4nx4ygbd61rj on gene_info (gene_id);
create index IDX5yu2eylrxdoynsllo3dls8c4h on gene_info (gene_id, taxon_id);
create index IDXqadnox9xnoxkennvla4nxito4 on gene_info (symbol, taxon_id);
alter table gene_info add constraint UKsu8xm3u7b6aod4nx4ygbd61rj unique (gene_id);
alter table gene_info add constraint FKjhrcx93vfadlsujyvmudxc7dg foreign key (taxon_id) references taxon (taxon_id);

-- enforce some non-nullable bit fields
update user set enabled = false where enabled is null;
alter table user modify column enabled bit not null;
update user set hide_genelist = true where hide_genelist is null;
alter table user modify column hide_genelist bit not null;
update user set shared = false where shared is null;
alter table user modify column shared bit not null;

alter table user add column researcher_position varchar(255);
alter table user add column contact_email varchar(255);
alter table user add column contact_email_verified bit not null;
alter table user drop column origin;
alter table user drop column origin_url;

alter table gene add column user_privacy_level integer default NULL;

-- the previous ortholog table was using gene identifier instead of the primary key of the gene_info table, so it's
-- unuseable in this release
drop table ortholog;
create table ortholog (source_gene integer not null, target_gene integer not null, primary key (source_gene, target_gene));
alter table ortholog add constraint fk_ortholog_source_gene foreign key (source_gene) references gene_info (id);
alter table ortholog add constraint fk_ortholog_target_gene foreign key (target_gene) references gene_info (id);

create table user_researcher_category (user_id integer not null auto_increment, researcher_category varchar(255), primary key (user_id));
alter table user_researcher_category add constraint fk_researcher_category_user_id foreign key (user_id) references user (user_id);

-- add service account role to the next available id
insert into role values (NULL, 'ROLE_SERVICE_ACCOUNT');

create table access_token (id integer not null auto_increment, expiry_date datetime not null, token varchar(255) not null, user_id integer not null, primary key (id));
alter table access_token add constraint fk_access_token_user_id foreign key (user_id) references user (user_id);
alter table access_token add constraint uk_access_token_token unique (token);

-- migrate password reset token expiry date to non-nullable timestamps
alter table verification_token modify column expiry_date timestamp not null;
alter table password_reset_token modify column expiry_date timestamp not null;
update password_reset_token set expiry_date = CURRENT_TIMESTAMP() where expiry_date is null;
update verification_token set expiry_date = CURRENT_TIMESTAMP() where expiry_date is null;

-- enforce non-nullable tokens remove null tokens
delete from verification_token where token is null;
alter table verification_token modify column token varchar(255) not null;
delete from password_reset_token where token is null;
alter table password_reset_token modify column token varchar(255) not null;

-- make token uniques and indexed
alter table verification_token add constraint uk_verification_token_token unique (token);
alter table password_reset_token add constraint uk_password_reset_token_token unique (token);

-- add an email to the verification token
alter table verification_token add column email varchar(255) not null;
update verification_token set email = (select email from user where user.user_id = verification_token.user_id);

-- organ systems
create table organ_info (id integer not null auto_increment, description TEXT, name TEXT, uberon_id varchar(14), active bit not null, ordering integer, primary key (id));
create table user_organ (id integer not null auto_increment, description TEXT, name TEXT, uberon_id varchar(14), user_id integer not null, primary key (id));
alter table organ_info add constraint UK3gxhitw1572dhfctt2mjulc45 unique (uberon_id);
alter table user_organ add constraint UKt09aoc8fqrshueqnau3m5423i unique (user_id, uberon_id);
alter table user_organ add constraint FKtmll6kbthwqqh53034wn9i220 foreign key (user_id) references user (user_id);

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

-- Add commmon frog and fission yeast by default
-- insert frog at position 5
update taxon set ordering = ordering + 1 where ordering >= 5;
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (8364, 'frog', 'Xenopus tropicalis', 'ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Non-mammalian_vertebrates/Xenopus_tropicalis.gene_info.gz', false, 5);
-- insert fission yeast at position 9
update taxon set ordering = ordering + 1 where ordering >= 9;
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (4896, 'fission yeast', 'Schizosaccharomyces pombe', 'ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Fungi/All_Fungi.gene_info.gz', false, 9);

-- Update budding yeast common name
update taxon set common_name = 'budding yeast' where taxon_id = 559292;
update taxon set scientific_name = 'Saccharomyces cerevisiae' where taxon_id = 559292;
