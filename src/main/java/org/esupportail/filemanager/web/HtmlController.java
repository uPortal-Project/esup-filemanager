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
package org.esupportail.filemanager.web;

import jakarta.annotation.Resource;
import org.esupportail.filemanager.beans.JsTreeFile;
import org.esupportail.filemanager.utils.PathEncodingUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = {"/", ""})
public class HtmlController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HtmlController.class);

    @Resource
    PathEncodingUtils pathEncodingUtils;


    @GetMapping
    public String browse(Model model, @RequestParam(required = false) String dir) {
        if(!StringUtils.hasText(dir)) {
            dir = JsTreeFile.ROOT_DRIVE;
            dir = pathEncodingUtils.encodeDir(dir);
        }
        model.addAttribute("defaultPath", dir);
        return "view-modern";
    }

}
