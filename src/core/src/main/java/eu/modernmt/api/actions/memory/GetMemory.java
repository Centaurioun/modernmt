package eu.modernmt.api.actions.memory;

import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.framework.routing.TemplateException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Memory;
import eu.modernmt.persistence.PersistenceException;

/** Created by davide on 15/12/15. */
@Route(
    aliases = {"memories/:id", "domains/:id"},
    method = HttpMethod.GET)
public class GetMemory extends ObjectAction<Memory> {

  @Override
  protected Memory execute(RESTRequest req, Parameters _params) throws PersistenceException {
    Params params = (Params) _params;
    return ModernMT.memory.get(params.id);
  }

  @Override
  protected Parameters getParameters(RESTRequest req)
      throws Parameters.ParameterParsingException, TemplateException {
    return new Params(req);
  }

  public static class Params extends Parameters {

    private final long id;

    public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
      super(req);
      id = req.getPathParameterAsLong("id");
    }
  }
}
