# Migrate from previous releases

Your current data should not be lost while migrating, but you should definitely
have a database backup in case things go wrong for any reason. Read about [database backup options for MySQL](https://dev.mysql.com/doc/refman/5.7/en/backup-methods.html).

First, take down your running instance to make sure that the previous release
will not interfere with the migration process.

Then, run the newer application release with the following parameter in
`application.properties`:

```
spring.datasource.initialize=true
```

which will update the database to support the latest model definitions.

If you get any errors during any part of the following process, please contact
us.

Once the update is successful, you should revert the option back to `false` to
speed up future restarts.

Take a look in the sections below for version-specific migration procedures.

As of 1.3.2, database migrations are automated with Flyway which will run at startup of the application. This behaviour
can be disabled with the following parameter in `application.properties`:

```
flyway.enabled=false
```

## Migrate from 1.3 to 1.4

Gene informations are now stored in the database. As a result, taxons are known
for ortholog genes and the `ortholog_taxon` column has become unnecessary, and
a source of error for Hibernate.

 - [V1.4.0__drop_ortholog_target_taxon.sql](src/main/resources/db/migration/V1.4.0__drop_ortholog_target_taxon.sql)

## Migrate from 1.3.x to 1.3.2

NCBI gene broke because they introduced genes with unexpected date format. We
adjusted our parsing code to process date formats and now store a `DATE` type
in the database.

 - [V1.3.2__fix_gene_modification_date_type.sql](src/main/resources/db/migration/V1.3.2__fix_gene_modification_date_type.sql)

## Migrate from version 1.1.x to 1.2

There are new security settings that can be added to your `application.properties` file.
See the section 'Privacy and search Defaults' in the 'Customize Settings' example file.

The new security settings will have to be back-filled for the users that have
registered prior to this update. This can be done by directly editing the
database entries like so:

```sql
update user set hide_genelist = 0, privacy_level = 0, shared = 1;
```

The values of these settings should correspond with the defaults you have set in your `application-prod.properties` file.
Specifically:
 - `hide_genelist = X` if X =1, hides users gene list from public searches. is only effective when the setting `rdp.settings.privacy.allow-hide-genelist` is enabled.
 - `privacy_level = X` where X is the privacy level. Should have the same value as `rdp.settings.privacy.default-level`, or whatever the users prior to this update agreed to.
 - `shared = X` where X is 1 or 0. Corresponds to `rdp.settings.privacy.default-sharing`

#### Registered user search in previous version
If you previously had search enabled for registered users, you want to set `privacy_level` to `1` and `rdp.settings.privacy.default-level` to `true`.

The original system for enabling registered users to use the search function was based on assigning a different role to all users. This has been discontinued, and needs to be manually switched for all existing users.
This can be easily done by running the following command on your database (provided you have the original set of roles that came with the application,
where ROLE_USER has id 2, and ROLE_MANAGER has id 3):

```sql
update user_role set role_id = 2 where role_id = 3;
```

### International search
There are few steps that need to be taken in order to make the international search available when
migrating your old application to the new version that supports it.

Firstly, a special user has to be created that will provide access to the remote instances.
To do this, run the following command on your database. This account
should not be used for any other purpose, which is why it's username and password are randomly generated
by the following script.

Note that in order for this command to be guaranteed to work, the RDMM application connected to this database must be shut down.

```mysql
INSERT INTO user ( email, enabled, password, privacy_level, description, last_name, name, shared, hide_genelist)
VALUES(CONCAT(RAND(),"@rdmm.com"), 0, MD5(RAND()), 0, "Remote admin profile", "", "", false, false);
INSERT INTO user_role (user_id,role_id) VALUES ((select max(user_id) from user), 1);
INSERT INTO user_role (user_id,role_id) VALUES ((select max(user_id) from user), 2);
SELECT max(user_id) from user;
```

The last command will output an information that you will need in the next step. It will look like this:
```Ini
+--------------+
| max(user_id) |
+--------------+
|          550 |
+--------------+
1 row in set (0.00 sec)
```
Make note of the number in the box (550 in the above example).

With this information in hand, you will need to update your application-prod.properties file:

```
## whether to enable international searching
rdp.settings.isearch.enabled=true
## whether international search is selected by default
rdp.settings.isearch.default-on=false
## urls of international instances to search when enabled. Separate with a comma
rdp.settings.isearch.apis=
## Admin user id used for authenticated remote search. Not using 1 or 2 because those are IDs frequently used in tests. If changed, also update data.sql
rdp.settings.isearch.user-id=550
## Token used for remote search with administrative rights. Obtain from RDMM program coordinator.
rdp.settings.isearch.search-token=XXXX
## Tokens for remote requests that authorize administrative access. Usually equal to the search-token, but can contain multiple entries separated by comma.
rdp.settings.isearch.auth-tokens=XXXX
```
Use the number you noted in the previous step for the `rdp.settings.isearch.user-id` line.

You can obtain the values for lines `rdp.settings.isearch.apis`, `rdp.settings.isearch.search-token` and `rdp.settings.isearch.auth-tokens` from the central network
administrator. The search-token and auth-tokens are highly confidential information, and we ask you to not
share them with anyone, since if would compromise the security of the whole network.

It is possible that as new international partners register to the network, we will provide you with
updated values for the `rdp.settings.isearch.apis` line. Or in case of a security breach,
we might ask you to update your search-token and auth-tokens.

### Custom taxon ordering
A new property for taxa has been introduced, that allows a custom order of taxa in the dropdown menus.

You can customize the order by running the following commands on your database.
Just edit the `ordering=X` number to represent what position you would like the taxon to be on. You can skip taxa that you
do not use (i.e. that are not active):
```mysql
update taxon set ordering = 1 where common_name = "human";
update taxon set ordering = 2 where common_name = "mouse";
update taxon set ordering = 3 where common_name = "rat";
update taxon set ordering = 4 where common_name = "zebrafish";
update taxon set ordering = 5 where common_name = "fruit fly";
update taxon set ordering = 6 where common_name = "roundworm";
update taxon set ordering = 7 where common_name = "yeast";
update taxon set ordering = 8 where common_name = "e. coli";
```

### New FAQs

There are new categories talking about the now available privacy and sharing options. You can use our updated
faq file (see the faq.properties file in our github repository), or add the new categories manually to your existing file.


### Start the application

After finishing all the steps, you can start your RDMM application again and test if everything works as expected.
