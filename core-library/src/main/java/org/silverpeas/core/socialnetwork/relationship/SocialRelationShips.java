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
package org.silverpeas.core.socialnetwork.relationship;

import org.silverpeas.core.date.Date;
import org.silverpeas.core.exception.SilverpeasException;
import org.silverpeas.core.socialnetwork.SocialNetworkException;
import org.silverpeas.core.socialnetwork.model.SocialInformation;
import org.silverpeas.core.socialnetwork.provider.SocialRelationShipsInterface;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Bensalem Nabil
 */
@Singleton
public class SocialRelationShips implements SocialRelationShipsInterface {

  @Inject
  private RelationShipService relationShipService;

  @Override
  public List<SocialInformation> getSocialInformationsList(String userId, Date begin, Date end)
      throws SilverpeasException {
    try {
      return relationShipService.getAllMyRelationShips(userId, begin, end);
    } catch (SQLException ex) {
      throw new SocialNetworkException("SocialEvent.getSocialInformationsList()",
          SilverpeasException.ERROR, "root.EX_CANT_GET_REMOTE_OBJECT", ex);
    }
  }

  @Override
  public List<SocialInformation> getSocialInformationsListOfMyContacts(String myId,
      List<String> myContactsIds, Date begin, Date end) throws SilverpeasException {
    try {
      return relationShipService.getAllRelationShipsOfContacts(myContactsIds, begin,
          end);
    } catch (SQLException ex) {
      throw new SocialNetworkException("SocialEvent.getSocialInformationsList()",
          SilverpeasException.ERROR, "root.EX_CANT_GET_REMOTE_OBJECT", ex);
    }
  }
}
