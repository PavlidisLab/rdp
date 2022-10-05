# Installation

This section describes the essential steps to deploy an RDP registry.

## Requirements

- Java 8+
- MySQL 5.7+ or equivalent
- SMTP mail server

## Obtain a distribution of RDP

Download the [JAR distribution from GitHub](https://github.com/PavlidisLab/rdp/releases/v{{ config.extra.rdp_version }}).

```bash
wget https://github.com/PavlidisLab/rdp/releases/download/v{{ config.extra.rdp_version }}/rdp-{{ config.extra.rdp_version }}.jar
```

The JAR contains the core application, including an embedded webserver (Tomcat 9), a task scheduler, an in-memory
cache, and much more!

## Set up the MySQL database

Create the database and an associated user that the application will use to store and retrieve data.

```sql
create database '<database name>' character set utf8mb4;
create user  '<database username>'@'%' identified by '<database password>';
grant all on rdp.* to '<database username>'@'%';
```

If you're using MySQL 5.6 or prior, use the `utf8` character set. It is a 3 bytes subset of the typical 4-bytes UTF-8
character encoding. Otherwise, you will face issues with the index size limit of 767 bytes due to some of our indexed
columns containing 255 characters (4 * 255 = 1020 > 767, but 3 * 255 = 765).

## Setup application.properties

In the working directory of the Web application, create an `application.properties`
file that contains at least the following entries:

```properties
spring.profiles.active=prod

spring.datasource.url=jdbc:mysql://<database host>/<database name>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=<database username>
spring.datasource.password=<database password>

spring.mail.host=<mail host>
spring.mail.port=587
spring.mail.username=<mail username>
spring.mail.password=<mail password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# This is very important for generating URLs
rdp.site.host=https://register.example.com
rdp.site.context=
rdp.site.mainsite=https://example.com/

rdp.site.contact-email=registry-help@example.com
rdp.site.admin-email=registry-admin@example.com
```

This file contains the database and SMTP credentials and various runtime configurations. Make sure it's only readable by
the user that will run the instance.

Documentation for options with their default values are available
in [application.properties](https://github.com/PavlidisLab/rdp/blob/{{ config.extra.git_ref }}/src/main/resources/application.properties).

And a `messages.properties` with the following entries:

```properties
# Adjust this to your own network name
rdp.site.fullname=Rare Disease Model & Mechanism Network
rdp.site.shortname=RDMM
```

**Note:** For backward compatibility reasons, the content of `application-prod.properties` and `login.properties` files
is included in the application's messages. Please ensure that any residual messages are migrated to `messages.properties`.

That should be enough to get the Web service started. Now you can launch it by issuing the following command:

```bash
java -jar rdp-{{ config.extra.rdp_version }}.jar
```

If your email server is not properly configured, you will see an error from the Spring Actuator health check. You can
detect further issues by looking at the
`/admin/health` endpoint with administrative privilege.

## Create an administrative account

Use the registration page to create a user account and promote it to administrator privileges. Consider this to be a
sanity check for your database and email server setup.

Once created, obtain its user identifier:

```sql
select user_id from user where email = '<your user email>';
```

Then add the `ROLE_ADMIN` (`role_id` is `1`) role to that user:

```sql
insert into user_role (user_id, role_id) values ('<user_id>', 1);
```

Next, we will see how the Web application can be customized further.
