package ru.andreevcode.logicore.corelogistics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CoreLogisticsApplicationIT extends BaseIT {

    @Test
    void contextLoads() {
    }

}
