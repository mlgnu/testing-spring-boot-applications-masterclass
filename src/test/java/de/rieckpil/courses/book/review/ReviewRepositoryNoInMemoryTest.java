package de.rieckpil.courses.book.review;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewRepositoryNoInMemoryTest {

//  disabled to be able to reuse testcontainer
//  @Container
  static PostgreSQLContainer<?> container =
      new PostgreSQLContainer<>("postgres:17.2")
          .withDatabaseName("test")
          .withUsername("duke")
          .withPassword("s3cret")
          .withReuse(true);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", container::getJdbcUrl);
    registry.add("spring.datasource.password", container::getPassword);
    registry.add("spring.datasource.username", container::getUsername);
  }

  @Autowired private ReviewRepository cut;

  @BeforeAll
  static void beforeAll() {
    container.start();
  }

  @Test
  @Sql(scripts = "/scripts/INIT_REVIEW_EACH_BOOK.sql")
  @DisplayName("should get two review statistics when database contains two book with review")
  void shouldGetTwoReviewStatisticsWhenDatabaseContainsTwoBooksWithReview() {
    List<ReviewStatistic> result = cut.getReviewStatistics();

    assertEquals(2, result.get(1).getRatings());
    assertEquals(2, result.get(1).getId());
    assertEquals(new BigDecimal("3.00"), result.get(1).getAvg());
  }
}
