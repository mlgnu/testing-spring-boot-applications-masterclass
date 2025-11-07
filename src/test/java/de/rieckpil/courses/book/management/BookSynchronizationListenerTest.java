package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSynchronizationListenerTest {

  private static final String VALID_ISBN = "1234567891234";

  @Mock private BookRepository bookRepository;

  @Mock private OpenLibraryApiClient openLibraryApiClient;

  @InjectMocks private BookSynchronizationListener cut;

  @Captor private ArgumentCaptor<Book> bookArgumentCaptor;

  @Test
  @DisplayName("should reject book when ISBN is malformed")
  void shouldRejectBookWhenIsbnIsMalformed() {
    BookSynchronization bookSynchronization = new BookSynchronization("32");
    cut.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient, bookRepository);
  }

  @Test
  @DisplayName("shouldn't override when book already exists")
  void shouldNotOverrideWhenBookAlreadyExists() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(new Book());
    cut.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient);
    verify(bookRepository, times(0)).save(ArgumentMatchers.any());
  }

  @Test
  @DisplayName("should throw exception when processing fails")
  void shouldThrowExceptionWhenProcessingFails() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenThrow(new RuntimeException("Network timeout"));

    Assertions.assertThrows(RuntimeException.class, () -> cut.consumeBookUpdates(bookSynchronization));
  }

  @Test
  @DisplayName("should store book when new and correct ISBN")
  void shouldStoreBookWhenNewAndCorrectIsbn() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);

    Book requestedBook = new Book();
    requestedBook.setTitle("Head First Design Patterns");
    requestedBook.setIsbn(VALID_ISBN);

    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenReturn(requestedBook);
    when(bookRepository.save(ArgumentMatchers.any())).then(invocation -> {
        Book methodArgument = invocation.getArgument(0);
        methodArgument.setId(1L);
        return methodArgument;
      });

    cut.consumeBookUpdates(bookSynchronization);

    verify(bookRepository.save(bookArgumentCaptor.capture()));

    Book methodArgument = bookArgumentCaptor.getValue();
    assertEquals(VALID_ISBN, methodArgument.getIsbn());
    assertEquals("Head First Design Patterns", methodArgument.getTitle());
  }
}
