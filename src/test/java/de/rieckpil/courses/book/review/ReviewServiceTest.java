package de.rieckpil.courses.book.review;

import de.rieckpil.courses.book.management.Book;
import de.rieckpil.courses.book.management.BookRepository;
import de.rieckpil.courses.book.management.User;
import de.rieckpil.courses.book.management.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewVerifier reviewVerifier;

  @Mock private UserService userService;

  @Mock private BookRepository bookRepository;

  @Mock private ReviewRepository reviewRepository;

  @InjectMocks private ReviewService cut;

  private static final String EMAIL = "duke@spring.io";
  private static final String USERNAME = "duke";
  private static final String ISBN = "42";

  @Test
  @DisplayName("test dependencies shouldn't be null")
  void shouldNotBeNull() {
    assertNotNull(reviewRepository);
    assertNotNull(reviewVerifier);
    assertNotNull(userService);
    assertNotNull(bookRepository);
    assertNotNull(cut);
  }

  @Test
  @DisplayName("should throw exception when reviewed book doesn't exist")
  void shouldThrowExceptionWhenReviewedBookIsNotExisting() {
    when(bookRepository.findByIsbn(ISBN)).thenReturn(null);

    assertThrows(
      IllegalArgumentException.class, () ->
        cut.createBookReview(ISBN, null, USERNAME, EMAIL)
    );
  }

  @Test
  @DisplayName("should reject review when review quality is bad")
  void shouldRejectReviewWhenReviewQualityIsBad() {
    BookReviewRequest bookReviewRequest =
      new BookReviewRequest("Title", "Bad review", 1);

    when(bookRepository.findByIsbn(ISBN)).thenReturn(new Book());
    when(reviewVerifier.doesMeetQualityStandards(bookReviewRequest.getReviewContent())).thenReturn(false);

    assertThrows(
      BadReviewQualityException.class, () ->
        cut.createBookReview(ISBN, bookReviewRequest, USERNAME, EMAIL)
    );
    verifyNoInteractions(reviewRepository);
    verify(reviewRepository, times(0)).save(ArgumentMatchers.any(Review.class));
  }

  @Test
  @DisplayName("should store review when its quality is good and book is present")
  void shouldStoreReviewWhenReviewQualityIsGoodAndBookIsPresent() {
    BookReviewRequest bookReviewRequest =
      new BookReviewRequest("title", "good book", 5);

    when(bookRepository.findByIsbn(ISBN)).thenReturn(new Book());
    when(reviewVerifier.doesMeetQualityStandards(bookReviewRequest.getReviewContent())).thenReturn(true);
    when(userService.getOrCreateUser(USERNAME, EMAIL)).thenReturn(new User());
    when(reviewRepository.save(any(Review.class))).thenAnswer( invocation -> {
        Review reviewToSave = invocation.getArgument(0);
        reviewToSave.setId(42L);
        return reviewToSave;
      }
    );

    Long result = cut.createBookReview(ISBN, bookReviewRequest, USERNAME, EMAIL);
    assertEquals(42, result);
  }
}
