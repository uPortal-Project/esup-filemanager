/*
 * Function used to enable/disable the toolbar as well
 * as the Browser area menu.  Takes a function that takes
 * the name of the action and enabled/disabled
 */

function updateActions(setActionElementEnabled) {
    console.log("updateActions");
    var baSelData = getBrowserAreaCheckedSelectionData();

    console.log("Update actions : " + stringifyJSON(baSelData));

    //If no items are selected, it is as if a folder is selected
    setUploadEnabled(baSelData.numberItemsChecked == 0);
    setActionElementEnabled("new_file", baSelData.numberItemsChecked == 0);
    setActionElementEnabled("new_folder", baSelData.numberItemsChecked == 0);
    
    //Also, the context menu should be closed if nothing is selected
    if (baSelData.numberItemsChecked == 0) {
    	$(".contextMenu").hide();
    }

    setActionElementEnabled("download", baSelData.singleFileSelected == true && baSelData.readable && baSelData.numberItemsChecked == 1);

    setActionElementEnabled("rename", baSelData.numberItemsChecked == 1 && baSelData.readable && baSelData.writeable);

    setActionElementEnabled("zip", baSelData.readable && baSelData.numberItemsChecked > 0);
    setActionElementEnabled("copy", baSelData.readable && baSelData.numberItemsChecked > 0);

    //It is possible to paste with a single file selected.  It
    //represents the currently parent repository
    setActionElementEnabled("paste", (
    //Either a writeable dir
    (baSelData.writeable && baSelData.singleFolderSelected)
    // or any file
    || baSelData.singleFileSelected)
    //And a cut or copy must have been completed
    && canPaste(baSelData.path));

    setActionElementEnabled("delete", baSelData.writeable && baSelData.numberItemsChecked > 0);

    setActionElementEnabled("cut", baSelData.readable && baSelData.writeable && baSelData.numberItemsChecked > 0);
    
    

}



function handleLeftTreeSelection(treeNode) {
    console.log("handleLeftTreeSelection");
    
    //Fetch now when we know treeNode exists.  After the fetch is completed, this is 
    //not garaunteed if the element is not opened automatically.
    
    var path = getPathFromLiNode(treeNode);
    var type = getTypeFromLiNode(treeNode);
    
    cursor_wait();

    $.ajax({
        async: true,
        type: 'POST',
        url: '/esup-portlet-stockage/servlet-ajax/htmlFileTree',
        data: {
            "dir": path,
            "sharedSessionId": sharedSessionId
        },
        success: function (r) {
            console.log("handleLeftTreeSelection: htmlFileTree ajax call succeeded");
            initializeBrowserMain(r);

            var needsAuthentication = $("#browserMain #authenticationForm").length > 0;

            var type = getTypeFromLiNode(treeNode);
            if (type != "category" && !needsAuthentication) {
                //refresh the details area
                updateDetailsArea({
                    "dirs": path,
                    "type": type,
                    "sharedSessionId": sharedSessionId
                });

            } else {
                $("#detailArea").html("");
            }


            //Disable/Enable toolbar items
            setToolbarActionElementEnabled('refresh', true);
            setToolbarActionElementEnabled('thumbnail', !needsAuthentication);
            setToolbarActionElementEnabled('list', !needsAuthentication);
            setUploadEnabled(!needsAuthentication);


            setToolbarActionElementEnabled('new_folder', !needsAuthentication);
            setToolbarActionElementEnabled('new_file', !needsAuthentication);

            setToolbarActionElementEnabled("download", false);
            setToolbarActionElementEnabled("zip", false);
            setToolbarActionElementEnabled("copy", false);
            setToolbarActionElementEnabled("cut", false);
            setToolbarActionElementEnabled("paste", canPaste(path));
            setToolbarActionElementEnabled("rename", false);
            setToolbarActionElementEnabled("delete", false);

        },
        error: function (r) {
            console.log("Left tree node refresh ajax call failed");
            $("#browserMain").html("");
            if (r.statusText == 'reload') {
                location.reload();
            } else {
                showInfoToolBar(r.responseText);
            }


        },
        complete: cursor_clear
    });


}

function updateDetailsArea(dataObj) {
    console.log("updateDetailsArea : " + stringifyJSON(dataObj));

    cursor_wait();
    $.ajax({
        async: true,
        type: 'POST',
        url: '/esup-portlet-stockage/servlet-ajax/detailsArea',
        data: dataObj,
        success: function (response) {
            $("#detailArea").html(response);
        },
        error: function (response) {
            console.log("Detail area failed");
            $("#detailArea").html("");
            if (response.statusText == 'reload') {
                location.reload();
            } else {
                showInfoToolBar(response.responseText);
            }

        },
        complete: cursor_clear
    });
}

function handleBrowserAreaSelection() {
    console.log("handleBrowserAreaSelection")

    //refresh the details area
    updateDetailsArea($('#filesForm').serialize());

    //Update based on the currently selected items
    updateActions(setToolbarActionElementEnabled);

}

