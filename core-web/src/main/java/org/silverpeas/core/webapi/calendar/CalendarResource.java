/*
 * Copyright (C) 2000 - 2016 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.core.webapi.calendar;

import org.apache.commons.lang3.tuple.Pair;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.annotation.RequestScoped;
import org.silverpeas.core.annotation.Service;
import org.silverpeas.core.cache.model.SimpleCache;
import org.silverpeas.core.cache.service.CacheServiceProvider;
import org.silverpeas.core.calendar.Attendee;
import org.silverpeas.core.calendar.Calendar;
import org.silverpeas.core.calendar.CalendarEvent;
import org.silverpeas.core.calendar.CalendarEventOccurrence;
import org.silverpeas.core.importexport.ExportDescriptor;
import org.silverpeas.core.importexport.ExportException;
import org.silverpeas.core.importexport.ImportException;
import org.silverpeas.core.io.upload.FileUploadManager;
import org.silverpeas.core.io.upload.UploadedFile;
import org.silverpeas.core.util.StringUtil;
import org.silverpeas.core.util.logging.SilverLogger;
import org.silverpeas.core.web.http.RequestParameterDecoder;
import org.silverpeas.core.web.mvc.webcomponent.WebMessager;
import org.silverpeas.core.webapi.base.annotation.Authorized;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.silverpeas.core.calendar.icalendar.ICalendarExporter.CALENDAR;
import static org.silverpeas.core.webapi.calendar.CalendarEventOccurrenceEntity.decodeId;
import static org.silverpeas.core.webapi.calendar.CalendarResourceURIs.*;
import static org.silverpeas.core.webapi.calendar.CalendarWebServiceProvider.assertDataConsistency;
import static org.silverpeas.core.webapi.calendar.CalendarWebServiceProvider.assertEntityIsDefined;

/**
 * A REST Web resource giving calendar data.
 * @author Yohann Chastagnier
 */
@Service
@RequestScoped
@Path(CALENDAR_BASE_URI + "/{componentInstanceId}")
@Authorized
public class CalendarResource extends AbstractCalendarResource {

  private static final int DEFAULT_NB_MAX_NEXT_OCC = 10;
  private static final int[] NEXT_OOC_MONTH_OFFSETS = {1, 6, 12, 1200};

  @QueryParam("zoneid")
  private String zoneId;

  /**
   * Gets the zoneId into which dates must be set.
   * @return a {@link ZoneId} instance if zoneid parameter has been set, null otherwise.
   */
  public ZoneId getZoneId() {
    return StringUtil.isDefined(zoneId) ? ZoneId.of(zoneId) : null;
  }

  /**
   * Gets the JSON representation of a list of calendar.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * calendars.
   * @see WebProcess#execute()
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<CalendarEntity> getCalendars() {
    List<Calendar> calendars =
        process(() -> getCalendarWebServiceProvider().getCalendarsOf(getComponentId())).execute();
    return asWebEntities(calendars);
  }

  /**
   * Gets the JSON representation of a calendar represented by the given identifier.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @param calendarId the identifier of the aimed calendar
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * calendar.
   * @see WebProcess#execute()
   */
  @GET
  @Path("{calendarId}")
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEntity getCalendar(@PathParam("calendarId") String calendarId) {
    final Calendar calendar = process(() -> Calendar.getById(calendarId)).execute();
    assertDataConsistency(getComponentId(), calendar);
    return asWebEntity(calendar);
  }

