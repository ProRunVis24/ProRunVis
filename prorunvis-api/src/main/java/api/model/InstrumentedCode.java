package api.model;

import jakarta.persistence.*;

@Entity
public class InstrumentedCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    @Lob
    private String instrumentedSourceZip;

    @Lob
    private String nodeMapJson;

    public Long getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getInstrumentedSourceZip() {
        return instrumentedSourceZip;
    }

    public String getNodeMapJson() {
        return nodeMapJson;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setInstrumentedSourceZip(String instrumentedSourceZip) {
        this.instrumentedSourceZip = instrumentedSourceZip;
    }

    public void setNodeMapJson(String nodeMapJson) {
        this.nodeMapJson = nodeMapJson;
    }
}
