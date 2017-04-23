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
 * GroupNavigationStock.java
 */

package org.silverpeas.web.jobdomain;

import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.util.StringUtil;
import org.silverpeas.core.admin.service.AdminController;
import org.silverpeas.core.admin.user.model.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manage the informations needed for groups navigation and browse PRE-REQUIRED : the
 * Group passed in the constructor MUST BE A VALID GROUP (with Id, etc...)
 * @t.leroi
 */
public class GroupNavigationStock extends NavigationStock {
  private Group navGroup = null;
  private String groupId = null;

  public GroupNavigationStock(String navGroup, AdminController adc,
      List<String> manageableGroupIds) {
    super(adc, manageableGroupIds);
    groupId = navGroup;
    refresh();
  }

  public void refresh() {
    navGroup = Group.getById(groupId);
    String[] subUsersIds = navGroup.getUserIds();
    if (subUsersIds == null) {
      subUsers = new UserDetail[0];
    } else {
      subUsers = adc.getUserDetails(subUsersIds);
    }
    JobDomainSettings.sortUsers(subUsers);

    String[] subGroupsIds = adc.getAllSubGroupIds(navGroup.getId());
    if (subGroupsIds == null) {
      subGroups = new Group[0];
    } else {
      if (manageableGroupIds != null) {
        subGroupsIds = filterGroupsToGroupManager(subGroupsIds);
      }

      subGroups = new Group[subGroupsIds.length];
      for (int i = 0; i < subGroupsIds.length; i++) {
        subGroups[i] = adc.getGroupById(subGroupsIds[i]);
      }
    }
    JobDomainSettings.sortGroups(subGroups);
  }

  private String[] filterGroupsToGroupManager(String[] groupIds) {
    List<String> temp = new ArrayList<String>();

    // filter groups
    for (String groupId : groupIds) {
      if (isGroupVisible(groupId)) {
        temp.add(groupId);
      }
    }

    return temp.toArray(new String[temp.size()]);
  }

  public boolean isThisGroup(String grId) {
    if (StringUtil.isDefined(grId)) {
      return grId.equals(navGroup.getId());
    } else {
      return isGroupValid(navGroup) == false;
    }
  }

  public Group getThisGroup() {
    return navGroup;
  }

  public static boolean isGroupValid(Group gr) {
    return gr != null && StringUtil.isDefined(gr.getId());
  }
}
