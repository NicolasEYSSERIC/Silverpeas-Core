/*
 * Copyright (C) 2000 - 2018 Silverpeas
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
 * "https://www.silverpeas.org/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.core.admin.domain.driver.googledriver;

import com.google.api.services.admin.directory.model.User;
import org.silverpeas.core.admin.domain.AbstractDomainDriver;
import org.silverpeas.core.admin.service.AdminException;
import org.silverpeas.core.admin.user.model.GroupDetail;
import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.admin.user.model.UserFull;
import org.silverpeas.core.util.SettingBundle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.silverpeas.core.admin.domain.DomainDriver.ActionConstants.ACTION_MASK_RO_PULL_USER;
import static org.silverpeas.core.admin.user.constant.UserAccessLevel.USER;
import static org.silverpeas.core.admin.user.constant.UserState.DEACTIVATED;
import static org.silverpeas.core.admin.user.constant.UserState.VALID;

/**
 * Domain driver for LDAP access. Could be used to access any type of LDAP DB (even exchange)
 * IMPORTANT : For the moment, it is not possible to add, remove or update a group neither add or
 * remove an user. However, it is possible to update an user...
 * @author tleroi
 */
public class GoogleDriver extends AbstractDomainDriver {

  protected SettingBundle settings;
  private UserFilterManager userFilterManager;

  /**
   * Virtual method that performs extra initialization from a properties file. To overload by the
   * class who need it.
   * @param rs name of resource file
   */
  @Override
  public void initFromProperties(SettingBundle rs) {
    settings = rs;
    userFilterManager = new GoogleUserFilterManager(this, rs);
  }

  /**
   * Called when Admin starts the synchronization
   * @return
   */
  @Override
  public long getDriverActions() {
    return ACTION_MASK_RO_PULL_USER;
  }

  @Override
  public boolean isSynchroThreaded() {
    return settings.getBoolean("synchro.Threaded", false);
  }

  /**
   * Import a given user in Database from the reference
   * @param userLogin The User Login to import
   * @return The User object that contain new user information
   */
  @Override
  public UserDetail importUser(String userLogin) {
    return null;
  }

  /**
   * Remove a given user from database
   * @param userId The user id To remove synchro
   */
  @Override
  public void removeUser(String userId) {
    // In this driver, do nothing
  }

  /**
   * Update user information in database
   * @param userId The User Id to synchronize
   * @return The User object that contain new user information
   */
  @Override
  public UserDetail synchroUser(String userId) throws AdminException {
    return getUser(userId);
  }

  @Override
  public String createUser(UserDetail user) {
    return null;
  }

  @Override
  public void deleteUser(String userId) {
    // Silverpeas doesn't modify the data on Google
  }

  @Override
  public void updateUserFull(UserFull user) {
    // Silverpeas doesn't modify the data on Google
  }

  @Override
  public void updateUserDetail(UserDetail user) {
    // Silverpeas doesn't modify the data on Google
  }

  /**
   * Retrieve user information from database
   * @param specificId The user id as stored in the database
   * @return The User object that contain new user information
   * @throws AdminException
   */
  @Override
  public UserFull getUserFull(String specificId) throws AdminException {
    return userFullMapper.apply(request().user(specificId));
  }

  /**
   * Retrieve user information from database
   * @param specificId The user id as stored in the database
   * @return The User object that contain new user information
   * @throws AdminException
   */
  @Override
  public UserDetail getUser(String specificId) throws AdminException {
    return userDetailMapper.apply(request().user(specificId));
  }

  /**
   * Retrieve all users from the database
   * @return User[] An array of User Objects that contain users information
   * @throws AdminException
   */
  @Override
  public UserDetail[] getAllUsers() throws AdminException {
    return request().users().stream().map(userDetailMapper).toArray(UserDetail[]::new);
  }

  @Override
  public UserDetail[] getUsersBySpecificProperty(String propertyName, String propertyValue) {
    // No specific property handled for now
    return new UserDetail[0];
  }

  @Override
  public UserDetail[] getUsersByQuery(Map<String, String> query) {
    // No specific property handled for now
    return new UserDetail[0];
  }

  /**
   * Retrieve user's groups
   * @param specificId The user id as stored in the database
   * @return The User's groups specific Ids
   */
  @Override
  public String[] getUserMemberGroupIds(String specificId) {
    // In this driver, do nothing
    return new String[0];
  }

