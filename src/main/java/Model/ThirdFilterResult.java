package Model;

public class ThirdFilterResult {
    String junitVersion;
    String projectName;
    String repositoryId;
    String repositoryName;
    String testingFramework;

    @Override
    public String toString() {
        return "ThirdFilterResult{" +
                "junitVersion='" + junitVersion + '\'' +
                ", projectName='" + projectName + '\'' +
                ", repositoryId='" + repositoryId + '\'' +
                ", repositoryName='" + repositoryName + '\'' +
                ", testingFramework='" + testingFramework + '\'' +
                '}';
    }

    public String getJunitVersion() {
        return junitVersion;
    }

    public void setJunitVersion(String junitVersion) {
        this.junitVersion = junitVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getTestingFramework() {
        return testingFramework;
    }

    public void setTestingFramework(String testingFramework) {
        this.testingFramework = testingFramework;
    }
}
