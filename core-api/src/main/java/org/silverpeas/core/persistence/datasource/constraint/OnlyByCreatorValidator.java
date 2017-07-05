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
package org.silverpeas.core.persistence.datasource.constraint;

import org.silverpeas.core.SilverpeasRuntimeException;
import org.silverpeas.core.admin.user.model.User;
import org.silverpeas.core.persistence.datasource.model.Entity;
import org.silverpeas.core.util.StringUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * Validator of the {@link OnlyByCreator} constraint.
 * @author mmoquillon
 */
public class OnlyByCreatorValidator implements ConstraintValidator<OnlyByCreator, Entity> {
  private String owner;

  @Override
  public void initialize(final OnlyByCreator constraintAnnotation) {
    this.owner = constraintAnnotation.owner();
  }

  @Override
  public boolean isValid(final Entity entity, final ConstraintValidatorContext context) {
    User requester = User.getCurrentRequester();
    if (requester != null) {
      Entity concerned = getConcernedEntity(entity);
      if (StringUtil.isDefined(concerned.getCreatedBy())) {
        return requester.isAccessAdmin() || requester.getId().equals(concerned.getCreatedBy());
      }
    }
    return true;
  }

  private Entity getConcernedEntity(final Entity entity) {
    Entity entityOwner;
    if (!owner.isEmpty()) {
      try {
        Field ownerField = entity.getClass().getDeclaredField(owner);
        ownerField.setAccessible(true);
        entityOwner = (Entity) ownerField.get(entity);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new SilverpeasRuntimeException(e.getMessage(), e);
      }
    } else {
      entityOwner = entity;
    }
    return entityOwner;
  }

}
