-- this is part of the 1.5.0 migration but has to be versioned such that it runs after the table creation
alter table ontology_term_info_synonyms
    modify column synonym varchar(255) binary;

-- add full text search indices
create fulltext index ft_ontology_term_info_name on ontology_term_info (name);
create fulltext index ft_ontology_term_info_synonym on ontology_term_info_synonyms (synonym);
create fulltext index ft_ontology_term_info_definition on ontology_term_info (definition);