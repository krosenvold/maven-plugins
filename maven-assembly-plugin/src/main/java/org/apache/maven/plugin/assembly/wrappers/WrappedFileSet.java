package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.FileSet;

import java.util.List;

/**
 * Created by kristian on 02.11.14.
 */
public class WrappedFileSet
{
    private final FileSet fileSet;

    public WrappedFileSet( FileSet fileSet )
    {
        this.fileSet = fileSet;
    }

    public String getDirectory()
    {
        return fileSet.getDirectory();
    }

    public String getDirectoryMode()
    {
        return fileSet.getDirectoryMode();
    }

    public List<String> getExcludes()
    {
        return fileSet.getExcludes();
    }

    public String getFileMode()
    {
        return fileSet.getFileMode();
    }

    public List<String> getIncludes()
    {
        return fileSet.getIncludes();
    }

    public String getLineEnding()
    {
        return fileSet.getLineEnding();
    }

    public String getOutputDirectory()
    {
        return fileSet.getOutputDirectory();
    }

    public boolean isFiltered()
    {
        return fileSet.isFiltered();
    }

    public boolean isUseDefaultExcludes()
    {
        return fileSet.isUseDefaultExcludes();
    }
}
