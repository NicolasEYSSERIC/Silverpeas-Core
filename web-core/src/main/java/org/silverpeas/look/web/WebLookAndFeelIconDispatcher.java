package org.silverpeas.look.web;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;

import com.silverpeas.util.FileUtil;
import com.silverpeas.util.StringUtil;
import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.webactiv.util.FileRepositoryManager;
import com.stratelia.webactiv.util.ResourceLocator;

public class WebLookAndFeelIconDispatcher extends HttpServlet {

  private static final String internalPath;
  private static final String iconsPath;
  private static final boolean alternativeIcons;
  
  static {
    ResourceLocator generalLookSettings = new ResourceLocator("org.silverpeas.lookAndFeel.generalLook", "");
    internalPath = FileRepositoryManager.getSilverpeasHome()+generalLookSettings.getString("icons.path.internal");
    iconsPath = generalLookSettings.getString("icons.path.set");
    alternativeIcons = StringUtil.isDefined(iconsPath);
  }
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    
    String requestURI = req.getRequestURI().substring(1); // silverpeas/util/icons/truc/groupe.gif
     
    String targetPath = requestURI.substring(requestURI.indexOf('/')); // /util/icons....
    if (alternativeIcons) {
      targetPath = targetPath.replaceFirst("/icons/", iconsPath);
    }
        
    File icon = new File(internalPath+targetPath);    
    sendFile(res, icon);    
  }
  
  protected void sendFile(HttpServletResponse response, File file) throws IOException {
    response.setContentType(FileUtil.getMimeType(file.getName()));
    response.setHeader("Content-Length", String.valueOf(file.length()));
    try {
      FileUtils.copyFile(file, response.getOutputStream());
      response.getOutputStream().flush();
    } catch (IOException e) {
      SilverTrace.error("peasUtil", "AbstractFileSender.sendFile", "root.EX_CANT_READ_FILE",
          " file: " + file.getAbsolutePath(), e);
    }
  }

}