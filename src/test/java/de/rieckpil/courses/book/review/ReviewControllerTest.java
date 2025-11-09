package de.rieckpil.courses.book.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.rieckpil.courses.config.WebSecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.lang.reflect.Array;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
// see
// https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes#migrating-from-websecurityconfigureradapter-to-securityfilterchain
@Import(WebSecurityConfig.class)
class ReviewControllerTest {

  @MockitoBean private ReviewService reviewService;

  @Autowired private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  public void beforeEach() {
    this.objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("should return twenty non-ordered reviews when no parameter is specified")
  void shouldReturnTwentyReviewsWithoutAnyOrderWhenNoParametersAreSpecified() throws Exception {
    ArrayNode result = objectMapper.createArrayNode();

    ObjectNode statistics = objectMapper.createObjectNode();
    statistics.put("bookId", 1);
    statistics.put("isbn", 42);
    statistics.put("avg", 89.3);
    statistics.put("ratings", 2);

    result.add(statistics);

    when(reviewService.getAllReviews(20, "none")).thenReturn(result);

    this.mockMvc
      .perform(get("/api/books/reviews"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.size()", Matchers.is(1)));
  }

  @Test
  @DisplayName("shouldn't return review statistics when use isn't authenticate4d")
  void shouldNotReturnReviewStatisticsWhenUserIsUnauthenticated() throws Exception {
    this.mockMvc
      .perform(get("/api/books/reviews/statistics"))
      .andExpect(status().isUnauthorized());

    verifyNoInteractions(reviewService);
  }

  @Test
  @WithMockUser(username = "duke")
  @DisplayName("should return review statistics when user is authenticated")
  void shouldReturnReviewStatisticsWhenUserIsAuthenticated() throws Exception {
    this.mockMvc
      .perform(get("/api/books/reviews/statistics"))
      // can use .with(user("duke")
      .andExpect(status().isOk());

    verify(reviewService).getReviewStatistics();
  }

  @Test
  @DisplayName("should create a new book review for authenticated users with valid payload")
  void shouldCreateNewBookReviewForAuthenticatedUserWithValidPayload() throws Exception {
    // can use DTO and serialize it to make the request, but won't detect field naming changes when using constructor
    String requestBody = """
      {
        "reviewTitle": "Great book for learning Java!",
        "reviewContent": "this book is one of the best I've seen when it comes to Java, it teaches you the fundamentals in a good way",
        "rating": 4
      }
      """;

    when(reviewService.createBookReview(eq("42"), any(BookReviewRequest.class),
      eq("duke"), endsWith("spring.io")))
      .thenReturn(84L);

    this.mockMvc
      .perform(post("/api/books/{isbn}/reviews", 42)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .with(jwt().jwt(builder -> builder
          .claim("email", "duke@spring.io")
          .claim("preferred_username", "duke"))))
      .andExpect(status().isCreated())
      .andExpect(header().exists("Location"))
      .andExpect(header().string("Location", Matchers.containsString("/books/42/reviews/84")));
  }

  @Test
  @DisplayName("should reject book reviews for authenticated users with invalid payload")
  void shouldRejectNewBookReviewForAuthenticatedUsersWithInvalidPayload() throws Exception {
    // testing different validation rules is best done in unit tests with spring validations
    // so that web tests aren't polluted
    String requestBody = """
      {
        "reviewTitle": "Great book for learning Java!",
        "rating": -1
      }
      """;

    this.mockMvc
      .perform(post("/api/books/{isbn}/reviews", 42)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .with(jwt().jwt(builder -> builder
          .claim("email", "duke@spring.io")
          .claim("preferred_username", "duke"))))
      .andExpect(status().isBadRequest())
      .andDo(MockMvcResultHandlers.print());
  }

  @Test
  @DisplayName("shouldn't allow reviews deletion for authenticated users without moderator role")
  void shouldNotAllowDeletingReviewsWhenUserIsAuthenticatedWithoutModeratorRole()
      throws Exception {
    this.mockMvc
      .perform(delete("/api/books/{isbn}/reviews/{reviewId}", 31, 22)
        .with(jwt()))
      .andExpect(status().isForbidden());

    verifyNoInteractions(reviewService);
  }

  @Test
  @WithMockUser(roles = "moderator")
  @DisplayName("should allow reviews deletion for authenticated users that has moderator role")
  void shouldAllowDeletingReviewsWhenUserIsAuthenticatedAndHasModeratorRole() throws Exception {
    this.mockMvc
      .perform(delete("/api/books/{isbn}/reviews/{reviewId}", 31, 22))
//        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_moderator"))))
      .andExpect(status().isOk());

    verify(reviewService).deleteReview("31", 22L);
  }
}
