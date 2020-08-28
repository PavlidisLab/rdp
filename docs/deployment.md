# Deployment

This section expands the process of deploying in a production environment. Make
sure you are familiarized with the [installation](/installation) procedure
first before attempting the described steps.

## Administrative user

Your rgr instance is initialized with an administrator user. Make sure you
change its default password before exposing your application to the Internet.

## Web server

There a few options you will likely want to specify when deploying your rgr
instance.

* `-Dserver.port=<port>`: Port for the webserver to listen on.
* `-Dspring.config.location=file:<faq location>`: Location to find the FAQ question & answers
* `-Djava.security.egd=file:/dev/./urandom`:  Specify this if you receive logs such as:
  _"Creation of SecureRandom instance for session ID generation using
  [SHA1PRNG] took [235,853] milliseconds."_ The secure random calls may be
  blocking as there is not enough entropy to feed them in `/dev/random`.

## Application

* `cd /project/directory`
* `wget https://github.com/PavlidisLab/modinvreg/releases/download/vx.x/rdp-x.x.x.jar`
* create application-prod.properties, faq.properties and optionally login.properties
* test run the jar: java -Dserver.port=8080 -Dspring.config.location=file:faq.properties -Djava.security.egd=file:/dev/./urandom -jar rdp-x.x.x.jar
* (Optional) Log into the database and activate other organisms.
* Set up jar as systemd service:
  - create file /etc/systemd/system/rdp.service containing similar to the following:

## Integration with systemd

If you are not using a container technology such as Docker, we recommend that
you use a systemd service unit to deploy your rgr instance.

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

For custom deployments see: https://docs.spring.io/spring-boot/docs/current/reference/html/cloud-deployment.html
To install as a system service see: https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html
