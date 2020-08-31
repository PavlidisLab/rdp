# Customize your instance

## Additional database configurations

The Web application will auto-magically initialize the database and fill the
tables with gene orthologs data. Since this can be pretty expensive to run each
time the server is restarted, you should set the following to skip
initialization:

```ini
spring.datasource.initialize=false
```

## Gene information and GO terms ontology

Gene information and GO terms ontology are downloaded from NCBI and scheduled
for update on a monthly basis.

There are a couple of ways this process can be configured to prevent periodic
update or to use data from the local filesystem.

```ini
rdp.settings.cache.enabled=true
rdp.settings.cache.load-from-disk=false
```

By default, RDP will retrieve the latest genes and gene-term associations from
NCBI, and GO terms from [Ontobee](http://www.ontobee.org/ontology/OBI). Users
genes and terms will be updated in the aftermath of a successful update.

However, if you choose to load data from the disk, you must provide a location
where gene and GO terms can be downloaded relative to the working directory of
the Web application.

```ini
rdp.settings.cache.gene-files-location=genes
rdp.settings.cache.term-file=go.obo
rdp.settings.cache.annotation-file=gene2go.gz
```

With the above settings and given that *Homo sapiens* taxon is enabled, RDP
will retrieve gene information from `genes/Homo_sapiens.gene_info.gz`.

## Internationalization and custom messages

Some text displayed in RDP can be customized and internationalized.

To do so, create a `messages.properties` file in the working directory of the Web application
add the entries you want to change. Default values are found in
[messages.properties](https://github.com/PavlidisLab/rgr/blob/development/src/main/resources/messages.properties)

You can use suffixed like `messages_en_CA.properties` for region-specific
localization.

Note that `application-prod.properties` and `login.properties` are also used
for messages for backward compatibility. New and existing messages should be
moved to `messages.properties`.

## FAQ

The FAQ can be customized in `faq.properties`.

  - Location: Specified using `-Dspring.config.location=file:<location>`
  - Defaults: Empty
  - Contents: All of the question and answer style items that will display in the frequently asked questions page. Each entry requires two parts: `rdp.faq.questions.<q_key>` and `rdp.faq.answers.<q_key>` which hold the question and the corresponding answer, respectively.
  - Example: https://github.com/PavlidisLab/modinvreg/blob/master/faq.properties

#### Notes
* The organisms table is prepopulated on creation however all but human are turned off. Set the active column to 1 in the database to turn on an organism (Example (this will activate mouse): `update taxon set active=1 where taxon_id=10090`)
* If a required table is not found in the database upon application startup it will create it, it will NOT delete existing data.
* In order to access the RDMMN shared data system (international search), your application must use HTTPs. If you do not have HTTPs setup for you domain, you can consult the following guides on how to set it up:
    - [medium.com/@raupach/how-to-install-lets-encrypt-with-tomcat-3db8a469e3d2](https://medium.com/@raupach/how-to-install-lets-encrypt-with-tomcat-3db8a469e3d2)
    - [community.letsencrypt.org/t/configuring-lets-encrypt-with-tomcat-6-x-and-7-x/32416](https://community.letsencrypt.org/t/configuring-lets-encrypt-with-tomcat-6-x-and-7-x/32416)

## Ortholog mapping

There is a static ortholog mapping included with the application, that will automatically populate the database on startup.

For future updates of the ortholog mapping, you can watch the
[data.sql](https://github.com/PavlidisLab/modinvreg/blob/development/src/main/resources/data.sql)
file for changes, and run the ortholog part on your database when that happens.

## Building from source

You can customize RDP by editing the publicly available source code and
packaging the JAR archive yourself.

```bash
git clone https://github.com/PavlidisLab/rgr.git
cd rgr/
# edit what you want...
./mvnw package
```

The new build will be available in the `target` directory.
