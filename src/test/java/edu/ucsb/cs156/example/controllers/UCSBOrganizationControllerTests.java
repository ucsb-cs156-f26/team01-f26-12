package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {

  @MockitoBean UCSBOrganizationRepository ucsbOrganizationRepository;
  @MockitoBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/UCSBOrganization/all")).andExpect(status().isForbidden());
    verifyNoInteractions(ucsbOrganizationRepository);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_get_an_empty_list_when_no_organizations_exist() throws Exception {
    when(ucsbOrganizationRepository.findAll()).thenReturn(List.of());

    MvcResult response =
        mockMvc.perform(get("/api/UCSBOrganization/all")).andExpect(status().isOk()).andReturn();

    assertEquals("[]", response.getResponse().getContentAsString());
    verify(ucsbOrganizationRepository).findAll();
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_get_all_organizations_with_all_fields() throws Exception {
    UCSBOrganization sky = organization(false);
    UCSBOrganization krc =
        UCSBOrganization.builder()
            .orgCode("KRC")
            .orgTranslationShort("KOREAN RADIO CL")
            .orgTranslation("KOREAN RADIO CLUB")
            .inactive(true)
            .build();
    List<UCSBOrganization> organizations = List.of(sky, krc);
    when(ucsbOrganizationRepository.findAll()).thenReturn(organizations);

    MvcResult response =
        mockMvc.perform(get("/api/UCSBOrganization/all")).andExpect(status().isOk()).andReturn();

    assertEquals(
        mapper.writeValueAsString(organizations), response.getResponse().getContentAsString());
    verify(ucsbOrganizationRepository).findAll();
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc.perform(createRequest(false).with(csrf())).andExpect(status().isForbidden());
    verifyNoInteractions(ucsbOrganizationRepository);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_post() throws Exception {
    mockMvc.perform(createRequest(false).with(csrf())).andExpect(status().isForbidden());
    verifyNoInteractions(ucsbOrganizationRepository);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admins_can_post_an_organization() throws Exception {
    UCSBOrganization organization = organization(true);
    when(ucsbOrganizationRepository.save(eq(organization))).thenReturn(organization);

    MvcResult response =
        mockMvc.perform(createRequest(true).with(csrf())).andExpect(status().isOk()).andReturn();

    assertEquals(
        mapper.writeValueAsString(organization), response.getResponse().getContentAsString());
    verify(ucsbOrganizationRepository).save(eq(organization));
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/UCSBOrganization").param("orgCode", "SKY"))
        .andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_an_existing_organization() throws Exception {
    UCSBOrganization sky = organization(false);
    when(ucsbOrganizationRepository.findById(eq("SKY"))).thenReturn(Optional.of(sky));

    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBOrganization").param("orgCode", "SKY"))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbOrganizationRepository).findById("SKY");
    assertEquals(mapper.writeValueAsString(sky), response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void missing_organizations_return_not_found() throws Exception {
    when(ucsbOrganizationRepository.findById(eq("MISSING"))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/UCSBOrganization").param("orgCode", "MISSING"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbOrganizationRepository).findById("MISSING");
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBOrganization with id MISSING not found", json.get("message"));
  }

  private UCSBOrganization organization(boolean inactive) {
    return UCSBOrganization.builder()
        .orgCode("SKY")
        .orgTranslationShort("SKYDIVING CLUB")
        .orgTranslation("SKYDIVING CLUB AT UCSB")
        .inactive(inactive)
        .build();
  }

  private MockHttpServletRequestBuilder createRequest(boolean inactive) {
    return post("/api/UCSBOrganization/post")
        .param("orgCode", "SKY")
        .param("orgTranslationShort", "SKYDIVING CLUB")
        .param("orgTranslation", "SKYDIVING CLUB AT UCSB")
        .param("inactive", Boolean.toString(inactive));
  }
}
