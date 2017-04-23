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

/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent)
 ---*/

/*
 * NavigationStock.java
 */

package org.silverpeas.web.jobdomain;

import org.silverpeas.core.admin.service.AdminController;
import org.silverpeas.core.admin.user.model.Group;
import org.silverpeas.core.admin.user.model.UserDetail;

import java.util.Arrays;
import java.util.List;

/**
 * This class manage the informations needed for groups navigation and browse PRE-REQUIRED : the
 * Group passed in the constructor MUST BE A VALID GROUP (with Id, etc...)
 * @t.leroi
 */
public class NavigationStock {
  Group[] subGroups = null;
  UserDetail[] subUsers = null;
  AdminController adc = null;
  List<String> manageableGroupIds = null;

  public NavigationStock(AdminController adc, List<String> manageableGroupIds) {
    this.adc = adc;
    this.manageableGroupIds = manageableGroupIds;
  }

  // SubUsers functions
  public UserDetail[] getAllUserPage() {
    return subUsers;
  }

  public UserDetail[] getUserPage() {
    return subUsers;
  }

  // SubGroups functions
  public Group[] getAllGroupPage() {
    return subGroups;
  }

  public Group[] getGroupPage() {
    return subGroups;
  }

  protected boolean isGroupVisible(String groupId) {
    if (manageableGroupIds.contains(groupId)) {
      return true;
    } else {
      // get all subGroups of group
      List<String> subGroupIds = Arrays.asList(adc
          .getAllSubGroupIdsRecursively(groupId));

      // check if at least one manageable group is part of subGroupIds
      for (String manageableGroupId : manageableGroupIds) {
        if (subGroupIds.contains(manageableGroupId)) {
          return true;
        }
      }
    }
    return false;
  }
}