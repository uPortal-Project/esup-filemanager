/*
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
( function($) {

"use strict";
var uploadListFadeoutTime = 3000;
var infoTolbarFadeoutTime = 8000;
var esupStockFixBlockSizes = $.browser.msie;
//esupStockFixBlockSizes = true;
var cursorWaitDialog = (useCursorWaitDialog == 'true');

var sortField = "titleAsc";

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


    $(document).ready(function () {

        /* GIP recia : START --> manage size of container */

      var treeAreaWidth = $('#arborescentArea').css("width");
      var treeAreaHeight = $('#arborescentArea').css("height");

      //In order for resizable and scrollbars to work together, we wrap the tree area in a div.
      // Technique is from :
      // http://stackoverflow.com/questions/3858460/jquery-ui-resizable-with-scroll-bars

      if (!esupStockFixBlockSizes) {

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
                       maxHeight: 600,
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

           // take care of resizing window
              $(window).resize(function() {
                console.log("Handler for .resize() called.");
                $("#leftArea").css("width", "30%");
                $("#browserArea").css("width", "67%");

                var finalWidthArbo = $("#leftArea").width();
                console.log("change width to detail and arborescence area " + finalWidthArbo);
                $("#detailArea").css("width", finalWidthArbo);
                $("#arborescentArea").parent().css("width", finalWidthArbo);

                //var finalHeightArbo = $("#leftArea").height()-$("#detailArea").height()-$("#arborescentAreaTitle").height();
                //console.log("change height to arborescence area " + finalHeightArbo);
                //$("#arborescentArea").parent().css("height", finalHeightArbo);
              });

      } else {
          $("#arborescentArea").css({overflow:'auto',
              width: $("#leftArea").width(),
              height:'320px'});
          $("#browserArea").css({overflow:'auto',
              height:'460px'});
      }

      $("#browserMain").css({
          width:'100%',
          height:'92%'});

        initJstree();

        var uploader = new esupQq.FileUploader({
            multiple: true,
            template: fileuploadTemplate,
            fileTemplate: fileTemplate,
            element: $("#file-uploader")[0],
            action: uploadFileURL,
            onSubmit: function (id, fileName) {
                uploader.setParams({
                    dir: $("#bigdirectory").attr("rel"),
                    qqfile: fileName
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



function handleDrop(dragElement, dropElement) {
  var sourcePath, targetPath;

  if (dropElement.parents("#arborescentArea").length > 0) {
    console.log("handleDrop: Target is left tree");
    targetPath = getPathFromLiNode(dropElement.parent("li"));
  } else if (dropElement.attr("id") == "browserMain") {
    console.log("handleDrop: Target is the current directory in the browser area");
    targetPath = dropElement.find("#bigdirectory").attr("rel");
  } else {
    console.log("handleDrop: Target is a directory in the browser area");
    targetPath = dropElement.find("a[rel]").attr("rel");
  }

  var sourceLeftTree = false;

    //Does the draggable come from the left pane or the right
    if (dragElement.parents("#arborescentArea").length > 0) {
      console.log("handleDrop: Came from left tree");
      sourcePath = getPathFromLiNode(dragElement.parent("li"));
      sourceLeftTree = true;
    } else if (dragElement.parents("#browserMain").length > 0) {
      console.log("handleDrop: Came from browser area");
      sourcePath = dragElement.find("a[rel]").attr("rel");
    } else {
      //If we don't know what has been drug, then stop, otherwise errors are likely
      console.log("handleDrop: Unknown source, possibly draggable ui dialog");
      return;
    }

  if (!isValidPaste(sourcePath, targetPath)) {
    console.log("Preventing drag and drop to current location");
    return;
  }

  if (sourceLeftTree && targetPath.indexOf(sourcePath) != -1) {
    console.log("Preventing drag/drop of base folder to a descendent folder");
    return;
  }

  //Detect if the copy is between 2 stockage areas or within the same

  //Basically, look for the 2nd ~ on both the source and target.  If the left part is the
  //same, we will do a cut / paste otherwise a copy / paste

  var sourceStockageArea = getStockageArea(sourcePath);
  var targetStockageArea = getStockageArea(targetPath);

  var isCopy = true;

  if (sourceStockageArea == targetStockageArea) {
    isCopy = false;
  }

  console.log("Source stockage area: [" + sourceStockageArea + "] target: [" + targetStockageArea + "] copying? " + isCopy);

    //Remove the revert option as this is a successful drop ; both the browser area and left tree area will be refreshed
  //after the doCutCopyPaste afterwhich this option will be reinstated
    $( ".draggable" ).draggable("option", "revert", false);

    doCutCopyPaste(isCopy, sourcePath, targetPath);

}



/**
 * Initializes drag / drop in browser area
 */
function initDragAndDrop() {

    $("#browserArea div.draggable").draggable({
        helper: 'clone',
        revert: true,
        start: function () {
            console.log("Right tree Drag Start ");
        },
        drag: function () {},
        stop: function () {
            console.log("Right tree Stop ");
        }
    });

    $("#browserArea div.droppable").droppable({
        //activeClass: "ui-state-hover",
        hoverClass: "ui-selected",
        greedy: true,
        drop: function (event, ui) {
            console.log("Drop event in right tree area");
            handleDrop(ui.draggable, $(this));
        }
    });

    $("#browserMain").droppable({
        //activeClass: "ui-state-hover",
        hoverClass: "ui-selected",
        greedy: false,
        drop: function (event, ui) {
            console.log("jQuery ui droppable event in browser area");
            handleDrop(ui.draggable, $(this));
        }
    });
}


function bindDragDropInLeftTree() {

  //Add css classes to flag drag/dropable

  //The folder nodes are contained within the drive nodes (the parents) so
  //we need to set "greedy" to make sure a drop in a folder takes precedence
  $('#arborescentArea [rel="folder"]').children("a").each(function (idx, val) {
        //console.log(val);
        var jqObj = $(val);
        jqObj.addClass("draggable");
        jqObj.addClass("droppable-greedy");
    });

  //Drives are only potential drop targets
  $('#arborescentArea [rel="drive"]').children("a").each(function (idx, val) {
        //console.log(val);
        var jqObj = $(val);
        jqObj.addClass("droppable");
    });

  //Add the drag / drop functionality
  $("#arborescentArea .draggable").draggable({ /*containment: ".portlet-section-body",*/
        helper: 'clone',
        revert: true,
        start: function () {
            console.log("Left Tree Drag Start");
        },
        drag: function () {
            //console.log("Drag");
        },
        stop: function () {
            console.log("Left Tree Drag Stop");
        }
    });

    $("#arborescentArea .droppable").droppable({
        //activeClass: "ui-state-hover",
        //hoverClass: "jstree-hovered",
        drop: function (event, ui) {
          console.log("Drop event in left tree area");
            handleDrop(ui.draggable, $(this));
        },
      stop: function(event, ui) {
        console.log("Drop stop event in left tree area");
      }
    });

    $("#arborescentArea .droppable-greedy").droppable({
        //activeClass: "ui-state-hover",
      hoverClass: "jstree-hovered",
        greedy: true,
        drop: function (event, ui) {
          console.log("Drop event (greedy) in left tree area");
            handleDrop(ui.draggable, $(this));
        }, stop: function(event, ui) {
        console.log("Drop stop event (greedy) in left tree area");
      }
    });
}

/**
 * Called by drag/drop to perform the file movements
 *
 */
function doCutCopyPaste(isCopy, sourcePath, targetPath) {

    console.log("doCutCopyPaste");

    var prepareFilesUrl = (isCopy ? prepareCopyFilesURL : prepareCutFilesURL);

    $.post(prepareFilesUrl, "dirs=" + encodeURIComponent(sourcePath), function (data) {
      console.log("prepareCopyFiles Ajax call completed.  Starting paste operation");
        cursor_clear();
        showInfoToolBar(data.msg);
        pasteToPath(targetPath);
        console.log("Paste ajax call completed.");
    });

}

function doCutOrCopy(isCopy) {

  var prepareFilesUrl = (isCopy ? prepareCopyFilesURL : prepareCutFilesURL);

  cursor_wait();
    var dirs = getCheckedDirs();
    console.log("doCutOrCopy(" + isCopy + ") dirs: " + dirs);

    $.ajax({
      url: prepareFilesUrl,
      data: $("#filesForm").serialize(),
      success: function (data) {
        console.log("Cut/copy succeeded");
        showInfoToolBar(data.hashMap.msg);
        $('#toolbar-paste').data("cutCopyFinished", true);
        $('#toolbar-paste').data("dirs", dirs);
      },
      error: function (data) {
        console.log("Cut/copy failed");
        $('#toolbar-paste').data("cutCopyFinished", false);
        $('#toolbar-paste').data("dirs", null);
      },
      complete: cursor_clear

    });
}

function copyFiles() {
    doCutOrCopy(true);
}


function cutFiles() {
    doCutOrCopy(false);
}


/*
 * Handler for the paste menu / toolbar item.
 */
function pasteFiles(obj) {

  var baSelData = getBrowserAreaCheckedSelectionData();

  var targetPath = null;

  //Paste into a singlely selected folder
    if (baSelData.singleFolderSelected == 1) {
      console.log("Pasting into selected folder");
      targetPath = baSelData.path;
    } else {
      console.log("Pasting into selected file's parent directory");
      //Paste into current Browser area directory
      targetPath = $("#bigdirectory").attr('rel');
    }

    pasteToPath(targetPath);
}

function canPaste(targetPath) {
  console.log("canPaste: " + targetPath);
  //First we must have cut/copied something
  if (!$('#toolbar-paste').data("cutCopyFinished")) {
    return false;
  }

  //Then the source must not be the same as target
  var dirs = $('#toolbar-paste').data("dirs");

    if (dirs == null || dirs.length == 0) {
      console.log("Dirs is not populated! Can't paste");
      return;
    }

    //To check validity, any of the source will work as they are all in the same directory as it is impossible to select files from multiple levels
    var sourcePath = dirs[0];

  return isValidPaste(sourcePath, targetPath);
}

function pasteToPath(path) {
  console.log("Entering pasteToPath.  Path: " + path);
    cursor_wait();

    var pastFilesUrl = pastFilesURL;
    $.ajax({
      url: pastFilesUrl,
      data: "dir=" + encodeURIComponent(path),
      async: true,
      success: function (data) {
        console.log("Past ajax call completed.");

        $('#toolbar-paste').data("cutCopyFinished", false);

          if (data.status) {
              showInfoToolBar(data.msg);
          } else {
              showInfoToolBar(data.msg);
          }

          console.log("pasteToPath(" + path + ")");

          console.log("Selecting left tree node after paste");
          //Select the Node in the left tree ; the path is also the id of the dom element
          //In the case of cut / paste, need to make sure the left tree has been refreshed
          openAndSelectLiNode(path, true);

          console.log("Selecting done");
      },
      complete: cursor_clear

    });
}

function isValidPaste(sourcePath, targetPath) {
  console.log("isValidPaste : Checking validity of paste from " + sourcePath + " to " + targetPath);
  //Detect if the source is already part of target
  var parentSourcePath = getParentPath(sourcePath);

  if (targetPath == parentSourcePath) {
    console.log("isValidPaste : Source is already part of target");
    return false;
  }

  if (sourcePath == targetPath) {
    console.log("isValidPaste : Preventing drag and drop to self");
    return false;
  }

  return true;
}

function initJstree() {
   //When a node is opened, ensure all the drag/drop functionality
    //is binded.
    $("#fileTree").bind("loaded.jstree", function (event, data) {
        console.log("JSTree loaded.jstree event");
        //bindDragDropInLeftTree();
    }).bind("open_node.jstree", function (event, data) {
      var treeNode = data.rslt.obj;
      var path = getPathFromLiNode(treeNode);
        console.log("JSTree open_node event on node " + path + " Binding drag/drop to folders -- Folders present : " + $('#arborescentArea [rel="folder"]').length);

      //In case this is the first time the node is opened, we make sure that drag/drop has been initialized
        bindDragDropInLeftTree();
    }).bind('select_node.jstree', function (e, data) {
        handleLeftTreeSelection(data.rslt.obj);
    }).bind('reopen.jstree', function () {
      if($("#fileTree").jstree("ui").data.ui.to_select.length == 0)
        $("#fileTree").jstree("ui").data.ui.to_select = [getLiIdFromPath(defaultPath)];
    }).jstree({
        // the list of plugins to include
        //"plugins" : [ "themes", "json_data", "ui", "crrm", "cookies", "dnd", "search", "types", "hotkeys", "contextmenu"],
        /* GIP RECIA : add plugins "contextmenu", "dnd" */
        "plugins": ["themes", "json_data", "ui", "crrm", "cookies", "types", "contextmenu"],
        // Plugin configuration
        "contextmenu": {
            "items": customMenu
        },

        // I usually configure the plugin that handles the data first - in this case JSON as it is most common
        "json_data": {
            // I chose an ajax enabled tree - again - as this is most common, and maybe a bit more complex
            // All the options are the same as jQuery's except for `data` which CAN (not should) be a function
            "ajax": {
                // the URL to fetch the data
                "url": fileChildrenURL,
                "type": 'POST',
                // this function is executed in the instance's scope (this refers to the tree instance)
                // the parameter is the node being loaded (may be -1, 0, or undefined when loading the root nodes)
                "data": function (n) {

                    // the result is fed to the AJAX request `data` option
                    var retData = {
                        "dir": n == -1 ? defaultPath : $.data(n.get(0), "encPath"),
                        "hierarchy": n == -1 ? "all" : "",
                    };

                    console.log("Jtree JSON data function : " + stringifyJSON(retData) + " loading node data ");

                    return retData;
                },
                "success": function (data, textStatus, jqXHR) {
                    console.log("JSTree ajax data loaded");
                    bindDragDropInLeftTree();
                },
                "error": function (response) {
                    if(response.responseText != "[]") {
                        console.log("filechildren failed");
                        showDialogError(response.responseText);
                    }
                }
            }
        },

        // the UI plugin - it handles selecting/deselecting/hovering nodes
        "ui": {
            // this makes the node with ID node_4 selected onload
            "select_multiple_modifier": false,
            "select_limit": 1
        },



        // Using types - most of the time this is an overkill
        // Still meny people use them - here is how
        "types": {
            // I set both options to -2, as I do not need depth and children count checking
            // Those two checks may slow jstree a lot, so use only when needed
            "max_depth": -2,
            "max_children": -2,
            // I want only `drive` nodes to be root nodes
            // This will prevent moving or creating any other type as a root node
            "valid_children": ["drive"],
            "types": {
                // The default type
                "default": {
                    // I want this type to have no children (so only leaf nodes)
                    // In my case - those are files
                    "valid_children": "none"
                    // If we specify an icon for the default type it WILL OVERRIDE the theme icons
                },
                // The `folder` type
                "folder": {
                    // can have files and other folders inside of it, but NOT `drive` nodes
                    "valid_children": ["default", "folder"]
                },
                // The `drive` nodes
                "drive": {
                    // can have files and folders inside, but NOT other `drive` nodes
                    "valid_children": ["default", "folder"],
                    // those options prevent the functions with the same name to be used on the `drive` type nodes
                    // internally the `before` event is used
                    "start_drag": false,
                    "move_node": false,
                    "delete_node": false,
                    "remove": false
                }
            }
        }

    });

}


function customMenu(node) {
    console.log("customMenu");

    //Select the node on right click
    $("#fileTree").jstree("select_node", node, true);

    // The default set of all items
    var items = {

        "refresh": {
            // The item label
            "label": "Rafraichir",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Refresh");
                $("#fileTree").jstree("refresh", obj);
            },
            // All below are optional
            "_disabled": false,
            "separator_before": false,
            "separator_after": true,
            // false or string - if does not contain `/` - used as classname
            "icon": "/esup-filemanager/img/flaticons/refresh_16px.png"
        },
        "upload": {
            // The item label
            "label": $('.qq-upload-button').html(),
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Transférer");

                //Do exactly the same as if the user clicked on the transfer button
                var elem = $('#file-uploader input');
                elem.click();
            },
            // All below are optional
            "_disabled": false,
            // clicking the item won't do a thing
            "_class": "class",
            // class is applied to the item LI node
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/upload_16px.png"
        },

        "newfolder": {
            // The item label
            "label": $('#toolbar-new_folder').html(),
            // The function to execute upon a click
            "action": function (obj) {
                console.log("New folder");
                handleCreateDirectory();
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/new_folder_16px.png"
        },
        "newfile": {
            // The item label
            "label": $('#toolbar-new_file').html(),
            // The function to execute upon a click
            "action": function (obj) {
                console.log("New file");
                handleCreateFile();
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/new_file_16px.png"
        },
        "pasteItem": {
            // The item label
            "label": $('#myMenu li.paste a').html(),
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Paste : " + obj);
                pasteFiles();
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/paste_16px.png"
        }
        /*,
        "downloadItem": {
            // The item label
            "label": "Télécharger",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Download : " + obj);

            },
            "_disabled": false,
            "separator_before": true,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/download_16px.png"
        },
        "zipItem": {
            // The item label
            "label": "Zip",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Zip : " + obj);
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/delete_16px.png"
        },
        "copyItem": {
            // The item label
            "label": "Copier",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Copy : " + obj);

            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/copy_16px.png"
        },
        "cutItem": {
            // The item label
            "label": $('#myMenu li.cut a').html(),
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Delete : " + obj);
                deleteFiles({
                    "dirs": obj.attr("id")
                });
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/cut_16px.png"
        },

        "renameItem": {
            // The item label
            "label": "Renommer",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Rename : " + obj);
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/rename_16px.png"
        },
        "deleteItem": {
            // The item label
            "label": "Supprimmer",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Delete : " + obj);
                deleteFiles({
                    "dirs": obj.attr("id")
                });
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-filemanager/img/flaticons/delete_16px.png"
        }*/
    };


/*
    delete items.deleteItem;
    delete items.downloadItem;
    delete items.zipItem;
    delete items.copyItem;
    delete items.cutItem;
    delete items.renameItem;*/

    if (!$('#toolbar-paste').data("cutCopyFinished")) {
      delete items.pasteItem;
    }

    //TODO

    return items;
}

/**
 * Returns path (unique file/path) from an LI node in the left tree
 */

function getPathFromLiNode(treeNode) {
    //console.log("getPathFromLiNode: TreeNode : " + treeNode.html() + " Data :" + $.data(treeNode.get(0), "encPath"));

    return $.data(treeNode.get(0), "encPath");
}

function getTypeFromLiNode(treeNode) {
  return $.data(treeNode.get(0), "type");
}

function getLiIdFromPath(path) {
  return path;
}

/*
 * Given a path, returns its parent path.
 */
function getParentPath(path) {

    var parentPath = '';

    $.ajax({
        async: false,
        type: 'POST',
        url: getParentPathURL,
        data:  {
            "dir": path
        },
        success: function (parentPathResp) {
            parentPath = parentPathResp;
        },
        error: function (response) {
            console.log("getParentPath failed");
            $("#detailArea").html("");
            showDialogError("getParentPath failed : " + response.responseText);
        }
    });

    return parentPath;
}

/*
 * getStockageArea("FS:Shared~bob2~images/galerie/htdhtd") ==
 *   "FS:Shared~bob2"
 *
 *   TODO ...
 */
function getStockageArea(path) {
    var tokens = path.split(/[~\/:]/);

    var parentPath = path;

    if (tokens.length >= 2) {
        return tokens[0] + ":" + tokens[1] + "~" + tokens[2];
    }

    console.log("Unknown stockage area for path : " + path);
    return "";
}

/**
 * Opens and then selects an LI Node
 */
function openAndSelectLiNode(path, withRefresh) {


    var parentPath = getParentPath(path);

    console.log("openAndSelectLiNode : " + parentPath + ", " + path);

    if (!parentPath) {
      //no need to open any nodes
      $("#fileTree").jstree("select_node", getLiNodeFromPath(path), true);
      if (withRefresh) {
        refreshCurrentDirectory();
      }
    } else {
      var parentNode = getLiNodeFromPath(parentPath);
      //console.log("openAndSelectLiNode.  parentNode : " + getJqueryObj(parentNode).html() );
      $("#fileTree").jstree("open_node", parentNode, function () {
        var nodeToSelect = getLiNodeFromPath(path);
            console.log("Selecting node after open " + path + " node: " + getJqueryObj(nodeToSelect).html());
            $("#fileTree").jstree("select_node", nodeToSelect, true);
            if (withRefresh) {
              //Refreshes left tree
            $("#fileTree").jstree("refresh", nodeToSelect);
          }
        });
    }


}


/**
 *
 * @param path
 * @returns Dom LI node from a path
 */
function getLiNodeFromPath(path) {
    var id = getLiIdFromPath(path);
    var node = $('#' + id);

    if (node && node.length > 0) {
        return node.get(0);
    }

    console.log("Node not found for path [" + path + "] id " + id);

    //Root node
    return -1;
}/*
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

    cursor_wait();

    $.ajax({
        async: true,
        type: 'POST',
        url: htmlFileTreeURL,
        data: {
            "dir": path,
            "sortField": sortField
        },
        success: function (r) {
            console.log("handleLeftTreeSelection: htmlFileTree ajax call succeeded");
            initializeBrowserMain(r);

            var needsAuthentication = $("#browserMain #authenticationForm").length > 0;

            var type = getTypeFromLiNode(treeNode);
            if (type != "root" && type != "category" && !needsAuthentication) {
                //refresh the details area
                updateDetailsArea({
                    "dirs": path,
                    "type": type
                });

            } else {
                $("#detailArea").html("");
            }

        var writeable = $("#browserMain div.breadcrumbs > .writeable").html() === "true";

            //Disable/Enable toolbar items
            setToolbarActionElementEnabled('refresh', true);
            setToolbarActionElementEnabled('thumbnail', !needsAuthentication);
            setToolbarActionElementEnabled('list', !needsAuthentication);
            setUploadEnabled(!needsAuthentication && writeable);

            setToolbarActionElementEnabled('new_folder', !needsAuthentication && writeable);
            setToolbarActionElementEnabled('new_file', !needsAuthentication && writeable);

            setToolbarActionElementEnabled("download", false);
            setToolbarActionElementEnabled("zip", false);
            setToolbarActionElementEnabled("copy", false);
            setToolbarActionElementEnabled("cut", false);
            setToolbarActionElementEnabled("paste", writeable && canPaste(path));
            setToolbarActionElementEnabled("rename", false);
            setToolbarActionElementEnabled("delete", false);

        },
        error: function (r) {
            console.log("Left tree node refresh ajax call failed");
            $("#browserMain").html("");
            if (r.statusText == 'reload') {
                location.reload();
            } else {
                showDialogError(r.responseText);
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
        url: detailsAreaURL,
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
                showDialogError(response.responseText);
            }

        },
        complete: cursor_clear
    });
}

function showDialogError(error) {
     console.log("showDialogError");
     console.log("error : " + error);

     var dialogElem = $("#errorDialog");

     if (dialogElem.dialog("isOpen")) {
         console.log("errorDialog already open");
     }
     dialogElem.html(error);
     dialogElem.dialog({
         modal: true,
         resizable: true,
         closeOnEscape: true
     });
}

function handleBrowserAreaSelection() {
    console.log("handleBrowserAreaSelection");

    //refresh the details area
    updateDetailsArea($('#filesForm').serialize());

    //Update toolbar based on the currently selected items
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
        url: createFileURL,
        data: {
            "parentDir": parentDir,
            "title": name,
            "type": fileOrFolder
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
        url: toggleThumbnailModeURL,
        data: {
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
            renameSpan.find("input").focus();
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
        url: renameFileURL,
        data: {
            "parentDir": parentDir,
            "dir": dir,
            "title": title
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

    var url = /\?/.test(downloadFileURL) ? downloadFileURL + '&' : downloadFileURL + '?';
    url = url + 'dir=' + encodeURIComponent(fileName);

    console.log(url);
    //window.open(url);
    window.location.href = url;
}

function downloadZip() {
    console.log("downloadZip");
    var dirs = getCheckedDirs();
    if (dirs.length > 0) {
        var downloadZipUrl = downloadZipURL;
        $("#filesForm").attr("action", downloadZipUrl);
        $("#filesForm").submit();
    }
}

$.downloadZip = function() { downloadZip(); };


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
        var removeFilesUrl = removeFilesURL;
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

  /**
   * Gets the container object.  In the thumbnail view, it is a div,
   * in the normal view, it is a td.
   *
   * Returns the parents as a jQuery result set
   * @param obj
   * @returns
   */

  function getParentRow(obj) {
      //console.log("getParentRow " + obj);
    var jqObj = getJqueryObj(obj);
      //In thumbnail mode, the parent 'row' is just a div
      var trParent = jqObj.closest("tr", $("#jqueryFileTree")[0]);

      if (trParent.length > 0) {
          return trParent;
      }

      var divParent = jqObj.closest("div.thumbnail");

      if (divParent.length == 0) {
          console.log("Div parent not found!!");
      }

      return divParent;
  }

  function getPathFromDomElem(obj) {
    var parent = getParentRow(obj);
    var link = parent.find("a.file, a.fileTreeRef, a.fileCatRef");
    return link.attr("rel");
  }

  function getCurrentDirectory() {
    return $("#bigdirectory").attr('rel');
  }

  /**
   *
   * @param obj a jquery object
   * @returns a jquery object wrapping the checkbox
   */
  function getCheckbox(obj) {
      //console.log("getCheckbox " + obj)
      var parentObj = getParentRow(obj);
      var checkbox = parentObj.find("input.browsercheck");
      return checkbox;
  }

  /**
   *
   * @param obj A dom element
   * @returns the jquery object representing which element is highlighted / selected
   */
  function getSelectableObject(obj) {
    var parent = getParentRow(obj);

    if (getIsThumbnailMode()) {
      return parent.find("span.thumbnailLinkText");
    } else {
      return parent;
    }
  }

  /**
   *
   * @param obj a jquery object that is a child of the row / thumbnail div
   */
  function selectObject(obj, updateBrowserArea) {
      console.log("select object");

      if (getJqueryObj(obj).hasClass("fileCatRef")) {
        console.log("Not selecting drive / category");
        return;
      }

      //Store in case of shift selection
      $("#browserArea").data("lastItemChecked", getJqueryObj(obj));

      var wasUnselected = !isChecked(obj);

      getSelectableObject(obj).addClass('ui-selected');

      getCheckbox(obj).attr('checked', 'checked');
      if (wasUnselected && updateBrowserArea) {
          handleBrowserAreaSelection();
      }

  }

  function clearSelections() {
    var checkedElements = $('input.browsercheck', $('#jqueryFileTree'));
      console.log("Removing checkbox from " + checkedElements.length + " elements");
      checkedElements.each(function (idx, elem) {
        //Not necesary to update browser area since we will select an item and update it
          deselectObject(elem, false);
      });
  }



  /**
   *
   * @param obj
   * @returns if a row / thumbnail div is selected
   */
  function isChecked(obj) {
    var selObj = getSelectableObject(obj);
      return selObj.hasClass('ui-selected');
  }

  function deselectObject(obj, updateBrowserArea) {
      //console.log("deselect object");
      var wasSelected = isChecked(obj);
      getSelectableObject(obj).removeClass('ui-selected');
      getCheckbox(obj).removeAttr('checked');

      if (wasSelected && updateBrowserArea) {
          handleBrowserAreaSelection();
      }
  }


  function initContextMenu() {

      var thumbnailItems = $("#jqueryFileTree div.thumbnail");

      console.log("initContextMenu : init browser area context menu, thumbnail items : " + thumbnailItems.length);
      // Show menu when a list item is clicked
      $("#jqueryFileTree FORM table tr, #jqueryFileTree div.thumbnail").contextMenu({
          menu: 'myMenu'
      }, function (action, el, pos) {
          switch (action) {
          case 'download':
              downloadFile(el.find("a[class='file']").attr("rel"));
              break;
          case 'copy':
              copyFiles();
              break;
          case 'cut':
              cutFiles();
              break;
          case 'paste':
              pasteFiles();
              break;
          case 'delete':
              deleteFiles();
              break;
          case 'zip':
              downloadZip();
              break;
          case 'rename':
              handleRename();
              break;
          default:
              console.log("Context menu action not found");
          }

      }, function (el) {
          console.log("Context menu callback");
          console.log(el);

          //Enable / disable browser area (not the jsTree on the left) context menu items using same method as toolbar
          updateActions(function (actionName, enabled) {

              var elem = $("#myMenu li." + actionName);
              if (elem.length == 0) {
                  console.log("No BA menu items! " + actionName);
                  return;
              }
              if (enabled) {
                  elem.show();
              } else {
                  elem.hide();
              }
          });

      });

      $("#jqueryFileTree FORM table tr, #jqueryFileTree div.thumbnail").bind('contextmenu', function() {
        console.log("item  context menu");
        return false;
        });

      $("body").bind('contextmenu', function() {
        console.log("Body  system context menu ");
        return false;
        });
  }

  function handleItemDblClick(e, jqElem) {
    if (!jqElem) {
      jqElem =  $(this);
    }


    console.log("Item Double clicked " + jqElem.html() );

    //Cancel singe click action
    var singleClickFuncId = jqElem.data("singleClickFuncId");
    if (singleClickFuncId) {
      clearTimeout(singleClickFuncId);
      jqElem.data("singleClickFuncId", null);
    }

    //Reset click counter
    jqElem.data("clicks", 0);

    //Perform double click action
    if (jqElem.hasClass("file")) {
      handleFileDoubleClick(jqElem, e);
    } else if (jqElem.hasClass("fileTreeRef") || jqElem.hasClass("fileCatRef")) {
      handleFolderDoubleClick(jqElem, e);
    }
  }

  function handleItemDblOrOneClick(e) {

        /*Because of limitations with jQuerys double click, we are
          obliged to get a bit fancy in order to have a distinct
          action on both the click and double click events*/

        var jqElem = $(this);

        //Retrieve how many times this element has been clicked
        var clicks = 1 + (jqElem.data("clicks") ? jqElem.data("clicks") : 0);

        //Store after incrementing
        jqElem.data("clicks", clicks);

        console.log("Item clicked " + jqElem.html() + " clicks : " + clicks);

        if (clicks == 1) {
          //Start a timeout function.  If we get another click in time, it will
          //be canceled
            var func = function() {
                console.log("Executing single click, should be later");
                handleItemClick(e,jqElem);
            };

          console.log("Starting timer for single click.");
          var singleClickFuncId = setTimeout(func, 300);

          //Store timeout function id in case we need to cancel it
          jqElem.data("singleClickFuncId", singleClickFuncId);
        }

        return false;
      }


  function handleItemClick(e,jqElem) {

      if (!jqElem) {
           jqElem =  $(this);
      }

      //Reset click counter
      jqElem.data("clicks", 0);
      jqElem.data("singleClickFuncId", null);

      console.log("Browser area click");
      handleItemSelection(jqElem, e);

      //hide the context menu
      $(".contextMenu").hide();
  }

  function handleFolderDoubleClick(elemClicked, event) {
    console.log("handleFolderDoubleClick");
    var path = elemClicked.attr('rel');

      var liId = getLiIdFromPath(path);

      console.log("Directory double clicked " + path + ", " + liId);

      //Select the node in the left tree
      openAndSelectLiNode(path);
  }

  function handleFileDoubleClick(elemClicked, event) {
    console.log("handleFileDoubleClick");
      var id = elemClicked.attr('rel');
      downloadFile(id);
  }

  function handleShiftSelection(elemClicked, event) {

    console.log("Shift key pressed");
    var selector = "#browserArea .file, #browserArea .fileTreeRef";
    var selectableItems = $(selector);

    var lastItemChecked = $("#browserArea").data("lastItemChecked");
    if (!lastItemChecked) {
      lastItemChecked = getJqueryObj(selectableItems[0]);
    }

    var start =  elemClicked.index(selector);
    var end = lastItemChecked.index(selector);
    //var start = jQuery.inArray(elemClicked, selectableItems);
    //var end = jQuery.inArray(lastItemChecked, selectableItems);

    clearSelections();

    for(var i=Math.min(start,end);i<=Math.max(start,end);i++) {
          selectObject(selectableItems[i], false);
    }
    $("#browserArea").data("lastItemChecked", getJqueryObj(selectableItems[end]));

    console.log("Start: " + start + " end: " + end);
    handleBrowserAreaSelection();
    return;

  }

  /*
   * Used by both left and right clicks to update the set of currently
   * selected items
   */
    function handleItemSelection(elemClicked, event) {
        console.log("handleItemSelection " + elemClicked);

        var isElemChecked = isChecked(elemClicked);

    if(event.which !== 3 || !isElemChecked) {

        if (event.ctrlKey) {
            console.log("Ctrl key pressed");
        } else if (event.shiftKey) {
          return handleShiftSelection(elemClicked, event);
        } else {

            console.log("Ctrl key not pressed");

            //Remove all checkboxes
            clearSelections();
        }

        if (isElemChecked) {
            console.log("Current row selected");
            // to resolve the bug of selection after the clearSelection we reselect the element to refresh the page with a deselectObject
            selectObject(elemClicked, false);
            deselectObject(elemClicked, true);
        } else {
            console.log("Current row not selected");
            selectObject(elemClicked, true);
        }

    }
    }

    $.handleItemSelection = function(elemClicked, event) {handleItemSelection(elemClicked, event);};

  /**
   * Initializes context menu events
   */
  function initEvents() {


      $('#browserMain div.breadcrumbs a.fileTreeRefCrumbs').bind('click', function () {
          var path = $(this).attr('rel');
          openAndSelectLiNode(path);
      });

      $('#browserMain div#jqueryFileTree thead a.sortTable').bind('click', function () {
          sortField = $(this).attr('rel');
          var path = $("#bigdirectory").attr('rel');
          openAndSelectLiNode(path);
      });

      $('#newFileOrFolderSubmit').bind('click', function () {
          newFileOrFolder($("#bigdirectory").attr('rel'), $("#newFileOrFolderInput").val(), $("#folderOrFileChoice").val());
      });

      $('#newFileOrFolderInput').keyup(function (e) {
          if (e.keyCode == 13) {
              newFileOrFolder($("#bigdirectory").attr('rel'), $("#newFileOrFolderInput").val(), $("#folderOrFileChoice").val());
          }
      });

      $('#jqueryFileTree a.renameSubmit').bind('click', function () {
          rename(getCurrentDirectory(), getPathFromDomElem(this), $(this).prev().val());
      });

      $('#jqueryFileTree input.renameInput').keyup(function (e) {
          if (e.keyCode == 13) {
              rename(getCurrentDirectory(), getPathFromDomElem(this), $(this).val());
          }
      });


      //Though not as clear, it is faster to select the items this way
      //var selector = '#jqueryFileTree a.draggable';
      var selector = '#jqueryFileTree a.file, #jqueryFileTree a.fileTreeRef, #jqueryFileTree a.fileCatRef';

      var elems = $(selector);



      if(useDoubleClick == "false") {
          console.log("Binding single click like dbleClick");
          elems.bind('click', handleItemDblClick);
      } else {
          console.log("Binding single click");
          elems.bind('click', handleItemDblOrOneClick);
          console.log("Binding double click");
          elems.bind('dblclick', handleItemDblClick);
        }

      $("#jqueryFileTreeBody").selectable({
          cancel: 'a,span,#newFileOrFolderInput',
          filter: '.selectable',
          stop: function () {
            console.log("jquery ui selectable : stop event");

              if ($(this).attr("id") == "jqueryFileTreeBody") {
                  console.log("Not selecting container obj");

                  //Make sure Details area is up to date
                  handleBrowserAreaSelection();
                  return;
              }
              //refresh and manage checkbox value
              selectObject(this, true);
              //hide the context menu
              $(".contextMenu").hide();

          },
          create: function () {
              // On cache les checkboxs une fois le plugin initialisé
              $(".browsercheck", this).hide();
          },
          selecting: function (event, ui) {
            //Only update once we are done
              console.log("jquery ui selectable : Selecting event");
              selectObject(ui.selecting, false);
          },
          unselecting: function (event, ui) {
            console.log("jquery ui selectable : Unselecting event");
            //Only update once we are done
              deselectObject(ui.unselecting, false);
          }
      });
  }

  function initializeBrowserMain(innerHtml) {
    if (innerHtml != null) {
      $("#browserMain").html(innerHtml);
    }
      console.log("initializeBrowserMain");

      if (!esupStockFixBlockSizes) {
        $("#jqueryFileTree").css({display:'block', overflow:'auto',
            height: '100%',
            width: '100%'});
      }

      initDragAndDrop();
      initContextMenu();
      initEvents();

      updateThumbnailIcons();
  }



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
    $("body").css("cursor", "wait");

    if(cursorWaitDialog) {
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
}

// Returns the cursor to the default pointer

function cursor_clear() {
    console.log("Cursor clear");
    $("body").css("cursor", "auto");
    if(cursorWaitDialog) {
      $("#waitingDialog").dialog('close');
    }
}




function showInfoToolBar(msg) {
    console.log("showInfoToolBar");
    $("#info-toolbar").html(msg);
    /*$("#info-toolbar span").show('blind', {
        direction: 'vertical'
    }, 2000);*/
    setTimeout(function() { hideInfoToolBar(); }, 4000);
    $("#info-toolbar").bind('click', function () {
      hideInfoToolBar() ;
    });
}

function hideInfoToolBar() {
    console.log("hideInfoToolBar");
    /*$("#info-toolbar span").hide('blind', {
        direction: 'vertical'
    }, 2000);*/
    $("#info-toolbar").html("<span>...</span>");
}

$.hideInfoToolBar = function() {hideInfoToolBar();};


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
        var obj = document.getElementById(fileId);
        $("#fileTree").jstree("select_node", obj, true);
    }
}

/**
 * Utility function to get the jquery object.
 * @param elem
 * @returns
 */
function getJqueryObj(elem) {

  if (elem instanceof $) {
    //Already a jquery object
    return elem;
  } else {
    //A dom element
    return $(elem);
  }
}



function authenticate(dir, username, password) {
    console.log("authenticate");
    cursor_wait();
    $.ajax({
        async: true,
        type: 'POST',
        url: authenticateURL,
        data: {
            "dir": dir,
            "username": username,
            "password": password
        },
        success: function (data) {
            showInfoToolBar(data.msg);
            if (data.status) getFile(null, dir);
            refreshCurrentDirectory();
        },
        complete: cursor_clear
    });
}

$.authenticate = function(dir, username, password) { authenticate(dir, username, password); };


// keyboard events

  var isCtrl = false;
  var isShift = false;

  $(document).keyup(function(e) {
    if(e.which == 17) isCtrl=false;
    if(e.which == 16) isShift=false;
  });

  $(document).keydown(function(e) {
    if ($("#browserArea #authenticationForm").length) {return;}
    if ($(".renameSpan:not(:hidden)").length) {return;}
    if(e.which == 17) { isCtrl=true; return; }
    if(e.which == 16) { isShift=true; return; }

    if(e.which == 86 && isCtrl) {
      console.log("Ctrl-v key pressed");
      pasteFiles();
      return;
    }


    if (e.which == 9) {
        console.log("Tab key pressed");
        e.preventDefault();
        clearSelections();
        var selector = "#browserArea .file, #browserArea .fileTreeRef";
        var selectableItems = $(selector);
        selectObject(getJqueryObj(selectableItems[0]), true);
        return;
    }

    var dirs = getCheckedDirs();
    if (dirs == null || dirs.length == 0) {
      return;
    }

    switch (e.which) {
    case 67:
      if(isCtrl) {
    console.log("Ctrl-c key pressed");
    copyFiles();
      }
      break;
    case 88:
      if(isCtrl) {
    console.log("Ctrl-x key pressed");
    cutFiles();
      }
      break;
    case 46:
      console.log("Suppr key pressed");
      deleteFiles();
      break;
    case 113:
      console.log("F2 key pressed");
      handleRename();
      break;
    case 13:
      // if e.target.nodeName  != BODY maybe we're on dialog / form, etc ...
      if(e.target.nodeName == 'BODY') {
        console.log("Enter key pressed");
        var baSelData = getBrowserAreaCheckedSelectionData();
        if(baSelData.singleFileSelected) {
          downloadFile(baSelData.path);
        } else if (baSelData.singleFolderSelected) {
          openAndSelectLiNode(baSelData.path);
    }
      }
      break;
    case 38:
        console.log("Up key pressed");
        e.preventDefault();
        var selector = "#browserArea .file, #browserArea .fileTreeRef";
        var selectableItems = $(selector);

        var lastItemChecked = $("#browserArea").data("lastItemChecked");
        if (!lastItemChecked) {
          lastItemChecked = getJqueryObj(selectableItems[0]);
        }
        var i = lastItemChecked.index(selector);
        if(i > 0) {
            if(!isShift)
                deselectObject(selectableItems[i], false);
            selectObject(selectableItems[i-1], false);
        }
        handleBrowserAreaSelection();
        break;
    case 40:
        console.log("Down key pressed");
        e.preventDefault();
        var selector = "#browserArea .file, #browserArea .fileTreeRef";
        var selectableItems = $(selector);

        var lastItemChecked = $("#browserArea").data("lastItemChecked");
        if (!lastItemChecked) {
          lastItemChecked = getJqueryObj(selectableItems[0]);
        }
        var i = lastItemChecked.index(selector);
        if(selectableItems.length > i+1) {
            if(!isShift)
                deselectObject(selectableItems[i], false);
            selectObject(selectableItems[i+1], false);
        }
        handleBrowserAreaSelection();
        break;
    }

    return;

  });


})(jQuery);



function disableEnterKey(e) {
    var key;
    if (window.event) key = window.event.keyCode; //IE
    else key = e.which; //firefox
    return (key != 13);
}