  /**
   * Creates the calendar from its JSON representation and returns it once created.<br/>
   * If the user isn't authenticated, a 401 HTTP code is returned. If the user isn't authorized to
   * save the calendar, a 403 is returned. If a problem occurs when processing the request, a 503
   * HTTP code is returned.
   * @param calendarEntity the calendar data
   * @return the response to the HTTP POST request with the JSON representation of the created
   * calendar.
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEntity createCalendar(CalendarEntity calendarEntity) {
    final Calendar calendar = new Calendar(getComponentId(), calendarEntity.getTitle());
    calendarEntity.merge(calendar);
    Calendar createdCalendar =
        process(() -> getCalendarWebServiceProvider().saveCalendar(calendar)).execute();
    if (createdCalendar.isSynchronized()) {
      synchronizeCalendar(createdCalendar.getId());
    }
    return asWebEntity(createdCalendar);
  }

  /**
   * Updates the calendar from its JSON representation and returns it once updated. If the calendar
   * to update doesn't match with the requested one, a 400 HTTP code is returned. If the calendar
   * doesn't exist, a 404 HTTP code is returned. If the user isn't authenticated, a 401 HTTP code is
   * returned. If the user isn't authorized to save the calendar, a 403 is returned. If a problem
   * occurs when processing the request, a 503 HTTP code is returned.
   * @param calendarId the identifier of the updated calendar
   * @param calendarEntity the data of the calendar
   * @return the response to the HTTP PUT request with the JSON representation of the updated
   * calendar.
   */
  @PUT
  @Path("{calendarId}")
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEntity updateCalendar(@PathParam("calendarId") String calendarId,
      CalendarEntity calendarEntity) {
    final Calendar calendar = Calendar.getById(calendarId);
    assertDataConsistency(getComponentId(), calendar);
    boolean synchronizedRequired = calendar.isSynchronized() &&
        !calendar.getExternalCalendarUrl().toString()
            .equals(calendarEntity.getExternalUrl().toString());
    calendarEntity.merge(calendar);
    Calendar updatedCalendar =
        process(() -> getCalendarWebServiceProvider().saveCalendar(calendar)).execute();
    if (synchronizedRequired) {
      synchronizeCalendar(updatedCalendar.getId());
    }
    return asWebEntity(updatedCalendar);
  }

  /**
   * Deletes the calendar from its JSON identifier.
   * If the calendar doesn't exist, a 404 HTTP code is returned. If the user isn't authenticated, a
   * 401 HTTP code is returned. If the user isn't authorized to save the calendar, a 403 is
   * returned. If a problem occurs when processing the request, a 503 HTTP code is returned.
   * @param calendarId the identifier of the deleted calendar
   */
  @DELETE
  @Path("{calendarId}")
  @Produces(MediaType.APPLICATION_JSON)
  public void deleteCalendar(@PathParam("calendarId") String calendarId) {
    final Calendar calendar = Calendar.getById(calendarId);
    assertDataConsistency(getComponentId(), calendar);
    process(() -> {
      getCalendarWebServiceProvider().deleteCalendar(calendar);
      return null;
    }).execute();
  }

  /**
   * Gets the JSON representation of a calendar represented by the given identifier.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @param calendarId the identifier of the aimed calendar
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * calendar.
   * @see WebProcess#execute()
   */
  @GET
  @Path("{calendarId}/export/ical")
  @Produces("text/calendar")
  public Response exportCalendarAsICalendarFormat(@PathParam("calendarId") String calendarId) {
    final Calendar calendar = process(() -> Calendar.getById(calendarId)).execute();
    assertDataConsistency(getComponentId(), calendar);
    return Response.ok((StreamingOutput) output -> {
      try {
        final ExportDescriptor descriptor = ExportDescriptor
            .withOutputStream(output)
            .withParameter(CALENDAR, calendar);
        getCalendarWebServiceProvider().exportCalendarAsICalendarFormat(calendar, descriptor);
      } catch (ExportException e) {
        SilverLogger.getLogger(this).error(e);
        throw new WebApplicationException(INTERNAL_SERVER_ERROR);
      }
    })
    .header("Content-Disposition",
        String.format("inline;filename=\"%s\"", calendar.getTitle() + ".ics"))
    .build();
  }

  /**
   * Permits to import one iCalendar file from http request.
   * The file upload is performed by FileUploadResource mechanism.<br/>
   * This service is awaiting the upload parameters handled by silverpeas-fileUpload.js plugin.<br/>
   * (see {@link FileUploadManager}) in order to get more information.
   * If the user isn't authenticated, a 401 HTTP code is returned.
   * If a problem occurs when processing the request, a 503 HTTP code is returned.
   */
  @POST
  @Path("{calendarId}/import/ical")
  @Produces(MediaType.APPLICATION_JSON)
  public Response importEventsAsICalendarFormat(@PathParam("calendarId") String calendarId) {
    final Calendar calendar = process(() -> Calendar.getById(calendarId)).execute();
    assertDataConsistency(getComponentId(), calendar);
    try {
      FileUploadManager.getUploadedFiles(getHttpRequest(), getUser())
          .forEach(uploadedFile -> performImportEventAsICalendarFormat(calendar, uploadedFile));
    } catch (WebApplicationException e) {
      SilverLogger.getLogger(this).error(e);
      Response.ResponseBuilder response = Response.fromResponse(e.getResponse());
      if (e.getCause() != null && StringUtil.isDefined(e.getCause().getMessage())) {
        response.entity(e.getCause().getMessage());
      } else {
        response.entity(e.getMessage());
      }
      return response.build();
    }
    return Response.ok().build();
  }

