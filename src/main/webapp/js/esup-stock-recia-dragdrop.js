
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
	
	var sourceStockageArea = getStockageArea(sourcePath)
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
