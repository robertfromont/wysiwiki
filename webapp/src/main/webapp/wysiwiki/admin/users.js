function readUsers() {
    let oReq = new XMLHttpRequest();
    oReq.addEventListener("load", function(e) {
//        try {
            const xmlUsers = this.responseXML.documentElement;
            const divUsers = document.getElementById("users");
            divUsers.innerHTML = ""; // remove any current children
            for (let u = 0; u < xmlUsers.children.length; u++) {
                // parse user
                const user = xmlUsers.children[u];
                const userRoles = [];
                if (user.childElementCount) {
                    const rolesElement = user.children[0];
                    for (let r = 0; r < rolesElement.childElementCount; r++) {
                        const child = rolesElement.children[r];
                        if (child.getAttribute("rolename")) {
                            userRoles.push(child.getAttribute("rolename"));
                        }
                    } // next child
                }
                
                // construct UI elements
                const trUser = document.createElement("tr");
                divUsers.appendChild(trUser);
                trUser.className = "user";

                const username = user.getAttribute("username");
                const tdUsername = document.createElement("td");
                trUser.appendChild(tdUsername);
                tdUsername.className = "username";
                tdUsername.appendChild(document.createTextNode(username));
                
                const tdPassword = document.createElement("td");
                trUser.appendChild(tdPassword);
                tdPassword.className = "password";
                const divPassword = document.createElement("div");
                tdPassword.appendChild(divPassword);
                const inputPassword = document.createElement("input");
                divPassword.appendChild(inputPassword);
                inputPassword.type = "password";
                inputPassword.placeholder = "Change Password";
                const btnPassword = document.createElement("button");
                divPassword.appendChild(btnPassword);
                btnPassword.appendChild(document.createTextNode("💾"));
                btnPassword.title = "Save new password";
                btnPassword.disabled = true;
                inputPassword.onkeyup = function() {
                    btnPassword.disabled = inputPassword.value.length == 0;
                }
                btnPassword.onclick = function() {
                    changePassword(username, inputPassword);
                }
                
                const tdRead = document.createElement("td");
                trUser.appendChild(tdRead);
                tdRead.className = "role-read";
                const inputRead = document.createElement("input");
                tdRead.appendChild(inputRead);
                inputRead.type = "checkbox";
                inputRead.title = "Read";
                if (userRoles.indexOf("read") >= 0) inputRead.checked = true;
                
                const tdWrite = document.createElement("td");
                trUser.appendChild(tdWrite);
                tdWrite.className = "role-write";
                const inputWrite = document.createElement("input");
                tdWrite.appendChild(inputWrite);
                inputWrite.type = "checkbox";
                inputWrite.title = "Write";
                if (userRoles.indexOf("write") >= 0) inputWrite.checked = true;
                
                const tdAdmin = document.createElement("td");
                trUser.appendChild(tdAdmin);
                tdAdmin.className = "role-admin";
                const inputAdmin = document.createElement("input");
                tdAdmin.appendChild(inputAdmin);
                inputAdmin.type = "checkbox";
                inputAdmin.title = "Admin";
                if (userRoles.indexOf("admin") >= 0) inputAdmin.checked = true;

                const updateUserRoles = function() {
                    const roles = [];
                    if (inputRead.checked) roles.push("read");
                    if (inputWrite.checked) roles.push("write");
                    if (inputAdmin.checked) roles.push("admin");
                    updateUser(username, roles);
                };
                inputRead.onclick = updateUserRoles;
                inputWrite.onclick = updateUserRoles;
                inputAdmin.onclick = updateUserRoles;
                
                const tdDelete = document.createElement("td");
                trUser.appendChild(tdDelete);
                tdDelete.className = "delete";
                const btnDelete = document.createElement("button");
                tdDelete.appendChild(btnDelete);
                btnDelete.appendChild(document.createTextNode("❌"));
                btnDelete.onclick = function() {
                    if (confirm(`Are you sure you want to delete ${username}?`)) {
                        deleteUser(username);
                    }
                };

            }
//        } catch(x) {
//            console.error(`Error parsing users ${x}:\n${this.responseText}`);
//        }
    });
    oReq.addEventListener("error", function(r) {
        console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
    });
    oReq.open("GET", "wysiwiki/admin/users/");
    oReq.send();
}

