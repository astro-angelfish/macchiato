package moe.orangemc.macchiato.plugin.library;

import moe.orangemc.macchiato.api.terminal.TerminalColor;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;
import moe.orangemc.macchiato.plugin.PluginDescriptionFile;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibraryLoader {
    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;
    private final WrappedTerminal startupTerminal;

    public LibraryLoader(WrappedTerminal startupTerminal) {
        this.startupTerminal = startupTerminal;
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        this.repository = locator.getService(RepositorySystem.class);
        this.session = MavenRepositorySystemUtils.newSession();

        session.setChecksumPolicy( RepositoryPolicy.CHECKSUM_POLICY_FAIL );
        session.setLocalRepositoryManager(repository.newLocalRepositoryManager(session, new LocalRepository("libraries" )));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferStarted(TransferEvent event) {
                startupTerminal.println(TerminalColor.GREEN + "Macchiato is downloading " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName() + ", please wait...");
            }
        });
        session.setReadOnly();

        this.repositories = repository.newResolutionRepositories( session, Collections.singletonList(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()));
    }

    public ClassLoader createLoader(PluginDescriptionFile desc) {
        if (desc.getLibraries().isEmpty()) {
            return null;
        }

        List<Dependency> dependencies = new ArrayList<>();
        for (String library : desc.getLibraries()) {
            Artifact artifact = new DefaultArtifact(library);
            Dependency dependency = new Dependency(artifact, null);

            dependencies.add(dependency);
        }

        DependencyResult result;
        try {
            result = repository.resolveDependencies( session, new DependencyRequest(new CollectRequest((Dependency) null, dependencies, repositories), null));
        } catch (DependencyResolutionException ex) {
            throw new RuntimeException("Error resolving libraries", ex);
        }

        List<URL> jarFiles = new ArrayList<>();
        for (ArtifactResult artifact : result.getArtifactResults())
        {
            File file = artifact.getArtifact().getFile();

            URL url;
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException ex) {
                throw new AssertionError(ex);
            }

            jarFiles.add( url );
        }

        return new URLClassLoader(jarFiles.toArray(new URL[0]), getClass().getClassLoader());
    }
}