  private void performImportEventAsICalendarFormat(final Calendar calendar,
      final UploadedFile uploadedFile) {
    try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream
        (uploadedFile.getFile()))) {
      getCalendarWebServiceProvider()
          .importEventsAsICalendarFormat(calendar, bis);
    } catch (IOException | ImportException e) {
      throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
    } finally {
      uploadedFile.getUploadSession().clear();
    }
  }

  /**
   * Permits to synchronize manually a calendar for which an external url is set.
   * If the user isn't authenticated, a 401 HTTP code is returned.
   * If a problem occurs when processing the request, a 503 HTTP code is returned.
   */
  @PUT
  @Path("{calendarId}/synchronization")
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("UnusedReturnValue")
  public Response synchronizeCalendar(@PathParam("calendarId") String calendarId) {
    final Calendar calendar = process(() -> Calendar.getById(calendarId)).execute();
    assertDataConsistency(getComponentId(), calendar);
    try {
      getCalendarWebServiceProvider().synchronizeCalendar(calendar);
    } catch (ImportException e) {
      SilverLogger.getLogger(this).error(e);
      getMessager().addError(getBundle()
          .getStringWithParams("calendar.message.calendar.synchronize.error", calendar.getTitle()));
      return Response.serverError().entity(asWebEntity(calendar)).build();
    }
    return Response.ok(asWebEntity(calendar)).build();
  }

  /**
   * Gets the JSON representation of a list of calendar event occurrence.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * occurrences.
   * @see WebProcess#execute()
   */
  @GET
  @Path(CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART + "/next")
  @Produces(MediaType.APPLICATION_JSON)
  public List<CalendarEventOccurrenceEntity> getNextEventOccurrences(
      @QueryParam("limit") Integer limit) {
    final int nbOccLimit = (limit != null && limit > 0 && limit < 100) ? limit :
        DEFAULT_NB_MAX_NEXT_OCC;
    final List<Calendar> calendars =
        process(() -> getCalendarWebServiceProvider().getCalendarsOf(getComponentId())).execute();
    final LocalDate startDate =
        getZoneId() != null ? LocalDateTime.now(getZoneId()).toLocalDate() : LocalDate.now();
    final List<CalendarEventOccurrenceEntity> occurrences = new ArrayList<>();
    for (int nbMonthsToAdd : NEXT_OOC_MONTH_OFFSETS) {
      occurrences.clear();
      LocalDate endDate = startDate.plusMonths(nbMonthsToAdd);
      calendars.forEach(c -> occurrences.addAll(getEventOccurrencesOf(c, startDate, endDate)));
      if (occurrences.size() >= nbOccLimit) {
        break;
      }
    }
    return occurrences.stream().sorted(Comparator.comparing(CalendarEventEntity::getStartDate))
        .limit(nbOccLimit).collect(Collectors.toList());
  }

  /**
   * Gets the JSON representation of a list of calendar event occurrence.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * occurrences.
   * @see WebProcess#execute()
   */
  @GET
  @Path(CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ParticipantCalendarEventOccurrencesEntity> getAllEventOccurrencesFrom() {
    CalendarEventOccurrenceRequestParameters params = RequestParameterDecoder
        .decode(getHttpRequest(), CalendarEventOccurrenceRequestParameters.class);
    final LocalDate startDate = params.getStartDateOfWindowTime().toLocalDate();
    final LocalDate endDate = params.getEndDateOfWindowTime().toLocalDate();
    final Set<User> users = params.getUsers();
    return getAllEventOccurrencesFrom(startDate, endDate, users);
  }

  /**
   * Can be extended.
   * @see #getAllEventOccurrencesFrom()
   */
  protected List<ParticipantCalendarEventOccurrencesEntity> getAllEventOccurrencesFrom(
      final LocalDate startDate, final LocalDate endDate, final Set<User> users) {
    Map<String, List<CalendarEventOccurrence>> occurrences = process(
        () -> getCalendarWebServiceProvider()
            .getAllEventOccurrencesByUserIds(Pair.of(getComponentId(), getUser()), startDate,
                endDate, users)).execute();
    List<ParticipantCalendarEventOccurrencesEntity> webEntities = new ArrayList<>();
    users.forEach(user -> webEntities.add(ParticipantCalendarEventOccurrencesEntity.from(user)
        .withOccurrences(asOccurrenceWebEntities(
            Optional.ofNullable(occurrences.get(user.getId())).orElse(Collections.emptyList())))));
    return webEntities;
  }

  /**
   * Gets the JSON representation of a list of calendar event occurrence.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @param calendarId the identifier of calendar the occurrences must belong with
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * occurrences.
   * @see WebProcess#execute()
   */
  @GET
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART)
  @Produces(MediaType.APPLICATION_JSON)
  public List<CalendarEventOccurrenceEntity> getEventOccurrencesOf(
      @PathParam("calendarId") String calendarId) {
    Calendar calendar = Calendar.getById(calendarId);
    assertDataConsistency(getComponentId(), calendar);
    CalendarEventOccurrenceRequestParameters params = RequestParameterDecoder
        .decode(getHttpRequest(), CalendarEventOccurrenceRequestParameters.class);
    final LocalDate startDate = params.getStartDateOfWindowTime().toLocalDate();
    final LocalDate endDate = params.getEndDateOfWindowTime().toLocalDate();
    return getEventOccurrencesOf(calendar, startDate, endDate);
  }

  /**
   * Can be extended.
   * @see #getEventOccurrencesOf(String) ()
   */
  protected List<CalendarEventOccurrenceEntity> getEventOccurrencesOf(final Calendar calendar,
      final LocalDate startDate, final LocalDate endDate) {
    List<CalendarEventOccurrence> occurrences = process(
        () -> getCalendarWebServiceProvider().getEventOccurrencesOf(calendar, startDate, endDate))
        .execute();
    return asOccurrenceWebEntities(occurrences);
  }

  /**
   * Gets the JSON representation of a list of calendar event occurrence of an aimed event.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @param calendarId the identifier of calendar the event must belong with
   * @param eventId the identifier of event the returned occurrences must be linked with
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * occurrences.
   * @see WebProcess#execute()
   */
  @GET
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/{eventId}/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART)
  @Produces(MediaType.APPLICATION_JSON)
  public List<CalendarEventOccurrenceEntity> getEventOccurrencesOf(
      @PathParam("calendarId") String calendarId, @PathParam("eventId") String eventId) {
    final Calendar calendar = Calendar.getById(calendarId);
    final CalendarEvent event = CalendarEvent.getById(eventId);
    assertDataConsistency(calendar.getComponentInstanceId(), calendar, event);
    CalendarEventOccurrenceRequestParameters params = RequestParameterDecoder
        .decode(getHttpRequest(), CalendarEventOccurrenceRequestParameters.class);
    final LocalDate occStartDate = params.getStartDateOfWindowTime().toLocalDate();
    final LocalDate occEndDate = params.getEndDateOfWindowTime().toLocalDate();
    List<CalendarEventOccurrenceEntity> occurrences =
        getEventOccurrencesOf(calendar, occStartDate, occEndDate);
    occurrences.removeIf(occurrence -> !occurrence.getEventId().equals(eventId));
    return occurrences;
  }

  /**
   * Gets the JSON representation of a calendar eventof an aimed event.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @param calendarId the identifier of calendar the event must belong with.
   * @param eventId the identifier of the aimed event.
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * event.
   * @see WebProcess#execute()
   */
  @GET
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEventEntity getEvent(
      @PathParam("calendarId") String calendarId, @PathParam("eventId") String eventId) {
    final Calendar calendar = Calendar.getById(calendarId);
    final CalendarEvent event = process(() -> CalendarEvent.getById(eventId)).execute();
    assertDataConsistency(calendar.getComponentInstanceId(), calendar, event);
    return asEventWebEntity(event);
  }

  /**
   * Creates a calendar event from the JSON representation of an occurrence and returns the
   * created event.<br/> If the user isn't authenticated, a 401 HTTP code is returned. If the user
   * isn't authorized to save the calendar, a 403 is returned. If a problem occurs when
   * processing the request, a 503 HTTP code is returned.
   * @param calendarId the identifier of calendar the event must belong with
   * @param eventEntity the calendar event data
   * @return the response to the HTTP POST request with the JSON representation of the created
   * calendar event.
   */
  @POST
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART)
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEventEntity createEvent(@PathParam("calendarId") String calendarId,
      CalendarEventEntity eventEntity) {
    final Calendar calendar = Calendar.getById(calendarId);
    assertDataConsistency(getComponentId(), calendar);
    CalendarEvent createdEvent = process(() -> getCalendarWebServiceProvider()
        .createEvent(calendar, eventEntity.getMergedEvent()))
        .execute();
    return asEventWebEntity(createdEvent);
  }

  /**
   * Gets the JSON representation of a calendar event occurrence of an aimed event.
   * If it doesn't exist, a 404 HTTP code is returned.
   * @param calendarId the identifier of calendar the event must belong with.
   * @param eventId the identifier of event the returned occurrence must be linked with.
   * @param occurrenceId the identifier of the aimed occurrence.
   * @return the response to the HTTP GET request with the JSON representation of the asked
   * occurrence.
   * @see WebProcess#execute()
   */
  @GET
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/{eventId}/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART + "/{occurrenceId}")
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEventOccurrenceEntity getEventOccurrence(
      @PathParam("calendarId") String calendarId, @PathParam("eventId") String eventId,
      @PathParam("occurrenceId") String occurrenceId) {
    final Calendar calendar = Calendar.getById(calendarId);
    final CalendarEvent event = CalendarEvent.getById(eventId);
    final CalendarEventOccurrence occurrence =
        CalendarEventOccurrence.getById(decodeId(occurrenceId)).orElse(null);
    assertDataConsistency(calendar.getComponentInstanceId(), calendar, event, occurrence);
    return asOccurrenceWebEntity(occurrence);
  }

  /**
   * Updates a occurrence from its JSON representation and returns the list of
   * updated and created events.<br/> If the user isn't authenticated, a 401 HTTP code is
   * returned. If the user isn't authorized to save the calendar, a 403 is returned. If a problem
   * occurs when processing the request, a 503 HTTP code is returned.
   * @param calendarId the identifier of calendar the event must belong with.
   * @param eventId the identifier of updated event.
   * @param occurrenceId the identifier of the aimed occurrence.
   * @param occurrenceEntity the calendar event data given threw an occurrence structure
   * @return the response to the HTTP POST request with the JSON representation of the
   * updated/created events.
   */
  @PUT
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/{eventId}/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART + "/{occurrenceId}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<CalendarEventEntity> updateEventOccurrence(@PathParam("calendarId") String calendarId,
      @PathParam("eventId") String eventId, @PathParam("occurrenceId") String occurrenceId,
      CalendarEventOccurrenceUpdateEntity occurrenceEntity) {
    final Calendar originalCalendar = Calendar.getById(calendarId);
    final CalendarEvent previousEventData = CalendarEvent.getById(eventId);
    final CalendarEventOccurrence occToUpdate = occurrenceEntity.getMergedOccurrence();
    assertDataConsistency(getComponentId(), originalCalendar, previousEventData, occToUpdate);
    if (!occToUpdate.getId().equals(decodeId(occurrenceId))) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    List<CalendarEvent> updatedEvents = process(() -> getCalendarWebServiceProvider()
        .saveOccurrence(occToUpdate, occurrenceEntity.getUpdateMethodType(), getZoneId()))
        .execute();
    return asEventWebEntities(updatedEvents);
  }

  /**
   * Deletes an event from the JSON representation of an occurrence and returns an updated event if
   * any.<br/> If the user isn't authenticated, a 401 HTTP code is returned. If the user isn't
   * authorized to save the calendar, a 403 is returned. If a problem occurs when processing the
   * request, a 503 HTTP code is returned.
   * @param calendarId the identifier of calendar the event must belong with.
   * @param eventId the identifier of deleted event.
   * @param occurrenceId the identifier of the aimed occurrence.
   * @param occurrenceEntity the calendar event data given threw an occurrence structure
   * @return the response to the HTTP POST request with the JSON representation of an updated
   * event if any.
   */
  @DELETE
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/{eventId}/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART + "/{occurrenceId}")
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEventEntity deleteEventOccurrence(@PathParam("calendarId") String calendarId,
      @PathParam("eventId") String eventId, @PathParam("occurrenceId") String occurrenceId,
      CalendarEventOccurrenceDeleteEntity occurrenceEntity) {
    final Calendar originalCalendar = Calendar.getById(calendarId);
    final CalendarEvent previousEventData = CalendarEvent.getById(eventId);
    final CalendarEventOccurrence occToDelete = occurrenceEntity.getMergedOccurrence();
    assertDataConsistency(getComponentId(), originalCalendar, previousEventData, occToDelete);
    if (!occToDelete.getId().equals(decodeId(occurrenceId))) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    CalendarEvent updatedEvent = process(() -> getCalendarWebServiceProvider()
        .deleteOccurrence(occToDelete, occurrenceEntity.getDeleteMethodType(), getZoneId()))
        .execute();
    return updatedEvent != null ? asEventWebEntity(updatedEvent) : null;
  }

  /**
   * Updates the participation status of an attendee about an event.<br/> If the user isn't
   * authenticated, a 401 HTTP code is returned. If the user isn't authorized to save the calendar,
   * a 403 is returned. If a problem occurs when processing the request, a 503 HTTP code is
   * returned.
   * @param calendarId the identifier of calendar the event must belong with
   * @param eventId the identifier of event
   * @param attendeeId the identifier of the attendee belonging the event
   * @param answerEntity the new participation status with all needed data to save it.
   * @return the response to the HTTP POST request with the JSON representation of the
   * updated/created events.
   */
  @PUT
  @Path("{calendarId}/" + CalendarResourceURIs.CALENDAR_EVENT_URI_PART + "/{eventId}/" +
      CalendarResourceURIs.CALENDAR_EVENT_OCCURRENCE_URI_PART + "/{occurrenceId}/" +
      CalendarResourceURIs.CALENDAR_EVENT_ATTENDEE_URI_PART + "/{attendeeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public CalendarEventEntity updateEventAttendeeParticipation(
      @PathParam("calendarId") String calendarId, @PathParam("eventId") String eventId,
      @PathParam("occurrenceId") String occurrenceId, @PathParam("attendeeId") String attendeeId,
      CalendarEventAttendeeAnswerEntity answerEntity) {
    if(StringUtil.isLong(attendeeId) && !getUser().getId().equals(attendeeId)) {
      throw new WebApplicationException(FORBIDDEN);
    }
    final Calendar originalCalendar = Calendar.getById(calendarId);
    final CalendarEvent event = CalendarEvent.getById(eventId);
    final CalendarEventOccurrence occurrence =
        CalendarEventOccurrence.getById(decodeId(occurrenceId)).orElse(null);
    assertDataConsistency(originalCalendar.getComponentInstanceId(), originalCalendar, event,
        occurrence);
    answerEntity.setId(attendeeId);
    CalendarEvent updatedEvent = process(() -> getCalendarWebServiceProvider()
        .updateOccurrenceAttendeeParticipation(occurrence, answerEntity.getId(),
            answerEntity.getParticipationStatus(), answerEntity.getAnswerMethodType(), getZoneId()))
        .lowestAccessRole(null)
        .execute();
    return updatedEvent != null ? asEventWebEntity(updatedEvent) : null;
  }

  /**
   * Converts the list of calendar into list of calendar web entities.
   * @param calendars the calendars to convert.
   * @return the calendar web entities.
   */
  public List<CalendarEntity> asWebEntities(Collection<Calendar> calendars) {
    return calendars.stream().map(this::asWebEntity).collect(Collectors.toList());
  }

  /**
   * Converts the calendar into its corresponding web entity. If the specified calendar isn't
   * defined, then an HTTP 404 error is sent back instead of the entity representation of the
   * calendar.
   * @param calendar the calendar to convert.
   * @return the corresponding calendar entity.
   */
  public CalendarEntity asWebEntity(Calendar calendar) {
    assertEntityIsDefined(calendar);
    final CalendarEntity calendarEntity = CalendarEntity.fromCalendar(calendar)
        .withURI(calendarUri(getUri(), calendar));
    if (calendarEntity.isUserPersonal() || calendarEntity.canBeModified()) {
      calendarEntity
          .withICalPublicURI(iCalPublicURI(getUri(), calendar))
          .withICalPrivateURI(iCalPrivateURI(getUri(), calendar));
    } else {
      calendarEntity.setExternalUrl(null);
    }
    return calendarEntity;
  }

  /**
   * Converts the list of calendar event into list of calendar event web entities.
   * @param events the calendar events to convert.
   * @return the calendar event web entities.
   */
  public List<CalendarEventEntity> asEventWebEntities(
      Collection<CalendarEvent> events) {
    return events.stream().map(this::asEventWebEntity).collect(Collectors.toList());
  }

  /**
   * Converts the calendar event into its corresponding web entity. If the specified
   * calendar event isn't defined, then an HTTP 404 error is sent back instead of the
   * entity representation of the calendar event.
   * @param event the calendar event  to convert.
   * @return the corresponding calendar event  entity.
   */
  public CalendarEventEntity asEventWebEntity(CalendarEvent event) {
    assertEntityIsDefined(event.asCalendarComponent());
    SimpleCache cache = CacheServiceProvider.getRequestCacheService().getCache();
    CalendarEventEntity entity = cache.get(event.getId(), CalendarEventEntity.class);
    if (entity == null) {
      entity = CalendarEventEntity.fromEvent(event, getComponentId(), getZoneId())
          .withEventURI(getUri().getWebResourcePathBuilder().path(event.getCalendar().getId())
              .path(CALENDAR_EVENT_URI_PART)
              .path(event.getId()).build())
          .withCalendarURI(getUri().getWebResourcePathBuilder().path(event.getCalendar().getId())
              .build());
      cache.put(event.getId(), entity);
    }
    return entity;
  }

  /**
   * Converts the list of calendar event occurrence into list of calendar event occurrence web
   * entities.
   * @param occurrences the calendar event occurrences to convert.
   * @return the calendar event occurrence web entities.
   */
  public List<CalendarEventOccurrenceEntity> asOccurrenceWebEntities(
      Collection<CalendarEventOccurrence> occurrences) {
    return occurrences.stream().map(this::asOccurrenceWebEntity).collect(Collectors.toList());
  }

  /**
   * Converts the calendar event occurrence into its corresponding web entity. If the specified
   * calendar event occurrence isn't defined, then an HTTP 404 error is sent back instead of the
   * entity representation of the calendar event occurrence.
   * @param occurrence the calendar event occurrence to convert.
   * @return the corresponding calendar event occurrence entity.
   */
  public CalendarEventOccurrenceEntity asOccurrenceWebEntity(
      CalendarEventOccurrence occurrence) {
    assertEntityIsDefined(occurrence.getCalendarEvent().asCalendarComponent());
    List<CalendarEventAttendeeEntity> attendeeEntities = occurrence.getAttendees().stream()
        .map(attendee -> asAttendeeWebEntity(occurrence, attendee))
        .collect(Collectors.toList());
    return CalendarEventOccurrenceEntity.fromOccurrence(occurrence, getComponentId(), getZoneId())
        .withCalendarURI(calendarUri(getUri(), occurrence.getCalendarEvent().getCalendar()))
        .withEventURI(eventURI(getUri(), occurrence.getCalendarEvent()))
        .withOccurrenceURI(occurrenceURI(getUri(), occurrence))
        .withOccurrenceViewURI(occurrenceViewURI(occurrence))
        .withAttendees(attendeeEntities);
  }

  /**
   * Converts the calendar event attendee into its corresponding web entity. If the specified
   * calendar event attendee isn't defined, then an HTTP 404 error is sent back instead of the
   * entity representation of the calendar event occurrence.
   *
   * @param occurrence the occurrence the attendees belongs to.
   * @param attendee the calendar event attendee to convert.
   * @return the corresponding calendar event attendee entity.
   */
  public CalendarEventAttendeeEntity asAttendeeWebEntity(
      final CalendarEventOccurrence occurrence, Attendee attendee) {
    assertEntityIsDefined(attendee);
    return CalendarEventAttendeeEntity.from(attendee)
        .withURI(occurrenceAttendeeURI(getUri(), occurrence, attendee));
  }

  @Override
  protected String getBundleLocation() {
    return "org.silverpeas.calendar.multilang.calendarBundle";
  }

  private CalendarWebServiceProvider getCalendarWebServiceProvider() {
    return CalendarWebServiceProvider.get();
  }

  private WebMessager getMessager() {
    return WebMessager.getInstance();
  }
}