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

package org.silverpeas.core.web.mvc.controller;

import org.silverpeas.core.admin.component.constant.ComponentInstanceParameterName;
import org.silverpeas.core.admin.service.OrganizationController;
import org.silverpeas.core.admin.user.model.SilverpeasRole;
import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.clipboard.ClipboardSelection;
import org.silverpeas.core.personalization.UserPreferences;
import org.silverpeas.core.util.LocalizationBundle;
import org.silverpeas.core.util.SettingBundle;

import java.util.Collection;

/**
 * The interface for all component session controllers.
 * @author nicolas eysseric and didier wenzek
 */

public interface ComponentSessionController {
  /** Return the organizationController */
  OrganizationController getOrganisationController();

  /** Return the user language */
  String getLanguage();

  /** Return the UserDetail of the current user */
  UserDetail getUserDetail();

  /** Return the UserId of the current user */
  String getUserId();

  /** Return the space label (as known by the user) */
  String getSpaceLabel();

  /** Return the space id */
  String getSpaceId();

  /** Return the name of the component (as specified in the xmlComponent) */
  String getComponentName();

  /** Return the component label (as known by the user) */
  String getComponentLabel();

  /** Return the component id */
  String getComponentId();

  /**
   * Return the parameter value of current component instance from a given parameter name
   */
  String getComponentParameterValue(String parameterName);

  /**
   * Return the parameter value of current component instance from a given centralized parameter
   * name
   */
  String getComponentParameterValue(ComponentInstanceParameterName parameterName);

  String[] getUserAvailComponentIds();

  String getComponentUrl();

  /**
   * Gets the roles the user has on the component.
   * @return a list of user roles.
   */
  Collection<SilverpeasRole> getSilverpeasUserRoles();

  /**
   * Gets the highest role the user has on the component.
   * @return a user role.
   */
  SilverpeasRole getHighestSilverpeasUserRole();

  /** Return the name of the user's roles */
  @Deprecated
  String[] getUserRoles();

  UserPreferences getPersonalization();

  LocalizationBundle getMultilang();

  SettingBundle getIcon();

  SettingBundle getSettings();

  // Maintenance Mode
  boolean isAppInMaintenance();

  void setAppModeMaintenance(boolean mode);

  boolean isSpaceInMaintenance(String spaceId);

  void setSpaceModeMaintenance(String spaceId, boolean mode);

  Collection getClipboardSelectedObjects() throws Exception;

  String getClipboardErrorMessage() throws Exception;

  Exception getClipboardExceptionError() throws Exception;

  Collection getClipboardObjects() throws Exception;

  void addClipboardSelection(ClipboardSelection selection) throws Exception;

  void clipboardPasteDone() throws Exception;

  void setClipboardSelectedElement(int index, boolean selected) throws Exception;

  int getClipboardSize() throws Exception;

  void removeClipboardElement(int index) throws Exception;

}
