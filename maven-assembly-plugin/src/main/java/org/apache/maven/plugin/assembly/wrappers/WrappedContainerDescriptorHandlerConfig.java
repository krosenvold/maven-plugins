package org.apache.maven.plugin.assembly.wrappers;

import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;

public class WrappedContainerDescriptorHandlerConfig
{
    private final ContainerDescriptorHandlerConfig containerDescriptorHandlerConfig;

    public WrappedContainerDescriptorHandlerConfig( ContainerDescriptorHandlerConfig containerDescriptorHandlerConfig )
    {
        this.containerDescriptorHandlerConfig = containerDescriptorHandlerConfig;
    }

    public String getHandlerName()
    {
        return containerDescriptorHandlerConfig.getHandlerName();
    }

    public Object getConfiguration()
    {
        return containerDescriptorHandlerConfig.getConfiguration();
    }
}
