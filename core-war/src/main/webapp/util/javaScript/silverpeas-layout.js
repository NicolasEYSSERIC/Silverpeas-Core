/*
 * Copyright (C) 2000 - 2018 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception. You should have recieved a copy of the text describing
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

(function($window) {

  /**
   * The instance of the plugin must be attached to the top window.
   * If the plugin is called from an iframe, then the iframe plugin instance is the reference of
   * the one of the top window. By this way, all different javascript window instances use the same
   * plugin instance.
   * If the plugin, on top window, is already defined, nothing is done.
   */

  if ($window.spLayout) {
    if (!window.spLayout) {
      window.spLayout = $window.spLayout;
    }
    return;
  }

  if (!$window.LayoutSettings) {
    $window.LayoutSettings = new SilverpeasPluginSettings();
  }

  var layoutDebug = false;

  var PDC_ACTIVATED = $window.LayoutSettings.get("layout.pdc.activated");
  var PDC_URL_BASE = $window.LayoutSettings.get("layout.pdc.baseUrl");
  var PDC_DEFAULT_ACTION = $window.LayoutSettings.get("layout.pdc.action.default");

  var __eventManager = {};
  applyEventDispatchingBehaviorOn(__eventManager);

  /**
   * Common behavior
   */
  var Part = SilverpeasClass.extend({
    initialize : function(mainLayout, selector) {
      this.mainLayout = mainLayout;
      this.selector = selector;
      this.eventNamePrefix =
          selector.replace(/sp-layout-/g, "").replace(/part/g, "").replace(/layout/g, "").replace(
              /[ -\\#]/g, "");
      this.container = $window.document.querySelector(selector);
      this.lastStartLoadTime = 0;
    },
    getMainLayout : function() {
      return this.mainLayout;
    },
    getContainer : function() {
      return this.container;
    },
    hide : function() {
      this.getContainer().style.display = 'none';
      this.dispatchEvent("hide");
    },
    show : function() {
      this.getContainer().style.display = '';
      this.dispatchEvent("show");
    },
    isShown : function() {
      return this.getContainer().style.display !== 'none';
    },
    normalizeEventName : function(eventName) {
      return this.eventNamePrefix + eventName;
    },
    addEventListener : function(eventName, listener, listenerId) {
      switch (eventName) {
        case 'start-load':
        case 'load':
        case 'show':
        case 'hide':
          var normalizedEventName = this.normalizeEventName(eventName);
          __eventManager.addEventListener(normalizedEventName, listener, listenerId);
          break;
        default:
          __logError("'" + eventName + "' is not handled on part represented by the selector '" +
              this.selector);
      }
    },
    dispatchEvent : function(eventName, data) {
      if (eventName === 'start-load') {
        this.lastStartLoadTime = new Date().getTime();
      }
      var normalizedEventName = this.normalizeEventName(eventName);
      __eventManager.dispatchEvent(normalizedEventName, data);
    },
    getLastStartLoadTime : function() {
      return this.lastStartLoadTime;
    }
  });

  // Header Part
  var HeaderPart = Part.extend({
    load : function(urlParameters) {
      __logDebug("loading header part");
      var headerPartURL = $window.LayoutSettings.get("layout.header.url");
      this.dispatchEvent("start-load");
      return sp.load(this.getContainer(), sp.ajaxRequest(headerPartURL).withParams(urlParameters))
          .then(function() {
            this.dispatchEvent("load");
          }.bind(this));
    }
  });

  // Body Part
  var BodyPart = Part.extend({
    initialize : function(mainLayout, partSelectors) {
      this._super(mainLayout, partSelectors.body);
      this.partSelectors = partSelectors;
      this.__nb_subLoads = 0;
      this.__hidePromise = undefined;
      this.__hide_timeout = undefined;
    },
    resize : function() {
      var bodyLayoutHeight = $window.innerHeight -
          this.getMainLayout().getHeader().getContainer().offsetHeight;
      if (PDC_ACTIVATED) {
        bodyLayoutHeight -= this.getMainLayout().getFooter().getContainer().offsetHeight;
      }
      this.getContainer().style.height = bodyLayoutHeight + 'px';
      if (this.rootLayout) {
        this.rootLayout.style.height = this.rootLayout.parentNode.style.height;
      }
      __logDebug("resizing body height part to '" + bodyLayoutHeight + "px'");
    },
    showProgressMessage : function(hidePromise) {
      __showProgressPopup();
      if (typeof this.__hidePromise === 'undefined' && sp.promise.isOne(hidePromise)) {
        this.__hidePromise = hidePromise;
      }
      __logDebug('showPM - __nb_subLoads state ' + this.__nb_subLoads);
    },
    hideProgressMessage : function() {
      this.__nb_subLoads -= 1;
      var __hideProgressMessage = function() {
        clearTimeout(this.__hide_timeout);
        this.__hide_timeout = setTimeout(function() {
          this.__nb_subLoads = 0;
          setTimeout(__hideProgressPopup, 0);
        }.bind(this), 250);
        this.__hidePromise = undefined;
      }.bind(this);
      if (this.__nb_subLoads <= 0) {
        this.__nb_subLoads = 0;
        if (this.__hidePromise) {
          this.__hidePromise.then(__hideProgressMessage,__hideProgressMessage);
        } else {
          __hideProgressMessage.call(this);
        }
      }
      __logDebug('hidePM - __nb_subLoads state ' + this.__nb_subLoads);
    },
    load : function(urlParameters) {
      __logDebug("loading body part");
      applyReadyBehaviorOn(this);
      var bodyPartURL = $window.LayoutSettings.get("layout.body.url");
      this.__nb_subLoads = 0;
      this.dispatchEvent("start-load");
      var ajaxConfig = sp.ajaxRequest(bodyPartURL).withParams(urlParameters);
      return sp.load(this.getContainer(), ajaxConfig)
          .then(function() {
              __logDebug("... initializing the context of body part instance");
            this.rootLayout = $window.document.querySelector(this.partSelectors.bodyNavigationAndContentLayout);
            this.resize();
            this.togglePart = new BodyTogglePart(this.getMainLayout(), this.partSelectors.bodyToggles);
            this.navigationPart = new BodyNavigationPart(this.getMainLayout(), this.partSelectors.bodyNavigation);
            this.contentPart = new BodyContentPart(this.getMainLayout(), this.partSelectors.bodyContent);
            this.contentFrame = this.getContent().getContainer().querySelector('iframe');
            this.contentFrame.setAttribute('webkitallowfullscreen', 'true');
            this.contentFrame.setAttribute('mozallowfullscreen', 'true');
            this.contentFrame.setAttribute('allowfullscreen', 'true');
            this.contentFrame.addEventListener("load", function() {
              __logDebug("body content part loaded");
              if (typeof this.getContent().notifyReady === 'function') {
                __logDebug("resolving promise of body content load");
                this.getContent().notifyReady();
              } else {
                __logDebug("no promise to resolve about the body content loading on body layout load");
              }

              var frameContentDocument = this.contentFrame.contentWindow.document;
              frameContentDocument.body.setAttribute('tabindex', '-1');
              frameContentDocument.body.focus();

              this.getContent().dispatchEvent("load");
              this.hideProgressMessage();
            }.bind(this));
            __logDebug("resolving promise of body layout load");
            this.dispatchEvent("load");
            this.notifyReady();
          }.bind(this));
    },
    getToggles : function() {
      return this.togglePart;
    },
    getNavigation : function() {
      return this.navigationPart;
    },
    getContent : function() {
      return this.contentPart;
    }
  });

  // Toggle Part
  var BodyTogglePart = Part.extend({
    initialize : function(mainLayout, partSelector) {
      this._super(mainLayout, partSelector);
      this.headerToggle = $window.document.querySelector("#header-toggle");
      this.navigationToggle = $window.document.querySelector("#navigation-toggle");

      this.headerToggle.addEventListener('click', this.toggleHeader.bind(this), '__click__BodyTogglePart');
      this.navigationToggle.addEventListener('click', this.toggleNavigation.bind(this), '__click__BodyTogglePart');
    },
    toggleHeader : function() {
      var icon = this.headerToggle.querySelector('img');
      if (this.getMainLayout().getHeader().isShown()) {
        this.getMainLayout().getHeader().hide();
        icon.src = "icons/silverpeasV5/extendTopBar.gif";
      } else {
        this.getMainLayout().getHeader().show();
        icon.src = "icons/silverpeasV5/reductTopBar.gif";
      }
      icon.blur();
      this.getMainLayout().getBody().resize();
    },
    toggleNavigation : function() {
      var icon = this.navigationToggle.querySelector('img');
      if (this.getMainLayout().getBody().getNavigation().isShown()) {
        this.getMainLayout().getBody().getNavigation().hide();
        icon.src = "icons/silverpeasV5/extend.gif";
      } else {
        this.getMainLayout().getBody().getNavigation().show();
        icon.src = "icons/silverpeasV5/reduct.gif";
      }
      icon.blur();
      this.getMainLayout().getBody().resize();
    },
    addEventListener : function(eventName, listener, listenerId) {
      switch (eventName) {
        case 'hide-navigation-toggle':
        case 'show-navigation-toggle':
          var normalizedEventName = this.normalizeEventName(eventName);
          __eventManager.addEventListener(normalizedEventName, listener, listenerId);
          break;
        default:
          this._super(eventName, listener);
      }
    },
    hideNavigationToggle : function() {
      this.navigationToggle.style.display = 'none';
      this.dispatchEvent("hide-navigation-toggle");
    },
    showNavigationToggle : function() {
      this.navigationToggle.style.display = '';
      this.dispatchEvent("show-navigation-toggle");
    }
  });

  // Navigation Part
  var BodyNavigationPart = Part.extend({
    initialize : function(mainLayout, partSelector) {
      this._super(mainLayout, partSelector);
      this.addEventListener("start-load", function() {
        spLayout.getBody().showProgressMessage();
      }, '__start-load__BodyNavigationPart');
      this.addEventListener("load", function() {
        spLayout.getBody().hideProgressMessage();
      }, '__load__BodyNavigationPart');
    },
    load : function(urlParameters) {
      __logDebug("loading body navigation part");
      spLayout.getBody().__nb_subLoads += 1;
      spLayout.getBody().showProgressMessage();
      this.getMainLayout().getBody().getToggles().hide();
      var parameters = extendsObject({
        "privateDomain" : "", "privateSubDomain" : "", "component_id" : ""
      }, urlParameters);
      var bodyNavigationPartURL = $window.LayoutSettings.get("layout.body.navigation.url");
      var ajaxConfig = sp.ajaxRequest(bodyNavigationPartURL).withParams(parameters);
      return sp.load(this.getContainer(), ajaxConfig)
          .then(function() {
            this.getMainLayout().getBody().getToggles().show();
          }.bind(this));
    },
    addEventListener : function(eventName, listener, listenerId) {
      switch (eventName) {
        case 'changeselected':
          var normalizedEventName = this.normalizeEventName(eventName);
          __eventManager.addEventListener(normalizedEventName, listener, listenerId);
          break;
        default:
          this._super(eventName, listener);
      }
    },
    hide : function(withToggle) {
      if (withToggle) {
        spLayout.getBody().getToggles().hideNavigationToggle();
      }
      this._super();
    },
    show : function() {
      spLayout.getBody().getToggles().showNavigationToggle();
      this._super();
    }
  });

  // Content Part
  var BodyContentPart = Part.extend({
    load : function(url) {
      __logDebug("loading body content part");
      spLayout.getBody().__nb_subLoads += 1;
      var promise = applyReadyBehaviorOn(this);
      this.dispatchEvent("start-load");
      $window.MyMain.location.assign(url);
      return promise;
    },
    toggleFullscreen : function(fullscreen) {
      if (spFscreen.fullscreenEnabled()) {
        var _fullscreen = typeof fullscreen === 'undefined' ? !this._lastFullscreen : fullscreen;
        if (_fullscreen) {
          var $iframe = this.getContainer().querySelector('iframe');
          spFscreen.requestFullscreen($iframe);
        } else if (!_fullscreen) {
          spFscreen.exitFullscreen();
        }
        this._lastFullscreen = _fullscreen;
        return _fullscreen;
      }
      return false;
    },
    muteMouseEvents : function() {
      var $iframe = this.getContainer().querySelector('iframe');
      $iframe.classList.add('sp-layout-part-on-top-element-drag');
    },
    unmuteMouseEvents : function() {
      var $iframe = this.getContainer().querySelector('iframe');
      $iframe.classList.remove('sp-layout-part-on-top-element-drag');
    },
    forceOnBackground : function() {
      this.getContainer().style.zIndex = -1;
    },
    unforceOnBackground : function() {
      this.getContainer().style.zIndex = '';
    },
    setOnForeground : function() {
      this.getContainer().classList.add('sp-layout-part-on-foreground');
    },
    setOnBackground : function() {
      this.getContainer().classList.remove('sp-layout-part-on-foreground');
    }
  });

  // Footer Part
  var FooterPart = Part.extend({
    loadPdc : function(urlParameters) {
      if (PDC_ACTIVATED) {
        __logDebug("loading PDC part");
        var parameters = extendsObject({
          "action" : PDC_DEFAULT_ACTION,
          "SearchPage" : "/admin/jsp/silverpeas-pdc-search-footer-part.jsp"
        }, urlParameters);
        var action = parameters.action;
        delete parameters.action;
        var ajaxConfig = sp.ajaxRequest(PDC_URL_BASE + action).withParams(parameters);
        return sp.load(this.getContainer(), ajaxConfig).then(function() {
          this.dispatchEvent("pdcload");
        }.bind(this));
      }
    },
    hidePdc : function() {
      if (PDC_ACTIVATED){
        __logDebug("hiding PDC part");
        this.getMainLayout().getFooter().hide();
        this.getMainLayout().getBody().resize();
        this.dispatchEvent("pdchide");
      }
    },
    showPdc : function() {
      if (PDC_ACTIVATED){
        __logDebug("showing PDC part");
        this.getMainLayout().getFooter().show();
        this.getMainLayout().getBody().resize();
        this.dispatchEvent("pdcshow");
      }
    },
    addEventListener : function(eventName, listener, listenerId) {
      switch (eventName) {
        case 'pdcload':
        case 'pdcshow':
        case 'pdchide':
          var normalizedEventName = this.normalizeEventName(eventName);
          __eventManager.addEventListener(normalizedEventName, listener, listenerId);
          break;
        default:
          this._super(eventName, listener);
      }
    }
  });

  // Content Part
  var SplashContentUrlPart = Part.extend({
    initialize : function(mainLayout) {
      var overlay = document.createElement('div');
      overlay.classList.add('sp-layout-splash-content-url-part-overlay');
      overlay.style.display = 'none';
      document.body.appendChild(overlay);
      var contentFrame = document.createElement('iframe');
      contentFrame.setAttribute('src', 'about:blank');
      contentFrame.setAttribute('name', 'SpLayoutSplashContentWindow');
      contentFrame.setAttribute('marginheight', '0');
      contentFrame.setAttribute('frameborder', '0');
      contentFrame.setAttribute('scrolling', 'auto');
      contentFrame.setAttribute('width', '100%');
      contentFrame.setAttribute('height', '100%');
      contentFrame.setAttribute('webkitallowfullscreen', 'true');
      contentFrame.setAttribute('mozallowfullscreen', 'true');
      contentFrame.setAttribute('allowfullscreen', 'true');
      var container = document.createElement('div');
      container.setAttribute('id', 'sp-layout-splash-content-url-part');
      container.style.display = 'none';
      container.appendChild(contentFrame);
      document.body.appendChild(container);
      contentFrame.addEventListener("load", function() {
        __logDebug("splash content part loaded");
        if (typeof this.notifyReady === 'function') {
          __logDebug("resolving promise of splash content load");
          this.notifyReady();
        } else {
          __logDebug("no promise to resolve about the splash content loading");
        }

        var frameContentDocument = this.contentFrame.contentWindow.document;
        frameContentDocument.body.setAttribute('tabindex', '-1');
        frameContentDocument.body.focus();

        this.dispatchEvent("load");
        spLayout.getBody().hideProgressMessage();
      }.bind(this));

      this._super(mainLayout, '#sp-layout-splash-content-url-part');
      this.overlay = overlay;
      this.contentFrame = contentFrame;

      this.addEventListener("start-load", function() {
        spLayout.getBody().showProgressMessage();
      }, '__start-load__SplashContentUrlPart');
    },
    load : function(url) {
      __logDebug("loading splash content part");
      var promise = applyReadyBehaviorOn(this);
      this.dispatchEvent("start-load");
      this.contentFrame.setAttribute('src', url);
      promise.then(function() {
        var progressMessageDeferred;
        if (!this.isShown()) {
          progressMessageDeferred = sp.promise.deferred();
          spLayout.getBody().showProgressMessage(progressMessageDeferred.promise);
        }
        this.show(progressMessageDeferred);
      }.bind(this));
      return promise;
    },
    close : function() {
      this.hide().then(function() {
        this.contentFrame.setAttribute('src', 'about:blank');
        this.dispatchEvent("close");
      }.bind(this));
    },
    show : function(progressMessageDeferred) {
      var _super = this._super;
      return new Promise(function(resolve) {
        var __end = function() {
          resolve();
          if (progressMessageDeferred) {
            progressMessageDeferred.resolve();
          }
        };
        if (!this.isShown()) {
          this.overlay.style.display = 'block';
          jQuery(this.getContainer()).fadeIn(200, function() {
            _super.call(this);
            __end();
          }.bind(this));
        } else {
          __end();
        }
      }.bind(this));
    },
    hide : function() {
      var _super = this._super;
      return new Promise(function(resolve) {
        if (this.isShown()) {
          jQuery(this.getContainer()).fadeOut(400, function() {
            this.overlay.style.display = 'none';
            _super.call(this);
            resolve();
          }.bind(this));
        } else {
          resolve();
        }
      }.bind(this));
    },
    addEventListener : function(eventName, listener, listenerId) {
      switch (eventName) {
        case 'close':
          var normalizedEventName = this.normalizeEventName(eventName);
          __eventManager.addEventListener(normalizedEventName, listener, listenerId);
          break;
        default:
          this._super(eventName, listener);
      }
    },
    muteMouseEvents : function() {
      var $iframe = this.getContainer().querySelector('iframe');
      $iframe.classList.add('sp-layout-part-on-top-element-drag');
    },
    unmuteMouseEvents : function() {
      var $iframe = this.getContainer().querySelector('iframe');
      $iframe.classList.remove('sp-layout-part-on-top-element-drag');
    }
  });

  /**
   * Handling the rendering of the Silverpeas's layout.
   * @constructor
   */
  $window.SilverpeasLayout = function(partSelectors) {
    __logDebug("initializing Silverpeas Layout plugin");
    var headerPart = new HeaderPart(this, partSelectors.header);
    var bodyPart = new BodyPart(this, partSelectors);
    var footerPart = new FooterPart(this, partSelectors.footer);
    var splashContentUrlPart = new SplashContentUrlPart(this);

    this.getHeader = function() {
      return headerPart;
    };
    this.getBody = function() {
      return bodyPart;
    };
    this.getFooter = function() {
      return footerPart;
    };
    this.getSplash = function() {
      return splashContentUrlPart;
    };
    this.isWindowTop = function(win) {
      return this.getWindowTopFrom(win).window === win;
    };
    this.getWindowTopFrom = function(win) {
      var $top = win.top;
      if (win.name === 'SpLayoutSplashContentWindow') {
        $top = win;
      } else if (win.parent.window.name === 'SpLayoutSplashContentWindow') {
        $top = win.parent;
      }
      return $top;
    };

    var timer_resize;
    $window.addEventListener('resize', function() {
      clearTimeout(timer_resize);
      timer_resize = setTimeout(function() {
        this.getBody().resize();
      }.bind(this), 0);
    }.bind(this));
  };

  function __showProgressPopup() {
    jQuery.progressMessage();
  }

  function __hideProgressPopup() {
    jQuery.closeProgressMessage();
  }

  /**
   * Logs errors.
   * @param message
   * @private
   */
  function __logError(message) {
    sp.log.error("Layout - " + message);
  }

  /**
   * Logs debug messages.
   * @private
   */
  function __logDebug() {
    if (layoutDebug) {
      var mainDebugStatus = sp.log.debugActivated;
      sp.log.debugActivated = true;
      var messages = [];
      Array.prototype.push.apply(messages, arguments);
      messages.splice(0, 0, "Layout -");
      sp.log.debug.apply(this, messages);
      sp.log.debugActivated = mainDebugStatus;
    }
  }
})(window.opener ? window.opener.top.window : top.window);

