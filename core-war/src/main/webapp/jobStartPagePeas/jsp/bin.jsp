<%@ page import="org.silverpeas.core.admin.component.model.ComponentInstLight" %>
<%@ page import="org.silverpeas.core.admin.space.SpaceInstLight" %>
<%@ page import="org.silverpeas.core.util.EncodeHelper" %><%--

    Copyright (C) 2000 - 2013 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have received a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>

<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://www.silverpeas.com/tld/viewGenerator" prefix="view"%>
<%@ include file="check.jsp" %>

<%
List<SpaceInstLight> removedSpaces 		= (List<SpaceInstLight>) request.getAttribute("Spaces");
List<ComponentInstLight> removedComponents 	= (List<ComponentInstLight>) request.getAttribute("Components");

boolean emptyBin = false;
if ((removedSpaces == null || removedSpaces.isEmpty()) && (removedComponents == null || removedComponents.isEmpty())) {
  emptyBin = true;
}

if (!emptyBin) {
  operationPane.addOperation(resource.getIcon("JSPP.restoreAll"),resource.getString("JSPP.BinRestore"),"javascript:onClick=restore()");
  operationPane.addOperation(resource.getIcon("JSPP.deleteAll"),resource.getString("JSPP.BinDelete"),"javascript:onClick=remove()");
  operationPane.addOperation(null , "Vider la corbeille", "javascript:onclick=emptyBin()");
}

browseBar.setComponentName(resource.getString("JSPP.Bin"));
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<view:looknfeel withCheckFormScript="true"/>
<view:includePlugin name="qtip"/>
<view:includePlugin name="popup"/>
<script type="text/javascript">
<!--
function removeItem(id) {
  jQuery.popup.confirm("<%=resource.getString("JSPP.BinDeleteConfirm")%>", function() {
    var url = webContext+"/services/bin/delete?";
    if (id.startsWith("WA")) {
      url += "spaceIds="+id;
    } else {
      url += "appIds="+id;
    }
    $.ajax({
      url: url,
      type: "DELETE",
      contentType: "application/json",
      dataType: "json",
      cache: false,
      success: function(data) {
        processResponse(data);
      },
      error: function(data) {
        // do something here
        notyError(data);
      }
    });
    return true;
  });
}

function processResponse(data) {
  //Start by process deletion without errors
  var onceApp = (data.appIdsOK.length == 1);
  var onceSpace = (data.spaceIdsOK.length == 1);
  var uniqueDeletion = false;
  if ((onceApp && !onceSpace) || (!onceApp && onceSpace)) {
    var id;
    if (onceApp) {
      id = data.appIdsOK[0];
    } else {
      id = data.spaceIdsOK[0];
    }
    $("#"+id).remove();
    uniqueDeletion = true;
  } else {
    data.appIdsOK.forEach(function(appId) {
      $("#"+appId).remove();
    });
    data.spaceIdsOK.forEach(function(spaceId) {
      $("#"+spaceId).remove();
    });
  }

  //Then process errors
  var errorLabel = "";
  data.appIdsNOK.forEach(function(appId) {
    errorLabel += " - "+$("#"+appId+">a").text()+"<br/>";
  });
  data.spaceIdsNOK.forEach(function(spaceId) {
    errorLabel += " - "+$("#"+spaceId+">a").text()+"<br/>";
  });
  if (errorLabel.isDefined()) {
    errorLabel = "Les éléments suivants n'ont pas pu être supprimés : <br/>" + errorLabel;
    notyError(errorLabel);
  } else if (uniqueDeletion) {
    notySuccess("L'élément a bien été supprimé définitivement...")
  } else {
    notySuccess("Tous les éléments ont bien été supprimés définitivement...");
  }

  var spacesInBin = $('#binContentSpaces>tbody>tr').length > 0;
  var appsInBin =  $('#binContentComponents>tbody>tr').length > 0;
  /*if (!spacesInBin) {
    $('#binContentSpaces').remove();
  }
  if (!appsInBin) {
    $('#binContentComponents').remove();
  }*/
  if (!spacesInBin && !appsInBin) {
    $("#binForm").remove();
    $(".cellOperation").remove();
    $(".inlineMessage").css("display", "block");
  }
}

