package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.OrganInfo;

@Repository
public interface OrganInfoRepository extends JpaRepository<OrganInfo, Integer> {

}
