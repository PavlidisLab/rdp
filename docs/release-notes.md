# Release Notes

## 1.4.0

### Extensions to the user profile

Users can pick a researcher position to indicate if they are a Principal Investigator.

Researcher categories are now featured on the user profile and allow the search results to be narrowed by 
researcher categories. Those categories include *in vivo*, *in vitro* and *in silico* denominations.

### Organ systems

Users can select a set of organ systems they focus their research on. These organs are based on [Uberon multi-species anatomy ontology](http://www.obofoundry.org/ontology/uberon.html) 
and a set of icons kindly provided by [X Lab]() in Houston.

### Gene privacy levels

User now have more granularity in how they pick the privacy levels of their individual TIER1 and TIER2 genes. They can
either choose to have it tied to the profile.


### More robust security

RDP now fully integrates Spring Security features with its privacy levels making sure that no information leaks in the
wrong hands.

### Cached data are kept up-to-date

Cached data (including gene information, orthologs, GO terms, etc...) and their corresponding user associations are 
updated monthly and don't impede the boot time (except for GO terms and gene2go annotations).

Gene information are stored in the database, allowing very efficient queries of orthologs.

### Database improvements

Complete integration of Flyway to manage production database migrations.

Eliminate eager fetching of model relationships in favour of more selective optimized fetch join queries where it is 
truly needed.

Orthologs are now a proper many-to-many relationship that uses efficient SQL jointures.
   
### Dependencies updates

 - move to Spring Boot to 1.5.22 (latest stable release of the 1.5 series)
 - use jQuery 3.2.1 consistently
 - use external resources from CDN