function initializeSilverpeasLayout(bodyLoadParameters) {
  if (top === window) {
    var partSelectors = {
      "header" : "#sp-layout-header-part",
      "body" : "#sp-layout-body-part",
      "bodyToggles" : "#sp-layout-body-part-layout-toggle-part",
      "bodyNavigationAndContentLayout" : "#sp-layout-body-part-layout",
      "bodyNavigation" : "#sp-layout-body-part-layout-navigation-part",
      "bodyContent" : "#sp-layout-body-part-layout-content-part",
      "footer" : "#sp-layout-footer-part"
    };
    window.spServerEventSource = new function() {
      var commonSseUrl = webContext + '/sse/common';
      if (window.EVENT_SOURCE_POLYFILL_ACTIVATED) {
        commonSseUrl += '?heartbeat=true';
      }
      var serverEventSource = new EventSource(commonSseUrl);
      applyEventDispatchingBehaviorOn(this, {
        onAdd : function(serverEventName, listener) {
          serverEventSource.addEventListener(serverEventName, listener);
        },
        onRemove : function(serverEventName, listener) {
          serverEventSource.removeEventListener(serverEventName, listener);
        }
      });
      this.close = function() {
        serverEventSource.close();
      };
    };
    window.spLayout = new SilverpeasLayout(partSelectors);
    spLayout.getHeader().load();
    spLayout.getBody().load(bodyLoadParameters).then(function() {
      window.spWindow = new SilverpeasWindow();
    });
  }
}