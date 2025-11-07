package de.rieckpil.courses.book.review;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static de.rieckpil.courses.book.review.RandomReviewParameterResolverExtension.RandomReview;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RandomReviewParameterResolverExtension.class)
class ReviewVerifierTest {

  private ReviewVerifier reviewVerifier;

  @BeforeEach
  void setup() {
    reviewVerifier = new ReviewVerifier();
  }

  @Test
  void shouldFailWhenReviewContainsSwearWord() {
    String review = "This book is shit";
    System.out.println("Testing a review");

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier did not detect swear word");
  }

  @Test
  @DisplayName("Should fail when review contains 'lorem ipsum'")
  void testLoremIpsum() {
    String review = "lorem ipsum this is a good book";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier didn't detect lorem ipsum");
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/badReview.csv")
  void shouldFailWhenReviewIsOfBadQuality(String review) {
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier didn't detect bad review");
  }

  @RepeatedTest(5)
  void shouldFailWhenRandomReviewQualityIsBad(@RandomReview String review) {
    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertFalse(result, "ReviewVerifier didn't detect a random bad quality review");
  }

  @Test
  void shouldPassWhenReviewIsGood() {
    String review = "I really recommend this book to whoever is interested in advancing their carrier";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertTrue(result, "ReviewVerifier didn't pass a good review");
  }

  @Test
  @DisplayName("should pass when review is good Hamcrest")
  void shouldPassWhenReviewIsGoodHamcrest() {
    String review = "I really recommend this book to whoever is interested in advancing their carrier";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    assertThat("ReviewVerifier didn't pass a good review", result, equalTo(true));
  }

  @Test
  @DisplayName("should pass when review is good AssertJ")
  void shouldPassWhenReviewIsGoodAssertJ() {
    String review = "I really recommend this book to whoever is interested in advancing their carrier";

    boolean result = reviewVerifier.doesMeetQualityStandards(review);
    Assertions.assertThat(result)
      .withFailMessage("ReviewVerifier didn't pass a good review")
      .isEqualTo(true);
  }
}
