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

(function ($) {
		
	$(document).ready(function() { 
		
		function cursor_wait() {
			$("body").css("cursor", "progress");
		}

		// Returns the cursor to the default pointer
		function cursor_clear() {
			 $("body").css("cursor", "auto");
		}

		
		var uploader =  new qq.FileUploader({
			multiple: true,
			template: fileuploadTemplate,
			fileTemplate: fileTemplate,  			
			element: document.getElementById('file-uploader'),
			action: '/esup-portlet-stockage/servlet-ajax/uploadFile',     
			onSubmit: function(id, fileName){
					uploader.setParams({
						dir: $("#bigdirectory").attr("rel"),
						sharedSessionId: sharedSessionId
		 			});
					cursor_wait();
					$('.qq-upload-list').show();
			},
			onComplete: function(id, fileName, result){
				cursor_clear();
				$("#info-toolbar").html(result.msg);
				$('#info-toolbar').show();
				if(result.success) {
					var id = $("#bigdirectory").attr('rel');
					var obj = document.getElementById(id);
					$("#fileTree").jstree("refresh", obj); 
					$("#fileTree").jstree("select_node", obj, true); 
				}
				if(uploader.getInProgress() == 0) {
					$('.qq-upload-list').fadeOut(uploadListFadeoutTime);
					$('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
				}							
			},
			onCancel: function(id, fileName){
				if(uploader.getInProgress() == 0) {
					$('.qq-upload-list').fadeOut(uploadListFadeoutTime);
				}			
				$('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
			}
		 });

		     $('#toolbar-copy').bind('click', function() {
		    	    cursor_wait();
		    		var dirs = getCheckedDirs();
		    		var prepareCopyFilesUrl = '/esup-portlet-stockage/servlet-ajax/prepareCopyFiles';
		    		 $.post(prepareCopyFilesUrl, $("#filesForm").serialize(), function(data){
		    		   cursor_clear();
		  	    	   $("#info-toolbar").html(data.msg);
					   $('#info-toolbar').show();
					   $('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
		    		 });
		     });

		     $('#toolbar-cut').bind('click', function() {
		    	cursor_wait();
		 		var dirs = getCheckedDirs();
		 		var prepareCutFilesUrl = '/esup-portlet-stockage/servlet-ajax/prepareCutFiles';
		 		 $.post(prepareCutFilesUrl, $("#filesForm").serialize(), function(data){
		 			   cursor_clear();
			    	   $("#info-toolbar").html(data.msg);
					   $('#info-toolbar').show();
					   $('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
			    	});
		 	 });

		     $('#toolbar-past').bind('click', function() {
		    	 cursor_wait();
		    	 var id = $("#bigdirectory").attr('rel');
		    	 var pastFilesUrl = '/esup-portlet-stockage/servlet-ajax/pastFiles';
		 		 $.post(pastFilesUrl, "dir="+id+"&sharedSessionId="+sharedSessionId, function(data){
		 			 cursor_clear();
		 			 if(data.status) {
			    	   $("#info-toolbar").html(data.msg);
					   $('#info-toolbar').show();
					   $('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
		 			 } else {
		 				$("#info-toolbar").html(data.msg);
						$('#info-toolbar').show();
					    $('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
		 			 }
			    	   var obj = document.getElementById(id);
			    	   $("#fileTree").jstree("refresh", obj); 
			    	   $("#fileTree").jstree("select_node", obj, true); 
			      });
		  	 });

		     $('#toolbar-create').bind('click', function() {
		    	 $("#newDir").removeClass('esupHide');
		    	 $("#newDirInput").focus();
		  	});

		     $('#toolbar-rename').bind('click', function() {
			 var dirs = getCheckedDirs();
			 if(dirs.length == 0) { 
		    	     $(".renameSpan").removeClass('esupHide');
			 } else {
			     $(".browsercheck:checked").each(function() {
			 	 $(this).parent().next(".renameSpan").removeClass('esupHide');
				});
			 }
		     });

			$('#toolbar-delete').bind('click', function() {
				cursor_wait();
				var removeFilesUrl = '/esup-portlet-stockage/servlet-ajax/removeFiles';
			    $.post(removeFilesUrl, $("#filesForm").serialize(), function(data){
			    	    cursor_clear();
			    		if(data.status) {
			    			$("#info-toolbar").html(data.msg);
							$('#info-toolbar').show();
							$('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
			 			} else {
			 				$("#info-toolbar").html(data.msg);
							$('#info-toolbar').show();
							$('#info-toolbar').fadeOut(infoTolbarFadeoutTime);
			 			}
			    	   var id = $("#bigdirectory").attr('rel');
			    	   var obj = document.getElementById(id);
			    	   $("#fileTree").jstree("refresh", obj); 
			    	   $("#fileTree").jstree("select_node", obj, true); 
			    	   
			    });
			});
			
			$('#toolbar-zip').bind('click', function() {
	    		var dirs = getCheckedDirs();
	    		if(dirs.length > 0) {
	    			var downloadZipUrl = '/esup-portlet-stockage/servlet-ajax/downloadZip';
	    			$("#filesForm").attr("action", downloadZipUrl);
	    			$("#filesForm").submit();
	    		}
			});

			function getCheckedDirs()  {
				var dirs = new Array;
				$(".browsercheck:checked").each(function() {
			 	  dirs.push($(this).val());
				});
				return dirs;
			} 	 
			
			

			$("#fileTree")
				.jstree({ 
					
					// the list of plugins to include
					//"plugins" : [ "themes", "json_data", "ui", "crrm", "cookies", "dnd", "search", "types", "hotkeys", "contextmenu"],
					"plugins" : [ "themes", "json_data", "ui", "crrm", "cookies", "types"],
					// Plugin configuration

					// I usually configure the plugin that handles the data first - in this case JSON as it is most common
					"json_data" : { 
						// I chose an ajax enabled tree - again - as this is most common, and maybe a bit more complex
						// All the options are the same as jQuery's except for `data` which CAN (not should) be a function
						"ajax" : {
							// the URL to fetch the data
							"url" : '/esup-portlet-stockage/servlet-ajax/fileChildren',
							"type" : 'POST',
							// this function is executed in the instance's scope (this refers to the tree instance)
							// the parameter is the node being loaded (may be -1, 0, or undefined when loading the root nodes)
							"data" : function (n) { 
								// the result is fed to the AJAX request `data` option
								return { 
									"dir" : n.attr ? n.attr("id") : "",
									"sharedSessionId" : sharedSessionId
								}; 
							}
						}
					},
					
					// the UI plugin - it handles selecting/deselecting/hovering nodes
					"ui" : {
						// this makes the node with ID node_4 selected onload
						"select_multiple_modifier" :  false,
						"select_limit" : 1
					},

					
					
						// Using types - most of the time this is an overkill
						// Still meny people use them - here is how
						"types" : {
							// I set both options to -2, as I do not need depth and children count checking
							// Those two checks may slow jstree a lot, so use only when needed
							"max_depth" : -2,
							"max_children" : -2,
							// I want only `drive` nodes to be root nodes 
							// This will prevent moving or creating any other type as a root node
							"valid_children" : [ "drive" ],
							"types" : {
								// The default type
								"default" : {
									// I want this type to have no children (so only leaf nodes)
									// In my case - those are files
									"valid_children" : "none"
									// If we specify an icon for the default type it WILL OVERRIDE the theme icons
								},
								// The `folder` type
								"folder" : {
									// can have files and other folders inside of it, but NOT `drive` nodes
									"valid_children" : [ "default", "folder" ]
								},
								// The `drive` nodes 
								"drive" : {
									// can have files and folders inside, but NOT other `drive` nodes
									"valid_children" : [ "default", "folder" ],
									// those options prevent the functions with the same name to be used on the `drive` type nodes
									// internally the `before` event is used
									"start_drag" : false,
									"move_node" : false,
									"delete_node" : false,
									"remove" : false
								}
							}
						}
				
					})
				
				.bind('select_node.jstree', function(e, data) {
					cursor_wait();
					$.ajax({
						async : false,
						type: 'POST',
						url: '/esup-portlet-stockage/servlet-ajax/htmlFileTree',
						data : { 
							"dir" : data.rslt.obj.attr("id"),
							"sharedSessionId" : sharedSessionId
						}, 
						success : function (r) {
							cursor_clear();
							$("#browserMain").html(r);
						},
						error : function (r) {
							cursor_clear();
							if (r.statusText == 'reload')
								 location.reload();
							else if(r.statusText)
								alert(r.statusText);
							else
								alert("dossier indisponible ...");
						}
					});
		    	 });

			function downloadFile(dir) {
				
				$.ajax({
					async : false,
					type: 'POST',
					url: '/esup-portlet-stockage/servlet-ajax/downloadFile',
					data : { 
						"dir" : dir,
						"sharedSessionId" : sharedSessionId
					}
				});
				
			}		
			
	});	


})(jQuery);


function getFile(parentDir, fileId) {
	if(parentDir != null) {
		var parentObj = document.getElementById(parentDir);
		$("#fileTree").jstree("open_node", parentObj, function() {
			var obj = document.getElementById(fileId);
			$("#fileTree").jstree("select_node", obj, true); 
		}, true);
	} else {
		if(fileId != 'FS%3A') {
			var obj = document.getElementById(fileId);
			$("#fileTree").jstree("select_node", obj, true); 
		} else {
			$.ajax({
				async : false,
				type: 'POST',
				url: '/esup-portlet-stockage/servlet-ajax/htmlFileTree',
				data : { 
					"dir" : fileId,
					"sharedSessionId" : sharedSessionId
				}, 
				success : function (r) {
					$("#browserMain").html(r);
				}
			});
		}
	}
}

	
function newDir(parentDir, newDir) {
	$.ajax({
		async : false,
		type: 'POST',
		url: '/esup-portlet-stockage/servlet-ajax/createFile',
		data : { 
			"parentDir" : parentDir,
			"title" : newDir,
			"type" : "folder",
			"sharedSessionId" : sharedSessionId
		}, 
		success : function (r) {
			$("#browserMain").html(r);
			$('#fileTree').jstree("refresh", document.getElementById(parentDir));
		}
	});
}
	
function rename(parentDir, dir, title) {
	$.ajax({
		async : false,
		type: 'POST',
		url: '/esup-portlet-stockage/servlet-ajax/renameFile',
		data : { 
			"parentDir" : parentDir,
			"dir" : dir,
			"title" : title,
			"sharedSessionId" : sharedSessionId
		}, 
		success : function (r) {
			$("#browserMain").html(r);
			$("#fileTree").jstree("refresh", document.getElementById(parentDir)); 
		}
	});
}

function authenticate(dir, username, password) {
	$.ajax({
		async : false,
		type: 'POST',
		url: '/esup-portlet-stockage/servlet-ajax/authenticate',
		data : { 
			"dir" : dir,
			"username" : username,
			"password" : password,
			"sharedSessionId" : sharedSessionId
		}, 
		success : function (data) { 
			$("#info-toolbar").html(data.msg);
			$('#info-toolbar').show();
 		    $('#info-toolbar').fadeOut(infoTolbarFadeoutTime);

			if(data.status) 
				getFile(null, dir);
		}
	});
}