/*
 * Initialize all toolbar buttons.  Stores a click handler that will be binded when
 * enabled and unbounded with disabled.   
 */

function initToolBar() {
    console.log("initToolBar");

    $('#toolbar-download').data('_click', function () {

        var baSelData = getBrowserAreaCheckedSelectionData();
        console.log("Toolbar download : " + stringifyJSON(baSelData));
        downloadFile(baSelData.path);
    });

    $('#toolbar-refresh').data('_click', function () {
        handleRefresh();
    });

    $('#toolbar-thumbnail').data('_click', function () {
        handleThumbnailMode();
    });
    
    $('#toolbar-list').data('_click', function () {
        handleThumbnailMode();
    });

    $('#toolbar-copy').data('_click', function () {
        copyFiles();
    });

    $('#toolbar-cut').data('_click', function () {
        cutFiles();
    });

    $('#toolbar-paste').data('_click', function () {
        pasteFiles();
    });

    $('#toolbar-new_folder').data('_click', function () {
        handleCreateDirectory();
    });

    $('#toolbar-new_file').data('_click', function () {
        handleCreateFile();
    });

    $('#toolbar-rename').data('_click', function () {
        handleRename();
    });

    $('#toolbar-delete').data('_click', function () {
        deleteFiles();
    });

    $('#toolbar-zip').data('_click', function () {
        downloadZip();
    });

    setToolbarActionElementEnabled("thumbnail", false);
    setUploadEnabled(false);
}


/*
 * Enables / disables toolbar menu items 
 * @param actionName
 * @param enabled
 */

function setToolbarActionElementEnabled(actionName, enabled) {
    //console.log("setToolbarActionElementEnabled(" + actionName + ", " + enabled + ")");
    var elem = $('#toolbar-' + actionName);
    if (elem.length == 0) {
        return;
    }
    
    var enabledCssClass = "enabled";
    var disabledCssClass = "disabled";
    
    if (enabled) {
        elem.addClass(enabledCssClass);
        elem.removeClass(disabledCssClass);
        elem.attr("title", elem.data("title"));

        //Restore the event (if it the element has been disabled at least once) ; otherwise
        //the click event should already be set
        if (elem.data("_click") != null) {
            elem.unbind("click.toolbarEvent");
            elem.bind("click.toolbarEvent", elem.data("_click"));
        }

    } else {
        elem.removeClass(enabledCssClass);
        elem.addClass(disabledCssClass);
        elem.data("title", elem.attr("title"));
        elem.removeAttr("title");

        elem.unbind("click.toolbarEvent");
    }


}

/*
 * The upload button is a js component that requires special handling
 * 
 */

function setUploadEnabled(enabled) {
    console.log("setUploadEnabled : " + enabled);
    var qqUploadButton = $('.qq-upload-button');

    if (enabled) {
        $('#file-uploader').find("input").removeAttr("disabled");
        qqUploadButton.addClass('enabled_upload');
        qqUploadButton.removeClass('disabled_upload');
    } else {
        $('#file-uploader').find("input").attr("disabled", true);
        qqUploadButton.removeClass('enabled_upload');
        qqUploadButton.addClass('disabled_upload');
    }
}


/*
 * Shows input box to create a file
 */

function handleCreateFile() {
    console.log("handleCreateFile");
    $("#folderOrFileChoice").val("file");
    $("#newDir").removeClass('esupHide');
    $("#browserMain").scrollTop($("#browserMain")[0].scrollHeight);
    $("#newFileOrFolderInput").focus();
}


/*
 * Shows input box to create a directory
 */

function handleCreateDirectory() {
    console.log("handleCreateDirectory");
    $("#folderOrFileChoice").val("folder");
    $("#newDir").removeClass('esupHide');
    $("#browserMain").scrollTop($("#browserMain")[0].scrollHeight);
    $("#newFileOrFolderInput").focus();
}

function newFileOrFolder(parentDir, name, fileOrFolder) {
    cursor_wait();
    console.log("newFileOrFolder name=[" + name + "] type: " + fileOrFolder);
    $.ajax({
        async: true,
        type: 'POST',
        url: '/esup-portlet-stockage/servlet-ajax/createFile',
        data: {
            "parentDir": parentDir,
            "title": name,
            "type": fileOrFolder,
            "sharedSessionId": sharedSessionId
        },
        success: function (r) {
            console.log("newFileOrFolder success");
            initializeBrowserMain(r);

            if (fileOrFolder == "folder") {
                //To show the new folder in the lt area
                refreshCurrentDirectory();
            }
        },
        error: function (r) {
            console.log("newFileOrFolder error");
            showInfoToolBar(r.responseText);
        },
        complete: cursor_clear
    });
}

function handleRefresh() {
    console.log("handleRefresh");

    //Blank out browser main for edge cases like when the currently viewed folder is deleted
    $("#browserMain").html("");
    $("#fileTree").jstree("refresh", -1);
    console.log("handleRefresh done ");
}