function createUser(username, password, roles) {
    let oReq = new XMLHttpRequest();
    oReq.addEventListener("load", function(e) {
        if (this.status != 200) {
            showError(this.responseText);
        } else {
            showMessage(`Added ${username}`);
            document.getElementById("username").value = "";
            document.getElementById("password").value = "";
        }
        readUsers();
    });
    oReq.addEventListener("error", function(r) {
        console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
        showError(`${r.status}: ${r.statusText}\n${r.responseText}`);
    });
    oReq.open("PUT", `wysiwiki/admin/users/${username}`);

    // construct XML body
    const xmlDoc = document.implementation.createDocument(null, "user");
    const xmlUser = xmlDoc.documentElement;
    const xmlPassword = xmlDoc.createElement("password");
    xmlPassword.appendChild(xmlDoc.createTextNode(password));
    xmlUser.appendChild(xmlPassword);
    const xmlRoles = xmlDoc.createElement("roles");
    for (role of roles) {
        const xmlRole = xmlDoc.createElement("role");
        xmlRole.setAttribute("rolename", role);
        xmlRoles.appendChild(xmlRole);
    } // next role
    xmlUser.appendChild(xmlRoles);
    
    oReq.send(xmlDoc);
}

function changePassword(username, inputPassword) {
    if (username && inputPassword.value) {
        let oReq = new XMLHttpRequest();
        oReq.addEventListener("load", function(e) {
            if (this.status != 200) {
                showError(this.responseText);
            } else {
                showMessage(`Password changed for ${username}`);
                inputPassword.value = "";
                inputPassword.onkeyup(null);
            }
        });
        oReq.addEventListener("error", function(r) {
            console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
            showError(`${r.status}: ${r.statusText}\n${r.responseText}`);
        });
        oReq.open("PUT", `wysiwiki/admin/users/${username}`);

        // construct XML body
        const xmlDoc = document.implementation.createDocument(null, "user");
        const xmlUser = xmlDoc.documentElement;
        const xmlPassword = xmlDoc.createElement("password");
        xmlPassword.appendChild(xmlDoc.createTextNode(inputPassword.value));
        xmlUser.appendChild(xmlPassword);
    
        oReq.send(xmlDoc);
    }
}

function updateUser(username, roles) {
    let oReq = new XMLHttpRequest();
    oReq.addEventListener("load", function(e) {
        if (this.status != 200) {
            showError(this.responseText);
        } else {
            showMessage(`Update roles for ${username}`);
        }
        readUsers();
    });
    oReq.addEventListener("error", function(r) {
        console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
        showError(`${r.status}: ${r.statusText}\n${r.responseText}`);
    });
    oReq.open("PUT", `wysiwiki/admin/users/${username}`);

    // construct XML body
    const xmlDoc = document.implementation.createDocument(null, "user");
    const xmlUser = xmlDoc.documentElement;
    const xmlRoles = xmlDoc.createElement("roles");
    for (role of roles) {
        const xmlRole = xmlDoc.createElement("role");
        xmlRole.setAttribute("rolename", role);
        xmlRoles.appendChild(xmlRole);
    } // next role
    xmlUser.appendChild(xmlRoles);
    
    oReq.send(xmlDoc);
}

function deleteUser(username) {
    let oReq = new XMLHttpRequest();
    oReq.addEventListener("load", function(e) {
        if (this.status != 200) {
            showError(this.responseText);
        } else {
            showMessage(`Deleted ${username}`);
        }
        readUsers();
    });
    oReq.addEventListener("error", function(r) {
        console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
        showError(`${r.status}: ${r.statusText}\n${r.responseText}`);
    });
    oReq.open("DELETE", `wysiwiki/admin/users/${username}`);
    oReq.send();
}

function showMessage(message) {
    document.getElementById("message").innerHTML = `<span>${message}</span>`;
}
function showError(message) {
    document.getElementById("message").innerHTML = `<span class="error">${message}</span>`;
}

window.addEventListener("load", function(e) {
    readUsers();
    document.getElementById("createUser").onclick = function() {
        const username = document.getElementById("username");
        if (username.value.trim() == "") {
            showError("You must specify a Username");
            username.focus();
            return;
        }
        const password = document.getElementById("password");
        if (password.value.trim() == "") {
            showError("You must specify a Password");
            password.focus();
            return;
        }
        const roles = [];
        if (document.getElementById("read").checked) roles.push("read");
        if (document.getElementById("write").checked) roles.push("write");
        if (document.getElementById("admin").checked) roles.push("admin");
        createUser(username.value, password.value, roles);
    }
});
