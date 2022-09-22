truncate ontology_term_info_synonyms;
alter table ontology_term_info_synonyms
    modify column synonym varchar(255);