/*
 * Copyright (C) 2000 - 2016 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.web.jobdomain.control;

import org.apache.commons.fileupload.FileItem;
import org.silverpeas.core.admin.domain.DomainDriver;
import org.silverpeas.core.admin.domain.DomainServiceProvider;
import org.silverpeas.core.admin.domain.DomainType;
import org.silverpeas.core.admin.domain.exception.DomainConflictException;
import org.silverpeas.core.admin.domain.exception.DomainCreationException;
import org.silverpeas.core.admin.domain.exception.DomainDeletionException;
import org.silverpeas.core.admin.domain.model.Domain;
import org.silverpeas.core.admin.domain.model.DomainProperty;
import org.silverpeas.core.admin.domain.quota.UserDomainQuotaKey;
import org.silverpeas.core.admin.domain.synchro.SynchroDomainReport;
import org.silverpeas.core.admin.quota.exception.QuotaException;
import org.silverpeas.core.admin.service.AdminController;
import org.silverpeas.core.admin.service.AdminException;
import org.silverpeas.core.admin.space.SpaceInstLight;
import org.silverpeas.core.admin.user.constant.UserAccessLevel;
import org.silverpeas.core.admin.user.model.Group;
import org.silverpeas.core.admin.user.model.GroupDetail;
import org.silverpeas.core.admin.user.model.GroupProfileInst;
import org.silverpeas.core.admin.user.model.UserDetail;
import org.silverpeas.core.admin.user.model.UserFull;
import org.silverpeas.core.exception.SilverpeasException;
import org.silverpeas.core.exception.UtilException;
import org.silverpeas.core.exception.UtilTrappedException;
import org.silverpeas.core.notification.message.MessageNotifier;
import org.silverpeas.core.notification.user.client.NotificationManagerException;
import org.silverpeas.core.notification.user.client.NotificationMetaData;
import org.silverpeas.core.notification.user.client.NotificationParameters;
import org.silverpeas.core.notification.user.client.NotificationSender;
import org.silverpeas.core.notification.user.client.UserRecipient;
import org.silverpeas.core.personalization.UserPreferences;
import org.silverpeas.core.security.authentication.password.service.PasswordCheck;
import org.silverpeas.core.security.authentication.password.service.PasswordRulesServiceProvider;
import org.silverpeas.core.security.encryption.X509Factory;
import org.silverpeas.core.template.SilverpeasTemplate;
import org.silverpeas.core.template.SilverpeasTemplateFactory;
import org.silverpeas.core.ui.DisplayI18NHelper;
import org.silverpeas.core.util.ArrayUtil;
import org.silverpeas.core.util.WebEncodeHelper;
import org.silverpeas.core.util.LocalizationBundle;
import org.silverpeas.core.util.Pair;
import org.silverpeas.core.util.ResourceLocator;
import org.silverpeas.core.util.ServiceProvider;
import org.silverpeas.core.util.SettingBundle;
import org.silverpeas.core.util.StringUtil;
import org.silverpeas.core.util.URLUtil;
import org.silverpeas.core.util.csv.CSVReader;
import org.silverpeas.core.util.csv.Variant;
import org.silverpeas.core.util.logging.Level;
import org.silverpeas.core.util.logging.SilverLogger;
import org.silverpeas.core.web.mvc.controller.AbstractComponentSessionController;
import org.silverpeas.core.web.mvc.controller.ComponentContext;
import org.silverpeas.core.web.mvc.controller.MainSessionController;
import org.silverpeas.core.web.selection.Selection;
import org.silverpeas.core.web.selection.SelectionException;
import org.silverpeas.core.web.selection.SelectionUsersGroups;
import org.silverpeas.web.jobdomain.DomainNavigationStock;
import org.silverpeas.web.jobdomain.GroupNavigationStock;
import org.silverpeas.web.jobdomain.JobDomainPeasDAO;
import org.silverpeas.web.jobdomain.JobDomainPeasException;
import org.silverpeas.web.jobdomain.JobDomainPeasTrappedException;
import org.silverpeas.web.jobdomain.JobDomainSettings;
import org.silverpeas.web.jobdomain.SynchroUserWebServiceItf;
import org.silverpeas.web.jobdomain.UserRequestData;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.*;

import static org.silverpeas.core.SilverpeasExceptionMessages.*;
import static org.silverpeas.core.personalization.service.PersonalizationServiceProvider
    .getPersonalizationService;

/**
 * Class declaration
 *
 * @author
 */
public class JobDomainPeasSessionController extends AbstractComponentSessionController {

  private String mTargetUserId = null;
  private String targetDomainId = "";
  private DomainNavigationStock mTargetDomain = null;
  private List<GroupNavigationStock> mGroupsPath = new ArrayList<>();
  private SynchroThread mThethread = null;
  private Exception mErrorOccured = null;
  private String mSynchroReport = "";
  private Selection sel = null;
  private List<UserDetail> usersToImport = null;
  private Map<String, String> queryToImport = null;
  private AdminController mAdminCtrl = null;
  private List<String> listSelectedUsers = new ArrayList<String>();
  // pagination de la liste des résultats
  private int indexOfFirstItemToDisplay = 0;
  private boolean refreshDomain = true;
  private ListIndex currentIndex = new ListIndex(0);
  private List<UserDetail> sessionUsers = new ArrayList<>();

  private static final Properties templateConfiguration = new Properties();
  private static final String USER_ACCOUNT_TEMPLATE_FILE = "userAccount_email";

  private static final List<String> USERTYPES =
      Arrays.asList("Admin", "AdminPdc", "AdminDomain", "User", "Guest");

  /**
   * Standard Session Controller Constructeur
   *
   * @param mainSessionCtrl The user's profile
   * @param componentContext The component's profile
   * @see
   */
  public JobDomainPeasSessionController(MainSessionController mainSessionCtrl,
      ComponentContext componentContext) {
    super(mainSessionCtrl, componentContext,
        "org.silverpeas.jobDomainPeas.multilang.jobDomainPeasBundle",
        "org.silverpeas.jobDomainPeas.settings.jobDomainPeasIcons",
        "org.silverpeas.jobDomainPeas.settings.jobDomainPeasSettings");
    setComponentRootName(URLUtil.CMP_JOBDOMAINPEAS);
    mAdminCtrl = ServiceProvider.getService(AdminController.class);
    sel = getSelection();
    templateConfiguration.setProperty(SilverpeasTemplate.TEMPLATE_ROOT_DIR, getSettings()
        .getString("templatePath"));
    templateConfiguration.setProperty(SilverpeasTemplate.TEMPLATE_CUSTOM_DIR, getSettings()
        .getString("customersTemplatePath"));
  }

  public int getMinLengthLogin() {
    return JobDomainSettings.m_MinLengthLogin;
  }

  public boolean isUserAddingAllowedForGroupManager() {
    return JobDomainSettings.m_UserAddingAllowedForGroupManagers;
  }

  public boolean isAccessGranted() {
    return !getUserManageableGroupIds().isEmpty() || getUserDetail().isAccessAdmin()
        || getUserDetail().
        isAccessDomainManager();
  }

  public void setRefreshDomain(boolean refreshDomain) {
    this.refreshDomain = refreshDomain;
  }

  /*
   * USER functions
   */
  public void setTargetUser(String userId) {
    mTargetUserId = userId;
    processIndex(mTargetUserId);
  }

  public UserDetail getTargetUserDetail() throws JobDomainPeasException {
    UserDetail valret = null;

    if (StringUtil.isDefined(mTargetUserId)) {
      valret = getOrganisationController().getUserDetail(mTargetUserId);
      if (valret == null) {
        throw new JobDomainPeasException(unknown("user", mTargetUserId));
      }
    }
    return valret;
  }

  public UserFull getTargetUserFull() throws JobDomainPeasException {
    if (StringUtil.isDefined(mTargetUserId)) {
      UserFull user = UserFull.getById(mTargetUserId);
      if (user == null) {
        throw new JobDomainPeasException(unknown("user", mTargetUserId));
      }
      return user;
    }
    return null;
  }

  /**
   * Create a user
   *
   * @param userRequestData the data of the user from the request.
   * @param properties the user extra data.
   * @param req the current HttpServletRequest
   * @return
   * @throws JobDomainPeasException
   * @throws JobDomainPeasTrappedException
   */
  public String createUser(UserRequestData userRequestData, Map<String, String> properties,
      HttpServletRequest req) throws JobDomainPeasException, JobDomainPeasTrappedException {
    UserDetail theNewUser = new UserDetail();
    if (mAdminCtrl.isUserByLoginAndDomainExist(userRequestData.getLogin(), targetDomainId)) {
      JobDomainPeasTrappedException te = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.createUser()",
          SilverpeasException.ERROR, "admin.EX_ERR_LOGIN_ALREADY_USED");
      te.setGoBackPage("displayUserCreate");
      throw te;
    }

    theNewUser.setId("-1");
    if (StringUtil.isDefined(targetDomainId) && !targetDomainId.equals(Domain.MIXED_DOMAIN_ID)) {
      theNewUser.setDomainId(targetDomainId);
    }
    userRequestData.applyDataOnNewUser(theNewUser);
    String idRet = mAdminCtrl.addUser(theNewUser);
    if (StringUtil.isNotDefined(idRet)) {
      throw new JobDomainPeasException(failureOnAdding("user", theNewUser.getLogin()));
    }
    refresh();
    getSubUsers(false);
    setTargetUser(idRet);
    theNewUser.setId(idRet);

