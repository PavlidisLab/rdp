package ubc.pavlab.rdp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(MigrationConfig.class)
public class MigrationConfigTest {

    @Test
    public void migrateWithNoExistingDatabase() {
    }
}