function remove() {
  jQuery.popup.confirm("<%=resource.getString("JSPP.BinDeleteConfirmSelected")%>", function() {
    var url = webContext+"/services/bin/delete?";
    var spaceIds = [];
    $('input:checked[name=SpaceIds]').each(function() {
      spaceIds.push($(this).val());
    });
    url += "spaceIds="+spaceIds;
    var appIds = [];
    $('input:checked[name=ComponentIds]').each(function() {
      appIds.push($(this).val());
    });
    url += "&appIds="+appIds;
    $.ajax({
      url: url,
      type: "DELETE",
      contentType: "application/json",
      dataType: "json",
      cache: false,
      success: function(data) {
        processResponse(data);
      },
      error: function(data) {
        // do something here
        notyError(data);
      }
    });
    return true;
  });
}

function emptyBin() {
  jQuery.popup.confirm("<%=resource.getString("JSPP.BinDeleteConfirmSelected")%>", function() {
    var url = webContext+"/services/bin/empty";
    $.ajax({
      url: url,
      type: "POST",
      contentType: "application/json",
      dataType: "json",
      cache: false,
      success: function(data) {
        processResponse(data);
      },
      error: function(data) {
        // do something here
        notyError(data);
      }
    });
    return true;
  });
}

function restore() {
  jQuery.popup.confirm("<%=resource.getString("JSPP.BinRestoreSelected")%>", function() {
    window.document.binForm.action = "RestoreFromBin";
    window.document.binForm.submit();
  });
}

function jqCheckAll2(id, name)
{
   $("input[name='" + name + "'][type='checkbox']").attr('checked', $('#' + id).is(':checked'));
}

$(document).ready(function() {
  // By supplying no content attribute, the library uses each elements title attribute by default
  $('.item-path').qtip({
    content : {
      text : false,
      title : {
        text : "<%=resource.getString("GML.path")%>"
      }
    },
    style : {
      tip : true,
      classes : "qtip-shadow qtip-green"
    },
    position : {
      adjust : {
        method : "flip flip"
      },
      at : "bottom center",
      my : "top left",
      viewport : $(window)
    }
  });
});
-->
</script>
  <style type="text/css">
    <%if (!emptyBin) { %>
      .inlineMessage {
        display: none;
      }
    <% } %>
  </style>
