# Customize your instance

## Ontology data 

RDP relies on a rich set of ontologies to describe taxa, GO terms, organ systems, etc.

This data is retrieved from various remote locations at startup and updated on a monthly basis.


### Disable data loading

To prevent data from being loaded on startup and recurrently, set the following parameter:

```ìni
rdp.settings.cache.enabled=false
```

You should deploy your RDP instance at least once to have initial data before setting this property and whenever you
update the software.

### Gene information, GO terms

By default, RDP will retrieve the latest genes and gene-term associations from
NCBI, and GO terms from [Ontobee](http://www.ontobee.org/ontology/OBI). Users
genes and terms will be updated in the aftermath of a successful update.

Note that the URL used for retrieving data from NCBI is defined in the database. If `rdp.settings.load-from-disk` is 
enabled, the basename of the URL will be used, relative to `rdp.settings.gene-files-location`.

```sql
select taxon_id, scientific_name, gene_url from taxon;
```

### Default locations for data

By default, RDP retrieves data from the following locations for orthologs, GO terms, gene2go and Uberon identifiers.

```ìni
rdp.settings.cache.ortholog-file=classpath:cache/DIOPT_filtered_data_May2021.gz
rdp.settings.cache.term-file=http://purl.obolibrary.org/obo/go.obo
rdp.settings.cache.annotation-file=ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene2go.gz
rdp.settings.cache.organ-file=http://purl.obolibrary.org/obo/uberon.obo
```

Gene information are obtained from [NCBI Gene FTP server](https://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/) with URLs
stored in the database.

### Loading data from disk

If you choose to load data from the disk or any other location supported by, a location where genes and GO terms can be 
obtained relative to the working directory of the Web application must be provided.

```ini
rdp.settings.cache.load-from-disk=true
rdp.settings.cache.gene-files-location=file:genes/
rdp.settings.cache.ortholog-file=file:DIOPT_filtered_data_March2020.txt
rdp.settings.cache.term-file=file:go.obo
rdp.settings.cache.annotation-file=file:gene2go.gz
rdp.settings.cache.organ-file=file:uberon.obo
```

With the above settings and given that *Homo sapiens* taxon is enabled, RDP will retrieve gene information from 
`genes/Homo_sapiens.gene_info.gz`.

### Taxon

The taxon table is pre-populated during the first migration, and only human is activated and only human is activated. 
To enable other organisms, set their `active` column to `1` in the database. 

For example, the following will activate the mouse taxa:

```sql
update taxon set active = 1 where taxon_id = 10090;
```


### Ortholog mapping

There is a static orthologs mapping included with the application based on [DIOPT](https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-12-357), 
that will automatically populate the database on startup. They are also updated monthly.

As an alternative, you can also use NCBI gene orthologs:

```ini
rdp.settings.cache.orthologs-file=ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene_orthologs.gz
```

### Organ systems

Organ systems ontology is based on [Uberon multi-species anatomy ontology](http://www.obofoundry.org/ontology/uberon.html) 
and updated monthly.

Only a select few organ systems are active by default. You can activate more by running the following SQL command with
a Uberon identifier of your choice:

```sql
update organ_info set active = true where uberon_id = '<uberon_id>';
```

To disable organ systems altogether, set the following in your configuration:

```
rdp.settings.organs.enabled=false
```

### International data

In order to access the RDMMN shared data system (via the international search), your application must use HTTPS.  If you
do not have HTTPS setup for you domain, you can consult the following guides on how to set it up:

 - [medium.com/@raupach/how-to-install-lets-encrypt-with-tomcat-3db8a469e3d2](https://medium.com/@raupach/how-to-install-lets-encrypt-with-tomcat-3db8a469e3d2)
 - [community.letsencrypt.org/t/configuring-lets-encrypt-with-tomcat-6-x-and-7-x/32416](https://community.letsencrypt.org/t/configuring-lets-encrypt-with-tomcat-6-x-and-7-x/32416)

Registries can access each other public data by setting up the `rdp.settings.isearch.apis` configuration key in
the `application.properties` file. You can put there a comma-delimited list of partner registry URLs.

```ini
rdp.settings.isearch.enabled=true
rdp.settings.isearch.apis=https://register.rare-diseases-catalyst-network.ca/
```

If your current user has administrative priviledges, a special search token is appended to your remote queries. This
should be unique to your registry.

To generate a secure token, you can use OpenSSL: `openssl rand -base64 24`.

```ini
rdp.settings.isearch.search-token=hrol3Y4z2OE0ayK227i8oHTLDjPtRfb4
```

Send that token securely to the partner registries.

On the receiving side, the partner registry must create a user that is used to
perform privileged searches. This can be achieved by creating a [service account](/service-account).

Let's assume that the created user's ID was 522. The partner would then add the
token to its `rdp.settings.isearch.auth-tokens` setting along any existing
tokens.

```ini
rdp.settings.isearch.user-id=522
rdp.settings.isearch.auth-tokens=jLb22QZzsaT6/w3xwDHBObmZPypJgXfb,hrol3Y4z2OE0ayK227i8oHTLDjPtRfb4
```

That's it. You can now query private data from the partner registry when logged in as an administrator on your own
registry.

## Tiers

User genes are categorized in tiers corresponding to the level of involvement of a researcher with the gene. Researcher 
have direct access to their TIER1 genes, and a focus on their TIER2 genes. TIER3 genes result from GO term associations.

To enable only TIER1 and TIER2, and thus disabling GO terms-related features, add the following to your configuration:

```ini
rdp.settings.enabled-tiers=TIER1,TIER2
```

## Researcher categories

Researcher categories can be enabled or disabled by setting the `rdp.settings.profile.enabled-researcher-categories` to
a list of desired values:

```ini
rdp.settings.enabled-researcher-categories=IN_SILICO,IN_VIVO
```

## Privacy levels

Privacy levels can be selectively enabled for user profiles and genes.

```ini
rdp.settings.privacy.enabled-levels=PUBLIC,SHARED,PRIVATE
rdp.settings.privacy.enabled-gene-levels=PUBLIC,SHARED,PRIVATE
```

Note that any value enabled for genes that is not also enabled for profiles will be ignored.

## Anonymized search results

By default, search results that are not accessible to a given user are anonymized. All identifiable information is
stripped from the model and numerical identifiers are randomized in such a way that it becomes impossible to relate
users or genes from different search.

This feature can be disabled by setting the following configuration key to `false`:

```
rdp.settings.privacy.enable-anonymized-search-results=false
```

## Internationalization and custom messages

Some text displayed in RDP can be customized and internationalized.

To do so, create a `messages.properties` file in the working directory of the Web application
add the entries you want to change. Default values are found in
[messages.properties](https://github.com/PavlidisLab/rgr/blob/master/src/main/resources/messages.properties)

You can use suffixed like `messages_en_CA.properties` for region-specific
localization.

Note that `application-prod.properties` and `login.properties` are also used
for messages for backward compatibility. New and existing messages should be
moved to `messages.properties`.

## FAQ

The FAQ can be customized by relocating the resource to a local file (i.e. `faq.properties`) with
the `rdp.settings.faq-file` configuration.

```ini
rdp.settings.faq-file=file:faq.properties
```

All the question and answer style items that will display in the frequently asked questions page. Each entry requires 
two parts: `rdp.faq.questions.<q_key>` and `rdp.faq.answers.<q_key>` which hold the question and the corresponding 
answer, respectively.

```ini
rdp.faq.questions.<q_key>=A relevant question.
rdp.faq.answers.<q_key>=A plausible answer.
```

Example of a FAQ can be found in [faq.properties](https://github.com/PavlidisLab/rgr/tree/master/src/main/resources/faq.properties).

## Style and static resources

Static resources can be selectively replaced by including a search directory for Spring static resources.

```ini
spring.resources.static-locations=file:static/,classpath:/static/
```

Here's the list of paths that can be adjusted using the above setting:

```
static/
    css/
        common.css # general pages
        login.css  # login-like pages (i.e. registration, reset password, etc.)
    images/
        model-organisms/
            <taxon_id>.svg
        organs/
            <uberon_id>.svg
        researcher-categories/
            <researcher_category_id>.svg
        brand.png
        favicon-16x16.png
        favicon-32x32.png
        header.jpg
```

We strongly recommend against overriding JavaScript files as it could break functionalities of the website.

## Building from source

You can customize RDP by editing the publicly available source code and
packaging the JAR archive yourself.

```bash
git clone https://github.com/PavlidisLab/rgr.git
git checkout v1.4.0
cd rgr/
# edit what you want...
./mvnw package
```

The new build will be available in the `target` directory.
