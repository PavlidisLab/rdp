**User search**
----
  Search for public users

* **URL**

  /api/users/search

* **Method:**
  
  `GET`
  
*  **URL Params**

   **Required:**
   
   `nameLike=[alphanumeric]`
   `prefix=[boolean]`

   **Optional:**
 
   `auth=[alphanumeric]`

* **Data Params**

  None

* **Success Response:**
  
  Returns a list of users.

  * **Code:** 200 <br />
    **Content:** 
```
[
   {
      "id":551,
      "email":"manuel.belmadani+RDMAPI@gmail.com",
      "name":"API",
      "lastName":"TestAccount",
      "description":"This is a test account for the API.\n\nSee documentation or contact registry-help@rare-diseases-catalyst-network.ca for additional inquiries.",
      "organization":"UBC",
      "department":"Support",
      "phone":"",
      "website":"http://register.rare-diseases-catalyst-network.ca",
      "privacyLevel":2,
      "shared":true,
      "hideGenelist":false,
      "publications":[

      ],
      "origin":"RDMMN",
      "originUrl":"https://register.rare-diseases-catalyst-network.ca/",
      "userTerms":[

      ],
      "userGenes":{
         "100533695":{
            "geneId":100533695,
            "taxon":{
               "id":9606,
               "scientificName":"Homo sapiens",
               "commonName":"human",
               "ordering":1
            },
            "symbol":"RLIMP1",
            "name":"ring finger protein, LIM domain interacting pseudogene 1",
            "aliases":"-",
            "modificationDate":20190213,
            "tier":"TIER2",
            "remoteUser":null
         },
         "100505876":{
            "geneId":100505876,
            "taxon":{
               "id":9606,
               "scientificName":"Homo sapiens",
               "commonName":"human",
               "ordering":1
            },
            "symbol":"CEBPZOS",
            "name":"CEBPZ opposite strand",
            "aliases":"CEBPZ-AS1",
            "modificationDate":20190415,
            "tier":"TIER2",
            "remoteUser":null
         },
         "394269":{
            "geneId":394269,
            "taxon":{
               "id":9606,
               "scientificName":"Homo sapiens",
               "commonName":"human",
               "ordering":1
            },
            "symbol":"BRCA1P1",
            "name":"BRCA1 pseudogene 1",
            "aliases":"LBRCA1|PsiBRCA1|pseudo-BRCA1",
            "modificationDate":20190213,
            "tier":"TIER2",
            "remoteUser":null
         }
      }
   }
]
```
 
* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `NO CONTENT`
    **Reason:** Invalid endpoint.

  * **Code:** 302 FOUND <br />
    **Content:** `NO CONTENT`
    **Reason:** Could not follow redirects. For cURL, this is fixed by using the `-L` option.

  * **Code:** 400 BAD REQUEST <br />
    **Content:** 
    ```
    {
        "timestamp": 1562186167149,
        "status": 400,
        "error": "Bad Request",
        "exception": "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException",
        "message": "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Boolean'; nested exception is java.lang.IllegalArgumentException: Invalid boolean value [not_a_boolean]",
        "path": "/api/users/search/"
    }
    ```
    **Reason:** Did not use a valid boolean (true|false) for the prefix parameter.

* **Sample Call:**
    Using cURL:
  ```bash
  curl -L -v "http://register.rare-diseases-catalyst-network.ca/api/users/search/?nameLike=TestAccount&prefix=false" 2> /dev/null 
  ```
  
    Output:
    ```
    [{"id":551,"email":"manuel.belmadani+RDMAPI@gmail.com","name":"API","lastName":"TestAccount","description":"This is a test account for the API.\n\nSee documentation or contact registry-help@rare-diseases-catalyst-network.ca for additional inquiries.","organization":"UBC","department":"Support","phone":"","website":"http://register.rare-diseases-catalyst-network.ca","privacyLevel":2,"shared":true,"hideGenelist":false,"publications":[],"origin":"RDMMN","originUrl":"https://register.rare-diseases-catalyst-network.ca/","userTerms":[],"userGenes":{"100533695":{"geneId":100533695,"taxon":{"id":9606,"scientificName":"Homo sapiens","commonName":"human","ordering":1},"symbol":"RLIMP1","name":"ring finger protein, LIM domain interacting pseudogene 1","aliases":"-","modificationDate":20190213,"tier":"TIER2","remoteUser":null},"100505876":{"geneId":100505876,"taxon":{"id":9606,"scientificName":"Homo sapiens","commonName":"human","ordering":1},"symbol":"CEBPZOS","name":"CEBPZ opposite strand","aliases":"CEBPZ-AS1","modificationDate":20190415,"tier":"TIER2","remoteUser":null},"394269":{"geneId":394269,"taxon":{"id":9606,"scientificName":"Homo sapiens","commonName":"human","ordering":1},"symbol":"BRCA1P1","name":"BRCA1 pseudogene 1","aliases":"LBRCA1|PsiBRCA1|pseudo-BRCA1","modificationDate":20190213,"tier":"TIER2","remoteUser":null}}}]
    ```

* **Notes:**
* The cURL option `-L` might be required to follow redirects.
* Redirecting the cURL standard error output to `/dev/null` with `2>` keeps only the response object and prevents cURL messages from mangling the output.