**Gene search**
----
  Search for public users by description.

* **URL**

  `/api/genes/search`

* **Method:**
  
  `GET`
  
 **URL Params**

* **Required:**
    * `symbol=[alphanumeric]`
        * NCBI gene symbol.
    * `taxonId=[integer]`
        *  NCBI Taxonomy ID.
        * Accepted `taxonId` values are `{4932, 6239, 7227, 7955, 10090, 10116, 559292}`
    * `tier=[alphanumeric]`
        * Gene tier defined by the user.
        * Accepted `tier` values are `{TIER1, TIER2, ANY}`
    
   **Optional:**

  * `orthologId=[integer]`
    *  A NCBI Taxonomy ID used to search for orthologs in that given taxon.
    * Accepted `taxonId` values are `{4932, 6239, 7227, 7955, 10090, 10116, 559292}`, or `-99` can be used for none.
  * `auth=[alphanumeric]`
    *  Authentication token.
    

* **Data Params**

  None

* **Success Response:**
  
  Returns a list of users.

  * **Code:** 200 <br />
    **Content:** 
    ```
    [
      {
        "geneId": 394269,
        "taxon": {
          "id": 9606,
          "scientificName": "Homo sapiens",
          "commonName": "human",
          "ordering": 1
        },
        "symbol": "BRCA1P1",
        "name": "BRCA1 pseudogene 1",
        "aliases": "LBRCA1|PsiBRCA1|pseudo-BRCA1",
        "modificationDate": 20190213,
        "tier": "TIER2",
        "remoteUser": {
          "id": 551,
          "email": "manuel.belmadani+RDMAPI@gmail.com",
          "name": "API",
          "lastName": "TestAccount",
          "description": "This is a test account for the API.\n\nSee documentation or contact registry-help@rare-diseases-catalyst-network.ca for additional inquiries.",
          "organization": "UBC",
          "department": "Support",
          "phone": "",
          "website": "http://register.rare-diseases-catalyst-network.ca",
          "privacyLevel": 2,
          "shared": true,
          "hideGenelist": false,
          "publications": [],
          "origin": "RDMMN",
          "originUrl": "https://register.rare-diseases-catalyst-network.ca/",
          "userTerms": [],
          "userGenes": {}
        }
      }
    ]
    ```
 
* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `NO CONTENT`
    **Reason:** Invalid endpoint, or missing a required parameter.

  * **Code:** 404 NOT FOUND <br />
    **Content:** `Unknown gene.`
    **Reason:** Using a an invalid combination of `taxonId` and `symbol`, or a gene/taxon not known to the database.

  * **Code:** 404 NOT FOUND <br />
    **Content:** `Could not find any orthologs with given parameters.`
    **Reason:** Using a an invalid `orthologId` in combination of a given `taxonId` and `symbol`.
 
  * **Code:** 404 NOT FOUND <br />
    **Content:** `Tier3 genes not published to partner registires.`
    **Reason:** Using TIER3 as the `tier` parameter. TIER3 genes are not supported by the API

  * **Code:** 404 NOT FOUND <br />
    **Content:**
    ```
    {
        "timestamp": 1562189118085,
        "status": 400,
        "error": "Bad Request",
        "exception": "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException",
        "message": "Failed to convert value of type 'java.lang.String' to required type 'ubc.pavlab.rdp.model.enums.TierType'; nested exception is org.springframework.core.convert.ConversionFailedException: Failed to convert from type [java.lang.String] to type [@org.springframework.web.bind.annotation.RequestParam ubc.pavlab.rdp.model.enums.TierType] for value 'TIER4'; nested exception is java.lang.IllegalArgumentException: No enum constant ubc.pavlab.rdp.model.enums.TierType.TIER4",
        "path": "/api/genes/
    }
    ```
    **Reason:** Using an invalid TIER value.

  * **Code:** 400 BAD REQUEST <br />
    **Content:** 
    ```
                    {
                    "timestamp": 1562188847011,
                    "status": 400,
                    "error": "Bad Request",
                    "exception": "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException",
                    "message": "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; nested exception is java.lang.NumberFormatException: For input string: \"AAA\"",
                    "path": "/api/genes/search/"
                    }
    ```
    **Reason:** Using an invalid type value, such as characther for the `taxonId`.

* **Sample Call:**
    Using cURL:
  ```bash
  curl -L -v "http://register.rare-diseases-catalyst-network.ca/api/genes/search/?symbol=BRCA1P1&taxonId=9606&tier=TIER2&orthologTaxonId=-99" 2> /dev/null 
  ```
  
    Output:
    ```
    [
      {
        "geneId": 394269,
        "taxon": {
          "id": 9606,
          "scientificName": "Homo sapiens",
          "commonName": "human",
          "ordering": 1
        },
        "symbol": "BRCA1P1",
        "name": "BRCA1 pseudogene 1",
        "aliases": "LBRCA1|PsiBRCA1|pseudo-BRCA1",
        "modificationDate": 20190213,
        "tier": "TIER2",
        "remoteUser": {
          "id": 551,
          "email": "manuel.belmadani+RDMAPI@gmail.com",
          "name": "API",
          "lastName": "TestAccount",
          "description": "This is a test account for the API.\n\nSee documentation or contact registry-help@rare-diseases-catalyst-network.ca for additional inquiries.",
          "organization": "UBC",
          "department": "Support",
          "phone": "",
          "website": "http://register.rare-diseases-catalyst-network.ca",
          "privacyLevel": 2,
          "shared": true,
          "hideGenelist": false,
          "publications": [],
          "origin": "RDMMN",
          "originUrl": "https://register.rare-diseases-catalyst-network.ca/",
          "userTerms": [],
          "userGenes": {}
        }
      }
    ]
    ```

* **Notes:**
* The cURL option `-L` might be required to follow redirects. 
* Redirecting the cURL standard error output to `/dev/null` with `2>` keeps only the response object and prevents cURL messages from mangling the output.