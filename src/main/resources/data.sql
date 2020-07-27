## Default roles
insert ignore into role values (1,'ROLE_ADMIN');
insert ignore into role values (2,'ROLE_USER');

## Default admin password is: defaultadminpwd26
insert ignore into user (user_id, email, enabled, password, privacy_level, description, last_name, name, shared, hide_genelist) values (3, CONCAT(RAND(),"@msl.ubc.ca_notreal"), 0, MD5(RAND()), 0, "Remote admin profile", "", "", false, false);
insert ignore into user_role (user_id,role_id) values (3, 1);
insert ignore into user_role (user_id,role_id) values (3, 2);

## Default taxons
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (9606, "human", "homo sapiens", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/mammalia/homo_sapiens.gene_info.gz", true, 1);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (10090, "mouse", "mus musculus", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/mammalia/mus_musculus.gene_info.gz", false, 2);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (10116, "rat", "rattus norvegicus", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/mammalia/rattus_norvegicus.gene_info.gz", false, 3);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (559292, "yeast", "saccharomyces cerevisiae s288c", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/fungi/saccharomyces_cerevisiae.gene_info.gz", false, 7);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (7955, "zebrafish", "danio rerio", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/non-mammalian_vertebrates/danio_rerio.gene_info.gz", false, 4);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (7227, "fruit fly", "drosophila melanogaster", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/invertebrates/drosophila_melanogaster.gene_info.gz", false, 5);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (6239, "roundworm", "caenorhabditis elegans", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/invertebrates/caenorhabditis_elegans.gene_info.gz", false, 6);
insert ignore into taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) values (511145, "e. coli", "escherichia coli", "ftp://ftp.ncbi.nlm.nih.gov/gene/data/gene_info/archaea_bacteria/escherichia_coli_str._k-12_substr._mg1655.gene_info.gz", false, 8);

## Default organ systems for Hoomans
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "appendages", "Limb/Appendage");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "cardiovascular", "Cardiovascular");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "eye_and_ear", "Eye and Ear");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "head_and_neck", "Head and Neck");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "musculature", "Musculature");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "nervous_system", "Nervous System");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "respiratory", "Respiratory");
insert ignore into organ_info (taxon_id, symbol, name) values (9606, "skeletal", "Skeletal and Connective Tissue");
