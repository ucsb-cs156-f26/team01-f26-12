package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Articles;
import edu.ucsb.cs156.example.repositories.ArticlesRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = ArticlesController.class)
@Import(TestConfig.class)
public class ArticlesControllerTests extends ControllerTestCase {

  @MockitoBean ArticlesRepository articlesRepository;

  @MockitoBean UserRepository userRepository;

  // ---------- GET /all ----------

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/Articles/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/Articles/all")).andExpect(status().is(200));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_articles() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2022-03-11T00:00:00");

    Articles a1 =
        Articles.builder()
            .title("Title One")
            .url("https://example.com/one")
            .explanation("First article")
            .email("one@ucsb.edu")
            .dateAdded(ldt1)
            .build();

    Articles a2 =
        Articles.builder()
            .title("Title Two")
            .url("https://example.com/two")
            .explanation("Second article")
            .email("two@ucsb.edu")
            .dateAdded(ldt2)
            .build();

    ArrayList<Articles> expected = new ArrayList<>(Arrays.asList(a1, a2));
    when(articlesRepository.findAll()).thenReturn(expected);

    MvcResult response =
        mockMvc.perform(get("/api/Articles/all")).andExpect(status().isOk()).andReturn();

    verify(articlesRepository, times(1)).findAll();
    assertEquals(mapper.writeValueAsString(expected), response.getResponse().getContentAsString());
  }

  // ---------- POST /post ----------

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/Articles/post")
                .param("title", "T")
                .param("url", "https://example.com")
                .param("explanation", "E")
                .param("email", "a@ucsb.edu")
                .param("dateAdded", "2022-01-03T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/Articles/post")
                .param("title", "T")
                .param("url", "https://example.com")
                .param("explanation", "E")
                .param("email", "a@ucsb.edu")
                .param("dateAdded", "2022-01-03T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_article() throws Exception {
    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");

    Articles article =
        Articles.builder()
            .title("Title One")
            .url("https://example.com/one")
            .explanation("First article")
            .email("one@ucsb.edu")
            .dateAdded(ldt)
            .build();

    when(articlesRepository.save(eq(article))).thenReturn(article);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/Articles/post")
                    .param("title", "Title One")
                    .param("url", "https://example.com/one")
                    .param("explanation", "First article")
                    .param("email", "one@ucsb.edu")
                    .param("dateAdded", "2022-01-03T00:00:00")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(articlesRepository, times(1)).save(article);
    assertEquals(mapper.writeValueAsString(article), response.getResponse().getContentAsString());
  }

  // ---------- GET by id ----------

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/Articles").param("id", "7")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
    Articles article =
        Articles.builder()
            .title("Title One")
            .url("https://example.com/one")
            .explanation("First article")
            .email("one@ucsb.edu")
            .dateAdded(LocalDateTime.parse("2022-01-03T00:00:00"))
            .build();

    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.of(article));

    MvcResult response =
        mockMvc
            .perform(get("/api/Articles").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(articlesRepository, times(1)).findById(eq(7L));
    assertEquals(mapper.writeValueAsString(article), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
    when(articlesRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/Articles").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(articlesRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Articles with id 7 not found", json.get("message"));
  }

  // ---------- PUT ----------

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_article() throws Exception {
    Articles orig =
        Articles.builder()
            .title("Title One")
            .url("https://example.com/one")
            .explanation("First article")
            .email("one@ucsb.edu")
            .dateAdded(LocalDateTime.parse("2022-01-03T00:00:00"))
            .build();

    Articles edited =
        Articles.builder()
            .title("Title Edited")
            .url("https://example.com/edited")
            .explanation("Edited article")
            .email("edited@ucsb.edu")
            .dateAdded(LocalDateTime.parse("2023-01-03T00:00:00"))
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(articlesRepository.findById(eq(67L))).thenReturn(Optional.of(orig));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/Articles")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(articlesRepository, times(1)).findById(67L);
    verify(articlesRepository, times(1)).save(edited);
    assertEquals(requestBody, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_article_that_does_not_exist() throws Exception {
    Articles edited =
        Articles.builder()
            .title("Title Edited")
            .url("https://example.com/edited")
            .explanation("Edited article")
            .email("edited@ucsb.edu")
            .dateAdded(LocalDateTime.parse("2023-01-03T00:00:00"))
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(articlesRepository.findById(eq(67L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/Articles")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(articlesRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("Articles with id 67 not found", json.get("message"));
  }
}
