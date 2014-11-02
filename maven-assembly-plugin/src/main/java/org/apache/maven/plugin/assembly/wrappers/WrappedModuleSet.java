package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.model.ModuleSet;

import java.util.List;
import java.util.Set;

public class WrappedModuleSet
{
    private final ModuleSet moduleSet;

    private Set<Artifact> resolvedArtifacts;
    private final WrappedModuleBinaries binaries;

    private final WrappedModuleSources sources;


    public WrappedModuleSet( ModuleSet moduleSet )
    {
        this.moduleSet = moduleSet;
        this.binaries = new WrappedModuleBinaries( moduleSet.getBinaries() );
        sources = new WrappedModuleSources( moduleSet.getSources() );
    }

    public WrappedModuleBinaries getBinaries()
    {
        return binaries;
    }

    public WrappedModuleSources getSources()
    {
        return sources;
    }

    public List<String> getExcludes()
    {
        return moduleSet.getExcludes();
    }

    public List<String> getIncludes()
    {
        return moduleSet.getIncludes();
    }

    public boolean isIncludeSubModules()
    {
        return moduleSet.isIncludeSubModules();
    }

    public boolean isUseAllReactorProjects()
    {
        return moduleSet.isUseAllReactorProjects();
    }

    public Set<Artifact> getResolvedArtifacts()
    {
        return resolvedArtifacts;
    }

    public void setResolvedArtifacts( Set<Artifact> resolvedArtifacts )
    {
        this.resolvedArtifacts = resolvedArtifacts;
    }

}

