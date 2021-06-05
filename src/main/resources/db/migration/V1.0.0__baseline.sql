create table descriptions (user_id integer not null, description TEXT, taxon_id integer not null, primary key (user_id, taxon_id));
create table gene (id integer not null auto_increment, synonyms TEXT, gene_id integer, modification_date int(11), description TEXT, symbol varchar(63), tier varchar(5), taxon_id integer not null, user_id integer not null, primary key (id));
create table ortholog (source_gene integer not null, target_gene integer not null, target_taxon integer not null, primary key (source_gene, target_gene, target_taxon));
create table password_reset_token (id integer not null auto_increment, expiry_date datetime, token varchar(255), user_id integer not null, primary key (id));
create table publication (publication_id integer not null auto_increment, pmid integer, title varchar(255), user_id integer, primary key (publication_id));
create table role (role_id integer not null auto_increment, role varchar(255), primary key (role_id));
create table taxon (taxon_id integer not null, active bit not null, common_name varchar(255), gene_url varchar(255), ordering integer, scientific_name varchar(255), primary key (taxon_id));
create table term (id integer not null auto_increment, aspect varchar(255), definition TEXT, go_id varchar(10), name TEXT, frequency integer, size integer, taxon_id integer, user_id integer, primary key (id));
create table user (user_id integer not null auto_increment, email varchar(255) not null, enabled bit, origin varchar(255), origin_url varchar(255), password varchar(255) not null, privacy_level integer, department varchar(255), description TEXT, hide_genelist bit, last_name varchar(255), name varchar(255), organization varchar(255), phone varchar(255), shared bit, website varchar(255), primary key (user_id));
create table user_role (user_id integer not null, role_id integer not null, primary key (user_id, role_id));
create table verification_token (id integer not null auto_increment, expiry_date datetime, token varchar(255), user_id integer not null, primary key (id));
create index IDXrj5dlny39nvsq3ebw4uftgsja on gene (gene_id);
create index gene_id_tier_hidx on gene (gene_id, tier);
create index symbol_taxon_id_tier_hidx on gene (symbol, taxon_id, tier);
alter table gene add constraint UKl4j6xdfhifrfq30mo35umcioe unique (user_id, gene_id);
create index go_id_hidx on term (go_id);
alter table term add constraint UKckgp405x8rt6mqki18jc5wx36 unique (user_id, taxon_id, go_id);
alter table descriptions add constraint FKd8jpgy0bmjsbwqegqqjgb9cif foreign key (taxon_id) references taxon (taxon_id);
alter table descriptions add constraint FK6501475nsrwi4tucfi8mfjr0h foreign key (user_id) references user (user_id);
alter table gene add constraint FKtqhagy6kmkx36gh9w2ku5g9gf foreign key (taxon_id) references taxon (taxon_id);
alter table gene add constraint FKfitav6vg0nilrvfwrqdtbvgh2 foreign key (user_id) references user (user_id);
alter table password_reset_token add constraint FK5lwtbncug84d4ero33v3cfxvl foreign key (user_id) references user (user_id);
alter table publication add constraint FKq2ei3a07e3ln96uel4alss2u7 foreign key (user_id) references user (user_id);
alter table term add constraint FK3p6e5vb4ri2ekg1ct1aoq57md foreign key (taxon_id) references taxon (taxon_id);
alter table term add constraint FKktl98gic60ehb8miresv21f12 foreign key (user_id) references user (user_id);
alter table user_role add constraint FKa68196081fvovjhkek5m97n3y foreign key (role_id) references role (role_id);
alter table user_role add constraint FK859n2jvi8ivhui0rl0esws6o foreign key (user_id) references user (user_id);
alter table verification_token add constraint FKrdn0mss276m9jdobfhhn2qogw foreign key (user_id) references user (user_id);

-- Default roles
insert into role values (1,'ROLE_ADMIN');
insert into role values (2,'ROLE_USER');

-- Default taxon
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (9606, 'human', 'Homo sapiens', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz', true, 1);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (10090, 'mouse', 'Mus musculus', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Mus_musculus.gene_info.gz', false, 2);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (10116, 'rat', 'Rattus norvegicus', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Rattus_norvegicus.gene_info.gz', false, 3);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (7955, 'zebrafish', 'Danio rerio', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Non-mammalian_vertebrates/Danio_rerio.gene_info.gz', false, 4);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (7227, 'fruit fly', 'Drosophila melanogaster', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Drosophila_melanogaster.gene_info.gz', false, 5);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (6239, 'roundworm', 'Caenorhabditis elegans', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Caenorhabditis_elegans.gene_info.gz', false, 6);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (559292, 'yeast', 'Saccharomyces cerevisiae S288c', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Fungi/Saccharomyces_cerevisiae.gene_info.gz', false, 7);
insert into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (511145, 'e. coli', 'Escherichia coli', 'ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Archaea_Bacteria/Escherichia_coli_str._K-12_substr._MG1655.gene_info.gz', false, 8);