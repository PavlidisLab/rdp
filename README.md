# Rare Disease Project
Registry for model organism researchers, developed for the Canadian Rare Disease Models &amp; Mechanisms Network

## Setup Instructions

### Requirements
* Java 8+
* MySQL 5.5+ or equivalent

### Obtain Distribution
Download the latest jar distribution [here](https://github.com/PavlidisLab/modinvreg/releases/latest).

The jar contains the core application, including an embedded webserver (Tomcat 8.5.x). 

### Set up database
Create database and associated user that the application will use to connect.

Ex.
```SQL
CREATE DATABASE afg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER  'rdpuser'@'%' identified by 'rdppassword';
grant all on rdp.* to 'rdpuser'@'%';
```

### Customize Settings
The only thing that's left is customization and connection with your database.
This is done by creating the following properties files

* application-prod.properties
  - Location: Working directory of running jar
  - Defaults: https://github.com/PavlidisLab/modinvreg/blob/master/src/main/resources/application.properties
  - Contents: Majority of the properties related to the running of the application (including database connection information) and message customisation.
  - Example:
```Ini
# ===============================
# = DATA SOURCE
# ===============================

### Database Connection ###
spring.datasource.url = jdbc:mysql://<Host Here>:3306/<Database Name Here>
spring.datasource.username = DB_USER
spring.datasource.password = DB_PASS

# ==============================================================
# = Spring Email
# ==============================================================

### Email Server Setup ##
spring.mail.host=smtp.gmail.com
spring.mail.username=example@gmail.com
spring.mail.password=example_password
spring.mail.port=587

# ==============================================================
# = Application Specific Defaults
# ==============================================================

### Host of your production website (No Trailing Slash) ###
rdp.site.host=http://register.rare-diseases-catalyst-network.ca

## Context Root Path of production website (leave blank if none) ###
rdp.site.context=

### Emails (these can be the same) ###

### Contact Email is the one displayed in the website ###
rdp.site.contact-email=registry-help@example.ca

### Admin Email is the one used by the system to send and receive email ###
rdp.site.admin-email=registry-help@example.ca

# ==============================================================
# = Privacy and search Defaults
# ==============================================================

# Privacy settings
## Whether the search page is accessible by visitors (i.e. non-registered users)
rdp.settings.privacy.public-search=true
## Whether the search page is accessible by registered users
rdp.settings.privacy.registered-search=true
## 0 = private, 1 = shared with registered users, 2 = public
rdp.settings.privacy.default-level=0
## Minimum allowed privacy level
rdp.settings.privacy.min-level=0
## share with international instances
rdp.settings.privacy.default-sharing=false
## whether users are allowed to change their privacy settings
rdp.settings.privacy.customizable-level=false
## whether users are allowed to change their international sharing setting
rdp.settings.privacy.customizable-sharing=false
## whether users are allowed to hide their gene-list when their data is shared or public
rdp.settings.privacy.allow-hide-genelist=false

# international search settings
## whether to enable international searching
rdp.settings.isearch.enabled=true
## whether international search is selected by default
rdp.settings.isearch.default-on=true
## urls of international instances to search when enabled. Separate with a comma
rdp.settings.isearch.apis=http://local.net:8080
## Admin user id used for authenticated remote search. Not using 1 or 2 because those are IDs frequently used in tests. If changed, also update data.sql
rdp.settings.isearch.user-id=3
## Token used for remote search with administrative rights. Obtain from RDMM program coordinator.
rdp.settings.isearch.search-token=
## Tokens for remote requests that authorize administrative access. Usually equal to the search-token, but can contain multiple entries separated by comma.
rdp.settings.isearch.auth-tokens=

# ==============================================================
# = Custom Messages
# ==============================================================

### Customisable HTML Embeddings ###
rdp.site.fullname=Rare Diseases: Models & Mechanisms Network
rdp.site.shortname=RDMMN

rdp.site.logohtml=<h2 class="navbar-text m-0">${rdp.site.fullname}</h2>

rdp.site.welcome=<p>The ${rdp.site.fullname} Registry collects information on model organism researchers \
  and the specific genes they study. The Registry is the mechanism by which researchers can find potential \
  matches of model organism researchers to human rare disease or cancer researchers.</p>

rdp.site.email.registration-welcome=Thank you for registering with ${rdp.site.fullname}. (${rdp.site.host}${rdp.site.context}).

rdp.site.email.registration-ending=You will then be able to log in using the password you provided, and \
  start filling in your profile.\r\n\r\n If you have questions or difficulties with registration please \
  feel free to contact us: ${rdp.site.contact-email}
```
* faq.properties
  - Location: Specified using `-Dspring.config.location=file:<location>`
  - Defaults: Empty
  - Contents: All of the question and answer style items that will display in the frequently asked questions page. Each entry requires two parts: `rdp.faq.questions.<q_key>` and `rdp.faq.answers.<q_key>` which hold the question and the corresponding answer, respectively.
  - Example: https://github.com/PavlidisLab/modinvreg/blob/master/faq.properties
* login.properties
  - Location: Working directory of running jar
  - Defaults: https://github.com/PavlidisLab/modinvreg/blob/master/src/main/resources/application.properties
  - Contents: Create this file if you would like to customise spring specific messages such as incorrect username/password.

### Running Application
To start the application the simplest command is: java -jar rdp-x.x.x.jar

That being said, there a few options you will likely want to specify

* `-Dserver.port=<port>`: Port for the webserver to listen on.
* `-Dspring.config.location=file:<faq location>`: Location to find the FAQ question & answers
* `-Djava.security.egd=file:/dev/./urandom`:  Specify this if you receive logs such as: _"Creation of SecureRandom instance for session ID generation using [SHA1PRNG] took [235,853] milliseconds."_ The secure random calls may be blocking as there is not enough entropy to feed them in /dev/random.

The webserver will start initialising, create any missing required tables in the connected database and shortly be serving content at the provided port.

#### Notes
* The organisms table is prepopulated on creation however all but human are turned off. Set the active column to 1 in the database to turn on an organism (Example (this will activate mouse): `update taxon set active=1 where taxon_id=10090`)
* If a required table is not found in the database upon application startup it will create it, it will NOT delete existing data.

## Building From Source

* Clone the repo or download the source distribution.
  - `git clone https://github.com/PavlidisLab/modinvreg.git`
* Package
  - `cd modinvreg/`
  - `./mvnw package`

The jar will be create in the target directory.

For custom deployments see: https://docs.spring.io/spring-boot/docs/current/reference/html/cloud-deployment.html
To install as a system service see: https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html

## Example Setup for Centos 7 With Mysql 5.5+ & Apache

### Database
* mysql -uroot -p
* CREATE DATABASE rdp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
* CREATE USER  'rdpuser'@'%' identified by 'rdppassword';
* grant all on rdp.* to 'rdpuser'@'%';

### Application
* `cd /project/directory`
* `wget https://github.com/PavlidisLab/modinvreg/releases/download/vx.x/rdp-x.x.x.jar`
* create application-prod.properties, faq.properties and optionally login.properties
* test run the jar: java -Dserver.port=8080 -Dspring.config.location=file:faq.properties -Djava.security.egd=file:/dev/./urandom -jar rdp-x.x.x.jar
* (Optional) Log into the database and activate other organisms.
* Set up jar as systemd service:
  - create file /etc/systemd/system/rdp.service containing similar to the following:
  
```Ini
[Unit]
Description=rdp
After=syslog.target

[Service]
User=tomcat
Group=tomcat
WorkingDirectory=/project/directory
ExecStart=/bin/java -Xms256m -Xmx3g -Dserver.port=8083 -Dspring.config.location=file:faq.properties -Djava.security.egd=file:/dev/./urandom -jar rdp-x.x.x.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```
* Start service: `systemctl start rdp.service`
* View logs: `journalctl -f -u rdp.service`

### Apache
* Create a standard virtualhost with the following proxies:
  - `ProxyPass / http://localhost:<port>/`
  - `ProxyPassReverse / http://localhost:<port>/`
  
## Ortholog mapping
There is a static ortholog mapping included with the application, that will automatically populate the database on startup.

For future updates of the ortholog mapping, you can watch the file 
https://github.com/PavlidisLab/modinvreg/blob/development/src/main/resources/data.sql
for changes, and run the ortholog part on your database when that happens.
  
## Migration from version 1.1.x to 1.2
Your current data should not be lost in this step, but you should definitely have a 
database backup in case things go wrong for any reason.

Read about database backup options for mysql here: https://dev.mysql.com/doc/refman/5.7/en/backup-methods.html

If you get any errors during any part of the following process, please contact us.

Obtain the latest jar file and replace your old jar file with it.
Add this line to your application-prod.properties:
```Ini
spring.datasource.initialization-mode=always
```
Run the application, but do not allow users to connect to it (you can shut it down immediately after it has successfully started up). 
This should add new properties to the database, that are required for the new version to run properly, and 
fill the ortholog mapping table.

Remove the line you added from the application-prod.properties file again. 
Keeping it should not have any adverse effect, but it will slow down further startup times of the application.

There are new security settings that can be added to your application-prod.properties file. 
See the section 'Privacy and search Defaults' in the 'Customize Settings' example file.

The new security settings will have to be back-filled for the users that have registered prior to this update. 
This can be done by directly editing the database entries like so:
```mysql
UPDATE user SET 
hide_genelist = 0, 
privacy_level = 0, 
shared = 1;
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
```mysql
UPDATE user_role SET role_id = 2 WHERE role_id = 3;
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

```Ini
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

### Start the application

After finishing all the steps, you can start your RDMM application again and test if everything works as expected.