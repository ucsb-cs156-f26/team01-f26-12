package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = HelpRequestController.class)
@Import(TestConfig.class)
public class HelpRequestControllerTests extends ControllerTestCase {

  @MockitoBean HelpRequestRepository helpRequestRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for GET /api/helprequests/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequests/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/helprequests/all")).andExpect(status().is(200));
  }

  // Authorization tests for POST /api/helprequests/post

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/helprequests/post")
                .param("requesterEmail", "cgaucho@ucsb.edu")
                .param("teamId", "s22-5pm-3")
                .param("tableOrBreakoutRoom", "7")
                .param("requestTime", "2022-01-03T00:00:00")
                .param("explanation", "Need help with Swagger-ui")
                .param("solved", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/helprequests/post")
                .param("requesterEmail", "cgaucho@ucsb.edu")
                .param("teamId", "s22-5pm-3")
                .param("tableOrBreakoutRoom", "7")
                .param("requestTime", "2022-01-03T00:00:00")
                .param("explanation", "Need help with Swagger-ui")
                .param("solved", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  // Authorization tests for GET /api/helprequests?id=...

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc.perform(get("/api/helprequests").param("id", "7")).andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_help_request_by_id_when_it_exists() throws Exception {
    HelpRequest helpRequest =
        HelpRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .teamId("s22-5pm-3")
            .tableOrBreakoutRoom("7")
            .requestTime(LocalDateTime.parse("2022-01-03T00:00:00"))
            .explanation("Need help with Swagger-ui")
            .solved(false)
            .build();

    when(helpRequestRepository.findById(7L)).thenReturn(Optional.of(helpRequest));

    MvcResult response =
        mockMvc
            .perform(get("/api/helprequests").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(7L);

    String expectedJson = mapper.writeValueAsString(helpRequest);
    String responseString = response.getResponse().getContentAsString();

    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_gets_404_when_help_request_does_not_exist() throws Exception {
    when(helpRequestRepository.findById(7L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/helprequests").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(7L);

    Map<String, Object> json = responseToJson(response);

    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("HelpRequest with id 7 not found", json.get("message"));
  }

  // Tests with mocks for database actions

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_helprequests() throws Exception {
    // Arrange
    LocalDateTime requestTime1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime requestTime2 = LocalDateTime.parse("2022-03-11T00:00:00");

    HelpRequest helpRequest1 =
        HelpRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .teamId("s22-5pm-3")
            .tableOrBreakoutRoom("7")
            .requestTime(requestTime1)
            .explanation("Need help with Swagger-ui")
            .solved(false)
            .build();

    HelpRequest helpRequest2 =
        HelpRequest.builder()
            .requesterEmail("ldelplaya@ucsb.edu")
            .teamId("s22-6pm-3")
            .tableOrBreakoutRoom("11")
            .requestTime(requestTime2)
            .explanation("Dokku problems")
            .solved(false)
            .build();

    ArrayList<HelpRequest> expectedHelpRequests = new ArrayList<>();
    expectedHelpRequests.addAll(Arrays.asList(helpRequest1, helpRequest2));

    when(helpRequestRepository.findAll()).thenReturn(expectedHelpRequests);

    // Act
    MvcResult response =
        mockMvc.perform(get("/api/helprequests/all")).andExpect(status().isOk()).andReturn();

    // Assert
    verify(helpRequestRepository, times(1)).findAll();

    String expectedJson = mapper.writeValueAsString(expectedHelpRequests);
    String responseString = response.getResponse().getContentAsString();

    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_helprequest() throws Exception {
    // Arrange
    LocalDateTime requestTime = LocalDateTime.parse("2022-01-03T00:00:00");

    HelpRequest helpRequest =
        HelpRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .teamId("s22-5pm-3")
            .tableOrBreakoutRoom("7")
            .requestTime(requestTime)
            .explanation("Need help with Swagger-ui")
            .solved(true)
            .build();

    when(helpRequestRepository.save(eq(helpRequest))).thenReturn(helpRequest);

    // Act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/helprequests/post")
                    .param("requesterEmail", "cgaucho@ucsb.edu")
                    .param("teamId", "s22-5pm-3")
                    .param("tableOrBreakoutRoom", "7")
                    .param("requestTime", "2022-01-03T00:00:00")
                    .param("explanation", "Need help with Swagger-ui")
                    .param("solved", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // Assert
    verify(helpRequestRepository, times(1)).save(helpRequest);

    String expectedJson = mapper.writeValueAsString(helpRequest);
    String responseString = response.getResponse().getContentAsString();

    assertEquals(expectedJson, responseString);
  }
}
