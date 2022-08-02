linkList = {
	Home:"./index.html",
	Downloads:"downloads.html",
	Log:"programming-log.html",
}


header = document.querySelector("header");
anchor_values = linkList;
labels = Object.keys(anchor_values);
title = document.createElement("h1");
title.innerText = "Project blog";
header.appendChild(title);
nav = document.createElement("nav");
ul = document.createElement("ul");
labels.forEach(label => {
	li = document.createElement("li");
	a = document.createElement("a");
	a.innerText = label;
	a.target = "_self";
	a.href = anchor_values[label];
	li.appendChild(a);
	ul.appendChild(li);
});
nav.appendChild(ul);
header.appendChild(nav);
