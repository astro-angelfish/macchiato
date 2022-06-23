package macchiato;

import java.io.File;

public class CoffeeConfiguration {
    private boolean verbose;
    private String host;
    private int port;
    private File[] sourceDirectories;
    private File[] jarResources;
    private boolean listen;
    private File scriptFile;

    public boolean isVerbose() {
        return verbose;
    }

    public int getPort() {
        return port;
    }

    public File[] getSourceDirectories() {
        return sourceDirectories;
    }

    public boolean isListen() {
        return listen;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setSourceDirectories(File[] sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    public void setListen(boolean listen) {
        this.listen = listen;
    }

    public File getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(File scriptFile) {
        this.scriptFile = scriptFile;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public File[] getJarResources() {
        return jarResources;
    }

    public void setJarResources(File[] jarResources) {
        this.jarResources = jarResources;
    }
}
