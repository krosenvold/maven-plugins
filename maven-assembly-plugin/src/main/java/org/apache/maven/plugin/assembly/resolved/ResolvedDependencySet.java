package org.apache.maven.plugin.assembly.resolved;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.model.DependencySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResolvedDependencySet
{
    private final DependencySet dependencySet;

    private final Set<Artifact> artifacts;

    public ResolvedDependencySet( DependencySet dependencySet, Set<Artifact> artifacts )
    {
        this.dependencySet = dependencySet;
        this.artifacts = artifacts;
    }


    public Set<Artifact> getArtifacts()
    {
        return artifacts;
    }

    public DependencySet getDependencySet()
    {
        return dependencySet;
    }

    public static ResolvedDependencySet createResolvedDependencySet(DependencySet dependencySet )
    {
        return new ResolvedDependencySet( dependencySet, null );
    }

    public static List<ResolvedDependencySet> createResolvedDependencySet(List<DependencySet> dependencySet )
    {
        List<ResolvedDependencySet> result = new ArrayList<ResolvedDependencySet>(  );
        for ( DependencySet set : dependencySet )
        {
            result.add( createResolvedDependencySet( set ));
        }
        return result;
    }

    public ResolvedDependencySet withArtifacts( Set<Artifact> artifacts )
    {
        return new ResolvedDependencySet( dependencySet, artifacts );
    }


}
