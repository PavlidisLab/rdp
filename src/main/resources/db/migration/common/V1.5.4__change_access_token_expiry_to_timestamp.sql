alter table access_token
    modify column expiry_date timestamp not null;