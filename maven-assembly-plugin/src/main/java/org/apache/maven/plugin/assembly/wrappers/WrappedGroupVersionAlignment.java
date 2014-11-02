package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.GroupVersionAlignment;

import java.util.List;

public class WrappedGroupVersionAlignment
{
    private final GroupVersionAlignment groupVersionAlignment;

    public WrappedGroupVersionAlignment( GroupVersionAlignment groupVersionAlignment )
    {
        this.groupVersionAlignment = groupVersionAlignment;
    }

    public List<String> getExcludes()
    {
        return groupVersionAlignment.getExcludes();
    }

    public String getId()
    {
        return groupVersionAlignment.getId();
    }

    public String getVersion()
    {
        return groupVersionAlignment.getVersion();
    }
}
