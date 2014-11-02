package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.GroupVersionAlignment;
import org.apache.maven.plugin.assembly.model.Repository;

import java.util.ArrayList;
import java.util.List;

public class WrappedRepository
{
    private final Repository repository;

    private final List<WrappedGroupVersionAlignment> groupVersionAlignments = new ArrayList<WrappedGroupVersionAlignment>(  );
    public WrappedRepository( Repository repository )
    {
        this.repository = repository;
        for ( GroupVersionAlignment groupVersionAlignment : repository.getGroupVersionAlignments() )
        {
            groupVersionAlignments.add( new WrappedGroupVersionAlignment( groupVersionAlignment ));
        }
    }

    public List<WrappedGroupVersionAlignment> getGroupVersionAlignments()
    {
        return groupVersionAlignments;
    }

    public String getDirectoryMode()
    {
        return repository.getDirectoryMode();
    }

    public List<String> getExcludes()
    {
        return repository.getExcludes();
    }

    public String getFileMode()
    {
        return repository.getFileMode();
    }

    public List<String> getIncludes()
    {
        return repository.getIncludes();
    }

    public String getOutputDirectory()
    {
        return repository.getOutputDirectory();
    }

    public String getScope()
    {
        return repository.getScope();
    }

    public boolean isIncludeMetadata()
    {
        return repository.isIncludeMetadata();
    }
}
