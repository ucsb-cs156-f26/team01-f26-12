package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for listing and creating UCSB organizations. */
@Tag(name = "UCSBOrganization")
@RequestMapping("/api/UCSBOrganization")
@RestController
@Slf4j
public class UCSBOrganizationController extends ApiController {

  @Autowired UCSBOrganizationRepository ucsbOrganizationRepository;

  /**
   * Returns all organizations for a logged-in user.
   *
   * @return all organizations in the database
   */
  @Operation(summary = "List all UCSB organizations")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/all")
  public Iterable<UCSBOrganization> allOrganizations() {
    return ucsbOrganizationRepository.findAll();
  }

  /**
   * Creates an organization. Only administrators may create organizations.
   *
   * @param orgCode the organization's unique code
   * @param orgTranslationShort the organization's abbreviated name
   * @param orgTranslation the organization's full name
   * @param inactive whether the organization is inactive
   * @return the saved organization
   */
  @Operation(summary = "Create a UCSB organization (admin only)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public UCSBOrganization postOrganization(
      @Parameter(description = "Unique organization code", example = "SKY") @RequestParam
          String orgCode,
      @Parameter(description = "Abbreviated organization name", example = "SKYDIVING CLUB")
          @RequestParam
          String orgTranslationShort,
      @Parameter(description = "Full organization name", example = "SKYDIVING CLUB AT UCSB")
          @RequestParam
          String orgTranslation,
      @Parameter(description = "Whether the organization is inactive", example = "false")
          @RequestParam
          boolean inactive) {
    UCSBOrganization organization = new UCSBOrganization();
    organization.setOrgCode(orgCode);
    organization.setOrgTranslationShort(orgTranslationShort);
    organization.setOrgTranslation(orgTranslation);
    organization.setInactive(inactive);
    return ucsbOrganizationRepository.save(organization);
  }
}
