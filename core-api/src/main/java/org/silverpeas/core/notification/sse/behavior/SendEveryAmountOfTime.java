/*
 * Copyright (C) 2000 - 2021 Silverpeas
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
package org.silverpeas.core.notification.sse.behavior;

import org.silverpeas.core.notification.sse.ServerEvent;

/**
 * If an event implements this interface, its sending is performed by a JOB which is triggered
 * every an amount of time. It permits to limit the load when lot of event can be thrown.
 * <p>
 * It has no impact about store management.
 * </p>
 * <p>
 *  This behavior is only possible with {@link StoreLastOnly} behavior.
 * </p>
 * @author Yohann Chastagnier
 */
public interface SendEveryAmountOfTime extends StoreLastOnly {

  /**
   * Indicates of the {@link ServerEvent} has already been waiting for send.
   * @return true if it has already, false otherwise.
   */
  boolean hasWaitingFor();

  /**
   * Indicates that the {@link ServerEvent} will be waiting for a while before to be sent.
   */
  void markAsWaitingFor();
}