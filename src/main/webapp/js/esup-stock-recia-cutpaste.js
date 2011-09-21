
/**
 * Called by drag/drop to perform the file movements
 * 
 */
function doCutCopyPaste(isCopy, sourcePath, targetPath) {
	
    console.log("doCutCopyPaste");
    
    var prepareFilesUrl = '/esup-portlet-stockage/servlet-ajax/' + (isCopy ? 'prepareCopyFiles' : 'prepareCutFiles');
    
    $.post(prepareFilesUrl, "dirs=" + encodeURIComponent(sourcePath) + "&sharedSessionId=" + sharedSessionId, function (data) {
    	console.log("prepareCopyFiles Ajax call completed.  Starting paste operation");
        cursor_clear();
        showInfoToolBar(data.msg);        
        pasteToPath(targetPath);
        console.log("Paste ajax call completed.");
    });
    
}

function doCutOrCopy(isCopy) {
	
	var prepareFilesUrl = '/esup-portlet-stockage/servlet-ajax/' + (isCopy ? 'prepareCopyFiles' : 'prepareCutFiles');
    
	cursor_wait();
    var dirs = getCheckedDirs();
    console.log("doCutOrCopy(" + isCopy + ") dirs: " + dirs);
    
    $.ajax({
    	url: prepareFilesUrl, 
    	data: $("#filesForm").serialize(),
    	success: function (data) {
    		console.log("Cut/copy succeeded");
    		showInfoToolBar(data.msg);
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
    
	return isValidPaste(sourcePath, targetPath)
}

function pasteToPath(path) {
	console.log("Entering pasteToPath.  Path: " + path);
    cursor_wait();

    var pastFilesUrl = '/esup-portlet-stockage/servlet-ajax/pastFiles';
    $.ajax({
    	url: pastFilesUrl,
    	data: "dir=" + encodeURIComponent(path) + "&sharedSessionId=" + sharedSessionId,
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