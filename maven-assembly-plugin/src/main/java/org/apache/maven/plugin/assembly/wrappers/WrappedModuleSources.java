package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.ModuleSources;

import java.util.ArrayList;
import java.util.List;

public class WrappedModuleSources
{
    private final ModuleSources moduleSources;
    private final List<WrappedFileSet> fileSets = new ArrayList<WrappedFileSet>(  );

    public WrappedModuleSources( ModuleSources moduleSources )
    {
        this.moduleSources = moduleSources;
        for ( FileSet fileSet : moduleSources.getFileSets() )
        {
            fileSets.add( new WrappedFileSet( fileSet ));
        }
    }

    public List<WrappedFileSet> getFileSets()
    {
        return fileSets;
    }

    public String getDirectoryMode()
    {
        return moduleSources.getDirectoryMode();
    }

    public List<String> getExcludes()
    {
        return moduleSources.getExcludes();
    }

    public String getFileMode()
    {
        return moduleSources.getFileMode();
    }

    public List<String> getIncludes()
    {
        return moduleSources.getIncludes();
    }

    public String getOutputDirectory()
    {
        return moduleSources.getOutputDirectory();
    }

    public String getOutputDirectoryMapping()
    {
        return moduleSources.getOutputDirectoryMapping();
    }

    public boolean isExcludeSubModuleDirectories()
    {
        return moduleSources.isExcludeSubModuleDirectories();
    }

    public boolean isIncludeModuleDirectory()
    {
        return moduleSources.isIncludeModuleDirectory();
    }

    public boolean isUseDefaultExcludes()
    {
        return moduleSources.isUseDefaultExcludes();
    }
}