  /**
   * Import a given group in Database from the reference
   * @param groupName The group name to import
   * @return The group object that contain new group information
   */
  @Override
  public GroupDetail importGroup(String groupName) {
    return null;
  }

  /**
   * Remove a given group from database
   * @param groupId The group id To remove synchro
   */
  @Override
  public void removeGroup(String groupId) {
    // Silverpeas doesn't modify the remote LDAP
  }

  /**
   * Update group information in database
   * @param groupId The group Id to synchronize
   * @return The group object that contain new group information
   */
  @Override
  public GroupDetail synchroGroup(String groupId) {
    return getGroup(groupId);
  }

  @Override
  public String createGroup(GroupDetail group) {
    // Silverpeas doesn't modify the remote LDAP
    return null;
  }

  @Override
  public void deleteGroup(String groupId) {
    // Silverpeas doesn't modify the remote LDAP
  }

  @Override
  public void updateGroup(GroupDetail group) {
    // Silverpeas doesn't modify the remote LDAP
  }

  /**
   * Retrieve group information from database
   * @param specificId The group id as stored in the database
   * @return The GroupDetail object that contains user information
   */
  @Override
  public GroupDetail getGroup(String specificId) {
    // In this driver, do nothing
    return null;
  }

  @Override
  public GroupDetail getGroupByName(String groupName) {
    // In this driver, do nothing
    return null;
  }

  /**
   * Retrieve all groups contained in the given group
   * @param groupId The group id as stored in the database
   * @return GroupDetail[] An array of GroupDetail Objects that contain groups information
   */
  @Override
  public GroupDetail[] getGroups(String groupId) {
    // In this driver, do nothing
    return new GroupDetail[0];
  }

  /**
   * Retrieve all groups from the database
   * @return GroupDetail[] An array of GroupDetail Objects that contain groups information
   */
  @Override
  public GroupDetail[] getAllGroups() {
    // In this driver, do nothing
    return new GroupDetail[0];
  }

  /**
   * Retrieve all root groups from the database
   * @return GroupDetail[] An array of GroupDetail Objects that contain root groups information
   */
  @Override
  public GroupDetail[] getAllRootGroups() {
    return getAllGroups();
  }

  @Override
  public String[] getGroupMemberGroupIds(String groupId) {
    // In this driver, do nothing
    return new String[0];
  }

  @Override
  public List<String> getUserAttributes() {
    return Collections.emptyList();
  }

  @Override
  public void resetPassword(UserDetail user, String password) {
    // Access in read only
  }

  @Override
  public void resetEncryptedPassword(UserDetail user, String encryptedPassword) {
    // Access in read only
  }

  @Override
  public Optional<UserFilterManager> getUserFilterManager() {
    return Optional.of(userFilterManager);
  }

  GoogleDirectoryRequester request() {
    return new GoogleDirectoryRequester(settings.getString("service.account.user"),
        settings.getString("service.account.jsonKey"),
        userFilterManager.getRule());
  }

  private Function<User, UserDetail> userDetailMapper = u -> {
    final UserDetail user = new UserDetail();
    setCommonUserProps(u, user);
    return user;
  };

  private Function<User, UserFull> userFullMapper = u -> {
    final UserFull user = new UserFull();
    setCommonUserProps(u, user);
    // No specific property handled for now
    return user;
  };

  private void setCommonUserProps(final User u, final UserDetail user) {
    user.setSpecificId(u.getId());
    user.setLogin(u.getPrimaryEmail());
    user.setLastName(u.getName().getFamilyName());
    user.setFirstName(u.getName().getGivenName());
    @SuppressWarnings("unchecked") final List<Map<String, String>> emails = (List) u.getEmails();
    final String email = emails.stream()
        .filter(m -> "Work".equalsIgnoreCase(m.get("type")) || "Work".equalsIgnoreCase(m.get("customType")))
        .map(m -> m.get("address")).findFirst().orElseGet(u::getPrimaryEmail);
    user.seteMail(email);
    user.setAccessLevel(USER);
    if (u.getSuspended()) {
      user.setState(DEACTIVATED);
    } else {
      user.setState(VALID);
    }
  }
}
