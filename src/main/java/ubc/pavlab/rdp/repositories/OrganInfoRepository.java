package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.OrganInfo;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface OrganInfoRepository extends JpaRepository<OrganInfo, Integer> {

    Optional<OrganInfo> findByUberonId( String id );

    Collection<OrganInfo> findByActiveTrueOrderByOrdering();

    Collection<OrganInfo> findByUberonIdIn( Collection<String> organUberonIds );
}
