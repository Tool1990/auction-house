/**
 * Allgemeine Anmerkungen:
 *
 * namespace / dateipfad scheint laut aufgabe noch nciht richtig zu sein
 * - statt: src.Web-INF.js
 * - soll sein: de.sb.broker (in unserer struktur ist auch noch ein .rest nach broker)
 *
 * TODO:
 * - display of min Bid
 * - display of edit button / bid field
 * 		- callback methods of the edit button
 * 			- auction editor logic (template is already there)
 */



"use strict";

this.de = this.de || {};
this.de.sb = this.de.sb || {};
this.de.sb.broker = this.de.sb.broker || {};

(function () {
	var SUPER = de.sb.broker.Controller;
	var TIMESTAMP_OPTIONS = {
		year: 'numeric', month: 'numeric', day: 'numeric',
		hour: 'numeric', minute: 'numeric', second: 'numeric',
		hour12: false
	};


	/**
	 * Creates a new auctions controller that is derived from an abstract controller.
	 * @param sessionContext {de.sb.broker.SessionContext} a session context
	 */
	de.sb.broker.OpenAuctionsController = function (sessionContext) {
		SUPER.call(this, 1, sessionContext);
	}
	de.sb.broker.OpenAuctionsController.prototype = Object.create(SUPER.prototype);
	de.sb.broker.OpenAuctionsController.prototype.constructor = de.sb.broker.OpenAuctionsController;


	/**
	 * Displays the associated view.
	 */
	de.sb.broker.OpenAuctionsController.prototype.display = function () {
		if (!this.sessionContext.user) return;
		SUPER.prototype.display.call(this);

		// one section for all auctions
		// template?
		var sectionElement = document.querySelector("#open-auctions-template").content.cloneNode(true).firstElementChild;
		document.querySelector("main").appendChild(sectionElement);

		// asynchron thread handling (?)
		var indebtedSemaphore = new de.sb.util.Semaphore(1 - 2);
		var statusAccumulator = new de.sb.util.StatusAccumulator();
		var self = this;


		// filling
		// not sure if one function is enough (probably not...)
		// this time all auctions should be in one section
		// the only difference is an edit button on the own auctions
		// other foreign auctions have a bid-field
		var resource = "/services/auctions";
		de.sb.util.AJAX.invoke(resource, "GET", {"Accept": "application/json"}, null, this.sessionContext, function (request) {

			if (request.status === 200) {
                self.displayAuctions(JSON.parse(request.responseText));
			}
			statusAccumulator.offer(request.status, request.statusText);
			indebtedSemaphore.release();
		});
		// filling
		// var resource = "/services/people/" + this.sessionContext.user.identity + "/auctions?seller=false&closed=false";
		// de.sb.util.AJAX.invoke(resource, "GET", {"Accept": "application/json"}, null, this.sessionContext, function (request) {
		// 	if (request.status === 200) {
		// 		var reqAuctions = JSON.parse(request.responseText);
		// 		auctions = auctions.concat(reqAuctions);
         //        console.log("bidder", reqAuctions);
		// 	}
		// 	statusAccumulator.offer(request.status, request.statusText);
		// 	indebtedSemaphore.release();
		// });

		indebtedSemaphore.acquire(function () {
			self.displayStatus(statusAccumulator.status, statusAccumulator.statusText);
		});
	}


		/**
		 * Displays the given auctions that feature the requester as seller.
		 * @param auctions {Array} the seller auctions
		 */
		de.sb.broker.OpenAuctionsController.prototype.displayAuctions = function (auctions) {
			var tableBodyElement = document.querySelector("section.open-auctions tbody");
			var rowTemplate = document.createElement("tr");
			for (var index = 0; index < 7; ++index) {
				var cellElement = document.createElement("td");
				cellElement.appendChild(document.createElement("output"));
				rowTemplate.appendChild(cellElement);
			}

			var self = this;
			auctions.forEach(function (auction) {
				var rowElement = rowTemplate.cloneNode(true);
				tableBodyElement.appendChild(rowElement);
				var activeElements = rowElement.querySelectorAll("output");
				activeElements[0].value = auction.seller == undefined ? self.sessionContext.user.alias : auction.seller.alias;
				activeElements[0].title = createDisplayTitle(activeElements[0].value);
				activeElements[1].value = new Date(auction.creationTimestamp).toLocaleString(TIMESTAMP_OPTIONS);
				activeElements[2].value = new Date(auction.closureTimestamp).toLocaleString(TIMESTAMP_OPTIONS);
				activeElements[3].value = auction.title;
				activeElements[3].title = auction.description;
				activeElements[4].value = auction.unitCount;
				activeElements[5].value = (parseInt(auction.askingPrice) * 0.01).toFixed(2);	// min Bid not saved in auctios
                var btn = document.createElement('input');
                btn.type = "button";
                btn.className = "btn";
                btn.value = "edit";
                btn.onclick = (function(){
                	self.displayAuctionEdit(auction.id);
				}).bind(self);
                var toAppend = activeElements[0].value === self.sessionContext.user.alias ? btn: "TODO";
				activeElements[6].append(toAppend);
			});
		}

		/**
		 * Displays the auction the user wants to edit
		 * @param auction id
		 */
        de.sb.broker.OpenAuctionsController.prototype.displayAuctionEdit = function(auctionId){
        	if(!document.querySelector("main").contains(document.querySelector(".auction-form"))) {
                var sectionElement = document.querySelector("#auction-form-template").content.cloneNode(true).firstElementChild;
                document.querySelector("main").appendChild(sectionElement);
            }
		}

		/**
		 * Displays the given auctions that feature the requester as bidder.
		 * @param auctions {Array} the bidder auctions
		 */
		// de.sb.broker.OpenAuctionsController.prototype.displayBidderAuctions = function (auctions) {
		// 	var tableBodyElement = document.querySelector("section.open-auctions tbody");
		// 	var rowTemplate = document.createElement("tr");
		// 	for (var index = 0; index < 8; ++index) {
		// 		var cellElement = document.createElement("td");
		// 		cellElement.appendChild(document.createElement("output"));
		// 		rowTemplate.appendChild(cellElement);
		// 	}
        //
		// 	var self = this;
		// 	auctions.forEach(function (auction) {
		// 		var rowElement = rowTemplate.cloneNode(true);
		// 		tableBodyElement.appendChild(rowElement);
		// 		var activeElements = rowElement.querySelectorAll("output");
		// 		activeElements[0].value = auction.seller.alias;
		// 		activeElements[0].title = createDisplayTitle(auction.seller);
		// 		activeElements[1].value = new Date(auction.creationTimestamp).toLocaleString(TIMESTAMP_OPTIONS);
		// 		activeElements[2].value = new Date(auction.closureTimestamp).toLocaleString(TIMESTAMP_OPTIONS);
		// 		activeElements[3].value = auction.title;
		// 		activeElements[3].title = auction.description;
		// 		activeElements[4].value = auction.unitCount;
		// 		//activeElements[5].value = (auction.minPrice * 0.01).toFixed(2);	// min Bid not saved in auctios
		// 		//activeElements[6].value = ; // bid field
		// 	});
		// }


		/**
		 * Creates a display title for the given person.
		 * @param person {Object} the person
		 */
		function createDisplayTitle (person) {
			if (!person) return "";
			if (!person.name) return person.alias;
			return person.name.given + " " + person.name.family + " (" + person.contact.email + ")";
		}

} ());