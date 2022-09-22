# Migrate from previous releases

Your current data should not be lost while migrating, but you should definitely have a database backup in case things go
wrong for any reason. Read
about [database backup options for MySQL](https://dev.mysql.com/doc/refman/5.7/en/backup-methods.html).

First, take down your running registry instance to make sure it won't interfere with the migration process.

Then, launch the newer release which will perform the necessary migrations on startup.

At this point, your instance should be updated and running. No further action is necessary. If you get any errors during
any part of the process, please contact us.

As of 1.3.2, database migrations are automated with Flyway which will run at startup of the application. This behaviour
can be disabled with the following parameter in `application.properties`:

```properties
spring.flyway.enabled=false
flyway.enabled=false # for <1.5 releases
```

Take a look at the sections below for version-specific migration procedures.

## Migrate from 1.4 to 1.5

This release introduce strict validation for the `application.properties` configuration file. If you had invalid values,
adjust them as indicated.

This release does not enable [human organ systems](customization.md#human-organ-systems) by default. If you still want
to use this feature, enable it with:

```properties
rdp.settings.organs.enabled=true
```

The anonymized search results are also disabled by default. To enable them set the following:

```properties
rdp.settings.privacy.enable-anonymized-search-results=true
```

The redundant `rdp.site.context` property has been removed. In the unlikely case you were using it, you can instead
append the context to `rdp.site.host`.

```properties
# before
rdp.site.host=http://example.com
rdp.site.context=/test
```

```properties
# after
rdp.site.host=http://example.com/test
```

Note that we switched to Spring Boot 2, so you should migrate all the custom properties set in `application.properties`
accordingly. Notably, the Flyway options `flyway.*` must be migrated to `spring.flyway.*`.

There are incompatibilities with the previous Flyway 3.2.1 release that is handled on startup. If you encounter issues
related to Flyway, please make sure to [report them on GitHub](https://github.com/PavlidisLab/rdp/issues).

Static assets are now packaged with [Webpack](https://webpack.js.org/), meaning that any attempt to override CSS or JS
assets will not work anymore. Refer to the [editing assets](customization.md#editing-assets) section to customize
assets.

### API breaking changes

If you use our API, there's been a few minor cleanups that resulted in breaking changes.

The `id` and `anonymousId` fields are now mutually exclusives. This means that you have to test for the existence of the
attribute before retrieving its value.

The user model has been substantially simplified to provide only necessary information.

- `fullName` is not exposed anymore, you can construct an adequate representation instead from `name` and `lastName`
- `contactEmail` has been removed, `email` will display the public-facing email if available
- `hideGenelist` and `shared` have been removed

The gene model no longer exposes `modificationDate`, which is irrelevant for user-associated gene anyway.

The `/api/stats` endpoint has been stabilized and some of its attributes were renamed for more consistency.

## Migrate from 1.3 to 1.4

This release includes the initial schema of the 1.3.1 release as baseline. This means that you don't have to go through
the process of running previous migrations when you deploy releases from the 1.4 series.

Organ information have been included as well as additional fields in the user profile. Gene information are now stored
in the database. As a result, taxon are known for ortholog genes and the `ortholog_taxon`  column has become
unnecessary, and a source of error for Hibernate.

From this release and onward, Spring resources are used to map external URL and local files for cached resources such as
gene information, orthologs, etc.

If you changed any of the `rdp.settings.cache` parameter, you will have to add a `file:` prefix to refer to local files.
You can also use the `https:` and `ftp:` prefixes for referring to remote files.

```properties
rdp.settings.cache.enabled=false
rdp.settings.cache.load-from-disk=true
rdp.settings.cache.gene-files-location=file:genes/
rdp.settings.cache.ortholog-file=file:DIOPT_filtered_data_March2020.gz
rdp.settings.cache.term-file=file:go.obo
rdp.settings.cache.annotation-file=file:gene2go.gz
rdp.settings.cache.organ-file=file:uberon.obo
```

We recommend that you move any messages from `application-prod.properties` and `login.properties` into
`messages.properties`. More details are available in [Customize your instance](customization.md).

The FAQ defaults are now distributed in the JAR archive and can be overridden with a custom resource. If you had
a `faq.properties` previously setup, add the following line in your configuration to use it:

```properties
rdp.settings.faq-file=file:faq.properties
```

Consider also taking a look at the packaged [faq.properties](https://github.com/PavlidisLab/rdp/blob/{{ config.extra.git_ref }}/src/main/resources/faq.properties)
file and update your custom FAQ accordingly.

## Migrate from 1.3.x to 1.3.2

NCBI gene broke because they introduced genes with unexpected date format. We adjusted our parsing code to process date
formats and now store a `DATE` type in the database.

## Migrate from version 1.1.x to 1.2

There are new security settings that can be added to your `application.properties` file. See the section 'Privacy and
search Defaults' in the 'Customize Settings' example file.

The new security settings will have to be back-filled for the users that have registered prior to this update. This can
be done by directly editing the database entries like so:

```sql
update user
set hide_genelist = 0,
    privacy_level = 0,
    shared        = 1;
```

The values of these settings should correspond with the defaults you have set in your `application-prod.properties`
file. Specifically:

- `hide_genelist = X` if X =1, hides users gene list from public searches. is only effective when the
  setting `rdp.settings.privacy.allow-hide-genelist` is enabled.
- `privacy_level = X` where X is the privacy level. Should have the same value as `rdp.settings.privacy.default-level`,
  or whatever the users prior to this update agreed to.
- `shared = X` where X is 1 or 0. Corresponds to `rdp.settings.privacy.default-sharing`

#### Registered user search in previous version

If you previously had search enabled for registered users, you want to set `privacy_level` to `1`
and `rdp.settings.privacy.default-level` to `true`.

The original system for enabling registered users to use the search function was based on assigning a different role to
all users. This has been discontinued, and needs to be manually switched for all existing users. This can be easily done
by running the following command on your database (provided you have the original set of roles that came with the
application, where ROLE_USER has id 2, and ROLE_MANAGER has id 3):

```sql
update user_role
set role_id = 2
where role_id = 3;
```

### International search

There are few steps that need to be taken in order to make the international search available when migrating your old
application to the new version that supports it.

Firstly, a special user has to be created that will provide access to the remote instances. To do this, run the
following command on your database. This account should not be used for any other purpose, which is why it's username
and password are randomly generated by the following script.

Note that in order for this command to be guaranteed to work, the RDMM application connected to this database must be
shut down.

```sql
INSERT INTO user (email, enabled, password, privacy_level, description, last_name, name, shared, hide_genelist,
                  contact_email_verified)
VALUES (CONCAT(RAND(), '@rdmm.com'), 0, MD5(RAND()), 0, 'Remote admin profile', '', '', false, false, false);
INSERT INTO user_role (user_id, role_id)
VALUES ((select max(user_id) from user), 1);
INSERT INTO user_role (user_id, role_id)
VALUES ((select max(user_id) from user), 2);
SELECT max(user_id)
from user;
```

The last command will output an information that you will need in the next step. It will look like this:

```
+--------------+
| max(user_id) |
+--------------+
|          550 |
+--------------+
1 row in set (0.00 sec)
```

Make note of the number in the box (550 in the above example).

With this information in hand, you will need to update your application-prod.properties file:

```properties
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

You can obtain the values for lines `rdp.settings.isearch.apis`, `rdp.settings.isearch.search-token`
and `rdp.settings.isearch.auth-tokens` from the central network administrator. The search-token and auth-tokens are
highly confidential information, and we ask you to not share them with anyone, since it would compromise the security of
the whole network.

It is possible that as new international partners register to the network, we will provide you with updated values for
the `rdp.settings.isearch.apis` line. Or in case of a security breach, we might ask you to update your search-token and
auth-tokens.

### Custom taxon ordering

A new property for taxa has been introduced, that allows a custom order of taxa in the dropdown menus.

You can customize the order by running the following commands on your database. Just edit the `ordering=X` number to
represent what position you would like the taxon to be on. You can skip taxa that you do not use (i.e. that are not
active):

```sql
update taxon
set ordering = 1
where common_name = 'human';
update taxon
set ordering = 2
where common_name = 'mouse';
update taxon
set ordering = 3
where common_name = 'rat';
update taxon
set ordering = 4
where common_name = 'zebrafish';
update taxon
set ordering = 5
where common_name = 'fruit fly';
update taxon
set ordering = 6
where common_name = 'roundworm';
update taxon
set ordering = 7
where common_name = 'yeast';
update taxon
set ordering = 8
where common_name = 'e. coli';
```

### New FAQs

There are new categories talking about the now available privacy and sharing options. You can use our updated faq file (
see the faq.properties file in our GitHub repository), or add the new categories manually to your existing file.
