function initJstree() {
	 //When a node is opened, ensure all the drag/drop functionality
    //is binded.  
    $("#fileTree").bind("loaded.jstree", function (event, data) {
        console.log("JSTree loaded.jstree event");
        //bindDragDropInLeftTree();
    }).bind("open_node.jstree", function (event, data) {
    	var treeNode = data.rslt.obj;
    	var path = getPathFromLiNode(treeNode)
        console.log("JSTree open_node event on node " + path + " Binding drag/drop to folders -- Folders present : " + $('#arborescentArea [rel="folder"]').length);

    	//In case this is the first time the node is opened, we make sure that drag/drop has been initialized
        bindDragDropInLeftTree();
    }).bind('select_node.jstree', function (e, data) {
        handleLeftTreeSelection(data.rslt.obj);
    }).jstree({
        // the list of plugins to include
        //"plugins" : [ "themes", "json_data", "ui", "crrm", "cookies", "dnd", "search", "types", "hotkeys", "contextmenu"],
        /* GIP RECIA : add plugins "contextmenu", "dnd" */
        "plugins": ["themes", "json_data", "ui", "crrm", "cookies", "types", "contextmenu"],
        // Plugin configuration
        /* GIP RECIA : START --> Configuration of contextmenu plugin */
        "contextmenu": {
            "items": customMenu


        },
        /* GIP RECIA : END --> Configuration of contextmenu plugin */

        // I usually configure the plugin that handles the data first - in this case JSON as it is most common
        "json_data": {
            // I chose an ajax enabled tree - again - as this is most common, and maybe a bit more complex
            // All the options are the same as jQuery's except for `data` which CAN (not should) be a function
            "ajax": {
                // the URL to fetch the data
                "url": '/esup-portlet-stockage/servlet-ajax/fileChildren',
                "type": 'POST',
                // this function is executed in the instance's scope (this refers to the tree instance)
                // the parameter is the node being loaded (may be -1, 0, or undefined when loading the root nodes)
                "data": function (n) {

                    // the result is fed to the AJAX request `data` option
                    var retData = {
                        "dir": n == -1 ? "" : jQuery.data(n.get(0), "path"),
                        "sharedSessionId": sharedSessionId
                    };

                    console.log("Jtree JSON data function : " + stringifyJSON(retData) + " loading node data ");

                    return retData;
                },
                "success": function (data, textStatus, jqXHR) {
                    console.log("JSTree ajax data loaded");
                    
                    //Save the path ==> id mappings in order to be able to look up the ids from paths later on
                    
                    $.each(data, function(idx, value) {
                    	addPathToIdEntry(value.metadata.path, value.attr.id);
                    	
                    	if (value.children) {
                    		$.each(value.children, function(idxChild, childValue) {
	                        	addPathToIdEntry(childValue.metadata.path, childValue.attr.id);
	                        });
                    	
                    	}
                    });
                    
                    bindDragDropInLeftTree();
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
            "icon": "/esup-portlet-stockage/img/refresh_24px.png"
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
            "icon": "/esup-portlet-stockage/img/upload_24px.gif"
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
            "icon": "/esup-portlet-stockage/img/new_folder_24px.gif"
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
            "icon": "/esup-portlet-stockage/img/new_file_24px.png"
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
            "icon": "/esup-portlet-stockage/img/paste_24px.gif"
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
            "icon": "/esup-portlet-stockage/img/download_24px.gif"
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
            "icon": "/esup-portlet-stockage/img/delete_24px.gif"
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
            "icon": "/esup-portlet-stockage/img/copy_24px.gif"
        },
        "cutItem": {
            // The item label
            "label": $('#myMenu li.cut a').html(),
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Delete : " + obj);
                deleteFiles({
                    "dirs": obj.attr("id"),
                    "sharedSessionId": sharedSessionId
                });
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-portlet-stockage/img/cut_24px.gif"
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
            "icon": "/esup-portlet-stockage/img/rename_16px.gif"
        },
        "deleteItem": {
            // The item label
            "label": "Supprimmer",
            // The function to execute upon a click
            "action": function (obj) {
                console.log("Delete : " + obj);
                deleteFiles({
                    "dirs": obj.attr("id"),
                    "sharedSessionId": sharedSessionId
                });
            },
            "_disabled": false,
            "separator_before": false,
            "separator_after": false,
            "icon": "/esup-portlet-stockage/img/delete_24px.gif"
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
	//console.log("getPathFromLiNode: TreeNode : " + treeNode.html() + " Data :" + jQuery.data(treeNode.get(0), "path"));
	
    return jQuery.data(treeNode.get(0), "path");
}

function getTypeFromLiNode(treeNode) {
	return jQuery.data(treeNode.get(0), "type");
}

function getLiIdFromPath(path) {

	var pathToIdMap = getPathToIdMap();
	var li_id = pathToIdMap[path];
	
    console.log("li id is : [" + li_id + "] for path [" + path + "]");
    return li_id;
}

/*
 * Given a path, returns its parent.
 * Examples
 *  getParentPath("FS:Shared~bob2~images/galerie/htdhtd") ==
 *   "FS:Shared~bob2~images/galerie"
 * 
 *  getParentPath("FS:Shared~bob2) ==
 *   "FS:Shared"
 */

function getParentPath(path) {
    var tokens = path.split(/[~\/]/);

    var parentPath = path;

    if (tokens.length > 0) {
        var lastToken = tokens.pop();
        parentPath = parentPath.substring(0, path.length - lastToken.length - 1);
    }

    return parentPath;
}

/*
 * getStockageArea("FS:Shared~bob2~images/galerie/htdhtd") ==
 *   "FS:Shared~bob2"
 */

function getStockageArea(path) {
    var tokens = path.split(/[~\/:]/);

    var parentPath = path;

    if (tokens.length >= 2) {
        return tokens[0] + ":" + tokens[1] + "~" + tokens[2];
    }

    console.log("Unknown stockage area for path : " + path)
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
    	console.log("openAndSelectLiNode.  parentNode : " + getJqueryObj(parentNode).html() );
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

function getPathToIdMap() {
	var pathToIdMap = $("#arborescentArea").data("pathToIdMap");
	
	if (pathToIdMap == null) {
		pathToIdMap = {};
	}	
	
	return pathToIdMap;
}

function addPathToIdEntry(path, id) {
	//console.log("addPathToIdEntry.  Adding path " + path + ", " + id);
	
	var pathToIdMap = getPathToIdMap();
	pathToIdMap[path] = id;

	$("#arborescentArea").data("pathToIdMap", pathToIdMap);
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
}