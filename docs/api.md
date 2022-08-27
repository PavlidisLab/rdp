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
with a secret access token.

```http
GET /api/** HTTP/1.1
Authorization: Bearer {accessToken}
```

Passing the authorization token via `auth` query parameter is deprecated as of 1.4.0.

```http
GET /api/users?auth={accessToken} HTTP/1.1
```

Keep in mind that the token you use is tied to a user that has permissions. What you can see through the API will be
restricted to what the corresponding user is authorized to read.

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

## List all categories/ontologies (new in 1.5.0)

```http
GET /api/ontologies HTTP/1.1
```

## List all terms in a category/ontology (new in 1.5.0)

```http
GET /api/ontologies/{ontologyName}/terms HTTP/1.1
```

- `page` the page to query starting from zero to `totalPages`

## Retrieve a single category/ontology term (new in 1.5.0)

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

## Find a user gene by its anonymous identifier (new in 1.5.0)

Retrieves an anonymized result from a previous [user gene search](#search-genes).

```http
GET /api/genes/by-anonymous-id/{anonymousId} HTTP/1.1
```

## Statistics

Obtain a few statistics for the number of registered users, genes, etc.

```http
GET /api/stats HTTP/1.1
```

The `version` field was added in 1.5.0. Previously, you had to parse the `/api` endpoint and extract the version
from `info.version`.

Example output:

```json
{
  "version": "1.5.0",
  "users": 2,
  "publicUsers": 0,
  "usersWithGenes": 1,
  "userGenes": 2,
  "uniqueUserGenes": 2,
  "uniqueUserGenesTAll": 2,
  "uniqueUserGenesHumanTAll": 2,
  "researchersByTaxa": {
    "mouse": 1,
    "budding yeast": 0,
    "roundworm": 0,
    "frog": 0,
    "rat": 0,
    "e. coli": 0,
    "fruit fly": 0,
    "fission yeast": 0,
    "zebrafish": 0,
    "human": 0
  }
}
```

