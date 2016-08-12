package org.silverpeas.core.webapi.admin;

import org.silverpeas.core.admin.component.model.ComponentInstLight;
import org.silverpeas.core.admin.service.AdminController;
import org.silverpeas.core.admin.space.SpaceInstLight;
import org.silverpeas.core.annotation.RequestScoped;
import org.silverpeas.core.annotation.Service;
import org.silverpeas.core.util.ServiceProvider;
import org.silverpeas.core.util.StringUtil;
import org.silverpeas.core.webapi.base.annotation.Authenticated;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.silverpeas.core.webapi.admin.AdminResourceURIs.BIN_BASE_URI;

/**
 * A REST Web resource giving bin data (spaces and/or components).
 * @author Nicolas Eysseric
 */
@Service
@RequestScoped
@Path(BIN_BASE_URI)
@Authenticated
public class BinResource extends AbstractAdminResource {

  @POST
  @Path("empty")
  public Map<String, List<String>> empty() {
    checkAdminPrivilege();

    List<String> spaceIds = new ArrayList<String>();
    List<SpaceInstLight> spaces = getAdmin().getRemovedSpaces();
    for (SpaceInstLight space : spaces) {
      spaceIds.add(space.getId());
    }

    List<String> appIds = new ArrayList<String>();
    List<ComponentInstLight> apps = getAdmin().getRemovedComponents();
    for (ComponentInstLight app : apps) {
      appIds.add(app.getId());
    }

    return deleteSpacesAndApps(spaceIds.toArray(new String[0]), appIds.toArray(new String[0]));
  }

  @DELETE
  @Path("delete")
  public Map<String, List<String>> delete(@QueryParam("spaceIds") final String spaceIds,
      @QueryParam("appIds") final String appIds) {

    String[] sIds = new String[0];
    if (spaceIds != null) {
      sIds = spaceIds.split(",");
    }
    String[] aIds = new String[0];
    if (appIds != null) {
      aIds = appIds.split(",");
    }
    return deleteSpacesAndApps(sIds, aIds);
  }

  private Map<String, List<String>> deleteSpacesAndApps(String[] spaceIds, String appIds[]) {
    // first check if user is allowed to do admin operations
    checkAdminPrivilege();

    List<String> deleteOKForSpaceIds = new ArrayList<>();
    List<String> deleteNOKForSpaceIds = new ArrayList<>();
    for (String id : spaceIds) {
      String result = getAdmin().deleteSpaceInstById(getUserDetail(), id, true);
      if (StringUtil.isNotDefined(result)) {
        deleteNOKForSpaceIds.add(id);
      } else {
        deleteOKForSpaceIds.add(id);
      }
    }

    List<String> deleteOKForAppIds = new ArrayList<>();
    List<String> deleteNOKForAppIds = new ArrayList<>();
    for (String id : appIds) {
      String result = getAdmin().deleteComponentInst(getUserDetail(), id, true);
      if (StringUtil.isNotDefined(result)) {
        deleteNOKForAppIds.add(id);
      } else {
        deleteOKForAppIds.add(id);
      }
    }

    HashMap<String, List<String>> map = new HashMap<String, List<String>>();
    map.put("spaceIdsOK", deleteOKForSpaceIds);
    map.put("spaceIdsNOK", deleteNOKForSpaceIds);
    map.put("appIdsOK", deleteOKForAppIds);
    map.put("appIdsNOK", deleteNOKForAppIds);

    return map;
  }

  private AdminController getAdmin() {
    return ServiceProvider.getService(AdminController.class);
  }

  private void checkAdminPrivilege() {
    if (!getUserDetail().isAccessAdmin()) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
  }

  /*
   * (non-Javadoc)
   * @see com.silverpeas.web.RESTWebService#getComponentId()
   */
  @Override
  public String getComponentId() {
    throw new UnsupportedOperationException(
        "The BinResource doesn't belong to any component instance ids");
  }

}