</head>
<body>
<%
out.println(window.printBefore());
%>
<view:frame>
<form name="binForm" id="binForm" action="" method="post">
<%
  //Array of deleted spaces
  if (removedSpaces != null && !removedSpaces.isEmpty()) {

    ArrayPane arrayPane = gef.getArrayPane("binContentSpaces", "ViewBin", request, session);
    arrayPane.addArrayColumn(resource.getString("GML.space"));
    arrayPane.addArrayColumn(resource.getString("JSPP.BinRemoveDate"));
    ArrayColumn columnOp = arrayPane.addArrayColumn("<span style=\"float:left\">"+resource.getString("GML.operation")+"</span> <input type=\"checkbox\" id=\"checkAllSpaces\" onclick=\"jqCheckAll2(this.id, 'SpaceIds')\" style=\"float:left;margin:0px;margin-left:5px;padding:0px;vertical-align:middle;background-color:none;\"/>");
    columnOp.setSortable(false);

		for (SpaceInstLight space : removedSpaces) {
      ArrayLine line = arrayPane.addArrayLine();
      line.setId(space.getId());
      ArrayCellText cellLabel = null;
			if (space.isRoot())
				cellLabel = line.addArrayCellText(space.getName());
			else
				cellLabel = line.addArrayCellText("<a href=\"#\" class=\"item-path\" title=\""+
            EncodeHelper.javaStringToJsString(space.getPath(" > "))+"\"/>"+EncodeHelper.javaStringToHtmlString(space.getName())+"</a>");
			cellLabel.setCompareOn(space.getName());
			ArrayCellText cell = line.addArrayCellText(resource.getOutputDateAndHour(space.getRemoveDate())+"&nbsp;("+space.getRemoverName()+")");
			cell.setCompareOn(space.getRemoveDate());

			IconPane iconPane = gef.getIconPane();
			Icon restoreIcon = iconPane.addIcon();
			restoreIcon.setProperties(resource.getIcon("JSPP.restore"), resource.getString("JSPP.BinRestore"), "RestoreFromBin?ItemId="+space.getId());
			Icon deleteIcon = iconPane.addIcon();
			deleteIcon.setProperties(resource.getIcon("JSPP.delete"), resource.getString("JSPP.BinDelete"), "javaScript:onClick=removeItem('"+space.getId()+"')");
			line.addArrayCellText(restoreIcon.print()+"&nbsp;&nbsp;&nbsp;"+deleteIcon.print()+"&nbsp;&nbsp;&nbsp;<input type=\"checkbox\" name=\"SpaceIds\" value=\""+space.getId()+"\">");
		}
		out.println(arrayPane.print());
    out.println("<br/>");
	}

	//Array of deleted components
	if (removedComponents != null && !removedComponents.isEmpty()) {
    ArrayPane arrayPane = gef.getArrayPane("binContentComponents", "ViewBin", request, session);
    arrayPane.addArrayColumn(resource.getString("GML.component"));
    arrayPane.addArrayColumn(resource.getString("JSPP.BinRemoveDate"));
    ArrayColumn columnOp = arrayPane.addArrayColumn("<span style=\"float:left\">"+resource.getString("GML.operation")+"</span> <input type=\"checkbox\" id=\"checkAllComponents\" onclick=\"jqCheckAll2(this.id, 'ComponentIds')\" style=\"float:left;margin:0px;margin-left:5px;padding:0px;vertical-align:middle;background-color:none;\"/>");
    columnOp.setSortable(false);

    for (ComponentInstLight component : removedComponents) {
			ArrayLine line = arrayPane.addArrayLine();
      line.setId(component.getId());
			line.addArrayCellText("<a href=\"#\" class=\"item-path\" title=\""+component.getPath(" > ")+"\"/>"+
					EncodeHelper.javaStringToHtmlString(component.getLabel())+"</a>");
			ArrayCellText cell = line.addArrayCellText(resource.getOutputDateAndHour(component.getRemoveDate())+"&nbsp;("+component.getRemoverName()+")");
			cell.setCompareOn(component.getRemoveDate());

			IconPane iconPane = gef.getIconPane();
			Icon restoreIcon = iconPane.addIcon();
			restoreIcon.setProperties(resource.getIcon("JSPP.restore"), resource.getString("JSPP.BinRestore"), "RestoreFromBin?ItemId="+component.getId());
			Icon deleteIcon = iconPane.addIcon();
			deleteIcon.setProperties(resource.getIcon("JSPP.delete"), resource.getString("JSPP.BinDelete"), "javaScript:onClick=removeItem('"+component.getId()+"')");
			line.addArrayCellText(restoreIcon.print()+"&nbsp;&nbsp;&nbsp;"+deleteIcon.print()+"&nbsp;&nbsp;&nbsp;<input type=\"checkbox\" name=\"ComponentIds\" value=\""+component.getId()+"\">");
		}
		out.println(arrayPane.print());
	}
%>
</form>
  <span class="inlineMessage"><%=resource.getString("JSPP.BinEmpty")%></span>
</view:frame>
<%
out.println(window.printAfter());
%>
</body>
</html>