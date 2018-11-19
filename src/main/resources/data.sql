INSERT IGNORE INTO role VALUES (1,'ROLE_ADMIN');
INSERT IGNORE INTO role VALUES (2,'ROLE_USER');

INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (9606, "human", "Homo sapiens", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz", true);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (10090, "mouse", "Mus musculus", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Mus_musculus.gene_info.gz", false);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (10116, "rat", "Rattus norvegicus", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Rattus_norvegicus.gene_info.gz", false);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (559292, "yeast", "Saccharomyces cerevisiae S288c", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Fungi/Saccharomyces_cerevisiae.gene_info.gz", false);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (7955, "zebrafish", "Danio rerio", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Non-mammalian_vertebrates/Danio_rerio.gene_info.gz", false);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (7227, "fruit fly", "Drosophila melanogaster", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Drosophila_melanogaster.gene_info.gz", false);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (6239, "roundworm", "Caenorhabditis elegans", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Caenorhabditis_elegans.gene_info.gz", false);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active) VALUES (511145, "e. coli", "Escherichia coli", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Archaea_Bacteria/Escherichia_coli_str._K-12_substr._MG1655.gene_info.gz", false);