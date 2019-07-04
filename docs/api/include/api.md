**API**
----
  Root endpoint with welcome message and api version.

* **URL**

  `/api`

* **Method:**
  
  `GET`
  
*  **URL Params**

   None

   **Required:**
 
   None

   **Optional:**
 
   None

* **Data Params**

  None

* **Success Response:**
  
  Returns a "welcome object" with the API version.

  * **Code:** 200
    **Content:** `{"message":"This is this applications API. Please see documentation.","version":"1.0.0"}`
 
* **Error Response:**

  * **Code:** 404 NOT FOUND
    **Content:** `NO CONTENT`

* **Sample Call:**
  ```bash
  curl -v https://register.rare-diseases-catalyst-network.ca/api
  ```
    Output:
    ```
    *   Trying 137.82.176.6...
    * Connected to register.rare-diseases-catalyst-network.ca (137.82.176.6) port 443 (#0)
    * found 148 certificates in /etc/ssl/certs/ca-certificates.crt
    * found 597 certificates in /etc/ssl/certs
    * ALPN, offering http/1.1
    * SSL connection using TLS1.2 / ECDHE_RSA_AES_128_GCM_SHA256
    * 	 server certificate verification OK
    * 	 server certificate status verification SKIPPED
    * 	 common name: register.rare-diseases-catalyst-network.ca (matched)
    * 	 server certificate expiration date OK
    * 	 server certificate activation date OK
    * 	 certificate public key: RSA
    * 	 certificate version: #3
    * 	 subject: CN=register.rare-diseases-catalyst-network.ca
    * 	 start date: Fri, 17 May 2019 12:44:47 GMT
    * 	 expire date: Thu, 15 Aug 2019 12:44:47 GMT
    * 	 issuer: C=US,O=Let's Encrypt,CN=Let's Encrypt Authority X3
    * 	 compression: NULL 
    * ALPN, server did not agree to a protocol
    > GET /api HTTP/1.1
    > Host: register.rare-diseases-catalyst-network.ca
    > User-Agent: curl/7.47.0
    > Accept: */*
    > 
    < HTTP/1.1 200 
    < Date: Tue, 02 Jul 2019 19:05:48 GMT
    < Server: Apache/2.4.6 (CentOS) OpenSSL/1.0.2k-fips PHP/5.4.16
    < X-Content-Type-Options: nosniff
    < X-XSS-Protection: 1; mode=block
    < Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    < Pragma: no-cache
    < Expires: 0
    < X-Frame-Options: DENY
    < Content-Type: application/json;charset=UTF-8
    < Vary: Accept-Encoding
    < Transfer-Encoding: chunked
    < 
    * Connection #0 to host register.rare-diseases-catalyst-network.ca left intact
    {"message":"This is this applications API. Please see documentation.","version":"1.0.0"}
    ```

* **Notes:**

  None