function refreshCurrentDirectory() {
    
    var path = $("#bigdirectory").attr('rel');
    
    console.log("refreshCurrentDirectory.  Path: " + path);
    
    var id = getLiIdFromPath(path);
    var obj = document.getElementById(id);
    $("#fileTree").jstree("refresh", obj);
    
    handleLeftTreeSelection(getJqueryObj(obj));
}

function getIsThumbnailMode() {
	//console.log("getIsThumbnailMode: " + $("#toolbar-thumbnail").is(":visible"));
	//return !$("#toolbar-thumbnail").is(":visible");
	return $("#thumbnail_mode").html() == "true";
	
}

function updateThumbnailIcons() {
	

    var thumbnailMode = getIsThumbnailMode();
    
	if (!thumbnailMode) {
    	$("#toolbar-thumbnail").show();
    	$("#toolbar-list").hide();
    } else {
    	$("#toolbar-thumbnail").hide();
    	$("#toolbar-list").show();
    }
}

function handleThumbnailMode() {
    console.log("handleThumbnailMode");

    var thumbnailMode = !getIsThumbnailMode();
    
    $.ajax({
        async: true,
        type: 'POST',
        url: '/esup-portlet-stockage/servlet-ajax/toggleThumbnailMode',
        data: {
            "sharedSessionId" : sharedSessionId,
            "thumbnailMode" : thumbnailMode
        },
        success: function (r) {
            console.log("handleThumbnailMode success : " + r);
            
            updateThumbnailIcons();
            
            var path = getCurrentDirectory();
            var liId = getLiIdFromPath(path);
            var liElem = $("#" + liId);
            handleLeftTreeSelection(liElem);
        },
        error: function (r) {
            console.log("handleThumbnailMode error");
        }
    });

}

/*
 * Shows the rename form
 */

function handleRename() {
    console.log("handleRename");
    var dirs = getCheckedDirs();
    if (dirs.length == 0) {
        console.log("handleRename: no dirs selected");
        $(".renameSpan").removeClass('esupHide');
    } else {
        console.log("handleRename: un hiding rename forms");
        $(".browsercheck:checked").each(function () {
        	var parent = getJqueryObj(getParentRow(this)); 
            var renameSpan = parent.find(".renameSpan");
            renameSpan.removeClass('esupHide');
            if (getIsThumbnailMode()) {
            	var link = parent.find("span.thumbnailLinkText");
                link.hide();
            } else {
            	var link = parent.find("a.file, a.fileTreeRef");
                link.hide();
            }
            
            console.log("handleRename: " + link);
        });
    }
}

function rename(parentDir, dir, title) {
    console.log("rename");
    cursor_wait();
    $.ajax({
        async: true,
        type: 'POST',
        url: '/esup-portlet-stockage/servlet-ajax/renameFile',
        data: {
            "parentDir": parentDir,
            "dir": dir,
            "title": title,
            "sharedSessionId": sharedSessionId
        },
        success: function (r) {
            initializeBrowserMain(r);

            //In case a folder was renamed, must refresh the LT as well
            refreshCurrentDirectory();
        },
        error: function (r) {
            console.log("Error in rename");
            showInfoToolBar(r.responseText);
        },
        complete: cursor_clear
    });
}

/*
 * Initiates AJAX request
 * 
 */

function downloadFile(fileName) {

    console.log("downloadFile. Path: " + fileName);

    var url = '/esup-portlet-stockage/servlet-ajax/downloadFile?dir=' + encodeURIComponent(fileName) + '&sharedSessionId=' + sharedSessionId;

    console.log(url);
    window.open(url);
}

function downloadZip() {
    console.log("downloadZip");
    var dirs = getCheckedDirs();
    if (dirs.length > 0) {
        var downloadZipUrl = '/esup-portlet-stockage/servlet-ajax/downloadZip';
        $("#filesForm").attr("action", downloadZipUrl);
        $("#filesForm").submit();
    }
}


function deleteFiles(dirsDataStruct) {
    console.log("deleteFiles");

    if (dirsDataStruct == null) {
        dirsDataStruct = $("#filesForm").serializeArray();
    }

    var ok_text = $('#delete-confirm-ok').html();
    var cancel_text = $('#delete-confirm-cancel').html();

    
    var dialogButtons = {};
    
    dialogButtons[ok_text] = function () {

        $(this).dialog("close");
        console.log("deleteFiles : " + stringifyJSON(dirsDataStruct));
        cursor_wait();
        var removeFilesUrl = '/esup-portlet-stockage/servlet-ajax/removeFiles';
        $.post(removeFilesUrl, dirsDataStruct, function (data) {
            cursor_clear();
            if (data.status) {
                showInfoToolBar(data.msg);
            } else {
                showInfoToolBar(data.msg);
            }
            refreshCurrentDirectory();
        });
    };
    
    dialogButtons[cancel_text] = function () {
        $(this).dialog("close");
    };
    
    $("#delete-confirm").dialog({
        resizable: false,
        height: 165,
        modal: true,
        buttons: dialogButtons
    });




}