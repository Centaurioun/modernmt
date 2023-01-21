package eu.modernmt.api.framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Created by davide on 15/12/15. */
public class RESTResponse {

  public static final class ApiNotFoundException extends Exception {
    public ApiNotFoundException() {
      super("Invalid API endpoint");
    }
  }

  public static final class NotFoundException extends Exception {
    public NotFoundException() {
      super("Requested resource not found");
    }
  }

  protected final Logger logger = LogManager.getLogger(getClass());

  private HttpServletResponse response;
  private JsonObject content = null;

  public RESTResponse(HttpServletResponse response) {
    this.response = response;
  }

  public void apiNotFound() {
    output(HttpServletResponse.SC_NOT_FOUND, null, new ApiNotFoundException());
  }

  public void resourceNotFound() {
    output(HttpServletResponse.SC_NOT_FOUND, null, new NotFoundException());
  }

  public void badRequest() {
    badRequest(null);
  }

  public void badRequest(Throwable e) {
    output(HttpServletResponse.SC_BAD_REQUEST, null, e);
  }

  public void ok() {
    ok(new JsonObject());
  }

  public void ok(JsonElement json) {
    output(HttpServletResponse.SC_OK, json, null);
  }

  public void forbidden() {
    forbidden(null);
  }

  public void forbidden(Throwable e) {
    output(HttpServletResponse.SC_FORBIDDEN, null, e);
  }

  public void unexpectedError() {
    unexpectedError(null);
  }

  public void unexpectedError(Throwable e) {
    output(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, e);
  }

  public void unavailable() {
    unavailable(null);
  }

  public void unavailable(Throwable e) {
    output(HttpServletResponse.SC_SERVICE_UNAVAILABLE, null, e);
  }

  private void output(int httpStatus, JsonElement json, Throwable throwable) {
    if (content != null) throw new IllegalStateException("Output has been already set");

    content = new JsonObject();
    content.addProperty("status", httpStatus);

    if (throwable != null) content.add("error", encode(throwable));
    else if (json != null) content.add("data", json);

    response.setStatus(httpStatus);
    response.setContentType("application/json; charset=utf-8");
    response.addHeader("Access-Control-Allow-Origin", "*");

    try {
      if (content != null) {
        String rawContent = content.toString() + '\n';
        response.getOutputStream().write(rawContent.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      logger.error("unable to write response", e);
    }
  }

  private static JsonObject encode(Throwable e) {
    // Message
    String msg = e.getMessage();
    if (msg == null || msg.trim().isEmpty()) {
      Throwable cause = e.getCause();
      if (cause != null) msg = cause.getMessage();
    }

    msg = msg == null ? null : msg.trim();

    // Type
    String type = e.getClass().getSimpleName();

    // Encoding
    JsonObject error = new JsonObject();

    error.addProperty("type", type);
    if (msg != null) error.addProperty("message", msg);

    return error;
  }

  public int getHttpStatus() {
    return response.getStatus();
  }

  public JsonElement getContent() {
    return content;
  }
}
