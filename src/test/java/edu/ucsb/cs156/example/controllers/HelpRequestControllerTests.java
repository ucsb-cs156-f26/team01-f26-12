package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

  // Authorization tests for PUT /api/helprequests?id=...

  @Test
  public void logged_out_users_cannot_update() throws Exception {
    mockMvc
        .perform(
            put("/api/helprequests")
                .param("id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_update() throws Exception {
    mockMvc
        .perform(
            put("/api/helprequests")
                .param("id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_update_an_existing_help_request() throws Exception {
    LocalDateTime originalRequestTime = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime updatedRequestTime = LocalDateTime.parse("2023-01-03T00:00:00");

    HelpRequest originalHelpRequest =
        HelpRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .teamId("s22-5pm-3")
            .tableOrBreakoutRoom("7")
            .requestTime(originalRequestTime)
            .explanation("Need help with Swagger-ui")
            .solved(false)
            .build();

    HelpRequest updatedHelpRequest =
        HelpRequest.builder()
            .requesterEmail("updated@ucsb.edu")
            .teamId("f23-10am-2")
            .tableOrBreakoutRoom("12")
            .requestTime(updatedRequestTime)
            .explanation("Updated explanation")
            .solved(true)
            .build();

    String requestBody = mapper.writeValueAsString(updatedHelpRequest);

    when(helpRequestRepository.findById(7L)).thenReturn(Optional.of(originalHelpRequest));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/helprequests")
                    .param("id", "7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(helpRequestRepository, times(1)).findById(7L);

    ArgumentCaptor<HelpRequest> captor = ArgumentCaptor.forClass(HelpRequest.class);
    verify(helpRequestRepository, times(1)).save(captor.capture());

    HelpRequest savedHelpRequest = captor.getValue();

    assertEquals("updated@ucsb.edu", savedHelpRequest.getRequesterEmail());
    assertEquals("f23-10am-2", savedHelpRequest.getTeamId());
    assertEquals("12", savedHelpRequest.getTableOrBreakoutRoom());
    assertEquals(updatedRequestTime, savedHelpRequest.getRequestTime());
    assertEquals("Updated explanation", savedHelpRequest.getExplanation());
    assertEquals(true, savedHelpRequest.getSolved());

    String responseString = response.getResponse().getContentAsString();
    Map<String, Object> responseJson = mapper.readValue(responseString, Map.class);

    assertEquals("updated@ucsb.edu", responseJson.get("requesterEmail"));
    assertEquals("f23-10am-2", responseJson.get("teamId"));
    assertEquals("12", responseJson.get("tableOrBreakoutRoom"));
    assertEquals("2023-01-03T00:00:00", responseJson.get("requestTime"));
    assertEquals("Updated explanation", responseJson.get("explanation"));
    assertEquals(true, responseJson.get("solved"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_update_help_request_that_does_not_exist() throws Exception {
    HelpRequest updatedHelpRequest =
        HelpRequest.builder()
            .requesterEmail("updated@ucsb.edu")
            .teamId("f23-10am-2")
            .tableOrBreakoutRoom("12")
            .requestTime(LocalDateTime.parse("2023-01-03T00:00:00"))
            .explanation("Updated explanation")
            .solved(true)
            .build();

    String requestBody = mapper.writeValueAsString(updatedHelpRequest);

    when(helpRequestRepository.findById(7L)).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/helprequests")
                    .param("id", "7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
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

    MvcResult response =
        mockMvc.perform(get("/api/helprequests/all")).andExpect(status().isOk()).andReturn();

    verify(helpRequestRepository, times(1)).findAll();

    String expectedJson = mapper.writeValueAsString(expectedHelpRequests);
    String responseString = response.getResponse().getContentAsString();

    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_helprequest() throws Exception {
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

    verify(helpRequestRepository, times(1)).save(helpRequest);

    String expectedJson = mapper.writeValueAsString(helpRequest);
    String responseString = response.getResponse().getContentAsString();

    assertEquals(expectedJson, responseString);
  }
}
