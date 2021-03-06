/**
 * Copyright (C) 2000 - 2012 Silverpeas
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

package com.silverpeas.jcrutil;

import com.silverpeas.jcrutil.model.impl.AbstractJcrRegisteringTestCase;
import com.silverpeas.jcrutil.security.impl.SilverpeasSystemCredentials;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Calendar;

import static org.junit.Assert.*;

@ContextConfiguration(locations = {"/spring-in-memory-jcr.xml"})
public class TestBasicDaoFactory extends AbstractJcrRegisteringTestCase {

  public TestBasicDaoFactory() {
  }

  @Override
  protected IDataSet getDataSet() throws Exception {
    ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(this.getClass().
        getResourceAsStream("test-jcrutil-dataset.xml")));
    dataSet.addReplacementObject("[NULL]", null);
    return dataSet;
  }

  @Override
  protected void clearRepository() throws Exception {
    Session session = null;
    try {
      session = getRepository().login(new SilverpeasSystemCredentials());
      session.getRootNode().getNode("kmelia36").remove();
      session.save();
    } catch (PathNotFoundException pex) {
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  @Test
  public void testGetComponentId() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String firstNodeName = RandomGenerator.getRandomString();
      Node firstNode = componentNode.addNode(firstNodeName, JcrConstants.NT_FOLDER);
      assertNotNull(firstNode);
      assertEquals("kmelia36", BasicDaoFactory.getComponentId(firstNode));
      String secondNodeName = RandomGenerator.getRandomString();
      Node secondNode = firstNode.addNode(secondNodeName, JcrConstants.NT_FOLDER);
      assertNotNull(secondNode);
      assertEquals("kmelia36", BasicDaoFactory.getComponentId(secondNode));
      String thirdNodeName = RandomGenerator.getRandomString();
      Node thirdNode = firstNode.addNode(thirdNodeName, JcrConstants.NT_FOLDER);
      assertNotNull(thirdNode);
      assertEquals("kmelia36", BasicDaoFactory.getComponentId(thirdNode));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testAddStringProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_I18N_NODE);
      String property = RandomGenerator.getRandomString();
      BasicDaoFactory.addStringProperty(node, JcrConstants.SLV_PROPERTY_NAME, property);
      assertTrue(node.hasProperty(JcrConstants.SLV_PROPERTY_NAME));
      assertEquals(property, node.getProperty(JcrConstants.SLV_PROPERTY_NAME).getString());
      String description = RandomGenerator.getRandomString();
      BasicDaoFactory.addStringProperty(node, JcrConstants.SLV_PROPERTY_DESCRIPTION, description);
      assertTrue(node.hasProperty(JcrConstants.SLV_PROPERTY_DESCRIPTION));
      assertEquals(description,
          node.getProperty(JcrConstants.SLV_PROPERTY_DESCRIPTION).getString());
      BasicDaoFactory.addStringProperty(node, JcrConstants.SLV_PROPERTY_DESCRIPTION, null);
      assertFalse(node.hasProperty(JcrConstants.SLV_PROPERTY_DESCRIPTION));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testAddDateProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_LINK);
      Calendar calend = RandomGenerator.getRandomCalendar();
      BasicDaoFactory
          .addDateProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE, calend.getTime());
      assertTrue(node.hasProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE));
      assertEquals(calend.getTime(),
          node.getProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE).getDate().getTime());
      BasicDaoFactory.addDateProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE, null);
      assertFalse(node.hasProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testAddCalendarProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_LINK);
      Calendar calend = RandomGenerator.getRandomCalendar();
      BasicDaoFactory.addCalendarProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE, calend);
      assertTrue(node.hasProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE));
      assertEquals(calend.getTimeInMillis(),
          node.getProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE).getDate().getTimeInMillis());
      BasicDaoFactory.addDateProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE, null);
      assertFalse(node.hasProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testGetStringProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36",
          JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_I18N_NODE);
      String name = RandomGenerator.getRandomString();
      node.setProperty(JcrConstants.SLV_PROPERTY_NAME, name);
      assertEquals(name, BasicDaoFactory.getStringProperty(node,
          JcrConstants.SLV_PROPERTY_NAME));
      assertNull(BasicDaoFactory.getStringProperty(node,
          JcrConstants.SLV_PROPERTY_DESCRIPTION));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testGetCalendarProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_LINK);
      Calendar calend = RandomGenerator.getRandomCalendar();
      Property dateProperty = node.setProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE, calend);
      assertEquals(calend.getTimeInMillis(),
          BasicDaoFactory.getCalendarProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE)
              .getTimeInMillis());
      dateProperty.remove();
      assertNull(
          BasicDaoFactory.getCalendarProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testGetDateProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_LINK);
      Calendar calend = RandomGenerator.getRandomCalendar();
      Property dateProperty = node.setProperty(JcrConstants.SLV_PROPERTY_CREATION_DATE, calend);
      assertEquals(calend.getTime(), BasicDaoFactory.getDateProperty(node,
          JcrConstants.SLV_PROPERTY_CREATION_DATE));
      dateProperty.remove();
      assertNull(BasicDaoFactory.getDateProperty(node, JcrConstants.SLV_PROPERTY_CREATION_DATE));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testGetIntProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_LINK);
      int id = RandomGenerator.getRandomYear();
      Property property = node.setProperty(JcrConstants.SLV_PROPERTY_AUTHOR, id);
      assertEquals(id, BasicDaoFactory.getIntProperty(node, JcrConstants.SLV_PROPERTY_AUTHOR));
      property.remove();
      assertEquals(0, BasicDaoFactory.getIntProperty(node, JcrConstants.SLV_PROPERTY_AUTHOR));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testGetLongProperty() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String nodeName = RandomGenerator.getRandomString();
      Node node = componentNode.addNode(nodeName, JcrConstants.SLV_LINK);
      long id = RandomGenerator.getRandomYear();
      Property property = node.setProperty(JcrConstants.SLV_PROPERTY_AUTHOR, id);
      assertEquals(id, BasicDaoFactory.getLongProperty(node, JcrConstants.SLV_PROPERTY_AUTHOR));
      property.remove();
      assertEquals(0l, BasicDaoFactory.getLongProperty(node, JcrConstants.SLV_PROPERTY_AUTHOR));
    } finally {
      BasicDaoFactory.logout(session);
    }
  }

  @Test
  public void testRemoveReference() throws Exception {
    String uuid1 = RandomGenerator.getRandomString();
    String uuid2 = RandomGenerator.getRandomString();
    String uuid3 = RandomGenerator.getRandomString();
    String uuid4 = RandomGenerator.getRandomString();
    Value[] references = new Value[]{
        ValueFactoryImpl.getInstance().createValue(uuid1),
        ValueFactoryImpl.getInstance().createValue(uuid2),
        ValueFactoryImpl.getInstance().createValue(uuid3),
        ValueFactoryImpl.getInstance().createValue(uuid4)};
    Value[] result = BasicDaoFactory.removeReference(references, uuid3);
    assertNotNull(result);
    assertEquals(3, result.length);
    assertEquals(uuid1, result[0].getString());
    assertEquals(uuid2, result[1].getString());
    assertEquals(uuid4, result[2].getString());
    result = BasicDaoFactory.removeReference(references, uuid1);
    assertNotNull(result);
    assertEquals(3, result.length);
    assertEquals(uuid2, result[0].getString());
    assertEquals(uuid3, result[1].getString());
    assertEquals(uuid4, result[2].getString());
  }

  @Test
  public void testComputeUniqueName() throws Exception {
    Session session = null;
    try {
      session = BasicDaoFactory.getSystemSession();
      Node componentNode = session.getRootNode().addNode("kmelia36", JcrConstants.NT_FOLDER);
      String firstNodeName = BasicDaoFactory.computeUniqueName("", "SB_Node_Node");
      Node firstNode = componentNode.addNode(firstNodeName, JcrConstants.NT_FOLDER);
      assertNotNull(firstNode);
      String secondNodeName = BasicDaoFactory.computeUniqueName("", "SB_Node_Node");
      assertEquals((Integer.parseInt(firstNodeName) + 1) + "", secondNodeName);
      Node secondNode = componentNode.addNode(secondNodeName, JcrConstants.NT_FOLDER);
      assertNotNull(secondNode);
    } finally {
      BasicDaoFactory.logout(session);
    }
  }
}
