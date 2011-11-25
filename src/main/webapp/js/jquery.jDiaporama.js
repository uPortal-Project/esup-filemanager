(function($) {

	
	var defaults = {
			auto : true,
			delay : 3,
			animationSpeed : "normal",
			controls : true,
			status_controls : true,
			keyboard : true,
			infos : true,
			currentimage : true,
			paused : false,
			boucles : 0,
			sens : "right",
			onrollover : true,
			random : false,
			maxImageWidth : 0,
			maxImageHeight : 0
		};
	
	var options;
	
	function setContainerSizes(diapo, imgDomObj, doneCallback) {
				
		console.log("setContainerSizes");
		var newImgObj = $("<img/>");

		newImgObj.load(function() {
			console.log("Image loaded, setContainerSizes. w/h"
					+ options.maxImageWidth + ", " + options.maxImageHeight);

			// Ajustement de la taille du container
			var width = this.width;
			var height = this.height;

			console.log("Original image h/w " + height + ", " + width);
			
			var ratio = 1.0;
			
			if (options.maxImageHeight > 0
					&& options.maxImageHeight < height) {
				ratio = height / options.maxImageHeight;				
				console.log("Height is at max, ration is " + ratio);								
			} 
			
			if (options.maxImageWidth > 0
					&& options.maxImageWidth < width) {
				ratio = Math.max(ratio, width / options.maxImageWidth);
				console.log("Width is at max, ration is " + ratio);
			}
			
			width = width / ratio;
			height = height / ratio;

			console.log("Width height of diaporama containers : " + width
					+ ", " + height);

			diapo.width(width);
			diapo.height(height);
			diapo.parent().width(width);
			diapo.parent().height(height);
			$(imgDomObj).width(width);
			$(imgDomObj).height(height);

			doneCallback();
		});

		newImgObj.attr("src", imgDomObj.src);

	}
	
	var methods = {
		init : function(settings) {

		
			options = $.extend(defaults, settings);

			this
					.each(function() {

						// Si le diaporama comporte plus qu'une image
						if ($("li", diapo).length > 1) {

							var diapo = $(this);
							var mouseover = false;
							var sens = options.sens;
							var pause = false;
							var width = 0;
							var height = 0;
							var current_slide = 0;
							var nb_slides = $("li", diapo).length;

							diapo.wrap("<div class='jDiaporama'></div>");

							// Detection et actions des mouseover
							diapo.parent().mouseenter(function() {
								mouseover = true;
								if (options.onrollover)
									displayInfos($("li.active", diapo), "show");

								if (options.controls)
									diapo.siblings(".jDiaporama_controls").fadeIn();

							}).mouseleave(function() {
								mouseover = false;
								if (options.onrollover)
									displayInfos($("li.active", diapo), "hide");

								if (options.controls)
									diapo.siblings(".jDiaporama_controls").hide();
							});

							

							function init() {

								var img_element = $("li:first-child img", diapo)[0];
								console.log("jDiaporama init : " + img_element.src);

								var init_f = function() {

									if (options.controls)
										diapo.siblings(".jDiaporama_status").show();

									if (options.auto && options.paused)
										$(".pause", diapo.siblings()).trigger("click");

									if (!options.onrollover)
										displayInfos($("li", diapo), "show");

									if (options.controls && options.status_controls)
										$(
												"#jDiaporama_bullet_"
														+ (parseInt($("li", diapo).index(
																$("li:first-child", diapo))) + 1),
												diapo.siblings()).addClass("active");

								};

								setContainerSizes(diapo, img_element, init_f);

							}

							var inter = "";

							if (options.auto && !options.paused)
								inter = setInterval(function() {
									displayDiaporama(options)
								}, (options.delay * 1000));

							$("li", diapo).hide();
							$("li:first-child", diapo).addClass("active").fadeIn(
									options.animationSpeed, init);

							// Pour chaque �l�ment
							$("li", diapo).each(
									function() {
										elt = $(this);

										i = parseInt($("li", diapo).index($(this)) + 1);
										$(this).attr("id", "jDiaporama_image_" + i);

										// Affichage de la description si renseign� et activ�
										if (options.infos) {
											var is_desc = ($("img", elt).attr("title") != "");
											var is_title = ($("img", elt).attr("alt") != "");

											if (is_desc)
												elt.append("<p class='desc'>"
														+ $("img", elt).attr("title") + "</p>");

											if (is_title)
												elt.append("<p class='title'>"
														+ $("img", elt).attr("alt") + "</p>");

											if (options.currentimage)
												elt.append("<p class='count'>"
														+ parseInt($("li", diapo).index(elt) + 1) + "/"
														+ diapo.children().length + "</p>");
										}
									})

							// Navigation au clavier
							if (options.keyboard)
								$(document).keydown(function(event) {
									switch (event.keyCode) {
									case 37:
										// Fl�che gauche
										prev();
										break;

									case 39:
										// Fl�che droite
										next();
										break;
									}
								});

							// Controls
							if (options.controls) {
								if (options.status_controls) {
									// Etat du diaporama
									diapo.after("<div class='jDiaporama_status'></div>");
									$("li", diapo).each(
											function() {
												i = parseInt($("li", diapo).index($(this)) + 1);
												$(".jDiaporama_status", diapo.parent()).append(
														"<a id='jDiaporama_bullet_" + i
																+ "' href='#'>Image " + i + "</a>");
											})

									$(".jDiaporama_status", diapo.parent()).css("margin-left",
											-($(".jDiaporama_status", diapo.parent()).width() / 2));

									$(".jDiaporama_status a", diapo.parent())
											.click(
													function() {
														if ($("li.active", diapo).attr("id").split("_")[2] != $(
																this).attr("id").split("_")[2])
															nextImage(options, $(this));
														return false;
													})
								}

								diapo
										.after("<div class='jDiaporama_controls'><a href='#' class='prev'>Prec.</a> "
												+ ((options.auto) ? "<a href='#' class='pause'>Pause</a>"
														: "") + " <a href='#' class='next'>Suiv.</a></div>");

								$(".prev", diapo.siblings()).click(function() {
									prev();
									return false;
								});

								$(".next", diapo.siblings()).click(function() {
									next();
									return false;
								});

								$(".pause", diapo.siblings()).click(function() {
									if ($(this).hasClass("pause")) {
										$(this).removeClass("pause").addClass("play");
										clearInterval(inter);
										pause = true;
									} else {
										$(this).removeClass("play").addClass("pause");
										inter = setInterval(function() {
											displayDiaporama(options)
										}, (options.delay * 1000));
										pause = false;
									}

									return false;
								});
							}

							function next() {
								if (options.random)
									randomImage();
								else {
									if (!$("li.active", diapo).is(":last-child"))
										elt = $("li.active", diapo).next();
									else
										elt = $("li:first-child", diapo);

									nextImage(options, elt);
									sens = "right";
								}
							}

							function prev() {
								if (options.random)
									randomImage();
								else {
									if (!$("li.active", diapo).is(":first-child"))
										elt = $("li.active", diapo).prev();
									else
										elt = $("li:last-child", diapo);

									nextImage(options, elt);
									sens = "left";
								}
							}

							function randomImage() {
								rand = Math.floor(Math.random() * nb_slides) + 1;
								id = $("li.active", diapo).attr("id").split("_")[2];

								while (rand == id) {
									rand = Math.floor(Math.random() * nb_slides) + 1;
								}

								nextImage(options, $("li#jDiaporama_image_" + rand, diapo));
							}

							// Affichage des infos sur l'image courante

							function displayInfos(elt, display) {
								var is_desc = ($("img", elt).attr("title") != "");
								var is_title = ($("img", elt).attr("alt") != "");

								if (is_desc)
									if (display == "show")
										$(".desc", elt).slideDown("fast");
									else
										$(".desc", elt).slideUp("fast");
								if (is_title)
									if (display == "show")
										$(".title", elt).slideDown("fast");
									else
										$(".title", elt).slideUp("fast");
								if (options.currentimage)
									if (display == "show")
										$(".count", elt).slideDown("fast");
									else
										$(".count", elt).slideUp("fast");
							}

							// Affiche l'�l�ment suivant

							function nextImage(options, elt) {
								clearInterval(inter);

								$("li.active", diapo).fadeOut(options.animationSpeed)
										.removeClass("active");
								$(".jDiaporama_status a", diapo.parent()).removeClass("active");

								id = elt.attr("id").split("_")[2];
								$("li#jDiaporama_image_" + id, diapo).addClass("active")
										.fadeIn(options.animationSpeed);

								var img_element = $("li#jDiaporama_image_" + id + " img", diapo)[0];

								console.log("nextImage : " + img_element.src);

								var restOfFunction = function() {

									if (options.controls && options.status_controls)
										$("#jDiaporama_bullet_" + id, diapo.siblings()).addClass(
												"active");

									if (options.infos && mouseover && options.onrollover)
										displayInfos($("li.active", diapo), "show");
									else if (!mouseover && options.onrollover)
										displayInfos($("li.active", diapo), "hide");

									if (!pause && options.auto) {
										if (options.boucles == 0
												|| (options.boucles > 0 && (diapo.data("current_slide") / diapo
														.children().length) < options.boucles))
											inter = setInterval(function() {
												displayDiaporama(options)
											}, (options.delay * 1000));
										else
											$(".pause", diapo.siblings()).remove();
									}
								};

								setContainerSizes(diapo, img_element, restOfFunction);
							}

							function displayDiaporama(options) {
								current_slide++;
								diapo.data("current_slide", current_slide);

								if (sens == "right")
									next();
								else
									prev();
							}

						}
					});

			return this;
		},
		options : function(settings) {
			this.each(function() {
				var diapo = $(this);
				console.log("Options " + this);
				options = $.extend(defaults, settings);
				
				var img_element = $("li.active img", diapo)[0];
				console.log("jDiaporama options : " + img_element.src);

				var init_f = function() {
				};

				setContainerSizes(diapo, img_element, init_f);
			});
			
		},
		hide : function() {
		},
		update : function() {
		}
	};

	$.fn.jDiaporama = function(method) {

		// Method calling logic
		if (methods[method]) {
			return methods[method].apply(this, Array.prototype.slice.call(arguments,
					1));
		} else if (typeof method === 'object' || !method) {
			return methods.init.apply(this, arguments);
		} else {
			$.error('Method ' + method + ' does not exist on jQuery.tooltip');
		}

	};
})(jQuery);