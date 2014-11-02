package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.ModuleSet;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.resolved.ResolvedModuleSet;
import org.apache.maven.plugin.assembly.resolved.functions.ResolvedModuleSetConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WrappedAssembly
{
    private final Assembly assembly;

    private Set<Artifact> resolvedArtifacts;


    private final List<WrappedContainerDescriptorHandlerConfig> containerDescriptorHandlers =
        new ArrayList<WrappedContainerDescriptorHandlerConfig>(  );

    private final List<WrappedModuleSet> moduleSets = new ArrayList<WrappedModuleSet>(  );

    private final List<WrappedFileItem> fileItems = new ArrayList<WrappedFileItem>(  );

    private final List<WrappedFileSet> fileSets = new ArrayList<WrappedFileSet>(  );

    private final List<WrappedRepository> repositories = new ArrayList<WrappedRepository>(  );

    private final List<WrappedDependencySet> dependencySets = new ArrayList<WrappedDependencySet>(  );

    public WrappedAssembly( Assembly assembly )
    {
        this.assembly = assembly;
        for ( ContainerDescriptorHandlerConfig item : assembly.getContainerDescriptorHandlers() )
        {
            containerDescriptorHandlers.add( new WrappedContainerDescriptorHandlerConfig( item ) );
        }
        for ( ModuleSet moduleSet : assembly.getModuleSets() )
        {
            moduleSets.add( new WrappedModuleSet( moduleSet ));
        }
        for ( FileItem fileItem : assembly.getFiles() )
        {
            fileItems.add( new WrappedFileItem( fileItem ));
        }
        for ( FileSet fileSet : assembly.getFileSets() )
        {
            fileSets.add( new WrappedFileSet( fileSet ));
        }
        for ( Repository repository : assembly.getRepositories() )
        {
            repositories.add( new WrappedRepository( repository ));
        }
        for ( DependencySet dependencySet : assembly.getDependencySets() )
        {
            dependencySets.add( new WrappedDependencySet( dependencySet ));
        }
    }

    public List<WrappedContainerDescriptorHandlerConfig> getContainerDescriptorHandlers()
    {
        return containerDescriptorHandlers;
    }

    public List<WrappedModuleSet> getModuleSets()
    {
        return moduleSets;
    }

    public List<WrappedFileItem> getFileItems()
    {
        return fileItems;
    }

    public List<WrappedFileSet> getFileSets()
    {
        return fileSets;
    }

    public List<WrappedRepository> getRepositories()
    {
        return repositories;
    }

    public List<String> getComponentDescriptors()
    {
        return assembly.getComponentDescriptors();
    }

    public String getId()
    {
        return assembly.getId();
    }

    public List<String> getFormats()
    {
        return assembly.getFormats();
    }

    public String getModelEncoding()
    {
        return assembly.getModelEncoding();
    }

    public boolean isIncludeBaseDirectory()
    {
        return assembly.isIncludeBaseDirectory();
    }

    public boolean isIncludeSiteDirectory()
    {
        return assembly.isIncludeSiteDirectory();
    }

    public String getBaseDirectory()
    {
        return assembly.getBaseDirectory();
    }

    public List<WrappedDependencySet> getDependencySets()
    {
        return dependencySets;
    }

    public Set<Artifact> getResolvedArtifacts()
    {
        return resolvedArtifacts;
    }

    public void setResolvedArtifacts( Set<Artifact> resolvedArtifacts )
    {
        this.resolvedArtifacts = resolvedArtifacts;
    }


    public void forEachResolvedModule( ResolvedModuleSetConsumer resolvedModuleSetConsumer )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        if ( moduleSets == null )
            return;
        for ( WrappedModuleSet resolvedModuleSet : moduleSets )
        {
            resolvedModuleSetConsumer.accept( resolvedModuleSet );
        }
    }

    public List<FileItem> getFiles()
    {
        return assembly.getFiles();
    }
}
