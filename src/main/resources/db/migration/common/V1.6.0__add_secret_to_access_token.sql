alter table access_token
    add column secret varchar(255); -- may be null for pre-1.6 tokens