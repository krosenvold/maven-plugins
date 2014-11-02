package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.UnpackOptions;

import java.util.List;

public class WrappedDependencySet
{
    private final DependencySet dependencySet;

    public WrappedDependencySet( DependencySet dependencySet )
    {
        this.dependencySet = dependencySet;
    }

    public String getDirectoryMode()
    {
        return dependencySet.getDirectoryMode();
    }

    public List<String> getExcludes()
    {
        return dependencySet.getExcludes();
    }

    public String getFileMode()
    {
        return dependencySet.getFileMode();
    }

    public List<String> getIncludes()
    {
        return dependencySet.getIncludes();
    }

    public String getOutputDirectory()
    {
        return dependencySet.getOutputDirectory();
    }

    public String getOutputFileNameMapping()
    {
        return dependencySet.getOutputFileNameMapping();
    }

    public String getScope()
    {
        return dependencySet.getScope();
    }

    public UnpackOptions getUnpackOptions()
    {
        return dependencySet.getUnpackOptions();
    }

    public boolean isUnpack()
    {
        return dependencySet.isUnpack();
    }

    public boolean isUseProjectArtifact()
    {
        return dependencySet.isUseProjectArtifact();
    }

    public boolean isUseProjectAttachments()
    {
        return dependencySet.isUseProjectAttachments();
    }

    public boolean isUseStrictFiltering()
    {
        return dependencySet.isUseStrictFiltering();
    }

    public boolean isUseTransitiveDependencies()
    {
        return dependencySet.isUseTransitiveDependencies();
    }

    public boolean isUseTransitiveFiltering()
    {
        return dependencySet.isUseTransitiveFiltering();
    }

    public void setUseProjectArtifact( boolean useProjectArtifact )
    {
        dependencySet.setUseProjectArtifact( useProjectArtifact );
    }
}
