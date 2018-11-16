/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.aihdconfigs.page.controller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.aihdconfigs.AihdUser;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.referenceapplication.ReferenceApplicationConstants;
import org.openmrs.module.referenceapplication.ReferenceApplicationWebConstants;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.page.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openmrs.module.referenceapplication.ReferenceApplicationWebConstants.COOKIE_NAME_LAST_SESSION_LOCATION;
import static org.openmrs.module.referenceapplication.ReferenceApplicationWebConstants.REQUEST_PARAMETER_NAME_REDIRECT_URL;
import static org.openmrs.module.referenceapplication.ReferenceApplicationWebConstants.SESSION_ATTRIBUTE_REDIRECT_URL;

/**
 * Spring MVC controller for /terms.htm page
 */
@Controller
public class TermsPageController {

    //see TRUNK-4536 for details why we need this
    private static final String GET_LOCATIONS = "Get Locations";

    // RA-592: don't use PrivilegeConstants.VIEW_LOCATIONS
    private static final String VIEW_LOCATIONS = "View Locations";

    protected final Log log = LogFactory.getLog(getClass());

    @RequestMapping("/"+"terms.htm")
    public String overrideLoginpage() {
        return "forward:/" + "aihdconfigs" + "/" + "terms" + ".page";
    }

    /**
     * @should redirect the user to the home page if they are already authenticated
     * @should show the user the login page if they are not authenticated
     * @should set redirectUrl in the page model if any was specified in the request
     * @should set the referer as the redirectUrl in the page model if no redirect param exists
     * @should set redirectUrl in the page model if any was specified in the session
     * @should not set the referer as the redirectUrl in the page model if referer url is outside context path
     * @should set the referer as the redirectUrl in the page model if referer url is within context path
     */
    public String get(PageModel model,
                      UiUtils ui,
                      PageRequest pageRequest,
                      @SpringBean("appFrameworkService") AppFrameworkService appFrameworkService) {

        return null;
    }
}
