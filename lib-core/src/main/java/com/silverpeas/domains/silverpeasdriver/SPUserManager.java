/**
 * Copyright (C) 2000 - 2013 Silverpeas
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

package com.silverpeas.domains.silverpeasdriver;

import org.silverpeas.persistence.model.identifier.UniqueIntegerIdentifier;
import org.silverpeas.persistence.repository.BasicEntityRepository;

import java.util.List;

/**
 * @author ehugonnet
 */
public interface SPUserManager extends BasicEntityRepository<SPUser, UniqueIntegerIdentifier> {

  List<SPUser> findByFirstname(String firstName);

  List<SPUser> findByLastname(String lastName);

  List<SPUser> findByPhone(String phone);

  List<SPUser> findByHomephone(String homephone);

  List<SPUser> findByCellphone(String cellphone);

  List<SPUser> findByFax(String fax);

  List<SPUser> findByAddress(String address);

  List<SPUser> findByTitle(String title);

  List<SPUser> findByCompany(String company);

  List<SPUser> findByPosition(String position);

  List<SPUser> findByEmail(String email);

}