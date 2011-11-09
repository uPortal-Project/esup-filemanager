  function disableEnterKey(e) {
      var key;
      if (window.event) key = window.event.keyCode; //IE
      else key = e.which; //firefox
      return (key != 13);
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

          //Enable / disable context menu items using same method as toolbar
          updateActions(function (actionName, enabled) {

              var elem = $("#myMenu li." + actionName);
              if (elem.length == 0) {
                  console.log("No BA menu items! " + actionName)
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
  
  function handleItemClick(e) {
	  
	  /*Because of limitations with jQuerys double click, we are 
	  	obliged to get a bit fancy in order to have a distinct
	  	action on both the click and double click events*/
	  
	  var jqElem = $(this);
	  
	  //Retrieve how many times this element has been clicked
	  var clicks = 1 + (jqElem.data("clicks") ? jqElem.data("clicks") : 0);
	  	  
	  //Store after incrementing
	  jqElem.data("clicks", clicks); 
	  
	  console.log("Item clicked " + jqElem.html() + " clicks : " + clicks);
	  
	  if(useDoubleClick == "false") {
		  handleItemDblClick(e, jqElem);
		  return false;
	  }
	  
	  if (clicks == 1) {
		  //Start a timeout function.  If we get another click in time, it will
		  //be canceled
		  var func = function() {
			  console.log("Executing single click, should be later");
			  
			  //Reset click counter
			  jqElem.data("clicks", 0);
			  jqElem.data("singleClickFuncId", null);
			  
			  console.log("Browser area click");
		      handleItemSelection(jqElem, e);

		      //hide the context menu
		      $(".contextMenu").hide();
		  };
		  
		  if (jQuery.browser.msie) {
			  console.log("Calling single click function immediately for IE");
			  func();
		  } else {
			  console.log("Starting timer for single click.");
			  var singleClickFuncId = setTimeout(func, 300);
		  }
		  
		  //Store timeout function id in case we need to cancel it
		  jqElem.data("singleClickFuncId", singleClickFuncId);
	  } else if (clicks == 2) {
		  console.log("Executing double click");
		  handleItemDblClick(e, jqElem);		  
	  }	
	  
      return false;
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
      id = elemClicked.attr('rel');
      downloadFile(id);
  }

  function handleShiftSelection(elemClicked, event) {
  
  	console.log("Shift key pressed");
  	var selector = "#browserArea .file, #browserArea .fileTreeRef"
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
  	
  	for(i=Math.min(start,end);i<=Math.max(start,end);i++) {
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
        
        
        if (event.ctrlKey) {
            console.log("Ctrl key pressed");
        } else if (event.shiftKey) {
        	return handleShiftSelection(elemClicked, event);
        } else {
        
            console.log("Ctrl key not pressed");
            
            //Remove all checkboxes
            clearSelections();
        }

        if (isChecked(elemClicked)) {
            console.log("Current row selected");
            deselectObject(elemClicked, true);
        } else {
            console.log("Current row not selected");
            selectObject(elemClicked, true);
        }
    }
    
  /**
   * Initializes context menu events
   */
  function initEvents() {


      $('#browserMain div.breadcrumbs a.fileTreeRefCrumbs').bind('click', function () {
          var path = $(this).attr('rel');
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
      
      elems.bind('click', handleItemClick);
      
      //Other browsers will use single click for both actions
      if (jQuery.browser.msie) {
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
              $(".browsercheck", this).hide()
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

      $("#jqueryFileTree").css({display:'block', overflow:'auto',
      		  height: '100%',
      		  width: '100%'});      

      initDragAndDrop();
      initContextMenu();
      initEvents();
      
      updateThumbnailIcons();
  }
