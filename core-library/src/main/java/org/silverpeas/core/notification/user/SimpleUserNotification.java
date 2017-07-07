/*
 * Copyright (C) 2000 - 2017 Silverpeas
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
package org.silverpeas.core.notification.user;

import org.silverpeas.core.admin.user.model.Group;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.notification.user.builder.AbstractTemplateUserNotificationBuilder;
import org.silverpeas.core.notification.user.client.NotificationMetaData;
import org.silverpeas.core.notification.user.client.constant.NotifAction;
import org.silverpeas.core.notification.user.client.constant.NotifMediaType;
import org.silverpeas.core.notification.user.model.NotificationResourceData;
import org.silverpeas.core.template.SilverpeasTemplate;
import org.silverpeas.core.ui.DisplayI18NHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This implementation of {@link UserNotification} permits to send a simple user notification,
 * basically a title and a message.
 * <p>This is useful when user notification must be performed into a simple functional context.</p>
 * <p>This implementation is designed in order to handle the messages into all the handled user
 * languages</p>
 * @author Yohann Chastagnier
 */
public class SimpleUserNotification implements UserNotification {

  private User sender = null;
  private NotifAction notifAction = NotifAction.REPORT;
  private Function<String, String> title = s -> "";
  private Function<String, String> message = m -> "";
  private Set<String> userIds = new HashSet<>();
  private Set<String> groupIds = new HashSet<>();
  private Set<String> externalMails = new HashSet<>();

  /**
   * Hidden constructor.
   */
  private SimpleUserNotification() {
  }

  /**
   * Initializes an instance by indicating the system as the sender.
   * @return the new instance.
   */
  public static SimpleUserNotification fromSystem() {
    return from(null);
  }

  /**
   * Initializes an instance by specifying directly the sender.
   * @param sender the {@link User} which represents the sender.
   * @return itself.
   */
  public static SimpleUserNotification from(User sender) {
    SimpleUserNotification userNotification = new SimpleUserNotification();
    userNotification.sender = sender;
    return userNotification;
  }

  /**
   * Sets the {@link Function} which provides a title from a language.
   * @param title a {@link Function} which provides a title by applying a language.
   * @return itself.
   */
  public SimpleUserNotification withTitle(Function<String, String> title) {
    this.title = title;
    return this;
  }

  /**
   * Sets the {@link Function} which provides a message from a language.
   * @param message a {@link Function} which provides a message by applying a language.
   * @return itself.
   */
  public SimpleUserNotification andMessage(Function<String, String> message) {
    this.message = message;
    return this;
  }

  /**
   * Sets the given users as receivers.
   * @param users the receivers represented by a list of {@link User}.
   * @return itself.
   */
  public SimpleUserNotification toUsers(Collection<User> users) {
    return toUsers(users.stream());
  }

  /**
   * Sets the given users as receivers.
   * @param users the receivers represented by an array of {@link User}.
   * @return itself.
   */
  public SimpleUserNotification toUsers(User... users) {
    return toUsers(Arrays.stream(users));
  }

  /**
   * Sets the given users as receivers.
   * @param users the receivers represented by a stream of {@link User}.
   * @return itself.
   */
  public SimpleUserNotification toUsers(Stream<User> users) {
    users.forEach(u -> userIds.add(u.getId()));
    return this;
  }

  /**
   * Sets the users of given groups as receivers.
   * @param groups receivers represented by the users contained into the list of {@link Group}.
   * @return itself.
   */
  public SimpleUserNotification toGroups(Collection<Group> groups) {
    return toGroups(groups.stream());
  }

  /**
   * Sets the users of given groups as receivers.
   * @param groups receivers represented by the users contained into the array of {@link
   * Group}.
   * @return itself.
   */
  public SimpleUserNotification toGroups(Group... groups) {
    return toGroups(Arrays.stream(groups));
  }

  /**
   * Sets the users of given groups as receivers.
   * @param groups receivers represented by the users contained into the stream of {@link
   * Group}.
   * @return itself.
   */
  public SimpleUserNotification toGroups(Stream<Group> groups) {
    groups.forEach(g -> groupIds.add(g.getId()));
    return this;
  }

  /**
   * Sets the given e-mails as receivers.
   * @param eMails a list of receiver e-mails.
   * @return itself.
   */
  public SimpleUserNotification toEMails(Collection<String> eMails) {
    return toEMails(eMails.stream());
  }

  /**
   * Sets the given e-mails as receivers.
   * @param eMails an array of receiver e-mails.
   * @return itself.
   */
  public SimpleUserNotification toEMails(String... eMails) {
    return toEMails(Arrays.stream(eMails));
  }

  /**
   * Sets the given e-mails as receivers.
   * @param eMails a stream of receiver e-mails.
   * @return itself.
   */
  public SimpleUserNotification toEMails(Stream<String> eMails) {
    eMails.forEach(e -> externalMails.add(e));
    return this;
  }

  @Override
  public NotificationMetaData getNotificationMetaData() {
    return build().getNotificationMetaData();
  }

  @Override
  public void send() {
    build().send();
  }

  @Override
  public void send(final NotifMediaType mediaType) {
    build().send(mediaType);
  }

  private UserNotification build() {
    return new SimpleUserNotificationBuilder(this).build();
  }

  private static class SimpleUserNotificationBuilder
      extends AbstractTemplateUserNotificationBuilder<Object> {

    private static final Object NO_RESOURCE = new Object();
    private SimpleUserNotification source;

    private SimpleUserNotificationBuilder(final SimpleUserNotification source) {
      super(NO_RESOURCE);
      this.source = source;
    }

    @Override
    protected void performTemplateData(final String language, final Object resource,
        final SilverpeasTemplate template) {
      final String title = source.title.apply(language);
      final String message = source.message.apply(language);
      final String senderName = source.sender != null ? source.sender.getDisplayedName() : "";
      getNotificationMetaData().addLanguage(language, title, "");
      template.setAttribute("message", message);
      template.setAttribute("sender", senderName);
    }

    @Override
    protected void performNotificationResource(final String language, final Object resource,
        final NotificationResourceData notificationResourceData) {
      // Nothing to perform here
    }

    @Override
    protected String getTitle() {
      return source.title.apply(DisplayI18NHelper.getDefaultLanguage());
    }

    @Override
    protected String getFileName() {
      return "simple";
    }

    @Override
    protected String getBundleSubjectKey() {
      return null;
    }

    @Override
    protected String getTemplatePath() {
      return "notification/user";
    }

    @Override
    protected NotifAction getAction() {
      return source.notifAction;
    }

    @Override
    protected String getComponentInstanceId() {
      return null;
    }

    @Override
    protected String getSender() {
      return source.sender != null ? source.sender.getId() : "";
    }

    @Override
    protected Collection<String> getUserIdsToNotify() {
      return source.userIds;
    }

    @Override
    protected Collection<String> getGroupIdsToNotify() {
      return source.groupIds;
    }

    @Override
    protected Collection<String> getExternalAddressesToNotify() {
      return source.externalMails;
    }

    @Override
    protected boolean isSendImmediatly() {
      return true;
    }
  }
}
