
-- username: administrator, password: changemeadmin
insert into USER (ID, FIRSTNAME, LASTNAME, USERNAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (1, "administrator",  "", "administrator", "7920f75bb07e1d15aa8a42238eec19774fbc28de", 1, "email@email.ca", "hint");
-- username: aspiredbAgent, password: aspagent4
insert into USER (ID, FIRSTNAME, LASTNAME, USERNAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (2, "aspiredbAgent",  "", "aspiredbAgent", "2eb2e0ac05fa979005a81e7abbe42d01b0f4f829", 1, "email@email.ca", "hint");
-- username: user, password: changeme
insert into USER (ID, FIRSTNAME, LASTNAME, USERNAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (3, "user",  "", "user", "ba8de2e66e08abf7f5f9f90cced8533bdeb1fcc1", 1, "email@chibi.ubc.ca", "hint");




-- Note that 'Administrators' is a constant set in AuthorityConstants.
insert into USER_GROUP (ID, NAME, DESCRIPTION) VALUES (1, "Administrators", "Users with administrative rights");
insert into USER_GROUP (ID, NAME, DESCRIPTION) VALUES (2, "Users", "Default group for all authenticated users");
insert into USER_GROUP (ID, NAME, DESCRIPTION) VALUES (3, "Agents", "For 'autonomous' agents that run within the server context, such as scheduled tasks.");
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (1, "ADMIN",1);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (2, "USER", 2);
insert into GROUP_AUTHORITY (ID, AUTHORITY, GROUP_FK) VALUES (3, "AGENT",3);

insert into GROUP_MEMBERS (USER_GROUP_FK, GROUP_MEMBERS_FK) VALUES (1, 1);

-- add admin to the user group (note that there is no need for a corresponding ACL entry)
insert into GROUP_MEMBERS (USER_GROUP_FK, GROUP_MEMBERS_FK) VALUES (2, 1);

-- add agent to the agent group
insert into GROUP_MEMBERS (USER_GROUP_FK, GROUP_MEMBERS_FK) VALUES (3, 2);

-- add user  to the user group (note that there is no need for a corresponding ACL entry)
insert into GROUP_MEMBERS (USER_GROUP_FK, GROUP_MEMBERS_FK) VALUES (2, 3);
