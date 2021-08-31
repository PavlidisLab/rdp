# Deployment

This section expands the process of deploying in a production environment. Make sure you are familiarized with the
[installation](/installation) procedure first before attempting the described steps.

## Web server

There a few options you will likely want to specify when deploying your RDP instance.

### Server port

Set `-Dserver.port=<port>` to adjust the port the embedded webserver is listening on.

### Fast random number generation

Specify `-Djava.security.egd=file:/dev/urandom` this if you receive logs such as: _"Creation of SecureRandom instance
for session ID generation using [SHA1PRNG] took [235,853] milliseconds."_ The secure random calls may be blocking as
there is not enough entropy to feed them in `/dev/random`.

### Proxy configuration

Set `-Dhttp.proxyHost` and `-Dhttp.proxyPort` to perform HTTP accesses through a proxy.

Likewise, you can set up a proxy for FTP connections with `-Dftp.proxyHost` and `-Dftp.proxyPort`.

## General steps

1. Create a working directory for the web service: `mkdir -p /project/directory`
2. Move into that directory: `cd /project/directory`
3. Download the latest
   release: `wget -O rdp-{{ config.extra.rdp_version }}.jar https://github.com/PavlidisLab/rdp/releases/download/v{{ config.extra.rdp_version }}/rdp-{{ config.extra.rdp_version }}.jar`
4. Create `application.properties`, and optionally `faq.properties` and
   `messages.properties` if you want to [customize messages](customization.md#customizing-the-applications-messages).
5. Test your setup: `java -jar rdp-{{ config.extra.rdp_version }}.jar`
6. Log into the database and activate other organisms and organ systems (optional)
7. Deploy using Systemd (see below) or a Docker container

## Integration with systemd

If you are not using a container technology such as Docker, we recommend that you use a systemd service unit to deploy
your RDP instance.

Create a file under `/etc/systemd/system/rdp.service` with the following content:

```Ini
[Unit]
Description=rdp
After=syslog.target

[Service]
User=tomcat
Group=tomcat
WorkingDirectory=/project/directory
ExecStart=/bin/java -Xms256m -Xmx3g -Dserver.port=<port> -Djava.security.egd=file:/dev/urandom -jar rdp-{{ config.extra.rdp_version }}.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

* Start service: `systemctl start rdp.service`
* View logs: `journalctl -f -u rdp.service`

### Apache

Create a standard virtual host section with the following proxies:

```
ProxyPass / http://localhost:<port>/
ProxyPassReverse / http://localhost:<port>/
```

## More information

For custom cloud deployments
see [Spring Boot: Deploying to the Cloud](https://docs.spring.io/spring-boot/docs/current/reference/html/cloud-deployment.html)
.

To install as a system service
see [Spring Boot: Installing Spring Boot Applications](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html)
.
