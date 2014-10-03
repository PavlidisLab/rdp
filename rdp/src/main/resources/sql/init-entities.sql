
-- username: administrator, password: changemeadmin
insert into USER (ID, FIRSTNAME, LASTNAME, USERNAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (1, "administrator",  "", "administrator", "$2a$10$rOj2xdKqWiHh0Dtm1KoVmeI0K0cEu4iqqr/iuamgklA3TMoab9NIe", 1, "administrator@email.ca", "hint");
-- username: aspiredbAgent, password: aspagent4
insert into USER (ID, FIRSTNAME, LASTNAME, USERNAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (2, "aspiredbAgent",  "", "aspiredbAgent", "$2a$10$g9wNokjCk.pv1iiIn2YWnOnQ1hbb5xJbb4qt6u1KGoaACT6sZrjz.", 1, "aspiredbAgent@email.ca", "hint");
-- username: user, password: changeme
insert into USER (ID, FIRSTNAME, LASTNAME, USERNAME, PASSWORD, ENABLED, EMAIL, PASSWORD_HINT) values (3, "user",  "", "user", "$2a$10$Wym8DlT/8LLue8RUXt44r.6.WIaJGu0ghoxLoHGMlVUxVDAsV1wuK", 1, "user@chibi.ubc.ca", "hint");




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
