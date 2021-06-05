# Release Notes

## 1.4.0

### Service accounts

[Service accounts](service-accounts.md) are special kind of users that are used to authenticate external services that
integrate with RDP.

### Extensions to the user profile

Users can pick a researcher position to indicate if they are a Principal Investigator.

User profiles now feature researcher categories, allowing search results to be narrowed by a specific kind of expertise. 
These categories include *in vivo*, *in vitro* and *in silico* denominations.

### Organ systems

Users can select a set of organ systems they focus their research on. These organs are based on [Uberon multi-species anatomy ontology](http://www.obofoundry.org/ontology/uberon.html) 
and a set of icons kindly provided by [Shinya Yamamoto Lab](https://www.researchgate.net/lab/Shinya-Yamamoto-Lab) in Houston.

### Gene privacy levels

User now have more granularity in how they pick the privacy levels of their individual TIER1 and TIER2 genes. They can
either choose to have it tied to the profile.

### Anonymized search results and match request

Search results that are not available to a given user for privacy reasons can be anonymized. A new feature called
"Request Access" allow the user to reach out to the researcher.

### More robust security

RDP now fully integrates Spring Security features with its privacy levels making sure that no information leaks in the
wrong hands.

### Up-to-date cached data

Cached data (including gene information, orthologs, GO terms, etc...) and their corresponding user associations are 
updated monthly and don't impede the boot time (except for GO terms and gene2go annotations).

Gene information are stored in the database, allowing very efficient queries of orthologs.

### Database improvements

Complete integration of Flyway to manage production database migrations.

Initial data is part of the migration process.

Eliminate eager fetching of model relationships in favour of more selective optimized fetch join queries where it is 
truly needed.

Orthologs are now a proper many-to-many relationship that uses efficient SQL jointures.

### Canonical search URLs

Search feature not produces canonical URLs that can be shared.
   
### Dependencies updates

 - move to Spring Boot to 1.5.22 (latest stable release of the 1.5 series)
 - use jQuery 3.2.1 consistently
 - use external resources from CDN
