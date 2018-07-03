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

# Default user will be ROLE_MANAGER instead of ROLE_USER
# Set to true if you want the default newly created user
# to be able to search and view other users' profile.
rdp.settings.send-email-on-registration=true

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
