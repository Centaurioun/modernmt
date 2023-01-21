package eu.modernmt.api.framework.routing;

import com.google.gson.JsonElement;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.RESTResponse;
import eu.modernmt.api.framework.actions.Action;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class RouterServlet extends HttpServlet {

  private static final String DEFAULT_ENCODING = "UTF-8";

  protected final Logger logger = LogManager.getLogger(getClass());
  private RouteTree routes;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    routes = new RouteTree();

    for (Class<?> clazz : getDeclaredActions()) {
      if (!Action.class.isAssignableFrom(clazz)) continue;

      Class<? extends Action> actionClass = clazz.asSubclass(Action.class);

      Route route = actionClass.getAnnotation(Route.class);
      if (route != null) {
        HttpMethod method = route.method();

        for (String path : route.aliases()) {
          RouteTemplate template = new RouteTemplate('/' + path, actionClass, method);
          routes.add(template);

          if (logger.isDebugEnabled()) logger.debug("REST API registered: " + template);
        }
      }
    }
  }

  protected abstract Collection<Class<?>> getDeclaredActions() throws ServletException;

  private RESTRequest wrapRequest(HttpServletRequest req) {
    // Character Encoding
    String encoding = req.getCharacterEncoding();
    if (encoding == null)
      try {
        req.setCharacterEncoding(DEFAULT_ENCODING);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Invalid DEFAULT_ENCODING", e);
      }

    return new RESTRequest(req, routes);
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) {
    long start = System.currentTimeMillis();

    RESTRequest restRequest = wrapRequest(req);
    RESTResponse restResponse = new RESTResponse(resp);

    Route route = null;

    try {
      Class<? extends Action> actionClass = restRequest.getActionClass();
      if (actionClass == null) {
        restResponse.apiNotFound();
      } else {
        route = actionClass.getAnnotation(Route.class);

        Action action = actionClass.newInstance();
        action.execute(restRequest, restResponse);
      }
    } catch (Throwable e) {
      logger.error("Unexpected exceptions", e);
      restResponse.unexpectedError(e);
    } finally {
      long elapsedTime = System.currentTimeMillis() - start;

      if (logger.isInfoEnabled() && route != null && route.log()) {
        StringBuilder log = new StringBuilder();
        log.append('"');
        log.append(restRequest);
        log.append("\" ");
        log.append(restResponse.getHttpStatus());
        log.append(' ');
        log.append(elapsedTime);

        if (logger.isDebugEnabled()) {
          JsonElement json = restResponse.getContent();

          if (json != null) {
            String content = json.toString();
            if (content.length() > 500) content = content.substring(0, 499) + "[...]";

            log.append(' ');
            log.append(content);
          }
        }

        logger.info(log);
      }
    }
  }
}
