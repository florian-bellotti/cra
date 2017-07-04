package com.fbellotti.user.model;

/**
 * @author <a href="http://fbellotti.com">Florian BELLOTTI</a>
 */
public class Error {

  private String code;
  private String message;

  public Error() {
    // empty
  }

  public Error(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
