delete from user_researcher_category where researcher_category is null;
alter table user_researcher_category modify column researcher_category varchar(255) not null;
alter table user_researcher_category drop foreign key fk_researcher_category_user_id;
alter table user_researcher_category drop primary key;
alter table user_researcher_category add primary key (user_id, researcher_category);
alter table user_researcher_category add constraint fk_researcher_category_user_id foreign key (user_id) references user (user_id);