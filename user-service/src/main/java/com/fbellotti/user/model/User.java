package com.fbellotti.user.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class User {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  private final int id;

  private String userName;
  private String password;
  private String firstName;
  private String lastName;

  public User() {
    this.id = COUNTER.getAndIncrement();
  }

  public User(String userName, String password) {
    this.id = COUNTER.getAndIncrement();
    this.userName = userName;
    this.password = password;
  }

  public int getId() {
    return id;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
