package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.errors.EntityNotFoundException;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** This is a REST controller for HelpRequests. */
@Tag(name = "HelpRequests")
@RequestMapping("/api/helprequests")
@RestController
@Slf4j
public class HelpRequestController extends ApiController {

  @Autowired HelpRequestRepository helpRequestRepository;

  /**
   * List all help requests.
   *
   * @return an iterable of HelpRequest objects
   */
  @Operation(summary = "List all help requests")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/all")
  public Iterable<HelpRequest> allHelpRequests() {
    Iterable<HelpRequest> helpRequests = helpRequestRepository.findAll();
    return helpRequests;
  }

  /**
   * Get a single help request by ID.
   *
   * @param id the ID of the help request
   * @return the matching HelpRequest
   */
  @Operation(summary = "Get a single help request by ID")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("")
  public HelpRequest getHelpRequestById(
      @Parameter(name = "id", description = "ID of the help request") @RequestParam Long id) {

    HelpRequest helpRequest =
        helpRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(HelpRequest.class, id));

    return helpRequest;
  }

  /**
   * Delete a single help request.
   *
   * @param id the ID of the help request to delete
   * @return a message indicating that the help request was deleted
   */
  @Operation(summary = "Delete a single help request")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("")
  public Object deleteHelpRequest(@Parameter(name = "id") @RequestParam Long id) {
    HelpRequest helpRequest =
        helpRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(HelpRequest.class, id));

    helpRequestRepository.delete(helpRequest);

    return genericMessage("HelpRequest with id %s deleted".formatted(id));
  }

  /**
   * Update a single help request.
   *
   * @param id the ID of the help request to update
   * @param incoming the updated help request values
   * @return the updated HelpRequest
   */
  @Operation(summary = "Update a single help request")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PutMapping("")
  public HelpRequest updateHelpRequest(
      @Parameter(name = "id", description = "ID of the help request") @RequestParam Long id,
      @RequestBody @Valid HelpRequest incoming) {

    HelpRequest helpRequest =
        helpRequestRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException(HelpRequest.class, id));

    helpRequest.setRequesterEmail(incoming.getRequesterEmail());
    helpRequest.setTeamId(incoming.getTeamId());
    helpRequest.setTableOrBreakoutRoom(incoming.getTableOrBreakoutRoom());
    helpRequest.setRequestTime(incoming.getRequestTime());
    helpRequest.setExplanation(incoming.getExplanation());
    helpRequest.setSolved(incoming.getSolved());

    helpRequestRepository.save(helpRequest);

    return helpRequest;
  }

  /**
   * Create a new help request.
   *
   * @param requesterEmail email address of the person requesting help
   * @param teamId team identifier
   * @param tableOrBreakoutRoom table or breakout room number
   * @param requestTime time when help was requested
   * @param explanation explanation of the help needed
   * @param solved whether the request has been solved
   * @return the saved HelpRequest
   */
  @Operation(summary = "Create a new help request")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public HelpRequest postHelpRequest(
      @Parameter(name = "requesterEmail") @RequestParam String requesterEmail,
      @Parameter(name = "teamId") @RequestParam String teamId,
      @Parameter(name = "tableOrBreakoutRoom") @RequestParam String tableOrBreakoutRoom,
      @Parameter(name = "requestTime", description = "Time of the request in ISO date-time format")
          @RequestParam("requestTime")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime requestTime,
      @Parameter(name = "explanation") @RequestParam String explanation,
      @Parameter(name = "solved") @RequestParam boolean solved) {

    HelpRequest helpRequest = new HelpRequest();

    helpRequest.setRequesterEmail(requesterEmail);
    helpRequest.setTeamId(teamId);
    helpRequest.setTableOrBreakoutRoom(tableOrBreakoutRoom);
    helpRequest.setRequestTime(requestTime);
    helpRequest.setExplanation(explanation);
    helpRequest.setSolved(solved);

    HelpRequest savedHelpRequest = helpRequestRepository.save(helpRequest);

    return savedHelpRequest;
  }
}
