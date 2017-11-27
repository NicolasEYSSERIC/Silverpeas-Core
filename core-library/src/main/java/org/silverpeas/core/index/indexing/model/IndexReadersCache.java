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
package org.silverpeas.core.index.indexing.model;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.silverpeas.core.util.logging.SilverLogger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IndexReadersCache {
  private static final IndexReadersCache instance = new IndexReadersCache();
  private Map<String, IndexReader> indexReaders;

  private IndexReadersCache() {
    indexReaders = new HashMap<>();
  }

  private static IndexReadersCache getInstance() {
    return instance;
  }

  public static synchronized IndexReader getIndexReader(String path) {
    if (!getInstance().indexReaders.containsKey(path)) {
      try (DirectoryReader iReader = DirectoryReader.open(
          FSDirectory.open(new File(path).toPath()))) {
        getInstance().indexReaders.put(path, iReader);
      } catch (Exception e) {
        SilverLogger.getLogger(IndexReadersCache.class).error(e);
      }
    }
    return getInstance().indexReaders.get(path);
  }

  public static synchronized void removeIndexReader(String path) {
    if (getInstance().indexReaders.containsKey(path)) {
      IndexReader indexReader = getInstance().indexReaders.get(path);
      try {
        indexReader.close();
      } catch (IOException e) {
        SilverLogger.getLogger(IndexReadersCache.class).error(e);
      }
      getInstance().indexReaders.remove(path);
    }
  }

}
