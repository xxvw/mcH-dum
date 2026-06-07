(function () {
    "use strict";

    var state = {
        server: "",
        path: "",
        selectedFile: "",
        servers: []
    };

    var elements = {};
    var scene = null;

    document.addEventListener("DOMContentLoaded", function () {
        elements = {
            serverSelect: document.getElementById("serverSelect"),
            startForm: document.getElementById("startForm"),
            stopForm: document.getElementById("stopForm"),
            deleteForm: document.getElementById("deleteForm"),
            runningBadge: document.getElementById("runningBadge"),
            serverCount: document.getElementById("serverCount"),
            serverList: document.getElementById("serverList"),
            consoleOutput: document.getElementById("consoleOutput"),
            consoleState: document.getElementById("consoleState"),
            commandForm: document.getElementById("commandForm"),
            commandInput: document.getElementById("commandInput"),
            fileList: document.getElementById("fileList"),
            breadcrumb: document.getElementById("breadcrumb"),
            upButton: document.getElementById("upButton"),
            uploadForm: document.getElementById("uploadForm"),
            uploadInput: document.getElementById("uploadInput"),
            fileEditor: document.getElementById("fileEditor"),
            selectedFile: document.getElementById("selectedFile"),
            saveFileButton: document.getElementById("saveFileButton"),
            deleteFileButton: document.getElementById("deleteFileButton"),
            toast: document.getElementById("toast"),
            sceneTitle: document.getElementById("sceneTitle"),
            sceneDetail: document.getElementById("sceneDetail")
        };

        state.server = elements.serverSelect && elements.serverSelect.value ? elements.serverSelect.value : "";
        bindEvents();
        initScene();
        animateIn();
        refreshStatus();
        refreshConsole();
        refreshFiles();
        window.setInterval(refreshStatus, 5000);
        window.setInterval(refreshConsole, 2000);
    });

    function bindEvents() {
        if (elements.serverSelect) {
            elements.serverSelect.addEventListener("change", function () {
                selectServer(elements.serverSelect.value);
            });
        }
        if (elements.commandForm) {
            elements.commandForm.addEventListener("submit", sendCommand);
        }
        if (elements.upButton) {
            elements.upButton.addEventListener("click", goUp);
        }
        if (elements.uploadForm) {
            elements.uploadForm.addEventListener("submit", uploadFile);
        }
        if (elements.saveFileButton) {
            elements.saveFileButton.addEventListener("click", saveFile);
        }
        if (elements.deleteFileButton) {
            elements.deleteFileButton.addEventListener("click", deleteFile);
        }
    }

    function selectServer(serverName) {
        state.server = serverName || "";
        state.path = "";
        state.selectedFile = "";
        if (elements.serverSelect && elements.serverSelect.value !== state.server) {
            elements.serverSelect.value = state.server;
        }
        updateActionForms();
        refreshConsole();
        refreshFiles();
        renderServers();
    }

    function updateActionForms() {
        var server = encodeURIComponent(state.server || "");
        var disabled = !server;
        if (elements.startForm) {
            elements.startForm.action = server ? "/admin/servers/" + server + "/start" : "";
            elements.startForm.querySelector("button").disabled = disabled;
        }
        if (elements.stopForm) {
            elements.stopForm.action = server ? "/admin/servers/" + server + "/stop" : "";
            elements.stopForm.querySelector("button").disabled = disabled;
        }
        if (elements.deleteForm) {
            elements.deleteForm.action = server ? "/admin/servers/" + server + "/delete" : "";
            elements.deleteForm.querySelector("button").disabled = disabled;
        }
    }

    function refreshStatus() {
        return getJson("/admin/api/status")
            .then(function (data) {
                state.servers = data.servers || [];
                if (!state.server && state.servers.length > 0) {
                    state.server = state.servers[0].name;
                }
                renderMetrics(data);
                renderServerOptions();
                renderServers();
                updateActionForms();
                updateScene(data);
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function renderMetrics(data) {
        setMetric("totalServers", data.totalServers || 0);
        setMetric("runningServers", (data.runningServers || 0) + " running");
        setMetric("usedMemory", data.usedMemory || "0 B");
        setMetric("maxMemory", "max " + (data.maxMemory || "0 B"));
        setMetric("processCpuLoad", percent(data.processCpuLoad));
        setMetric("systemCpuLoad", "system " + percent(data.systemCpuLoad));
        setMetric("dataSize", data.dataSize || "0 B");
        setMetric("diskFree", "free " + (data.diskFree || "0 B"));
        if (elements.runningBadge) {
            elements.runningBadge.textContent = (data.runningServers || 0) + " / " + (data.maxServers || 0) + " running";
        }
        if (elements.serverCount) {
            elements.serverCount.textContent = (data.totalServers || 0) + " total";
        }
        if (elements.sceneTitle) {
            elements.sceneTitle.textContent = (data.runningServers || 0) + " / " + (data.totalServers || 0) + " servers";
        }
        if (elements.sceneDetail) {
            elements.sceneDetail.textContent = "CPU " + percent(data.processCpuLoad) + " / Memory " + (data.usedMemory || "0 B");
        }
    }

    function setMetric(name, value) {
        var node = document.querySelector("[data-metric='" + name + "']");
        if (node) {
            node.textContent = value;
        }
    }

    function renderServerOptions() {
        if (!elements.serverSelect) {
            return;
        }
        var current = state.server;
        elements.serverSelect.innerHTML = "";
        state.servers.forEach(function (server) {
            var option = document.createElement("option");
            option.value = server.name;
            option.textContent = server.name + " :" + server.port;
            elements.serverSelect.appendChild(option);
        });
        if (current && state.servers.some(function (server) { return server.name === current; })) {
            elements.serverSelect.value = current;
        } else if (state.servers.length > 0) {
            state.server = state.servers[0].name;
            elements.serverSelect.value = state.server;
        }
    }

    function renderServers() {
        if (!elements.serverList) {
            return;
        }
        elements.serverList.innerHTML = "";
        if (state.servers.length === 0) {
            elements.serverList.appendChild(emptyRow("No servers"));
            return;
        }
        state.servers.forEach(function (server) {
            var row = document.createElement("button");
            row.type = "button";
            row.className = "server-row" + (server.name === state.server ? " active" : "");
            row.innerHTML = [
                "<span class='row-title'><span>" + escapeHtml(server.name) + "</span>" + statusPill(server.status) + "</span>",
                "<span class='row-meta'>:" + escapeHtml(String(server.port)) + " / " + escapeHtml(server.size || "0 B") + "</span>"
            ].join("");
            row.addEventListener("click", function () {
                selectServer(server.name);
            });
            elements.serverList.appendChild(row);
        });
    }

    function refreshConsole() {
        if (!state.server || !elements.consoleOutput) {
            if (elements.consoleOutput) {
                elements.consoleOutput.textContent = "";
            }
            return Promise.resolve();
        }
        return getJson("/admin/api/servers/" + encodeURIComponent(state.server) + "/console")
            .then(function (data) {
                var lines = data.lines || [];
                elements.consoleOutput.textContent = lines.join("\n");
                elements.consoleOutput.scrollTop = elements.consoleOutput.scrollHeight;
                if (elements.consoleState) {
                    elements.consoleState.textContent = lines.length + " lines";
                }
            })
            .catch(function (error) {
                if (elements.consoleState) {
                    elements.consoleState.textContent = "Error";
                }
                showToast(error.message, "error");
            });
    }

    function sendCommand(event) {
        event.preventDefault();
        if (!state.server || !elements.commandInput || !elements.commandInput.value.trim()) {
            return;
        }
        var form = new FormData();
        form.set("command", elements.commandInput.value.trim());
        fetch("/admin/api/servers/" + encodeURIComponent(state.server) + "/command", {
            method: "POST",
            body: form
        })
            .then(readResponse)
            .then(function (data) {
                showToast(data.message || "Sent", data.ok ? "success" : "error");
                elements.commandInput.value = "";
                refreshConsole();
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function refreshFiles() {
        if (!state.server || !elements.fileList) {
            clearEditor();
            if (elements.fileList) {
                elements.fileList.innerHTML = "";
            }
            return Promise.resolve();
        }
        var params = new URLSearchParams();
        params.set("path", state.path || "");
        if (elements.breadcrumb) {
            elements.breadcrumb.textContent = "/" + (state.path || "");
        }
        return getJson("/admin/api/servers/" + encodeURIComponent(state.server) + "/files?" + params.toString())
            .then(function (data) {
                renderFiles(data.entries || []);
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function renderFiles(entries) {
        elements.fileList.innerHTML = "";
        if (entries.length === 0) {
            elements.fileList.appendChild(emptyRow("Empty"));
            return;
        }
        entries.forEach(function (entry) {
            var row = document.createElement("button");
            row.type = "button";
            row.className = "file-row " + (entry.directory ? "directory" : "file") + (entry.path === state.selectedFile ? " active" : "");
            row.innerHTML = [
                "<span class='row-title'><span>" + escapeHtml(entry.name) + "</span></span>",
                "<span class='row-meta'>" + escapeHtml(entry.directory ? entry.path : entry.size) + "</span>"
            ].join("");
            row.addEventListener("click", function () {
                if (entry.directory) {
                    state.path = entry.path;
                    state.selectedFile = "";
                    clearEditor();
                    refreshFiles();
                } else {
                    openFile(entry.path);
                }
            });
            elements.fileList.appendChild(row);
        });
    }

    function openFile(path) {
        var params = new URLSearchParams();
        params.set("path", path);
        getJson("/admin/api/servers/" + encodeURIComponent(state.server) + "/files/content?" + params.toString())
            .then(function (data) {
                state.selectedFile = data.path || path;
                if (elements.fileEditor) {
                    elements.fileEditor.value = data.content || "";
                }
                if (elements.selectedFile) {
                    elements.selectedFile.textContent = state.selectedFile;
                }
                refreshFiles();
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function saveFile() {
        if (!state.server || !state.selectedFile) {
            return;
        }
        var form = new FormData();
        form.set("path", state.selectedFile);
        form.set("content", elements.fileEditor ? elements.fileEditor.value : "");
        fetch("/admin/api/servers/" + encodeURIComponent(state.server) + "/files/content", {
            method: "POST",
            body: form
        })
            .then(readResponse)
            .then(function (data) {
                showToast(data.message || "Saved", data.ok ? "success" : "error");
                refreshFiles();
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function deleteFile() {
        if (!state.server || !state.selectedFile) {
            return;
        }
        var form = new FormData();
        form.set("path", state.selectedFile);
        fetch("/admin/api/servers/" + encodeURIComponent(state.server) + "/files/delete", {
            method: "POST",
            body: form
        })
            .then(readResponse)
            .then(function (data) {
                showToast(data.message || "Deleted", data.ok ? "success" : "error");
                clearEditor();
                refreshFiles();
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function uploadFile(event) {
        event.preventDefault();
        if (!state.server || !elements.uploadInput || elements.uploadInput.files.length === 0) {
            return;
        }
        var form = new FormData();
        form.set("path", state.path || "");
        form.set("file", elements.uploadInput.files[0]);
        fetch("/admin/api/servers/" + encodeURIComponent(state.server) + "/files/upload", {
            method: "POST",
            body: form
        })
            .then(readResponse)
            .then(function (data) {
                showToast(data.message || "Uploaded", data.ok ? "success" : "error");
                elements.uploadInput.value = "";
                refreshFiles();
            })
            .catch(function (error) {
                showToast(error.message, "error");
            });
    }

    function goUp() {
        if (!state.path) {
            return;
        }
        var parts = state.path.split("/").filter(Boolean);
        parts.pop();
        state.path = parts.join("/");
        state.selectedFile = "";
        clearEditor();
        refreshFiles();
    }

    function clearEditor() {
        state.selectedFile = "";
        if (elements.fileEditor) {
            elements.fileEditor.value = "";
        }
        if (elements.selectedFile) {
            elements.selectedFile.textContent = "No file";
        }
    }

    function initScene() {
        var canvas = document.getElementById("serverScene");
        if (!canvas) {
            return;
        }
        if (!window.THREE || !hasWebGl()) {
            initFallbackScene(canvas);
            return;
        }
        var renderer;
        try {
            renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: true, alpha: true });
        } catch (error) {
            initFallbackScene(canvas);
            return;
        }
        var threeScene = new THREE.Scene();
        var camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
        camera.position.set(0, 2.6, 7);
        var group = new THREE.Group();
        threeScene.add(group);
        threeScene.add(new THREE.AmbientLight(0x8ecae6, 1.2));
        var keyLight = new THREE.DirectionalLight(0xffffff, 2.2);
        keyLight.position.set(4, 5, 5);
        threeScene.add(keyLight);
        scene = { renderer: renderer, scene: threeScene, camera: camera, group: group, nodes: [] };
        resizeScene();
        window.addEventListener("resize", resizeScene);
        renderLoop();
    }

    function hasWebGl() {
        try {
            var canvas = document.createElement("canvas");
            return !!(canvas.getContext("webgl") || canvas.getContext("experimental-webgl"));
        } catch (error) {
            return false;
        }
    }

    function initFallbackScene(canvas) {
        scene = { fallback: true, canvas: canvas, data: { servers: [] }, phase: 0 };
        resizeScene();
        window.addEventListener("resize", resizeScene);
        renderLoop();
    }

    function updateScene(data) {
        if (scene && scene.fallback) {
            scene.data = data || { servers: [] };
            return;
        }
        if (!scene || !window.THREE) {
            return;
        }
        while (scene.group.children.length) {
            scene.group.remove(scene.group.children[0]);
        }
        scene.nodes = [];
        var servers = data.servers || [];
        var count = Math.max(servers.length, 1);
        var radius = Math.min(2.6, 1.1 + count * .16);
        servers.forEach(function (server, index) {
            var angle = (index / count) * Math.PI * 2;
            var geometry = new THREE.BoxGeometry(.54, .54, .54);
            var material = new THREE.MeshStandardMaterial({
                color: server.status === "RUNNING" ? 0x23c483 : 0x8fa3b8,
                roughness: .36,
                metalness: .18
            });
            var cube = new THREE.Mesh(geometry, material);
            cube.position.set(Math.cos(angle) * radius, Math.sin(index * 1.7) * .22, Math.sin(angle) * radius);
            cube.rotation.set(.4, .4, 0);
            scene.group.add(cube);
            scene.nodes.push(cube);
            if (window.gsap) {
                gsap.to(cube.scale, {
                    x: server.status === "RUNNING" ? 1.18 : .92,
                    y: server.status === "RUNNING" ? 1.18 : .92,
                    z: server.status === "RUNNING" ? 1.18 : .92,
                    duration: .8,
                    repeat: server.status === "RUNNING" ? -1 : 0,
                    yoyo: true,
                    ease: "sine.inOut"
                });
            }
        });
        var core = new THREE.Mesh(
            new THREE.IcosahedronGeometry(.82, 1),
            new THREE.MeshStandardMaterial({ color: 0x43b7bf, roughness: .28, metalness: .28 })
        );
        scene.group.add(core);
        scene.nodes.push(core);
    }

    function resizeScene() {
        if (!scene) {
            return;
        }
        if (scene.fallback) {
            var fallbackCanvas = scene.canvas;
            var fallbackWidth = fallbackCanvas.clientWidth || fallbackCanvas.parentElement.clientWidth || 640;
            var fallbackHeight = fallbackCanvas.clientHeight || fallbackCanvas.parentElement.clientHeight || 272;
            var ratio = window.devicePixelRatio || 1;
            fallbackCanvas.width = Math.max(1, Math.floor(fallbackWidth * ratio));
            fallbackCanvas.height = Math.max(1, Math.floor(fallbackHeight * ratio));
            return;
        }
        var canvas = scene.renderer.domElement;
        var width = canvas.clientWidth || canvas.parentElement.clientWidth || 640;
        var height = canvas.clientHeight || canvas.parentElement.clientHeight || 272;
        scene.renderer.setSize(width, height, false);
        scene.camera.aspect = width / height;
        scene.camera.updateProjectionMatrix();
    }

    function renderLoop() {
        if (!scene) {
            return;
        }
        if (scene.fallback) {
            drawFallbackScene();
            window.requestAnimationFrame(renderLoop);
            return;
        }
        scene.group.rotation.y += .006;
        scene.nodes.forEach(function (node, index) {
            node.rotation.x += .006 + index * .0004;
            node.rotation.y += .009;
        });
        scene.renderer.render(scene.scene, scene.camera);
        window.requestAnimationFrame(renderLoop);
    }

    function drawFallbackScene() {
        var canvas = scene.canvas;
        var ctx = canvas.getContext("2d");
        var width = canvas.width;
        var height = canvas.height;
        var ratio = window.devicePixelRatio || 1;
        var servers = (scene.data && scene.data.servers) || [];
        var count = Math.max(servers.length, 1);
        scene.phase += .015;

        ctx.clearRect(0, 0, width, height);
        ctx.fillStyle = "#111827";
        ctx.fillRect(0, 0, width, height);

        var cx = width * .58;
        var cy = height * .48;
        var radius = Math.min(width, height) * .28;
        ctx.strokeStyle = "rgba(157, 219, 210, .22)";
        ctx.lineWidth = 1 * ratio;
        for (var ring = 1; ring <= 3; ring++) {
            ctx.beginPath();
            ctx.arc(cx, cy, radius * ring / 3, 0, Math.PI * 2);
            ctx.stroke();
        }

        servers.forEach(function (server, index) {
            var angle = scene.phase + (index / count) * Math.PI * 2;
            var x = cx + Math.cos(angle) * radius;
            var y = cy + Math.sin(angle) * radius * .62;
            ctx.fillStyle = server.status === "RUNNING" ? "#23c483" : "#8fa3b8";
            ctx.beginPath();
            ctx.roundRect(x - 11 * ratio, y - 11 * ratio, 22 * ratio, 22 * ratio, 5 * ratio);
            ctx.fill();
            ctx.strokeStyle = "rgba(255, 255, 255, .38)";
            ctx.stroke();
        });

        ctx.fillStyle = "#43b7bf";
        ctx.beginPath();
        ctx.arc(cx, cy, 26 * ratio, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = "rgba(255, 255, 255, .72)";
        ctx.beginPath();
        ctx.arc(cx - 7 * ratio, cy - 8 * ratio, 7 * ratio, 0, Math.PI * 2);
        ctx.fill();
    }

    function animateIn() {
        if (!window.gsap) {
            return;
        }
        gsap.from(".command-strip, .scene-panel, .metric-card, .workbench > article", {
            y: 14,
            opacity: 0,
            duration: .55,
            stagger: .04,
            ease: "power2.out"
        });
    }

    function getJson(url) {
        return fetch(url).then(readResponse);
    }

    function readResponse(response) {
        return response.json().then(function (data) {
            if (!response.ok) {
                throw new Error(data.message || response.statusText);
            }
            return data;
        });
    }

    function showToast(message, type) {
        if (!elements.toast || !message) {
            return;
        }
        elements.toast.textContent = message;
        elements.toast.hidden = false;
        elements.toast.className = "notice " + (type === "error" ? "error" : "success");
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(function () {
            elements.toast.hidden = true;
        }, 3200);
    }

    function statusPill(status) {
        var cls = status === "RUNNING" ? "online" : (status === "STARTING" ? "warning" : "muted");
        return "<span class='pill " + cls + "'>" + escapeHtml(status || "UNKNOWN") + "</span>";
    }

    function emptyRow(text) {
        var row = document.createElement("div");
        row.className = "file-row";
        row.innerHTML = "<span class='row-meta'>" + escapeHtml(text) + "</span>";
        return row;
    }

    function percent(value) {
        var numeric = Number(value || 0);
        return numeric.toFixed(1).replace(".0", "") + "%";
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
})();
