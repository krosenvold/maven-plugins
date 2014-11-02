package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.FileItem;

public class WrappedFileItem
{
    private final FileItem fileItem;

    public WrappedFileItem( FileItem fileItem )
    {
        this.fileItem = fileItem;
    }

    public boolean isFiltered()
    {
        return fileItem.isFiltered();
    }

    public String getSource()
    {
        return fileItem.getSource();
    }

    public String getOutputDirectory()
    {
        return fileItem.getOutputDirectory();
    }

    public String getLineEnding()
    {
        return fileItem.getLineEnding();
    }

    public String getFileMode()
    {
        return fileItem.getFileMode();
    }

    public String getDestName()
    {
        return fileItem.getDestName();
    }
}
