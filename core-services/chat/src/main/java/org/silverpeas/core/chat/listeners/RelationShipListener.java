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
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.core.chat.listeners;

import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.chat.servers.ChatServer;
import org.silverpeas.core.chat.servers.DefaultChatServer;
import org.silverpeas.core.notification.system.CDIResourceEventListener;
import org.silverpeas.core.socialnetwork.relationship.RelationShip;
import org.silverpeas.core.socialnetwork.relationship.RelationShipEvent;
import org.silverpeas.core.util.logging.SilverLogger;

import javax.inject.Inject;

/**
 * Listen relationship modifications to clone them in the Chat server
 * @author remipassmoilesel
 */
public class RelationShipListener extends CDIResourceEventListener<RelationShipEvent> {

  private SilverLogger logger = SilverLogger.getLogger(this);

  @Inject
  @DefaultChatServer
  private ChatServer server;

  @Override
  public void onCreation(final RelationShipEvent event) throws Exception {
    final RelationShip rs = event.getTransition().getAfter();

    UserDetail uf1 = UserDetail.getById(String.valueOf(rs.getUser1Id()));
    UserDetail uf2 = UserDetail.getById(String.valueOf(rs.getUser2Id()));

    server.createRelationShip(uf1, uf2);

    logger.debug("Chat relationship between {0} and {1} has been created", uf1.getId(), uf2.getId());
  }

  @Override
  public void onDeletion(final RelationShipEvent event) throws Exception {
    final RelationShip rs = event.getTransition().getBefore();

    UserDetail uf1 = UserDetail.getById(String.valueOf(rs.getUser1Id()));
    UserDetail uf2 = UserDetail.getById(String.valueOf(rs.getUser2Id()));

    server.deleteRelationShip(uf1, uf2);

    logger.debug("Chat relationship between {0} and {1} has been deleted", uf1.getId(), uf2.getId());
  }

  @Override
  public boolean isEnabled() {
    return ChatServer.isEnabled();
  }
}
