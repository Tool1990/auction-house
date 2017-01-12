/**
 * de.sb.broker.PreferencesController: broker preferences controller.
 * Copyright (c) 2013-2015 Sascha Baumeister
 */
"use strict";

this.de = this.de || {};
this.de.sb = this.de.sb || {};
this.de.sb.broker = this.de.sb.broker || {};
(function () {
	var SUPER = de.sb.broker.Controller;

	/**
	 * Creates a new preferences controller that is derived from an abstract controller.
	 * @param sessionContext {de.sb.broker.SessionContext} a session context
	 */
	de.sb.broker.PreferencesController = function (sessionContext) {
		SUPER.call(this, 3, sessionContext);
	}
	de.sb.broker.PreferencesController.prototype = Object.create(SUPER.prototype);
	de.sb.broker.PreferencesController.prototype.constructor = de.sb.broker.PreferencesController;


	/**
	 * Displays the associated view.
	 */
	de.sb.broker.PreferencesController.prototype.display = function () {
		if (!this.sessionContext.user) return;
		SUPER.prototype.display.call(this);
		this.displayStatus(200, "OK");

		var sectionElement = document.querySelector("#preferences-template").content.cloneNode(true).firstElementChild;
		sectionElement.querySelector("button").addEventListener("click", this.persistUser.bind(this));
		sectionElement.addEventListener("dragover", function(event) {
			//allow drop
			event.preventDefault();
		});
		sectionElement.addEventListener("drop", this.persistAvatar.bind(this));
		document.querySelector("main").appendChild(sectionElement);

		if(!this.sessionContext.user.avatar) {
            var self = this;
            var resource = "/services/people/" + self.sessionContext.user.identity + "/avatar";
            de.sb.util.AJAX.invoke(resource, "GET", {"Accept": "application/json"}, null, self.sessionContext, function (request) {
                if (request.status === 200) {
                    self.sessionContext.user.avatar = de.sb.util.createImage(JSON.parse(request.responseText));

                }
                self.displayUser();
            });
        }else{
			this.displayUser();
		}
	}


	/**
	 * Displays the session user.
	 */
	de.sb.broker.PreferencesController.prototype.displayUser = function () {
		var user = this.sessionContext.user;
		console.log(user);
		var activeElements = document.querySelectorAll("section.preferences input");
		activeElements[0].value = user.group;
		activeElements[1].value = user.alias;
		activeElements[2].value = "";
		activeElements[3].value = user.name.given;
		activeElements[4].value = user.name.family;
		activeElements[5].value = user.address.street;
		activeElements[6].value = user.address.postCode;
		activeElements[7].value = user.address.city;
		activeElements[8].value = user.contact.email;
		activeElements[9].value = user.contact.phone;

		var avatar = document.querySelector("section.preferences img");
		if(this.sessionContext.user.avatar){
			avatar.src = this.sessionContext.user.avatar;
		} else{
			avatar.src = "http://placehold.it/100x100";
		}
	}

	/**
	 * Persists the avatar.
	 */
	de.sb.broker.PreferencesController.prototype.persistAvatar = function (event) {
		event.preventDefault();
		var file = event.dataTransfer.files[0];
		var fr = new FileReader();
		fr.onload = function() {
			var self = this;
			var arrayBuffer = fr.result;
			var byteArray = new Uint8Array(arrayBuffer);
			var body = JSON.stringify(byteArray);
			var header = {"Content-type": "application/json"};
			//var header = {"Content-type": file.type};
			var url = "/services/people/" + self.sessionContext.user.identity + "/avatar";
			de.sb.util.AJAX.invoke(url, "PUT", header, body, this.sessionContext, function (request) {
				self.displayStatus(request.status, request.statusText);
				if (request.status === 200) {
					self.sessionContext.user.version = self.sessionContext.user.version + 1;
				} else if (request.status === 409) {
					de.sb.broker.APPLICATION.welcomeController.display();
				} else {
					self.displayUser();
				}
			});
		}.bind(this);
		fr.readAsArrayBuffer(file);
	}


	/**
	 * Persists the session user.
	 */
	de.sb.broker.PreferencesController.prototype.persistUser = function () {
		var inputElements = document.querySelectorAll("section.preferences input");

		var user = JSON.parse(JSON.stringify(this.sessionContext.user));
		var password = inputElements[2].value.trim();
		user.name.given = inputElements[3].value.trim();
		user.name.family = inputElements[4].value.trim();
		user.address.street = inputElements[5].value.trim();
		user.address.postCode = inputElements[6].value.trim();
		user.address.city = inputElements[7].value.trim();
		user.contact.email = inputElements[8].value.trim();
		user.contact.phone = inputElements[9].value.trim();

		var self = this;
		var header = {"Content-type": "application/json"};
		if (password) header["Set-password"] = password;
		var body = JSON.stringify(user);
		de.sb.util.AJAX.invoke("/services/people", "PUT", header, body, this.sessionContext, function (request) {
			self.displayStatus(request.status, request.statusText);
			if (request.status === 200) {
				self.sessionContext.user.version = self.sessionContext.user.version + 1;
				self.sessionContext.user.name = user.name;
				self.sessionContext.user.address = user.address;
				self.sessionContext.user.contact = user.contact;
				if (password.length !== 0) self.sessionContext.userPassword = password;
			} else if (request.status === 409) {
				de.sb.broker.APPLICATION.welcomeController.display(); 
			} else {
				self.displayUser();
			}
		});
	}
} ());