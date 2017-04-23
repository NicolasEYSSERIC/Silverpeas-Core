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
 * FLOSS exception.  You should have received a copy of the text describing
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

/*
 * DomainNavigationStock.java
 */

package org.silverpeas.web.jobdomain;

import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.admin.service.AdminController;
import org.silverpeas.core.admin.domain.model.Domain;
import org.silverpeas.core.admin.user.model.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manage the informations needed for domains navigation and browse PRE-REQUIRED : the
 * Domain passed in the constructor MUST BE A VALID DOMAIN (with Id, etc...)
 * @t.leroi
 */
public class DomainNavigationStock extends NavigationStock {
  private Domain mNavDomain = null;
  private String mDomainId = null;

  public DomainNavigationStock(String navDomain, AdminController adc,
      List<String> manageableGroupIds) {
    super(adc, manageableGroupIds);
    mDomainId = navDomain;
    refresh();
  }

  public void refresh() {
    mNavDomain = adc.getDomain(mDomainId);
    subUsers = adc.getUsersOfDomain(mNavDomain.getId());
    if (subUsers == null) {
      subUsers = new UserDetail[0];
    }
    JobDomainSettings.sortUsers(subUsers);
    subGroups = adc.getRootGroupsOfDomain(mNavDomain.getId());
    if (subGroups == null) {
      subGroups = new Group[0];
    }

    if (manageableGroupIds != null) {
      subGroups = filterGroupsToGroupManager(subGroups);
    }

    JobDomainSettings.sortGroups(subGroups);
  }

  private Group[] filterGroupsToGroupManager(Group[] groups) {
    List<Group> temp = new ArrayList<Group>();

    // filter groups
    for (Group group : groups) {
      if (isGroupVisible(group.getId())) {
        temp.add(group);
      }
    }

    return temp.toArray(new Group[temp.size()]);
  }

  public Domain getThisDomain() {
    return mNavDomain;
  }

}