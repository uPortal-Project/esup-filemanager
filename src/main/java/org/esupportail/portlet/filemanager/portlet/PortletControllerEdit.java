/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.portlet.filemanager.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

@Controller
@Scope("request")
@RequestMapping("EDIT")
public class PortletControllerEdit {

    private static final Logger logger = LoggerFactory.getLogger(PortletControllerEdit.class);

    @RenderMapping
    public ModelAndView renderEditView(RenderRequest request, RenderResponse response) throws Exception {

        ModelMap model = new ModelMap();
        final PortletPreferences prefs = request.getPreferences();

        String viewMode = prefs.getValue(PortletController.PREF_PORTLET_VIEW, PortletController.STANDARD_VIEW);
        String showHiddenFiles = prefs.getValue(PortletController.PREF_SHOW_HIDDEN_FILES, "false");
        String useCursorWaitDialog = prefs.getValue(PortletController.PREF_USE_CURSOR_WAIT_DIALOG, "false");
        String useDoubleClick = prefs.getValue(PortletController.PREF_USE_DOUBLE_CLICK, "true");

        model.put("viewMode", viewMode);
        model.put("showHiddenFiles", showHiddenFiles);
        model.put("useCursorWaitDialog", useCursorWaitDialog);
        model.put("useDoubleClick", useDoubleClick);

        boolean roViewMode = prefs.isReadOnly(PortletController.PREF_PORTLET_VIEW);
        boolean roShowHiddenFiles = prefs.isReadOnly(PortletController.PREF_SHOW_HIDDEN_FILES);
        boolean roUseCursorWaitDialog = prefs.isReadOnly(PortletController.PREF_USE_CURSOR_WAIT_DIALOG);
        boolean roUseDoubleClick = prefs.isReadOnly(PortletController.PREF_USE_DOUBLE_CLICK);
        model.put("roViewMode", roViewMode);
        model.put("roShowHiddenFiles", roShowHiddenFiles);
        model.put("roUseCursorWaitDialog", roUseCursorWaitDialog);
        model.put("roUseDoubleClick", roUseDoubleClick);

        return new ModelAndView("edit-portlet", model);
    }

    @ActionMapping
    public void updatePreferences(ActionRequest request, ActionResponse response,
                                  @RequestParam(required=false) String viewMode,
                                  @RequestParam(required=false) String showHiddenFiles,
                                  @RequestParam(required=false) String useCursorWaitDialog,
                                  @RequestParam(required=false) String useDoubleClick
    ) throws Exception {

        final PortletPreferences prefs = request.getPreferences();

        if(viewMode != null)
            prefs.setValue(PortletController.PREF_PORTLET_VIEW, viewMode);
        if(showHiddenFiles != null)
            prefs.setValue(PortletController.PREF_SHOW_HIDDEN_FILES, showHiddenFiles);
        if(useCursorWaitDialog != null)
            prefs.setValue(PortletController.PREF_USE_CURSOR_WAIT_DIALOG, useCursorWaitDialog);
        if(useDoubleClick != null)
            prefs.setValue(PortletController.PREF_USE_DOUBLE_CLICK, useDoubleClick);

        prefs.store();

        response.setPortletMode(PortletMode.VIEW);
    }
}
