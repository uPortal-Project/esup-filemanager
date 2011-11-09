/*
 * Copyright (C) 2011 Esup Portail http://www.esup-portail.org
 * Copyright (C) 2011 UNR RUNN http://www.unr-runn.fr
 * @Author (C) 2011 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
 * @Contributor (C) 2011 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
 * @Contributor (C) 2011 Julien Marchal <Julien.Marchal@univ-nancy2.fr>
 * @Contributor (C) 2011 Julien Gribonvald <Julien.Gribonvald@recia.fr>
 * @Contributor (C) 2011 David Clarke <david.clarke@anu.edu.au>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
var uploadListFadeoutTime = 3000;
var infoTolbarFadeoutTime = 8000;

//IE does not define this by default
if ("undefined" === typeof window.console)
{
	    window.console = {
	        "log": function() { }
	    };	
}

//Another IE fix, prevents IE from cacheing ajax calls
$.ajaxSetup({
    cache: false
});
	
(function ($) {

    $(document).ready(function () {

        /* GIP recia : START --> manage size of container */
    	
    	var treeAreaWidth = $('#arborescentArea').css("width");
    	var treeAreaHeight = $('#arborescentArea').css("height");
    	
    	//In order for resizable and scrollbars to work together, we wrap the tree area in a div.  
    	// Technique is from :
    	// http://stackoverflow.com/questions/3858460/jquery-ui-resizable-with-scroll-bars
    	
        $("#arborescentArea")
        .wrap('<div/>')
            .parent()
              .css({'display':'inline-block',
                    'overflow':'hidden',
                    'height': treeAreaHeight,
                    'width': treeAreaWidth,
                    'paddingBottom':'2px',
                    'paddingRight':'2px'
                   }).resizable({
                       minHeight: 122,
                       maxHeight: 500,
                       minWidth: 122,
                       maxWidth: 738,
                       ghost: false,
                       helper: false,
                       stop: function (event, ui) {
                    	   console.log("Ui width: " + ui.size.width);
                    	   console.log("Ui orig width: " + ui.originalSize.width);
                           var offsetWidth = ui.size.width - ui.originalSize.width;
                           var newWidth = $("#browserArea").width() - offsetWidth; // - * - = +
                           $("#browserArea").css("width", newWidth);
                           var finalWidthArbo = $("#arborescentArea").width();
                           console.log("Final width arbo " + finalWidthArbo);
                           //change width to detail area
                           $("#detailArea").css("width", finalWidthArbo);

                           //change width of main left container                
                           $("#leftArea").css("width", finalWidthArbo);
                           
                           var newHeight = $("#leftArea").height();
                           console.log("newHeight for browserArea " + newHeight);
                           $("#browserArea").css("height", $("#leftArea").height());
                       }
                   }).find("#arborescentArea")
                        .css({overflow:'auto',
                              width:'100%',
                              height:'100%'});

        $("#browserMain").css({
            width:'100%',
            height:'92%'});


        // take care of resizing window
        var esupStockResizeDoit;
        $(window).resize(function() {
        	clearTimeout(esupStockResizeDoit);
        	console.log("Handler for .resize() called.");
        	esupStockResizeDoit = setTimeout(function(){
	            $("#leftArea").css("width", "30%");
	            $("#browserArea").css("width", "67%");
	        	
	             var finalWidthArbo = $("#leftArea").width();
	             console.log("change width to detail and arborescence area " + finalWidthArbo);
	             $("#detailArea").css("width", finalWidthArbo);
	             $("#arborescentArea").parent().css("width", finalWidthArbo);
	             
	             var finalHeightArbo = $("#leftArea").height()-$("#detailArea").height();
	             console.log("change height to arborescence area " + finalHeightArbo);
	             $("#arborescentArea").parent().css("height", finalHeightArbo);   
        	}, 100);
        }); 
        
        
        
        initJstree();

        var uploader = new qq.FileUploader({
            multiple: true,
            template: fileuploadTemplate,
            fileTemplate: fileTemplate,
            element: document.getElementById('file-uploader'),
            action: '/esup-portlet-stockage/servlet-ajax/uploadFile',
            onSubmit: function (id, fileName) {
                uploader.setParams({
                    dir: $("#bigdirectory").attr("rel"),
                    sharedSessionId: sharedSessionId
                });
                cursor_wait();
                $('.qq-upload-list').show();
            },
            onComplete: function (id, fileName, result) {
                cursor_clear();
                showInfoToolBar(result.msg);
                if (result.success) {
                    refreshCurrentDirectory();
                }
                if (uploader.getInProgress() == 0) {
                    $('.qq-upload-list').fadeOut(uploadListFadeoutTime);
                    //$('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
                }
            },
            onCancel: function (id, fileName) {
            	cursor_clear();
                if (uploader.getInProgress() == 0) {
                    $('.qq-upload-list').fadeOut(uploadListFadeoutTime);
                }
                //$('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
            }
        });

       
        //must be done after the uploader is initialized
        initToolBar();

        //Move the browser area menu to the body in order that it is positioned correctly
        var menu = $("#myMenu");
        menu.detach();
        
        menu.appendTo("body");

    });

})(jQuery);



