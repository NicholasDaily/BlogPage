function changePayStatus(identification, value, element){
	var formData = new FormData();
	formData.append("identifier", identification);
	formData.append("value", value);
	formData.append("isWeek", "FALSE");
	var request = new XMLHttpRequest();
	request.open("POST", "/updatePayStatus");
	request.send(formData);
				
	request.onreadystatechange = function(){
		if(request.readyState === 4){
			if(request.status === 200){
				console.log(request.response);
				/*START MODIFYING HTML*/
				if(element.classList.contains('payment-incomplete')){
					element.classList.remove('payment-incomplete');
					element.classList.add('payment-complete');
					element.innerHTML = '&#10003;';
					console.log(element.getAttribute("onclick"));
					element.setAttribute('onclick', element.getAttribute("onclick").replace("TRUE", "FALSE"));
					if(document.getElementsByClassName("payment-incomplete").length == 1){
						var weekPayStatus = document.getElementById("paid-status-week");
						weekPayStatus.classList.remove('payment-incomplete');
						weekPayStatus.classList.add('payment-complete');
						weekPayStatus.innerHTML = '&#10003;';
						weekPayStatus.setAttribute('onclick', weekPayStatus.getAttribute("onclick").replace("TRUE", "FALSE"));
					}

				}else{
					element.classList.remove('payment-complete');
					element.classList.add('payment-incomplete');
					element.innerHTML = '&#9447;';
					console.log(element.getAttribute("onclick"));
					element.setAttribute('onclick', element.getAttribute("onclick").replace("FALSE", "TRUE"));
					if(document.getElementsByClassName("payment-incomplete").length > 0){
						var weekPayStatus = document.getElementById("paid-status-week");
						weekPayStatus.classList.remove('payment-complete');
						weekPayStatus.classList.add('payment-incomplete');
						weekPayStatus.innerHTML = '&#9447;';
						weekPayStatus.setAttribute('onclick', weekPayStatus.getAttribute("onclick").replace("FALSE", "TRUE"));
					}
				}
			}
		}else{
			console.log("Error: " + request.status);
		}
	}
}

function changePayStatusWeek(weekStart, value){
	var formData = new FormData();
	formData.append("identifier", weekStart);
	formData.append("value", value);
	formData.append("isWeek", "TRUE");
	var request = new XMLHttpRequest();
	request.open("POST", "/updatePayStatus");
	request.send(formData);
				
	request.onreadystatechange = function(){
		if(request.readyState === 4){
			if(request.status === 200){
				//console.log(request.response);
				/*START MODIFYING HTML*/
				var weekPayStatus = document.getElementById("paid-status-week");
				if(weekPayStatus.classList.contains('payment-incomplete')){
					elements = document.querySelectorAll('.payment-incomplete');
					console.log(elements);
					for(i = 0; i < elements.length; i++){
						if(elements[i].getAttribute("onclick") != null){
							elements[i].classList.remove('payment-incomplete');
							elements[i].classList.add('payment-complete');
							elements[i].innerHTML = '&#10003;';
							elements[i].setAttribute('onclick', elements[i].getAttribute("onclick").replace("TRUE", "FALSE"));
						}
					}
				}else{
					elements = document.querySelectorAll('.payment-complete');
					console.log(elements);
					for(i = 0; i < elements.length; i++){
						if(elements[i].getAttribute("onclick") != null){
							elements[i].classList.remove('payment-complete');
							elements[i].classList.add('payment-incomplete');
							elements[i].innerHTML = '&#9447;';
							elements[i].setAttribute('onclick', elements[i].getAttribute("onclick").replace("FALSE", "TRUE"));
						}
					}
				}

			}
		}else{
			console.log("Error: " + request.status);
		}
	}
}
