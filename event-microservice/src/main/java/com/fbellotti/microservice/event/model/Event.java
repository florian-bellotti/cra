package com.fbellotti.microservice.event.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Date;

/**
 * Event data object.
 *
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
@DataObject(generateConverter = true)
public class Event {

  private String id;
  private String title;
  private Date startDate;
  private Date endDate;
  private String description;
  private String projectCode;

  public Event() {
    // empty
  }

  public Event(JsonObject json) {
    EventConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    EventConverter.toJson(this, json);
    return json;
  }

  public String checkEvent() {
    if (title == null || title.isEmpty()) {
      return "Event's title is empty.";
    }
    return null;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  @Override
  public String toString() {
    return this.toJson().encodePrettily();
  }
}
