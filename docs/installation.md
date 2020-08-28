# Installation

This section describe the essential steps to install and run an RDP instance.

## Requirements

* Java 8+
* MySQL 5.5+ or equivalent
- SMTP mail server

## Obtain a distribution of RDP

Download the [latest jar distribution](https://github.com/PavlidisLab/rgr/releases/latest) from GitHub.

```bash
wget https://github.com/PavlidisLab/rgr/releases/download/v1.3.2/rdp-1.3.2.jar
```

The jar contains the core application, including an embedded webserver (Tomcat 8.5.x),
a task scheduler, an in-memory cache, and much more!

## Setup the MySQL database

Create the database and an associated user that the application will use to
store and retrieve data.

```sql
create database <database name> character set utf8mb4 collate utf8mb4_unicode_ci;
create user  '<database username>'@'%' identified by '<database password>';
grant all on rdp.* to '<database username>'@'%';
```

## Setup application.properties

In the working directory of the Web application, create an `application.properties`
file that contains at least the following entries:

```ini
spring.profiles.active=prod

spring.datasource.url=jdbc:mysql://<database host>:3306/<database name>
spring.datasource.username=<database username>
spring.datasource.password=<database password>

spring.mail.host=<mail host>
spring.mail.port=587
spring.mail.username=<mail username>
spring.mail.password=<mail password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

This file contains the database and SMTP credentials and various runtime
configurations. Make sure it's only readable by the user that will run the
instance.

Take a look at [application.properties](https://github.com/PavlidisLab/rgr/blob/development/src/main/resources/application.properties)
for detailed documentation and default values.

That should be enough to get the Web service started. Now you can launch it be
issuing the following command:

```bash
java -jar rdp-1.3.2.jar
```

If your email server is not configured properly, you will see an error from the
Spring Actuator health check. You can detect further issues by looking at the
`/admin/health` endpoint with administrative privilege.

Next, we will see how the Web application can be customized further.
