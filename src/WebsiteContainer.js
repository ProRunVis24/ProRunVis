import React, { useState, useEffect } from "react";
import DirectoryBar from "./WebsiteElements/DirectoryBar";
import EditorManager from "./Editor/EditorManager";
import JsonManager from "./Editor/JsonManager";
import JsonViewer from "./JsonViewer"; // Importiere die JsonViewer-Komponente

/**
 * Contains both the right and left component. It handles communication between the two, mainly regarding changes in
 * the uploaded project files, currently displayed and active files. Active meaning in which file the currently
 * selected function is located.
 * @returns {Element} Right and Left Component
 * @constructor
 */
function WebsiteContainer() {
    const [displayedFile, setDisplayedFile] = useState(null);
    const [activeFile, setActiveFile] = useState(null);
    const [jsonManager, setJsonManager] = useState(null);
    const [uploadedFiles, setUploadedFiles] = useState([]);
    const [jsonData, setJsonData] = useState(null); // State für die JSON-Daten

    /**
     * Changes both the current active and current displayed file to the file located at the given filepath.
     * @param path string of file path relative to the project root.
     */
    function setActiveAndDisplayed(path) {
        uploadedFiles.forEach((uploadedFile) => {
            if (path === uploadedFile.webkitRelativePath) {
                setDisplayedFile(uploadedFile);
                setActiveFile(uploadedFile);
            }
        });
    }

    /**
     * Resets the displayed file to the currently active file.
     */
    function setDisplayedToActive() {
        setDisplayedFile(activeFile);
    }

    /**
     * Determines whether the currently displayed file is also the active file.
     * @returns {boolean} result of the above.
     */
    function isActiveDisplayed() {
        if (activeFile == null || displayedFile == null)
            return false;
        return activeFile === displayedFile;
    }

    /**
     * Takes in files and creates a new trace. Then create a JsonManager with json trace data provided by the backend.
     * TODO Create link and fetch to the backend here currently using hardcoded example json
     * @param files project files to be traced.
     */
    async function passOnUploadedFiles(files){
        setUploadedFiles(files);

        await fetch("/api/upload",{
            method: "POST",
            body: new FormData(document.getElementById("upload-form"))
        });

        await fetch("/api/process")
            .then((response) => response.json())
            .then((jsonData) => setJsonManager(new JsonManager(jsonData)));
    }

    // useEffect, um jsonData zu setzen, wenn jsonManager sich ändert
    useEffect(() => {
        if (jsonManager) {
            setJsonData(jsonManager.getJsonData());
        }
    }, [jsonManager]);

    return (
        <>
            <DirectoryBar
                setDisplayedFile={setDisplayedFile}
                setDisplayedToActive={setDisplayedToActive}
                passOnUploadedFiles={passOnUploadedFiles}
            />
            <EditorManager
                displayedFile={displayedFile}
                setActiveAndDisplayed={setActiveAndDisplayed}
                isActiveDisplayed={isActiveDisplayed}
                jsonManager={jsonManager}
            />
            {/* Füge die JsonViewer-Komponente hinzu */}
            {jsonData && <JsonViewer jsonData={jsonData} />}
        </>
    );
}

export default WebsiteContainer;
