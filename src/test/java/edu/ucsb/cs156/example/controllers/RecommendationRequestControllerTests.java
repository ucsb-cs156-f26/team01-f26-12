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
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
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

@WebMvcTest(controllers = RecommendationRequestController.class)
@Import(TestConfig.class)
public class RecommendationRequestControllerTests extends ControllerTestCase {

  @MockitoBean RecommendationRequestRepository recommendationRequestRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for /api/recommendationrequests/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/recommendationrequests/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/recommendationrequests/all")).andExpect(status().is(200)); // logged
  }

  // Authorization tests for /api/recommendationrequests/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendationrequests/post")
                .param("requesterEmail", "tom@gmail.com")
                .param("professorEmail", "douglas@gmail.com")
                .param("explanation", "caltech phd for cs")
                .param("dateRequested", "2022-01-03T00:00:00")
                .param("dateNeeded", "2022-01-03T00:00:01")
                .param("done", "true")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendationrequests/post")
                .param("requesterEmail", "tom@gmail.com")
                .param("professorEmail", "douglas@gmail.com")
                .param("explanation", "caltech phd for cs")
                .param("dateRequested", "2022-01-03T00:00:00")
                .param("dateNeeded", "2022-01-03T00:00:01")
                .param("done", "true")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange
    LocalDateTime dr = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime dn = LocalDateTime.parse("2022-01-03T00:00:01");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("tom@gmail.com")
            .professorEmail("douglas@gmail.com")
            .explanation("caltech phd for cs")
            .dateRequested(dr)
            .dateNeeded(dn)
            .done(true)
            .build();

    when(recommendationRequestRepository.findById(eq(7L)))
        .thenReturn(Optional.of(recommendationRequest));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequests").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(recommendationRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequests").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("RecommendationRequest with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_recommendationrequests() throws Exception {

    // arrange
    LocalDateTime dr1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime dn1 = LocalDateTime.parse("2022-01-03T00:00:01");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .requesterEmail("tom@gmail.com")
            .professorEmail("douglas@gmail.com")
            .explanation("caltech phd for cs")
            .dateRequested(dr1)
            .dateNeeded(dn1)
            .done(true)
            .build();

    LocalDateTime dr2 = LocalDateTime.parse("2026-04-02T12:21:01");
    LocalDateTime dn2 = LocalDateTime.parse("2026-06-06T10:23:02");

    RecommendationRequest recommendationRequest2 =
        RecommendationRequest.builder()
            .requesterEmail("mary@gmail.com")
            .professorEmail("bob@gmail.com")
            .explanation("uc berkeley phd for physics")
            .dateRequested(dr2)
            .dateNeeded(dn2)
            .done(false)
            .build();

    ArrayList<RecommendationRequest> expectedDates = new ArrayList<>();
    expectedDates.addAll(Arrays.asList(recommendationRequest1, recommendationRequest2));

    when(recommendationRequestRepository.findAll()).thenReturn(expectedDates);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequests/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(recommendationRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedDates);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendationrequest() throws Exception {
    // arrange

    LocalDateTime dr = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime dn = LocalDateTime.parse("2022-01-03T00:00:01");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .requesterEmail("tom@gmail.com")
            .professorEmail("douglas@gmail.com")
            .explanation("caltech phd for cs")
            .dateRequested(dr)
            .dateNeeded(dn)
            .done(true)
            .build();

    when(recommendationRequestRepository.save(eq(recommendationRequest1)))
        .thenReturn(recommendationRequest1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequests/post")
                    .param("requesterEmail", "tom@gmail.com")
                    .param("professorEmail", "douglas@gmail.com")
                    .param("explanation", "caltech phd for cs")
                    .param("dateRequested", "2022-01-03T00:00:00")
                    .param("dateNeeded", "2022-01-03T00:00:01")
                    .param("done", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).save(recommendationRequest1);
    String expectedJson = mapper.writeValueAsString(recommendationRequest1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_recommendation_request_that_does_not_exist() throws Exception {
    // arrange
    LocalDateTime dr = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime dn = LocalDateTime.parse("2023-01-03T00:00:01");

    RecommendationRequest editedRecommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("tom@gmail.com")
            .professorEmail("mrbill@gmail.com")
            .explanation("MIT phd for cs")
            .dateRequested(dr)
            .dateNeeded(dn)
            .done(false)
            .build();

    String requestBody = mapper.writeValueAsString(editedRecommendationRequest);

    when(recommendationRequestRepository.findById(eq(67L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/recommendationrequests")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("RecommendationRequest with id 67 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_recommendation_request() throws Exception {
    // arrange

    LocalDateTime dr1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime dn1 = LocalDateTime.parse("2022-01-03T00:00:01");

    RecommendationRequest recommendationRequestOrig =
        RecommendationRequest.builder()
            .requesterEmail("tom@gmail.com")
            .professorEmail("douglas@gmail.com")
            .explanation("caltech phd for cs")
            .dateRequested(dr1)
            .dateNeeded(dn1)
            .done(true)
            .build();

    LocalDateTime dr2 = LocalDateTime.parse("2022-04-03T00:00:00");
    LocalDateTime dn2 = LocalDateTime.parse("2023-01-03T00:00:01");

    RecommendationRequest recommendationRequestEdited =
        RecommendationRequest.builder()
            .requesterEmail("thomas@gmail.com")
            .professorEmail("mrbill@gmail.com")
            .explanation("MIT phd for cs")
            .dateRequested(dr2)
            .dateNeeded(dn2)
            .done(false)
            .build();

    String requestBody = mapper.writeValueAsString(recommendationRequestEdited);

    when(recommendationRequestRepository.findById(eq(67L)))
        .thenReturn(Optional.of(recommendationRequestOrig));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/recommendationrequests")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(recommendationRequestRepository, times(1)).findById(67L);
    verify(recommendationRequestRepository, times(1))
        .save(recommendationRequestEdited); // should be saved with correct user
    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }
}
