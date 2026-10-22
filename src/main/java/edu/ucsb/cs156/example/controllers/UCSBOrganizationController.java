package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.errors.EntityNotFoundException;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for UCSB organizations. */
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
   * Returns a single organization by its organization code.
   *
   * @param orgCode the organization's unique code
   * @return the matching organization
   */
  @Operation(summary = "Get a single UCSB organization by code")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("")
  public UCSBOrganization getById(
      @Parameter(description = "Organization code to look up", example = "SKY") @RequestParam
          String orgCode) {
    UCSBOrganization organization =
        ucsbOrganizationRepository
            .findById(orgCode)
            .orElseThrow(() -> new EntityNotFoundException(UCSBOrganization.class, orgCode));
    return organization;
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

  /**
   * Updates an organization's fields while retaining its organization code.
   *
   * @param orgCode the code of the organization to update
   * @param incoming the new organization field values
   * @return the updated organization
   */
  @Operation(summary = "Update a UCSB organization (admin only)")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PutMapping("")
  public UCSBOrganization updateOrganization(
      @Parameter(description = "Organization code to update", example = "SKY") @RequestParam
          String orgCode,
      @RequestBody @Valid UCSBOrganization incoming) {
    UCSBOrganization organization =
        ucsbOrganizationRepository
            .findById(orgCode)
            .orElseThrow(() -> new EntityNotFoundException(UCSBOrganization.class, orgCode));

    organization.setOrgTranslationShort(incoming.getOrgTranslationShort());
    organization.setOrgTranslation(incoming.getOrgTranslation());
    organization.setInactive(incoming.getInactive());
    ucsbOrganizationRepository.save(organization);
    return organization;
  }
}
