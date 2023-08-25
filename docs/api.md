# API

RDP provides an extensive RESTful API to perform gene searches, obtain user details, etc.

## OpenAPI specification

The [OpenAPI](https://www.openapis.org/) specification can be retrieved to get a complete description of the endpoints
and accepted parameters.

```http
GET /api HTTP/1.1
```

## CORS

As of 1.5.0, a [CORS policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) has been established to allow
scripts served from the main website to freely access the API endpoints.

In your `application.properties`, set `rdp.site.mainsite` to your main website URL:

```properties
rdp.site.mainsite=https://example.com
```

## Authentication

Clients can authenticate on any API endpoint using a secret access token attached to their account. This allows them to
perform the action with their level of access.

For partner registry administrators, the access can instead be authenticated with the remote admin using one of the
token listed in `rdp.settings.isearch.auth-tokens` as described in
the [International data](customization.md#international-data)
customization section.

For external services, authentication should instead be performed with using a [service account](service-accounts.md)
with an access token and a secret.

```http
GET /api/** HTTP/1.1
Authorization: Bearer {accessToken}:{secret}
```

Tokens created prior to the 1.6 release may omit the `:{secret}` part. We highly recommend that you revoke and
regenerate new tokens with secrets.

Passing the authorization token via `auth` query parameter is deprecated as of 1.4.0.

```http
GET /api/users?auth={accessToken} HTTP/1.1
```

Keep in mind that the token you use is tied to a user that has permissions. What you can see through the API will be
restricted to what the corresponding user is authorized to read.

## Anonymized results

If `rdp.settings.privacy.enable-anonymized-search-results` is set to `true`, certain results may be anonymized depending
on your permissions.

These results will have an `anonymousId` attribute that can subsequently be used to retrieve the
result with elevated privileges via either the [user by anonymous ID](#find-a-user-by-its-identifier)
or [gene by anonymous ID](#find-a-user-gene-by-its-anonymous-identifier) endpoint.

The way we envision this is that the user will forward the anonymized ID to a registry administrator which can then
mediate the interaction between the two users.

For example, an initial unauthenticated request might return a few anonymized results:

```http
GET /api/users HTTP/1.1
```

```json
{
  "content": [
    {
      "anonymousId": "7d0aaaa3-e119-4ff1-a280-b0a94d510ab0",
      "email": null,
      "name": "RDP User",
      "...": "..."
    },
    {
      "...": "..."
    }
  ]
}
```

The get more information about that user, you may perform a request with elevated privileges using a token-based
authentication:

```http
GET /api/users/by-anonymous-id/7d0aaaa3-e119-4ff1-a280-b0a94d510ab0 HTTP/1.1
Authorization: Bearer k1j2okjd98e3u1
```

```json
{
  "id": 113,
  "email": "foo@example.com",
  "...": "..."
}
```

## List all users

List all users in a paginated format.

```http
GET /api/users HTTP/1.1
```

- `page` the page to query starting from zero to `totalPages`

If `rdp.settings.privacy.enable-anonymized-search-results` is set to `true`, anonymized results are included in the
output.

## List all genes

List all genes in a paginated format.

```http
GET /api/genes HTTP/1.1
```

- `page` the page to query starting from zero to `totalPages`

If `rdp.settings.privacy.enable-anonymized-search-results` is set to `true`, anonymized results are included in the
output.

## Retrieve a single taxon

!!! note

    New in 1.5.0.

```http 
GET /api/taxa/{taxonId} HTTP/1.1
```

This can be used in conjunction to the [statistics](#statistics) endpoint to get more details about taxa retrieved in
the researcher count mapping.

## List all categories/ontologies

!!! note

    New in 1.5.0.

```http
GET /api/ontologies HTTP/1.1
```

## List all terms in a category/ontology

!!! note

    New in 1.5.0.

```http
GET /api/ontologies/{ontologyName}/terms HTTP/1.1
```

- `page` the page to query starting from zero to `totalPages`

## List specific terms in a category/ontology

!!! note

    New in 1.5.0.

```http
GET /api/ontologies/{ontologyName}/terms?ontologyTermIds HTTP/1.1
```

To retrieve specific terms, you may use `ontologyTermIds` query parameter and pass it as many time as you want. The
output is
not paginated and the `page` parameter
from [List all terms in a category/ontology](#list-all-terms-in-a-categoryontology) is ignored.

## Retrieve a single category/ontology term

!!! note

    New in 1.5.0.

```http
GET /api/ontologies/{ontologyName}/terms/{termId}
```

## Search users

Search users by name or description.

```http
GET /api/users/search HTTP/1.1
```

By name:

- `nameLike` a name in the user profile
- `prefix` a boolean `true` or `false` whether the name search is matching a prefix

By description:

- `descriptionLike` a description in the user profile

By name and description: (new in 1.5.0)

- `nameLike`
- `prefix`
- `descriptionLike`

In any case, you can narrow down the result with the following query parameters:

- `researcherPositions` a set of researcher positions, only
  `PRINCIPAL_INVESTIGATOR` is currently supported
- `researcherCategories` a set of researcher category like `IN_SILICO` or `IN_VITRO`
- `organUberonIds` a set of Uberon identifiers that the user has added to its profile as organ systems
- `ontologyNames` and `ontologyTermIds` two lists of corresponding ontology names and term IDs (new in 1.5.0)

!!! warning

    If an ontology cannot be retrieved by `ontologyNames`, a `400 Bad Request` status will be emitted in the response.
    You should use the [list all categories/ontologies](#list-all-terms-in-a-categoryontology) endpoint prior to
    supplying terms.

    No error will be produced if some of the terms do not exist, however no results will be returned.

## Search genes

Search user genes by symbols.

```http
GET /api/genes/search HTTP/1.1
```

- `symbol` a gene symbol
- `taxonId` a taxon identifier in which the search is performed
- `orthologTaxonId` an ortholog taxon identifier, omitting it searches all taxa
- `tier` a single tier to filter by (deprecated as of 1.4.0 in favour of `tiers`)
- `tiers` a set of tiers including `TIER1`, `TIER2` and `TIER3`

Then any of the following as described in "Search users":

- `researcherPositions`
- `researcherCategories`
- `organUberonIds`
- `ontologyNames` and `ontologyTermIds` two lists of corresponding ontology names and term IDs (new in 1.5.0)

!!! warning

    If an ontology cannot be retrieved by `ontologyNames`, a `400 Bad Request` status will be emitted in the response.
    You should use the [list all categories/ontologies](#list-all-terms-in-a-categoryontology) endpoint prior to
    supplying terms.

    No error will be produced if some of the terms do not exist, however no results will be returned.

## Find a user by its identifier

Find a user given its real or anonymous identifier.

```http
GET /api/users/{userId} HTTP/1.1
```

Or anonymously:

```http
GET /api/users/by-anonymous-id/{anonymousId} HTTP/1.1
```

The anonymous identifier can be obtained by performing a users or genes search and retrieving the `anonymousId` from its
result.

## Find a user gene by its anonymous identifier

!!! note

    New in 1.5.0.

Retrieves an anonymized result from a previous [user gene search](#search-genes).

```http
GET /api/genes/by-anonymous-id/{anonymousId} HTTP/1.1
```

## Statistics

Obtain a few statistics for the number of registered users, genes, etc.

```http
GET /api/stats HTTP/1.1
```

!!! note

    The `version` field was added in 1.5.0. Previously, you had to parse the `/api` endpoint and extract the version
    from `info.version`.

!!! warning

    This endpoint was stabilized in the 1.5.0 release and some of its attributes have been renamed as a result. This 
    is noted in the [1.5 migration](migration.md#migrate-from-14-to-15) notes.

Example output:

```json
{
  "version": "1.5.0",
  "users": 22,
  "publicUsers": 0,
  "usersWithGenes": 4,
  "userGenes": 18,
  "uniqueUserGenes": 18,
  "uniqueUserGenesInAllTiers": 18,
  "uniqueHumanUserGenesInAllTiers": 30,
  "researchersByTaxonId": {
    "4896": 0,
    "7955": 0,
    "559292": 0,
    "10116": 1,
    "9606": 1,
    "10090": 2,
    "7227": 0,
    "8364": 0,
    "6239": 1
  }
}
```

To obtain more details about the taxon, you may use the [taxon endpoint](#retrieve-a-single-taxon). Note that JSON does
not allow integer has keys in a mapping, but those can be safely treated as such.