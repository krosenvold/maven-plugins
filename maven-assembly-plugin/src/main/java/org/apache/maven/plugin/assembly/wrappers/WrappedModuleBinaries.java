package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.ModuleBinaries;
import org.apache.maven.plugin.assembly.model.UnpackOptions;

import java.util.ArrayList;
import java.util.List;

public class WrappedModuleBinaries
{
    private final ModuleBinaries moduleBinaries;

    private final List<WrappedDependencySet> dependencySets = new ArrayList<WrappedDependencySet>(  );

    public WrappedModuleBinaries( ModuleBinaries moduleBinaries )
    {
        this.moduleBinaries = moduleBinaries;
        for ( DependencySet dependencySet : moduleBinaries.getDependencySets() )
        {
            dependencySets.add( new WrappedDependencySet( dependencySet ));
        }
    }

    public List<WrappedDependencySet> getDependencySets()
    {
        return dependencySets;
    }

    public String getDirectoryMode()
    {
        return moduleBinaries.getDirectoryMode();
    }

    public List<String> getExcludes()
    {
        return moduleBinaries.getExcludes();
    }

    public String getFileMode()
    {
        return moduleBinaries.getFileMode();
    }

    public List<String> getIncludes()
    {
        return moduleBinaries.getIncludes();
    }

    public String getOutputDirectory()
    {
        return moduleBinaries.getOutputDirectory();
    }

    public String getOutputFileNameMapping()
    {
        return moduleBinaries.getOutputFileNameMapping();
    }

    public UnpackOptions getUnpackOptions()
    {
        return moduleBinaries.getUnpackOptions();
    }

    public boolean isIncludeDependencies()
    {
        return moduleBinaries.isIncludeDependencies();
    }

    public boolean isUnpack()
    {
        return moduleBinaries.isUnpack();
    }

    public String getAttachmentClassifier()
    {
        return moduleBinaries.getAttachmentClassifier();
    }
}