    // Registering the preferred user language if any and if it is different from the default one
    if (StringUtil.isDefined(userRequestData.getLanguage()) &&
        !userRequestData.getLanguage().equals(DisplayI18NHelper.getDefaultLanguage())) {
      UserPreferences userPreferences = theNewUser.getUserPreferences();
      userPreferences.setLanguage(userRequestData.getLanguage());
      getPersonalizationService().saveUserSettings(userPreferences);
    }

    // Send an email to alert this user
    notifyUserAccount(userRequestData, theNewUser, req, true);

    // Update UserFull informations
    UserFull uf = getTargetUserFull();
    if (uf != null) {
      if (uf.isPasswordAvailable()) {
        uf.setPasswordValid(userRequestData.isPasswordValid());
        uf.setPassword(userRequestData.getPassword());
      }

      idRet = updateUserFull(uf, properties);
    }
    // regroupement de l'utilisateur dans un groupe
    regroupInGroup(properties, null);
    // If group is provided, add newly created user to it
    if (StringUtil.isDefined(userRequestData.getGroupId())) {
      mAdminCtrl.addUserInGroup(idRet, userRequestData.getGroupId());
    }

    return idRet;
  }

  /**
   * notifyUserAccount send an email to the user only if userPasswordValid, sendEmail are true, and
   * if userEMail and userPassword are defined
   *
   * @param userRequestData the data of the user from the request.
   * @param user the userDetail
   * @param req the current HttpServletRequest
   * @param isNewUser boolean true if it's a created user, false else if
   */
  private void notifyUserAccount(UserRequestData userRequestData, UserDetail user,
      HttpServletRequest req, boolean isNewUser) {

    // Add code here in order to send an email notification
    if (userRequestData.isPasswordValid() && userRequestData.isSendEmail() &&
        StringUtil.isDefined(user.geteMail()) &&
        StringUtil.isDefined(userRequestData.getPassword())) {

      // Send an email notification
      Map<String, SilverpeasTemplate> templates = new HashMap<String, SilverpeasTemplate>();

      NotificationMetaData notifMetaData =
          new NotificationMetaData(NotificationParameters.ADDRESS_BASIC_SMTP_MAIL, "", templates,
              USER_ACCOUNT_TEMPLATE_FILE);

      String loginUrl = getLoginUrl(user, req);

      for (String lang : DisplayI18NHelper.getLanguages()) {
        LocalizationBundle notifBundle = ResourceLocator.getLocalizationBundle(
            "org.silverpeas.jobDomainPeas.multilang.jobDomainPeasBundle", lang);
        notifMetaData.addLanguage(lang, notifBundle.getString("JDP.createAccountNotifTitle"), "");
        templates.put(lang, getTemplate(user, loginUrl, userRequestData.getPassword(), isNewUser));
      }

      notifMetaData.addUserRecipient(new UserRecipient(user.getId()));
      NotificationSender sender = new NotificationSender(null);
      try {
        sender.notifyUser(NotificationParameters.ADDRESS_BASIC_SMTP_MAIL, notifMetaData);
      } catch (NotificationManagerException e) {
        SilverLogger.getLogger(this).error(e.getMessage(), e);
      }
    }
  }

  /**
   * Retrieve the login URL
   *
   * @param user the user detail (UserDetail)
   * @param req the current HttpServletRequest
   * @return the login URL string representation
   */
  private String getLoginUrl(UserDetail user, HttpServletRequest req) {
    SettingBundle general =
        ResourceLocator.getSettingBundle("org.silverpeas.lookAndFeel.generalLook");
    String loginPage = general.getString("loginPage");
    if (!StringUtil.isDefined(loginPage)) {
      loginPage = "/defaultLogin.jsp";
      String domainId = user.getDomainId();
      if (StringUtil.isDefined(domainId) && !"-1".equals(domainId)) {
        loginPage += "?DomainId=" + domainId;
      }
    }
    return URLUtil.getFullApplicationURL(req) + loginPage;
  }

  /**
   * Return the silverpeas template email configuration
   *
   * @param userDetail the current user detail
   * @param loginURL the login URL String
   * @param userPassword the current user password we have to send to new/modified user
   * @param isNew true if it's a created user, false else if
   * @return a SilverpeasTemplate
   */
  private SilverpeasTemplate getTemplate(UserDetail userDetail, String loginURL,
      String userPassword, boolean isNew) {
    Properties configuration = new Properties(templateConfiguration);
    SilverpeasTemplate template = SilverpeasTemplateFactory.createSilverpeasTemplate(configuration);
    template.setAttribute("userDetail", userDetail);
    template.setAttribute("loginURL", loginURL);
    template.setAttribute("pwd", userPassword);
    if (isNew) {
      template.setAttribute("createdUser", "true");
    }
    return template;
  }

  /**
   * Regroupement éventuel de l'utilisateur dans un groupe (pour les domaines SQL)
   *
   * @throws JobDomainPeasException
   */
  private void regroupInGroup(Map<String, String> properties, String lastGroupId)
      throws JobDomainPeasException {

    // Traitement du domaine SQL
    if (!getTargetDomain().isMixedOne() && !"0".equals(getTargetDomain().getId()) &&
        "org.silverpeas.core.admin.domain.driver.sqldriver.SQLDriver"
            .equals(getTargetDomain().getDriverClassName())) {

      SettingBundle specificRs = getTargetDomain().getSettings();
      int numPropertyRegroup = specificRs.getInteger("property.Grouping", -1);
      String nomRegroup = null;
      String theUserIdToRegroup = mTargetUserId;
      String[] newUserIds;
      List<String> lUserIds;
      List<String> lNewUserIds;
      if (numPropertyRegroup > -1) {
        String nomPropertyRegroupement = specificRs.getString("property_" + numPropertyRegroup
            + ".Name", null);

        if (nomPropertyRegroupement != null) {
          // Suppression de l'appartenance de l'utilisateur au groupe auquel il appartenait
          if (lastGroupId != null) {
            Group lastGroup = mAdminCtrl.getGroupById(lastGroupId);
            lUserIds = Arrays.asList(lastGroup.getUserIds());
            lNewUserIds = new ArrayList<String>(lUserIds);
            lNewUserIds.remove(theUserIdToRegroup);
            newUserIds = lNewUserIds.toArray(new String[lNewUserIds.size()]);
            updateGroupSubUsers(lastGroupId, newUserIds);
          }

          // Recherche du nom du regroupement (nom du groupe)
          for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().equals(nomPropertyRegroupement)) {
              nomRegroup = entry.getValue();
              break;
            }
          }
        }
      }

      if (StringUtil.isNotEmpty(nomRegroup)) {
        // Recherche le groupe dans le domaine
        GroupDetail group = mAdminCtrl.getGroupByNameInDomain(nomRegroup, targetDomainId);
        if (group == null) {
          // le groupe n'existe pas, on le crée
          group = new GroupDetail();
          group.setId("-1");
          group.setDomainId(targetDomainId);
          // root group
          group.setSuperGroupId(null);
          group.setName(nomRegroup);
          group.setDescription("");
          String groupId = mAdminCtrl.addGroup(group);
          group = mAdminCtrl.getGroupById(groupId);
        }

        lUserIds = Arrays.asList(group.getUserIds());
        lNewUserIds = new ArrayList<String>(lUserIds);
        lNewUserIds.add(theUserIdToRegroup);
        newUserIds = lNewUserIds.toArray(new String[lNewUserIds.size()]);

        // Ajout de l'appartenance de l'utilisateur au groupe
        updateGroupSubUsers(group.getId(), newUserIds);
      }
    }
  }

  /**
   * Parse the CSV file.
   *
   * @param filePart
   * @param req the current HttpServletRequest
   * @throws UtilTrappedException
   * @throws JobDomainPeasTrappedException
   * @throws JobDomainPeasException
   */
  public void importCsvUsers(FileItem filePart, boolean sendEmail, HttpServletRequest req)
      throws UtilTrappedException, JobDomainPeasTrappedException, JobDomainPeasException {
    InputStream is;
    try {
      is = filePart.getInputStream();
    } catch (IOException e) {
      JobDomainPeasTrappedException jdpe = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.importCsvUsers",
          SilverpeasException.ERROR, "jobDomainPeas.EX_CSV_FILE", e);
      jdpe.setGoBackPage("displayUsersCsvImport");
      throw jdpe;
    }
    CSVReader csvReader = new CSVReader(getLanguage());
    csvReader.initCSVFormat("org.silverpeas.jobDomainPeas.settings.usersCSVFormat", "User", ";",
        getTargetDomain().getPropFileName(), "property");

    // spécifique domaine Silverpeas (2 colonnes en moins (password et
    // passwordValid)
    if (getTargetDomain().isMixedOne() || "0".equals(getTargetDomain().getId())) {
      // domaine Silverpeas
      csvReader.setSpecificNbCols(csvReader.getSpecificNbCols() - 2);
    }

    Variant[][] csvValues;
    try {
      csvValues = csvReader.parseStream(is);
    } catch (UtilTrappedException ute) {
      ute.setGoBackPage("displayUsersCsvImport");
      throw ute;
    }

    StringBuilder listErrors = new StringBuilder("");
    String nom;
    String prenom;
    String login;
    String existingLogin;
    String email;
    String droits;
    UserAccessLevel userAccessLevel;
    String motDePasse;

    String title;
    String company;
    String position;
    String boss;
    String phone;
    String homePhone;
    String fax;
    String cellularPhone;
    String address;

    String informationSpecifiqueString;
    boolean informationSpecifiqueBoolean;

    for (int i = 0; i < csvValues.length; i++) {
      // Nom
      nom = csvValues[i][0].getValueString();
      if (nom.length() == 0) {
        // champ obligatoire
        listErrors.append(getErrorMessage(i + 1, 1, nom));
        listErrors.append(getString("JDP.obligatoire")).append("<br/>");
      } else if (nom.length() > 100) {
        listErrors.append(getErrorMessage(i + 1, 1, nom));
        listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").
            append(getString("JDP.caracteres")).append("<br/>");
      }

      // Prenom
      prenom = csvValues[i][1].getValueString();
      if (prenom.length() > 100) {
        listErrors.append(getErrorMessage(i + 1, 2, prenom));
        listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").append(
            getString("JDP.caracteres")).append("<br/>");
      }

      // Login
      login = csvValues[i][2].getValueString();
      if (login.length() == 0) {
        listErrors.append(getErrorMessage(i + 1, 3, login));
        listErrors.append(getString("JDP.obligatoire")).append("<br/>");
      } else if (login.length() < JobDomainSettings.m_MinLengthLogin) {
        listErrors.append(getErrorMessage(i + 1, 3, login));
        listErrors.append(getString("JDP.nbCarMin")).append(" ").append(
            JobDomainSettings.m_MinLengthLogin).append(" ").append(getString("JDP.caracteres")).
            append("<br/>");
      } else if (login.length() > 50) {
        listErrors.append(getErrorMessage(i + 1, 3, login));
        listErrors.append(getString("JDP.nbCarMax")).append(" 50 ").append(getString(
            "JDP.caracteres")).append("<br/>");
      } else {
        // verif login unique
        existingLogin = mAdminCtrl.getUserIdByLoginAndDomain(login,
            targetDomainId);
        if (existingLogin != null) {
          listErrors.append(getErrorMessage(i + 1, 3, login));
          listErrors.append(getString("JDP.existingLogin")).append("<br/>");
        }
      }

      // Email
      email = csvValues[i][3].getValueString();
      if (email.length() > 100) {
        listErrors.append(getErrorMessage(i + 1, 4, email));
        listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").append(getString(
            "JDP.caracteres")).append("<br/>");
      }

      // Droits
      droits = csvValues[i][4].getValueString();
      if (!"".equals(droits) && !USERTYPES.contains(droits)) {
        listErrors.append(getErrorMessage(i + 1, 5, droits));
        listErrors.append(getString("JDP.valeursPossibles")).append("<br/>");
      }

      // MotDePasse
      motDePasse = csvValues[i][5].getValueString();
      // password is not mandatory
      if (StringUtil.isDefined(motDePasse)) {
        // Cheking password
        PasswordCheck passwordCheck = PasswordRulesServiceProvider.getPasswordRulesService().check(motDePasse);
        if (!passwordCheck.isCorrect()) {
          listErrors.append(getErrorMessage(i + 1, 6, motDePasse))
              .append(passwordCheck.getFormattedErrorMessage(getLanguage()));
          listErrors.append("<br/>");
        } else if (motDePasse.length() > 32) {
          listErrors.append(getErrorMessage(i + 1, 6, motDePasse));
          listErrors.append(getString("JDP.nbCarMax")).append(" 32 ").append(getString(
              "JDP.caracteres")).append("<br/>");
        }
      }

      if (csvReader.getSpecificNbCols() > 0) {
        if (getTargetDomain().isMixedOne() || "0".equals(getTargetDomain().getId())) {
          // domaine Silverpeas

          // title
          title = csvValues[i][6].getValueString();
          if (title.length() > 100) {
            listErrors.append(getErrorMessage(i + 1, 7, title));
            listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // company
          company = csvValues[i][7].getValueString();
          if (company.length() > 100) {
            listErrors.append(getErrorMessage(i + 1, 8, company));
            listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // position
          position = csvValues[i][8].getValueString();
          if (position.length() > 100) {
            listErrors.append(getErrorMessage(i + 1, 9, position));
            listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // boss
          boss = csvValues[i][9].getValueString();
          if (boss.length() > 100) {
            listErrors.append(getErrorMessage(i + 1, 10, boss));
            listErrors.append(getString("JDP.nbCarMax")).append(" 100 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // phone
          phone = csvValues[i][10].getValueString();
          if (phone.length() > 20) {
            listErrors.append(getErrorMessage(i + 1, 11, phone));
            listErrors.append(getString("JDP.nbCarMax")).append(" 20 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // homePhone
          homePhone = csvValues[i][11].getValueString();
          if (homePhone.length() > 20) {
            listErrors.append(getErrorMessage(i + 1, 12, homePhone));
            listErrors.append(getString("JDP.nbCarMax")).append(" 20 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // fax
          fax = csvValues[i][12].getValueString();
          if (fax.length() > 20) {
            listErrors.append(getErrorMessage(i + 1, 13, fax));
            listErrors.append(getString("JDP.nbCarMax")).append(" 20 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // cellularPhone
          cellularPhone = csvValues[i][13].getValueString();
          if (cellularPhone.length() > 20) {
            listErrors.append(getErrorMessage(i + 1, 14, cellularPhone));
            listErrors.append(getString("JDP.nbCarMax")).append(" 20 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }

          // address
          address = csvValues[i][14].getValueString();
          if (address.length() > 500) {
            listErrors.append(getErrorMessage(i + 1, 15, address));
            listErrors.append(getString("JDP.nbCarMax")).append(" 500 ").append(getString(
                "JDP.caracteres")).append("<br/>");
          }
        } else {
          // domaine SQL
          for (int j = 0; j < csvReader.getSpecificNbCols(); j++) {
            if (Variant.TYPE_STRING.equals(csvReader.getSpecificColType(j))) {
              informationSpecifiqueString = csvValues[i][j + 6].getValueString();
              // verify the length
              if (informationSpecifiqueString.length() > csvReader.getSpecificColMaxLength(j)) {
                listErrors.append(getErrorMessage(i + 1, j + 6, informationSpecifiqueString));
                listErrors.append(getString("JDP.nbCarMax")).append(" ")
                    .append(csvReader.getSpecificColMaxLength(j)).append(" ")
                    .append(getString("JDP.caracteres")).append("<br/>");
              }
            }
          }
        }
      }
    }

    if (listErrors.length() > 0) {
      JobDomainPeasTrappedException jdpe = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.importCsvUsers",
          SilverpeasException.ERROR, "jobDomainPeas.EX_CSV_FILE", listErrors.toString());
      jdpe.setGoBackPage("displayUsersCsvImport");
      throw jdpe;
    }

    // pas d'erreur, on importe les utilisateurs
    HashMap<String, String> properties;
    for (Variant[] csvValue : csvValues) {
      // Nom
      nom = csvValue[0].getValueString();

      // Prenom
      prenom = csvValue[1].getValueString();

      // Login
      login = csvValue[2].getValueString();

      // Email
      email = csvValue[3].getValueString();

      // Droits
      droits = csvValue[4].getValueString();
      if ("Admin".equals(droits)) {
        userAccessLevel = UserAccessLevel.ADMINISTRATOR;
      } else if ("AdminPdc".equals(droits)) {
        userAccessLevel = UserAccessLevel.PDC_MANAGER;
      } else if ("AdminDomain".equals(droits)) {
        userAccessLevel = UserAccessLevel.DOMAIN_ADMINISTRATOR;
      } else if ("User".equals(droits)) {
        userAccessLevel = UserAccessLevel.USER;
      } else if ("Guest".equals(droits)) {
        userAccessLevel = UserAccessLevel.GUEST;
      } else {
        userAccessLevel = UserAccessLevel.USER;
      }

      // MotDePasse
      motDePasse = csvValue[5].getValueString();

      // données spécifiques
      properties = new HashMap<String, String>();
      if (csvReader.getSpecificNbCols() > 0) {
        if (getTargetDomain().isMixedOne() || "0".equals(getTargetDomain().getId())) {
          // domaine Silverpeas

          // title
          title = csvValue[6].getValueString();
          properties.put(csvReader.getSpecificParameterName(0), title);

          // company
          company = csvValue[7].getValueString();
          properties.put(csvReader.getSpecificParameterName(1), company);

          // position
          position = csvValue[8].getValueString();
          properties.put(csvReader.getSpecificParameterName(2), position);

          // boss
          boss = csvValue[9].getValueString();
          properties.put(csvReader.getSpecificParameterName(3), boss);

          // phone
          phone = csvValue[10].getValueString();
          properties.put(csvReader.getSpecificParameterName(4), phone);

          // homePhone
          homePhone = csvValue[11].getValueString();
          properties.put(csvReader.getSpecificParameterName(5), homePhone);

          // fax
          fax = csvValue[12].getValueString();
          properties.put(csvReader.getSpecificParameterName(6), fax);

          // cellularPhone
          cellularPhone = csvValue[13].getValueString();
          properties.put(csvReader.getSpecificParameterName(7), cellularPhone);

          // address
          address = csvValue[14].getValueString();
          properties.put(csvReader.getSpecificParameterName(8), address);

        } else {
          // domaine SQL
          // informations spécifiques
          for (int j = 0; j < csvReader.getSpecificNbCols(); j++) {
            if (Variant.TYPE_STRING.equals(csvReader.getSpecificColType(j))) {
              informationSpecifiqueString = csvValue[j + 6].getValueString();
              properties.put(csvReader.getSpecificParameterName(j),
                  informationSpecifiqueString);
            } else if (Variant.TYPE_BOOLEAN.equals(csvReader.getSpecificColType(j))) {
              informationSpecifiqueBoolean = csvValue[j + 6].getValueBoolean();
              if (informationSpecifiqueBoolean) {
                properties.put(csvReader.getSpecificParameterName(j), "1");
              } else {
                properties.put(csvReader.getSpecificParameterName(j), "0");
              }
            }
          }
        }
      }

      // password is not mandatory
      boolean passwordValid = StringUtil.isDefined(motDePasse);
      UserRequestData userRequestData = new UserRequestData();
      userRequestData.setLogin(login);
      userRequestData.setLastName(nom);
      userRequestData.setFirstName(prenom);
      userRequestData.setEmail(email);
      userRequestData.setAccessLevel(userAccessLevel);
      userRequestData.setPasswordValid(passwordValid);
      userRequestData.setPassword(motDePasse);
      userRequestData.setSendEmail(sendEmail);
      userRequestData.setUserManualNotifReceiverLimitEnabled(true);
      createUser(userRequestData, properties, req);
    }
  }

  private String getErrorMessage(int line, int column, String value) {
    StringBuilder str = new StringBuilder();
    str.append(getString("JDP.ligne")).append(" = ").append(line).append(", ");
    str.append(getString("JDP.colonne")).append(" = ").append(column).append(", ");
    str.append(getString("JDP.valeur")).append(" = ").append(StringUtil.truncate(value, 100))
        .append(", ");
    return str.toString();
  }

  private String getLastGroupId(UserFull theUser) {
    // Traitement du domaine SQL
    if (!getTargetDomain().isMixedOne()
        && !"0".equals(getTargetDomain().getId()) &&
        "org.silverpeas.core.admin.domain.driver.sqldriver.SQLDriver"
            .equals(getTargetDomain().getDriverClassName())) {
      SettingBundle specificRs = getTargetDomain().getSettings();
      int numPropertyRegroup = specificRs.getInteger("property.Grouping", -1);
      String nomLastGroup = null;
      if (numPropertyRegroup > -1) {
        String nomPropertyRegroupement = specificRs.getString("property_" + numPropertyRegroup
            + ".Name", null);
        if (nomPropertyRegroupement != null) {
          // Recherche du nom du regroupement (nom du groupe)
          String value = null;
          for (String key : theUser.getPropertiesNames()) {
            value = theUser.getValue(key);
            if (key.equals(nomPropertyRegroupement)) {
              nomLastGroup = value;
              break;
            }
          }
        }
      }

      if (StringUtil.isNotEmpty(nomLastGroup)) {
        // Recherche le groupe dans le domaine
        Group group = mAdminCtrl.getGroupByNameInDomain(nomLastGroup, targetDomainId);
        if (group != null) {
          return group.getId();
        }
      }
    }

    return null;
  }

  /**
   * Modify user account information
   *
   * @param userRequestData the data of the user from the request.
   * @param properties the user extra data.
   * @param req the current HttpServletRequest
   * @throws JobDomainPeasException
   */
  public void modifyUser(UserRequestData userRequestData, Map<String, String> properties,
      HttpServletRequest req) throws JobDomainPeasException {

    UserFull theModifiedUser = mAdminCtrl.getUserFull(userRequestData.getId());
    if (theModifiedUser == null) {
      throw new JobDomainPeasException(unknown("user", userRequestData.getId()));
    }

    // nom du groupe auquel était rattaché l'utilisateur
    String lastGroupId = getLastGroupId(theModifiedUser);

    userRequestData.applyDataOnExistingUser(theModifiedUser);

    notifyUserAccount(userRequestData, theModifiedUser, req, false);

    String idRet = updateUserFull(theModifiedUser, properties);
    refresh();
    setTargetUser(idRet);

    // regroupement de l'utilisateur dans un groupe
    regroupInGroup(properties, lastGroupId);
  }

  public void modifySynchronizedUser(UserRequestData userRequestData)
      throws JobDomainPeasException {


    UserDetail theModifiedUser = mAdminCtrl.getUserDetail(userRequestData.getId());
    if (theModifiedUser == null) {
      throw new JobDomainPeasException(unknown("synchronized user", userRequestData.getId()));
    }
    theModifiedUser.setAccessLevel(userRequestData.getAccessLevel());
    theModifiedUser.setUserManualNotificationUserReceiverLimit(
        userRequestData.getUserManualNotifReceiverLimitValue());
    String idRet = mAdminCtrl.updateSynchronizedUser(theModifiedUser);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(
          failureOnUpdate("synchronized user", userRequestData.getId()));
    }
    refresh();
    setTargetUser(idRet);
  }

  public void modifyUserFull(UserRequestData userRequestData, Map<String, String> properties)
      throws JobDomainPeasException {
    UserFull theModifiedUser = mAdminCtrl.getUserFull(userRequestData.getId());
    if (theModifiedUser == null) {
      throw new JobDomainPeasException(unknown("user", userRequestData.getId()));
    }

    theModifiedUser.setAccessLevel(userRequestData.getAccessLevel());
    theModifiedUser.setUserManualNotificationUserReceiverLimit(
        userRequestData.getUserManualNotifReceiverLimitValue());

    String idRet = updateUserFull(theModifiedUser, properties);
    refresh();
    setTargetUser(idRet);
  }

  public void blockUser(String userId) throws JobDomainPeasException {
    mAdminCtrl.blockUser(userId);
  }

  public void unblockUser(String userId) throws JobDomainPeasException {
    mAdminCtrl.unblockUser(userId);
  }

  public void deactivateUser(String userId) throws JobDomainPeasException {
    mAdminCtrl.deactivateUser(userId);
  }

  public void activateUser(String userId) throws JobDomainPeasException {
    mAdminCtrl.activateUser(userId);
  }

  public void deleteUser(String idUser) throws JobDomainPeasException {
    boolean deleteUser = true;

    if (!UserAccessLevel.ADMINISTRATOR.equals(getUserAccessLevel())
        && !UserAccessLevel.DOMAIN_ADMINISTRATOR.equals(getUserAccessLevel()) && isGroupManager()) {
      // Manage deleting case for group manager
      deleteUser = deleteUserByGroupManager(idUser);
    }

    if (deleteUser) {
      String idRet = mAdminCtrl.deleteUser(idUser);
      if (!StringUtil.isDefined(idRet)) {
        throw new JobDomainPeasException(failureOnUpdate("user", idUser));
      }
      if (mTargetUserId.equals(idUser)) {
        mTargetUserId = null;
      }

      if ((getDomainActions() & DomainDriver.ACTION_X509_USER) != 0) {
        // revocate user's certificate
        revocateCertificate(idUser);
      }

      refresh();
    }
  }

  private boolean deleteUserByGroupManager(String userId) {
    boolean deleteUser = true;
    List<String> directGroupIds = Arrays.asList(getOrganisationController().
        getDirectGroupIdsOfUser(userId));
    List<String> manageableGroupIds = getUserManageableGroupIds();

    String directGroupId;
    String rootGroupId;
    List<String> groupIdLinksToRemove = new ArrayList<String>();
    for (String directGroupId1 : directGroupIds) {
      directGroupId = directGroupId1;

      // get root group of each directGroup
      List<String> groupPath = mAdminCtrl.getPathToGroup(directGroupId);
      if (groupPath != null && !groupPath.isEmpty()) {
        rootGroupId = groupPath.get(0);
      } else {
        rootGroupId = directGroupId;
      }

      // if root group is not one of manageable group, avoid deletion
      // user belongs to another community
      if (!manageableGroupIds.contains(rootGroupId)) {
        deleteUser = false;
      } else {
        groupIdLinksToRemove.add(directGroupId);
      }
    }

    if (!deleteUser) {
      // removes only links between user and manageable groups
      for (String groupIdLinkToRemove : groupIdLinksToRemove) {
        mAdminCtrl.removeUserFromGroup(userId, groupIdLinkToRemove);
      }
      refresh();
    }
    return deleteUser;
  }

  public Iterator<DomainProperty> getPropertiesToImport() throws JobDomainPeasException {
    return mAdminCtrl.getSpecificPropertiesToImportUsers(targetDomainId,
        getLanguage()).iterator();
  }

  public void importUser(String userLogin) throws JobDomainPeasException {



    String idRet = mAdminCtrl.synchronizeImportUser(targetDomainId, userLogin);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnAdding("synchronized user", userLogin));
    }
    refresh();
    setTargetUser(idRet);
  }

  public void importUsers(String[] specificIds) throws JobDomainPeasException {
    for (int i = 0; specificIds != null && i < specificIds.length; i++) {
      mAdminCtrl.synchronizeImportUser(targetDomainId, specificIds[i]);
    }
    refresh();
  }

  public List<UserDetail> searchUsers(Map<String, String> query)
      throws JobDomainPeasException {

    queryToImport = query;
    usersToImport = mAdminCtrl.searchUsers(targetDomainId, query);
    return usersToImport;
  }

  public List<UserDetail> getUsersToImport() {
    return usersToImport;
  }

  public Map<String, String> getQueryToImport() {
    return queryToImport;
  }

  public UserFull getUser(String specificId) {
    return mAdminCtrl.getUserFull(targetDomainId, specificId);
  }

  public void synchroUser(String idUser) throws JobDomainPeasException {

    String idRet = mAdminCtrl.synchronizeUser(idUser);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnAdding("synchronize user", idUser));
    }
    refresh();
    setTargetUser(idRet);
  }

  public void unsynchroUser(String idUser) throws JobDomainPeasException {


    String idRet = mAdminCtrl.synchronizeRemoveUser(idUser);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnDeleting("synchronized user", idUser));
    }
    if (mTargetUserId.equals(idUser)) {
      mTargetUserId = null;
    }
    refresh();
  }

  /*
   * GROUP functions
   */
  public void returnIntoGroup(String groupId) throws JobDomainPeasException {
    if (!StringUtil.isDefined(groupId)) {
      mGroupsPath.clear();
    } else {
      int i = mGroupsPath.size() - 1;
      while (i >= 0 && !mGroupsPath.get(i).isThisGroup(groupId)) {
        mGroupsPath.remove(i);
        i--;
      }
    }
    setTargetUser(null);
  }

  public void removeGroupFromPath(String groupId) throws JobDomainPeasException {
    if (StringUtil.isDefined(groupId)) {
      int i = 0;
      while (i < mGroupsPath.size() && !mGroupsPath.get(i).isThisGroup(groupId)) {
        i++;
      }
      if (i < mGroupsPath.size()) {
        mGroupsPath = mGroupsPath.subList(0, i);
      }
    }
  }

  /**
   * @param groupId
   * @throws JobDomainPeasException
   */
  public void goIntoGroup(String groupId) throws JobDomainPeasException {
    if (StringUtil.isDefined(groupId)) {
      if (getTargetGroup() == null
          || (getTargetGroup() != null && !getTargetGroup().getId().equals(
              groupId))) {
        Group targetGroup = mAdminCtrl.getGroupById(groupId);
        // Add user access control for security purpose
        if (isUserAuthorizedToManageGroup(targetGroup)) {
          if (GroupNavigationStock.isGroupValid(targetGroup)) {
            List<String> manageableGroupIds = null;
            if (isOnlyGroupManager() && !isGroupManagerOnGroup(groupId)) {
              manageableGroupIds = getUserManageableGroupIds();
            }
            GroupNavigationStock newSubGroup = new GroupNavigationStock(groupId,
                mAdminCtrl, manageableGroupIds);
            mGroupsPath.add(newSubGroup);
          }
        } else {
          SilverLogger.getLogger(this)
              .warn("Security Alert: the user id {0} is attempting to access group id {1}",
                  getUserId(), groupId);
        }
      }
    } else {
      throw new JobDomainPeasException(undefined("group"));
    }
    setTargetUser(null);
  }

  private boolean isUserAuthorizedToManageGroup(Group group) {
    if (getUserDetail().isAccessAdmin() ||
        mAdminCtrl.isDomainManagerUser(getUserId(), group.getDomainId())) {
      return true;
    }

    // check if current user is manager of this group or one of its descendants
    Group[] groups = new Group[1];
    groups[0] = group;
    Group[] allowedGroups = filterGroupsToGroupManager(groups);
    if (!ArrayUtil.isEmpty(allowedGroups)) {
      return true;
    }

    // check if current user is manager of at least one parent group
    return isGroupManagerOnGroup(group.getId());
  }

  public Group getTargetGroup() throws JobDomainPeasException {
    if (mGroupsPath.isEmpty()) {
      return null;
    }
    return mGroupsPath.get(mGroupsPath.size()-1).getThisGroup();
  }

  /**
   * @return a List with 2 elements. First one, a List of UserDetail. Last one, a List of Group.
   * @throws JobDomainPeasException
   */
  public List<List> getGroupManagers() throws JobDomainPeasException {
    List<List> usersAndGroups = new ArrayList<List>();
    List<UserDetail> users = new ArrayList<UserDetail>();
    List<Group> groups = new ArrayList<Group>();

    GroupProfileInst profile = mAdminCtrl.getGroupProfile(getTargetGroup().getId());
    if (profile != null) {
      for (String groupId : profile.getAllGroups()) {
        groups.add(mAdminCtrl.getGroupById(groupId));
      }
      for (String userId : profile.getAllUsers()) {
        users.add(getUserDetail(userId));
      }
    }
    usersAndGroups.add(users);
    usersAndGroups.add(groups);
    return usersAndGroups;
  }

  // user panel de selection de n groupes et n users
  public void initUserPanelForGroupManagers(List<String> userIds,
      List<String> groupIds) throws SelectionException, JobDomainPeasException {
    sel.resetAll();
    sel.setHostSpaceName(getMultilang().getString("JDP.jobDomain"));
    sel.setHostComponentName(new Pair<>(getTargetGroup().getName(), null));
    LocalizationBundle generalMessage = ResourceLocator.getGeneralLocalizationBundle(getLanguage());
    Pair<String, String>[] hostPath =
        new Pair[]{new Pair<>(getMultilang().getString("JDP.roleManager") + " > " +
            generalMessage.getString("GML.selection"), null)};
    sel.setHostPath(hostPath);
    setDomainIdOnSelection(sel);
    sel.setPopupMode(true);
    sel.setHtmlFormElementId("roleItems");
    sel.setHtmlFormName("dummy");
    sel.setSelectedElements(userIds);
    sel.setSelectedSets(groupIds);
  }

  public void updateGroupProfile(List<String> userIds, List<String> groupIds)
      throws JobDomainPeasException {
    GroupProfileInst profile = mAdminCtrl.getGroupProfile(getTargetGroup().getId());
    profile.setUsers(userIds);
    profile.setGroups(groupIds);
    mAdminCtrl.updateGroupProfile(profile);
  }

  public boolean isGroupRoot(String groupId) throws JobDomainPeasException {
    Group gr = mAdminCtrl.getGroupById(groupId);
    return GroupNavigationStock.isGroupValid(gr) && this.refreshDomain && (!StringUtil.isDefined(gr.
        getSuperGroupId()) || "-1".equals(gr.getSuperGroupId()));
  }

  public Group[] getSubGroups(boolean isParentGroup)
      throws JobDomainPeasException {
    Group[] groups;

    if (isParentGroup) {
      if (mGroupsPath.isEmpty()) {
        throw new JobDomainPeasException(failureOnGetting("subgroups", ""));
      }
      groups = mGroupsPath.get(mGroupsPath.size()-1).getGroupPage();
    } else {
      // Domain case
      groups = mTargetDomain.getGroupPage();
    }
    if (isOnlyGroupManager() && !isGroupManagerOnCurrentGroup()) {
      groups = filterGroupsToGroupManager(groups);
    }
    return groups;
  }

  public List<UserDetail> getSubUsers(boolean isParentGroup) throws JobDomainPeasException {
    final UserDetail[] usDetails;
    if (isParentGroup) {
      if (mGroupsPath.isEmpty()) {
        throw new JobDomainPeasException(failureOnGetting("users of subgroups", ""));
      }
      usDetails = mGroupsPath.get(mGroupsPath.size()-1).getUserPage();
    } else {
      // Domain case
      usDetails = mTargetDomain.getUserPage();
    }
    setSessionUsers(Arrays.asList(usDetails));
    return getSessionUsers();
  }

  public String getPath(String baseURL, String toAppendAtEnd) throws JobDomainPeasException {
    StringBuilder strPath = new StringBuilder("");

    for (int i = 0; i < mGroupsPath.size(); i++) {
      Group theGroup = mGroupsPath.get(i).getThisGroup();
      appendSeparator(strPath);
      if ((i + 1) < mGroupsPath.size() || mTargetUserId != null || toAppendAtEnd != null) {
        strPath.append("<a href=\"").append(baseURL).append("groupReturn?Idgroup=").
            append(theGroup.getId()).append("\">").
            append(WebEncodeHelper.javaStringToHtmlString(theGroup.getName())).append("</a>");
      } else {
        strPath.append(WebEncodeHelper.javaStringToHtmlString(theGroup.getName()));
      }
    }
    if (mTargetUserId != null) {
      appendSeparator(strPath);
      if (toAppendAtEnd != null) {
        strPath.append("<a href=\"").append(baseURL).append("userContent?Iduser=").
            append(mTargetUserId).append("\">").
            append(WebEncodeHelper.javaStringToHtmlString(getTargetUserDetail().getDisplayedName())).
            append("</a>");
      } else {
        strPath.append(WebEncodeHelper
            .javaStringToHtmlString(getTargetUserDetail().getDisplayedName()));
      }
    }
    if (toAppendAtEnd != null) {
      appendSeparator(strPath);
      strPath.append(WebEncodeHelper.javaStringToHtmlString(toAppendAtEnd));
    }
    return strPath.toString();
  }

  private void appendSeparator(StringBuilder sb) {
    if (sb.length() > 0) {
      sb.append(" &gt ");
    }
  }

  public boolean createGroup(String idParent, String groupName,
      String groupDescription, String groupRule) throws JobDomainPeasException {
    GroupDetail theNewGroup = new GroupDetail();

    String rule = groupRule;
    boolean isSynchronizationToPerform = StringUtil.isDefined(rule);
    if (isSynchronizationToPerform) {
      rule = rule.trim();
    }

    theNewGroup.setId("-1");
    if (StringUtil.isDefined(targetDomainId)
        && !"-1".equals(targetDomainId)) {
      theNewGroup.setDomainId(targetDomainId);
    }
    theNewGroup.setSuperGroupId(idParent);
    theNewGroup.setName(groupName);
    theNewGroup.setDescription(groupDescription);
    theNewGroup.setRule(rule);
    String idRet = mAdminCtrl.addGroup(theNewGroup);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnAdding("group", groupName));
    }
    refresh();

    goIntoGroup(idRet);

    return isSynchronizationToPerform ? synchroGroup(idRet) : isGroupRoot(idRet);
  }

  public boolean modifyGroup(String idGroup, String groupName,
      String groupDescription, String groupRule) throws JobDomainPeasException {

    GroupDetail theModifiedGroup = mAdminCtrl.getGroupById(idGroup);
    if (theModifiedGroup == null) {
      throw new JobDomainPeasException(unknown("group", idGroup));
    }
    String rule = groupRule;
    if (StringUtil.isDefined(groupRule)) {
      rule = groupRule.trim();
    }
    boolean isSynchronizationToPerform =
        StringUtil.isDefined(rule) && !rule.equalsIgnoreCase(theModifiedGroup.getRule());
    theModifiedGroup.setName(groupName);
    theModifiedGroup.setDescription(groupDescription);
    theModifiedGroup.setRule(rule);

    String idRet = mAdminCtrl.updateGroup(theModifiedGroup);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnUpdate("group", idGroup));
    }
    refresh();
    return isSynchronizationToPerform ? synchroGroup(idRet) : isGroupRoot(idRet);
  }

  public boolean updateGroupSubUsers(String idGroup, String[] userIds)
      throws JobDomainPeasException {


    GroupDetail theModifiedGroup = mAdminCtrl.getGroupById(idGroup);
    if (theModifiedGroup == null) {
      throw new JobDomainPeasException(unknown("group", idGroup));
    }
    theModifiedGroup.setUserIds(userIds);
    String idRet = mAdminCtrl.updateGroup(theModifiedGroup);
    if ((idRet == null) || (idRet.length() <= 0)) {
      throw new JobDomainPeasException(failureOnUpdate("group", idGroup));
    }
    refresh();
    return true;
  }

  public boolean deleteGroup(String idGroup) throws JobDomainPeasException {


    String idRet = mAdminCtrl.deleteGroupById(idGroup);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnDeleting("group", idGroup));
    }
    removeGroupFromPath(idGroup);
    refresh();
    return true;
  }

  public boolean synchroGroup(String idGroup) throws JobDomainPeasException {

    String synchronizationResult = mAdminCtrl.synchronizeGroup(idGroup);
    if (!StringUtil.isDefined(synchronizationResult)) {
      throw new JobDomainPeasException(failureOnAdding("synchronized group", idGroup));
    }
    if (StringUtil.isLong(synchronizationResult)) {
      refresh();
      return true;
    }
    if (synchronizationResult.startsWith("expression.")) {
      if (synchronizationResult.startsWith("expression.groundrule.unknown")) {
        final String[] keyRule = synchronizationResult.split("[|]");
        String msgKey = keyRule[0];
        String groundRule = "<b>" + keyRule[1] + "</b>";
        MessageNotifier.addError(
            MessageFormat.format(getString("JDP.groupSynchroRule." + msgKey), groundRule));
      } else {
        MessageNotifier.addError(getString("JDP.groupSynchroRule." + synchronizationResult));
      }
    } else {
      MessageNotifier.addError(synchronizationResult);
    }
    return false;
  }

  public boolean unsynchroGroup(String idGroup) throws JobDomainPeasException {


    String idRet = mAdminCtrl.synchronizeRemoveGroup(idGroup);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnDeleting("synchronized group", idGroup));
    }
    removeGroupFromPath(idGroup);
    refresh();
    return true;
  }

  public boolean importGroup(String groupName) throws JobDomainPeasException {


    String idRet = mAdminCtrl.synchronizeImportGroup(targetDomainId,
        groupName);
    if (!StringUtil.isDefined(idRet)) {
      throw new JobDomainPeasException(failureOnAdding("synchronized group", groupName));
    }
    refresh();
    return true;
  }

  /*
   * DOMAIN functions
   */
  public void setDefaultTargetDomain() {
    UserDetail ud = getUserDetail();

    if (ud.isDomainAdminRestricted()) {
      setTargetDomain(ud.getDomainId());
    }
  }

  public void setTargetDomain(String domainId) {
    if (!StringUtil.isDefined(domainId)) {
      mTargetDomain = null;
      targetDomainId = "";
    } else {
      List<String> manageableGroupIds = null;
      if (isOnlyGroupManager()) {
        manageableGroupIds = getUserManageableGroupIds();
      }
      mTargetDomain = new DomainNavigationStock(domainId, mAdminCtrl, manageableGroupIds);
      targetDomainId = domainId;
    }
  }

  public Domain getTargetDomain() {
    if (mTargetDomain == null) {
      return null;
    }
    return mTargetDomain.getThisDomain();
  }

  public long getDomainActions() {
    if (targetDomainId.length() > 0) {
      return mAdminCtrl.getDomainActions(targetDomainId);
    }
    return 0;
  }

  public List<Domain> getAllDomains() {
    List<Domain> domains = new ArrayList<Domain>();
    UserDetail ud = getUserDetail();

    if (ud.isAccessDomainManager()) {
      // return only domain of user
      domains.add(mAdminCtrl.getDomain(ud.getDomainId()));
    } else if (ud.isAccessAdmin()) {
      // return mixed domain...
      domains.add(mAdminCtrl.getDomain(Domain.MIXED_DOMAIN_ID));

      // and all classic domains
      domains.addAll(Arrays.asList(mAdminCtrl.getAllDomains()));
    } else if (isCommunityManager()) {
      // return mixed domain...
      domains.add(mAdminCtrl.getDomain(Domain.MIXED_DOMAIN_ID));

      // domain of user...
      domains.add(mAdminCtrl.getDomain(ud.getDomainId()));

      // and default domain
      domains.add(mAdminCtrl.getDomain("0"));
    } else if (isOnlyGroupManager()) {
      // return mixed domain...
      domains.add(mAdminCtrl.getDomain(Domain.MIXED_DOMAIN_ID));

      // and domain of user
      domains.add(mAdminCtrl.getDomain(ud.getDomainId()));
    }
    return domains;
  }

  public boolean isOnlyGroupManager() {
    return isGroupManager() && !getUserDetail().isAccessAdmin()
        && !getUserDetail().isAccessDomainManager();
  }

  public boolean isCommunityManager() {
    if (!JobDomainSettings.m_UseCommunityManagement) {
      return false;
    }

    // check if user is able to manage at least one space and its corresponding group
    List<Group> groups = getUserManageableGroups();
    List<String> spaceIds = Arrays.asList(getUserManageableSpaceIds());
    for (String spaceId : spaceIds) {
      SpaceInstLight space = getOrganisationController().getSpaceInstLightById(spaceId);
      for (Group group : groups) {
        if (space.getName().equalsIgnoreCase(group.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isGroupManagerOnCurrentGroup() throws JobDomainPeasException {
    return getTargetGroup() != null && isGroupManagerOnGroup(getTargetGroup().getId());
  }

  public boolean isGroupManagerOnGroup(String groupId) {
    List<String> manageableGroupIds = getUserManageableGroupIds();
    if (manageableGroupIds.contains(groupId)) {
      // Current user is directly manager of group
      return true;
    } else {
      List<String> groupPath = mAdminCtrl.getPathToGroup(groupId);

      groupPath.retainAll(manageableGroupIds);

      if (!groupPath.isEmpty()) {
        // Current user is at least manager of one super group of group
        return true;
      }
    }
    return false;
  }

  public boolean isGroupManagerDirectlyOnCurrentGroup()
      throws JobDomainPeasException {
    List<String> manageableGroupIds = getUserManageableGroupIds();
    return manageableGroupIds.contains(getTargetGroup().getId());
  }

  public Group[] getAllRootGroups() {
    if (targetDomainId.length() <= 0) {
      return new Group[0];
    }
    Group[] selGroupsArray = mTargetDomain.getAllGroupPage();

    if (isOnlyGroupManager()) {
      selGroupsArray = filterGroupsToGroupManager(selGroupsArray);
    }
    JobDomainSettings.sortGroups(selGroupsArray);
    return selGroupsArray;
  }

  private Group[] filterGroupsToGroupManager(Group[] groups) {
    // get all manageable groups by current user
    List<String> manageableGroupIds = getUserManageableGroupIds();
    List<Group> temp = new ArrayList<Group>();
    // filter groups
    for (Group group : groups) {
      if (manageableGroupIds.contains(group.getId())) {
        temp.add(group);
      } else {
        // get all subGroups of group
        List<String> subGroupIds = Arrays.asList(mAdminCtrl.getAllSubGroupIdsRecursively(group.
            getId()));
        // check if at least one manageable group is part of subGroupIds
        Iterator<String> itManageableGroupsIds = manageableGroupIds.iterator();

        String manageableGroupId;
        boolean find = false;
        while (!find && itManageableGroupsIds.hasNext()) {
          manageableGroupId = itManageableGroupsIds.next();
          if (subGroupIds.contains(manageableGroupId)) {
            find = true;
          }
        }

        if (find) {
          temp.add(group);
        }
      }
    }
    return temp.toArray(new Group[temp.size()]);
  }

  public String createDomain(String domainName, String domainDescription, String domainDriver,
      String domainProperties, String domainAuthentication, String silverpeasServerURL,
      String domainTimeStamp) throws JobDomainPeasException, JobDomainPeasTrappedException {



    String newDomainId = null;

    try {
      Domain theNewDomain = new Domain();
      theNewDomain.setId("-1");
      theNewDomain.setName(domainName);
      theNewDomain.setDescription(domainDescription);
      theNewDomain.setDriverClassName(domainDriver);
      theNewDomain.setPropFileName(domainProperties);
      theNewDomain.setAuthenticationServer(domainAuthentication);
      theNewDomain.setSilverpeasServerURL(silverpeasServerURL);
      theNewDomain.setTheTimeStamp(domainTimeStamp);

      DomainServiceProvider.getDomainService(DomainType.EXTERNAL).createDomain(theNewDomain);
      refresh();
    } catch (DomainCreationException e) {
      throw new JobDomainPeasException(e);
    } catch (DomainConflictException e) {
      JobDomainPeasTrappedException trappedException = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.createDomain()",
          SilverpeasException.ERROR, "admin.MSG_ERR_DOMAIN_ALREADY_EXIST_DATABASE", e);
      trappedException.setGoBackPage("displayDomainCreate");
      throw trappedException;
    }

    return newDomainId;
  }

  public String createSQLDomain(String domainName, String domainDescription,
      String silverpeasServerURL, String usersInDomainQuotaMaxCount) throws JobDomainPeasException,
      JobDomainPeasTrappedException {

    // build Domain object
    Domain domainToCreate = new Domain();
    domainToCreate.setName(domainName);
    domainToCreate.setDescription(domainDescription);
    domainToCreate.setSilverpeasServerURL(silverpeasServerURL);

    // launch domain creation process
    String domainId;
    try {

      // Getting quota filled
      if (JobDomainSettings.usersInDomainQuotaActivated) {
        domainToCreate.setUserDomainQuotaMaxCount(usersInDomainQuotaMaxCount);
      }

      domainId = DomainServiceProvider.getDomainService(DomainType.SQL).createDomain(domainToCreate);
      domainToCreate.setId(domainId);

      if (JobDomainSettings.usersInDomainQuotaActivated) {
        // Registering "users in domain" quota
        DomainServiceProvider.getUserDomainQuotaService().initialize(
            UserDomainQuotaKey.from(domainToCreate),
            domainToCreate.getUserDomainQuota().getMaxCount());
      }

    } catch (QuotaException qe) {
      JobDomainPeasTrappedException trappedException = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.createSQLDomain()",
          SilverpeasException.ERROR, "admin.MSG_ERR_ADD_DOMAIN",
          getString("JDP.userDomainQuotaMaxCountError"), qe);
      trappedException.setGoBackPage("displayDomainSQLCreate");
      throw trappedException;
    } catch (DomainCreationException e) {
      throw new JobDomainPeasException(e);
    } catch (DomainConflictException e) {
      JobDomainPeasTrappedException trappedException = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.createSQLDomain()",
          SilverpeasException.ERROR, "admin.MSG_ERR_DOMAIN_ALREADY_EXIST", e);
      trappedException.setGoBackPage("displayDomainSQLCreate");
      throw trappedException;
    }

    return domainId;
  }

  public String modifyDomain(String domainName, String domainDescription,
      String domainDriver, String domainProperties,
      String domainAuthentication, String silverpeasServerURL,
      String domainTimeStamp) throws JobDomainPeasException,
      JobDomainPeasTrappedException {
    Domain theNewDomain = getTargetDomain();

    // Vérif domainName unique dans la table ST_Domain
    JobDomainPeasTrappedException trappedException = new JobDomainPeasTrappedException(
        "JobDomainPeasSessionController", SilverpeasException.WARNING,
        "jobDomainPeas.WARN_DOMAIN_SQL_NAME");
    trappedException.setGoBackPage("domainContent");
    Domain[] tabDomain = mAdminCtrl.getAllDomains();
    Domain domain;
    for (Domain aTabDomain : tabDomain) {
      domain = aTabDomain;
      if (!domain.getId().equals(theNewDomain.getId())
          && domain.getName().equalsIgnoreCase(domainName)) {
        throw trappedException;
      }
    }

    if (!StringUtil.isDefined(targetDomainId) || targetDomainId.equals(Domain.MIXED_DOMAIN_ID)) {
      throw new JobDomainPeasException(unknown("domain", domainName));
    }
    theNewDomain.setName(domainName);
    theNewDomain.setDescription(domainDescription);
    theNewDomain.setDriverClassName(domainDriver);
    theNewDomain.setPropFileName(domainProperties);
    theNewDomain.setAuthenticationServer(domainAuthentication);
    theNewDomain.setSilverpeasServerURL(silverpeasServerURL);
    theNewDomain.setTheTimeStamp(domainTimeStamp);
    String idRet = mAdminCtrl.updateDomain(theNewDomain);
    if ((idRet == null) || (idRet.length() <= 0)) {
      throw new JobDomainPeasException(failureOnUpdate("domain", domainName));
    }
    refresh();
    return idRet;
  }

  public String modifySQLDomain(String domainName, String domainDescription,
      String silverpeasServerURL, String usersInDomainQuotaMaxCount) throws JobDomainPeasException,
      JobDomainPeasTrappedException {
    Domain theNewDomain = getTargetDomain();

    // Vérif domainName unique dans la table ST_Domain
    JobDomainPeasTrappedException trappedException = new JobDomainPeasTrappedException(
        "JobDomainPeasSessionController", SilverpeasException.WARNING,
        "jobDomainPeas.WARN_DOMAIN_SQL_NAME");
    trappedException.setGoBackPage("domainContent");
    Domain[] tabDomain = mAdminCtrl.getAllDomains();
    Domain domain;
    for (Domain aTabDomain : tabDomain) {
      domain = aTabDomain;
      if (!domain.getId().equals(theNewDomain.getId())
          && domain.getName().equalsIgnoreCase(domainName)) {
        throw trappedException;
      }
    }

    String idRet;

    if (StringUtil.isNotDefined(targetDomainId) || targetDomainId.equals(Domain.MIXED_DOMAIN_ID)
        || "0".equals(targetDomainId)) {
      throw new JobDomainPeasException(unknown("domain", domainName));
    }
    theNewDomain.setName(domainName);
    theNewDomain.setDescription(domainDescription);
    theNewDomain.setSilverpeasServerURL(silverpeasServerURL);

    try {

      if (JobDomainSettings.usersInDomainQuotaActivated) {
        // Getting quota filled
        theNewDomain.setUserDomainQuotaMaxCount(usersInDomainQuotaMaxCount);
      }

      idRet = mAdminCtrl.updateDomain(theNewDomain);
      if (StringUtil.isNotDefined(idRet)) {
        throw new JobDomainPeasException(failureOnUpdate("domain", domainName));
      }

      if (JobDomainSettings.usersInDomainQuotaActivated) {
        // Registering "users in domain" quota
        DomainServiceProvider.getUserDomainQuotaService().initialize(
            UserDomainQuotaKey.from(theNewDomain), theNewDomain.getUserDomainQuota().getMaxCount());
      }

    } catch (QuotaException qe) {
      trappedException = new JobDomainPeasTrappedException(
          "JobDomainPeasSessionController.modifySQLDomain()",
          SilverpeasException.ERROR, "admin.EX_ERR_UPDATE_DOMAIN",
          getString("JDP.userDomainQuotaMaxCountError"), qe);
      trappedException.setGoBackPage("displayDomainSQLCreate");
      throw trappedException;
    }

    refresh();
    return idRet;
  }

  public void deleteDomain() throws JobDomainPeasException {
    try {
      DomainServiceProvider.getDomainService(DomainType.EXTERNAL).deleteDomain(getTargetDomain());
    } catch (DomainDeletionException e) {
      throw new JobDomainPeasException(e);
    }
  }

  public void deleteSQLDomain() throws JobDomainPeasException {
    try {
      DomainServiceProvider.getDomainService(DomainType.SQL).deleteDomain(getTargetDomain());
      DomainServiceProvider.getUserDomainQuotaService().remove(
          UserDomainQuotaKey.from(getTargetDomain()));
    } catch (DomainDeletionException e) {
      throw new JobDomainPeasException(e);
    }
  }

  public void refresh() {
    if (mTargetDomain != null) {
      mTargetDomain.refresh();
    }
    for (GroupNavigationStock aM_GroupsPath : mGroupsPath) {
      aM_GroupsPath.refresh();
    }
    setTargetUser(null);
  }

  /*
   * Selection Peas functions
   */
  public String initSelectionPeasForGroups(String compoURL) throws JobDomainPeasException {
    String hostSpaceName = getString("JDP.userPanelGroup");
    Pair<String, String> hostComponentName = new Pair<>(getTargetGroup().getName(),
        compoURL + "groupContent");
    Pair<String, String>[] hostPath = new Pair[0];
    String hostUrl = compoURL + "groupAddRemoveUsers";
    String cancelUrl = compoURL + "groupContent";

    Selection selection = getSelection();
    selection.resetAll();
    selection.setFilterOnDeactivatedState(false);
    selection.setHostSpaceName(hostSpaceName);
    selection.setHostPath(hostPath);
    selection.setHostComponentName(hostComponentName);

    selection.setGoBackURL(hostUrl);
    selection.setCancelURL(cancelUrl);

    setDomainIdOnSelection(selection);

    selection.setSelectedElements(SelectionUsersGroups.getUserIds(mGroupsPath.get(mGroupsPath.size()-1).
        getAllUserPage()));

    // Contraintes
    selection.setSetSelectable(false);
    selection.setPopupMode(false);
    return Selection.getSelectionURL();
  }

  private void setDomainIdOnSelection(Selection selection) {
    if (StringUtil.isDefined(targetDomainId) && !Domain.MIXED_DOMAIN_ID.equals(targetDomainId)) {
      // Add extra params
      SelectionUsersGroups sug = new SelectionUsersGroups();
      sug.setDomainId(targetDomainId);
      selection.setExtraParams(sug);
    }
  }

  /*
   * Appel UserPannel pour récup du user sélectionné :
   */
  public String[] getSelectedUsersIds() {
    return getSelection().getSelectedElements();
  }

  // Throws Specific Exception
  public String initSelectionPeasForOneGroupOrUser(String compoURL)
      throws JobDomainPeasException {
    String hostSpaceName = getString("JDP.userPanelDomain");
    Pair<String, String> hostComponentName = new Pair<>(getTargetDomain().getName(),
        compoURL + "domainContent");
    Pair<String, String>[] hostPath = new Pair[0];
    String hostUrl = compoURL + "selectUserOrGroup";
    String cancelUrl = compoURL + "domainContent";

    Selection selection = getSelection();
    selection.resetAll();
    selection.setFilterOnDeactivatedState(false);
    selection.setHostSpaceName(hostSpaceName);
    selection.setHostPath(hostPath);
    selection.setHostComponentName(hostComponentName);

    selection.setGoBackURL(hostUrl);
    selection.setCancelURL(cancelUrl);

    if (!StringUtil.isDefined(targetDomainId) || "-1".equals(targetDomainId)) {
      selection.setElementSelectable(false);
    }

    // Add extra params
    SelectionUsersGroups sug = new SelectionUsersGroups();
    sug.setDomainId(targetDomainId);
    selection.setExtraParams(sug);

    // Contraintes
    selection.setMultiSelect(false);
    selection.setPopupMode(false);
    return Selection.getSelectionURL();
  }

  public String getSelectedUserId() {
    return getSelection().getFirstSelectedElement();
  }

  public String getSelectedGroupId() {
    return getSelection().getFirstSelectedSet();
  }

  // Synchro Management
  // ------------------
  public void synchroSQLDomain() {
    if (mThethread == null) {
      SynchroDomainReport.setReportLevel(Level.INFO);
      SynchroDomainReport.waitForStart();
      mThethread = new SynchroWebServiceThread(this);
      mErrorOccured = null;
      mSynchroReport = "";
      mThethread.startTheThread();
    }
  }

  protected String synchronizeSilverpeasViaWebService() {
    StringBuilder sReport = new StringBuilder();
    SynchroUserWebServiceItf synchroUserWebService = null;
    try {
      sReport.append("Démarrage de la synchronisation...\n\n");
      // Démarrage de la synchro avec la Popup d'affichage
      SynchroDomainReport.startSynchro();
      Domain theDomain = getTargetDomain();

      SynchroDomainReport.warn("jobDomainPeas.synchronizeSilverpeasViaWebService",
          "Domaine : " + theDomain.getName() + " (id : " + theDomain.getId() + ")");

      // 1- Récupère la liste des groupes à synchroniser (en insert et update)
      Collection<Group> listGroupToInsertUpdate =
          JobDomainPeasDAO.selectGroupSynchroInsertUpdateTableDomain_Group(theDomain);

      // 2- Traitement Domaine, appel aux webServices
      SettingBundle propDomainSql = theDomain.getSettings();
      String nomClasseWebService = propDomainSql.getString("ExternalSynchroClass");
        synchroUserWebService = (SynchroUserWebServiceItf) Class.forName(nomClasseWebService).
            newInstance();

      synchroUserWebService.startConnection();

      // Insertion / Update de la société
      sReport.append(synchroUserWebService.insertUpdateDomainWebService(theDomain.getId(),
          theDomain.getName()));

      // 3- Traitement groupes, appel aux webServices
      if (listGroupToInsertUpdate != null && !listGroupToInsertUpdate.isEmpty()) {
        // Insertion / Update des groupes
        sReport.append(synchroUserWebService.insertUpdateListGroupWebService(theDomain.getId(),
            theDomain.getName(), listGroupToInsertUpdate));
      }

      // 4- Récupère la liste des users à synchroniser (en insert et update)
      Collection<UserFull> listUserToInsertUpdate =
          JobDomainPeasDAO.selectUserSynchroInsertUpdateTableDomain_User(theDomain);

      // 5- Récupère la liste des users à synchroniser (en delete)
      Collection<UserDetail> listUserToDelete =
          JobDomainPeasDAO.selectUserSynchroDeleteTableDomain_User(theDomain);

      // 6-Traitement users, appel aux webServices
      if (listUserToDelete != null && !listUserToDelete.isEmpty()) {
        // Suppression des users
        sReport.append(synchroUserWebService.deleteListUserWebService(theDomain.getId(),
            listUserToDelete));
      }

      // Insertion / Update des users
      if (listUserToInsertUpdate != null && !listUserToInsertUpdate.isEmpty()) {
        sReport.append(synchroUserWebService.insertUpdateListUserWebService(theDomain.getId(),
            listUserToInsertUpdate, listGroupToInsertUpdate));
      }

      sReport.append("\n\nFin de la synchronisation...");

    } catch (Exception e) {
      SilverLogger.getLogger(this).error(e.getMessage(), e);
      SynchroDomainReport.error(
          "JobDomainPeasSessionController.synchronizeSilverpeasViaWebService",
          "Problème lors de la synchronisation : " + e.getMessage(), null);
      sReport.append("Erreurs lors de la synchronisation : \n").append(e.getMessage());
    } finally {
      // Fin de synchro avec la Popup d'affichage
      SynchroDomainReport.stopSynchro();
      if (synchroUserWebService != null) {
        synchroUserWebService.endConnection();
      }
    }
    return sReport.toString();
  }

  public void synchroDomain(Level level) {
    if (mThethread == null) {
      SynchroDomainReport.setReportLevel(level);
      SynchroDomainReport.waitForStart();
      mThethread = new SynchroLdapThread(this, mAdminCtrl, targetDomainId);
      mErrorOccured = null;
      mSynchroReport = "";
      mThethread.startTheThread();
    }
  }

  public boolean isEnCours() {
    return mThethread != null && mThethread.isEnCours();
  }

  public String getSynchroReport() {
    if (mErrorOccured != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);

      mErrorOccured.printStackTrace(pw);
      return mErrorOccured.toString() + "\n" + sw.getBuffer().toString();
    }
    return mSynchroReport;
  }

  public void threadFinished() {
    mErrorOccured = mThethread.getErrorOccured();
    mSynchroReport = mThethread.getSynchroReport();
    mThethread = null;
  }

  public void getP12(String userId) throws JobDomainPeasException {
    UserDetail user = getUserDetail(userId);

    try {
      X509Factory.buildP12(user.getId(), user.getLogin(), user.getLastName(), user.getFirstName(),
          user.getDomainId());
    } catch (UtilException e) {
      throw new JobDomainPeasException(e);
    }
  }

  private void revocateCertificate(String userId) throws JobDomainPeasException {
    try {
      X509Factory.revocateUserCertificate(userId);
    } catch (UtilException e) {
      throw new JobDomainPeasException(e);
    }
  }

  /**
   * PAGINATION *
   */
  /**
   * Get list of selected users Ids
   */
  public List<String> getListSelectedUsers() {
    return listSelectedUsers;
  }

  public void clearListSelectedUsers() {
    listSelectedUsers.clear();
  }

  public void setListSelectedUsers(List<String> list) {
    listSelectedUsers = list;
  }

  public void setIndexOfFirstItemToDisplay(String index) {
    this.indexOfFirstItemToDisplay = Integer.parseInt(index);
  }

  public int getIndexOfFirstItemToDisplay() {
    return indexOfFirstItemToDisplay;
  }

  public List<Group> getUserManageableGroups() {
    List<String> groupIds = getUserManageableGroupIds();
    Group[] aGroups = getOrganisationController().getGroups(groupIds.toArray(new String[groupIds.
        size()]));
    return Arrays.asList(aGroups);
  }

  public UserDetail checkUser(UserDetail userToCheck) {
    UserDetail[] existingUsers = mTargetDomain.getAllUserPage();
    for (UserDetail existingUser : existingUsers) {
      if (userToCheck.getLastName().equalsIgnoreCase(existingUser.getLastName())
          && userToCheck.getFirstName().equalsIgnoreCase(existingUser.getFirstName())
          && userToCheck.geteMail().equalsIgnoreCase(existingUser.geteMail())) {
        return existingUser;
      }
    }
    return null;
  }

  /**
   * @return true if community management is activated and target user belongs to one group
   * manageable by current user
   */
  public boolean isUserInAtLeastOneGroupManageableByCurrentUser() {
    if (!JobDomainSettings.m_UseCommunityManagement) {
      return false;
    }
    List<String> groupIds = getUserManageableGroupIds();
    for (String groupId : groupIds) {
      UserDetail[] users = getOrganisationController().getAllUsersOfGroup(groupId);
      UserDetail user = getUser(mTargetUserId, users);

      if (user != null) {
        return true;
      }
    }
    return false;
  }

  private UserDetail getUser(String userId, UserDetail[] users) {
    for (UserDetail userDetail : users) {
      if (userId.equals(userDetail.getId())) {
        return userDetail;
      }
    }
    return null;
  }

  private List<UserDetail> getSessionUsers() {
    return sessionUsers;
  }

  private void setSessionUsers(List<UserDetail> users) {
    sessionUsers = users;
  }

  public ListIndex getIndex() {
    return currentIndex;
  }

  private void processIndex(String userId) {
    UserDetail user = UserDetail.getById(userId);
    currentIndex.setCurrentIndex(getSessionUsers().indexOf(user));
    currentIndex.setNbItems(getSessionUsers().size());
  }

  public UserDetail getPrevious() {
    return getSessionUsers().get(currentIndex.getPreviousIndex());
  }

  public UserDetail getNext() {
    return getSessionUsers().get(currentIndex.getNextIndex());
  }

  private String updateUserFull(UserFull user, Map<String, String> properties)
      throws JobDomainPeasException {
    // process extra properties
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      user.setValue(entry.getKey(), entry.getValue());
    }

    try {
      return mAdminCtrl.updateUserFull(user);
    } catch (AdminException e) {
      throw new JobDomainPeasException(failureOnUpdate("user", user.getId()), e);
    }
  }

}
