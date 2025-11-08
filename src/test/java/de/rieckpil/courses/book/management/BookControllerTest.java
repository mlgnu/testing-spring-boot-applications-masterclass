package de.rieckpil.courses.book.management;

import de.rieckpil.courses.config.WebSecurityConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
// see
// https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes#migrating-from-websecurityconfigureradapter-to-securityfilterchain
@Import(WebSecurityConfig.class)
class BookControllerTest {

  @MockitoBean private BookManagementService bookManagementService;

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("should get empty array when no book exists")
  void shouldGetEmptyArrayWhenNoBooksExists() throws Exception {
    MvcResult mvcResult = this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.size()", Matchers.is(0)))
      .andDo(MockMvcResultHandlers.print())
      .andReturn();
  }

  @Test
  @DisplayName("shouldn't return XML")
  void shouldNotReturnXML() throws Exception {
    MvcResult mvcResult = this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML))
      .andExpect(status().isNotAcceptable())
      .andReturn();
  }

  @Test
  void shouldGetBooksWhenServiceReturnsBooks() throws Exception {

    Book book1 = createBook(1L, "42", "Java 22", "Taha", "Master Java",
      "Software Engineering", 200L, "Oracle", "https://java.com/22");

    Book book2 = createBook(2L, "43", "Java 25", "Taha", "Master Java",
      "Software Engineering", 200L, "Oracle", "https://java.com/25");

    when(bookManagementService.getAllBooks()).thenReturn(List.of(book1, book2));

    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.size()", Matchers.is(2)))
      .andExpect(jsonPath("$.[0].isbn", Matchers.is("42")))
      .andExpect(jsonPath("$.[0].title", Matchers.is("Java 22")))
      .andExpect(jsonPath("$.[1].isbn", Matchers.is("43")))
      .andExpect(jsonPath("$.[1].title", Matchers.is("Java 25")))
      .andReturn();
  }

  private Book createBook(
      Long id,
      String isbn,
      String title,
      String author,
      String description,
      String genre,
      Long pages,
      String publisher,
      String thumbnailUrl) {
    Book result = new Book();
    result.setId(id);
    result.setIsbn(isbn);
    result.setTitle(title);
    result.setAuthor(author);
    result.setDescription(description);
    result.setGenre(genre);
    result.setPages(pages);
    result.setPublisher(publisher);
    result.setThumbnailUrl(thumbnailUrl);
    return result;
  }
}
