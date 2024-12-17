import React from 'react';
import ReactJson from 'react-json-view';
import './JsonViewer.css';

function JsonViewer({ jsonData }) {
    const handleDownload = () => {
        const fileName = 'data.json';
        const jsonStr = JSON.stringify(jsonData, null, 2);

        const blob = new Blob([jsonStr], { type: 'application/json' });
        const url = URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();

        // Aufräumen
        URL.revokeObjectURL(url);
    };

    return (
        <div className="json-viewer">
            <h2>JSON-Datei anzeigen</h2>
            <div className="json-display">
                <ReactJson src={jsonData} collapsed={2} />
            </div>
            <button onClick={handleDownload} className="download-button">
                JSON-Datei herunterladen
            </button>
        </div>
    );
}

// Add PropTypes validation
JsonViewer.propTypes = {
    jsonData: PropTypes.object.isRequired, // jsonData must be an object and is required
};

export default JsonViewer;