function getCheckedDirs() {
    console.log("getCheckedDirs");
    var dirs = new Array();
    $(".browsercheck:checked").each(function () {
        dirs.push($(this).val());
    });
    return dirs;
}

function cursor_wait() {
    console.log("cursor_wait");

    var dialogElem = $("#waitingDialog");
    
    if (dialogElem.dialog("isOpen")) {
        console.log("cursor_wait already open");
    }

    dialogElem.dialog({
        modal: true,
        resizable: false,
        closeOnEscape: false,
        dialogClass: "waiting-dialog"
    });
    
    
}

// Returns the cursor to the default pointer

function cursor_clear() {
    //$("body").css("cursor", "auto");
    console.log("Cursor clear");
    $("#waitingDialog").dialog('close');
}




function showInfoToolBar(msg) {
    console.log("showInfoToolBar");
    $("#info-toolbar").html(msg);
    /*$("#info-toolbar span").show('blind', {
        direction: 'vertical'
    }, 2000);*/
    setTimeout("hideInfoToolBar()", 4000);
}

function hideInfoToolBar() {
    console.log("hideInfoToolBar");
    /*$("#info-toolbar span").hide('blind', {
        direction: 'vertical'
    }, 2000);*/
    $("#info-toolbar").html("<span>...</span>");
}


function getBrowserAreaCheckedSelectionData() {
    console.log("getBrowserAreaCheckedSelectionData");

    var checkedItems = $(".browsercheck:checked");

    var returnData = new Object();

    returnData.numberItemsChecked = checkedItems.length;

    if (checkedItems.length == 1) {
        //Is it a folder or file?
        var checkedItemParent = getParentRow(checkedItems[0])[0];
        var folderList = $(checkedItemParent).find(".fileTreeRef");
        if (folderList.length > 0) {
            console.log("Folder selected");
            //a folder
            returnData.singleFolderSelected = true;
            returnData.path = $(folderList[0]).attr("rel");

        } else {
            var file = $(checkedItemParent).find(".file")[0];
            console.log(file);
            returnData.singleFileSelected = true;
            returnData.path = $(file).attr("rel");
            //a file
            console.log("File selected");
        }
    }

    var readable = true;
    var writeable = true;

    var parentTds = getParentRow(checkedItems);

    parentTds.each(function (idx, elem) {
        var parentTd = $(elem);
        readable = readable && parentTd.find("div.readable").html() === "true";
        writeable = writeable && parentTd.find("div.writeable").html() === "true";

        console.log(" readable : " + readable + " writeable : " + writeable);
    });

    returnData.readable = readable;
    returnData.writeable = writeable;

    return returnData;
}



function deleteFilesFromBrowserArea() {
    console.log("deleteFilesFromBrowserArea");
    deleteFiles($("#filesForm").serializeArray());
}

//implement stringifyJSON serialization
function stringifyJSON(obj) {
    var t = typeof (obj);
    if (t != "object" || obj === null) {
        // simple data type
        if (t == "string") obj = '"' + obj + '"';
        return String(obj);
    } else {
        // recurse array or object
        var n, v, json = [],
            arr = (obj && obj.constructor == Array);
        for (n in obj) {
            v = obj[n];
            t = typeof (v);
            if (t == "string") v = '"' + v + '"';
            else if (t == "object" && v !== null) v = stringifyJSON(v);
            json.push((arr ? "" : '"' + n + '":') + String(v));
        }
        return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
    }
};


function getFile(parentDir, fileId) {
    console.log("getFile");
    if (parentDir != null) {
        var parentObj = document.getElementById(parentDir);
        $("#fileTree").jstree("open_node", parentObj, function () {
            var obj = document.getElementById(fileId);
            $("#fileTree").jstree("select_node", obj, true);
        }, true);
    } else {
        if (fileId != 'FS:') {
            var obj = document.getElementById(fileId);
            $("#fileTree").jstree("select_node", obj, true);
        } else {
            $.ajax({
                async: true,
                type: 'POST',
                url: '/esup-portlet-stockage/servlet-ajax/htmlFileTree',
                data: {
                    "dir": fileId,
                    "sharedSessionId": sharedSessionId
                },
                success: function (r) {
                    $("#browserMain").html(r);
                }
            });
        }
    }
}

/**
 * Utility function to get the jquery object.  
 * @param elem
 * @returns
 */
function getJqueryObj(elem) {

	if (elem instanceof jQuery) {
		//Already a jquery object
		return elem;
	} else {
		//A dom element
		return $(elem);
	}
}



function authenticate(dir, username, password) {
    console.log("authenticate");
    $.ajax({
        async: true,
        type: 'POST',
        url: '/esup-portlet-stockage/servlet-ajax/authenticate',
        data: {
            "dir": dir,
            "username": username,
            "password": password,
            "sharedSessionId": sharedSessionId
        },
        success: function (data) {
            showInfoToolBar(data.msg);
            if (data.status) getFile(null, dir);
        }
    });
}
