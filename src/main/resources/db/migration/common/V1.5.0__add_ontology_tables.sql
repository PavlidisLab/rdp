create table ontology
(
    ontology_id  integer      not null auto_increment,
    name         varchar(255) not null,
    definition   text,
    ontology_url varchar(255),
    active       bit          not null,
    ordering     integer,
    primary key (ontology_id)
);
alter table ontology
    add constraint uk_ontology_name unique (name);

create table ontology_term_info
(
    ontology_term_info_id integer      not null auto_increment,
    ontology_id           integer      not null,
    term_id               varchar(255) not null,
    name                  varchar(255),
    definition            text,
    obsolete              bit          not null,
    active                bit          not null,
    ordering              integer,
    -- not sure we'll keep the following
    is_group              bit          not null,
    has_icon              bit          not null,
    primary key (ontology_term_info_id)
);
alter table ontology_term_info
    add constraint fk_ontology_term_info_ontology foreign key (ontology_id) references ontology (ontology_id);
alter table ontology_term_info
    add constraint uk_ontology_term_info_ontology_term unique (ontology_id, term_id);

create table ontology_term_info_sub_terms
(
    ontology_term_info_id     integer not null,
    ontology_sub_term_info_id integer not null,
    primary key (ontology_term_info_id, ontology_sub_term_info_id)
);
alter table ontology_term_info_sub_terms
    add constraint fk_ontology_term_info_sub_terms_term_info foreign key (ontology_term_info_id) references ontology_term_info (ontology_term_info_id);
alter table ontology_term_info_sub_terms
    add constraint fk_ontology_term_info_sub_terms_subterm_info foreign key (ontology_sub_term_info_id) references ontology_term_info (ontology_term_info_id);

create table user_ontology_term
(
    user_ontology_term_id integer      not null auto_increment,
    ontology_id           integer      not null,
    term_id               varchar(255) not null,
    user_id               integer      not null,
    name                  varchar(255),
    primary key (user_ontology_term_id)
);
alter table user_ontology_term
    add constraint fk_user_ontology_term_ontology foreign key (ontology_id) references ontology (ontology_id);
alter table user_ontology_term
    add constraint fk_user_ontology_term_user foreign key (user_id) references user (user_id);
alter table user_ontology_term
    add constraint uk_user_ontology_term_ontology_term_user unique (ontology_id, term_id, user_id)